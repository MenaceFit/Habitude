package com.menacefit.habitude.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.menacefit.habitude.data.preferences.ThemeMode

/** Consistent spacing scale — every padding/gap in the app traces back to one of these (spec section 31). */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

private fun lightScheme(accent: AccentColor) = lightColorScheme(
    primary = accent.light,
    onPrimary = Neutral.White,
    primaryContainer = accent.light.copy(alpha = 0.14f).compositeOver(Neutral.White),
    onPrimaryContainer = accent.light,
    secondary = Neutral.Grey700,
    onSecondary = Neutral.White,
    background = Neutral.Grey50,
    onBackground = Neutral.Grey950,
    surface = Neutral.White,
    onSurface = Neutral.Grey950,
    surfaceVariant = Neutral.Grey100,
    onSurfaceVariant = Neutral.Grey600,
    outline = Neutral.Grey300,
    outlineVariant = Neutral.Grey200,
    error = DangerLight,
    onError = Neutral.White,
)

private fun darkScheme(accent: AccentColor) = darkColorScheme(
    primary = accent.dark,
    onPrimary = Neutral.Black,
    primaryContainer = accent.dark.copy(alpha = 0.18f).compositeOver(Neutral.Black),
    onPrimaryContainer = accent.dark,
    secondary = Neutral.Grey300,
    onSecondary = Neutral.Black,
    background = Neutral.Black,
    onBackground = Neutral.Grey50,
    surface = Neutral.Grey950,
    onSurface = Neutral.Grey50,
    surfaceVariant = Neutral.Grey800,
    onSurfaceVariant = Neutral.Grey300,
    outline = Neutral.Grey700,
    outlineVariant = Neutral.Grey800,
    error = DangerDark,
    onError = Neutral.Black,
)

@Composable
fun HabitudeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.default,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) darkScheme(accentColor) else lightScheme(accentColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
