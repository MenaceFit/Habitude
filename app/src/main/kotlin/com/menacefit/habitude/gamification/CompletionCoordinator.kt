package com.menacefit.habitude.gamification

import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.data.repository.ProfileRepository
import com.menacefit.habitude.data.repository.StatsRepository
import com.menacefit.habitude.data.repository.TodayQuestSignals
import com.menacefit.habitude.data.repository.QuestRepository
import com.menacefit.habitude.domain.achievement.AchievementEvaluator
import com.menacefit.habitude.domain.achievement.AchievementStatsSnapshot
import com.menacefit.habitude.domain.model.AchievementType
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCompletion
import com.menacefit.habitude.domain.model.LevelInfo
import com.menacefit.habitude.domain.model.StreakResult
import com.menacefit.habitude.domain.model.isDueOn
import com.menacefit.habitude.domain.xp.LevelCurve
import com.menacefit.habitude.domain.xp.XpEngine
import com.menacefit.habitude.util.TimeProvider
import com.menacefit.habitude.util.localTimeOf
import com.menacefit.habitude.widget.WidgetRefresher
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.ceil

/** A completed interaction with a habit: everything the UI needs to play the right celebration. */
data class CompletionOutcome(
    val completion: HabitCompletion,
    val wasNewlyCompleted: Boolean,
    val xpAwarded: Int,
    val dailyGoalBonusAwarded: Boolean,
    val allHabitsBonusAwarded: Boolean,
    val newlyUnlockedAchievements: List<AchievementType>,
    val leveledUp: Boolean,
    val newLevelInfo: LevelInfo?,
    val streakRecordBroken: Boolean,
    val streak: StreakResult,
)

/**
 * The single entry point for "the user just acted on a habit" — every
 * screen that lets someone log progress (Today, habit list, widget) calls
 * through here instead of writing the completion itself, so XP, level-ups,
 * daily bonuses, streak records, achievements and quest progress can never
 * drift out of sync with each other.
 */
