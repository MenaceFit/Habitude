package com.menacefit.habitude.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.menacefit.habitude.domain.model.ChallengeStatus
import com.menacefit.habitude.domain.model.QuestMetric
import java.time.LocalDate

@Entity(tableName = "quest_instances", indices = [Index(value = ["date"])])
data class QuestInstanceEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val descriptionKey: String,
    val metric: QuestMetric,
    val metricParam: String?,
    val date: LocalDate,
    val targetProgress: Int,
    val currentProgress: Int,
    val xpReward: Int,
    val completed: Boolean,
)

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val startDate: LocalDate,
    val durationDays: Int,
    val linkedHabitId: String?,
    val xpReward: Int,
    val status: ChallengeStatus,
    val completedDates: Set<LocalDate>,
)
