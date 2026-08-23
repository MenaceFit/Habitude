package com.menacefit.habitude.domain.quest

import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.QuestInstance
import com.menacefit.habitude.domain.model.QuestMetric
import java.time.LocalDate
import kotlin.random.Random

/**
 * Rule-based daily quest generation. Quests are re-rolled once per calendar
 * day and are deterministic for a given day + profile seed (same inputs
 * always produce the same quest set), so re-opening the app never reshuffles
 * quests the user has already made progress on until local midnight passes.
 */
object QuestGenerator {
    private const val COMPLETE_HABITS_XP = 30
    private const val FULL_DAY_XP = 100
    private const val HARDEST_HABIT_XP = 40
    private const val EARLY_BIRD_XP = 30
    private const val EARLY_BIRD_HOUR = 9

    data class TodayHabit(val id: String, val difficulty: Difficulty, val category: HabitCategory)

    /** [profileSeed] decorrelates quest rolls between installs/profiles that happen to share a date. */
    fun generate(date: LocalDate, todayHabits: List<TodayHabit>, profileSeed: Long = 0L): List<QuestInstance> {
        if (todayHabits.isEmpty()) return emptyList()
        val random = Random(date.toEpochDay() xor profileSeed)
        val quests = mutableListOf<QuestInstance>()

        val completeCount = when {
            todayHabits.size <= 2 -> todayHabits.size
            todayHabits.size <= 5 -> 3
            else -> 4
        }
        quests += QuestInstance(
            id = questId(date, "complete_n"),
            templateId = "complete_n",
            descriptionKey = "quest_complete_n_habits",
            metric = QuestMetric.HABITS_COMPLETED_COUNT,
            metricParam = completeCount.toString(),
            date = date,
            targetProgress = completeCount,
            currentProgress = 0,
            xpReward = COMPLETE_HABITS_XP,
            completed = false,
        )

        quests += QuestInstance(
            id = questId(date, "full_day"),
            templateId = "full_day",
            descriptionKey = "quest_full_day",
            metric = QuestMetric.FULL_DAY_COMPLETION,
            date = date,
            targetProgress = 1,
            currentProgress = 0,
            xpReward = FULL_DAY_XP,
            completed = false,
        )

        val hardHabits = todayHabits.filter { it.difficulty == Difficulty.HARD }
        if (hardHabits.isNotEmpty()) {
            val chosen = hardHabits[random.nextInt(hardHabits.size)]
            quests += QuestInstance(
                id = questId(date, "hardest"),
                templateId = "hardest",
                descriptionKey = "quest_complete_specific",
                metric = QuestMetric.SPECIFIC_HABIT_COMPLETED,
                metricParam = chosen.id,
                date = date,
                targetProgress = 1,
                currentProgress = 0,
                xpReward = HARDEST_HABIT_XP,
                completed = false,
            )
        }

        if (todayHabits.size >= 2 && random.nextInt(100) < 40) {
            quests += QuestInstance(
                id = questId(date, "early_bird"),
                templateId = "early_bird",
                descriptionKey = "quest_before_hour",
                metric = QuestMetric.COMPLETE_BEFORE_HOUR,
                metricParam = EARLY_BIRD_HOUR.toString(),
                date = date,
                targetProgress = 1,
                currentProgress = 0,
                xpReward = EARLY_BIRD_XP,
                completed = false,
            )
        }

        return quests
    }

    private fun questId(date: LocalDate, templateKey: String) = "${date}_$templateKey"
}
