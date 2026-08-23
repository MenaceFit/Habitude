package com.menacefit.habitude.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.menacefit.habitude.data.local.entity.QuestInstanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface QuestDao {
    @Query("SELECT * FROM quest_instances WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<List<QuestInstanceEntity>>

    @Query("SELECT * FROM quest_instances WHERE date = :date")
    suspend fun getForDate(date: LocalDate): List<QuestInstanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(quests: List<QuestInstanceEntity>)

    @Update
    suspend fun update(quest: QuestInstanceEntity)

    @Query("SELECT COUNT(*) FROM quest_instances WHERE completed = 1")
    suspend fun getTotalCompletedCount(): Int

    @Query("DELETE FROM quest_instances")
    suspend fun deleteAll()
}
