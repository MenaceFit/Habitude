package com.menacefit.habitude.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.ui.theme.ExtraShapes
import com.menacefit.habitude.ui.theme.Spacing
import kotlin.math.roundToInt

private val ROW_HEIGHT = 64.dp

/**
 * A single-column list where every row can be dragged (via its handle,
 * after a long-press) to a new position. Rows keep their upstream order
 * whenever nothing is being dragged, and only apply a local reordering
 * while a drag is in flight — this avoids the list flow fighting the
 * gesture mid-drag while a completion elsewhere updates the underlying
 * data.
 */
@Composable
fun ReorderableHabitList(
    habits: List<Habit>,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    rowContent: @Composable (Habit) -> Unit,
) {
    var localOrder by remember { mutableStateOf(habits) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val rowHeightPx = with(density) { ROW_HEIGHT.toPx() }

    LaunchedEffect(habits, draggedId) {
        if (draggedId == null && localOrder.map { it.id } != habits.map { it.id }) {
            localOrder = habits
        }
    }

    LazyColumn(modifier = modifier) {
        items(localOrder, key = { it.id }) { habit ->
            val isDragged = habit.id == draggedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragged) dragOffsetPx else 0f
                        scaleX = if (isDragged) 1.02f else 1f
                        scaleY = if (isDragged) 1.02f else 1f
                    }
                    .background(
                        if (isDragged) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
                        ExtraShapes.card,
                    )
                    .padding(horizontal = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .pointerInput(habit.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedId = habit.id
                                    dragOffsetPx = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetPx += dragAmount.y
                                    val currentIndex = localOrder.indexOfFirst { it.id == habit.id }
                                    val targetIndex = (currentIndex + (dragOffsetPx / rowHeightPx).roundToInt())
                                        .coerceIn(0, localOrder.lastIndex)
                                    if (targetIndex != currentIndex) {
                                        val moved = localOrder.toMutableList()
                                        val item = moved.removeAt(currentIndex)
                                        moved.add(targetIndex, item)
                                        localOrder = moved
                                        dragOffsetPx -= (targetIndex - currentIndex) * rowHeightPx
                                    }
                                },
                                onDragEnd = {
                                    draggedId = null
                                    dragOffsetPx = 0f
                                    onReorder(localOrder.map { it.id })
                                },
                                onDragCancel = {
                                    draggedId = null
                                    dragOffsetPx = 0f
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.DragHandle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    rowContent(habit)
                }
            }
        }
    }
}
