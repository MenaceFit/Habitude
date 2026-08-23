package com.menacefit.habitude.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.repository.GoalRepository
import com.menacefit.habitude.domain.model.Goal
import com.menacefit.habitude.util.TimeProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(
    private val goalRepository: GoalRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    val goals: StateFlow<List<Goal>> = goalRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createGoal(title: String, icon: String, targetValue: Int, deadline: LocalDate?) {
        if (title.isBlank() || targetValue <= 0) return
        viewModelScope.launch {
            goalRepository.createGoal(title.trim(), icon, targetValue, timeProvider.todayLocalDate(), deadline)
        }
    }

    fun incrementProgress(goal: Goal) {
        viewModelScope.launch { goalRepository.updateProgress(goal.id, goal.currentValue + 1) }
    }

    fun decrementProgress(goal: Goal) {
        viewModelScope.launch { goalRepository.updateProgress(goal.id, goal.currentValue - 1) }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch { goalRepository.deleteGoal(id) }
    }
}
