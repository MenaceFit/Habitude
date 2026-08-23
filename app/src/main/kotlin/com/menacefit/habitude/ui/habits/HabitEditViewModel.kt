package com.menacefit.habitude.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.domain.model.ChecklistItem
import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.HabitTarget
import com.menacefit.habitude.domain.model.HabitType
import com.menacefit.habitude.notifications.ReminderCoordinator
import com.menacefit.habitude.util.TimeProvider
import com.menacefit.habitude.util.newId
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitEditUiState(
    val isNew: Boolean = true,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val originalHabit: Habit? = null,

    val name: String = "",
    val description: String = "",
    val icon: String = "🎯",
    val colorHex: String = "#6E56CF",
    val category: HabitCategory = HabitCategory.OTHER,
    val type: HabitType = HabitType.BOOLEAN,

    val quantityTarget: Double = 1.0,
    val quantityUnit: String = "",
    val durationMinutes: Int = 20,
    val countTarget: Int = 10,
    val countUnit: String = "",
    val limitMax: Double = 60.0,
    val limitUnit: String = "min",
    val timeHour: Int = 7,
    val timeMinute: Int = 0,
    val timeIsBefore: Boolean = true,
    val checklistItems: List<ChecklistItem> = emptyList(),

    val frequency: Frequency = Frequency.Daily,
    val difficulty: Difficulty = Difficulty.MEDIUM,

    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,

    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
) {
    val isValid: Boolean get() = name.isNotBlank() && (type != HabitType.CHECKLIST || checklistItems.isNotEmpty())
}

