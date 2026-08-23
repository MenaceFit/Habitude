package com.menacefit.habitude.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.menacefit.habitude.ui.theme.Spacing
import kotlin.math.roundToInt

data class LineChartPoint(val label: String, val value: Float)

/**
 * A single-series line chart with a soft gradient fill, drawn on a plain
 * Canvas (no charting dependency). Tap or drag along the line to see the
 * nearest point's exact value — the "interactive" requirement without
 * needing pinch-zoom (period changes are driven by the screen's own period
 * selector instead).
 */
@Composable
fun LineChart(
    points: List<LineChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() },
    maxLabels: Int = 6,
) {
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        if (points.isEmpty()) {
            Text(text = "", modifier = Modifier.height(160.dp))
            return@Box
        }
        val maxValue = (points.maxOf { it.value }).coerceAtLeast(1f)
        val minValue = 0f

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = Spacing.xs)
                .pointerInput(points) {
                    detectTapGestures { offset -> selectedIndex = nearestIndex(offset.x, size.width.toFloat(), points.size) }
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDrag = { change, _ -> selectedIndex = nearestIndex(change.position.x, size.width.toFloat(), points.size) },
                    )
                },
        ) {
            val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
            val chartHeight = size.height
            fun yFor(value: Float): Float {
                val ratio = (value - minValue) / (maxValue - minValue)
                return chartHeight - (ratio * chartHeight)
            }

            val linePath = Path()
            val fillPath = Path()
            points.forEachIndexed { index, point ->
                val x = if (points.size == 1) size.width / 2f else index * stepX
                val y = yFor(point.value)
                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, chartHeight)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (index == points.lastIndex) {
                    fillPath.lineTo(x, chartHeight)
                    fillPath.close()
                }
            }

            drawPath(fillPath, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.22f), Color.Transparent)))
            drawPath(linePath, color = lineColor, style = Stroke(width = 3.dp.toPx()))

            // Baseline grid line at zero.
            drawLine(
                color = lineColor.copy(alpha = 0.15f),
                start = Offset(0f, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )

            selectedIndex?.let { idx ->
                val point = points.getOrNull(idx) ?: return@let
                val x = if (points.size == 1) size.width / 2f else idx * stepX
                val y = yFor(point.value)
                drawLine(lineColor.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, chartHeight), strokeWidth = 1.dp.toPx())
                drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(x, y))
                drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
            }
        }

        selectedIndex?.let { idx ->
            val point = points.getOrNull(idx)
            if (point != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Text(
                        "${point.label}: ${valueFormatter(point.value)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            ChartXLabels(points.map { it.label }, maxLabels)
        }
    }
}

@Composable
private fun ChartXLabels(labels: List<String>, maxLabels: Int) {
    if (labels.isEmpty()) return
    val step = (labels.size / maxLabels).coerceAtLeast(1)
    val shown = labels.filterIndexed { index, _ -> index % step == 0 }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        shown.forEach { label ->
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun nearestIndex(x: Float, width: Float, count: Int): Int {
    if (count <= 1) return 0
    val stepX = width / (count - 1)
    return (x / stepX).roundToInt().coerceIn(0, count - 1)
}
