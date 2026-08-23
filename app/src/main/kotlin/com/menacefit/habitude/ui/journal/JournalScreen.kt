package com.menacefit.habitude.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.model.JournalEntry
import com.menacefit.habitude.domain.model.MoodLevel
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.theme.ExtraShapes
import com.menacefit.habitude.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c -> JournalViewModel(c.moodJournalRepository, c.habitRepository, c.timeProvider) }
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
            item { Text(stringResource(R.string.journal_title), style = MaterialTheme.typography.headlineMedium) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(R.string.journal_mood_label), style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MoodLevel.entries.forEach { level ->
                            MoodButton(level, selected = state.mood == level, onClick = { viewModel.setMood(level) })
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(R.string.journal_energy_label, state.energy), style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = state.energy.toFloat(),
                        onValueChange = { viewModel.setEnergy(it.toInt()) },
                        valueRange = 0f..10f,
                        steps = 9,
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(R.string.journal_text_label), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.text,
                        onValueChange = viewModel::setText,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        placeholder = { Text(stringResource(R.string.journal_text_placeholder)) },
                    )
                }
            }

            state.moodCorrelation?.let { correlation ->
                item {
                    Card(shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        val key = if (correlation.scoreDeltaPercent >= 0) R.string.insight_mood_correlation_positive else R.string.insight_mood_correlation_negative
                        Text(
                            stringResource(key, correlation.habitName, kotlin.math.abs(correlation.scoreDeltaPercent)),
                            modifier = Modifier.padding(Spacing.md),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item { Text(stringResource(R.string.journal_history_title), style = MaterialTheme.typography.titleMedium) }

            val pastEntries = state.recentEntries.filter { it.date != state.today }
            if (pastEntries.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.journal_history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                val moodByDate = state.recentMoods.associateBy { it.date }
                items(pastEntries, key = { it.id }) { entry ->
                    JournalHistoryRow(entry = entry, moodEmoji = moodByDate[entry.date]?.mood?.emoji)
                }
            }
        }
    }
}

@Composable
private fun JournalHistoryRow(entry: JournalEntry, moodEmoji: String?) {
    Card(shape = ExtraShapes.card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            if (moodEmoji != null) {
                Text(moodEmoji, style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    entry.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    entry.text.ifBlank { stringResource(R.string.journal_history_no_text) },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MoodButton(level: MoodLevel, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) Modifier.background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium) else Modifier
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(background)
            .padding(Spacing.sm),
    ) {
        Text(level.emoji, style = MaterialTheme.typography.headlineSmall)
    }
}