class HabitEditViewModel(
    private val habitRepository: HabitRepository,
    private val timeProvider: TimeProvider,
    private val reminderCoordinator: ReminderCoordinator,
    habitIdToEdit: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HabitEditUiState(isNew = habitIdToEdit == null, startDate = timeProvider.todayLocalDate()),
    )
    val uiState: StateFlow<HabitEditUiState> = _uiState.asStateFlow()

    init {
        if (habitIdToEdit != null) {
            viewModelScope.launch { loadHabit(habitIdToEdit) }
        } else {
            _uiState.update { it.copy(isLoaded = true) }
        }
    }

    private suspend fun loadHabit(id: String) {
        val habit = habitRepository.getHabit(id) ?: run { _uiState.update { it.copy(isLoaded = true) }; return }
        _uiState.update { state ->
            var next = state.copy(
                isLoaded = true,
                originalHabit = habit,
                name = habit.name,
                description = habit.description,
                icon = habit.icon,
                colorHex = habit.colorHex,
                category = habit.category,
                type = habit.type,
                frequency = habit.frequency,
                difficulty = habit.difficulty,
                reminderEnabled = habit.reminderEnabled,
                reminderHour = habit.reminderHour ?: 9,
                reminderMinute = habit.reminderMinute ?: 0,
                soundEnabled = habit.soundEnabled,
                vibrationEnabled = habit.vibrationEnabled,
                startDate = habit.startDate,
                endDate = habit.endDate,
            )
            next = when (val target = habit.target) {
                is HabitTarget.BooleanTarget -> next
                is HabitTarget.QuantityTarget -> next.copy(quantityTarget = target.targetAmount, quantityUnit = target.unit)
                is HabitTarget.DurationTarget -> next.copy(durationMinutes = target.targetMinutes)
                is HabitTarget.CountTarget -> next.copy(countTarget = target.targetCount, countUnit = target.unit)
                is HabitTarget.LimitTarget -> next.copy(limitMax = target.maxAmount, limitUnit = target.unit)
                is HabitTarget.TimeTarget -> next.copy(timeHour = target.hour, timeMinute = target.minute, timeIsBefore = target.isBefore)
                is HabitTarget.ChecklistTarget -> next.copy(checklistItems = target.items)
            }
            next
        }
    }

    fun setName(value: String) = update { it.copy(name = value) }
    fun setDescription(value: String) = update { it.copy(description = value) }
    fun setIcon(value: String) = update { it.copy(icon = value) }
    fun setColorHex(value: String) = update { it.copy(colorHex = value) }
    fun setCategory(value: HabitCategory) = update { it.copy(category = value) }
    fun setType(value: HabitType) = update { it.copy(type = value) }
    fun setQuantityTarget(value: Double) = update { it.copy(quantityTarget = value) }
    fun setQuantityUnit(value: String) = update { it.copy(quantityUnit = value) }
    fun setDurationMinutes(value: Int) = update { it.copy(durationMinutes = value) }
    fun setCountTarget(value: Int) = update { it.copy(countTarget = value) }
    fun setCountUnit(value: String) = update { it.copy(countUnit = value) }
    fun setLimitMax(value: Double) = update { it.copy(limitMax = value) }
    fun setLimitUnit(value: String) = update { it.copy(limitUnit = value) }
    fun setTimeTarget(hour: Int, minute: Int, isBefore: Boolean) = update { it.copy(timeHour = hour, timeMinute = minute, timeIsBefore = isBefore) }
    fun setFrequency(value: Frequency) = update { it.copy(frequency = value) }
    fun setDifficulty(value: Difficulty) = update { it.copy(difficulty = value) }
    fun setReminderEnabled(value: Boolean) = update { it.copy(reminderEnabled = value) }
    fun setReminderTime(hour: Int, minute: Int) = update { it.copy(reminderHour = hour, reminderMinute = minute) }
    fun setSoundEnabled(value: Boolean) = update { it.copy(soundEnabled = value) }
    fun setVibrationEnabled(value: Boolean) = update { it.copy(vibrationEnabled = value) }
    fun setStartDate(value: LocalDate) = update { it.copy(startDate = value) }
    fun setEndDate(value: LocalDate?) = update { it.copy(endDate = value) }

    fun addChecklistItem(label: String) {
        if (label.isBlank()) return
        update { it.copy(checklistItems = it.checklistItems + ChecklistItem(newId(), label.trim())) }
    }

    fun removeChecklistItem(itemId: String) {
        update { it.copy(checklistItems = it.checklistItems.filterNot { item -> item.id == itemId }) }
    }

    private fun update(transform: (HabitEditUiState) -> HabitEditUiState) {
        _uiState.update(transform)
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val target = buildTarget(state)
            val original = state.originalHabit

            val habit = (
                original?.copy(
                    name = state.name.trim(),
                    description = state.description.trim(),
                    icon = state.icon,
                    colorHex = state.colorHex,
                    category = state.category,
                    type = state.type,
                    target = target,
                    frequency = state.frequency,
                    difficulty = state.difficulty,
                    reminderEnabled = state.reminderEnabled,
                    reminderHour = if (state.reminderEnabled) state.reminderHour else null,
                    reminderMinute = if (state.reminderEnabled) state.reminderMinute else null,
                    soundEnabled = state.soundEnabled,
                    vibrationEnabled = state.vibrationEnabled,
                    startDate = state.startDate,
                    endDate = state.endDate,
                ) ?: Habit(
                    id = newId(),
                    name = state.name.trim(),
                    description = state.description.trim(),
                    icon = state.icon,
                    colorHex = state.colorHex,
                    category = state.category,
                    type = state.type,
                    target = target,
                    frequency = state.frequency,
                    difficulty = state.difficulty,
                    reminderEnabled = state.reminderEnabled,
                    reminderHour = if (state.reminderEnabled) state.reminderHour else null,
                    reminderMinute = if (state.reminderEnabled) state.reminderMinute else null,
                    soundEnabled = state.soundEnabled,
                    vibrationEnabled = state.vibrationEnabled,
                    startDate = state.startDate,
                    endDate = state.endDate,
                    createdAt = timeProvider.nowInstant(),
                )
                )

            if (original != null) habitRepository.updateHabit(habit) else habitRepository.createHabit(habit)
            reminderCoordinator.onHabitSaved(habit)
            _uiState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }

    private fun buildTarget(state: HabitEditUiState): HabitTarget = when (state.type) {
        HabitType.BOOLEAN -> HabitTarget.BooleanTarget
        HabitType.QUANTITY -> HabitTarget.QuantityTarget(state.quantityTarget, state.quantityUnit)
        HabitType.DURATION -> HabitTarget.DurationTarget(state.durationMinutes)
        HabitType.COUNT -> HabitTarget.CountTarget(state.countTarget, state.countUnit)
        HabitType.LIMIT -> HabitTarget.LimitTarget(state.limitMax, state.limitUnit)
        HabitType.TIME -> HabitTarget.TimeTarget(state.timeHour, state.timeMinute, state.timeIsBefore)
        HabitType.CHECKLIST -> HabitTarget.ChecklistTarget(state.checklistItems)
    }
}
