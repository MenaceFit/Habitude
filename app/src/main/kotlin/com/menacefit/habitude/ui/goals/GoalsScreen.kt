package com.menacefit.habitude.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.model.Goal
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.components.EmptyState
import com.menacefit.habitude.ui.components.PrimaryButton
import com.menacefit.habitude.ui.theme.ExtraShapes
import com.menacefit.habitude.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c -> GoalsViewModel(c.goalRepository, c.timeProvider) }
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.goals_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_create_goal))
            }
        },
    ) { padding ->
        if (goals.isEmpty()) {
            Box(modifier = Modifier.padding(padding)) {
                EmptyState(
                    emoji = "🎯",
                    title = stringResource(R.string.goals_empty_title),
                    description = stringResource(R.string.goals_empty_description),
                    actionLabel = stringResource(R.string.action_create_goal),
                    onAction = { showCreateDialog = true },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.lg, Spacing.lg, Spacing.lg, Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(goal, onIncrement = { viewModel.incrementProgress(goal) }, onDecrement = { viewModel.decrementProgress(goal) })
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, target ->
                viewModel.createGoal(title, "🎯", target, null)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun GoalCard(goal: Goal, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Card(shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("${goal.icon} ${goal.title}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(Spacing.xs))
            LinearProgressIndicator(
                progress = { goal.progressRatio },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${goal.currentValue} / ${goal.targetValue}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    IconButton(onClick = onDecrement) { Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.action_decrement)) }
                    IconButton(onClick = onIncrement) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_increment)) }
                }
            }
        }
    }
}

@Composable
private fun CreateGoalDialog(onDismiss: () -> Unit, onCreate: (title: String, target: Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goals_create_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.goals_name_label)) })
                OutlinedTextField(value = target, onValueChange = { target = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.goals_target_label)) })
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.action_create),
                enabled = title.isNotBlank() && (target.toIntOrNull() ?: 0) > 0,
                onClick = { onCreate(title, target.toIntOrNull() ?: 0) },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
