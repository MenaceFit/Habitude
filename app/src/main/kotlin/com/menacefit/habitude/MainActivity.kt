package com.menacefit.habitude

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menacefit.habitude.data.preferences.AppLanguage
import com.menacefit.habitude.data.preferences.AppSettings
import com.menacefit.habitude.ui.common.LocalAppContainer
import com.menacefit.habitude.ui.navigation.HabitudeNavHost
import com.menacefit.habitude.ui.theme.AccentColor
import com.menacefit.habitude.ui.theme.HabitudeTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Habitude)

        val container = (application as HabitudeApplication).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
                LocalizedContent(language = settings.language) {
                    HabitudeTheme(
                        themeMode = settings.themeMode,
                        accentColor = AccentColor.fromKey(settings.accentColorKey),
                    ) {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            HabitudeNavHost()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Forces [stringResource] (and every other resource lookup) below it to
 * resolve against [language] instead of the device's locale, so the
 * "Français / English / Système" setting takes effect immediately without
 * an activity restart. [AppLanguage.SYSTEM] passes the device context
 * through unchanged.
 */
@Composable
private fun LocalizedContent(language: AppLanguage, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val configuration = LocalConfiguration.current
    val localizedContext = remember(baseContext, configuration, language) {
        val locale = when (language) {
            AppLanguage.FRENCH -> Locale.FRENCH
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.SYSTEM -> null
        }
        if (locale == null) {
            baseContext
        } else {
            val localizedConfiguration = Configuration(configuration).apply { setLocale(locale) }
            baseContext.createConfigurationContext(localizedConfiguration)
        }
    }
    CompositionLocalProvider(LocalContext provides localizedContext) {
        content()
    }
}
