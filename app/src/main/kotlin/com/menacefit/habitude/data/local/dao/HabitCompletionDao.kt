package com.menacefit.habitude.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.menacefit.habitude.data.local.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    suspend fun getForDate(date: LocalDate): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date ASC")
    fun observeForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date ASC")
    suspend fun getForHabit(habitId: String): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getForHabitAndDate(habitId: String, date: LocalDate): HabitCompletionEntity?

    @Query("SELECT * FROM habit_completions WHERE date BETWEEN :start AND :end")
    suspend fun getForRange(start: LocalDate, end: LocalDate): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllOnce(): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date BETWEEN :start AND :end")
    suspend fun getForHabitAndRange(habitId: String, start: LocalDate, end: LocalDate): List<HabitCompletionEntity>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE completed = 1")
    fun observeTotalCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE completed = 1")
    suspend fun getTotalCompletedCount(): Int

    @Query("SELECT MIN(date) FROM habit_completions")
    suspend fun getEarliestCompletionDate(): LocalDate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteForHabitAndDate(habitId: String, date: LocalDate)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteAllForHabit(habitId: String)

    @Query("DELETE FROM habit_completions")
    suspend fun deleteAll()
}
