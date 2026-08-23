package com.menacefit.habitude.widget

import android.content.Context

/** Tiny Context-wrapping seam so [com.menacefit.habitude.gamification.CompletionCoordinator] can nudge the widget after a completion without holding a Context itself. */
class WidgetRefresher(private val context: Context) {
    fun refresh() {
        HabitWidgetProvider.requestUpdate(context)
    }
}
