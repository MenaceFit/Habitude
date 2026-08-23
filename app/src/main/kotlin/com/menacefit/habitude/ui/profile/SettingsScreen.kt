package com.menacefit.habitude.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.R
import com.menacefit.habitude.data.preferences.AppLanguage
import com.menacefit.habitude.data.preferences.ThemeMode
import com.menacefit.habitude.data.preferences.TimeFormatPreference
import com.menacefit.habitude.data.preferences.WeekStartPreference
import com.menacefit.habitude.data.repository.ImportMode
import com.menacefit.habitude.data.repository.ImportResult
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.components.ColorSwatch
import com.menacefit.habitude.ui.components.SelectableChip
import com.menacefit.habitude.ui.theme.AccentColor
import com.menacefit.habitude.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c -> SettingsViewModel(c.settingsRepository, c.exportImportRepository) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showResetConfirm by remember { mutableStateOf(false) }
    var importResultMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream).use { it.write(json) }
                }
            }
        }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val csv = viewModel.exportCsv()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream).use { it.write(csv) }
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                }
                if (text != null) {
                    when (val result = viewModel.importJson(text, ImportMode.MERGE)) {
                        is ImportResult.Success -> importResultMessage =
                            context.getString(R.string.settings_import_success, result.habitsImported, result.completionsImported)
                        is ImportResult.Failure -> importResultMessage = context.getString(R.string.settings_import_failure)
                    }
                }
            }
        }
    }

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
            item { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium) }

            item {
                SettingsSection(stringResource(R.string.settings_section_appearance)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SelectableChip(stringResource(R.string.theme_system), settings.themeMode == ThemeMode.SYSTEM, onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) })
                        SelectableChip(stringResource(R.string.theme_light), settings.themeMode == ThemeMode.LIGHT, onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) })
                        SelectableChip(stringResource(R.string.theme_dark), settings.themeMode == ThemeMode.DARK, onClick = { viewModel.setThemeMode(ThemeMode.DARK) })
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        items(AccentColor.entries) { accent ->
                            ColorSwatch(color = accent.light, selected = settings.accentColorKey == accent.key, onClick = { viewModel.setAccentColorKey(accent.key) })
                        }
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.settings_section_feedback)) {
                    SettingsSwitchRow(stringResource(R.string.settings_haptics), settings.hapticsEnabled, viewModel::setHapticsEnabled)
                    SettingsSwitchRow(stringResource(R.string.settings_sounds), settings.soundsEnabled, viewModel::setSoundsEnabled)
                    SettingsSwitchRow(stringResource(R.string.settings_notifications), settings.notificationsEnabled, viewModel::setNotificationsEnabled)
                }
            }

            item {
                SettingsSection(stringResource(R.string.settings_section_preferences)) {
                    Text(stringResource(R.string.settings_week_start), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SelectableChip(stringResource(R.string.week_start_monday), settings.weekStart == WeekStartPreference.MONDAY, onClick = { viewModel.setWeekStart(WeekStartPreference.MONDAY) })
                        SelectableChip(stringResource(R.string.week_start_sunday), settings.weekStart == WeekStartPreference.SUNDAY, onClick = { viewModel.setWeekStart(WeekStartPreference.SUNDAY) })
                    }
                    Text(stringResource(R.string.settings_time_format), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SelectableChip("24h", settings.timeFormat == TimeFormatPreference.HOUR_24, onClick = { viewModel.setTimeFormat(TimeFormatPreference.HOUR_24) })
                        SelectableChip("12h", settings.timeFormat == TimeFormatPreference.HOUR_12, onClick = { viewModel.setTimeFormat(TimeFormatPreference.HOUR_12) })
                    }
                    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SelectableChip(stringResource(R.string.language_system), settings.language == AppLanguage.SYSTEM, onClick = { viewModel.setLanguage(AppLanguage.SYSTEM) })
                        SelectableChip(stringResource(R.string.language_french), settings.language == AppLanguage.FRENCH, onClick = { viewModel.setLanguage(AppLanguage.FRENCH) })
                        SelectableChip(stringResource(R.string.language_english), settings.language == AppLanguage.ENGLISH, onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) })
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.settings_section_data)) {
                    SettingsActionRow(stringResource(R.string.settings_export_data)) { exportLauncher.launch("habitude_export.json") }
                    SettingsActionRow(stringResource(R.string.settings_export_csv)) { exportCsvLauncher.launch("habitude_completions.csv") }
                    SettingsActionRow(stringResource(R.string.settings_import_data)) { importLauncher.launch(arrayOf("application/json")) }
                    importResultMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }

            item {
                SettingsSection(stringResource(R.string.settings_section_danger)) {
                    SettingsActionRow(stringResource(R.string.settings_reset_data), isDestructive = true) { showResetConfirm = true }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAllData { showResetConfirm = false } }) {
                    Text(stringResource(R.string.settings_reset_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(label: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}
