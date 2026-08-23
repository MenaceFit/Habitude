@file:UseSerializers(LocalDateIsoSerializer::class)

package com.menacefit.habitude.domain.model

import com.menacefit.habitude.domain.serialization.LocalDateIsoSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate

/** One row per calendar day: a precomputed rollup so the dashboard/calendar/heatmap never recompute from raw completions on every frame. */
@Serializable
data class DailyStatsSnapshot(
    val date: LocalDate,
    val scheduledCount: Int,
    val completedCount: Int,
    val completionRate: Float,
    val xpEarned: Int,
    val performance: DayPerformance,
)

@Serializable
data class MoodEntry(
    val id: String,
    val date: LocalDate,
    val mood: MoodLevel,
    val energy: Int,
    val note: String = "",
)

@Serializable
data class JournalEntry(
    val id: String,
    val date: LocalDate,
    val text: String,
)

@Serializable
data class Goal(
    val id: String,
    val title: String,
    val icon: String = "🎯",
    val targetValue: Int,
    val currentValue: Int,
    val startDate: LocalDate,
    val deadline: LocalDate? = null,
    val achieved: Boolean = false,
) {
    val progressRatio: Float get() = if (targetValue <= 0) 0f else (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
}
