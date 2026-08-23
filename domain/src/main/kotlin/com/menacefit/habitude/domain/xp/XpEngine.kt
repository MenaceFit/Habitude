package com.menacefit.habitude.domain.xp

import com.menacefit.habitude.domain.model.Difficulty

/** Central XP reward table (section 12 of the spec) — the only place these numbers are allowed to live. */
object XpEngine {
    const val DAILY_GOAL_BONUS = 50
    const val ALL_HABITS_BONUS = 100

    fun xpForCompletion(difficulty: Difficulty): Int = difficulty.baseXp

    /**
     * Bonus XP awarded once per day. [dailyGoalReached] is "today's overall
     * progress ring hit 100%"; [allHabitsCompleted] is "literally every
     * active habit scheduled today was completed". Both can apply on the
     * same day (a perfect day with only a couple of light habits still
     * reaches 100% and completes everything), so they stack.
     */
    fun dailyBonusXp(dailyGoalReached: Boolean, allHabitsCompleted: Boolean): Int {
        var bonus = 0
        if (dailyGoalReached) bonus += DAILY_GOAL_BONUS
        if (allHabitsCompleted) bonus += ALL_HABITS_BONUS
        return bonus
    }
}
