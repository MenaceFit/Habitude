package com.menacefit.habitude.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.menacefit.habitude.di.AppContainer

/**
 * The manual-DI equivalent of `hiltViewModel()`: builds a one-off
 * [androidx.lifecycle.ViewModelProvider.Factory] from [create] and reads
 * the app's [AppContainer] from [LocalAppContainer], so every screen wires
 * its ViewModel in one line without a generated Hilt component.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = LocalAppContainer.current
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}

/**
 * Same as [appViewModel] but scoped to a parent nested-navigation-graph's
 * back stack entry instead of the current screen's — the standard
 * Navigation-Compose way to share one ViewModel across every screen in a
 * multi-step flow (onboarding), so it survives step-to-step navigation and
 * is cleared only when the whole flow is left.
 */
@Composable
inline fun <reified VM : ViewModel> sharedAppViewModel(navController: NavController, parentRoute: String, crossinline create: (AppContainer) -> VM): VM {
    val container = LocalAppContainer.current
    val parentEntry = remember(navController, parentRoute) { navController.getBackStackEntry(parentRoute) }
    return viewModel(viewModelStoreOwner = parentEntry, factory = viewModelFactory { initializer { create(container) } })
}
