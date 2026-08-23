package com.menacefit.habitude.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.Routine
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.components.EmptyState
import com.menacefit.habitude.ui.components.PrimaryButton
import com.menacefit.habitude.ui.theme.ExtraShapes
import com.menacefit.habitude.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c -> RoutinesViewModel(c.routineRepository, c.habitRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.routines_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_create_routine))
            }
        },
    ) { padding ->
        if (state.routines.isEmpty()) {
            Box(modifier = Modifier.padding(padding)) {
                EmptyState(
                    emoji = "🔁",
                    title = stringResource(R.string.routines_empty_title),
                    description = stringResource(R.string.routines_empty_description),
                    actionLabel = stringResource(R.string.action_create_routine),
                    onAction = { showCreateDialog = true },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.lg, Spacing.lg, Spacing.lg, Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.routines, key = { it.id }) { routine ->
                    RoutineRow(routine, state.activeHabits, onDelete = { viewModel.deleteRoutine(routine.id) })
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRoutineDialog(
            habits = state.activeHabits,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, icon, habitIds ->
                viewModel.createRoutine(name, icon, habitIds)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun RoutineRow(routine: Routine, habits: List<Habit>, onDelete: () -> Unit) {
    val names = routine.habitIds.mapNotNull { id -> habits.firstOrNull { it.id == id } }.joinToString(" · ") { "${it.icon} ${it.name}" }
    Card(shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${routine.icon} ${routine.name}", style = MaterialTheme.typography.titleMedium)
                if (names.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(names, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete)) }
        }
    }
}

@Composable
private fun CreateRoutineDialog(habits: List<Habit>, onDismiss: () -> Unit, onCreate: (name: String, icon: String, habitIds: List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.routines_create_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.routines_name_label)) })
                Text(stringResource(R.string.routines_pick_habits), style = MaterialTheme.typography.titleSmall)
                habits.forEach { habit ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = habit.id in selectedIds,
                            onCheckedChange = { checked -> selectedIds = if (checked) selectedIds + habit.id else selectedIds - habit.id },
                        )
                        Text("${habit.icon} ${habit.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.action_create),
                enabled = name.isNotBlank() && selectedIds.isNotEmpty(),
                onClick = { onCreate(name, "🔁", selectedIds.toList()) },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
