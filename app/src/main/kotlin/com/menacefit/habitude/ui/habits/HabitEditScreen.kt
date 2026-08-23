package com.menacefit.habitude.ui.habits

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.HabitType
import com.menacefit.habitude.ui.common.appViewModel
import com.menacefit.habitude.ui.components.ColorSwatch
import com.menacefit.habitude.ui.components.EmojiSwatch
import com.menacefit.habitude.ui.components.PrimaryButton
import com.menacefit.habitude.ui.components.SelectableChip
import com.menacefit.habitude.ui.components.SimpleDatePickerDialog
import com.menacefit.habitude.ui.components.SimpleTimePickerDialog
import com.menacefit.habitude.ui.theme.AccentColor
import com.menacefit.habitude.ui.theme.Spacing
import com.menacefit.habitude.ui.theme.color
import com.menacefit.habitude.ui.theme.toHexString
import java.time.format.DateTimeFormatter

private val ICON_CHOICES = listOf(
    "🏃", "📚", "💧", "🧘", "🥗", "😴", "💻", "💰", "🎯", "🏋️", "🚶", "🧠", "🛏️", "📵",
    "🍳", "🙏", "🌳", "🗒️", "📖", "🎨", "🎸", "🚴", "🧴", "☀️", "🌙", "✍️", "🧹", "🐶",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditScreen(habitId: String?, onDone: () -> Unit, onBack: () -> Unit) {
    val viewModel = appViewModel { c -> HabitEditViewModel(c.habitRepository, c.timeProvider, c.reminderCoordinator, habitId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showReminderPicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showTimeTargetPicker by remember { mutableStateOf(false) }
    var newChecklistItemText by remember { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: if denied, reminders simply won't post — HabitudeNotifier already checks this before showing one. */ }
    val context = LocalContext.current

    if (!state.isLoaded) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.habit_edit_title_new else R.string.habit_edit_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionLabel(stringResource(R.string.habit_edit_section_basics))
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.habit_edit_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = { Text(stringResource(R.string.habit_edit_description_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            SectionLabel(stringResource(R.string.habit_edit_section_icon))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(ICON_CHOICES) { emoji ->
                    EmojiSwatch(emoji = emoji, selected = emoji == state.icon, onClick = { viewModel.setIcon(emoji) })
                }
            }

            SectionLabel(stringResource(R.string.habit_edit_section_color))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(AccentColor.entries) { accent ->
                    val hex = accent.light.toHexString()
                    ColorSwatch(color = accent.light, selected = state.colorHex.equals(hex, ignoreCase = true), onClick = { viewModel.setColorHex(hex) })
                }
            }

            SectionLabel(stringResource(R.string.habit_edit_section_category))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(HabitCategory.entries) { category ->
                    SelectableChip(
                        label = category.name,
                        emoji = category.defaultEmoji,
                        selected = category == state.category,
                        onClick = { viewModel.setCategory(category) },
                    )
                }
            }

            SectionLabel(stringResource(R.string.habit_edit_section_type))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(HabitType.entries) { type ->
                    SelectableChip(label = typeLabel(type), selected = type == state.type, onClick = { viewModel.setType(type) })
                }
            }
            TypeSpecificFields(state, viewModel, onOpenTimePicker = { showTimeTargetPicker = true })

            SectionLabel(stringResource(R.string.habit_edit_section_frequency))
            FrequencyPicker(state.frequency, viewModel::setFrequency)

            SectionLabel(stringResource(R.string.habit_edit_section_difficulty))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Difficulty.entries.forEach { difficulty ->
                    SelectableChip(
                        label = "${difficultyLabel(difficulty)} (+${difficulty.baseXp} XP)",
                        selected = difficulty == state.difficulty,
                        onClick = { viewModel.setDifficulty(difficulty) },
                    )
                }
            }

            SectionLabel(stringResource(R.string.habit_edit_section_reminder))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.habit_edit_reminder_enable))
                Switch(
                    checked = state.reminderEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setReminderEnabled(enabled)
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            if (state.reminderEnabled) {
                PrimaryButton(
                    text = String.format("%02d:%02d", state.reminderHour, state.reminderMinute),
                    onClick = { showReminderPicker = true },
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.habit_edit_sound_enable))
                Switch(checked = state.soundEnabled, onCheckedChange = viewModel::setSoundEnabled)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.habit_edit_vibration_enable))
                Switch(checked = state.vibrationEnabled, onCheckedChange = viewModel::setVibrationEnabled)
            }

            SectionLabel(stringResource(R.string.habit_edit_section_dates))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrimaryButton(
                    text = stringResource(R.string.habit_edit_start_date, state.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)),
                    onClick = { showStartDatePicker = true },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(
                    text = state.endDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: stringResource(R.string.habit_edit_end_date_none),
                    onClick = { showEndDatePicker = true },
                )
                if (state.endDate != null) {
                    IconButton(onClick = { viewModel.setEndDate(null) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove))
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = { viewModel.save(onDone) },
                enabled = state.isValid && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }

    if (showReminderPicker) {
        SimpleTimePickerDialog(
            initialHour = state.reminderHour,
            initialMinute = state.reminderMinute,
            onConfirm = { h, m -> viewModel.setReminderTime(h, m); showReminderPicker = false },
            onDismiss = { showReminderPicker = false },
        )
    }
    if (showStartDatePicker) {
        SimpleDatePickerDialog(
            initialDate = state.startDate,
            onConfirm = { viewModel.setStartDate(it) },
            onDismiss = { showStartDatePicker = false },
        )
    }
    if (showEndDatePicker) {
        SimpleDatePickerDialog(
            initialDate = state.endDate,
            onConfirm = { viewModel.setEndDate(it) },
            onDismiss = { showEndDatePicker = false },
        )
    }
    if (showTimeTargetPicker) {
        SimpleTimePickerDialog(
            initialHour = state.timeHour,
            initialMinute = state.timeMinute,
            onConfirm = { h, m -> viewModel.setTimeTarget(h, m, state.timeIsBefore); showTimeTargetPicker = false },
            onDismiss = { showTimeTargetPicker = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun TypeSpecificFields(state: HabitEditUiState, viewModel: HabitEditViewModel, onOpenTimePicker: () -> Unit) {
    when (state.type) {
        HabitType.BOOLEAN -> {}
        HabitType.QUANTITY -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            NumberField(stringResource(R.string.habit_edit_target_amount), state.quantityTarget, Modifier.weight(1f)) { viewModel.setQuantityTarget(it) }
            OutlinedTextField(
                value = state.quantityUnit,
                onValueChange = viewModel::setQuantityUnit,
                label = { Text(stringResource(R.string.habit_edit_unit)) },
                modifier = Modifier.weight(1f),
            )
        }
        HabitType.DURATION -> NumberField(stringResource(R.string.habit_edit_target_minutes), state.durationMinutes.toDouble(), Modifier.fillMaxWidth()) {
            viewModel.setDurationMinutes(it.toInt())
        }
        HabitType.COUNT -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            NumberField(stringResource(R.string.habit_edit_target_count), state.countTarget.toDouble(), Modifier.weight(1f)) { viewModel.setCountTarget(it.toInt()) }
            OutlinedTextField(
                value = state.countUnit,
                onValueChange = viewModel::setCountUnit,
                label = { Text(stringResource(R.string.habit_edit_unit)) },
                modifier = Modifier.weight(1f),
            )
        }
        HabitType.LIMIT -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            NumberField(stringResource(R.string.habit_edit_limit_max), state.limitMax, Modifier.weight(1f)) { viewModel.setLimitMax(it) }
            OutlinedTextField(
                value = state.limitUnit,
                onValueChange = viewModel::setLimitUnit,
                label = { Text(stringResource(R.string.habit_edit_unit)) },
                modifier = Modifier.weight(1f),
            )
        }
        HabitType.TIME -> Column {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SelectableChip(
                    label = stringResource(R.string.habit_edit_time_before),
                    selected = state.timeIsBefore,
                    onClick = { viewModel.setTimeTarget(state.timeHour, state.timeMinute, true) },
                )
                SelectableChip(
                    label = stringResource(R.string.habit_edit_time_after),
                    selected = !state.timeIsBefore,
                    onClick = { viewModel.setTimeTarget(state.timeHour, state.timeMinute, false) },
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            PrimaryButton(text = String.format("%02d:%02d", state.timeHour, state.timeMinute), onClick = onOpenTimePicker)
        }
        HabitType.CHECKLIST -> ChecklistEditor(state, viewModel)
    }
}

@Composable
private fun ChecklistEditor(state: HabitEditUiState, viewModel: HabitEditViewModel) {
    var text by remember { mutableStateOf("") }
    Column {
        state.checklistItems.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(item.label, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { viewModel.removeChecklistItem(item.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.habit_edit_checklist_add_item)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            PrimaryButton(text = stringResource(R.string.action_add), onClick = { viewModel.addChecklistItem(text); text = "" })
        }
    }
}

@Composable
private fun NumberField(label: String, value: Double, modifier: Modifier = Modifier, onValueChange: (Double) -> Unit) {
    // No key on remember: the whole form is gated behind state.isLoaded, so
    // `value` is already final by this composable's first composition —
    // re-keying on every change would fight the user mid-edit (e.g. typing
    // "2." snapping back to "2" and blocking decimal entry).
    var text by remember { mutableStateOf(if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            newText.toDoubleOrNull()?.let(onValueChange)
        },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

@Composable
private fun FrequencyPicker(frequency: Frequency, onChange: (Frequency) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SelectableChip(stringResource(R.string.frequency_daily), frequency is Frequency.Daily, { onChange(Frequency.Daily) })
            SelectableChip(stringResource(R.string.frequency_specific_days), frequency is Frequency.SpecificDays, { onChange(Frequency.SpecificDays(setOf(1, 2, 3, 4, 5))) })
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SelectableChip(stringResource(R.string.frequency_times_per_week), frequency is Frequency.TimesPerWeek, { onChange(Frequency.TimesPerWeek(3)) })
            SelectableChip(stringResource(R.string.frequency_times_per_month), frequency is Frequency.TimesPerMonth, { onChange(Frequency.TimesPerMonth(4)) })
            SelectableChip(stringResource(R.string.frequency_every_n_days), frequency is Frequency.EveryNDays, { onChange(Frequency.EveryNDays(2)) })
        }

        when (frequency) {
            is Frequency.SpecificDays -> {
                Spacer(modifier = Modifier.height(Spacing.sm))
                WeekdayPicker(frequency.isoDays) { onChange(Frequency.SpecificDays(it)) }
            }
            is Frequency.TimesPerWeek -> {
                Spacer(modifier = Modifier.height(Spacing.sm))
                StepperRow(stringResource(R.string.frequency_times_label), frequency.times, 1, 7) { onChange(Frequency.TimesPerWeek(it)) }
            }
            is Frequency.TimesPerMonth -> {
                Spacer(modifier = Modifier.height(Spacing.sm))
                StepperRow(stringResource(R.string.frequency_times_label), frequency.times, 1, 28) { onChange(Frequency.TimesPerMonth(it)) }
            }
            is Frequency.EveryNDays -> {
                Spacer(modifier = Modifier.height(Spacing.sm))
                StepperRow(stringResource(R.string.frequency_every_n_label), frequency.n, 2, 30) { onChange(Frequency.EveryNDays(it)) }
            }
            Frequency.Daily -> {}
        }
    }
}

@Composable
private fun WeekdayPicker(selectedIso: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val labels = listOf(
        R.string.weekday_mon_short, R.string.weekday_tue_short, R.string.weekday_wed_short,
        R.string.weekday_thu_short, R.string.weekday_fri_short, R.string.weekday_sat_short, R.string.weekday_sun_short,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        labels.forEachIndexed { index, resId ->
            val iso = index + 1
            SelectableChip(
                label = stringResource(resId),
                selected = iso in selectedIso,
                onClick = { onChange(if (iso in selectedIso) selectedIso - iso else selectedIso + iso) },
            )
        }
    }
}

@Composable
private fun StepperRow(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = { if (value > min) onChange(value - 1) }) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { if (value < max) onChange(value + 1) }) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

private fun typeLabel(type: HabitType): String = type.name.lowercase().replaceFirstChar { it.uppercase() }
private fun difficultyLabel(difficulty: Difficulty): String = difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
