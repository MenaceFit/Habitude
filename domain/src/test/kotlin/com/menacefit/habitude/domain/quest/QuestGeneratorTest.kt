package com.menacefit.habitude.domain.quest

import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.QuestMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuestGeneratorTest {

    private val sampleHabits = listOf(
        QuestGenerator.TodayHabit("h1", Difficulty.EASY, HabitCategory.SPORT),
        QuestGenerator.TodayHabit("h2", Difficulty.MEDIUM, HabitCategory.STUDY),
        QuestGenerator.TodayHabit("h3", Difficulty.HARD, HabitCategory.WORK),
    )

    @Test
    fun `no habits today means no quests`() {
        assertTrue(QuestGenerator.generate(LocalDate.of(2026, 1, 1), emptyList()).isEmpty())
    }

    @Test
    fun `the same day and seed always generates the same quest set`() {
        val date = LocalDate.of(2026, 5, 10)
        val first = QuestGenerator.generate(date, sampleHabits, profileSeed = 42L)
        val second = QuestGenerator.generate(date, sampleHabits, profileSeed = 42L)
        assertEquals(first, second)
    }

    @Test
    fun `a different profile seed can change the optional quest roll`() {
        val date = LocalDate.of(2026, 5, 10)
        val seeds = (0L until 20L).map { seed -> QuestGenerator.generate(date, sampleHabits, profileSeed = seed).size }
        // Not every seed should produce an identical quest count (the early-bird quest is a coin flip).
        assertTrue(seeds.toSet().size > 1)
    }

    @Test
    fun `every generated quest set always includes the complete-n and full-day quests`() {
        val quests = QuestGenerator.generate(LocalDate.of(2026, 5, 10), sampleHabits, profileSeed = 1L)
        assertTrue(quests.any { it.metric == QuestMetric.HABITS_COMPLETED_COUNT })
        assertTrue(quests.any { it.metric == QuestMetric.FULL_DAY_COMPLETION })
    }

    @Test
    fun `a hard habit today always produces a specific-habit quest targeting a hard habit`() {
        val quests = QuestGenerator.generate(LocalDate.of(2026, 5, 10), sampleHabits, profileSeed = 7L)
        val specific = quests.firstOrNull { it.metric == QuestMetric.SPECIFIC_HABIT_COMPLETED }
        assertTrue(specific != null)
        assertEquals("h3", specific!!.metricParam)
    }

    @Test
    fun `no hard habits today means no specific-habit quest`() {
        val onlyEasyAndMedium = sampleHabits.filter { it.difficulty != Difficulty.HARD }
        val quests = QuestGenerator.generate(LocalDate.of(2026, 5, 10), onlyEasyAndMedium, profileSeed = 7L)
        assertTrue(quests.none { it.metric == QuestMetric.SPECIFIC_HABIT_COMPLETED })
    }

    @Test
    fun `quest ids are stable across regeneration for the same day so progress keys match`() {
        val date = LocalDate.of(2026, 5, 10)
        val a = QuestGenerator.generate(date, sampleHabits, profileSeed = 3L).map { it.id }
        val b = QuestGenerator.generate(date, sampleHabits, profileSeed = 3L).map { it.id }
        assertEquals(a, b)
    }
}
