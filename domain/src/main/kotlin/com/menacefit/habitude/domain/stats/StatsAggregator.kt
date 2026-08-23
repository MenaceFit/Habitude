package com.menacefit.habitude.domain.stats

import com.menacefit.habitude.domain.model.DailyStatsSnapshot
import com.menacefit.habitude.domain.model.DayPerformance
import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.isPeriodBased
import com.menacefit.habitude.domain.model.isScheduledOn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class HabitStatEntry(
    val habitId: String,
    val successRate: Double,
    val completedCount: Int,
    val scheduledCount: Int,
)

/**
 * Cross-habit and time-series aggregation for the Statistics screen. Two
 * different data shapes feed it on purpose:
 *  - per-habit / per-category breakdowns need habit-level granularity, so
 *    they take the raw habit list + completion dates directly;
 *  - whole-app time series (daily/weekly/monthly progression charts, "best
 *    day of week") read from precomputed [DailyStatsSnapshot] rows instead
 *    of ever re-scanning raw completions, per the app's performance budget.
 */
object StatsAggregator {

    fun successRateByHabit(
        habits: List<Habit>,
        completionDatesByHabit: Map<String, Set<LocalDate>>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<HabitStatEntry> = habits.map { habit ->
        val completedDates = completionDatesByHabit[habit.id].orEmpty()
        val clampedStart = maxOf(habit.startDate, rangeStart)
        val clampedEnd = habit.effectiveEndDate?.let { minOf(it, rangeEnd) } ?: rangeEnd
        if (clampedEnd.isBefore(clampedStart)) {
            HabitStatEntry(habit.id, 0.0, 0, 0)
        } else if (habit.frequency.isPeriodBased()) {
            periodBasedStat(habit, completedDates, clampedStart, clampedEnd)
        } else {
            dayBasedStat(habit, completedDates, clampedStart, clampedEnd)
        }
    }

    private fun dayBasedStat(habit: Habit, completedDates: Set<LocalDate>, start: LocalDate, end: LocalDate): HabitStatEntry {
        var scheduled = 0
        var completed = 0
        var d = start
        while (!d.isAfter(end)) {
            if (habit.frequency.isScheduledOn(d, habit.startDate)) {
                scheduled++
                if (d in completedDates) completed++
            }
            d = d.plusDays(1)
        }
        val rate = if (scheduled == 0) 0.0 else completed.toDouble() / scheduled
        return HabitStatEntry(habit.id, rate, completed, scheduled)
    }

    /** Period-quota frequencies don't have a fixed daily schedule, so the scheduled count is approximated as quota × whole periods covered by the range. */
    private fun periodBasedStat(habit: Habit, completedDates: Set<LocalDate>, start: LocalDate, end: LocalDate): HabitStatEntry {
        val quota = when (val f = habit.frequency) {
            is Frequency.TimesPerWeek -> f.times
            is Frequency.TimesPerMonth -> f.times
            else -> 1
        }.coerceAtLeast(1)
        val periodLengthDays = if (habit.frequency is Frequency.TimesPerMonth) 30 else 7
        val daysInRange = ChronoUnit.DAYS.between(start, end) + 1
        val periodsInRange = (daysInRange / periodLengthDays).toInt().coerceAtLeast(1)
        val scheduled = quota * periodsInRange
        val completed = completedDates.count { !it.isBefore(start) && !it.isAfter(end) }.coerceAtMost(scheduled)
        val rate = if (scheduled == 0) 0.0 else completed.toDouble() / scheduled
        return HabitStatEntry(habit.id, rate, completed, scheduled)
    }

    fun categoryComparison(
        habits: List<Habit>,
        completionDatesByHabit: Map<String, Set<LocalDate>>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): Map<HabitCategory, Double> {
        val entries = successRateByHabit(habits, completionDatesByHabit, rangeStart, rangeEnd).associateBy { it.habitId }
        return habits.groupBy { it.category }.mapValues { (_, habitsInCategory) ->
            val rates = habitsInCategory.mapNotNull { entries[it.id] }.filter { it.scheduledCount > 0 }
            if (rates.isEmpty()) 0.0 else rates.map { it.successRate }.average()
        }
    }

    /** Highest-average-completion-rate weekday across [snapshots], or null if there isn't enough data yet. */
    fun bestDayOfWeek(snapshots: List<DailyStatsSnapshot>): DayOfWeek? {
        val withData = snapshots.filter { it.scheduledCount > 0 }
        if (withData.isEmpty()) return null
        return withData.groupBy { it.date.dayOfWeek }
            .mapValues { (_, days) -> days.map { it.completionRate }.average() }
            .maxByOrNull { it.value }
            ?.key
    }

    /** Percentage change of the average completion rate between two equal-length periods, or null if [previous] has no data. */
    fun periodOverPeriodChangePercent(current: List<DailyStatsSnapshot>, previous: List<DailyStatsSnapshot>): Int? {
        val currentWithData = current.filter { it.scheduledCount > 0 }
        val previousWithData = previous.filter { it.scheduledCount > 0 }
        if (currentWithData.isEmpty() || previousWithData.isEmpty()) return null
        val currentAvg = currentWithData.map { it.completionRate }.average()
        val previousAvg = previousWithData.map { it.completionRate }.average()
        if (previousAvg <= 0.0) return null
        return (((currentAvg - previousAvg) / previousAvg) * 100).toInt()
    }

    fun averageCompletionRate(snapshots: List<DailyStatsSnapshot>): Double {
        val withData = snapshots.filter { it.scheduledCount > 0 }
        return if (withData.isEmpty()) 0.0 else withData.map { it.completionRate.toDouble() }.average()
    }

    fun perfectDaysCount(snapshots: List<DailyStatsSnapshot>): Int =
        snapshots.count { it.performance == DayPerformance.PERFECT }
}