class CompletionCoordinator(
    private val habitRepository: HabitRepository,
    private val profileRepository: ProfileRepository,
    private val statsRepository: StatsRepository,
    private val questRepository: QuestRepository,
    private val timeProvider: TimeProvider,
    private val widgetRefresher: WidgetRefresher,
) {
    companion object {
        /** Fraction of today's due habits that counts as "today's goal reached" — a softer bar than the literal 100% "all habits" bonus. */
        private const val DAILY_GOAL_FRACTION = 0.8
        private val MORNING_ROUTINE_CUTOFF: LocalTime = LocalTime.of(7, 0)
        private val NIGHT_ROUTINE_CUTOFF: LocalTime = LocalTime.of(22, 0)
    }

    suspend fun recordProgress(habit: Habit, value: Double, checkedIds: Set<String> = emptySet()): CompletionOutcome {
        val today = timeProvider.todayLocalDate()
        val allHabits = habitRepository.getActiveHabitsOnce()
        val dueToday = allHabits.filter { it.isDueOn(today) }

        val completionsBefore = habitRepository.getCompletionsForDate(today)
        val (allDoneBefore, goalDoneBefore) = dailyProgressState(dueToday, completionsBefore)

        val streakBefore = habitRepository.computeStreak(habit)
        val wasCompletedBefore = habitRepository.getCompletionForHabitAndDate(habit.id, today)?.completed == true

        val completion = habitRepository.recordProgress(habit, today, value, checkedIds)
        val wasNewlyCompleted = !wasCompletedBefore && completion.completed

        val streakAfter = habitRepository.computeStreak(habit)
        val recordBroken = wasNewlyCompleted && streakBefore.best > 0 && streakAfter.current > streakBefore.best

        if (!wasNewlyCompleted) {
            // Progress was logged (e.g. partial quantity) but didn't cross
            // the completion line, or a completed habit was un-done: no
            // rewards to grant either way, just persist the raw progress
            // and keep today's rollup accurate.
            statsRepository.recomputeSnapshot(today, allHabits, habitRepository.getCompletionsForDate(today), xpEarnedToday = 0)
            widgetRefresher.refresh()
            return CompletionOutcome(completion, false, 0, false, false, emptyList(), false, null, false, streakAfter)
        }

        val completionsAfter = habitRepository.getCompletionsForDate(today)
        val (allDoneAfter, goalDoneAfter) = dailyProgressState(dueToday, completionsAfter)
        val dailyGoalBonusAwarded = goalDoneAfter && !goalDoneBefore
        val allHabitsBonusAwarded = allDoneAfter && !allDoneBefore

        val baseXp = XpEngine.xpForCompletion(habit.difficulty)
        val bonusXp = XpEngine.dailyBonusXp(dailyGoalBonusAwarded, allHabitsBonusAwarded)
        val xpAwarded = baseXp + bonusXp

        val profileBefore = profileRepository.getProfile()
        val xpBefore = profileBefore?.totalXp ?: 0
        profileRepository.addXp(xpAwarded)
        val levelBefore = LevelCurve.levelForTotalXp(xpBefore).level
        val levelAfterInfo = LevelCurve.levelForTotalXp(xpBefore + xpAwarded)
        val leveledUp = levelAfterInfo.level > levelBefore

        statsRepository.recomputeSnapshot(today, allHabits, completionsAfter, xpAwarded)

        val totalCompletedAllTime = habitRepository.getTotalCompletedCount()
        val alreadyUnlocked = profileRepository.getUnlockedAchievementTypes()
        val consecutivePerfectDays = statsRepository.consecutivePerfectDaysEndingAt(today)
        val completionLocalTime = timeProvider.localTimeOf(completion.timestamp)

        val achievementSnapshot = AchievementStatsSnapshot(
            isFirstEverCompletion = totalCompletedAllTime == 1,
            bestStreakAcrossHabits = streakAfter.best,
            totalHabitsCompletedAllTime = totalCompletedAllTime,
            consecutivePerfectDays = consecutivePerfectDays,
            longestSingleHabitStreakDays = streakAfter.best,
            justBrokeStreakRecord = recordBroken,
            completedHabitBefore7am = completionLocalTime.isBefore(MORNING_ROUTINE_CUTOFF),
            completedHabitAfter10pm = !completionLocalTime.isBefore(NIGHT_ROUTINE_CUTOFF),
            allHabitsCompletedToday = allDoneAfter,
        )
        val newlyUnlocked = AchievementEvaluator.evaluate(achievementSnapshot, alreadyUnlocked)
        if (newlyUnlocked.isNotEmpty()) profileRepository.unlockAchievements(newlyUnlocked)

        questRepository.refreshProgress(
            today,
            TodayQuestSignals(
                completedHabitIds = completionsAfter.filter { it.completed }.map { it.habitId }.toSet(),
                completedCountToday = completionsAfter.count { it.completed },
                dailyGoalReached = allDoneAfter,
                earliestCompletionHourToday = completionLocalTime.hour,
            ),
        )

        widgetRefresher.refresh()

        return CompletionOutcome(
            completion = completion,
            wasNewlyCompleted = true,
            xpAwarded = xpAwarded,
            dailyGoalBonusAwarded = dailyGoalBonusAwarded,
            allHabitsBonusAwarded = allHabitsBonusAwarded,
            newlyUnlockedAchievements = newlyUnlocked,
            leveledUp = leveledUp,
            newLevelInfo = if (leveledUp) levelAfterInfo else null,
            streakRecordBroken = recordBroken,
            streak = streakAfter,
        )
    }

    /** @return true if a freeze was available and applied to protect [habit]'s streak on [date] (typically yesterday). */
    suspend fun applyStreakFreeze(habit: Habit, date: LocalDate): Boolean {
        val consumed = profileRepository.consumeStreakFreeze()
        if (!consumed) return false
        habitRepository.applyStreakFreeze(habit.id, date)
        return true
    }

    private fun dailyProgressState(dueToday: List<Habit>, completions: List<HabitCompletion>): Pair<Boolean, Boolean> {
        val completedIds = completions.filter { it.completed }.map { it.habitId }.toSet()
        val scheduledCount = dueToday.size
        val completedCount = dueToday.count { it.id in completedIds }
        val allDone = scheduledCount > 0 && completedCount == scheduledCount
        val goalDone = scheduledCount > 0 && completedCount >= ceil(scheduledCount * DAILY_GOAL_FRACTION).toInt()
        return allDone to goalDone
    }
}
