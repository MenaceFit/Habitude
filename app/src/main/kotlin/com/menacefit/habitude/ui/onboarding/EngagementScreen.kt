package com.menacefit.habitude.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.menacefit.habitude.R
import com.menacefit.habitude.ui.components.EmojiSwatch
import com.menacefit.habitude.ui.components.PrimaryButton
import com.menacefit.habitude.ui.theme.Spacing

private val AVATAR_CHOICES = listOf("🙂", "🚀", "🦁", "🌟", "🐺", "🌙", "🔥", "🌱", "🎯", "⚡", "🦉", "🐢")

@Composable
fun EngagementScreen(
    profileName: String,
    avatarEmoji: String,
    onNameChange: (String) -> Unit,
    onAvatarChange: (String) -> Unit,
    isSaving: Boolean,
    onFinish: () -> Unit,
) {
    var launched by remember { mutableStateOf(false) }

    AnimatedContent(targetState = launched, label = "engagement", transitionSpec = { fadeIn() togetherWith fadeOut() }) { isLaunched ->
        if (!isLaunched) {
            Column(modifier = Modifier.fillMaxSize().padding(Spacing.lg)) {
                Text(stringResource(R.string.onboarding_engagement_title), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    stringResource(R.string.onboarding_engagement_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.lg))

                OutlinedTextField(
                    value = profileName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.onboarding_engagement_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                Text(stringResource(R.string.onboarding_engagement_avatar_label), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(Spacing.sm))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(AVATAR_CHOICES) { emoji ->
                        EmojiSwatch(emoji = emoji, selected = emoji == avatarEmoji, onClick = { onAvatarChange(emoji) })
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = stringResource(R.string.onboarding_engagement_finish_button),
                    onClick = { launched = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            LaunchProgress(onFinish)
        }
    }
}

@Composable
private fun LaunchProgress(onFinish: () -> Unit) {
    val progress = remember { mutableStateOf(0f) }
    val animated by animateFloatAsState(targetValue = progress.value, animationSpec = tween(900, easing = LinearEasing), label = "launchProgress")
    var hasFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        progress.value = 1f
    }
    LaunchedEffect(animated) {
        if (animated >= 0.999f && !hasFinished) {
            hasFinished = true
            onFinish()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🚀", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            stringResource(R.string.onboarding_launch_message),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.xl))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}
