package com.menacefit.habitude.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.menacefit.habitude.HabitudeApplication
import com.menacefit.habitude.MainActivity
import com.menacefit.habitude.R
import com.menacefit.habitude.di.AppContainer
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitType
import com.menacefit.habitude.domain.model.isDueOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A classic RemoteViews widget (not Glance): fewer moving version-matching
 * parts to get wrong sight-unseen, and RemoteViews is battle-tested for
 * exactly this "small read-mostly surface + one action" shape (spec
 * section 27).
 */
class HabitWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_QUICK_COMPLETE = "com.menacefit.habitude.widget.ACTION_QUICK_COMPLETE"
        const val EXTRA_HABIT_ID = "habit_id"

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(context, HabitWidgetProvider::class.java)
                        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
                )
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val container = (context.applicationContext as HabitudeApplication).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildRemoteViews(context, container)
                appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_QUICK_COMPLETE) {
            val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
            val container = (context.applicationContext as HabitudeApplication).container
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (habitId != null) {
                        val habit = container.habitRepository.getHabit(habitId)
                        if (habit != null) container.completionCoordinator.recordProgress(habit, 1.0)
                    }
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))
                    val views = buildRemoteViews(context, container)
                    ids.forEach { id -> manager.updateAppWidget(id, views) }
                } finally {
                    pending.finish()
                }
            }
        }
        super.onReceive(context, intent)
    }

    private suspend fun buildRemoteViews(context: Context, container: AppContainer): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_habit)
        val today = container.timeProvider.todayLocalDate()

        val activeHabits = container.habitRepository.getActiveHabitsOnce()
        val dueToday = activeHabits.filter { it.isDueOn(today) }
        val completions = container.habitRepository.getCompletionsForDate(today)
        val completedIds = completions.filter { it.completed }.map { it.habitId }.toSet()
        val completedCount = dueToday.count { it.id in completedIds }

        views.setTextViewText(R.id.widget_progress_text, "$completedCount / ${dueToday.size}")

        val bestStreak = dueToday.maxOfOrNull { container.habitRepository.computeStreak(it).current } ?: 0
        views.setTextViewText(R.id.widget_streak_text, "🔥 $bestStreak")

        val snapshot = container.statsRepository.getSnapshot(today)
        views.setTextViewText(R.id.widget_xp_text, "+${snapshot?.xpEarned ?: 0} XP")

        val target: Habit? = dueToday.firstOrNull { it.type == HabitType.BOOLEAN && it.id !in completedIds }
        if (target != null) {
            views.setTextViewText(R.id.widget_quick_complete_button, "✓ ${target.name}")
            val actionIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_QUICK_COMPLETE
                putExtra(EXTRA_HABIT_ID, target.id)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                target.id.hashCode(),
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_quick_complete_button, actionPendingIntent)
        } else {
            views.setTextViewText(R.id.widget_quick_complete_button, context.getString(R.string.widget_quick_complete_all_done))
            views.setOnClickPendingIntent(R.id.widget_quick_complete_button, openAppPendingIntent(context))
        }

        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent(context))
        return views
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
