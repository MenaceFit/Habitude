package com.menacefit.habitude.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.menacefit.habitude.domain.model.DayPerformance
import java.time.LocalDate

/** One precomputed rollup row per calendar day — see [com.menacefit.habitude.domain.model.DailyStatsSnapshot]. */
@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val date: LocalDate,
    val scheduledCount: Int,
    val completedCount: Int,
    val completionRate: Float,
    val xpEarned: Int,
    val performance: DayPerformance,
)
