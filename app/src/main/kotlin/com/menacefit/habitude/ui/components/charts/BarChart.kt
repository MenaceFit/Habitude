package com.menacefit.habitude.ui.components.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.menacefit.habitude.ui.theme.Spacing
import kotlin.math.roundToInt

data class BarChartEntry(val label: String, val icon: String, val ratio: Float, val color: Color)

/** A ranked horizontal-bar comparison (success rate per habit, category comparison) — deliberately simple and legible over decorative. */
@Composable
fun HorizontalBarChart(entries: List<BarChartEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        entries.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.icon, modifier = Modifier.width(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text("${(entry.ratio * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = entry.color)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { entry.ratio.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = entry.color,
                        trackColor = entry.color.copy(alpha = 0.15f),
                    )
                }
            }
        }
    }
}
