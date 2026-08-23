package com.menacefit.habitude.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.menacefit.habitude.data.local.entity.DailyStatsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DailyStatsEntity>>

    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getRange(start: LocalDate, end: LocalDate): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    suspend fun get(date: LocalDate): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats")
    suspend fun getAllOnce(): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats ORDER BY date ASC")
    fun observeAll(): Flow<List<DailyStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: DailyStatsEntity)

    @Query("DELETE FROM daily_stats")
    suspend fun deleteAll()
}
