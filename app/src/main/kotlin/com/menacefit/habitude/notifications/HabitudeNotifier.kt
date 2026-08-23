package com.menacefit.habitude.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.menacefit.habitude.MainActivity
import com.menacefit.habitude.R
import com.menacefit.habitude.domain.model.Habit

object HabitudeNotifier {

    fun show(context: Context, habit: Habit, message: ReminderMessage) {
        val hasPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val (title, body) = textFor(context, message)
        val contentIntent = PendingIntent.getActivity(
            context,
            habit.id.hashCode(),
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.HABIT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(habit.id.hashCode(), notification)
    }

    private fun textFor(context: Context, message: ReminderMessage): Pair<String, String> {
        val title = context.getString(R.string.app_name)
        val body = when (message.type) {
            ReminderMessageType.RECORD_WITHIN_REACH -> context.getString(R.string.reminder_record_within_reach, message.habitName)
            ReminderMessageType.EVENING_ROUTINE_PENDING -> context.getString(R.string.reminder_evening_routine_pending)
            ReminderMessageType.STREAK_WAITING -> context.getString(R.string.reminder_streak_waiting, message.count, message.habitName)
            ReminderMessageType.HABITS_REMAINING -> context.getString(R.string.reminder_habits_remaining, message.count)
            ReminderMessageType.GENERIC -> context.getString(R.string.reminder_generic, message.habitName)
        }
        return title to body
    }
}
