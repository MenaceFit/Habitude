package com.menacefit.habitude.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.data.repository.ProfileRepository
import com.menacefit.habitude.domain.achievement.AchievementCatalog
import com.menacefit.habitude.domain.model.LevelInfo
import com.menacefit.habitude.domain.xp.LevelCurve
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    val avatarEmoji: String = "🙂",
    val levelInfo: LevelInfo = LevelCurve.levelForTotalXp(0),
    val unlockedCount: Int = 0,
    val totalAchievements: Int = AchievementCatalog.all.size,
    val bestStreakEver: Int = 0,
    val totalHabitsCompleted: Int = 0,
    val createdAt: Instant? = null,
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val habitRepository: HabitRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        profileRepository.observeProfile(),
        profileRepository.observeUnlockedAchievements(),
        habitRepository.observeTotalCompletedCount(),
    ) { profile, unlocked, totalCompleted ->
        val bestStreakEver = habitRepository.getActiveHabitsOnce().maxOfOrNull { habitRepository.computeStreak(it).best } ?: 0
        ProfileUiState(
            name = profile?.name.orEmpty(),
            avatarEmoji = profile?.avatarEmoji ?: "🙂",
            levelInfo = LevelCurve.levelForTotalXp(profile?.totalXp ?: 0),
            unlockedCount = unlocked.size,
            bestStreakEver = bestStreakEver,
            totalHabitsCompleted = totalCompleted,
            createdAt = profile?.createdAt,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    fun renameProfile(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val current = profileRepository.getProfile() ?: return@launch
            profileRepository.updateProfile(current.copy(name = newName.trim()))
        }
    }

    fun changeAvatar(emoji: String) {
        viewModelScope.launch {
            val current = profileRepository.getProfile() ?: return@launch
            profileRepository.updateProfile(current.copy(avatarEmoji = emoji))
        }
    }
}
