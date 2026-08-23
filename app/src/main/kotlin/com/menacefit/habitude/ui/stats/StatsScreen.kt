package com.menacefit.habitude.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.model.ConsistencyBreakdown
import com.menacefit.habitude.domain.stats.Insight
import com.menacefit.habitude.domain.stats.StatsPeriod
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.components.SelectableChip
import com.menacefit.habitude.ui.components.charts.BarChartEntry
import com.menacefit.habitude.ui.components.charts.HorizontalBarChart
import com.menacefit.habitude.ui.components.charts.LineChart
import com.menacefit.habitude.ui.components.charts.LineChartPoint
import com.menacefit.habitude.ui.theme.ExtraShapes
import com.menacefit.habitude.ui.theme.Spacing
import com.menacefit.habitude.ui.theme.StreakFlame
import com.menacefit.habitude.ui.theme.XpGold
import com.menacefit.habitude.ui.theme.color
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(onOpenCalendar: () -> Unit) {
    val viewModel = appViewModel { c -> StatsViewModel(c.habitRepository, c.statsRepository, c.profileRepository, c.goalRepository, c.timeProvider) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg, Spacing.lg, Spacing.lg, Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium)
                SelectableChip(label = "📅 " + stringResource(R.string.stats_calendar_link), selected = false, onClick = onOpenCalendar)
            }
        }
        item { PeriodSelector(state.period, viewModel::setPeriod) }
        item { GlobalStatsGrid(state.globalStats) }
        state.consistency?.let { consistency -> item { ConsistencyScoreCard(consistency) } }
        if (state.insights.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(R.string.stats_insights_title), style = MaterialTheme.typography.titleLarge)
                    state.insights.forEach { insight -> InsightCard(insight) }
                }
            }
        }
        item {
            ChartCard(title = stringResource(R.string.stats_chart_daily_progression)) {
                LineChart(
                    points = state.dailySeries.map { LineChartPoint(it.date.format(DateTimeFormatter.ofPattern("d/M")), it.completionRate * 100) },
                    valueFormatter = { "${it.toInt()}%" },
                )
            }
        }
        item {
            ChartCard(title = stringResource(R.string.stats_chart_xp_over_time)) {
                LineChart(
                    points = state.xpSeries.map { (date, xp) -> LineChartPoint(date.format(DateTimeFormatter.ofPattern("d/M")), xp.toFloat()) },
                    lineColor = XpGold,
                )
            }
        }
        item {
            ChartCard(title = stringResource(R.string.stats_chart_streak_evolution)) {
                LineChart(
                    points = state.streakSeries.map { (date, streak) -> LineChartPoint(date.format(DateTimeFormatter.ofPattern("d/M")), streak.toFloat()) },
                    lineColor = StreakFlame,
                )
            }
        }
        if (state.habitSuccessRates.isNotEmpty()) {
            item {
                ChartCard(title = stringResource(R.string.stats_chart_success_by_habit)) {
                    HorizontalBarChart(
                        entries = state.habitSuccessRates.take(8).map {
                            BarChartEntry(it.habit.name, it.habit.icon, it.entry.successRate.toFloat(), it.habit.category.color())
                        },
                    )
                }
            }
        }
        if (state.categoryComparison.isNotEmpty()) {
            item {
                ChartCard(title = stringResource(R.string.stats_chart_category_comparison)) {
                    HorizontalBarChart(
                        entries = state.categoryComparison.entries.sortedByDescending { it.value }.map { (category, ratio) ->
                            BarChartEntry(category.name, category.defaultEmoji, ratio.toFloat(), category.color())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(StatsPeriod.entries) { period ->
            SelectableChip(label = periodLabel(period), selected = period == selected, onClick = { onSelect(period) })
        }
    }
}

@Composable
private fun periodLabel(period: StatsPeriod): String = when (period) {
    StatsPeriod.WEEK -> stringResource(R.string.period_week)
    StatsPeriod.MONTH -> stringResource(R.string.period_month)
    StatsPeriod.THREE_MONTHS -> stringResource(R.string.period_3_months)
    StatsPeriod.SIX_MONTHS -> stringResource(R.string.period_6_months)
    StatsPeriod.YEAR -> stringResource(R.string.period_year)
    StatsPeriod.ALL -> stringResource(R.string.period_all)
}

@Composable
private fun GlobalStatsGrid(stats: GlobalStats) {
    val items = listOf(
        stringResource(R.string.stats_metric_success_rate) to "${stats.successRatePercent}%",
        stringResource(R.string.stats_metric_completed) to stats.totalHabitsCompleted.toString(),
        stringResource(R.string.stats_metric_total_xp) to stats.totalXp.toString(),
        stringResource(R.string.stats_metric_level) to stats.level.toString(),
        stringResource(R.string.stats_metric_best_streak) to "🔥 ${stats.bestStreakEver}",
        stringResource(R.string.stats_metric_current_streak) to "🔥 ${stats.currentBestStreak}",
        stringResource(R.string.stats_metric_perfect_days) to stats.perfectDaysCount.toString(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                rowItems.forEach { (label, value) ->
                    Card(modifier = Modifier.weight(1f), shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(value, style = MaterialTheme.typography.headlineSmall)
                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (rowItems.size == 1) Row(modifier = Modifier.weight(1f)) {}
            }
        }
    }
}

@Composable
private fun ConsistencyScoreCard(consistency: ConsistencyBreakdown) {
    Card(shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.stats_consistency_score_title), style = MaterialTheme.typography.titleMedium)
                Text("${consistency.overall} / 100", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                stringResource(R.string.stats_consistency_score_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InsightCard(insight: Insight) {
    Card(shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(insightText(insight), modifier = Modifier.padding(Spacing.md), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun insightText(insight: Insight): String = when (insight) {
    is Insight.RegularityChange -> {
        val key = if (insight.percentChange >= 0) R.string.insight_regularity_up else R.string.insight_regularity_down
        stringResource(key, kotlin.math.abs(insight.percentChange))
    }
    is Insight.BestDayOfWeek -> stringResource(R.string.insight_best_day, insight.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()))
    is Insight.TimeOfDayPerformance -> {
        val key = if (insight.betterInMorning) R.string.insight_better_morning else R.string.insight_better_evening
        stringResource(key, insight.differencePercent)
    }
    is Insight.MostConsistentHabit -> stringResource(R.string.insight_most_consistent_habit, insight.habitName, insight.successRatePercent)
    is Insight.WeeklyObjectiveCompletion -> stringResource(R.string.insight_weekly_objective, insight.completedPercent)
    is Insight.MoodCorrelation -> {
        val key = if (insight.scoreDeltaPercent >= 0) R.string.insight_mood_correlation_positive else R.string.insight_mood_correlation_negative
        stringResource(key, insight.habitName, kotlin.math.abs(insight.scoreDeltaPercent))
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
