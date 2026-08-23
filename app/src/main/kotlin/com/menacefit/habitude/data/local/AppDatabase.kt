package com.menacefit.habitude.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.menacefit.habitude.data.local.dao.AchievementDao
import com.menacefit.habitude.data.local.dao.ChallengeDao
import com.menacefit.habitude.data.local.dao.DailyStatsDao
import com.menacefit.habitude.data.local.dao.GoalDao
import com.menacefit.habitude.data.local.dao.HabitCompletionDao
import com.menacefit.habitude.data.local.dao.HabitDao
import com.menacefit.habitude.data.local.dao.JournalDao
import com.menacefit.habitude.data.local.dao.MoodDao
import com.menacefit.habitude.data.local.dao.QuestDao
import com.menacefit.habitude.data.local.dao.RoutineDao
import com.menacefit.habitude.data.local.dao.StreakFreezeDao
import com.menacefit.habitude.data.local.dao.UserProfileDao
import com.menacefit.habitude.data.local.entity.ChallengeEntity
import com.menacefit.habitude.data.local.entity.DailyStatsEntity
import com.menacefit.habitude.data.local.entity.GoalEntity
import com.menacefit.habitude.data.local.entity.HabitCompletionEntity
import com.menacefit.habitude.data.local.entity.HabitEntity
import com.menacefit.habitude.data.local.entity.JournalEntryEntity
import com.menacefit.habitude.data.local.entity.MoodEntryEntity
import com.menacefit.habitude.data.local.entity.QuestInstanceEntity
import com.menacefit.habitude.data.local.entity.RoutineEntity
import com.menacefit.habitude.data.local.entity.RoutineHabitCrossRefEntity
import com.menacefit.habitude.data.local.entity.StreakFreezeUsageEntity
import com.menacefit.habitude.data.local.entity.UnlockedAchievementEntity
import com.menacefit.habitude.data.local.entity.UserProfileEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        StreakFreezeUsageEntity::class,
        UserProfileEntity::class,
        UnlockedAchievementEntity::class,
        DailyStatsEntity::class,
        MoodEntryEntity::class,
        JournalEntryEntity::class,
        GoalEntity::class,
        QuestInstanceEntity::class,
        ChallengeEntity::class,
        RoutineEntity::class,
        RoutineHabitCrossRefEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun streakFreezeDao(): StreakFreezeDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun achievementDao(): AchievementDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun moodDao(): MoodDao
    abstract fun journalDao(): JournalDao
    abstract fun goalDao(): GoalDao
    abstract fun questDao(): QuestDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun routineDao(): RoutineDao

    companion object {
        private const val DATABASE_NAME = "habitude.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .build()
                .also { instance = it }
        }
    }
}
