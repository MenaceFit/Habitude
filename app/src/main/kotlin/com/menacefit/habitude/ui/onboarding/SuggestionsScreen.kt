package com.menacefit.habitude.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.onboarding.SuggestedHabitTemplate
import com.menacefit.habitude.ui.components.EmptyState
import com.menacefit.habitude.ui.components.PrimaryButton
import com.menacefit.habitude.ui.theme.ExtraShapes
import com.menacefit.habitude.ui.theme.Spacing
import com.menacefit.habitude.ui.theme.color
import com.menacefit.habitude.util.stringResourceByKey

@Composable
fun SuggestionsScreen(
    suggestions: List<SuggestedHabitTemplate>,
    acceptedIds: Set<String>,
    onToggle: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.lg)) {
        Text(stringResource(R.string.onboarding_suggestions_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.onboarding_suggestions_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        if (suggestions.isEmpty()) {
            EmptyState(
                emoji = "🤔",
                title = stringResource(R.string.onboarding_suggestions_empty_title),
                description = stringResource(R.string.onboarding_suggestions_empty_description),
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(suggestions, key = { it.id }) { template ->
                    val accepted = template.id in acceptedIds
                    Card(
                        onClick = { onToggle(template.id) },
                        shape = ExtraShapes.card,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(template.category.color().copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(template.icon, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                stringResourceByKey(template.nameKey),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                                textDecoration = if (accepted) TextDecoration.None else TextDecoration.LineThrough,
                                color = if (accepted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (accepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (accepted) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        PrimaryButton(text = stringResource(R.string.action_continue), onClick = onContinue, modifier = Modifier.fillMaxWidth())
    }
}
