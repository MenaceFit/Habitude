package com.menacefit.habitude.domain.model

/** Broad life areas a user can pick during onboarding and tag habits with. */
enum class HabitCategory(val defaultEmoji: String) {
    SPORT("🏋️"),
    DISCIPLINE("🧠"),
    STUDY("📚"),
    WORK("💼"),
    FINANCE("💰"),
    SLEEP("😴"),
    NUTRITION("🥗"),
    HEALTH("💧"),
    SCREEN_TIME("📱"),
    WELLBEING("🧘"),
    PRODUCTIVITY("🎯"),
    OTHER("✨"),
}

/**
 * The interaction model of a habit. Deliberately not a single boolean
 * "done/not done" — each type drives a different input widget and a
 * different completion rule (see [com.menacefit.habitude.domain.model.HabitTarget]).
 */
enum class HabitType {
    BOOLEAN,
    QUANTITY,
    DURATION,
    COUNT,
    LIMIT,
    TIME,
    CHECKLIST,
}

/** Difficulty drives the base XP reward for a single completion. */
enum class Difficulty(val baseXp: Int) {
    EASY(10),
    MEDIUM(20),
    HARD(40),
}

/** Aggregate performance bucket for a single calendar day, used by the calendar and heatmap. */
enum class DayPerformance {
    PERFECT,
    GOOD,
    LOW,
    NONE,
}

enum class AchievementType {
    FIRST_DAY,
    STREAK_7,
    STREAK_30,
    STREAK_100,
    HABITS_TOTAL_10,
    HABITS_TOTAL_100,
    HABITS_TOTAL_500,
    PERFECT_DAY,
    PERFECT_WEEK,
    PERFECT_MONTH,
    RECORD_BROKEN,
    MORNING_ROUTINE,
    NIGHT_ROUTINE,
    HABIT_90_DAYS,
    ALL_HABITS_DAY,
}

enum class ChallengeStatus {
    ACTIVE,
    COMPLETED,
    FAILED,
    ABANDONED,
}

enum class MoodLevel(val emoji: String, val score: Int) {
    AWFUL("😡", 1),
    LOW("😔", 2),
    NEUTRAL("😐", 3),
    GOOD("🙂", 4),
    GREAT("😄", 5),
}

/** What a daily quest measures, so the app can increment its progress as completions come in. */
enum class QuestMetric {
    HABITS_COMPLETED_COUNT,
    FULL_DAY_COMPLETION,
    SPECIFIC_HABIT_COMPLETED,
    COMPLETE_BEFORE_HOUR,
    CATEGORY_HABIT_COMPLETED,
}
