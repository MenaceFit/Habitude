package com.menacefit.habitude.domain.xp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCurveTest {

    @Test
    fun `thresholds match the spec's worked examples exactly`() {
        assertEquals(0, LevelCurve.xpThresholdForLevel(1))
        assertEquals(100, LevelCurve.xpThresholdForLevel(2))
        assertEquals(250, LevelCurve.xpThresholdForLevel(3))
        assertEquals(450, LevelCurve.xpThresholdForLevel(4))
    }

    @Test
    fun `zero xp is level 1 with zero progress into the level`() {
        val info = LevelCurve.levelForTotalXp(0)
        assertEquals(1, info.level)
        assertEquals(0, info.xpIntoLevel)
        assertEquals(100, info.xpForThisLevel)
        assertEquals(0f, info.progress, 1e-6f)
    }

    @Test
    fun `xp exactly at a threshold lands on the new level, not the old one`() {
        val info = LevelCurve.levelForTotalXp(100)
        assertEquals(2, info.level)
        assertEquals(0, info.xpIntoLevel)
    }

    @Test
    fun `partial progress into a level is reported relative to that level's own floor and ceiling`() {
        // The spec's worked mock-up numbers ("Niveau 12, 1240 / 1500 XP") are
        // illustrative UI copy, not a literal continuation of the level-1..4
        // formula it also specifies — this test instead checks the general
        // invariant that must hold at *any* level: xpIntoLevel + floor ==
        // totalXp, and xpForThisLevel == ceiling - floor.
        val level = 12
        val floor = LevelCurve.xpThresholdForLevel(level)
        val ceiling = LevelCurve.xpThresholdForLevel(level + 1)
        val totalXp = floor + (ceiling - floor) / 2

        val info = LevelCurve.levelForTotalXp(totalXp)

        assertEquals(level, info.level)
        assertEquals(totalXp - floor, info.xpIntoLevel)
        assertEquals(ceiling - floor, info.xpForThisLevel)
    }

    @Test
    fun `progress ratio stays within 0 and 1 and increases monotonically within a level`() {
        val floor = LevelCurve.xpThresholdForLevel(5)
        val ceiling = LevelCurve.xpThresholdForLevel(6)
        val mid = (floor + ceiling) / 2
        val infoStart = LevelCurve.levelForTotalXp(floor)
        val infoMid = LevelCurve.levelForTotalXp(mid)
        val infoEnd = LevelCurve.levelForTotalXp(ceiling - 1)

        assertTrue(infoStart.progress <= infoMid.progress)
        assertTrue(infoMid.progress <= infoEnd.progress)
        assertTrue(infoEnd.progress < 1f)
        assertTrue(infoStart.progress in 0f..1f)
    }

    @Test
    fun `negative xp is clamped to zero instead of crashing`() {
        val info = LevelCurve.levelForTotalXp(-500)
        assertEquals(1, info.level)
        assertEquals(0, info.totalXp)
    }

    @Test
    fun `very high xp resolves to a high level without excessive iteration`() {
        val info = LevelCurve.levelForTotalXp(1_000_000)
        assertTrue(info.level > 100)
        assertEquals(1_000_000, info.totalXp)
    }
}
