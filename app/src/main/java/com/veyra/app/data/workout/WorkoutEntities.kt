package com.veyra.app.data.workout

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long? = null,
    val restUntilEpochMs: Long? = null,
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
)
