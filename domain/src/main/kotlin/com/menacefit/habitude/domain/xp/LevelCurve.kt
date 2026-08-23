package com.menacefit.habitude.domain.xp

import com.menacefit.habitude.domain.model.LevelInfo

/**
 * Level thresholds follow a closed-form quadratic: level 1 = 0 XP, level 2 =
 * 100 XP, level 3 = 250 XP, level 4 = 450 XP, ... each level requiring 50 XP
 * more than the last (100, 150, 200, 250, ...). Solving that arithmetic
 * series in closed form gives:
 *
 *   threshold(L) = 25 * (L - 1) * (L + 2)
 *
 * which reproduces the examples exactly (L=2 -> 100, L=3 -> 250, L=4 -> 450)
 * without needing a lookup table, so it scales to arbitrarily high levels.
 */
object LevelCurve {

    fun xpThresholdForLevel(level: Int): Int {
        require(level >= 1) { "level must be >= 1" }
        return 25 * (level - 1) * (level + 2)
    }

    fun levelForTotalXp(totalXp: Int): LevelInfo {
        val xp = totalXp.coerceAtLeast(0)
        var level = 1
        while (xpThresholdForLevel(level + 1) <= xp) level++

        val floor = xpThresholdForLevel(level)
        val ceiling = xpThresholdForLevel(level + 1)
        val xpIntoLevel = xp - floor
        val xpForThisLevel = ceiling - floor
        val progress = if (xpForThisLevel <= 0) 1f else (xpIntoLevel.toFloat() / xpForThisLevel).coerceIn(0f, 1f)

        return LevelInfo(
            level = level,
            totalXp = xp,
            xpIntoLevel = xpIntoLevel,
            xpForThisLevel = xpForThisLevel,
            progress = progress,
        )
    }
}
