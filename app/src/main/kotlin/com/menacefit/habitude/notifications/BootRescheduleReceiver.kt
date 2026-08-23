package com.menacefit.habitude.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.menacefit.habitude.HabitudeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WorkManager persists and auto-resumes already-enqueued work across a
 * reboot on its own, so this isn't strictly required for correctness — it's
 * a defensive re-sync in case habit data changed while the app couldn't run
 * (e.g. right after a fresh install restore), re-deriving every reminder
 * schedule from the current habit list rather than trusting stale state.
 */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val container = (context.applicationContext as HabitudeApplication).container
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habits = container.habitRepository.getActiveHabitsOnce()
                habits.filter { it.reminderEnabled }.forEach { habit -> ReminderScheduler.schedule(context, habit) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
