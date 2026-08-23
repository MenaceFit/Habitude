package com.menacefit.habitude.ui.progress

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.achievement.AchievementCatalog
import com.menacefit.habitude.domain.model.AchievementType
import com.menacefit.habitude.domain.model.Challenge
import com.menacefit.habitude.domain.model.ChallengeStatus
import com.menacefit.habitude.domain.model.QuestInstance
import com.menacefit.habitude.domain.model.QuestMetric
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.components.PrimaryButton
import com.menacefit.habitude.ui.theme.ExtraShapes
import com.menacefit.habitude.ui.theme.Spacing
import com.menacefit.habitude.ui.theme.XpGold
import com.menacefit.habitude.util.stringResourceByKey
import java.time.LocalDate

@Composable
fun ProgressScreen() {
    val viewModel = appViewModel { c -> ProgressViewModel(c.questRepository, c.challengeRepository, c.profileRepository, c.habitRepository, c.timeProvider) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tabIndex by remember { mutableIntStateOf(0) }
    var showCreateChallenge by remember { mutableStateOf(false) }
    val tabs = listOf(R.string.progress_tab_quests, R.string.progress_tab_challenges, R.string.progress_tab_achievements)

    Scaffold(
        floatingActionButton = {
            if (tabIndex == 1) {
                FloatingActionButton(onClick = { showCreateChallenge = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_create_challenge))
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                stringResource(R.string.progress_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = Spacing.lg).padding(top = Spacing.sm, bottom = Spacing.sm),
            )
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, resId ->
                    Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(stringResource(resId)) })
                }
            }
            when (tabIndex) {
                0 -> QuestsTab(state.quests, state.activeHabits.associate { it.id to it.name })
                1 -> ChallengesTab(state.challenges, onCheckIn = viewModel::toggleChallengeCheckIn, onDelete = viewModel::deleteChallenge)
                else -> AchievementsTab(state.unlockedAchievements)
            }
        }
    }

    if (showCreateChallenge) {
        CreateChallengeDialog(
            onDismiss = { showCreateChallenge = false },
            onCreate = { title, days, xp ->
                viewModel.createChallenge(title, "", "🏆", days, null, xp)
                showCreateChallenge = false
            },
        )
    }
}

@Composable
private fun QuestsTab(quests: List<QuestInstance>, habitNameById: Map<String, String>) {
    if (quests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.progress_quests_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(quests, key = { it.id }) { quest ->
            Card(shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                            .background(if (quest.completed) XpGold else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (quest.completed) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(questText(quest, habitNameById), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (quest.currentProgress.toFloat() / quest.targetProgress.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("+${quest.xpReward}", style = MaterialTheme.typography.labelLarge, color = XpGold)
                }
            }
        }
    }
}

@Composable
private fun ChallengesTab(challenges: List<Challenge>, onCheckIn: (String) -> Unit, onDelete: (String) -> Unit) {
    if (challenges.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.progress_challenges_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(challenges, key = { it.id }) { challenge ->
            val today = LocalDate.now()
            val checkedToday = today in challenge.completedDates
            Card(shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${challenge.icon} ${challenge.title}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(statusLabel(challenge.status), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { onDelete(challenge.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    LinearProgressIndicator(
                        progress = { challenge.progressRatio },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        stringResource(R.string.progress_challenge_progress, challenge.completedDates.size, challenge.durationDays, challenge.xpReward),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (challenge.status == ChallengeStatus.ACTIVE) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        PrimaryButton(
                            text = stringResource(if (checkedToday) R.string.progress_challenge_checked_in else R.string.progress_challenge_check_in),
                            onClick = { onCheckIn(challenge.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun questText(quest: QuestInstance, habitNameById: Map<String, String>): String =
    if (quest.metric == QuestMetric.SPECIFIC_HABIT_COMPLETED) {
        val habitName = quest.metricParam?.let { habitNameById[it] }.orEmpty()
        stringResourceByKey(quest.descriptionKey, habitName)
    } else {
        stringResourceByKey(quest.descriptionKey, quest.metricParam ?: "")
    }

@Composable
private fun statusLabel(status: ChallengeStatus): String = when (status) {
    ChallengeStatus.ACTIVE -> stringResource(R.string.challenge_status_active)
    ChallengeStatus.COMPLETED -> stringResource(R.string.challenge_status_completed)
    ChallengeStatus.FAILED -> stringResource(R.string.challenge_status_failed)
    ChallengeStatus.ABANDONED -> stringResource(R.string.challenge_status_abandoned)
}

@Composable
private fun AchievementsTab(unlocked: Set<AchievementType>) {
    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(Spacing.lg)) {
        items(AchievementCatalog.all) { definition ->
            val isUnlocked = definition.type in unlocked
            Column(
                modifier = Modifier.padding(Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isUnlocked) XpGold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(definition.emoji, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.alpha(if (isUnlocked) 1f else 0.35f))
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    stringResourceByKey(definition.titleKey),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CreateChallengeDialog(onDismiss: () -> Unit, onCreate: (title: String, days: Int, xp: Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("7") }
    var xp by remember { mutableStateOf("200") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.progress_create_challenge_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.progress_challenge_name_label)) })
                OutlinedTextField(value = days, onValueChange = { days = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.progress_challenge_days_label)) })
                OutlinedTextField(value = xp, onValueChange = { xp = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.progress_challenge_xp_label)) })
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.action_create),
                enabled = title.isNotBlank() && days.toIntOrNull() != null && days.toIntOrNull() != 0,
                onClick = { onCreate(title, days.toIntOrNull() ?: 7, xp.toIntOrNull() ?: 0) },
            )
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
