package com.menacefit.habitude.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.menacefit.habitude.data.local.entity.StreakFreezeUsageEntity
import java.time.LocalDate

@Dao
interface StreakFreezeDao {
    @Query("SELECT * FROM streak_freeze_usage WHERE habitId = :habitId")
    suspend fun getForHabit(habitId: String): List<StreakFreezeUsageEntity>

    @Query("SELECT date FROM streak_freeze_usage WHERE habitId = :habitId")
    suspend fun getDatesForHabit(habitId: String): List<LocalDate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: StreakFreezeUsageEntity)

    @Query("DELETE FROM streak_freeze_usage WHERE habitId = :habitId AND date = :date")
    suspend fun remove(habitId: String, date: LocalDate)

    @Query("SELECT COUNT(*) FROM streak_freeze_usage")
    suspend fun getTotalUsageCount(): Int

    @Query("DELETE FROM streak_freeze_usage")
    suspend fun deleteAll()
}
