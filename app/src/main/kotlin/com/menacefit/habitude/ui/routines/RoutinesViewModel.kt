package com.menacefit.habitude.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.data.repository.RoutineRepository
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.Routine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutinesUiState(
    val routines: List<Routine> = emptyList(),
    val activeHabits: List<Habit> = emptyList(),
)

class RoutinesViewModel(
    private val routineRepository: RoutineRepository,
    private val habitRepository: HabitRepository,
) : ViewModel() {

    val uiState: StateFlow<RoutinesUiState> = combine(
        routineRepository.observeRoutines(),
        habitRepository.observeActiveHabits(),
    ) { routines, habits -> RoutinesUiState(routines, habits) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoutinesUiState())

    fun createRoutine(name: String, icon: String, habitIds: List<String>) {
        if (name.isBlank()) return
        viewModelScope.launch { routineRepository.createRoutine(name.trim(), icon, habitIds) }
    }

    fun updateRoutine(routine: Routine, habitIds: List<String>) {
        viewModelScope.launch { routineRepository.updateRoutineHabits(routine.id, routine.name, routine.icon, habitIds) }
    }

    fun deleteRoutine(id: String) {
        viewModelScope.launch { routineRepository.deleteRoutine(id) }
    }
}
