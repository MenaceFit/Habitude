package com.menacefit.habitude.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.menacefit.habitude.domain.model.MoodLevel
import java.time.LocalDate

@Entity(tableName = "mood_entries", indices = [Index(value = ["date"], unique = true)])
data class MoodEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val mood: MoodLevel,
    val energy: Int,
    val note: String,
)

@Entity(tableName = "journal_entries", indices = [Index(value = ["date"], unique = true)])
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val text: String,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val icon: String,
    val targetValue: Int,
    val currentValue: Int,
    val startDate: LocalDate,
    val deadline: LocalDate?,
    val achieved: Boolean,
)
