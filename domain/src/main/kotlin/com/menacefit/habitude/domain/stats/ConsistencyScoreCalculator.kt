package com.menacefit.habitude.domain.stats

import com.menacefit.habitude.domain.model.ConsistencyBreakdown
import kotlin.math.roundToInt

/**
 * Inputs are all pre-aggregated ratios/counts so this calculator stays a
 * pure, trivially-testable function — it never touches raw completions.
 *
 * @param successRate30d fraction (0..1) of scheduled habit-days completed over the trailing 30 days.
 * @param currentStreakDays the user's best *current* streak across their habits.
 * @param bestStreakEver the user's best streak of all time.
 * @param perfectDaysRatio30d fraction (0..1) of the last 30 days that hit 100% completion.
 * @param goalCompletionRate fraction (0..1) of active long-term objectives progress (see [com.menacefit.habitude.domain.model.Goal]).
 * @param frequencyAdherence fraction (0..1) of scheduled habit-days that were at least attempted (not silently ignored).
 */
data class ConsistencyInputs(
    val successRate30d: Double,
    val currentStreakDays: Int,
    val bestStreakEver: Int,
    val perfectDaysRatio30d: Double,
    val goalCompletionRate: Double,
    val frequencyAdherence: Double,
)

/**
 * Score in [0, 100] blending five weighted signals. The weights are the only
 * "tunable" surface of this feature and are kept as named constants so a
 * product change is a one-line diff instead of a magic number hunt.
 */
object ConsistencyScoreCalculator {
    private const val WEIGHT_SUCCESS_RATE = 0.30
    private const val WEIGHT_STREAK = 0.20
    private const val WEIGHT_PERFECT_DAYS = 0.20
    private const val WEIGHT_GOALS = 0.15
    private const val WEIGHT_FREQUENCY = 0.15

    fun compute(inputs: ConsistencyInputs): ConsistencyBreakdown {
        val successScore = toScore(inputs.successRate30d)
        val streakScore = streakFactor(inputs.currentStreakDays, inputs.bestStreakEver)
        val perfectDaysScore = toScore(inputs.perfectDaysRatio30d)
        val goalScore = toScore(inputs.goalCompletionRate)
        val frequencyScore = toScore(inputs.frequencyAdherence)

        val overall = (
            WEIGHT_SUCCESS_RATE * successScore +
                WEIGHT_STREAK * streakScore +
                WEIGHT_PERFECT_DAYS * perfectDaysScore +
                WEIGHT_GOALS * goalScore +
                WEIGHT_FREQUENCY * frequencyScore
            ).roundToInt().coerceIn(0, 100)

        return ConsistencyBreakdown(
            overall = overall,
            successRate = successScore,
            streakStrength = streakScore,
            perfectDaysRatio = perfectDaysScore,
            goalCompletion = goalScore,
            frequencyAdherence = frequencyScore,
        )
    }

    private fun toScore(ratio: Double): Int = (ratio.coerceIn(0.0, 1.0) * 100).roundToInt()

    /**
     * A 30-day-or-longer current streak alone maxes this sub-score out; on
     * top of that, being close to (or at) your personal best streak — rather
     * than far below it — earns extra credit, since that reflects sustained
     * effort rather than a single early lucky run.
     */
    private fun streakFactor(current: Int, best: Int): Int {
        val absolute = (current.coerceAtLeast(0) / 30.0).coerceIn(0.0, 1.0)
        val relativeToBest = if (best <= 0) 0.0 else (current.toDouble() / best).coerceIn(0.0, 1.0)
        return ((0.7 * absolute + 0.3 * relativeToBest) * 100).roundToInt()
    }
}
