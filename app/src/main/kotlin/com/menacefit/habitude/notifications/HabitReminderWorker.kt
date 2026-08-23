package com.menacefit.habitude.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.menacefit.habitude.di.AppContainer
import com.menacefit.habitude.domain.model.isDueOn
import kotlinx.coroutines.flow.first

class HabitReminderWorker(
    context: Context,
    params: WorkerParameters,
    private val container: AppContainer,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString(KEY_HABIT_ID) ?: return Result.failure()
        val habit = container.habitRepository.getHabit(habitId) ?: return Result.success()

        if (!habit.active || !habit.reminderEnabled) return Result.success()

        val settings = container.settingsRepository.settings.first()
        if (!settings.notificationsEnabled) {
            ReminderScheduler.schedule(applicationContext, habit)
            return Result.success()
        }

        val today = container.timeProvider.todayLocalDate()
        if (!habit.isDueOn(today)) {
            ReminderScheduler.schedule(applicationContext, habit)
            return Result.success()
        }

        val existing = container.habitRepository.getCompletionForHabitAndDate(habit.id, today)
        if (existing?.completed == true) {
            ReminderScheduler.schedule(applicationContext, habit)
            return Result.success()
        }

        val streak = container.habitRepository.computeStreak(habit)
        val allHabits = container.habitRepository.getActiveHabitsOnce()
        val dueToday = allHabits.filter { it.isDueOn(today) }
        val completionsToday = container.habitRepository.getCompletionsForDate(today)
        val completedIds = completionsToday.filter { it.completed }.map { it.habitId }.toSet()
        val remaining = dueToday.count { it.id !in completedIds }

        val isEveningHabit = (habit.reminderHour ?: 0) >= 20
        val eveningPending = dueToday.any { (it.reminderHour ?: 0) >= 20 && it.id !in completedIds }

        val message = ReminderMessageSelector.select(
            habitName = habit.name,
            remainingHabitsToday = remaining,
            currentStreak = streak.current,
            bestStreak = streak.best,
            isEveningRoutineHabit = isEveningHabit,
            eveningRoutinePending = eveningPending,
        )

        NotificationChannels.ensureCreated(applicationContext)
        HabitudeNotifier.show(applicationContext, habit, message)

        ReminderScheduler.schedule(applicationContext, habit)
        return Result.success()
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
    }
}
