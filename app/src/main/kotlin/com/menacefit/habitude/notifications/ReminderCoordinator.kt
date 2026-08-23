package com.menacefit.habitude.notifications

import android.content.Context
import com.menacefit.habitude.domain.model.Habit

/**
 * The seam between "a habit's reminder settings changed" and actually
 * (re)scheduling or cancelling its WorkManager request. Kept as a tiny
 * Context-wrapping coordinator (like [com.menacefit.habitude.util.SoundPlayer])
 * so ViewModels stay Context-free while still triggering a real side effect.
 */
class ReminderCoordinator(private val context: Context) {
    fun onHabitSaved(habit: Habit) {
        ReminderScheduler.schedule(context, habit)
    }

    fun onHabitRemoved(habitId: String) {
        ReminderScheduler.cancel(context, habitId)
    }
}
