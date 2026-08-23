package com.menacefit.habitude.ui.theme

import androidx.compose.ui.graphics.Color
import com.menacefit.habitude.domain.model.HabitCategory

// Neutral scale — the backbone of the "premium minimalist" look. Color is
// reserved for progress, category identity and feedback; the chrome around
// it stays deliberately quiet.
object Neutral {
    val Black = Color(0xFF0B0B0F)
    val Grey950 = Color(0xFF121218)
    val Grey900 = Color(0xFF17171F)
    val Grey800 = Color(0xFF232330)
    val Grey700 = Color(0xFF32323F)
    val Grey600 = Color(0xFF4A4A5A)
    val Grey500 = Color(0xFF6B6B7C)
    val Grey400 = Color(0xFF93939F)
    val Grey300 = Color(0xFFB8B8C4)
    val Grey200 = Color(0xFFDCDCE3)
    val Grey100 = Color(0xFFEFEFF3)
    val Grey50 = Color(0xFFF7F7FA)
    val White = Color(0xFFFFFFFF)
}

/** A curated accent palette the user picks their "couleur principale" from (spec section 37). Each key is stored verbatim in settings. */
enum class AccentColor(val key: String, val light: Color, val dark: Color) {
    VIOLET("violet", Color(0xFF6E56CF), Color(0xFF9B87F5)),
    BLUE("blue", Color(0xFF2170F7), Color(0xFF6EA8FF)),
    GREEN("green", Color(0xFF1A9E6B), Color(0xFF4FD69B)),
    ORANGE("orange", Color(0xFFE8720C), Color(0xFFFFA55C)),
    PINK("pink", Color(0xFFD6408F), Color(0xFFFF8AC1)),
    TEAL("teal", Color(0xFF0E9494), Color(0xFF5DD9D9));

    companion object {
        val default = VIOLET
        fun fromKey(key: String): AccentColor = entries.firstOrNull { it.key == key } ?: default
    }
}

// Semantic feedback colors — kept separate from category colors so
// "success/warning/danger" always reads the same regardless of theme.
val SuccessLight = Color(0xFF1A9E6B)
val SuccessDark = Color(0xFF4FD69B)
val WarningLight = Color(0xFFC98A00)
val WarningDark = Color(0xFFF2B93D)
val DangerLight = Color(0xFFD7373F)
val DangerDark = Color(0xFFFF6B6B)

val XpGold = Color(0xFFE3A62B)
val StreakFlame = Color(0xFFFF6A3D)

/** The inverse of Compose's `Color(0xAARRGGBB)` constructor — used wherever a color must be persisted as the `Habit.colorHex` string. */
fun Color.toHexString(): String {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

/** Stable per-category color identity, used on habit cards, charts and the category-comparison view. */
fun HabitCategory.color(): Color = when (this) {
    HabitCategory.SPORT -> Color(0xFFE8720C)
    HabitCategory.DISCIPLINE -> Color(0xFF6E56CF)
    HabitCategory.STUDY -> Color(0xFF2170F7)
    HabitCategory.WORK -> Color(0xFF4A4A5A)
    HabitCategory.FINANCE -> Color(0xFF1A9E6B)
    HabitCategory.SLEEP -> Color(0xFF5B4FC4)
    HabitCategory.NUTRITION -> Color(0xFF63A616)
    HabitCategory.HEALTH -> Color(0xFF0E9494)
    HabitCategory.SCREEN_TIME -> Color(0xFFD6408F)
    HabitCategory.WELLBEING -> Color(0xFF35B08A)
    HabitCategory.PRODUCTIVITY -> Color(0xFFC98A00)
    HabitCategory.OTHER -> Color(0xFF6B6B7C)
}
