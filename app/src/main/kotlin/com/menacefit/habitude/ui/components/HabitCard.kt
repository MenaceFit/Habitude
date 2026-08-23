package com.menacefit.habitude.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCompletion
import com.menacefit.habitude.domain.model.HabitTarget
import com.menacefit.habitude.domain.model.HabitType
import com.menacefit.habitude.domain.model.StreakResult
import com.menacefit.habitude.domain.model.progressRatio
import com.menacefit.habitude.ui.theme.ExtraShapes
import com.menacefit.habitude.ui.theme.Spacing
import com.menacefit.habitude.ui.theme.color

/** Everything the card needs to know about "what happens when the user acts on this habit right now" — the caller wires each to the [com.menacefit.habitude.gamification.CompletionCoordinator]. */
data class HabitCardActions(
    val onQuickComplete: () -> Unit,
    val onToggleChecklistItem: (itemId: String) -> Unit,
    val onOpenDetail: () -> Unit,
)

@Composable
fun HabitCard(
    habit: Habit,
    completion: HabitCompletion?,
    streak: StreakResult,
    actions: HabitCardActions,
    modifier: Modifier = Modifier,
) {
    val categoryColor = habit.category.color()
    val isCompleted = completion?.completed == true
    val value = completion?.value ?: 0.0
    val checkedIds = completion?.checklistCheckedIds ?: emptySet()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = actions.onOpenDetail),
        shape = ExtraShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(habit.icon, style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (streak.current > 0) StreakBadge(streak.current, compact = true)
                    Text(
                        subtitleFor(habit, value, checkedIds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (habit.type in PROGRESS_BAR_TYPES) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    LinearProgressIndicator(
                        progress = { habit.target.progressRatio(value, checkedIds) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = categoryColor,
                        trackColor = categoryColor.copy(alpha = 0.15f),
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            HabitCompletionControl(habit, isCompleted, categoryColor, actions)
        }

        AnimatedVisibility(visible = habit.type == HabitType.CHECKLIST) {
            val items = (habit.target as? HabitTarget.ChecklistTarget)?.items.orEmpty()
            Column(modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.sm)) {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.id in checkedIds, onCheckedChange = { actions.onToggleChecklistItem(item.id) })
                        Text(item.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private val PROGRESS_BAR_TYPES = setOf(HabitType.QUANTITY, HabitType.DURATION, HabitType.COUNT, HabitType.LIMIT)

@Composable
private fun HabitCompletionControl(habit: Habit, isCompleted: Boolean, categoryColor: Color, actions: HabitCardActions) {
    when (habit.type) {
        HabitType.BOOLEAN -> RoundCompletionButton(isCompleted, categoryColor, onClick = actions.onQuickComplete)
        HabitType.TIME -> IconButton(onClick = actions.onQuickComplete) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = if (isCompleted) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HabitType.CHECKLIST -> RoundCompletionButton(isCompleted, categoryColor, onClick = actions.onOpenDetail, icon = null)
        HabitType.QUANTITY, HabitType.DURATION, HabitType.COUNT, HabitType.LIMIT ->
            if (isCompleted) {
                RoundCompletionButton(true, categoryColor, onClick = actions.onQuickComplete)
            } else {
                IconButton(onClick = actions.onQuickComplete) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = categoryColor)
                }
            }
    }
}

@Composable
private fun RoundCompletionButton(isCompleted: Boolean, color: Color, onClick: () -> Unit, icon: Boolean? = true) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(if (isCompleted) Modifier.background(color) else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isCompleted && icon != false) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

private fun subtitleFor(habit: Habit, value: Double, checkedIds: Set<String>): String = when (val target = habit.target) {
    is HabitTarget.BooleanTarget -> habit.description.ifBlank { categoryLabel(habit) }
    is HabitTarget.QuantityTarget -> "${trimTrailingZero(value)} / ${trimTrailingZero(target.targetAmount)} ${target.unit}"
    is HabitTarget.DurationTarget -> "${value.toInt()} / ${target.targetMinutes} min"
    is HabitTarget.CountTarget -> "${value.toInt()} / ${target.targetCount} ${target.unit}".trim()
    is HabitTarget.LimitTarget -> "${trimTrailingZero(value)} / ${trimTrailingZero(target.maxAmount)} ${target.unit}"
    is HabitTarget.TimeTarget -> String.format("%02d:%02d", target.hour, target.minute)
    is HabitTarget.ChecklistTarget -> "${checkedIds.size} / ${target.items.size}"
}

private fun categoryLabel(habit: Habit): String = habit.category.name.lowercase().replaceFirstChar { it.uppercase() }

private fun trimTrailingZero(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
