package com.menacefit.habitude.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.menacefit.habitude.ui.theme.AccentColor
import com.menacefit.habitude.ui.theme.StreakFlame
import com.menacefit.habitude.ui.theme.XpGold
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(val angleDeg: Float, val distanceDp: Float, val sizeDp: Float, val color: Color)

/**
 * A short, self-contained particle burst + rising "+XP" label, fired once
 * per unique [triggerKey]. Kept intentionally quick (~700ms total) so it
 * reads as satisfying rather than something that slows the app down (spec
 * section 33: "animations rapides et premium, éviter les animations
 * inutiles qui ralentissent l'application").
 */
@Composable
fun CelebrationOverlay(
    triggerKey: Any?,
    xpAwarded: Int,
    modifier: Modifier = Modifier,
    accent: AccentColor = AccentColor.default,
) {
    val progress = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }
    val particles = remember(triggerKey) {
        if (triggerKey == null) emptyList() else buildParticles()
    }

    LaunchedEffect(triggerKey) {
        if (triggerKey == null) return@LaunchedEffect
        visible = true
        progress.snapTo(0f)
        launch {
            progress.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
            visible = false
        }
    }

    if (!visible) return

    val density = LocalDensity.current
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val eased = LinearOutSlowInEasing.transform(progress.value)
            val alpha = (1f - progress.value).coerceIn(0f, 1f)
            particles.forEach { particle ->
                val distancePx = with(density) { particle.distanceDp.dp.toPx() } * eased
                val x = center.x + cos(Math.toRadians(particle.angleDeg.toDouble())).toFloat() * distancePx
                val y = center.y + sin(Math.toRadians(particle.angleDeg.toDouble())).toFloat() * distancePx
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = with(density) { particle.sizeDp.dp.toPx() },
                    center = Offset(x, y),
                )
            }
        }

        val riseOffset = with(density) { (-40 * progress.value).dp.toPx() }
        Text(
            "+$xpAwarded XP",
            style = MaterialTheme.typography.headlineSmall,
            color = XpGold,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationY = riseOffset
                    this.alpha = (1f - progress.value).coerceIn(0f, 1f)
                },
        )
    }
}

private fun buildParticles(): List<Particle> {
    val palette = listOf(XpGold, StreakFlame, AccentColor.VIOLET.light, AccentColor.GREEN.light, AccentColor.BLUE.light)
    return List(18) {
        Particle(
            angleDeg = Random.nextFloat() * 360f,
            distanceDp = 60f + Random.nextFloat() * 60f,
            sizeDp = 3f + Random.nextFloat() * 4f,
            color = palette[Random.nextInt(palette.size)],
        )
    }
}
