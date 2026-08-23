package com.menacefit.habitude.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * How often a habit is expected to be performed.
 *
 * Design note: the streak/statistics engines always interpret the *entire*
 * history of a habit through its *current* frequency, rather than keeping a
 * versioned log of historical frequency changes. This mirrors how most
 * mainstream habit trackers behave and keeps the model tractable; it means
 * that changing a habit's frequency reshapes how past days are judged
 * (a day that was "unscheduled" can retroactively become "scheduled" and
 * vice-versa) instead of silently freezing history. This is a deliberate
 * trade-off, not an oversight.
 */
@Serializable
sealed class Frequency {

    @Serializable
    @SerialName("daily")
    data object Daily : Frequency()

    /** [isoDays] uses ISO-8601 day numbering: Monday=1 ... Sunday=7. */
    @Serializable
    @SerialName("specific_days")
    data class SpecificDays(val isoDays: Set<Int>) : Frequency()

    @Serializable
    @SerialName("times_per_week")
    data class TimesPerWeek(val times: Int) : Frequency()

    @Serializable
    @SerialName("times_per_month")
    data class TimesPerMonth(val times: Int) : Frequency()

    /** Every [n] days starting from the habit's start date (n=1 is equivalent to Daily). */
    @Serializable
    @SerialName("every_n_days")
    data class EveryNDays(val n: Int) : Frequency()

    companion object {
        fun everyDayOfWeek(): Set<Int> = (1..7).toSet()
    }
}

val DayOfWeek.iso: Int get() = this.value

/** Whether [date] is a day this habit is expected to be acted on, given it started on [startDate]. */
fun Frequency.isScheduledOn(date: LocalDate, startDate: LocalDate): Boolean {
    if (date.isBefore(startDate)) return false
    return when (this) {
        is Frequency.Daily -> true
        is Frequency.SpecificDays -> date.dayOfWeek.iso in isoDays
        is Frequency.EveryNDays -> {
            val n = n.coerceAtLeast(1)
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
            daysBetween % n == 0L
        }
        // Period-quota frequencies are not evaluated "per day": any day in an
        // active period is a candidate day. The quota is checked per-period
        // by StreakCalculator / StatsAggregator instead.
        is Frequency.TimesPerWeek -> true
        is Frequency.TimesPerMonth -> true
    }
}

/** True for the two frequency kinds whose completion rule is "N times within a period", not "every scheduled day". */
fun Frequency.isPeriodBased(): Boolean = this is Frequency.TimesPerWeek || this is Frequency.TimesPerMonth

/**
 * Whether [habit] should be shown as actionable on [date]: within its
 * active window (started, not archived/ended), and — for day-scheduled
 * frequencies — actually due that day. Period-quota habits (e.g. "3x/week")
 * are considered due every day within their active window, since the user
 * can work toward the quota on any day of the period; the ring for a single
 * day only reflects whether *that day's* action was logged.
 */
fun Habit.isDueOn(date: LocalDate): Boolean {
    if (date.isBefore(startDate)) return false
    val end = effectiveEndDate
    if (end != null && date.isAfter(end)) return false
    return if (frequency.isPeriodBased()) true else frequency.isScheduledOn(date, startDate)
}
