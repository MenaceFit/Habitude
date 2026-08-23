package com.menacefit.habitude.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.menacefit.habitude.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun get(id: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET totalXp = totalXp + :delta WHERE id = :id")
    suspend fun addXp(id: String, delta: Int)

    @Query("UPDATE user_profile SET streakFreezesAvailable = streakFreezesAvailable + :delta WHERE id = :id")
    suspend fun addStreakFreezes(id: String, delta: Int)

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}
