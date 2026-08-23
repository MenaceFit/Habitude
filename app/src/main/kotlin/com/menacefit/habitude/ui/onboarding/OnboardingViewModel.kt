package com.menacefit.habitude.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.preferences.SettingsRepository
import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.data.repository.ProfileRepository
import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.onboarding.HabitSuggestionCatalog
import com.menacefit.habitude.domain.onboarding.SuggestedHabitTemplate
import com.menacefit.habitude.ui.theme.color
import com.menacefit.habitude.ui.theme.toHexString
import com.menacefit.habitude.util.TimeProvider
import com.menacefit.habitude.util.newId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val selectedCategories: Set<HabitCategory> = emptySet(),
    val personalGoalText: String = "",
    val suggestions: List<SuggestedHabitTemplate> = emptyList(),
    val acceptedSuggestionIds: Set<String> = emptySet(),
    val profileName: String = "",
    val avatarEmoji: String = "🙂",
    val isSaving: Boolean = false,
)

class OnboardingViewModel(
    private val profileRepository: ProfileRepository,
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun toggleCategory(category: HabitCategory) {
        _uiState.update { state ->
            val turningOn = category !in state.selectedCategories
            val newSelection = if (turningOn) state.selectedCategories + category else state.selectedCategories - category
            val newSuggestions = newSelection.flatMap { HabitSuggestionCatalog.suggestionsFor(it) }
            // Newly revealed suggestions start pre-checked (the common
            // "recommended, opt out if you don't want it" onboarding
            // pattern); suggestions hidden by turning a category back off
            // leave the accepted set untouched, so re-enabling an unrelated
            // category never resurrects a suggestion the user unchecked.
            val newAccepted = if (turningOn) {
                state.acceptedSuggestionIds + HabitSuggestionCatalog.suggestionsFor(category).map { it.id }
            } else {
                state.acceptedSuggestionIds
            }
            state.copy(selectedCategories = newSelection, suggestions = newSuggestions, acceptedSuggestionIds = newAccepted)
        }
    }

    fun setPersonalGoalText(text: String) {
        _uiState.update { it.copy(personalGoalText = text) }
    }

    fun toggleSuggestion(templateId: String) {
        _uiState.update { state ->
            val newAccepted = if (templateId in state.acceptedSuggestionIds) state.acceptedSuggestionIds - templateId else state.acceptedSuggestionIds + templateId
            state.copy(acceptedSuggestionIds = newAccepted)
        }
    }

    fun setProfileName(name: String) {
        _uiState.update { it.copy(profileName = name) }
    }

    fun setAvatarEmoji(emoji: String) {
        _uiState.update { it.copy(avatarEmoji = emoji) }
    }

    /**
     * @param resolvedNames each accepted suggestion's localized display
     *   name, keyed by [SuggestedHabitTemplate.id] — resolved by the
     *   Composable caller (which has `stringResource`/`LocalContext`)
     *   rather than by this ViewModel, so business logic never depends on
     *   Android resources directly. From here on the created habit's name
     *   is just plain, user-editable text, like any manually-created habit.
     */
    fun completeOnboarding(resolvedNames: Map<String, String>, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val displayName = state.profileName.ifBlank { "Toi" }

            profileRepository.createProfile(
                name = displayName,
                avatarEmoji = state.avatarEmoji,
                selectedCategories = state.selectedCategories,
                personalGoalText = state.personalGoalText,
            )

            val today = timeProvider.todayLocalDate()
            state.suggestions
                .filter { it.id in state.acceptedSuggestionIds }
                .forEach { template ->
                    habitRepository.createHabit(
                        Habit(
                            id = newId(),
                            name = resolvedNames[template.id] ?: template.nameKey,
                            icon = template.icon,
                            colorHex = template.category.color().toHexString(),
                            category = template.category,
                            type = template.type,
                            target = template.target,
                            frequency = template.frequency,
                            difficulty = template.difficulty,
                            startDate = today,
                            createdAt = timeProvider.nowInstant(),
                        ),
                    )
                }

            settingsRepository.setOnboardingCompleted(true)
            _uiState.update { it.copy(isSaving = false) }
            onDone()
        }
    }
}
