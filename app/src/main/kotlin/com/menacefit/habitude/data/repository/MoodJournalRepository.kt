package com.menacefit.habitude.data.repository

import com.menacefit.habitude.data.local.dao.JournalDao
import com.menacefit.habitude.data.local.dao.MoodDao
import com.menacefit.habitude.data.mapper.toDomain
import com.menacefit.habitude.data.mapper.toEntity
import com.menacefit.habitude.domain.model.JournalEntry
import com.menacefit.habitude.domain.model.MoodEntry
import com.menacefit.habitude.domain.model.MoodLevel
import com.menacefit.habitude.util.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface MoodJournalRepository {
    fun observeAllMoods(): Flow<List<MoodEntry>>
    suspend fun getMoodRange(start: LocalDate, end: LocalDate): List<MoodEntry>
    fun observeMoodForDate(date: LocalDate): Flow<MoodEntry?>
    suspend fun setMood(date: LocalDate, mood: MoodLevel, energy: Int, note: String): MoodEntry

    fun observeAllJournalEntries(): Flow<List<JournalEntry>>
    fun observeJournalForDate(date: LocalDate): Flow<JournalEntry?>
    suspend fun setJournalText(date: LocalDate, text: String): JournalEntry
}

class MoodJournalRepositoryImpl(
    private val moodDao: MoodDao,
    private val journalDao: JournalDao,
) : MoodJournalRepository {

    override fun observeAllMoods(): Flow<List<MoodEntry>> = moodDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getMoodRange(start: LocalDate, end: LocalDate): List<MoodEntry> =
        moodDao.getRange(start, end).map { it.toDomain() }

    override fun observeMoodForDate(date: LocalDate): Flow<MoodEntry?> = moodDao.observeForDate(date).map { it?.toDomain() }

    override suspend fun setMood(date: LocalDate, mood: MoodLevel, energy: Int, note: String): MoodEntry {
        val existing = moodDao.getForDate(date)
        val entry = MoodEntry(id = existing?.id ?: newId(), date = date, mood = mood, energy = energy.coerceIn(0, 10), note = note)
        moodDao.upsert(entry.toEntity())
        return entry
    }

    override fun observeAllJournalEntries(): Flow<List<JournalEntry>> =
        journalDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeJournalForDate(date: LocalDate): Flow<JournalEntry?> =
        journalDao.observeForDate(date).map { it?.toDomain() }

    override suspend fun setJournalText(date: LocalDate, text: String): JournalEntry {
        val existing = journalDao.getForDate(date)
        val entry = JournalEntry(id = existing?.id ?: newId(), date = date, text = text)
        journalDao.upsert(entry.toEntity())
        return entry
    }
}
