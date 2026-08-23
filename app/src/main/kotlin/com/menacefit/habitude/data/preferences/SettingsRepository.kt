package com.menacefit.habitude.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "habitude_settings")

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccentColorKey(key: String)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setSoundsEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setWeekStart(pref: WeekStartPreference)
    suspend fun setTimeFormat(pref: TimeFormatPreference)
    suspend fun setLanguage(language: AppLanguage)
}

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val SOUNDS = booleanPreferencesKey("sounds_enabled")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val WEEK_START = stringPreferencesKey("week_start")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val LANGUAGE = stringPreferencesKey("language")
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.themeMode,
            accentColorKey = prefs[Keys.ACCENT_COLOR] ?: defaults.accentColorKey,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: defaults.hapticsEnabled,
            soundsEnabled = prefs[Keys.SOUNDS] ?: defaults.soundsEnabled,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: defaults.notificationsEnabled,
            weekStart = prefs[Keys.WEEK_START]?.let { runCatching { WeekStartPreference.valueOf(it) }.getOrNull() } ?: defaults.weekStart,
            timeFormat = prefs[Keys.TIME_FORMAT]?.let { runCatching { TimeFormatPreference.valueOf(it) }.getOrNull() } ?: defaults.timeFormat,
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: defaults.language,
        )
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setAccentColorKey(key: String) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = key }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    override suspend fun setSoundsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUNDS] = enabled }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    override suspend fun setWeekStart(pref: WeekStartPreference) {
        context.dataStore.edit { it[Keys.WEEK_START] = pref.name }
    }

    override suspend fun setTimeFormat(pref: TimeFormatPreference) {
        context.dataStore.edit { it[Keys.TIME_FORMAT] = pref.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }
}
