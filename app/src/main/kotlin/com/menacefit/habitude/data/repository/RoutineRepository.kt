package com.menacefit.habitude.data.repository

import com.menacefit.habitude.data.local.dao.RoutineDao
import com.menacefit.habitude.data.local.entity.RoutineEntity
import com.menacefit.habitude.data.local.entity.RoutineHabitCrossRefEntity
import com.menacefit.habitude.domain.model.Routine
import com.menacefit.habitude.util.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface RoutineRepository {
    fun observeRoutines(): Flow<List<Routine>>
    suspend fun createRoutine(name: String, icon: String, habitIds: List<String>): Routine
    suspend fun updateRoutineHabits(routineId: String, name: String, icon: String, habitIds: List<String>)
    suspend fun deleteRoutine(id: String)
}

class RoutineRepositoryImpl(private val routineDao: RoutineDao) : RoutineRepository {

    override fun observeRoutines(): Flow<List<Routine>> =
        combine(routineDao.observeRoutines(), routineDao.observeAllCrossRefs()) { routines, crossRefs ->
            val byRoutine = crossRefs.groupBy { it.routineId }
            routines.map { routine ->
                val habitIds = byRoutine[routine.id].orEmpty().sortedBy { it.position }.map { it.habitId }
                Routine(id = routine.id, name = routine.name, icon = routine.icon, sortOrder = routine.sortOrder, habitIds = habitIds)
            }
        }

    override suspend fun createRoutine(name: String, icon: String, habitIds: List<String>): Routine {
        val order = routineDao.getMaxRoutineSortOrder() + 1
        val id = newId()
        routineDao.upsertRoutine(RoutineEntity(id = id, name = name, icon = icon, sortOrder = order))
        routineDao.upsertCrossRefs(habitIds.mapIndexed { index, habitId -> RoutineHabitCrossRefEntity(id, habitId, index) })
        return Routine(id = id, name = name, icon = icon, sortOrder = order, habitIds = habitIds)
    }

    override suspend fun updateRoutineHabits(routineId: String, name: String, icon: String, habitIds: List<String>) {
        val currentSortOrder = routineDao.getRoutine(routineId)?.sortOrder ?: routineDao.getMaxRoutineSortOrder() + 1
        routineDao.deleteCrossRefsForRoutine(routineId)
        routineDao.upsertCrossRefs(habitIds.mapIndexed { index, habitId -> RoutineHabitCrossRefEntity(routineId, habitId, index) })
        routineDao.upsertRoutine(RoutineEntity(id = routineId, name = name, icon = icon, sortOrder = currentSortOrder))
    }

    override suspend fun deleteRoutine(id: String) {
        routineDao.deleteCrossRefsForRoutine(id)
        routineDao.deleteRoutine(id)
    }
}
