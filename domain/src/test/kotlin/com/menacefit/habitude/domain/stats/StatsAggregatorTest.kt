package com.menacefit.habitude.domain.stats

import com.menacefit.habitude.domain.model.DailyStatsSnapshot
import com.menacefit.habitude.domain.model.DayPerformance
import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.HabitTarget
import com.menacefit.habitude.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class StatsAggregatorTest {

    private fun habit(
        id: String,
        category: HabitCategory = HabitCategory.SPORT,
        frequency: Frequency = Frequency.Daily,
        startDate: LocalDate = LocalDate.of(2024, 1, 1),
    ) = Habit(
        id = id,
        name = id,
        icon = "🏃",
        colorHex = "#000000",
        category = category,
        type = HabitType.BOOLEAN,
        target = HabitTarget.BooleanTarget,
        frequency = frequency,
        difficulty = Difficulty.MEDIUM,
        startDate = startDate,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `success rate by habit counts only scheduled days within the requested range`() {
        val h = habit("h1", startDate = LocalDate.of(2024, 1, 1))
        val completions = mapOf("h1" to setOf(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 4)))

        val entries = StatsAggregator.successRateByHabit(
            habits = listOf(h),
            completionDatesByHabit = completions,
            rangeStart = LocalDate.of(2024, 1, 1),
            rangeEnd = LocalDate.of(2024, 1, 5),
        )

        assertEquals(1, entries.size)
        assertEquals(5, entries[0].scheduledCount)
        assertEquals(2, entries[0].completedCount)
        assertEquals(0.4, entries[0].successRate, 1e-9)
    }

    @Test
    fun `category comparison averages success rate across habits sharing a category`() {
        val sport1 = habit("s1", category = HabitCategory.SPORT)
        val sport2 = habit("s2", category = HabitCategory.SPORT)
        val range = LocalDate.of(2024, 1, 1)..LocalDate.of(2024, 1, 10)
        val completions = mapOf(
            "s1" to (1..10).map { LocalDate.of(2024, 1, it) }.toSet(), // 100%
            "s2" to setOf(LocalDate.of(2024, 1, 1)), // 10%
        )

        val comparison = StatsAggregator.categoryComparison(
            habits = listOf(sport1, sport2),
            completionDatesByHabit = completions,
            rangeStart = range.start,
            rangeEnd = range.endInclusive,
        )

        assertEquals(0.55, comparison.getValue(HabitCategory.SPORT), 1e-9)
    }

    @Test
    fun `best day of week picks the highest average completion rate weekday`() {
        val snapshots = listOf(
            snapshot(LocalDate.of(2024, 1, 1), rate = 1.0f), // Monday
            snapshot(LocalDate.of(2024, 1, 8), rate = 1.0f), // Monday
            snapshot(LocalDate.of(2024, 1, 2), rate = 0.2f), // Tuesday
            snapshot(LocalDate.of(2024, 1, 9), rate = 0.3f), // Tuesday
        )
        assertEquals(DayOfWeek.MONDAY, StatsAggregator.bestDayOfWeek(snapshots))
    }

    @Test
    fun `best day of week is null without enough data`() {
        assertNull(StatsAggregator.bestDayOfWeek(emptyList()))
        assertNull(StatsAggregator.bestDayOfWeek(listOf(snapshot(LocalDate.of(2024, 1, 1), rate = 1f, scheduled = 0))))
    }

    @Test
    fun `period over period change percent reflects genuine improvement`() {
        val previous = (1..7).map { snapshot(LocalDate.of(2024, 1, it), rate = 0.5f) }
        val current = (8..14).map { snapshot(LocalDate.of(2024, 1, it), rate = 0.75f) }
        val change = StatsAggregator.periodOverPeriodChangePercent(current, previous)
        assertEquals(50, change) // 0.75 is 50% higher than 0.5
    }

    @Test
    fun `period over period change is null when the previous period has no data`() {
        val current = (8..14).map { snapshot(LocalDate.of(2024, 1, it), rate = 0.75f) }
        assertNull(StatsAggregator.periodOverPeriodChangePercent(current, previous = emptyList()))
    }

    @Test
    fun `perfect days count only counts PERFECT performance days`() {
        val snapshots = listOf(
            snapshot(LocalDate.of(2024, 1, 1), rate = 1.0f, performance = DayPerformance.PERFECT),
            snapshot(LocalDate.of(2024, 1, 2), rate = 0.6f, performance = DayPerformance.GOOD),
            snapshot(LocalDate.of(2024, 1, 3), rate = 1.0f, performance = DayPerformance.PERFECT),
        )
        assertEquals(2, StatsAggregator.perfectDaysCount(snapshots))
    }

    private fun snapshot(
        date: LocalDate,
        rate: Float,
        scheduled: Int = 4,
        performance: DayPerformance = CalendarDayScorer.score(scheduled, (scheduled * rate).toInt()),
    ) = DailyStatsSnapshot(
        date = date,
        scheduledCount = scheduled,
        completedCount = (scheduled * rate).toInt(),
        completionRate = rate,
        xpEarned = 0,
        performance = performance,
    )
}
