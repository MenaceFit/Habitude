package com.menacefit.habitude.ui.navigation

/** Every destination in the app, in one place, so a typo in a route string shows up as "screen doesn't exist" immediately instead of silently. */
object Routes {
    const val SPLASH = "splash"

    const val ONBOARDING_GRAPH = "onboarding"
    const val ONBOARDING_WELCOME = "onboarding/welcome"
    const val ONBOARDING_GOALS = "onboarding/goals"
    const val ONBOARDING_PERSONAL_GOAL = "onboarding/personal_goal"
    const val ONBOARDING_SUGGESTIONS = "onboarding/suggestions"
    const val ONBOARDING_ENGAGEMENT = "onboarding/engagement"

    const val MAIN_GRAPH = "main"
    const val TODAY = "today"
    const val HABITS = "habits"
    const val STATS = "stats"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"

    const val HABIT_EDIT_ARG = "habitId"
    const val HABIT_CREATE = "habit_edit"
    const val HABIT_EDIT_PATTERN = "habit_edit/{$HABIT_EDIT_ARG}"
    fun habitEdit(habitId: String) = "habit_edit/$habitId"

    const val CALENDAR = "calendar"
    const val JOURNAL = "journal"
    const val GOALS = "goals"
    const val ROUTINES = "routines"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"

    val bottomNavRoutes = listOf(TODAY, HABITS, STATS, PROGRESS, PROFILE)
}
