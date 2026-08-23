@file:UseSerializers(LocalDateIsoSerializer::class, InstantIsoSerializer::class)

package com.menacefit.habitude.domain.model

import com.menacefit.habitude.domain.serialization.InstantIsoSerializer
import com.menacefit.habitude.domain.serialization.LocalDateIsoSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.time.LocalDate

@Serializable
data class ChecklistItem(
    val id: String,
    val label: String,
)

/**
 * Type-specific "what does done mean" configuration for a habit. Kept
 * separate from [HabitType] so the UI can pattern-match exhaustively while
 * persistence only needs to serialize one polymorphic field.
 */
@Serializable
sealed class HabitTarget {
    @Serializable
    @SerialName("boolean")
    data object BooleanTarget : HabitTarget()

    @Serializable
    @SerialName("quantity")
    data class QuantityTarget(val targetAmount: Double, val unit: String) : HabitTarget()

    @Serializable
    @SerialName("duration")
    data class DurationTarget(val targetMinutes: Int) : HabitTarget()

    @Serializable
    @SerialName("count")
    data class CountTarget(val targetCount: Int, val unit: String = "") : HabitTarget()

    @Serializable
    @SerialName("limit")
    data class LimitTarget(val maxAmount: Double, val unit: String) : HabitTarget()

    /** [hour]/[minute] is a 24h clock; [isBefore]=false means "at or after" (e.g. wake-up time). */
    @Serializable
    @SerialName("time")
    data class TimeTarget(val hour: Int, val minute: Int, val isBefore: Boolean = true) : HabitTarget()

    @Serializable
    @SerialName("checklist")
    data class ChecklistTarget(val items: List<ChecklistItem>) : HabitTarget()
}

@Serializable
data class Habit(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String,
    val colorHex: String,
    val category: HabitCategory,
    val type: HabitType,
    val target: HabitTarget,
    val frequency: Frequency,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val sortOrder: Int = 0,
    val active: Boolean = true,
    val archivedAt: LocalDate? = null,
    val createdAt: Instant,
) {
    /** The day a habit stops counting for scheduling/streak purposes: its archive date, its explicit end date, or "still ongoing". */
    val effectiveEndDate: LocalDate?
        get() = listOfNotNull(archivedAt, endDate).minOrNull()
}

@Serializable
data class HabitCompletion(
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val completed: Boolean,
    /** Progress value for QUANTITY/DURATION/COUNT/LIMIT types (units defined by the habit's [HabitTarget]). */
    val value: Double = 0.0,
    /** Checked item ids for CHECKLIST-type habits. */
    val checklistCheckedIds: Set<String> = emptySet(),
    val timestamp: Instant,
)
