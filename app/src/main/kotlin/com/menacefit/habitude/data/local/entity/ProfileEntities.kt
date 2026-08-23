package com.menacefit.habitude.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.menacefit.habitude.domain.model.AchievementType
import com.menacefit.habitude.domain.model.HabitCategory
import java.time.Instant

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarEmoji: String,
    val totalXp: Int,
    val streakFreezesAvailable: Int,
    val selectedCategories: Set<HabitCategory>,
    val personalGoalText: String,
    val gamificationEnabled: Boolean,
    val createdAt: Instant,
) {
    companion object {
        /** Single-row table: this app has exactly one local profile, no accounts. */
        const val LOCAL_PROFILE_ID = "local_profile"
    }
}

@Entity(tableName = "unlocked_achievements")
data class UnlockedAchievementEntity(
    @PrimaryKey val type: AchievementType,
    val unlockedAt: Instant,
)
