package com.veyra.app.data.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCatalogTest {
    @Test
    fun `catalog keys are unique and every exercise can seed a routine`() {
        val catalog = WorkoutCatalog.exercises

        assertEquals(catalog.size, catalog.map { it.key }.distinct().size)
        assertTrue(catalog.size >= 9)
        catalog.forEachIndexed { index, exercise ->
            val seeded = exercise.toRoutineExercise(routineId = 7, position = index)
            assertEquals(exercise.key, seeded.exerciseKey)
            assertEquals(index, seeded.position)
            assertTrue(seeded.targetSets > 0)
            assertTrue(seeded.restSeconds >= 0)
        }
    }

    @Test
    fun `all seeded split exercises exist in the catalog`() {
        val seededKeys = setOf(
            "incline_press", "bench", "triceps_extension",
            "lat_pulldown", "row", "biceps_curl",
            "squat", "leg_press", "leg_curl",
        )

        assertTrue(seededKeys.all { WorkoutCatalog.byKey(it) != null })
    }
}
