package com.aura.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MuscleIntensityTest {
    private fun exercise(primary: List<String>, secondary: List<String> = emptyList()) =
        Exercise("id", "name", "Weight & Reps", primary, secondary, "Barbell")

    @Test
    fun primaryMusclesCountFullyAndSecondaryHalf() {
        val intensity = muscleIntensity(listOf(exercise(listOf("Chest"), listOf("Triceps", "Shoulders"))))

        assertEquals(mapOf("chest" to 1f, "triceps" to 0.5f, "shoulders" to 0.5f), intensity)
    }

    @Test
    fun aMuscleKeepsItsHighestValueWhateverTheOrder() {
        val press = exercise(listOf("Chest"), listOf("Triceps"))
        val pushdown = exercise(listOf("Triceps"))

        assertEquals(1f, muscleIntensity(listOf(press, pushdown))["triceps"])
        assertEquals(1f, muscleIntensity(listOf(pushdown, press))["triceps"])
    }

    @Test
    fun keysMatchTheImageFileNames() {
        assertEquals(setOf("upper_back", "lower_back"), muscleIntensity(listOf(exercise(listOf("Upper Back", "Lower Back")))).keys)
    }

    @Test
    fun noExercisesWorkNoMuscles() {
        assertEquals(emptyMap<String, Float>(), muscleIntensity(emptyList()))
    }
}
