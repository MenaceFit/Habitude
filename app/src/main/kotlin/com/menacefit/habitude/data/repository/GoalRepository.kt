package com.menacefit.habitude.data.repository

import com.menacefit.habitude.data.local.dao.GoalDao
import com.menacefit.habitude.data.mapper.toDomain
import com.menacefit.habitude.data.mapper.toEntity
import com.menacefit.habitude.domain.model.Goal
import com.menacefit.habitude.util.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface GoalRepository {
    fun observeAll(): Flow<List<Goal>>
    suspend fun createGoal(title: String, icon: String, targetValue: Int, startDate: LocalDate, deadline: LocalDate?): Goal
    suspend fun updateProgress(id: String, newValue: Int): Goal?
    suspend fun deleteGoal(id: String)
}

class GoalRepositoryImpl(private val goalDao: GoalDao) : GoalRepository {

    override fun observeAll(): Flow<List<Goal>> = goalDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun createGoal(title: String, icon: String, targetValue: Int, startDate: LocalDate, deadline: LocalDate?): Goal {
        val goal = Goal(
            id = newId(), title = title, icon = icon, targetValue = targetValue, currentValue = 0,
            startDate = startDate, deadline = deadline, achieved = false,
        )
        goalDao.upsert(goal.toEntity())
        return goal
    }

    override suspend fun updateProgress(id: String, newValue: Int): Goal? {
        val existing = goalDao.get(id)?.toDomain() ?: return null
        val clamped = newValue.coerceIn(0, existing.targetValue)
        val updated = existing.copy(currentValue = clamped, achieved = clamped >= existing.targetValue)
        goalDao.update(updated.toEntity())
        return updated
    }

    override suspend fun deleteGoal(id: String) {
        goalDao.delete(id)
    }
}
