package com.menacefit.habitude.data.repository

import com.menacefit.habitude.data.local.dao.HabitCompletionDao
import com.menacefit.habitude.data.local.dao.HabitDao
import com.menacefit.habitude.data.local.dao.StreakFreezeDao
import com.menacefit.habitude.data.local.entity.StreakFreezeUsageEntity
import com.menacefit.habitude.data.mapper.toDomain
import com.menacefit.habitude.data.mapper.toEntity
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCompletion
import com.menacefit.habitude.domain.model.isSatisfiedBy
import com.menacefit.habitude.domain.streak.StreakCalculator
import com.menacefit.habitude.domain.model.StreakResult
import com.menacefit.habitude.util.TimeProvider
import com.menacefit.habitude.util.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

interface HabitRepository {
    fun observeActiveHabits(): Flow<List<Habit>>
    fun observeAllHabits(): Flow<List<Habit>>
    fun observeArchivedHabits(): Flow<List<Habit>>
    fun observeHabit(id: String): Flow<Habit?>
    suspend fun getHabit(id: String): Habit?
    suspend fun getActiveHabitsOnce(): List<Habit>
    suspend fun getAllHabitsOnce(): List<Habit>

    suspend fun createHabit(habit: Habit)
    suspend fun updateHabit(habit: Habit)
    suspend fun reorderHabits(orderedIds: List<String>)
    suspend fun archiveHabit(id: String, date: LocalDate)
    suspend fun unarchiveHabit(id: String)
    suspend fun deleteHabitPermanently(id: String)

    fun observeCompletionsForDate(date: LocalDate): Flow<List<HabitCompletion>>
    suspend fun getCompletionsForDate(date: LocalDate): List<HabitCompletion>
    fun observeCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>>
    suspend fun getCompletionsForHabit(habitId: String): List<HabitCompletion>
    suspend fun getCompletionsForRange(start: LocalDate, end: LocalDate): List<HabitCompletion>
    suspend fun getCompletionForHabitAndDate(habitId: String, date: LocalDate): HabitCompletion?

    /** Writes raw progress for a habit on a date, deriving `completed` from the habit's target rule. Returns the resulting completion. */
    suspend fun recordProgress(habit: Habit, date: LocalDate, value: Double, checkedIds: Set<String> = emptySet()): HabitCompletion
    suspend fun clearProgress(habitId: String, date: LocalDate)

    suspend fun getFreezeDatesForHabit(habitId: String): Set<LocalDate>
    suspend fun applyStreakFreeze(habitId: String, date: LocalDate)

    suspend fun computeStreak(habit: Habit, weekStart: DayOfWeek = DayOfWeek.MONDAY): StreakResult

