package com.menacefit.habitude.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.menacefit.habitude.data.local.entity.UnlockedAchievementEntity
import com.menacefit.habitude.domain.model.AchievementType
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM unlocked_achievements ORDER BY unlockedAt DESC")
    fun observeAll(): Flow<List<UnlockedAchievementEntity>>

    @Query("SELECT type FROM unlocked_achievements")
    suspend fun getUnlockedTypes(): List<AchievementType>

    @Query("SELECT * FROM unlocked_achievements")
    suspend fun getAllOnce(): List<UnlockedAchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: UnlockedAchievementEntity)

    @Query("DELETE FROM unlocked_achievements")
    suspend fun deleteAll()
}
