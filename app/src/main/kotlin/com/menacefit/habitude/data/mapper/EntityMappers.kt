package com.menacefit.habitude.data.mapper

import com.menacefit.habitude.data.local.entity.ChallengeEntity
import com.menacefit.habitude.data.local.entity.DailyStatsEntity
import com.menacefit.habitude.data.local.entity.GoalEntity
import com.menacefit.habitude.data.local.entity.HabitCompletionEntity
import com.menacefit.habitude.data.local.entity.HabitEntity
import com.menacefit.habitude.data.local.entity.JournalEntryEntity
import com.menacefit.habitude.data.local.entity.MoodEntryEntity
import com.menacefit.habitude.data.local.entity.QuestInstanceEntity
import com.menacefit.habitude.data.local.entity.UnlockedAchievementEntity
import com.menacefit.habitude.data.local.entity.UserProfileEntity
import com.menacefit.habitude.domain.model.Challenge
import com.menacefit.habitude.domain.model.DailyStatsSnapshot
import com.menacefit.habitude.domain.model.Goal
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.HabitCompletion
import com.menacefit.habitude.domain.model.JournalEntry
import com.menacefit.habitude.domain.model.MoodEntry
import com.menacefit.habitude.domain.model.QuestInstance
import com.menacefit.habitude.domain.model.UnlockedAchievement
import com.menacefit.habitude.domain.model.UserProfile

fun HabitEntity.toDomain() = Habit(
    id = id, name = name, description = description, icon = icon, colorHex = colorHex,
    category = category, type = type, target = target, frequency = frequency, difficulty = difficulty,
    reminderEnabled = reminderEnabled, reminderHour = reminderHour, reminderMinute = reminderMinute,
    soundEnabled = soundEnabled, vibrationEnabled = vibrationEnabled, startDate = startDate, endDate = endDate,
    sortOrder = sortOrder, active = active, archivedAt = archivedAt, createdAt = createdAt,
)

fun Habit.toEntity() = HabitEntity(
    id = id, name = name, description = description, icon = icon, colorHex = colorHex,
    category = category, type = type, target = target, frequency = frequency, difficulty = difficulty,
    reminderEnabled = reminderEnabled, reminderHour = reminderHour, reminderMinute = reminderMinute,
    soundEnabled = soundEnabled, vibrationEnabled = vibrationEnabled, startDate = startDate, endDate = endDate,
    sortOrder = sortOrder, active = active, archivedAt = archivedAt, createdAt = createdAt,
)

fun HabitCompletionEntity.toDomain() = HabitCompletion(
    id = id, habitId = habitId, date = date, completed = completed, value = value,
    checklistCheckedIds = checklistCheckedIds, timestamp = timestamp,
)

fun HabitCompletion.toEntity() = HabitCompletionEntity(
    id = id, habitId = habitId, date = date, completed = completed, value = value,
    checklistCheckedIds = checklistCheckedIds, timestamp = timestamp,
)

fun UserProfileEntity.toDomain() = UserProfile(
    id = id, name = name, avatarEmoji = avatarEmoji, totalXp = totalXp,
    streakFreezesAvailable = streakFreezesAvailable, selectedCategories = selectedCategories,
    personalGoalText = personalGoalText, gamificationEnabled = gamificationEnabled, createdAt = createdAt,
)

fun UserProfile.toEntity() = UserProfileEntity(
    id = id, name = name, avatarEmoji = avatarEmoji, totalXp = totalXp,
    streakFreezesAvailable = streakFreezesAvailable, selectedCategories = selectedCategories,
    personalGoalText = personalGoalText, gamificationEnabled = gamificationEnabled, createdAt = createdAt,
)

fun UnlockedAchievementEntity.toDomain() = UnlockedAchievement(type = type, unlockedAt = unlockedAt)
fun UnlockedAchievement.toEntity() = UnlockedAchievementEntity(type = type, unlockedAt = unlockedAt)

fun DailyStatsEntity.toDomain() = DailyStatsSnapshot(
    date = date, scheduledCount = scheduledCount, completedCount = completedCount,
    completionRate = completionRate, xpEarned = xpEarned, performance = performance,
)

fun DailyStatsSnapshot.toEntity() = DailyStatsEntity(
    date = date, scheduledCount = scheduledCount, completedCount = completedCount,
    completionRate = completionRate, xpEarned = xpEarned, performance = performance,
)

fun MoodEntryEntity.toDomain() = MoodEntry(id = id, date = date, mood = mood, energy = energy, note = note)
fun MoodEntry.toEntity() = MoodEntryEntity(id = id, date = date, mood = mood, energy = energy, note = note)

fun JournalEntryEntity.toDomain() = JournalEntry(id = id, date = date, text = text)
fun JournalEntry.toEntity() = JournalEntryEntity(id = id, date = date, text = text)

fun GoalEntity.toDomain() = Goal(
    id = id, title = title, icon = icon, targetValue = targetValue, currentValue = currentValue,
    startDate = startDate, deadline = deadline, achieved = achieved,
)

fun Goal.toEntity() = GoalEntity(
    id = id, title = title, icon = icon, targetValue = targetValue, currentValue = currentValue,
    startDate = startDate, deadline = deadline, achieved = achieved,
)

fun QuestInstanceEntity.toDomain() = QuestInstance(
    id = id, templateId = templateId, descriptionKey = descriptionKey, metric = metric, metricParam = metricParam,
    date = date, targetProgress = targetProgress, currentProgress = currentProgress, xpReward = xpReward,
    completed = completed,
)

fun QuestInstance.toEntity() = QuestInstanceEntity(
    id = id, templateId = templateId, descriptionKey = descriptionKey, metric = metric, metricParam = metricParam,
    date = date, targetProgress = targetProgress, currentProgress = currentProgress, xpReward = xpReward,
    completed = completed,
)

fun ChallengeEntity.toDomain() = Challenge(
    id = id, title = title, description = description, icon = icon, startDate = startDate,
    durationDays = durationDays, linkedHabitId = linkedHabitId, xpReward = xpReward, status = status,
    completedDates = completedDates,
)

fun Challenge.toEntity() = ChallengeEntity(
    id = id, title = title, description = description, icon = icon, startDate = startDate,
    durationDays = durationDays, linkedHabitId = linkedHabitId, xpReward = xpReward, status = status,
    completedDates = completedDates,
)
