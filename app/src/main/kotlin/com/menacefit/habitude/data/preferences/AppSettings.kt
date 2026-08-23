package com.menacefit.habitude.data.preferences

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage { SYSTEM, FRENCH, ENGLISH }
enum class WeekStartPreference { MONDAY, SUNDAY }
enum class TimeFormatPreference { HOUR_24, HOUR_12 }

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Key into the curated accent-color palette in the design system (see ui/theme/Color.kt). */
    val accentColorKey: String = "violet",
    val hapticsEnabled: Boolean = true,
    val soundsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val weekStart: WeekStartPreference = WeekStartPreference.MONDAY,
    val timeFormat: TimeFormatPreference = TimeFormatPreference.HOUR_24,
    val language: AppLanguage = AppLanguage.SYSTEM,
)
