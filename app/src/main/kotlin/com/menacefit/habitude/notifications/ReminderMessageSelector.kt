package com.menacefit.habitude.notifications

enum class ReminderMessageType {
    /** "Plus qu'une habitude pour battre ton record !" — completing this habit today would beat its own best streak. */
    RECORD_WITHIN_REACH,

    /** "Tu n'as pas encore validé ta routine du soir." */
    EVENING_ROUTINE_PENDING,

    /** "Ta série de N jours t'attend." */
    STREAK_WAITING,

    /** "Il te reste N habitudes pour atteindre 100%." */
    HABITS_REMAINING,

    /** Plain nudge with just the habit's name — used when nothing more specific applies. */
    GENERIC,
}

data class ReminderMessage(val type: ReminderMessageType, val habitName: String, val count: Int = 0)

/**
 * Picks the single most motivating reason to open the app right now, for
 * one habit's reminder (spec section 28-29). Ordered most-compelling-first:
 * a record within reach beats a plain streak reminder, which beats a
 * generic nudge — never more than one message per notification, to keep
 * this from ever reading as nagging.
 */
object ReminderMessageSelector {
    fun select(
        habitName: String,
        remainingHabitsToday: Int,
        currentStreak: Int,
        bestStreak: Int,
        isEveningRoutineHabit: Boolean,
        eveningRoutinePending: Boolean,
    ): ReminderMessage = when {
        bestStreak > 0 && currentStreak + 1 > bestStreak -> ReminderMessage(ReminderMessageType.RECORD_WITHIN_REACH, habitName)
        isEveningRoutineHabit && eveningRoutinePending -> ReminderMessage(ReminderMessageType.EVENING_ROUTINE_PENDING, habitName)
        currentStreak >= 3 -> ReminderMessage(ReminderMessageType.STREAK_WAITING, habitName, currentStreak)
        remainingHabitsToday > 0 -> ReminderMessage(ReminderMessageType.HABITS_REMAINING, habitName, remainingHabitsToday)
        else -> ReminderMessage(ReminderMessageType.GENERIC, habitName)
    }
}
