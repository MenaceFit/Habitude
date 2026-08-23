package com.menacefit.habitude.domain.model

/**
 * Single source of truth for "does this recorded progress count as done".
 * Both the UI (to decide when to fire the completion animation) and the
 * statistics engine call through here so the rule can never drift between
 * the two.
 *
 * @param value free-form progress amount whose meaning depends on the
 *   target: an accumulated quantity/duration/count, or — for [HabitTarget.TimeTarget] —
 *   minutes since midnight of the moment the action was logged.
 * @param checkedIds ids of checked [ChecklistItem]s, only meaningful for [HabitTarget.ChecklistTarget].
 */
fun HabitTarget.isSatisfiedBy(value: Double, checkedIds: Set<String> = emptySet()): Boolean = when (this) {
    is HabitTarget.BooleanTarget -> value >= 1.0
    is HabitTarget.QuantityTarget -> value >= targetAmount
    is HabitTarget.DurationTarget -> value >= targetMinutes
    is HabitTarget.CountTarget -> value >= targetCount
    is HabitTarget.LimitTarget -> value <= maxAmount
    is HabitTarget.TimeTarget -> {
        val targetMinutes = hour * 60 + minute
        if (isBefore) value <= targetMinutes else value >= targetMinutes
    }
    is HabitTarget.ChecklistTarget -> items.isNotEmpty() && items.all { it.id in checkedIds }
}

/** Progress ratio in [0, 1], used to render progress bars/rings for partially-completed habits. */
fun HabitTarget.progressRatio(value: Double, checkedIds: Set<String> = emptySet()): Float = when (this) {
    is HabitTarget.BooleanTarget -> if (value >= 1.0) 1f else 0f
    is HabitTarget.QuantityTarget -> if (targetAmount <= 0) 0f else (value / targetAmount).toFloat().coerceIn(0f, 1f)
    is HabitTarget.DurationTarget -> if (targetMinutes <= 0) 0f else (value / targetMinutes).toFloat().coerceIn(0f, 1f)
    is HabitTarget.CountTarget -> if (targetCount <= 0) 0f else (value / targetCount).toFloat().coerceIn(0f, 1f)
    is HabitTarget.LimitTarget -> if (maxAmount <= 0) 0f else (1f - (value / maxAmount).toFloat()).coerceIn(0f, 1f)
    is HabitTarget.TimeTarget -> if (isSatisfiedBy(value)) 1f else 0f
    is HabitTarget.ChecklistTarget -> if (items.isEmpty()) 0f else checkedIds.count { id -> items.any { it.id == id } }
        .toFloat() / items.size
}
