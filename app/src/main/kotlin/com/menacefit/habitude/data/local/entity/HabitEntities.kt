package com.menacefit.habitude.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.HabitTarget
import com.menacefit.habitude.domain.model.HabitType
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val colorHex: String,
    val category: HabitCategory,
    val type: HabitType,
    val target: HabitTarget,
    val frequency: Frequency,
    val difficulty: Difficulty,
    val reminderEnabled: Boolean,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val sortOrder: Int,
    val active: Boolean,
    val archivedAt: LocalDate?,
    val createdAt: Instant,
)

@Entity(
    tableName = "habit_completions",
    indices = [Index(value = ["habitId", "date"], unique = true), Index(value = ["date"])],
)
data class HabitCompletionEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: LocalDate,
    val completed: Boolean,
    val value: Double,
    val checklistCheckedIds: Set<String>,
    val timestamp: Instant,
)

@Entity(
    tableName = "streak_freeze_usage",
    indices = [Index(value = ["habitId", "date"], unique = true)],
)
data class StreakFreezeUsageEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: LocalDate,
    val usedAt: Instant,
)
