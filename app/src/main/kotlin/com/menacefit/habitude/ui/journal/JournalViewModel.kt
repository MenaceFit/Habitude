package com.menacefit.habitude.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.data.repository.MoodJournalRepository
import com.menacefit.habitude.domain.model.JournalEntry
import com.menacefit.habitude.domain.model.MoodEntry
import com.menacefit.habitude.domain.model.MoodLevel
import com.menacefit.habitude.domain.stats.Insight
import com.menacefit.habitude.domain.stats.PerformanceInsightsGenerator
import com.menacefit.habitude.util.TimeProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JournalUiState(
    val today: LocalDate = LocalDate.now(),
    val text: String = "",
    val mood: MoodLevel? = null,
    val energy: Int = 5,
    val recentEntries: List<JournalEntry> = emptyList(),
    val recentMoods: List<MoodEntry> = emptyList(),
    val moodCorrelation: Insight.MoodCorrelation? = null,
)

class JournalViewModel(
    private val moodJournalRepository: MoodJournalRepository,
    private val habitRepository: HabitRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val today = timeProvider.todayLocalDate()
    private val _uiState = MutableStateFlow(JournalUiState(today = today))
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                moodJournalRepository.observeAllJournalEntries(),
                moodJournalRepository.observeAllMoods(),
            ) { entries, moods -> entries to moods }
                .collect { (entries, moods) ->
                    val todayEntry = entries.firstOrNull { it.date == today }
                    val todayMood = moods.firstOrNull { it.date == today }
                    _uiState.update {
                        it.copy(
                            text = todayEntry?.text ?: it.text,
                            mood = todayMood?.mood ?: it.mood,
                            energy = todayMood?.energy ?: it.energy,
                            recentEntries = entries.take(30),
                            recentMoods = moods,
                        )
                    }
                }
        }
        viewModelScope.launch { computeCorrelation() }
    }

    fun setText(text: String) {
        _uiState.update { it.copy(text = text) }
        viewModelScope.launch { moodJournalRepository.setJournalText(today, text) }
    }

    fun setMood(mood: MoodLevel) {
        _uiState.update { it.copy(mood = mood) }
        viewModelScope.launch {
            moodJournalRepository.setMood(today, mood, _uiState.value.energy, "")
            computeCorrelation()
        }
    }

    fun setEnergy(energy: Int) {
        val current = _uiState.value.mood ?: MoodLevel.NEUTRAL
        _uiState.update { it.copy(energy = energy) }
        viewModelScope.launch { moodJournalRepository.setMood(today, current, energy, "") }
    }

    private suspend fun computeCorrelation() {
        val habits = habitRepository.getActiveHabitsOnce()
        val mostFrequentHabit = habits.maxByOrNull { habitRepository.getCompletionsForHabit(it.id).count { c -> c.completed } } ?: return
        val completedDates = habitRepository.getCompletionsForHabit(mostFrequentHabit.id).filter { it.completed }.map { it.date }.toSet()
        // One-shot snapshot of the mood history is enough here; this is
        // recomputed whenever the mood changes (called explicitly above).
        val snapshot = moodJournalRepository.observeAllMoods().first()
        val scoreByDate = snapshot.associate { it.date to it.mood.score.toDouble() }
        val insight = PerformanceInsightsGenerator.moodOrScoreCorrelation(mostFrequentHabit.name, completedDates, scoreByDate)
        _uiState.update { it.copy(moodCorrelation = insight) }
    }
}
