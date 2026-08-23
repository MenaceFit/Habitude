package com.menacefit.habitude.domain.stats

import java.time.LocalDate

/** The zoom levels offered on every chart in the Statistics screen (spec section 22). */
enum class StatsPeriod {
    WEEK, MONTH, THREE_MONTHS, SIX_MONTHS, YEAR, ALL;

    fun rangeFor(today: LocalDate, earliestDataDate: LocalDate): ClosedRange<LocalDate> {
        val start = when (this) {
            WEEK -> today.minusDays(6)
            MONTH -> today.minusDays(29)
            THREE_MONTHS -> today.minusMonths(3)
            SIX_MONTHS -> today.minusMonths(6)
            YEAR -> today.minusYears(1)
            ALL -> earliestDataDate
        }
        return maxOf(start, earliestDataDate)..today
    }
}
