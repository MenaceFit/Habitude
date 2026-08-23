package com.menacefit.habitude.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.R
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.components.XpBar
import com.menacefit.habitude.ui.theme.ExtraShapes
import com.menacefit.habitude.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAchievements: () -> Unit,
) {
    val viewModel = appViewModel { c -> ProfileViewModel(c.profileRepository, c.habitRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg, Spacing.lg, Spacing.lg, Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.avatarEmoji, style = MaterialTheme.typography.displayMedium)
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(state.name.ifBlank { stringResource(R.string.profile_default_name) }, style = MaterialTheme.typography.headlineSmall)
                state.createdAt?.let { createdAt ->
                    Text(
                        stringResource(
                            R.string.profile_member_since,
                            createdAt.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            XpBar(levelInfo = state.levelInfo, levelLabel = stringResource(R.string.level_label, state.levelInfo.level), modifier = Modifier.fillMaxWidth())
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                StatTile(stringResource(R.string.profile_stat_best_streak), "🔥 ${state.bestStreakEver}", Modifier.weight(1f))
                StatTile(stringResource(R.string.profile_stat_completed), state.totalHabitsCompleted.toString(), Modifier.weight(1f))
                StatTile(stringResource(R.string.profile_stat_badges), "${state.unlockedCount}/${state.totalAchievements}", Modifier.weight(1f))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ProfileMenuRow(Icons.Filled.EmojiEvents, stringResource(R.string.profile_menu_achievements), onOpenAchievements)
                ProfileMenuRow(Icons.Filled.Repeat, stringResource(R.string.profile_menu_routines), onOpenRoutines)
                ProfileMenuRow(Icons.Filled.Flag, stringResource(R.string.profile_menu_goals), onOpenGoals)
                ProfileMenuRow(Icons.AutoMirrored.Filled.MenuBook, stringResource(R.string.profile_menu_journal), onOpenJournal)
                ProfileMenuRow(Icons.Filled.Lock, stringResource(R.string.profile_menu_privacy), onOpenPrivacy)
                ProfileMenuRow(Icons.Filled.Settings, stringResource(R.string.profile_menu_settings), onOpenSettings)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileMenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
