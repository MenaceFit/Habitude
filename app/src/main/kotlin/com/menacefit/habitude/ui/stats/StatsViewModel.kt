package com.menacefit.habitude.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.repository.GoalRepository
import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.data.repository.ProfileRepository
import com.menacefit.habitude.data.repository.StatsRepository
import com.menacefit.habitude.domain.model.DailyStatsSnapshot
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.isPeriodBased
import com.menacefit.habitude.domain.model.isScheduledOn
import com.menacefit.habitude.domain.stats.ConsistencyBreakdown
import com.menacefit.habitude.domain.stats.ConsistencyInputs
import com.menacefit.habitude.domain.stats.ConsistencyScoreCalculator
import com.menacefit.habitude.domain.stats.HabitStatEntry
import com.menacefit.habitude.domain.stats.Insight
import com.menacefit.habitude.domain.stats.PerformanceInsightsGenerator
import com.menacefit.habitude.domain.stats.StatsAggregator
import com.menacefit.habitude.domain.stats.StatsPeriod
import com.menacefit.habitude.domain.xp.LevelCurve
import com.menacefit.habitude.util.TimeProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitStatEntryWithInfo(val habit: Habit, val entry: HabitStatEntry)

data class GlobalStats(
    val successRatePercent: Int = 0,
    val totalHabitsCompleted: Int = 0,
    val totalXp: Int = 0,
    val level: Int = 1,
    val bestStreakEver: Int = 0,
    val currentBestStreak: Int = 0,
    val perfectDaysCount: Int = 0,
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val period: StatsPeriod = StatsPeriod.MONTH,
    val globalStats: GlobalStats = GlobalStats(),
    val consistency: ConsistencyBreakdown? = null,
    val insights: List<Insight> = emptyList(),
    val dailySeries: List<DailyStatsSnapshot> = emptyList(),
    val streakSeries: List<Pair<LocalDate, Int>> = emptyList(),
    val xpSeries: List<Pair<LocalDate, Int>> = emptyList(),
    val habitSuccessRates: List<HabitStatEntryWithInfo> = emptyList(),
    val categoryComparison: Map<HabitCategory, Double> = emptyMap(),
)

