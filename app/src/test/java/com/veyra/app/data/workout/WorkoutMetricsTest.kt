package com.veyra.app.data.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutMetricsTest {
    @Test
    fun `estimated one rep max uses the Epley formula`() {
        assertEquals(100.0, WorkoutMetrics.estimatedOneRepMaxKg(75.0, 10), 0.001)
        assertEquals(0.0, WorkoutMetrics.estimatedOneRepMaxKg(75.0, 0), 0.001)
    }

    @Test
    fun `volume only counts completed valid sets`() {
        val sets = listOf(
            workoutSet(weight = "75", reps = "8", completed = true),
            workoutSet(weight = "24.5", reps = "10", completed = true),
            workoutSet(weight = "100", reps = "5", completed = false),
            workoutSet(weight = "", reps = "8", completed = true),
        )

        assertEquals(845.0, WorkoutMetrics.volumeKg(sets), 0.001)
        assertEquals(3 to 4, WorkoutMetrics.completion(sets))
    }

    private fun workoutSet(weight: String, reps: String, completed: Boolean) = WorkoutSetEntity(
        workoutId = 1,
        exerciseKey = "bench",
        exerciseName = "Bench press",
        equipment = "Barbell",
        exercisePosition = 0,
        setNumber = 1,
        previous = "70 × 8",
        weightKg = weight,
        reps = reps,
        completedAtEpochMs = if (completed) 1 else null,
    )
}
