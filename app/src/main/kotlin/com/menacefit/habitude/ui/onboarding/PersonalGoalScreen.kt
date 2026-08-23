package com.menacefit.habitude.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.menacefit.habitude.R
import com.menacefit.habitude.ui.components.PrimaryButton
import com.menacefit.habitude.ui.components.TertiaryTextButton
import com.menacefit.habitude.ui.theme.Spacing

@Composable
fun PersonalGoalScreen(
    goalText: String,
    onGoalTextChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.lg)) {
        Text(stringResource(R.string.onboarding_personal_goal_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.onboarding_personal_goal_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        OutlinedTextField(
            value = goalText,
            onValueChange = onGoalTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.onboarding_personal_goal_placeholder)) },
            minLines = 3,
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(text = stringResource(R.string.action_continue), onClick = onContinue, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(Spacing.xs))
        TertiaryTextButton(text = stringResource(R.string.action_skip), onClick = onSkip, modifier = Modifier.fillMaxWidth())
    }
}