class StatsViewModel(
    private val habitRepository: HabitRepository,
    private val statsRepository: StatsRepository,
    private val profileRepository: ProfileRepository,
    private val goalRepository: GoalRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setPeriod(period: StatsPeriod) {
        _uiState.update { it.copy(period = period) }
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val today = timeProvider.todayLocalDate()
            val period = _uiState.value.period

            val habits = habitRepository.getAllHabitsOnce()
            val earliestCompletion = habitRepository.getEarliestCompletionDate()
            val earliestHabitStart = habits.minOfOrNull { it.startDate }
            val earliestData = listOfNotNull(earliestCompletion, earliestHabitStart, today).min()
            val range = period.rangeFor(today, earliestData)

            val dailySeries = statsRepository.getRange(range.start, range.endInclusive)
            val previousRangeLength = java.time.temporal.ChronoUnit.DAYS.between(range.start, range.endInclusive) + 1
            val previousRange = range.start.minusDays(previousRangeLength)..range.start.minusDays(1)
            val previousSeries = statsRepository.getRange(previousRange.start, previousRange.endInclusive)

            val completionsByHabit = habits.associate { habit -> habit.id to habitRepository.getCompletionsForHabit(habit.id).filter { it.completed }.map { it.date }.toSet() }

            val successRates = StatsAggregator.successRateByHabit(habits, completionsByHabit, range.start, range.endInclusive)
            val entriesWithInfo = successRates.mapNotNull { entry -> habits.firstOrNull { it.id == entry.habitId }?.let { HabitStatEntryWithInfo(it, entry) } }
                .sortedByDescending { it.entry.successRate }

            val categoryComparison = StatsAggregator.categoryComparison(habits, completionsByHabit, range.start, range.endInclusive)

            val streaks = habits.filter { it.active }.map { habitRepository.computeStreak(it) }
            val bestStreakEver = streaks.maxOfOrNull { it.best } ?: 0
            val currentBestStreak = streaks.maxOfOrNull { it.current } ?: 0
            val totalCompleted = habitRepository.getTotalCompletedCount()
            val profile = profileRepository.getProfile()
            val levelInfo = LevelCurve.levelForTotalXp(profile?.totalXp ?: 0)
            val perfectDays = StatsAggregator.perfectDaysCount(dailySeries)
            val goals = goalRepository.observeAll().first()
            val goalCompletionRate = if (goals.isEmpty()) 0.0 else goals.map { it.progressRatio.toDouble() }.average()

            val consistency = ConsistencyScoreCalculator.compute(
                ConsistencyInputs(
                    successRate30d = StatsAggregator.averageCompletionRate(statsRepository.getRange(today.minusDays(29), today)),
                    currentStreakDays = currentBestStreak,
                    bestStreakEver = bestStreakEver,
                    perfectDaysRatio30d = run {
                        val last30 = statsRepository.getRange(today.minusDays(29), today)
                        val withData = last30.filter { it.scheduledCount > 0 }
                        if (withData.isEmpty()) 0.0 else StatsAggregator.perfectDaysCount(withData).toDouble() / withData.size
                    },
                    goalCompletionRate = goalCompletionRate,
                    frequencyAdherence = StatsAggregator.averageCompletionRate(dailySeries),
                ),
            )

            val insights = buildList {
                PerformanceInsightsGenerator.regularityChange(dailySeries, previousSeries)?.let { add(it) }
                PerformanceInsightsGenerator.bestDayOfWeek(dailySeries)?.let { add(it) }
                PerformanceInsightsGenerator.mostConsistentHabit(successRates, habits.associate { it.id to it.name })?.let { add(it) }
            }

            val streakSeries = computeMaxStreakSeries(habits.filter { !it.frequency.isPeriodBased() }, completionsByHabit, range.start, range.endInclusive)
            val xpSeries = dailySeries.map { it.date to it.xpEarned }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    globalStats = GlobalStats(
                        successRatePercent = (StatsAggregator.averageCompletionRate(dailySeries) * 100).toInt(),
                        totalHabitsCompleted = totalCompleted,
                        totalXp = profile?.totalXp ?: 0,
                        level = levelInfo.level,
                        bestStreakEver = bestStreakEver,
                        currentBestStreak = currentBestStreak,
                        perfectDaysCount = perfectDays,
                    ),
                    consistency = consistency,
                    insights = insights,
                    dailySeries = dailySeries,
                    streakSeries = streakSeries,
                    xpSeries = xpSeries,
                    habitSuccessRates = entriesWithInfo,
                    categoryComparison = categoryComparison,
                )
            }
        }
    }

    /** Longest currently-running streak among day-scheduled habits, at each date in the window — see the class doc for why this is a trend approximation, not the authoritative per-habit streak. */
    private fun computeMaxStreakSeries(
        habits: List<Habit>,
        completionsByHabit: Map<String, Set<LocalDate>>,
        start: LocalDate,
        end: LocalDate,
    ): List<Pair<LocalDate, Int>> {
        if (start.isAfter(end)) return emptyList()
        val perHabitRun = habits.associate { habit ->
            val completed = completionsByHabit[habit.id].orEmpty()
            val series = HashMap<LocalDate, Int>()
            var run = 0
            var d = start
            while (!d.isAfter(end)) {
                if (!d.isBefore(habit.startDate) && habit.frequency.isScheduledOn(d, habit.startDate)) {
                    run = if (d in completed) run + 1 else 0
                }
                series[d] = run
                d = d.plusDays(1)
            }
            habit.id to series
        }
        val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
        return dates.map { date -> date to (perHabitRun.values.maxOfOrNull { it[date] ?: 0 } ?: 0) }
    }
}
