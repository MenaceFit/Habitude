package com.menacefit.habitude.domain.achievement

import com.menacefit.habitude.domain.model.AchievementType

/**
 * Aggregate counters the evaluator needs. Building this snapshot is the
 * repository/viewmodel's job (it knows how to query Room); the evaluator
 * itself stays a pure, easily-tested decision table.
 */
data class AchievementStatsSnapshot(
    val isFirstEverCompletion: Boolean = false,
    val bestStreakAcrossHabits: Int = 0,
    val totalHabitsCompletedAllTime: Int = 0,
    /** Length of the current run of consecutive 100%-completion days. */
    val consecutivePerfectDays: Int = 0,
    /** Longest streak ever achieved by any single habit. */
    val longestSingleHabitStreakDays: Int = 0,
    /** True on the exact day a habit's current streak surpasses its own previous best. */
    val justBrokeStreakRecord: Boolean = false,
    val completedHabitBefore7am: Boolean = false,
    val completedHabitAfter10pm: Boolean = false,
    val allHabitsCompletedToday: Boolean = false,
)

/** Pure rule table: badge unlock conditions (spec section 16), evaluated against a snapshot and the set already unlocked. */
object AchievementEvaluator {

    fun evaluate(snapshot: AchievementStatsSnapshot, alreadyUnlocked: Set<AchievementType>): List<AchievementType> {
        val newlyUnlocked = mutableListOf<AchievementType>()
        fun consider(type: AchievementType, condition: Boolean) {
            if (condition && type !in alreadyUnlocked) newlyUnlocked += type
        }

        consider(AchievementType.FIRST_DAY, snapshot.isFirstEverCompletion)
        consider(AchievementType.STREAK_7, snapshot.bestStreakAcrossHabits >= 7)
        consider(AchievementType.STREAK_30, snapshot.bestStreakAcrossHabits >= 30)
        consider(AchievementType.STREAK_100, snapshot.bestStreakAcrossHabits >= 100)
        consider(AchievementType.HABITS_TOTAL_10, snapshot.totalHabitsCompletedAllTime >= 10)
        consider(AchievementType.HABITS_TOTAL_100, snapshot.totalHabitsCompletedAllTime >= 100)
        consider(AchievementType.HABITS_TOTAL_500, snapshot.totalHabitsCompletedAllTime >= 500)
        consider(AchievementType.PERFECT_DAY, snapshot.consecutivePerfectDays >= 1)
        consider(AchievementType.PERFECT_WEEK, snapshot.consecutivePerfectDays >= 7)
        consider(AchievementType.PERFECT_MONTH, snapshot.consecutivePerfectDays >= 30)
        consider(AchievementType.RECORD_BROKEN, snapshot.justBrokeStreakRecord)
        consider(AchievementType.MORNING_ROUTINE, snapshot.completedHabitBefore7am)
        consider(AchievementType.NIGHT_ROUTINE, snapshot.completedHabitAfter10pm)
        consider(AchievementType.HABIT_90_DAYS, snapshot.longestSingleHabitStreakDays >= 90)
        consider(AchievementType.ALL_HABITS_DAY, snapshot.allHabitsCompletedToday)

        return newlyUnlocked
    }
}
