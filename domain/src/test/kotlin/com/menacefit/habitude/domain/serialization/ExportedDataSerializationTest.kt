package com.menacefit.habitude.domain.serialization

import com.menacefit.habitude.domain.model.AchievementType
import com.menacefit.habitude.domain.model.Challenge
import com.menacefit.habitude.domain.model.ChallengeStatus
import com.menacefit.habitude.domain.model.ChecklistItem
import com.menacefit.habitude.domain.model.DailyStatsSnapshot
import com.menacefit.habitude.domain.model.DayPerformance
import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.ExportedData
import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.Goal
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.HabitCompletion
import com.menacefit.habitude.domain.model.HabitTarget
import com.menacefit.habitude.domain.model.HabitType
import com.menacefit.habitude.domain.model.JournalEntry
import com.menacefit.habitude.domain.model.MoodEntry
import com.menacefit.habitude.domain.model.MoodLevel
import com.menacefit.habitude.domain.model.Routine
import com.menacefit.habitude.domain.model.UnlockedAchievement
import com.menacefit.habitude.domain.model.UserProfile
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * The export/import feature (spec section 47) is only as trustworthy as
 * this round-trip: everything the app persists must survive
 * encode-then-decode byte-for-byte equal, including every polymorphic
 * [HabitTarget] and [Frequency] variant and every java.time field.
 */
class ExportedDataSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private fun sampleHabit(id: String, target: HabitTarget, frequency: Frequency) = Habit(
        id = id, name = "Habit $id", description = "desc", icon = "🏃", colorHex = "#FF00AA",
        category = HabitCategory.SPORT, type = HabitType.BOOLEAN, target = target, frequency = frequency,
        difficulty = Difficulty.HARD, reminderEnabled = true, reminderHour = 7, reminderMinute = 30,
        startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 12, 31), sortOrder = 2,
        active = true, archivedAt = null, createdAt = Instant.parse("2026-01-01T10:15:30Z"),
    )

    @Test
    fun `every HabitTarget variant survives a json round trip`() {
        val targets = listOf(
            HabitTarget.BooleanTarget,
            HabitTarget.QuantityTarget(2.5, "L"),
            HabitTarget.DurationTarget(45),
            HabitTarget.CountTarget(50, "reps"),
            HabitTarget.LimitTarget(120.0, "min"),
            HabitTarget.TimeTarget(23, 0, isBefore = true),
            HabitTarget.ChecklistTarget(listOf(ChecklistItem("1", "Eau"), ChecklistItem("2", "Lit"))),
        )
        for (target in targets) {
            val encoded = json.encodeToString(HabitTarget.serializer(), target)
            val decoded = json.decodeFromString(HabitTarget.serializer(), encoded)
            assertEquals(target, decoded)
        }
    }

    @Test
    fun `every Frequency variant survives a json round trip`() {
        val frequencies = listOf(
            Frequency.Daily,
            Frequency.SpecificDays(setOf(1, 3, 5)),
            Frequency.TimesPerWeek(3),
            Frequency.TimesPerMonth(2),
            Frequency.EveryNDays(4),
        )
        for (frequency in frequencies) {
            val encoded = json.encodeToString(Frequency.serializer(), frequency)
            val decoded = json.decodeFromString(Frequency.serializer(), encoded)
            assertEquals(frequency, decoded)
        }
    }

    @Test
    fun `a full export round trips byte-for-byte equal`() {
        val habit1 = sampleHabit("h1", HabitTarget.QuantityTarget(2.0, "L"), Frequency.Daily)
        val habit2 = sampleHabit("h2", HabitTarget.ChecklistTarget(listOf(ChecklistItem("a", "Step A"))), Frequency.TimesPerWeek(3))

        val data = ExportedData(
            exportedAt = Instant.parse("2026-08-22T12:00:00Z"),
            profile = UserProfile(
                id = "local_profile", name = "Alex", avatarEmoji = "🚀", totalXp = 1240,
                streakFreezesAvailable = 2, selectedCategories = setOf(HabitCategory.SPORT, HabitCategory.SLEEP),
                personalGoalText = "Courir un marathon", gamificationEnabled = true,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
            habits = listOf(habit1, habit2),
            completions = listOf(
                HabitCompletion(
                    id = "c1", habitId = "h1", date = LocalDate.of(2026, 8, 20), completed = true,
                    value = 2.0, checklistCheckedIds = emptySet(), timestamp = Instant.parse("2026-08-20T08:00:00Z"),
                ),
                HabitCompletion(
                    id = "c2", habitId = "h2", date = LocalDate.of(2026, 8, 21), completed = false,
                    value = 0.0, checklistCheckedIds = setOf("a"), timestamp = Instant.parse("2026-08-21T08:00:00Z"),
                ),
            ),
            unlockedAchievements = listOf(UnlockedAchievement(AchievementType.STREAK_7, Instant.parse("2026-08-15T00:00:00Z"))),
            dailyStats = listOf(
                DailyStatsSnapshot(LocalDate.of(2026, 8, 20), scheduledCount = 2, completedCount = 2, completionRate = 1f, xpEarned = 30, performance = DayPerformance.PERFECT),
            ),
            moods = listOf(MoodEntry("m1", LocalDate.of(2026, 8, 20), MoodLevel.GREAT, energy = 8, note = "Bonne journée")),
            journalEntries = listOf(JournalEntry("j1", LocalDate.of(2026, 8, 20), "Journal libre")),
            goals = listOf(Goal("g1", "Lire 12 livres", "📚", targetValue = 12, currentValue = 8, startDate = LocalDate.of(2026, 1, 1), deadline = LocalDate.of(2026, 12, 31), achieved = false)),
            challenges = listOf(
                Challenge(
                    id = "ch1", title = "7 jours de sport", description = "desc", icon = "🏋️",
                    startDate = LocalDate.of(2026, 8, 1), durationDays = 7, linkedHabitId = "h1", xpReward = 200,
                    status = ChallengeStatus.ACTIVE, completedDates = setOf(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2)),
                ),
            ),
            routines = listOf(Routine("r1", "Routine matin", "🌅", sortOrder = 0, habitIds = listOf("h1", "h2"))),
        )

        val encoded = json.encodeToString(ExportedData.serializer(), data)
        val decoded = json.decodeFromString(ExportedData.serializer(), encoded)

        assertEquals(data, decoded)
    }
}
