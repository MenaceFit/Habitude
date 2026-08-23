package com.menacefit.habitude.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.menacefit.habitude.data.local.entity.RoutineEntity
import com.menacefit.habitude.data.local.entity.RoutineHabitCrossRefEntity
import kotlinx.coroutines.flow.Flow

/**
 * Deliberately no Room `@Relation`/`@Transaction` POJO here: the repository
 * joins routines, cross-refs and habits itself (it already holds the habit
 * list in memory for everything else), which keeps this DAO — and its SQL —
 * trivial to read and get right.
 */
@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY sortOrder ASC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutine(id: String): RoutineEntity?

    @Query("SELECT * FROM routines ORDER BY sortOrder ASC")
    suspend fun getAllRoutinesOnce(): List<RoutineEntity>

    @Query("SELECT * FROM routine_habit_cross_ref ORDER BY position ASC")
    suspend fun getAllCrossRefsOnce(): List<RoutineHabitCrossRefEntity>

    @Query("SELECT * FROM routine_habit_cross_ref ORDER BY position ASC")
    fun observeAllCrossRefs(): Flow<List<RoutineHabitCrossRefEntity>>

    @Query("SELECT * FROM routine_habit_cross_ref WHERE routineId = :routineId ORDER BY position ASC")
    suspend fun getCrossRefsForRoutine(routineId: String): List<RoutineHabitCrossRefEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM routines")
    suspend fun getMaxRoutineSortOrder(): Int

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: String)

    @Query("DELETE FROM routine_habit_cross_ref WHERE routineId = :id")
    suspend fun deleteCrossRefsForRoutine(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrossRefs(crossRefs: List<RoutineHabitCrossRefEntity>)

    @Query("DELETE FROM routine_habit_cross_ref WHERE routineId = :routineId AND habitId = :habitId")
    suspend fun removeCrossRef(routineId: String, habitId: String)

    @Query("DELETE FROM routines")
    suspend fun deleteAll()

    @Query("DELETE FROM routine_habit_cross_ref")
    suspend fun deleteAllCrossRefs()
}
