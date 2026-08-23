package com.menacefit.habitude.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.model.DayPerformance
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.theme.Spacing
import com.menacefit.habitude.ui.theme.StreakFlame
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private fun DayPerformance?.toColor(): Color = when (this) {
    DayPerformance.PERFECT -> Color(0xFF2FA65A)
    DayPerformance.GOOD -> Color(0xFFD8C93A)
    DayPerformance.LOW -> Color(0xFFD1495B)
    DayPerformance.NONE, null -> Color(0xFFE3E3E8)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c -> CalendarViewModel(c.statsRepository, c.timeProvider) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(Spacing.lg, Spacing.lg, Spacing.lg, Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item { Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineMedium) }
            item { MonthHeader(state.displayedMonth, viewModel::previousMonth, viewModel::nextMonth) }
            item { WeekdayHeaderRow() }
            item { MonthGrid(state.daysInMonth, onDayClick = viewModel::selectDate) }
            item { LegendRow() }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(R.string.calendar_heatmap_title), style = MaterialTheme.typography.titleLarge)
                    HeatmapGrid(state.heatmapWeeks)
                }
            }
        }
    }

    val detail = state.selectedDayDetail
    if (detail != null) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissDetail) {
            DayDetailSheet(detail)
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) { Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.action_previous_month)) }
        Text(
            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() } + " " + month.year,
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.action_next_month)) }
    }
}

@Composable
private fun WeekdayHeaderRow() {
    val labels = listOf(
        R.string.weekday_mon_short, R.string.weekday_tue_short, R.string.weekday_wed_short,
        R.string.weekday_thu_short, R.string.weekday_fri_short, R.string.weekday_sat_short, R.string.weekday_sun_short,
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { resId ->
            Text(
                stringResource(resId),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthGrid(cells: List<CalendarDayCell>, onDayClick: (LocalDate) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth()) {
        items(cells.size) { index ->
            val cell = cells[index]
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(cell.performance.toColor().copy(alpha = if (cell.date == null) 0f else if (cell.performance == null) 0.3f else 1f))
                    .then(if (cell.date != null) Modifier.clickable { onDayClick(cell.date) } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (cell.date != null) {
                    Text(
                        cell.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (cell.performance == DayPerformance.PERFECT || cell.performance == DayPerformance.LOW) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        LegendDot(DayPerformance.PERFECT.toColor(), stringResource(R.string.calendar_legend_perfect))
        LegendDot(DayPerformance.GOOD.toColor(), stringResource(R.string.calendar_legend_good))
        LegendDot(DayPerformance.LOW.toColor(), stringResource(R.string.calendar_legend_low))
        LegendDot(DayPerformance.NONE.toColor(), stringResource(R.string.calendar_legend_none))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(" $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HeatmapGrid(weeks: List<List<HeatmapCell?>>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        items(weeks.size) { weekIndex ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                weeks[weekIndex].forEach { cell ->
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(cell?.performance.toColor().copy(alpha = if (cell == null) 0f else if (cell.performance == DayPerformance.NONE) 0.25f else 1f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayDetailSheet(detail: DayDetail) {
    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
        Text(detail.date.toString(), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            stringResource(R.string.calendar_detail_completed, detail.completedCount, detail.scheduledCount),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("🔥 " + stringResource(R.string.calendar_detail_xp, detail.xp), style = MaterialTheme.typography.bodyMedium, color = StreakFlame)
    }
}