    suspend fun getEarliestCompletionDate(): LocalDate?
    suspend fun getTotalCompletedCount(): Int
    fun observeTotalCompletedCount(): Flow<Int>
}

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val streakFreezeDao: StreakFreezeDao,
    private val timeProvider: TimeProvider,
) : HabitRepository {

    override fun observeActiveHabits(): Flow<List<Habit>> =
        habitDao.observeActiveHabits().map { list -> list.map { it.toDomain() } }

    override fun observeAllHabits(): Flow<List<Habit>> =
        habitDao.observeAllHabits().map { list -> list.map { it.toDomain() } }

    override fun observeArchivedHabits(): Flow<List<Habit>> =
        habitDao.observeArchivedHabits().map { list -> list.map { it.toDomain() } }

    override fun observeHabit(id: String): Flow<Habit?> = habitDao.observeHabit(id).map { it?.toDomain() }

    override suspend fun getHabit(id: String): Habit? = habitDao.getHabit(id)?.toDomain()

    override suspend fun getActiveHabitsOnce(): List<Habit> = habitDao.getActiveHabitsOnce().map { it.toDomain() }

    override suspend fun getAllHabitsOnce(): List<Habit> = habitDao.getAllHabitsOnce().map { it.toDomain() }

    override suspend fun createHabit(habit: Habit) {
        val nextOrder = habitDao.getMaxSortOrder() + 1
        habitDao.upsert(habit.copy(sortOrder = nextOrder).toEntity())
    }

    override suspend fun updateHabit(habit: Habit) {
        habitDao.update(habit.toEntity())
    }

    override suspend fun reorderHabits(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id -> habitDao.updateSortOrder(id, index) }
    }

    override suspend fun archiveHabit(id: String, date: LocalDate) {
        habitDao.archive(id, date)
    }

    override suspend fun unarchiveHabit(id: String) {
        habitDao.unarchive(id)
    }

    override suspend fun deleteHabitPermanently(id: String) {
        completionDao.deleteAllForHabit(id)
        habitDao.deleteById(id)
    }

    override fun observeCompletionsForDate(date: LocalDate): Flow<List<HabitCompletion>> =
        completionDao.observeForDate(date).map { list -> list.map { it.toDomain() } }

    override suspend fun getCompletionsForDate(date: LocalDate): List<HabitCompletion> =
        completionDao.getForDate(date).map { it.toDomain() }

    override fun observeCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>> =
        completionDao.observeForHabit(habitId).map { list -> list.map { it.toDomain() } }

    override suspend fun getCompletionsForHabit(habitId: String): List<HabitCompletion> =
        completionDao.getForHabit(habitId).map { it.toDomain() }

    override suspend fun getCompletionsForRange(start: LocalDate, end: LocalDate): List<HabitCompletion> =
        completionDao.getForRange(start, end).map { it.toDomain() }

    override suspend fun getCompletionForHabitAndDate(habitId: String, date: LocalDate): HabitCompletion? =
        completionDao.getForHabitAndDate(habitId, date)?.toDomain()

    override suspend fun recordProgress(habit: Habit, date: LocalDate, value: Double, checkedIds: Set<String>): HabitCompletion {
        val existing = completionDao.getForHabitAndDate(habit.id, date)
        val completed = habit.target.isSatisfiedBy(value, checkedIds)
        val completion = HabitCompletion(
            id = existing?.id ?: newId(),
            habitId = habit.id,
            date = date,
            completed = completed,
            value = value,
            checklistCheckedIds = checkedIds,
            timestamp = timeProvider.nowInstant(),
        )
        completionDao.upsert(completion.toEntity())
        return completion
    }

    override suspend fun clearProgress(habitId: String, date: LocalDate) {
        completionDao.deleteForHabitAndDate(habitId, date)
    }

    override suspend fun getFreezeDatesForHabit(habitId: String): Set<LocalDate> =
        streakFreezeDao.getDatesForHabit(habitId).toSet()

    override suspend fun applyStreakFreeze(habitId: String, date: LocalDate) {
        streakFreezeDao.insert(
            StreakFreezeUsageEntity(id = newId(), habitId = habitId, date = date, usedAt = timeProvider.nowInstant()),
        )
    }

    override suspend fun computeStreak(habit: Habit, weekStart: DayOfWeek): StreakResult {
        val completions = getCompletionsForHabit(habit.id).filter { it.completed }.map { it.date }.toSet()
        val freezes = getFreezeDatesForHabit(habit.id)
        return StreakCalculator.compute(
            frequency = habit.frequency,
            startDate = habit.startDate,
            effectiveEndDate = habit.effectiveEndDate,
            completedDates = completions,
            freezeDates = freezes,
            today = timeProvider.todayLocalDate(),
            weekStart = weekStart,
        )
    }

    override suspend fun getEarliestCompletionDate(): LocalDate? = completionDao.getEarliestCompletionDate()

    override suspend fun getTotalCompletedCount(): Int = completionDao.getTotalCompletedCount()

    override fun observeTotalCompletedCount(): Flow<Int> = completionDao.observeTotalCompletedCount()
}
