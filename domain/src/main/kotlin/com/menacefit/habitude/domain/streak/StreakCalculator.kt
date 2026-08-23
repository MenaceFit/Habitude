package com.menacefit.habitude.domain.streak

import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.StreakResult
import com.menacefit.habitude.domain.model.isPeriodBased
import com.menacefit.habitude.domain.model.isScheduledOn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Computes streaks and success rates for a single habit.
 *
 * Everything is keyed off [LocalDate] (a timezone-free calendar day), which
 * is what makes this robust across DST transitions, timezone changes and
 * leap years: a completion belongs to the calendar day the user was on when
 * they logged it, full stop — there is no re-interpretation based on an
 * instant + offset.
 */
object StreakCalculator {

    /**
     * @param startDate the habit's start date (no day before this counts).
     * @param effectiveEndDate the habit's archive/end date, or null if still ongoing.
     * @param completedDates calendar days on which the habit was completed.
     * @param freezeDates calendar days protected by a Streak Freeze: they neither break nor themselves count as a "real" completion, but they keep the chain alive.
     * @param today "now", as the caller's local date — always injected, never read from the system clock here, so this stays pure and testable.
     * @param weekStart which day a "week" starts on, for [Frequency.TimesPerWeek] periods (matches the user's start-of-week setting).
     */
    fun compute(
        frequency: Frequency,
        startDate: LocalDate,
        effectiveEndDate: LocalDate?,
        completedDates: Set<LocalDate>,
        freezeDates: Set<LocalDate> = emptySet(),
        today: LocalDate,
        weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ): StreakResult {
        val horizon = listOfNotNull(effectiveEndDate, today).min()
        if (horizon.isBefore(startDate)) {
            return StreakResult(0, 0, 0.0, 0, 0, 0)
        }
        return if (frequency.isPeriodBased()) {
            computePeriodBased(frequency, startDate, horizon, completedDates, freezeDates, today, weekStart)
        } else {
            computeDayBased(frequency, startDate, horizon, completedDates, freezeDates, today)
        }
    }

    private fun computeDayBased(
        frequency: Frequency,
        startDate: LocalDate,
        horizon: LocalDate,
        completedDates: Set<LocalDate>,
        freezeDates: Set<LocalDate>,
        today: LocalDate,
    ): StreakResult {
        var totalScheduled = 0
        var totalCompleted = 0
        var freezesUsed = 0
        var best = 0
        var run = 0

        var d = startDate
        while (!d.isAfter(horizon)) {
            if (frequency.isScheduledOn(d, startDate)) {
                totalScheduled++
                val completedHere = d in completedDates
                val frozenHere = !completedHere && d in freezeDates
                if (completedHere || frozenHere) {
                    if (completedHere) totalCompleted++ else freezesUsed++
                    run++
                    if (run > best) best = run
                } else {
                    run = 0
                }
            }
            d = d.plusDays(1)
        }

        // Current streak: walk backward from "today". If today is scheduled
        // but not yet acted on, that does not break the streak (the day
        // isn't over) — we simply start counting from yesterday instead.
        var current = 0
        var cursor = horizon
        if (cursor == today && frequency.isScheduledOn(cursor, startDate) &&
            cursor !in completedDates && cursor !in freezeDates
        ) {
            cursor = cursor.minusDays(1)
        }
        while (!cursor.isBefore(startDate)) {
            if (frequency.isScheduledOn(cursor, startDate)) {
                if (cursor in completedDates || cursor in freezeDates) {
                    current++
                    cursor = cursor.minusDays(1)
                } else {
                    break
                }
            } else {
                cursor = cursor.minusDays(1)
            }
        }

        val totalMissed = (totalScheduled - totalCompleted - freezesUsed).coerceAtLeast(0)
        val successRate = if (totalScheduled == 0) 0.0 else totalCompleted.toDouble() / totalScheduled
        return StreakResult(
            current = current,
            best = best,
            successRate = successRate,
            totalScheduled = totalScheduled,
            totalCompleted = totalCompleted,
            totalMissed = totalMissed,
        )
    }

    private data class PeriodOutcome(val isCurrent: Boolean, val doneCount: Int, val usableFreezes: Int, val metQuota: Boolean)

    private fun computePeriodBased(
        frequency: Frequency,
        startDate: LocalDate,
        horizon: LocalDate,
        completedDates: Set<LocalDate>,
        freezeDates: Set<LocalDate>,
        today: LocalDate,
        weekStart: DayOfWeek,
    ): StreakResult {
        val quota = when (frequency) {
            is Frequency.TimesPerWeek -> frequency.times
            is Frequency.TimesPerMonth -> frequency.times
            else -> 1
        }.coerceAtLeast(1)

        val periods = buildPeriods(frequency, startDate, horizon, weekStart)
        if (periods.isEmpty()) return StreakResult(0, 0, 0.0, 0, 0, 0)

        val outcomes = periods.map { period ->
            val isCurrent = today in period
            val doneCount = completedDates.count { it in period }
            val usableFreezes = freezeUsableFor(period, freezeDates, doneCount, quota)
            PeriodOutcome(isCurrent, doneCount, usableFreezes, metQuota = (doneCount + usableFreezes) >= quota)
        }

        // Best streak: longest run of quota-met periods. A still-open current
        // period that hasn't met quota yet is simply skipped (not a break).
        var best = 0
        var run = 0
        for (o in outcomes) {
            if (o.isCurrent && !o.metQuota) continue
            if (o.metQuota) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }

        // Current streak: walk backward from the most recent period, with the
        // same "still open" grace as above.
        var current = 0
        for (o in outcomes.asReversed()) {
            if (o.isCurrent && !o.metQuota) continue
            if (o.metQuota) current++ else break
        }

        val totalScheduled = quota * periods.size
        val totalCompleted = outcomes.sumOf { minOf(it.doneCount, quota) }
        val freezesUsed = outcomes.sumOf { it.usableFreezes }
        val totalMissed = (totalScheduled - totalCompleted - freezesUsed).coerceAtLeast(0)
        val successRate = if (totalScheduled == 0) 0.0 else totalCompleted.toDouble() / totalScheduled

        return StreakResult(current, best, successRate, totalScheduled, totalCompleted, totalMissed)
    }

    private fun freezeUsableFor(period: ClosedRange<LocalDate>, freezeDates: Set<LocalDate>, doneInPeriod: Int, quota: Int): Int {
        if (doneInPeriod >= quota) return 0
        val freezesInPeriod = freezeDates.count { it in period }
        return minOf(freezesInPeriod, quota - doneInPeriod)
    }

    private fun buildPeriods(
        frequency: Frequency,
        startDate: LocalDate,
        horizon: LocalDate,
        weekStart: DayOfWeek,
    ): List<ClosedRange<LocalDate>> {
        val periods = mutableListOf<ClosedRange<LocalDate>>()
        when (frequency) {
            is Frequency.TimesPerWeek -> {
                var periodStart = startDate.with(TemporalAdjusters.previousOrSame(weekStart))
                while (!periodStart.isAfter(horizon)) {
                    periods += periodStart..periodStart.plusDays(6)
                    periodStart = periodStart.plusWeeks(1)
                }
            }
            is Frequency.TimesPerMonth -> {
                var periodStart = startDate.withDayOfMonth(1)
                while (!periodStart.isAfter(horizon)) {
                    periods += periodStart..periodStart.withDayOfMonth(periodStart.lengthOfMonth())
                    periodStart = periodStart.plusMonths(1)
                }
            }
            else -> {}
        }
        return periods
    }
}
