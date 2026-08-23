package com.menacefit.habitude.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.menacefit.habitude.domain.model.Habit
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Schedules one [HabitReminderWorker] tick per habit per day via WorkManager
 * (not [android.app.AlarmManager]): a reminder within a few minutes of the
 * chosen time is plenty precise for a habit nudge, and this avoids needing
 * the sensitive exact-alarm permission entirely. Each firing reschedules
 * itself for the next day, so this only needs to be called again when the
 * habit's reminder settings actually change (or on boot, defensively).
 */
object ReminderScheduler {

    fun uniqueWorkName(habitId: String) = "habit_reminder_$habitId"

    fun schedule(context: Context, habit: Habit, zoneId: ZoneId = ZoneId.systemDefault()) {
        val hour = habit.reminderHour
        val minute = habit.reminderMinute
        if (!habit.reminderEnabled || !habit.active || hour == null || minute == null) {
            cancel(context, habit.id)
            return
        }

        val delay = delayUntilNext(hour, minute, zoneId)
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(HabitReminderWorker.KEY_HABIT_ID, habit.id).build())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(uniqueWorkName(habit.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, habitId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(habitId))
    }

    internal fun delayUntilNext(hour: Int, minute: Int, zoneId: ZoneId, now: LocalDateTime = LocalDateTime.now(zoneId)): Duration {
        var target = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }
}
