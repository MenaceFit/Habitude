package com.menacefit.habitude.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.notifications.ReminderCoordinator
import com.menacefit.habitude.util.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitListUiState(
    val searchQuery: String = "",
    val selectedCategory: HabitCategory? = null,
    val selectedDifficulty: Difficulty? = null,
    val showArchived: Boolean = false,
    val displayedHabits: List<Habit> = emptyList(),
    val archivedCount: Int = 0,
)

private data class Filters(val query: String, val category: HabitCategory?, val difficulty: Difficulty?, val archived: Boolean)

class HabitListViewModel(
    private val habitRepository: HabitRepository,
    private val timeProvider: TimeProvider,
    private val reminderCoordinator: ReminderCoordinator,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<HabitCategory?>(null)
    private val selectedDifficulty = MutableStateFlow<Difficulty?>(null)
    private val showArchived = MutableStateFlow(false)

    private val filters = combine(searchQuery, selectedCategory, selectedDifficulty, showArchived) { q, c, d, a -> Filters(q, c, d, a) }

    val uiState: StateFlow<HabitListUiState> = combine(
        habitRepository.observeActiveHabits(),
        habitRepository.observeArchivedHabits(),
        filters,
    ) { active, archived, f ->
        val source = if (f.archived) archived else active
        val filtered = source.filter { habit ->
            (f.query.isBlank() || habit.name.contains(f.query, ignoreCase = true)) &&
                (f.category == null || habit.category == f.category) &&
                (f.difficulty == null || habit.difficulty == f.difficulty)
        }.sortedBy { it.sortOrder }
        HabitListUiState(f.query, f.category, f.difficulty, f.archived, filtered, archived.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitListUiState())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setCategory(category: HabitCategory?) {
        selectedCategory.value = category
    }

    fun setDifficulty(difficulty: Difficulty?) {
        selectedDifficulty.value = difficulty
    }

    fun setShowArchived(show: Boolean) {
        showArchived.value = show
    }

    fun reorder(orderedIds: List<String>) {
        viewModelScope.launch { habitRepository.reorderHabits(orderedIds) }
    }

    fun archive(habitId: String) {
        viewModelScope.launch {
            habitRepository.archiveHabit(habitId, timeProvider.todayLocalDate())
            reminderCoordinator.onHabitRemoved(habitId)
        }
    }

    fun unarchive(habitId: String) {
        viewModelScope.launch {
            habitRepository.unarchiveHabit(habitId)
            habitRepository.getHabit(habitId)?.let { reminderCoordinator.onHabitSaved(it) }
        }
    }

    fun deletePermanently(habitId: String) {
        viewModelScope.launch {
            habitRepository.deleteHabitPermanently(habitId)
            reminderCoordinator.onHabitRemoved(habitId)
        }
    }
}
