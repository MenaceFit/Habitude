package com.menacefit.habitude.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.menacefit.habitude.data.local.entity.ChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY startDate DESC")
    fun observeAll(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges ORDER BY startDate DESC")
    suspend fun getAllOnce(): List<ChallengeEntity>

    @Query("SELECT * FROM challenges WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(challenge: ChallengeEntity)

    @Update
    suspend fun update(challenge: ChallengeEntity)

    @Query("DELETE FROM challenges WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM challenges")
    suspend fun deleteAll()
}
