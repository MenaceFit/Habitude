package com.menacefit.habitude.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Extra radii for shapes outside Material3's five-step scale, kept together so every rounded corner in the app traces back to one source. */
object ExtraShapes {
    val card = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(percent = 50)
    val bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}
