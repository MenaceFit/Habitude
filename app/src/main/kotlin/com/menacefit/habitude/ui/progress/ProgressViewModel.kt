package com.menacefit.habitude.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.repository.ChallengeRepository
import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.data.repository.ProfileRepository
import com.menacefit.habitude.data.repository.QuestRepository
import com.menacefit.habitude.domain.model.AchievementType
import com.menacefit.habitude.domain.model.Challenge
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.QuestInstance
import com.menacefit.habitude.util.TimeProvider
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgressUiState(
    val quests: List<QuestInstance> = emptyList(),
    val challenges: List<Challenge> = emptyList(),
    val activeHabits: List<Habit> = emptyList(),
    val unlockedAchievements: Set<AchievementType> = emptySet(),
    val unlockedAtByType: Map<AchievementType, Instant> = emptyMap(),
)

class ProgressViewModel(
    private val questRepository: QuestRepository,
    private val challengeRepository: ChallengeRepository,
    private val profileRepository: ProfileRepository,
    private val habitRepository: HabitRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = combine(
        questRepository.observeForDate(timeProvider.todayLocalDate()),
        challengeRepository.observeAll(),
        profileRepository.observeUnlockedAchievements(),
        habitRepository.observeActiveHabits(),
    ) { quests, challenges, unlocked, habits ->
        ProgressUiState(
            quests = quests,
            challenges = challenges,
            activeHabits = habits,
            unlockedAchievements = unlocked.map { it.type }.toSet(),
            unlockedAtByType = unlocked.associate { it.type to it.unlockedAt },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressUiState())

    fun createChallenge(title: String, description: String, icon: String, durationDays: Int, linkedHabitId: String?, xpReward: Int) {
        viewModelScope.launch {
            challengeRepository.createChallenge(title, description, icon, timeProvider.todayLocalDate(), durationDays, linkedHabitId, xpReward)
        }
    }

    fun toggleChallengeCheckIn(challengeId: String) {
        viewModelScope.launch {
            val today = timeProvider.todayLocalDate()
            challengeRepository.toggleCheckIn(challengeId, today, today)
        }
    }

    fun deleteChallenge(id: String) {
        viewModelScope.launch { challengeRepository.deleteChallenge(id) }
    }
}
