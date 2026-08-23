package com.menacefit.habitude.domain.stats

import com.menacefit.habitude.domain.model.DayPerformance
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarDayScorerTest {

    @Test
    fun `no scheduled habits means no data, not a low score`() {
        assertEquals(DayPerformance.NONE, CalendarDayScorer.score(scheduledCount = 0, completedCount = 0))
    }

    @Test
    fun `100 percent completion is a perfect day`() {
        assertEquals(DayPerformance.PERFECT, CalendarDayScorer.score(scheduledCount = 4, completedCount = 4))
    }

    @Test
    fun `exactly half completed is good, not low`() {
        assertEquals(DayPerformance.GOOD, CalendarDayScorer.score(scheduledCount = 4, completedCount = 2))
    }

    @Test
    fun `just under half is low`() {
        assertEquals(DayPerformance.LOW, CalendarDayScorer.score(scheduledCount = 3, completedCount = 1))
    }

    @Test
    fun `zero completed out of scheduled habits is a low day, not no-data`() {
        assertEquals(DayPerformance.LOW, CalendarDayScorer.score(scheduledCount = 3, completedCount = 0))
    }
}
