package com.menacefit.habitude.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.menacefit.habitude.R
import com.menacefit.habitude.data.preferences.AppSettings
import com.menacefit.habitude.ui.common.LocalAppContainer
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.components.CelebrationOverlay
import com.menacefit.habitude.ui.components.EmptyState
import com.menacefit.habitude.ui.components.HabitCard
import com.menacefit.habitude.ui.components.HabitCardActions
import com.menacefit.habitude.ui.components.ProgressRing
import com.menacefit.habitude.ui.components.XpBar
import com.menacefit.habitude.ui.theme.AccentColor
import com.menacefit.habitude.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    onCreateHabit: () -> Unit,
    onOpenHabitDetail: (String) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = appViewModel { c ->
        DashboardViewModel(c.habitRepository, c.profileRepository, c.questRepository, c.routineRepository, c.completionCoordinator, c.timeProvider)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())

    LaunchedEffect(uiState.celebration?.id) {
        val celebration = uiState.celebration ?: return@LaunchedEffect
        container.hapticFeedbackHelper.tick(settings.hapticsEnabled)
        container.soundPlayer.playComplete(settings.soundsEnabled)
        if (celebration.leveledUp || celebration.streakRecordBroken || celebration.unlockedAchievements.isNotEmpty()) {
            container.hapticFeedbackHelper.celebrate(settings.hapticsEnabled)
            container.soundPlayer.playLevelUp(settings.soundsEnabled)
        }
        delay(900)
        viewModel.consumeCelebration()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateHabit) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_create_habit))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!uiState.isLoading && uiState.habitItems.isEmpty()) {
                EmptyState(
                    emoji = "🌱",
                    title = stringResource(R.string.dashboard_empty_title),
                    description = stringResource(R.string.dashboard_empty_description),
                    actionLabel = stringResource(R.string.action_create_first_habit),
                    onAction = onCreateHabit,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.lg, Spacing.lg, Spacing.lg, Spacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    item { DashboardHeader(uiState, onOpenProfile) }
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            ProgressRing(
                                progress = uiState.dayProgress,
                                label = stringResource(R.string.dashboard_progress_label, uiState.completedCount, uiState.habitItems.size),
                            )
                        }
                    }
                    if (uiState.quests.isNotEmpty()) {
                        item { QuestsSummaryRow(completed = uiState.quests.count { it.completed }, total = uiState.quests.size) }
                    }
                    if (uiState.routines.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                uiState.routines.forEach { routineProgress ->
                                    RoutineRow(routineProgress)
                                }
                            }
                        }
                    }
                    item {
                        Text(stringResource(R.string.dashboard_habits_section_title), style = MaterialTheme.typography.titleLarge)
                    }
                    items(uiState.habitItems, key = { it.habit.id }) { item ->
                        HabitCard(
                            habit = item.habit,
                            completion = item.completion,
                            streak = item.streak,
                            actions = HabitCardActions(
                                onQuickComplete = { viewModel.quickAction(item.habit) },
                                onToggleChecklistItem = { itemId -> viewModel.toggleChecklistItem(item.habit, itemId) },
                                onOpenDetail = { onOpenHabitDetail(item.habit.id) },
                            ),
                        )
                    }
                }
            }

            CelebrationOverlay(
                triggerKey = uiState.celebration?.id,
                xpAwarded = uiState.celebration?.xpAwarded ?: 0,
                accent = AccentColor.fromKey(settings.accentColorKey),
            )
        }
    }
}

@Composable
private fun DashboardHeader(uiState: DashboardUiState, onOpenProfile: () -> Unit) {
    val today = LocalDate.now()
    val locale = Locale.getDefault()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) }
    val dateText = today.format(DateTimeFormatter.ofPattern("d MMMM", locale))

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("$dayName $dateText", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(motivationalMessage(uiState.dayProgress), style = MaterialTheme.typography.headlineSmall)
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .padding(start = Spacing.sm)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenProfile)
                    .padding(Spacing.xs),
            ) {
                Text(uiState.avatarEmoji, style = MaterialTheme.typography.displaySmall)
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        XpBar(
            levelInfo = uiState.levelInfo,
            levelLabel = stringResource(R.string.level_label, uiState.levelInfo.level),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun motivationalMessage(progress: Float): String = when {
    progress >= 1f -> stringResource(R.string.dashboard_message_perfect)
    progress >= 0.5f -> stringResource(R.string.dashboard_message_good)
    progress > 0f -> stringResource(R.string.dashboard_message_started)
    else -> stringResource(R.string.dashboard_message_start)
}

@Composable
private fun QuestsSummaryRow(completed: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🎯 " + stringResource(R.string.dashboard_quests_label), style = MaterialTheme.typography.titleSmall)
        Text("$completed / $total", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun RoutineRow(routineProgress: RoutineWithProgress) {
    if (routineProgress.totalCount == 0) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("${routineProgress.routine.icon} ${routineProgress.routine.name}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "${routineProgress.completedCount} / ${routineProgress.totalCount}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
