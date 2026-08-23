package com.menacefit.habitude.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.components.EmptyState
import com.menacefit.habitude.ui.components.SelectableChip
import com.menacefit.habitude.ui.theme.Spacing
import com.menacefit.habitude.ui.theme.color

@Composable
fun HabitListScreen(onCreateHabit: () -> Unit, onOpenDetail: (String) -> Unit) {
    val viewModel = appViewModel { c -> HabitListViewModel(c.habitRepository, c.timeProvider, c.reminderCoordinator) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var habitPendingDeletion by remember { mutableStateOf<Habit?>(null) }

    val isFiltering = state.searchQuery.isNotBlank() || state.selectedCategory != null || state.selectedDifficulty != null

    Scaffold(
        floatingActionButton = {
            if (!state.showArchived) {
                FloatingActionButton(onClick = onCreateHabit) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_create_habit))
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg)) {
            Spacer(modifier = Modifier.padding(top = Spacing.sm))
            Text(stringResource(R.string.habits_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.padding(top = Spacing.sm))
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text(stringResource(R.string.habits_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.padding(top = Spacing.sm))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                item {
                    SelectableChip(
                        label = stringResource(R.string.habits_filter_active),
                        selected = !state.showArchived,
                        onClick = { viewModel.setShowArchived(false) },
                    )
                }
                item {
                    SelectableChip(
                        label = stringResource(R.string.habits_filter_archived, state.archivedCount),
                        selected = state.showArchived,
                        onClick = { viewModel.setShowArchived(true) },
                    )
                }
                items(HabitCategory.entries) { category ->
                    SelectableChip(
                        label = category.defaultEmoji,
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.setCategory(if (state.selectedCategory == category) null else category) },
                    )
                }
                items(Difficulty.entries) { difficulty ->
                    SelectableChip(
                        label = difficulty.name.take(1),
                        selected = state.selectedDifficulty == difficulty,
                        onClick = { viewModel.setDifficulty(if (state.selectedDifficulty == difficulty) null else difficulty) },
                    )
                }
            }
            Spacer(modifier = Modifier.padding(top = Spacing.sm))

            if (state.displayedHabits.isEmpty()) {
                EmptyState(
                    emoji = if (state.showArchived) "🗄️" else "🌱",
                    title = stringResource(if (state.showArchived) R.string.habits_empty_archived_title else R.string.habits_empty_title),
                    description = stringResource(if (state.showArchived) R.string.habits_empty_archived_description else R.string.habits_empty_description),
                    actionLabel = if (state.showArchived) null else stringResource(R.string.action_create_first_habit),
                    onAction = if (state.showArchived) null else onCreateHabit,
                )
            } else if (isFiltering || state.showArchived) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(state.displayedHabits, key = { it.id }) { habit ->
                        HabitRow(
                            habit = habit,
                            isArchived = state.showArchived,
                            onOpenDetail = { onOpenDetail(habit.id) },
                            onArchive = { viewModel.archive(habit.id) },
                            onUnarchive = { viewModel.unarchive(habit.id) },
                            onRequestDelete = { habitPendingDeletion = habit },
                        )
                    }
                }
            } else {
                ReorderableHabitList(
                    habits = state.displayedHabits,
                    onReorder = viewModel::reorder,
                    modifier = Modifier.fillMaxSize(),
                ) { habit ->
                    HabitRow(
                        habit = habit,
                        isArchived = false,
                        onOpenDetail = { onOpenDetail(habit.id) },
                        onArchive = { viewModel.archive(habit.id) },
                        onUnarchive = { viewModel.unarchive(habit.id) },
                        onRequestDelete = { habitPendingDeletion = habit },
                    )
                }
            }
        }
    }

    val pending = habitPendingDeletion
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { habitPendingDeletion = null },
            title = { Text(stringResource(R.string.habits_delete_confirm_title)) },
            text = { Text(stringResource(R.string.habits_delete_confirm_message, pending.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePermanently(pending.id); habitPendingDeletion = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { habitPendingDeletion = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun HabitRow(
    habit: Habit,
    isArchived: Boolean,
    onOpenDetail: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(habit.category.color().copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(habit.icon, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.padding(start = Spacing.sm))
        Text(habit.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more_options))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (isArchived) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_unarchive)) },
                        leadingIcon = { Icon(Icons.Filled.Unarchive, contentDescription = null) },
                        onClick = { menuExpanded = false; onUnarchive() },
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_archive)) },
                        leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                        onClick = { menuExpanded = false; onArchive() },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete_permanently)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = { menuExpanded = false; onRequestDelete() },
                )
            }
        }
    }
}
