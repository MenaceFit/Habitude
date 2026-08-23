package com.menacefit.habitude.data.repository

import com.menacefit.habitude.data.local.dao.DailyStatsDao
import com.menacefit.habitude.data.mapper.toDomain
import com.menacefit.habitude.data.mapper.toEntity
import com.menacefit.habitude.domain.model.DailyStatsSnapshot
import com.menacefit.habitude.domain.model.DayPerformance
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCompletion
import com.menacefit.habitude.domain.model.isDueOn
import com.menacefit.habitude.domain.stats.CalendarDayScorer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface StatsRepository {
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DailyStatsSnapshot>>
    suspend fun getRange(start: LocalDate, end: LocalDate): List<DailyStatsSnapshot>
    suspend fun getSnapshot(date: LocalDate): DailyStatsSnapshot?

    /** Length of the run of 100%-completion days ending at (and including) [date], walking backward until the first non-perfect or missing day. */
    suspend fun consecutivePerfectDaysEndingAt(date: LocalDate): Int

    /**
     * Recomputes and persists the rollup for [date] from the current set of
     * habits + that day's completions. Called once after any completion
     * change so every screen that reads [DailyStatsSnapshot] (dashboard
     * ring, calendar, heatmap, charts) stays correct without ever
     * rescanning raw completions itself.
     */
    suspend fun recomputeSnapshot(date: LocalDate, allHabits: List<Habit>, completionsForDate: List<HabitCompletion>, xpEarnedToday: Int): DailyStatsSnapshot
}

class StatsRepositoryImpl(private val dailyStatsDao: DailyStatsDao) : StatsRepository {

    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DailyStatsSnapshot>> =
        dailyStatsDao.observeRange(start, end).map { list -> list.map { it.toDomain() } }

    override suspend fun getRange(start: LocalDate, end: LocalDate): List<DailyStatsSnapshot> =
        dailyStatsDao.getRange(start, end).map { it.toDomain() }

    override suspend fun getSnapshot(date: LocalDate): DailyStatsSnapshot? = dailyStatsDao.get(date)?.toDomain()

    override suspend fun consecutivePerfectDaysEndingAt(date: LocalDate): Int {
        var count = 0
        var cursor = date
        while (true) {
            val snapshot = dailyStatsDao.get(cursor) ?: break
            if (snapshot.performance != DayPerformance.PERFECT) break
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    override suspend fun recomputeSnapshot(
        date: LocalDate,
        allHabits: List<Habit>,
        completionsForDate: List<HabitCompletion>,
        xpEarnedToday: Int,
    ): DailyStatsSnapshot {
        val dueToday = allHabits.filter { it.active && it.isDueOn(date) }
        val completedIds = completionsForDate.filter { it.completed }.map { it.habitId }.toSet()
        val scheduledCount = dueToday.size
        val completedCount = dueToday.count { it.id in completedIds }
        val rate = if (scheduledCount == 0) 0f else completedCount.toFloat() / scheduledCount
        val performance = CalendarDayScorer.score(scheduledCount, completedCount)

        // Preserve any XP already recorded for the day when the caller
        // isn't reporting a fresh amount (e.g. a background recompute
        // triggered by editing an old habit, not a new completion).
        val existing = dailyStatsDao.get(date)
        val xp = if (xpEarnedToday != 0) (existing?.xpEarned ?: 0) + xpEarnedToday else existing?.xpEarned ?: 0

        val snapshot = DailyStatsSnapshot(
            date = date,
            scheduledCount = scheduledCount,
            completedCount = completedCount,
            completionRate = rate,
            xpEarned = xp,
            performance = performance,
        )
        dailyStatsDao.upsert(snapshot.toEntity())
        return snapshot
    }
}
