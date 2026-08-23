package com.menacefit.habitude.domain.xp

import com.menacefit.habitude.domain.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class XpEngineTest {

    @Test
    fun `completion xp matches the spec's per-difficulty amounts`() {
        assertEquals(10, XpEngine.xpForCompletion(Difficulty.EASY))
        assertEquals(20, XpEngine.xpForCompletion(Difficulty.MEDIUM))
        assertEquals(40, XpEngine.xpForCompletion(Difficulty.HARD))
    }

    @Test
    fun `no daily bonus when neither condition is met`() {
        assertEquals(0, XpEngine.dailyBonusXp(dailyGoalReached = false, allHabitsCompleted = false))
    }

    @Test
    fun `daily goal bonus alone is 50 xp`() {
        assertEquals(50, XpEngine.dailyBonusXp(dailyGoalReached = true, allHabitsCompleted = false))
    }

    @Test
    fun `all-habits bonus alone is 100 xp`() {
        assertEquals(100, XpEngine.dailyBonusXp(dailyGoalReached = false, allHabitsCompleted = true))
    }

    @Test
    fun `both daily bonuses stack on a perfect day`() {
        assertEquals(150, XpEngine.dailyBonusXp(dailyGoalReached = true, allHabitsCompleted = true))
    }
}
