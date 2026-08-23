package com.menacefit.habitude.domain.stats

import com.menacefit.habitude.domain.model.DayPerformance

/** Buckets a single day's completion ratio into the four states the calendar/heatmap render as colors. */
object CalendarDayScorer {
    fun score(scheduledCount: Int, completedCount: Int): DayPerformance {
        if (scheduledCount <= 0) return DayPerformance.NONE
        val rate = completedCount.toDouble() / scheduledCount
        return when {
            rate >= 1.0 -> DayPerformance.PERFECT
            rate >= 0.5 -> DayPerformance.GOOD
            else -> DayPerformance.LOW
        }
    }
}
