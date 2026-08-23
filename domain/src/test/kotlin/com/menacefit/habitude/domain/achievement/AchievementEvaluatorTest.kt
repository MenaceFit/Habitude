package com.menacefit.habitude.domain.achievement

import com.menacefit.habitude.domain.model.AchievementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEvaluatorTest {

    @Test
    fun `nothing unlocks from an all-default snapshot`() {
        val unlocked = AchievementEvaluator.evaluate(AchievementStatsSnapshot(), alreadyUnlocked = emptySet())
        assertTrue(unlocked.isEmpty())
    }

    @Test
    fun `first completion unlocks exactly the first-day badge`() {
        val unlocked = AchievementEvaluator.evaluate(
            AchievementStatsSnapshot(isFirstEverCompletion = true),
            alreadyUnlocked = emptySet(),
        )
        assertEquals(listOf(AchievementType.FIRST_DAY), unlocked)
    }

    @Test
    fun `a 7-day streak unlocks the 7-day badge but not the 30 or 100 day ones`() {
        val unlocked = AchievementEvaluator.evaluate(
            AchievementStatsSnapshot(bestStreakAcrossHabits = 7),
            alreadyUnlocked = emptySet(),
        )
        assertTrue(AchievementType.STREAK_7 in unlocked)
        assertFalse(AchievementType.STREAK_30 in unlocked)
        assertFalse(AchievementType.STREAK_100 in unlocked)
    }

    @Test
    fun `a 100-day streak unlocks all three streak badges at once on first evaluation`() {
        val unlocked = AchievementEvaluator.evaluate(
            AchievementStatsSnapshot(bestStreakAcrossHabits = 100),
            alreadyUnlocked = emptySet(),
        )
        assertTrue(AchievementType.STREAK_7 in unlocked)
        assertTrue(AchievementType.STREAK_30 in unlocked)
        assertTrue(AchievementType.STREAK_100 in unlocked)
    }

    @Test
    fun `already-unlocked achievements are never returned again`() {
        val unlocked = AchievementEvaluator.evaluate(
            AchievementStatsSnapshot(bestStreakAcrossHabits = 100),
            alreadyUnlocked = setOf(AchievementType.STREAK_7, AchievementType.STREAK_30, AchievementType.STREAK_100),
        )
        assertTrue(unlocked.isEmpty())
    }

    @Test
    fun `perfect day count graduates into perfect week and month badges`() {
        val sevenDays = AchievementEvaluator.evaluate(
            AchievementStatsSnapshot(consecutivePerfectDays = 7),
            alreadyUnlocked = setOf(AchievementType.PERFECT_DAY),
        )
        assertEquals(listOf(AchievementType.PERFECT_WEEK), sevenDays)

        val thirtyDays = AchievementEvaluator.evaluate(
            AchievementStatsSnapshot(consecutivePerfectDays = 30),
            alreadyUnlocked = setOf(AchievementType.PERFECT_DAY, AchievementType.PERFECT_WEEK),
        )
        assertEquals(listOf(AchievementType.PERFECT_MONTH), thirtyDays)
    }

    @Test
    fun `catalog has a definition for every achievement type with no duplicates`() {
        val types = AchievementCatalog.all.map { it.type }
        assertEquals(AchievementType.entries.toSet(), types.toSet())
        assertEquals(types.size, types.toSet().size)
    }
}
