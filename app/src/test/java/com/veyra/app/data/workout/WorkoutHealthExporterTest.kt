package com.veyra.app.data.workout

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutHealthExporterTest {
    @Test
    fun `maps a completed workout to an idempotent Health Connect session`() {
        val workout = WorkoutEntity(
            id = 42,
            title = "Push · Strength",
            startedAtEpochMs = 1_000,
            finishedAtEpochMs = 61_000,
        )
        val sets = listOf(
            set(id = 1, workoutId = workout.id, weight = "75", reps = "8", completed = true),
            set(id = 2, workoutId = workout.id, weight = "80", reps = "8", completed = false),
        )

        val record = WorkoutWithSets(workout, sets).toExerciseSessionRecord(ZoneOffset.UTC)

        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING, record.exerciseType)
        assertEquals("Push · Strength", record.title)
        assertEquals("veyra-workout-42", record.metadata.clientRecordId)
        assertTrue(record.notes.orEmpty().contains("1/2 sets"))
        assertTrue(record.notes.orEmpty().contains("600 kg volume"))
    }

    @Test
    fun `ensures Health Connect interval is valid for an instant workout`() {
        val workout = WorkoutEntity(
            id = 1,
            title = "Quick workout",
            startedAtEpochMs = 1_000,
            finishedAtEpochMs = 1_000,
        )

        val record = WorkoutWithSets(workout, emptyList()).toExerciseSessionRecord(ZoneOffset.UTC)

        assertTrue(record.endTime.isAfter(record.startTime))
    }

    private fun set(
        id: Long,
        workoutId: Long,
        weight: String,
        reps: String,
        completed: Boolean,
    ) = WorkoutSetEntity(
        id = id,
        workoutId = workoutId,
        exerciseKey = "bench",
        exerciseName = "Barbell bench press",
        equipment = "Barbell · Bench",
        exercisePosition = 0,
        setNumber = id.toInt(),
        previous = "70 × 8",
        weightKg = weight,
        reps = reps,
        completedAtEpochMs = if (completed) 10_000 else null,
    )
}
