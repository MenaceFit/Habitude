package com.menacefit.habitude.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.ui.components.PrimaryButton
import com.menacefit.habitude.ui.components.SelectableChip
import com.menacefit.habitude.ui.theme.Spacing

private fun categoryLabelRes(category: HabitCategory): Int = when (category) {
    HabitCategory.SPORT -> R.string.category_sport
    HabitCategory.DISCIPLINE -> R.string.category_discipline
    HabitCategory.STUDY -> R.string.category_study
    HabitCategory.WORK -> R.string.category_work
    HabitCategory.FINANCE -> R.string.category_finance
    HabitCategory.SLEEP -> R.string.category_sleep
    HabitCategory.NUTRITION -> R.string.category_nutrition
    HabitCategory.HEALTH -> R.string.category_health
    HabitCategory.SCREEN_TIME -> R.string.category_screen_time
    HabitCategory.WELLBEING -> R.string.category_wellbeing
    HabitCategory.PRODUCTIVITY -> R.string.category_productivity
    HabitCategory.OTHER -> R.string.category_other
}

@Composable
fun GoalsCategoryScreen(
    selectedCategories: Set<HabitCategory>,
    onToggle: (HabitCategory) -> Unit,
    onContinue: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.lg)) {
        Text(stringResource(R.string.onboarding_goals_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.onboarding_goals_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(bottom = Spacing.lg),
        ) {
            items(HabitCategory.entries) { category ->
                SelectableChip(
                    label = stringResource(categoryLabelRes(category)),
                    emoji = category.defaultEmoji,
                    selected = category in selectedCategories,
                    onClick = { onToggle(category) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        PrimaryButton(
            text = stringResource(R.string.action_continue),
            onClick = onContinue,
            enabled = selectedCategories.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
