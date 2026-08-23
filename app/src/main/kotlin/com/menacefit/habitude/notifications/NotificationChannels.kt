package com.menacefit.habitude.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.menacefit.habitude.R

object NotificationChannels {
    const val HABIT_REMINDERS = "habit_reminders"

    fun ensureCreated(context: Context) {
        val channel = NotificationChannel(
            HABIT_REMINDERS,
            context.getString(R.string.notification_channel_reminders_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
