package com.menacefit.habitude.domain.achievement

import com.menacefit.habitude.domain.model.AchievementType

/** Static, stable catalog of every achievement the app ships with — emoji + string-resource keys only; the localized text lives in the UI's string resources. */
data class AchievementDefinition(
    val type: AchievementType,
    val emoji: String,
    val titleKey: String,
    val descriptionKey: String,
)

object AchievementCatalog {
    val all: List<AchievementDefinition> = listOf(
        AchievementDefinition(AchievementType.FIRST_DAY, "🏆", "achievement_first_day_title", "achievement_first_day_desc"),
        AchievementDefinition(AchievementType.STREAK_7, "🔥", "achievement_streak_7_title", "achievement_streak_7_desc"),
        AchievementDefinition(AchievementType.STREAK_30, "🔥", "achievement_streak_30_title", "achievement_streak_30_desc"),
        AchievementDefinition(AchievementType.STREAK_100, "🔥", "achievement_streak_100_title", "achievement_streak_100_desc"),
        AchievementDefinition(AchievementType.HABITS_TOTAL_10, "💯", "achievement_habits_10_title", "achievement_habits_10_desc"),
        AchievementDefinition(AchievementType.HABITS_TOTAL_100, "💯", "achievement_habits_100_title", "achievement_habits_100_desc"),
        AchievementDefinition(AchievementType.HABITS_TOTAL_500, "💯", "achievement_habits_500_title", "achievement_habits_500_desc"),
        AchievementDefinition(AchievementType.PERFECT_DAY, "⚡", "achievement_perfect_day_title", "achievement_perfect_day_desc"),
        AchievementDefinition(AchievementType.PERFECT_WEEK, "⚡", "achievement_perfect_week_title", "achievement_perfect_week_desc"),
        AchievementDefinition(AchievementType.PERFECT_MONTH, "⚡", "achievement_perfect_month_title", "achievement_perfect_month_desc"),
        AchievementDefinition(AchievementType.RECORD_BROKEN, "🎖️", "achievement_record_title", "achievement_record_desc"),
        AchievementDefinition(AchievementType.MORNING_ROUTINE, "🌅", "achievement_morning_title", "achievement_morning_desc"),
        AchievementDefinition(AchievementType.NIGHT_ROUTINE, "🌙", "achievement_night_title", "achievement_night_desc"),
        AchievementDefinition(AchievementType.HABIT_90_DAYS, "🌳", "achievement_90_days_title", "achievement_90_days_desc"),
        AchievementDefinition(AchievementType.ALL_HABITS_DAY, "✅", "achievement_all_habits_title", "achievement_all_habits_desc"),
    )

    val byType: Map<AchievementType, AchievementDefinition> = all.associateBy { it.type }
}
