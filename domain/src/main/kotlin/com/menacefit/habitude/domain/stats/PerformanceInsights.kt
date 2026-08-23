package com.menacefit.habitude.domain.stats

import com.menacefit.habitude.domain.model.DailyStatsSnapshot
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A typed, locale-agnostic insight. Domain logic never emits user-facing
 * text (that would hard-code French/English into business rules) — the UI
 * layer maps each variant to a localized string resource.
 */
sealed class Insight {
    data class RegularityChange(val percentChange: Int) : Insight()
    data class BestDayOfWeek(val dayOfWeek: DayOfWeek) : Insight()
    data class TimeOfDayPerformance(val betterInMorning: Boolean, val differencePercent: Int) : Insight()
    data class MostConsistentHabit(val habitName: String, val successRatePercent: Int) : Insight()
    data class WeeklyObjectiveCompletion(val completedPercent: Int) : Insight()
    data class MoodCorrelation(val habitName: String, val scoreDeltaPercent: Int) : Insight()
}

/**
 * Generates the "Tes performances" insight cards purely from local data.
 * Every insight is optional: when there isn't enough history to say
 * something meaningful, it's simply omitted rather than guessed at.
 */
object PerformanceInsightsGenerator {

    fun regularityChange(currentWeek: List<DailyStatsSnapshot>, previousWeek: List<DailyStatsSnapshot>): Insight.RegularityChange? {
        val change = StatsAggregator.periodOverPeriodChangePercent(currentWeek, previousWeek) ?: return null
        if (change == 0) return null
        return Insight.RegularityChange(change)
    }

    fun bestDayOfWeek(snapshots: List<DailyStatsSnapshot>, minimumDataPoints: Int = 14): Insight.BestDayOfWeek? {
        if (snapshots.count { it.scheduledCount > 0 } < minimumDataPoints) return null
        val day = StatsAggregator.bestDayOfWeek(snapshots) ?: return null
        return Insight.BestDayOfWeek(day)
    }

    /**
     * Compares average completion rate of habit-completions logged before
     * noon vs at/after noon, using each completion's timestamp hour.
     */
    fun timeOfDayPerformance(completionHours: List<Int>, completionSuccess: List<Boolean>): Insight.TimeOfDayPerformance? {
        require(completionHours.size == completionSuccess.size)
        if (completionHours.size < 10) return null
        val morning = completionHours.indices.filter { completionHours[it] < 12 }
        val afternoon = completionHours.indices.filter { completionHours[it] >= 12 }
        if (morning.size < 3 || afternoon.size < 3) return null
        val morningRate = morning.count { completionSuccess[it] }.toDouble() / morning.size
        val afternoonRate = afternoon.count { completionSuccess[it] }.toDouble() / afternoon.size
        if (morningRate == afternoonRate) return null
        val betterInMorning = morningRate > afternoonRate
        val diff = kotlin.math.abs(morningRate - afternoonRate) * 100
        return Insight.TimeOfDayPerformance(betterInMorning, diff.toInt())
    }

    fun mostConsistentHabit(entries: List<HabitStatEntry>, habitNamesById: Map<String, String>, minimumScheduled: Int = 5): Insight.MostConsistentHabit? {
        val best = entries.filter { it.scheduledCount >= minimumScheduled }.maxByOrNull { it.successRate } ?: return null
        val name = habitNamesById[best.habitId] ?: return null
        return Insight.MostConsistentHabit(name, (best.successRate * 100).toInt())
    }

    fun weeklyObjectiveCompletion(completedThisWeek: Int, scheduledThisWeek: Int): Insight.WeeklyObjectiveCompletion? {
        if (scheduledThisWeek <= 0) return null
        return Insight.WeeklyObjectiveCompletion(((completedThisWeek.toDouble() / scheduledThisWeek) * 100).toInt())
    }

    /**
     * "Days you did X, your average day score is Y% higher" — averages
     * [dailyScoreByDate] on days the habit was completed vs the rest.
     */
    fun moodOrScoreCorrelation(
        habitName: String,
        habitCompletedDates: Set<LocalDate>,
        dailyScoreByDate: Map<LocalDate, Double>,
        minimumSamplesPerGroup: Int = 5,
    ): Insight.MoodCorrelation? {
        val withHabit = dailyScoreByDate.filterKeys { it in habitCompletedDates }.values
        val withoutHabit = dailyScoreByDate.filterKeys { it !in habitCompletedDates }.values
        if (withHabit.size < minimumSamplesPerGroup || withoutHabit.size < minimumSamplesPerGroup) return null
        val avgWith = withHabit.average()
        val avgWithout = withoutHabit.average()
        if (avgWithout <= 0.0) return null
        val delta = ((avgWith - avgWithout) / avgWithout * 100).toInt()
        if (delta == 0) return null
        return Insight.MoodCorrelation(habitName, delta)
    }
}
