package com.menacefit.habitude.data.repository

import androidx.room.withTransaction
import com.menacefit.habitude.data.local.AppDatabase
import com.menacefit.habitude.data.local.entity.RoutineEntity
import com.menacefit.habitude.data.local.entity.RoutineHabitCrossRefEntity
import com.menacefit.habitude.data.local.entity.UserProfileEntity
import com.menacefit.habitude.data.mapper.toDomain
import com.menacefit.habitude.data.mapper.toEntity
import com.menacefit.habitude.domain.model.ExportedData
import com.menacefit.habitude.domain.model.Routine
import com.menacefit.habitude.util.TimeProvider
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

enum class ImportMode { REPLACE, MERGE }

sealed class ImportResult {
    data class Success(val habitsImported: Int, val completionsImported: Int) : ImportResult()
    data class Failure(val reason: ImportFailureReason) : ImportResult()
}

enum class ImportFailureReason { INVALID_JSON, UNSUPPORTED_SCHEMA_VERSION, EMPTY_FILE }

interface ExportImportRepository {
    suspend fun exportToJson(): String
    suspend fun importFromJson(jsonText: String, mode: ImportMode): ImportResult

    /** A flat, spreadsheet-friendly export of every completion (secondary format alongside JSON, spec section 47). */
    suspend fun exportCompletionsToCsv(): String

    /** Wipes every locally-stored habit, completion, and profile record — the "Réinitialisation" settings action. Preferences (theme, language, ...) are untouched. */
    suspend fun resetAllData()
}

class ExportImportRepositoryImpl(
    private val database: AppDatabase,
    private val timeProvider: TimeProvider,
) : ExportImportRepository {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    override suspend fun exportToJson(): String {
        val routineEntities = database.routineDao().getAllRoutinesOnce()
        val crossRefsByRoutine = database.routineDao().getAllCrossRefsOnce().groupBy { it.routineId }
        val routines = routineEntities.map { routine ->
            Routine(
                id = routine.id,
                name = routine.name,
                icon = routine.icon,
                sortOrder = routine.sortOrder,
                habitIds = crossRefsByRoutine[routine.id].orEmpty().sortedBy { it.position }.map { it.habitId },
            )
        }

        val data = ExportedData(
            exportedAt = timeProvider.nowInstant(),
            profile = database.userProfileDao().get(UserProfileEntity.LOCAL_PROFILE_ID)?.toDomain(),
            habits = database.habitDao().getAllHabitsOnce().map { it.toDomain() },
            completions = database.habitCompletionDao().getAllOnce().map { it.toDomain() },
            unlockedAchievements = database.achievementDao().getAllOnce().map { it.toDomain() },
            dailyStats = database.dailyStatsDao().getAllOnce().map { it.toDomain() },
            moods = database.moodDao().getAllOnce().map { it.toDomain() },
            journalEntries = database.journalDao().getAllOnce().map { it.toDomain() },
            goals = database.goalDao().getAllOnce().map { it.toDomain() },
            challenges = database.challengeDao().getAllOnce().map { it.toDomain() },
            routines = routines,
        )
        return json.encodeToString(ExportedData.serializer(), data)
    }

    override suspend fun exportCompletionsToCsv(): String {
        val habitsById = database.habitDao().getAllHabitsOnce().associate { it.id to it.name }
        val completions = database.habitCompletionDao().getAllOnce().sortedBy { it.date }

        val builder = StringBuilder()
        builder.append("date,habit,completed,value,timestamp\n")
        completions.forEach { completion ->
            val habitName = habitsById[completion.habitId] ?: completion.habitId
            builder.append(csvField(completion.date.toString())).append(',')
                .append(csvField(habitName)).append(',')
                .append(csvField(completion.completed.toString())).append(',')
                .append(csvField(completion.value.toString())).append(',')
                .append(csvField(completion.timestamp.toString())).append('\n')
        }
        return builder.toString()
    }

    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    override suspend fun importFromJson(jsonText: String, mode: ImportMode): ImportResult {
        if (jsonText.isBlank()) return ImportResult.Failure(ImportFailureReason.EMPTY_FILE)
        val data = try {
            json.decodeFromString(ExportedData.serializer(), jsonText)
        } catch (e: SerializationException) {
            return ImportResult.Failure(ImportFailureReason.INVALID_JSON)
        } catch (e: IllegalArgumentException) {
            return ImportResult.Failure(ImportFailureReason.INVALID_JSON)
        }

        if (data.schemaVersion > ExportedData.CURRENT_SCHEMA_VERSION) {
            return ImportResult.Failure(ImportFailureReason.UNSUPPORTED_SCHEMA_VERSION)
        }

        database.withTransaction {
            if (mode == ImportMode.REPLACE) {
                wipeAllTables()
            }

            data.profile?.let { database.userProfileDao().upsert(it.toEntity()) }
            data.habits.forEach { database.habitDao().upsert(it.toEntity()) }
            data.completions.forEach { database.habitCompletionDao().upsert(it.toEntity()) }
            data.unlockedAchievements.forEach { database.achievementDao().insert(it.toEntity()) }
            data.dailyStats.forEach { database.dailyStatsDao().upsert(it.toEntity()) }
            data.moods.forEach { database.moodDao().upsert(it.toEntity()) }
            data.journalEntries.forEach { database.journalDao().upsert(it.toEntity()) }
            data.goals.forEach { database.goalDao().upsert(it.toEntity()) }
            data.challenges.forEach { database.challengeDao().upsert(it.toEntity()) }
            data.routines.forEach { routine ->
                database.routineDao().upsertRoutine(RoutineEntity(routine.id, routine.name, routine.icon, routine.sortOrder))
                database.routineDao().upsertCrossRefs(
                    routine.habitIds.mapIndexed { index, habitId -> RoutineHabitCrossRefEntity(routine.id, habitId, index) },
                )
            }
        }

        return ImportResult.Success(habitsImported = data.habits.size, completionsImported = data.completions.size)
    }

    override suspend fun resetAllData() {
        database.withTransaction { wipeAllTables() }
    }

    private suspend fun wipeAllTables() {
        database.habitCompletionDao().deleteAll()
        database.habitDao().deleteAll()
        database.streakFreezeDao().deleteAll()
        database.achievementDao().deleteAll()
        database.dailyStatsDao().deleteAll()
        database.moodDao().deleteAll()
        database.journalDao().deleteAll()
        database.goalDao().deleteAll()
        database.questDao().deleteAll()
        database.challengeDao().deleteAll()
        database.routineDao().deleteAllCrossRefs()
        database.routineDao().deleteAll()
        database.userProfileDao().deleteAll()
    }
}
