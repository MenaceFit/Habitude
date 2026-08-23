package com.menacefit.habitude.domain.model

import com.menacefit.habitude.domain.serialization.InstantIsoSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

/**
 * The full local dataset in one serializable envelope — this is exactly
 * (and only) what "Exporter mes données" writes to a `.json` file and
 * "Importer mes données" reads back (spec section 47). [schemaVersion] lets
 * a future release detect and migrate an older export instead of silently
 * misreading it.
 */
@Serializable
data class ExportedData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    @Serializable(with = InstantIsoSerializer::class) val exportedAt: Instant,
    val profile: UserProfile?,
    val habits: List<Habit> = emptyList(),
    val completions: List<HabitCompletion> = emptyList(),
    val unlockedAchievements: List<UnlockedAchievement> = emptyList(),
    val dailyStats: List<DailyStatsSnapshot> = emptyList(),
    val moods: List<MoodEntry> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val challenges: List<Challenge> = emptyList(),
    val routines: List<Routine> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
