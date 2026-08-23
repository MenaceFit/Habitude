package com.menacefit.habitude.domain.streak

import com.menacefit.habitude.domain.model.Frequency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    @Test
    fun `daily habit with a missed day in the middle breaks the streak but keeps the best run`() {
        val start = LocalDate.of(2026, 8, 1)
        val today = LocalDate.of(2026, 8, 10)
        val completed = (1..10).map { LocalDate.of(2026, 8, it) }.toSet() - LocalDate.of(2026, 8, 5)

        val result = StreakCalculator.compute(
            frequency = Frequency.Daily,
            startDate = start,
            effectiveEndDate = null,
            completedDates = completed,
            today = today,
        )

        assertEquals(5, result.current) // Aug 6..10
        assertEquals(5, result.best) // ties Aug 1..4 (4) vs Aug 6..10 (5)
        assertEquals(10, result.totalScheduled)
        assertEquals(9, result.totalCompleted)
        assertEquals(1, result.totalMissed)
        assertEquals(0.9, result.successRate, 1e-9)
    }

    @Test
    fun `today not yet completed does not break the streak, it just isn't counted until it's done`() {
        val start = LocalDate.of(2026, 8, 1)
        val today = LocalDate.of(2026, 8, 10)
        val completed = (1..9).map { LocalDate.of(2026, 8, it) }.toSet() // today (10th) missing

        val result = StreakCalculator.compute(
            frequency = Frequency.Daily,
            startDate = start,
            effectiveEndDate = null,
            completedDates = completed,
            today = today,
        )

        assertEquals(9, result.current)
        assertEquals(9, result.best)
        // Stats are literal as-of-now: today counts as scheduled-but-not-done
        // until it's actually completed, even though the streak forgives it.
        assertEquals(10, result.totalScheduled)
        assertEquals(9, result.totalCompleted)
        assertEquals(1, result.totalMissed)
    }

    @Test
    fun `a habit not yet completed on its very first day has a zero streak`() {
        val today = LocalDate.of(2026, 8, 1)
        val result = StreakCalculator.compute(
            frequency = Frequency.Daily,
            startDate = today,
            effectiveEndDate = null,
            completedDates = emptySet(),
            today = today,
        )
        assertEquals(0, result.current)
        assertEquals(0, result.best)
        assertEquals(0, result.totalCompleted)
    }

    @Test
    fun `specific days schedule ignores unscheduled days for both streak and misses`() {
        // 2024-01-01 is a Monday.
        val start = LocalDate.of(2024, 1, 1)
        val today = LocalDate.of(2024, 1, 15) // also a Monday
        val monWedFri = Frequency.SpecificDays(setOf(1, 3, 5))
        // Scheduled days: Jan 1(Mon), 3(Wed), 5(Fri), 8(Mon), 10(Wed), 12(Fri), 15(Mon) = 7 days.
        val completed = setOf(1, 3, 5, 8, 12, 15).map { LocalDate.of(2024, 1, it) }.toSet() // Jan 10 missed

        val result = StreakCalculator.compute(
            frequency = monWedFri,
            startDate = start,
            effectiveEndDate = null,
            completedDates = completed,
            today = today,
        )

        assertEquals(7, result.totalScheduled)
        assertEquals(6, result.totalCompleted)
        assertEquals(1, result.totalMissed)
        assertEquals(4, result.best) // Jan 1,3,5,8
        assertEquals(2, result.current) // Jan 12, 15 (Jan 10 broke it)
    }

    @Test
    fun `every-n-days frequency only counts the anchored days`() {
        val start = LocalDate.of(2024, 1, 1)
        val today = LocalDate.of(2024, 1, 10)
        val everyThreeDays = Frequency.EveryNDays(3)
        // Scheduled: Jan 1, 4, 7, 10.
        val completed = setOf(1, 4, 7, 10).map { LocalDate.of(2024, 1, it) }.toSet()

        val result = StreakCalculator.compute(
            frequency = everyThreeDays,
            startDate = start,
            effectiveEndDate = null,
            completedDates = completed,
            today = today,
        )

        assertEquals(4, result.totalScheduled)
        assertEquals(4, result.current)
        assertEquals(4, result.best)
        assertEquals(1.0, result.successRate, 1e-9)
    }

    @Test
    fun `times-per-week streak counts consecutive quota-met weeks and a not-yet-met current week keeps grace`() {
        // Week 1: Jan 1-7 (Mon start), Week 2: Jan 8-14, Week 3: Jan 15-21, Week 4: Jan 22-28.
        val start = LocalDate.of(2024, 1, 1)
        val today = LocalDate.of(2024, 1, 25) // inside week 4, week still open
        val threeTimesAWeek = Frequency.TimesPerWeek(3)
        val completed = setOf(
            // Week 1: 3 completions -> quota met.
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 5),
            // Week 2: only 2 -> quota missed.
            LocalDate.of(2024, 1, 8), LocalDate.of(2024, 1, 10),
            // Week 3: 3 -> quota met.
            LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 17), LocalDate.of(2024, 1, 19),
            // Week 4 (current, in progress): already 3 -> quota met early.
            LocalDate.of(2024, 1, 22), LocalDate.of(2024, 1, 23), LocalDate.of(2024, 1, 24),
        )

        val result = StreakCalculator.compute(
            frequency = threeTimesAWeek,
            startDate = start,
            effectiveEndDate = null,
            completedDates = completed,
            today = today,
        )

        assertEquals(2, result.current) // weeks 3 and 4
        assertEquals(2, result.best)
        assertEquals(12, result.totalScheduled) // 3 * 4 weeks
        assertEquals(11, result.totalCompleted) // 3+2+3+3
        assertEquals(1, result.totalMissed)
    }

    @Test
    fun `times-per-week current period not yet meeting quota does not zero out the streak`() {
        val start = LocalDate.of(2024, 1, 1)
        val today = LocalDate.of(2024, 1, 25) // week 4, still open, quota not met yet
        val threeTimesAWeek = Frequency.TimesPerWeek(3)
        val completed = setOf(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 5), // week 1 met
            LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 17), LocalDate.of(2024, 1, 19), // week 3 met
            LocalDate.of(2024, 1, 22), // week 4: only 1 so far, not met, but not over either
        )

        val result = StreakCalculator.compute(
            frequency = threeTimesAWeek,
            startDate = start,
            effectiveEndDate = null,
            completedDates = completed,
            today = today,
        )

        assertEquals(1, result.current) // week 3's completed run, week 4 simply not counted yet
        assertEquals(1, result.best)
    }

    @Test
    fun `archiving a habit freezes its streak as of the archive date, ignoring anything after`() {
        val start = LocalDate.of(2024, 1, 1)
        val archivedAt = LocalDate.of(2024, 1, 10)
        val today = LocalDate.of(2024, 1, 20) // long after archiving
        val completed = (1..10).map { LocalDate.of(2024, 1, it) }.toSet()

        val result = StreakCalculator.compute(
            frequency = Frequency.Daily,
            startDate = start,
            effectiveEndDate = archivedAt,
            completedDates = completed,
            today = today,
        )

        assertEquals(10, result.current)
        assertEquals(10, result.best)
        assertEquals(10, result.totalScheduled)
    }

    @Test
    fun `a streak freeze keeps the chain alive without counting as a genuine completion`() {
        val start = LocalDate.of(2024, 1, 1)
        val today = LocalDate.of(2024, 1, 5)
        val completed = setOf(1, 2, 4, 5).map { LocalDate.of(2024, 1, it) }.toSet() // Jan 3 missed
        val freezes = setOf(LocalDate.of(2024, 1, 3))

        val result = StreakCalculator.compute(
            frequency = Frequency.Daily,
            startDate = start,
            effectiveEndDate = null,
            completedDates = completed,
            freezeDates = freezes,
            today = today,
        )

        assertEquals(5, result.current) // chain never broke
        assertEquals(5, result.best)
        assertEquals(4, result.totalCompleted) // freeze isn't a "real" completion
        assertEquals(0, result.totalMissed) // but it does absorb the miss
        assertEquals(0.8, result.successRate, 1e-9)
    }

    @Test
    fun `leap day is handled like any other calendar day`() {
        val start = LocalDate.of(2024, 2, 28)
        val today = LocalDate.of(2024, 3, 1)
        val completed = setOf(
            LocalDate.of(2024, 2, 28), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 1),
        )

        val result = StreakCalculator.compute(
            frequency = Frequency.Daily,
            startDate = start,
            effectiveEndDate = null,
            completedDates = completed,
            today = today,
        )

        assertEquals(3, result.current)
        assertEquals(3, result.totalScheduled)
        assertEquals(1.0, result.successRate, 1e-9)
    }
}
