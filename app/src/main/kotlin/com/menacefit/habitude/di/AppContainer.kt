package com.menacefit.habitude.di

import android.content.Context
import com.menacefit.habitude.data.local.AppDatabase
import com.menacefit.habitude.data.preferences.SettingsRepository
import com.menacefit.habitude.data.preferences.SettingsRepositoryImpl
import com.menacefit.habitude.data.repository.ChallengeRepository
import com.menacefit.habitude.data.repository.ChallengeRepositoryImpl
import com.menacefit.habitude.data.repository.ExportImportRepository
import com.menacefit.habitude.data.repository.ExportImportRepositoryImpl
import com.menacefit.habitude.data.repository.GoalRepository
import com.menacefit.habitude.data.repository.GoalRepositoryImpl
import com.menacefit.habitude.data.repository.HabitRepository
import com.menacefit.habitude.data.repository.HabitRepositoryImpl
import com.menacefit.habitude.data.repository.MoodJournalRepository
import com.menacefit.habitude.data.repository.MoodJournalRepositoryImpl
import com.menacefit.habitude.data.repository.ProfileRepository
import com.menacefit.habitude.data.repository.ProfileRepositoryImpl
import com.menacefit.habitude.data.repository.QuestRepository
import com.menacefit.habitude.data.repository.QuestRepositoryImpl
import com.menacefit.habitude.data.repository.RoutineRepository
import com.menacefit.habitude.data.repository.RoutineRepositoryImpl
import com.menacefit.habitude.data.repository.StatsRepository
import com.menacefit.habitude.data.repository.StatsRepositoryImpl
import com.menacefit.habitude.gamification.CompletionCoordinator
import com.menacefit.habitude.notifications.ReminderCoordinator
import com.menacefit.habitude.util.HapticFeedbackHelper
import com.menacefit.habitude.util.SoundPlayer
import com.menacefit.habitude.util.SystemTimeProvider
import com.menacefit.habitude.util.TimeProvider
import com.menacefit.habitude.widget.WidgetRefresher

/**
 * Hand-rolled composition root (no Hilt/Dagger): a small app with a fixed,
 * well-understood dependency graph doesn't need annotation-processor-driven
 * DI, and building it by hand keeps every wire visible in one file. Held on
 * [com.menacefit.habitude.HabitudeApplication] and read from there by
 * Activities/Workers/the widget.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val timeProvider: TimeProvider = SystemTimeProvider()

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    val habitRepository: HabitRepository by lazy {
        HabitRepositoryImpl(database.habitDao(), database.habitCompletionDao(), database.streakFreezeDao(), timeProvider)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(database.userProfileDao(), database.achievementDao(), timeProvider)
    }

    val statsRepository: StatsRepository by lazy { StatsRepositoryImpl(database.dailyStatsDao()) }

    val questRepository: QuestRepository by lazy { QuestRepositoryImpl(database.questDao()) }

    val challengeRepository: ChallengeRepository by lazy { ChallengeRepositoryImpl(database.challengeDao()) }

    val goalRepository: GoalRepository by lazy { GoalRepositoryImpl(database.goalDao()) }

    val moodJournalRepository: MoodJournalRepository by lazy {
        MoodJournalRepositoryImpl(database.moodDao(), database.journalDao())
    }

    val routineRepository: RoutineRepository by lazy { RoutineRepositoryImpl(database.routineDao()) }

    val exportImportRepository: ExportImportRepository by lazy { ExportImportRepositoryImpl(database, timeProvider) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(appContext) }

    val widgetRefresher: WidgetRefresher by lazy { WidgetRefresher(appContext) }

    val completionCoordinator: CompletionCoordinator by lazy {
        CompletionCoordinator(habitRepository, profileRepository, statsRepository, questRepository, timeProvider, widgetRefresher)
    }

    val hapticFeedbackHelper: HapticFeedbackHelper by lazy { HapticFeedbackHelper(appContext) }
    val soundPlayer: SoundPlayer by lazy { SoundPlayer(appContext) }
    val reminderCoordinator: ReminderCoordinator by lazy { ReminderCoordinator(appContext) }
}
