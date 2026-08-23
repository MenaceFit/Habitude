package com.menacefit.habitude.data.repository

import com.menacefit.habitude.data.local.dao.QuestDao
import com.menacefit.habitude.data.mapper.toDomain
import com.menacefit.habitude.data.mapper.toEntity
import com.menacefit.habitude.domain.model.Habit
import com.menacefit.habitude.domain.model.QuestInstance
import com.menacefit.habitude.domain.model.QuestMetric
import com.menacefit.habitude.domain.quest.QuestGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Live signals the quest-progress recompute needs, gathered by the caller (which already has today's completions in hand). */
data class TodayQuestSignals(
    val completedHabitIds: Set<String>,
    val completedCountToday: Int,
    val dailyGoalReached: Boolean,
    val earliestCompletionHourToday: Int?,
)

interface QuestRepository {
    fun observeForDate(date: LocalDate): Flow<List<QuestInstance>>
    suspend fun getForDate(date: LocalDate): List<QuestInstance>

    /** Generates and persists today's quests the first time they're needed for [date]; returns the existing set on subsequent calls. */
    suspend fun ensureQuestsForDate(date: LocalDate, activeHabits: List<Habit>, profileSeed: Long): List<QuestInstance>

    /** Recomputes every quest for [date] against the latest signals and persists the changes; returns the updated list. */
    suspend fun refreshProgress(date: LocalDate, signals: TodayQuestSignals): List<QuestInstance>

    suspend fun getTotalCompletedCount(): Int
}

class QuestRepositoryImpl(private val questDao: QuestDao) : QuestRepository {

    override fun observeForDate(date: LocalDate): Flow<List<QuestInstance>> =
        questDao.observeForDate(date).map { list -> list.map { it.toDomain() } }

    override suspend fun getForDate(date: LocalDate): List<QuestInstance> = questDao.getForDate(date).map { it.toDomain() }

    override suspend fun ensureQuestsForDate(date: LocalDate, activeHabits: List<Habit>, profileSeed: Long): List<QuestInstance> {
        val existing = getForDate(date)
        if (existing.isNotEmpty()) return existing

        val todayHabits = activeHabits.map { QuestGenerator.TodayHabit(it.id, it.difficulty, it.category) }
        val generated = QuestGenerator.generate(date, todayHabits, profileSeed)
        if (generated.isEmpty()) return emptyList()
        questDao.upsertAll(generated.map { it.toEntity() })
        return generated
    }

    override suspend fun refreshProgress(date: LocalDate, signals: TodayQuestSignals): List<QuestInstance> {
        val quests = getForDate(date)
        val updated = quests.map { quest -> recompute(quest, signals) }
        updated.zip(quests).forEach { (new, old) ->
            if (new != old) questDao.update(new.toEntity())
        }
        return updated
    }

    override suspend fun getTotalCompletedCount(): Int = questDao.getTotalCompletedCount()

    private fun recompute(quest: QuestInstance, signals: TodayQuestSignals): QuestInstance {
        val progress = when (quest.metric) {
            QuestMetric.HABITS_COMPLETED_COUNT -> signals.completedCountToday
            QuestMetric.FULL_DAY_COMPLETION -> if (signals.dailyGoalReached) 1 else 0
            QuestMetric.SPECIFIC_HABIT_COMPLETED -> if (quest.metricParam in signals.completedHabitIds) 1 else 0
            QuestMetric.COMPLETE_BEFORE_HOUR -> {
                val limit = quest.metricParam?.toIntOrNull()
                val earliest = signals.earliestCompletionHourToday
                if (limit != null && earliest != null && earliest < limit) 1 else 0
            }
            QuestMetric.CATEGORY_HABIT_COMPLETED -> quest.currentProgress // not generated yet; leave untouched
        }.coerceAtMost(quest.targetProgress)

        return quest.copy(currentProgress = progress, completed = progress >= quest.targetProgress)
    }
}
