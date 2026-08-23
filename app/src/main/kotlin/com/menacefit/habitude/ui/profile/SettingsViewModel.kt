package com.menacefit.habitude.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.preferences.AppLanguage
import com.menacefit.habitude.data.preferences.AppSettings
import com.menacefit.habitude.data.preferences.SettingsRepository
import com.menacefit.habitude.data.preferences.ThemeMode
import com.menacefit.habitude.data.preferences.TimeFormatPreference
import com.menacefit.habitude.data.preferences.WeekStartPreference
import com.menacefit.habitude.data.repository.ExportImportRepository
import com.menacefit.habitude.data.repository.ImportMode
import com.menacefit.habitude.data.repository.ImportResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val exportImportRepository: ExportImportRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setThemeMode(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }
    fun setAccentColorKey(key: String) = launch { settingsRepository.setAccentColorKey(key) }
    fun setHapticsEnabled(enabled: Boolean) = launch { settingsRepository.setHapticsEnabled(enabled) }
    fun setSoundsEnabled(enabled: Boolean) = launch { settingsRepository.setSoundsEnabled(enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = launch { settingsRepository.setNotificationsEnabled(enabled) }
    fun setWeekStart(pref: WeekStartPreference) = launch { settingsRepository.setWeekStart(pref) }
    fun setTimeFormat(pref: TimeFormatPreference) = launch { settingsRepository.setTimeFormat(pref) }
    fun setLanguage(language: AppLanguage) = launch { settingsRepository.setLanguage(language) }

    suspend fun exportJson(): String = exportImportRepository.exportToJson()

    suspend fun exportCsv(): String = exportImportRepository.exportCompletionsToCsv()

    suspend fun importJson(json: String, mode: ImportMode): ImportResult = exportImportRepository.importFromJson(json, mode)

    fun resetAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            exportImportRepository.resetAllData()
            onDone()
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
