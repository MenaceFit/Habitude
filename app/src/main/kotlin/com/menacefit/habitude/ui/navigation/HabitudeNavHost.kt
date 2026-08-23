package com.menacefit.habitude.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.menacefit.habitude.R
import com.menacefit.habitude.ui.calendar.CalendarScreen
import com.menacefit.habitude.ui.common.LocalAppContainer
import com.menacefit.habitude.ui.common.sharedAppViewModel
import com.menacefit.habitude.ui.dashboard.DashboardScreen
import com.menacefit.habitude.ui.goals.GoalsScreen
import com.menacefit.habitude.ui.habits.HabitEditScreen
import com.menacefit.habitude.ui.habits.HabitListScreen
import com.menacefit.habitude.ui.journal.JournalScreen
import com.menacefit.habitude.ui.onboarding.EngagementScreen
import com.menacefit.habitude.ui.onboarding.GoalsCategoryScreen
import com.menacefit.habitude.ui.onboarding.OnboardingViewModel
import com.menacefit.habitude.ui.onboarding.PersonalGoalScreen
import com.menacefit.habitude.ui.onboarding.SuggestionsScreen
import com.menacefit.habitude.ui.onboarding.WelcomeScreen
import com.menacefit.habitude.ui.profile.PrivacyScreen
import com.menacefit.habitude.ui.profile.ProfileScreen
import com.menacefit.habitude.ui.profile.SettingsScreen
import com.menacefit.habitude.ui.progress.ProgressScreen
import com.menacefit.habitude.ui.routines.RoutinesScreen
import com.menacefit.habitude.ui.stats.StatsScreen
import com.menacefit.habitude.util.stringResourceByKey
import kotlinx.coroutines.flow.first

private data class BottomTab(val route: String, val labelRes: Int, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.TODAY, R.string.nav_today, Icons.Filled.Home),
    BottomTab(Routes.HABITS, R.string.nav_habits, Icons.Filled.Checklist),
    BottomTab(Routes.STATS, R.string.nav_stats, Icons.Filled.BarChart),
    BottomTab(Routes.PROGRESS, R.string.nav_progress, Icons.Filled.EmojiEvents),
    BottomTab(Routes.PROFILE, R.string.nav_profile, Icons.Filled.Person),
)

@Composable
fun HabitudeNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in Routes.bottomNavRoutes) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.MAIN_GRAPH) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SPLASH) { SplashRoute(navController) }

            navigation(startDestination = Routes.ONBOARDING_WELCOME, route = Routes.ONBOARDING_GRAPH) {
                composable(Routes.ONBOARDING_WELCOME) {
                    WelcomeScreen(onStart = { navController.navigate(Routes.ONBOARDING_GOALS) })
                }
                composable(Routes.ONBOARDING_GOALS) {
                    val vm = onboardingViewModel(navController)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    GoalsCategoryScreen(
                        selectedCategories = state.selectedCategories,
                        onToggle = vm::toggleCategory,
                        onContinue = { navController.navigate(Routes.ONBOARDING_SUGGESTIONS) },
                    )
                }
                composable(Routes.ONBOARDING_SUGGESTIONS) {
                    val vm = onboardingViewModel(navController)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    SuggestionsScreen(
                        suggestions = state.suggestions,
                        acceptedIds = state.acceptedSuggestionIds,
                        onToggle = vm::toggleSuggestion,
                        onContinue = { navController.navigate(Routes.ONBOARDING_PERSONAL_GOAL) },
                    )
                }
                composable(Routes.ONBOARDING_PERSONAL_GOAL) {
                    val vm = onboardingViewModel(navController)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    PersonalGoalScreen(
                        goalText = state.personalGoalText,
                        onGoalTextChange = vm::setPersonalGoalText,
                        onContinue = { navController.navigate(Routes.ONBOARDING_ENGAGEMENT) },
                        onSkip = { navController.navigate(Routes.ONBOARDING_ENGAGEMENT) },
                    )
                }
                composable(Routes.ONBOARDING_ENGAGEMENT) {
                    val vm = onboardingViewModel(navController)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    val resolvedNames = state.suggestions
                        .filter { it.id in state.acceptedSuggestionIds }
                        .associate { it.id to stringResourceByKey(it.nameKey) }
                    EngagementScreen(
                        profileName = state.profileName,
                        avatarEmoji = state.avatarEmoji,
                        onNameChange = vm::setProfileName,
                        onAvatarChange = vm::setAvatarEmoji,
                        isSaving = state.isSaving,
                        onFinish = {
                            vm.completeOnboarding(resolvedNames) {
                                navController.navigate(Routes.MAIN_GRAPH) {
                                    popUpTo(Routes.ONBOARDING_GRAPH) { inclusive = true }
                                }
                            }
                        },
                    )
                }
            }

            navigation(startDestination = Routes.TODAY, route = Routes.MAIN_GRAPH) {
                composable(Routes.TODAY) {
                    DashboardScreen(
                        onCreateHabit = { navController.navigate(Routes.HABIT_CREATE) },
                        onOpenHabitDetail = { habitId -> navController.navigate(Routes.habitEdit(habitId)) },
                        onOpenProfile = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } },
                    )
                }
                composable(Routes.HABITS) {
                    HabitListScreen(
                        onCreateHabit = { navController.navigate(Routes.HABIT_CREATE) },
                        onOpenDetail = { habitId -> navController.navigate(Routes.habitEdit(habitId)) },
                    )
                }
                composable(Routes.STATS) {
                    StatsScreen(onOpenCalendar = { navController.navigate(Routes.CALENDAR) })
                }
                composable(Routes.PROGRESS) { ProgressScreen() }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenJournal = { navController.navigate(Routes.JOURNAL) },
                        onOpenGoals = { navController.navigate(Routes.GOALS) },
                        onOpenRoutines = { navController.navigate(Routes.ROUTINES) },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                        onOpenAchievements = { navController.navigate(Routes.PROGRESS) { launchSingleTop = true } },
                    )
                }
            }

            composable(Routes.HABIT_CREATE) {
                HabitEditScreen(habitId = null, onDone = { navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
            composable(
                Routes.HABIT_EDIT_PATTERN,
                arguments = listOf(navArgument(Routes.HABIT_EDIT_ARG) { type = NavType.StringType }),
            ) { entry ->
                val habitId = entry.arguments?.getString(Routes.HABIT_EDIT_ARG)
                HabitEditScreen(habitId = habitId, onDone = { navController.popBackStack() }, onBack = { navController.popBackStack() })
            }

            composable(Routes.CALENDAR) { CalendarScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.JOURNAL) { JournalScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.GOALS) { GoalsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ROUTINES) { RoutinesScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.PRIVACY) { PrivacyScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun onboardingViewModel(navController: NavHostController): OnboardingViewModel =
    sharedAppViewModel(navController, Routes.ONBOARDING_GRAPH) { c ->
        OnboardingViewModel(c.profileRepository, c.habitRepository, c.settingsRepository, c.timeProvider)
    }

@Composable
private fun SplashRoute(navController: NavHostController) {
    val container = LocalAppContainer.current
    LaunchedEffect(Unit) {
        val settings = container.settingsRepository.settings.first()
        val destination = if (settings.onboardingCompleted) Routes.MAIN_GRAPH else Routes.ONBOARDING_GRAPH
        navController.navigate(destination) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
