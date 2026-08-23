package com.menacefit.habitude.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.data.repository.ProfileRepository
import com.menacefit.habitude.data.repository.QuestRepository
import com.menacefit.habitude.data.repository.RoutineRepository
import com.menacefit.habitude.domain.model.AchievementType
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCompletion
import com.menacefit.habitude.domain.model.HabitTarget
import com.menacefit.habitude.domain.model.HabitType
import com.menacefit.habitude.domain.model.LevelInfo
import com.menacefit.habitude.domain.model.QuestInstance
import com.menacefit.habitude.domain.model.Routine
import com.menacefit.habitude.domain.model.StreakResult
import com.menacefit.habitude.domain.model.isDueOn
import com.menacefit.habitude.domain.xp.LevelCurve
import com.menacefit.habitude.gamification.CompletionCoordinator
import com.menacefit.habitude.util.TimeProvider
import com.menacefit.habitude.util.localTimeOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardHabitItem(val habit: Habit, val completion: HabitCompletion?, val streak: StreakResult)

data class RoutineWithProgress(val routine: Routine, val completedCount: Int, val totalCount: Int)

data class CelebrationEvent(
    val id: Long,
    val xpAwarded: Int,
    val dailyGoalBonusAwarded: Boolean,
    val allHabitsBonusAwarded: Boolean,
    val leveledUp: Boolean,
    val newLevel: Int?,
    val streakRecordBroken: Boolean,
    val unlockedAchievements: List<AchievementType>,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val profileName: String = "",
    val avatarEmoji: String = "🙂",
    val levelInfo: LevelInfo = LevelCurve.levelForTotalXp(0),
    val dayProgress: Float = 0f,
    val completedCount: Int = 0,
    val habitItems: List<DashboardHabitItem> = emptyList(),
    val quests: List<QuestInstance> = emptyList(),
    val routines: List<RoutineWithProgress> = emptyList(),
    val celebration: CelebrationEvent? = null,
)

class DashboardViewModel(
    private val habitRepository: HabitRepository,
    private val profileRepository: ProfileRepository,
    private val questRepository: QuestRepository,
    private val routineRepository: RoutineRepository,
    private val completionCoordinator: CompletionCoordinator,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var celebrationCounter = 0L

    init {
        viewModelScope.launch { ensureTodayQuests() }
        viewModelScope.launch { observeData() }
    }

    private suspend fun ensureTodayQuests() {
        val today = timeProvider.todayLocalDate()
        val activeHabits = habitRepository.getActiveHabitsOnce()
        val profile = profileRepository.getProfile()
        questRepository.ensureQuestsForDate(today, activeHabits, profileSeed = profile?.createdAt?.epochSecond ?: 0L)
    }

    private suspend fun observeData() {
        val today = timeProvider.todayLocalDate()
        combine(
            habitRepository.observeActiveHabits(),
            habitRepository.observeCompletionsForDate(today),
            profileRepository.observeProfile(),
            questRepository.observeForDate(today),
            routineRepository.observeRoutines(),
        ) { habits, completions, profile, quests, routines ->
            val dueToday = habits.filter { it.isDueOn(today) }.sortedBy { it.sortOrder }
            val completionByHabit = completions.associateBy { it.habitId }
            val items = dueToday.map { habit ->
                DashboardHabitItem(habit, completionByHabit[habit.id], habitRepository.computeStreak(habit))
            }
            val completedCount = items.count { it.completion?.completed == true }
            val progress = if (items.isEmpty()) 0f else completedCount.toFloat() / items.size

            val routinesWithProgress = routines.map { routine ->
                val relevantHabits = routine.habitIds.mapNotNull { id -> dueToday.firstOrNull { it.id == id } }
                val completed = relevantHabits.count { completionByHabit[it.id]?.completed == true }
                RoutineWithProgress(routine, completed, relevantHabits.size)
            }

            DashboardUiState(
                isLoading = false,
                profileName = profile?.name.orEmpty(),
                avatarEmoji = profile?.avatarEmoji ?: "🙂",
                levelInfo = LevelCurve.levelForTotalXp(profile?.totalXp ?: 0),
                dayProgress = progress,
                completedCount = completedCount,
                habitItems = items,
                quests = quests,
                routines = routinesWithProgress,
            )
        }.collect { freshState ->
            // Preserve any celebration event already in flight: this
            // refresh fires from the same DB write that triggered it, so a
            // naive full replace could wipe the popup before it's shown.
            _uiState.update { current -> freshState.copy(celebration = current.celebration) }
        }
    }

    fun quickAction(habit: Habit) {
        viewModelScope.launch {
            val current = uiState.value.habitItems.firstOrNull { it.habit.id == habit.id }?.completion
            val newValue = when (habit.type) {
                HabitType.TIME -> minutesSinceMidnightNow()
                else -> nextQuickValue(habit, current?.value ?: 0.0)
            }
            applyProgress(habit, newValue, current?.checklistCheckedIds ?: emptySet())
        }
    }

    fun toggleChecklistItem(habit: Habit, itemId: String) {
        viewModelScope.launch {
            val current = uiState.value.habitItems.firstOrNull { it.habit.id == habit.id }?.completion
            val checked = current?.checklistCheckedIds ?: emptySet()
            val newChecked = if (itemId in checked) checked - itemId else checked + itemId
            applyProgress(habit, current?.value ?: 0.0, newChecked)
        }
    }

    private suspend fun applyProgress(habit: Habit, value: Double, checkedIds: Set<String>) {
        val outcome = completionCoordinator.recordProgress(habit, value, checkedIds)
        if (outcome.wasNewlyCompleted) {
            celebrationCounter++
            val event = CelebrationEvent(
                id = celebrationCounter,
                xpAwarded = outcome.xpAwarded,
                dailyGoalBonusAwarded = outcome.dailyGoalBonusAwarded,
                allHabitsBonusAwarded = outcome.allHabitsBonusAwarded,
                leveledUp = outcome.leveledUp,
                newLevel = outcome.newLevelInfo?.level,
                streakRecordBroken = outcome.streakRecordBroken,
                unlockedAchievements = outcome.newlyUnlockedAchievements,
            )
            _uiState.update { it.copy(celebration = event) }
        }
    }

    fun consumeCelebration() {
        _uiState.update { it.copy(celebration = null) }
    }

    private fun minutesSinceMidnightNow(): Double {
        val time = timeProvider.localTimeOf(timeProvider.nowInstant())
        return (time.hour * 60 + time.minute).toDouble()
    }

    private fun nextQuickValue(habit: Habit, currentValue: Double): Double = when (val target = habit.target) {
        is HabitTarget.BooleanTarget -> if (currentValue >= 1.0) 0.0 else 1.0
        is HabitTarget.QuantityTarget -> if (currentValue >= target.targetAmount) 0.0 else (currentValue + (target.targetAmount / 4.0).coerceAtLeast(0.1)).coerceAtMost(target.targetAmount)
        is HabitTarget.DurationTarget -> if (currentValue >= target.targetMinutes) 0.0 else (currentValue + (target.targetMinutes / 4.0).coerceAtLeast(1.0)).coerceAtMost(target.targetMinutes.toDouble())
        is HabitTarget.CountTarget -> if (currentValue >= target.targetCount) 0.0 else (currentValue + 1.0).coerceAtMost(target.targetCount.toDouble())
        is HabitTarget.LimitTarget -> currentValue + (target.maxAmount / 6.0).coerceAtLeast(1.0)
        is HabitTarget.TimeTarget -> currentValue
        is HabitTarget.ChecklistTarget -> currentValue
    }
}
