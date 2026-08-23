package com.menacefit.habitude.data.local

import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.menacefit.habitude.data.local.dao.HabitCompletionDao
import com.menacefit.habitude.data.local.dao.HabitDao
import com.menacefit.habitude.data.local.entity.HabitCompletionEntity
import com.menacefit.habitude.data.local.entity.HabitEntity
import com.menacefit.habitude.domain.model.ChecklistItem
import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.HabitTarget
import com.menacefit.habitude.domain.model.HabitType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/**
 * Room's annotation processor validates `@Query` SQL against the schema at
 * compile time, but it cannot catch a [Converters] bug — a [HabitTarget] or
 * [Frequency] value that doesn't round-trip through its JSON encoding is
 * exactly the kind of thing that only shows up by actually writing to and
 * reading from a real (in-memory) SQLite database, which is what this
 * exercises.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class HabitDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var completionDao: HabitCompletionDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitDao = database.habitDao()
        completionDao = database.habitCompletionDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun sampleHabit(
        id: String = "habit-1",
        target: HabitTarget = HabitTarget.BooleanTarget,
        frequency: Frequency = Frequency.Daily,
    ) = HabitEntity(
        id = id,
        name = "Méditer",
        description = "10 minutes le matin",
        icon = "🧘",
        colorHex = "#6E56CF",
        category = HabitCategory.WELLBEING,
        type = HabitType.BOOLEAN,
        target = target,
        frequency = frequency,
        difficulty = Difficulty.MEDIUM,
        reminderEnabled = true,
        reminderHour = 7,
        reminderMinute = 30,
        soundEnabled = true,
        vibrationEnabled = false,
        startDate = LocalDate.of(2026, 1, 1),
        endDate = null,
        sortOrder = 0,
        active = true,
        archivedAt = null,
        createdAt = Instant.parse("2026-01-01T08:00:00Z"),
    )

    @Test
    fun `insert then read back a simple habit round-trips every field`() = runTest {
        val habit = sampleHabit()
        habitDao.upsert(habit)

        val loaded = habitDao.getHabit(habit.id)

        assertEquals(habit, loaded)
    }

    @Test
    fun `polymorphic frequency and checklist target survive the JSON round-trip`() = runTest {
        val checklistTarget = HabitTarget.ChecklistTarget(
            items = listOf(
                ChecklistItem(id = "a", label = "Échauffement"),
                ChecklistItem(id = "b", label = "Étirements"),
            ),
        )
        val specificDays = Frequency.SpecificDays(isoDays = setOf(1, 3, 5))
        val habit = sampleHabit(id = "habit-2", target = checklistTarget, frequency = specificDays)

        habitDao.upsert(habit)
        val loaded = habitDao.getHabit(habit.id)

        assertEquals(checklistTarget, loaded?.target)
        assertEquals(specificDays, loaded?.frequency)
    }

    @Test
    fun `archiving clears active flag and stamps archivedAt`() = runTest {
        val habit = sampleHabit()
        habitDao.upsert(habit)
        val archiveDate = LocalDate.of(2026, 3, 15)

        habitDao.archive(habit.id, archiveDate)
        val archived = habitDao.getHabit(habit.id)

        assertEquals(false, archived?.active)
        assertEquals(archiveDate, archived?.archivedAt)

        habitDao.unarchive(habit.id)
        val unarchived = habitDao.getHabit(habit.id)

        assertEquals(true, unarchived?.active)
        assertNull(unarchived?.archivedAt)
    }

    @Test
    fun `completion queries filter by habit and date range using real ISO date comparisons`() = runTest {
        val habit = sampleHabit()
        habitDao.upsert(habit)

        val dates = listOf(
            LocalDate.of(2025, 12, 30),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 2, 1),
        )
        dates.forEach { date ->
            completionDao.upsert(
                HabitCompletionEntity(
                    id = "completion-$date",
                    habitId = habit.id,
                    date = date,
                    completed = true,
                    value = 1.0,
                    checklistCheckedIds = emptySet(),
                    timestamp = Instant.parse("2026-01-01T08:00:00Z"),
                ),
            )
        }

        // A year boundary (Dec 2025 -> Jan/Feb 2026) is exactly where a
        // naive non-ISO date format would sort incorrectly in SQLite.
        val inRange = completionDao.getForHabitAndRange(habit.id, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))

        assertEquals(setOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)), inRange.map { it.date }.toSet())
        assertEquals(4, completionDao.getTotalCompletedCount())
    }
}
