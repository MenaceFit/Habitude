@file:UseSerializers(InstantIsoSerializer::class)

package com.menacefit.habitude.domain.model

import com.menacefit.habitude.domain.serialization.InstantIsoSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val avatarEmoji: String = "🙂",
    val totalXp: Int = 0,
    val streakFreezesAvailable: Int = 1,
    val selectedCategories: Set<HabitCategory> = emptySet(),
    val personalGoalText: String = "",
    val gamificationEnabled: Boolean = true,
    val createdAt: Instant,
)
