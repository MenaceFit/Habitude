package com.menacefit.habitude.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.menacefit.habitude.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided — wrap the composable tree with CompositionLocalProvider(LocalAppContainer provides ...)")
}
