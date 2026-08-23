package com.menacefit.habitude.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsistencyScoreCalculatorTest {

    @Test
    fun `perfect inputs across the board yield a perfect score`() {
        val breakdown = ConsistencyScoreCalculator.compute(
            ConsistencyInputs(
                successRate30d = 1.0,
                currentStreakDays = 60,
                bestStreakEver = 60,
                perfectDaysRatio30d = 1.0,
                goalCompletionRate = 1.0,
                frequencyAdherence = 1.0,
            ),
        )
        assertEquals(100, breakdown.overall)
    }

    @Test
    fun `all-zero inputs yield a zero score, not a crash`() {
        val breakdown = ConsistencyScoreCalculator.compute(
            ConsistencyInputs(
                successRate30d = 0.0,
                currentStreakDays = 0,
                bestStreakEver = 0,
                perfectDaysRatio30d = 0.0,
                goalCompletionRate = 0.0,
                frequencyAdherence = 0.0,
            ),
        )
        assertEquals(0, breakdown.overall)
    }

    @Test
    fun `score is always within the documented 0 to 100 bounds, even for out-of-range inputs`() {
        val breakdown = ConsistencyScoreCalculator.compute(
            ConsistencyInputs(
                successRate30d = 5.0, // malformed caller input
                currentStreakDays = -10,
                bestStreakEver = -1,
                perfectDaysRatio30d = -2.0,
                goalCompletionRate = 3.0,
                frequencyAdherence = 1.5,
            ),
        )
        assertTrue(breakdown.overall in 0..100)
    }

    @Test
    fun `a current streak matching the personal best scores higher than an equal-length streak far below it`() {
        val atPersonalBest = ConsistencyScoreCalculator.compute(
            ConsistencyInputs(0.5, currentStreakDays = 10, bestStreakEver = 10, perfectDaysRatio30d = 0.5, goalCompletionRate = 0.5, frequencyAdherence = 0.5),
        )
        val farBelowBest = ConsistencyScoreCalculator.compute(
            ConsistencyInputs(0.5, currentStreakDays = 10, bestStreakEver = 200, perfectDaysRatio30d = 0.5, goalCompletionRate = 0.5, frequencyAdherence = 0.5),
        )
        assertTrue(atPersonalBest.streakStrength > farBelowBest.streakStrength)
        assertTrue(atPersonalBest.overall > farBelowBest.overall)
    }

    @Test
    fun `higher success rate strictly increases the overall score, all else equal`() {
        val low = ConsistencyScoreCalculator.compute(
            ConsistencyInputs(successRate30d = 0.2, currentStreakDays = 5, bestStreakEver = 5, perfectDaysRatio30d = 0.2, goalCompletionRate = 0.2, frequencyAdherence = 0.2),
        )
        val high = ConsistencyScoreCalculator.compute(
            ConsistencyInputs(successRate30d = 0.9, currentStreakDays = 5, bestStreakEver = 5, perfectDaysRatio30d = 0.2, goalCompletionRate = 0.2, frequencyAdherence = 0.2),
        )
        assertTrue(high.overall > low.overall)
    }
}
