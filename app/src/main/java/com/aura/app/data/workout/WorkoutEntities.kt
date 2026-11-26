package com.aura.app.data.workout

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtEpochMs: Long,
)

@Entity(
    tableName = "routine_exercises",
    primaryKeys = ["routineId", "exerciseKey"],
    indices = [Index("routineId")],
)
data class RoutineExerciseEntity(
    val routineId: Long,
    val exerciseKey: String,
    val exerciseName: String,
    val equipment: String,
    val position: Int,
    val targetSets: Int = 3,
    val defaultWeightKg: String = "",
    val defaultReps: String = "10",
    val restSeconds: Int = 90,
)

data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(parentColumn = "id", entityColumn = "routineId")
    val exercises: List<RoutineExerciseEntity>,
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long? = null,
    val restUntilEpochMs: Long? = null,
    val healthConnectId: String? = null,
)

@Entity(
    tableName = "workout_sets",
    indices = [
        Index("workoutId"),
        Index(value = ["workoutId", "exerciseKey", "setNumber"], unique = true),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseKey: String,
    val exerciseName: String,
    val equipment: String,
    val exercisePosition: Int,
    val setNumber: Int,
    val previous: String,
    val weightKg: String,
    val reps: String,
    val restSeconds: Int = 90,
    @ColumnInfo(defaultValue = "''") val exerciseNote: String = "",
    val completedAtEpochMs: Long? = null,
)

data class WorkoutWithSets(
    @Embedded val workout: WorkoutEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val sets: List<WorkoutSetEntity>,
)

data class WorkoutSummary(
    val id: Long,
    val title: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val completedSets: Int,
    val totalSets: Int,
    val volumeKg: Double,
    val healthConnectId: String?,
)

data class ExerciseHistoryPoint(
    val finishedAtEpochMs: Long,
    val bestWeightKg: Double,
    val volumeKg: Double,
    val estimatedOneRepMaxKg: Double,
)

data class ExerciseHistorySet(
    val workoutId: Long,
    val workoutTitle: String,
    val finishedAtEpochMs: Long,
    val setNumber: Int,
    val weightKg: String,
    val reps: String,
)
