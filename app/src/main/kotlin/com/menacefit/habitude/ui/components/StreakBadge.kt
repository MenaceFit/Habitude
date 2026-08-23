package com.menacefit.habitude.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.menacefit.habitude.ui.theme.Spacing
import com.menacefit.habitude.ui.theme.StreakFlame

@Composable
fun StreakBadge(days: Int, modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(StreakFlame.copy(alpha = 0.14f))
            .padding(horizontal = if (compact) Spacing.sm else Spacing.md, vertical = if (compact) 4.dp else Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔥", style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium)
        Text(
            " $days",
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
            color = StreakFlame,
        )
    }
}
