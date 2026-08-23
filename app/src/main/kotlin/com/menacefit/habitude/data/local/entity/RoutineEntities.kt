package com.menacefit.habitude.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A named, ordered group of existing habits (e.g. "Routine matin"). Progress is derived live from those habits' completion state today — routines don't track their own separate completion. */
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "routine_habit_cross_ref",
    primaryKeys = ["routineId", "habitId"],
    indices = [Index(value = ["habitId"])],
)
data class RoutineHabitCrossRefEntity(
    val routineId: String,
    val habitId: String,
    val position: Int,
)
