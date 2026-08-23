@file:UseSerializers(LocalDateIsoSerializer::class, InstantIsoSerializer::class)

package com.menacefit.habitude.domain.model

import com.menacefit.habitude.domain.serialization.InstantIsoSerializer
import com.menacefit.habitude.domain.serialization.LocalDateIsoSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.time.LocalDate

data class LevelInfo(
    val level: Int,
    val totalXp: Int,
    val xpIntoLevel: Int,
    val xpForThisLevel: Int,
    val progress: Float,
)

data class StreakResult(
    val current: Int,
    val best: Int,
    /** Fraction in [0,1] of eligible (scheduled, already-elapsed) days/periods that were completed. */
    val successRate: Double,
    val totalScheduled: Int,
    val totalCompleted: Int,
    val totalMissed: Int,
)

@Serializable
data class UnlockedAchievement(
    val type: AchievementType,
    val unlockedAt: Instant,
)

@Serializable
data class QuestInstance(
    val id: String,
    val templateId: String,
    val descriptionKey: String,
    val metric: QuestMetric,
    /** Extra context the metric needs: a habit id for [QuestMetric.SPECIFIC_HABIT_COMPLETED], an hour-of-day for [QuestMetric.COMPLETE_BEFORE_HOUR], a category name for [QuestMetric.CATEGORY_HABIT_COMPLETED]. */
    val metricParam: String? = null,
    val date: LocalDate,
    val targetProgress: Int,
    val currentProgress: Int,
    val xpReward: Int,
    val completed: Boolean,
)

@Serializable
data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val startDate: LocalDate,
    val durationDays: Int,
    /** Optional: tie the challenge to a specific habit's daily completion; null means "manual check-in" challenges. */
    val linkedHabitId: String? = null,
    val xpReward: Int,
    val status: ChallengeStatus,
    /** Calendar dates (within the challenge window) the user has checked off. */
    val completedDates: Set<LocalDate> = emptySet(),
) {
    val endDateExclusive: LocalDate get() = startDate.plusDays(durationDays.toLong())
    val progressRatio: Float get() = if (durationDays <= 0) 0f else (completedDates.size.toFloat() / durationDays).coerceIn(0f, 1f)
}

data class ConsistencyBreakdown(
    val overall: Int,
    val successRate: Int,
    val streakStrength: Int,
    val perfectDaysRatio: Int,
    val goalCompletion: Int,
    val frequencyAdherence: Int,
)
