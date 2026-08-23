package com.menacefit.habitude.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitTargetEvaluationTest {

    @Test
    fun `boolean target is satisfied only at value 1 or more`() {
        assertFalse(HabitTarget.BooleanTarget.isSatisfiedBy(0.0))
        assertTrue(HabitTarget.BooleanTarget.isSatisfiedBy(1.0))
    }

    @Test
    fun `quantity target is satisfied at or above the amount`() {
        val target = HabitTarget.QuantityTarget(targetAmount = 2.0, unit = "L")
        assertFalse(target.isSatisfiedBy(1.9))
        assertTrue(target.isSatisfiedBy(2.0))
        assertTrue(target.isSatisfiedBy(2.5))
        assertEquals(0.6f, target.progressRatio(1.2), 1e-4f)
    }

    @Test
    fun `limit target is satisfied by staying at or under the max`() {
        val limit = HabitTarget.LimitTarget(maxAmount = 120.0, unit = "min")
        assertTrue(limit.isSatisfiedBy(0.0))
        assertTrue(limit.isSatisfiedBy(120.0))
        assertFalse(limit.isSatisfiedBy(121.0))
    }

    @Test
    fun `limit target progress ratio decreases as usage approaches the max`() {
        val limit = HabitTarget.LimitTarget(maxAmount = 100.0, unit = "min")
        assertEquals(1f, limit.progressRatio(0.0), 1e-4f)
        assertEquals(0.5f, limit.progressRatio(50.0), 1e-4f)
        assertEquals(0f, limit.progressRatio(100.0), 1e-4f)
    }

    @Test
    fun `time target before works as a deadline and after works as a floor`() {
        val bedtime = HabitTarget.TimeTarget(hour = 23, minute = 0, isBefore = true) // 23:00 = 1380 min
        assertTrue(bedtime.isSatisfiedBy(1350.0)) // 22:30
        assertFalse(bedtime.isSatisfiedBy(1400.0)) // 23:20

        val wakeUp = HabitTarget.TimeTarget(hour = 7, minute = 0, isBefore = false) // 07:00 = 420 min, "at/after"
        assertFalse(wakeUp.isSatisfiedBy(400.0)) // woke up before 7am -> doesn't satisfy an "after" target
        assertTrue(wakeUp.isSatisfiedBy(420.0))
    }

    @Test
    fun `checklist target requires every item checked`() {
        val items = listOf(ChecklistItem("1", "Water"), ChecklistItem("2", "Bed"))
        val target = HabitTarget.ChecklistTarget(items)
        assertFalse(target.isSatisfiedBy(0.0, checkedIds = setOf("1")))
        assertTrue(target.isSatisfiedBy(0.0, checkedIds = setOf("1", "2")))
        assertEquals(0.5f, target.progressRatio(0.0, checkedIds = setOf("1")), 1e-4f)
    }

    @Test
    fun `empty checklist is never satisfied to avoid a free completion`() {
        val target = HabitTarget.ChecklistTarget(emptyList())
        assertFalse(target.isSatisfiedBy(0.0, checkedIds = emptySet()))
    }

    @Test
    fun `count and duration targets round-trip the same way as quantity`() {
        val count = HabitTarget.CountTarget(targetCount = 50, unit = "reps")
        assertFalse(count.isSatisfiedBy(49.0))
        assertTrue(count.isSatisfiedBy(50.0))

        val duration = HabitTarget.DurationTarget(targetMinutes = 20)
        assertFalse(duration.isSatisfiedBy(19.0))
        assertTrue(duration.isSatisfiedBy(20.0))
    }
}
