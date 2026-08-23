package com.menacefit.habitude.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.menacefit.habitude.data.local.entity.JournalEntryEntity
import com.menacefit.habitude.data.local.entity.MoodEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getRange(start: LocalDate, end: LocalDate): List<MoodEntryEntity>

    @Query("SELECT * FROM mood_entries")
    suspend fun getAllOnce(): List<MoodEntryEntity>

    @Query("SELECT * FROM mood_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: LocalDate): MoodEntryEntity?

    @Query("SELECT * FROM mood_entries WHERE date = :date LIMIT 1")
    fun observeForDate(date: LocalDate): Flow<MoodEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MoodEntryEntity)

    @Query("DELETE FROM mood_entries")
    suspend fun deleteAll()
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: LocalDate): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries")
    suspend fun getAllOnce(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    fun observeForDate(date: LocalDate): Flow<JournalEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAll()
}
