package com.menacefit.habitude.data.repository

import com.menacefit.habitude.data.local.dao.AchievementDao
import com.menacefit.habitude.data.local.dao.UserProfileDao
import com.menacefit.habitude.data.local.entity.UserProfileEntity
import com.menacefit.habitude.data.mapper.toDomain
import com.menacefit.habitude.data.mapper.toEntity
import com.menacefit.habitude.domain.model.AchievementType
import com.menacefit.habitude.domain.model.UnlockedAchievement
import com.menacefit.habitude.domain.model.UserProfile
import com.menacefit.habitude.util.TimeProvider
import com.menacefit.habitude.util.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile?
    suspend fun createProfile(name: String, avatarEmoji: String, selectedCategories: Set<com.menacefit.habitude.domain.model.HabitCategory>, personalGoalText: String): UserProfile
    suspend fun updateProfile(profile: UserProfile)
    suspend fun addXp(amount: Int)

    /** @return true if a freeze was available and consumed, false if the user had none left. */
    suspend fun consumeStreakFreeze(): Boolean
    suspend fun grantStreakFreezes(amount: Int)

    fun observeUnlockedAchievements(): Flow<List<UnlockedAchievement>>
    suspend fun getUnlockedAchievementTypes(): Set<AchievementType>

    /** Inserts newly-unlocked badges (already-unlocked ones are silently ignored) and records the unlock timestamp. */
    suspend fun unlockAchievements(types: List<AchievementType>)
}

class ProfileRepositoryImpl(
    private val profileDao: UserProfileDao,
    private val achievementDao: AchievementDao,
    private val timeProvider: TimeProvider,
) : ProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        profileDao.observe(UserProfileEntity.LOCAL_PROFILE_ID).map { it?.toDomain() }

    override suspend fun getProfile(): UserProfile? = profileDao.get(UserProfileEntity.LOCAL_PROFILE_ID)?.toDomain()

    override suspend fun createProfile(
        name: String,
        avatarEmoji: String,
        selectedCategories: Set<com.menacefit.habitude.domain.model.HabitCategory>,
        personalGoalText: String,
    ): UserProfile {
        val profile = UserProfile(
            id = UserProfileEntity.LOCAL_PROFILE_ID,
            name = name,
            avatarEmoji = avatarEmoji,
            totalXp = 0,
            streakFreezesAvailable = 1,
            selectedCategories = selectedCategories,
            personalGoalText = personalGoalText,
            gamificationEnabled = true,
            createdAt = timeProvider.nowInstant(),
        )
        profileDao.upsert(profile.toEntity())
        return profile
    }

    override suspend fun updateProfile(profile: UserProfile) {
        profileDao.update(profile.toEntity())
    }

    override suspend fun addXp(amount: Int) {
        if (amount == 0) return
        profileDao.addXp(UserProfileEntity.LOCAL_PROFILE_ID, amount)
    }

    override suspend fun consumeStreakFreeze(): Boolean {
        val profile = getProfile() ?: return false
        if (profile.streakFreezesAvailable <= 0) return false
        profileDao.addStreakFreezes(UserProfileEntity.LOCAL_PROFILE_ID, -1)
        return true
    }

    override suspend fun grantStreakFreezes(amount: Int) {
        if (amount == 0) return
        profileDao.addStreakFreezes(UserProfileEntity.LOCAL_PROFILE_ID, amount)
    }

    override fun observeUnlockedAchievements(): Flow<List<UnlockedAchievement>> =
        achievementDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getUnlockedAchievementTypes(): Set<AchievementType> = achievementDao.getUnlockedTypes().toSet()

    override suspend fun unlockAchievements(types: List<AchievementType>) {
        val now = timeProvider.nowInstant()
        types.forEach { type ->
            achievementDao.insert(UnlockedAchievement(type = type, unlockedAt = now).toEntity())
        }
    }
}
