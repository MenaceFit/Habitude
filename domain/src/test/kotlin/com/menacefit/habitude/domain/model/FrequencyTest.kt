package com.menacefit.habitude.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class FrequencyTest {

    private val start = LocalDate.of(2024, 1, 1) // Monday

    private fun habit(frequency: Frequency, startDate: LocalDate = start, archivedAt: LocalDate? = null, endDate: LocalDate? = null) = Habit(
        id = "h", name = "h", icon = "🏃", colorHex = "#000000", category = HabitCategory.SPORT,
        type = HabitType.BOOLEAN, target = HabitTarget.BooleanTarget, frequency = frequency,
        startDate = startDate, endDate = endDate, archivedAt = archivedAt, createdAt = Instant.EPOCH,
    )

    @Test
    fun `daily is scheduled every day from start`() {
        assertTrue(Frequency.Daily.isScheduledOn(start, start))
        assertTrue(Frequency.Daily.isScheduledOn(start.plusDays(365), start))
    }

    @Test
    fun `daily is never scheduled before the start date`() {
        assertFalse(Frequency.Daily.isScheduledOn(start.minusDays(1), start))
    }

    @Test
    fun `specific days only match the configured iso weekdays`() {
        val weekdaysOnly = Frequency.SpecificDays((1..5).toSet())
        assertTrue(weekdaysOnly.isScheduledOn(LocalDate.of(2024, 1, 1), start)) // Monday
        assertFalse(weekdaysOnly.isScheduledOn(LocalDate.of(2024, 1, 6), start)) // Saturday
        assertFalse(weekdaysOnly.isScheduledOn(LocalDate.of(2024, 1, 7), start)) // Sunday
    }

    @Test
    fun `every-n-days is scheduled only on multiples of n from start`() {
        val everyFive = Frequency.EveryNDays(5)
        assertTrue(everyFive.isScheduledOn(start, start))
        assertTrue(everyFive.isScheduledOn(start.plusDays(5), start))
        assertTrue(everyFive.isScheduledOn(start.plusDays(10), start))
        assertFalse(everyFive.isScheduledOn(start.plusDays(4), start))
        assertFalse(everyFive.isScheduledOn(start.plusDays(6), start))
    }

    @Test
    fun `period-based frequencies are period-based, not day-based`() {
        assertTrue(Frequency.TimesPerWeek(3).isPeriodBased())
        assertTrue(Frequency.TimesPerMonth(2).isPeriodBased())
        assertFalse(Frequency.Daily.isPeriodBased())
        assertFalse(Frequency.SpecificDays(setOf(1)).isPeriodBased())
        assertFalse(Frequency.EveryNDays(2).isPeriodBased())
    }

    @Test
    fun `a period-based habit is due every day within its active window`() {
        val h = habit(Frequency.TimesPerWeek(3))
        assertTrue(h.isDueOn(start))
        assertTrue(h.isDueOn(start.plusDays(1)))
        assertTrue(h.isDueOn(start.plusDays(2)))
    }

    @Test
    fun `a day-scheduled habit is only due on its scheduled weekdays`() {
        val h = habit(Frequency.SpecificDays(setOf(1, 3, 5))) // Mon/Wed/Fri
        assertTrue(h.isDueOn(LocalDate.of(2024, 1, 1))) // Monday
        assertFalse(h.isDueOn(LocalDate.of(2024, 1, 2))) // Tuesday
    }

    @Test
    fun `nothing is due before the start date or after archiving`() {
        val h = habit(Frequency.Daily, startDate = start, archivedAt = start.plusDays(5))
        assertFalse(h.isDueOn(start.minusDays(1)))
        assertTrue(h.isDueOn(start))
        assertTrue(h.isDueOn(start.plusDays(5)))
        assertFalse(h.isDueOn(start.plusDays(6)))
    }
}
