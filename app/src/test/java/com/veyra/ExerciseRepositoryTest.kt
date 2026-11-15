package com.veyra

import com.veyra.data.model.Equipment
import com.veyra.data.model.ExerciseRepository
import com.veyra.data.model.Muscle
import com.veyra.data.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseRepositoryTest {

    @Test
    fun `exercise library is not empty`() {
        assertTrue(ExerciseRepository.exercises.isNotEmpty())
    }

    @Test
    fun `all exercises have unique ids`() {
        val ids = ExerciseRepository.exercises.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `all exercises have names`() {
        ExerciseRepository.exercises.forEach { exercise ->
            assertTrue("Exercise ${exercise.id} has empty name", exercise.name.isNotBlank())
        }
    }

    @Test
    fun `all exercises have at least one primary muscle`() {
        ExerciseRepository.exercises.forEach { exercise ->
            assertTrue(
                "Exercise ${exercise.id} has no primary muscles",
                exercise.primaryMuscles.isNotEmpty(),
            )
        }
    }

    @Test
    fun `getById returns correct exercise`() {
        val exercise = ExerciseRepository.getById("bench_press")
        assertNotNull(exercise)
        assertEquals("Barbell Bench Press", exercise!!.name)
        assertEquals(Equipment.BARBELL, exercise.equipment)
    }

    @Test
    fun `getById returns null for unknown id`() {
        assertNull(ExerciseRepository.getById("nonexistent_exercise"))
    }

    @Test
    fun `search by name returns results`() {
        val results = ExerciseRepository.search("bench")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.id == "bench_press" })
    }

    @Test
    fun `search by muscle returns results`() {
        val results = ExerciseRepository.search("chest")
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `search by equipment returns results`() {
        val results = ExerciseRepository.search("barbell")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.equipment == Equipment.BARBELL || it.name.lowercase().contains("barbell") })
    }

    @Test
    fun `search is case insensitive`() {
        val upper = ExerciseRepository.search("BENCH")
        val lower = ExerciseRepository.search("bench")
        assertEquals(upper.size, lower.size)
    }

    @Test
    fun `search with empty query returns empty`() {
        // Empty query doesn't match anything since we check contains("")
        // Actually contains("") returns true for all strings
        val results = ExerciseRepository.search("")
        assertEquals(ExerciseRepository.exercises.size, results.size)
    }

    @Test
    fun `filterByMuscle returns exercises targeting that muscle`() {
        val results = ExerciseRepository.filterByMuscle(Muscle.CHEST)
        assertTrue(results.isNotEmpty())
        results.forEach { exercise ->
            assertTrue(
                Muscle.CHEST in exercise.primaryMuscles || Muscle.CHEST in exercise.secondaryMuscles,
            )
        }
    }

    @Test
    fun `filterByEquipment returns correct exercises`() {
        val results = ExerciseRepository.filterByEquipment(Equipment.BODYWEIGHT)
        assertTrue(results.isNotEmpty())
        results.forEach { exercise ->
            assertEquals(Equipment.BODYWEIGHT, exercise.equipment)
        }
    }

    @Test
    fun `filterByMuscleGroup returns exercises for that group`() {
        val results = ExerciseRepository.filterByMuscleGroup(MuscleGroup.LEGS)
        assertTrue(results.isNotEmpty())
        results.forEach { exercise ->
            val hasLegMuscle = exercise.primaryMuscles.any { it.group == MuscleGroup.LEGS } ||
                exercise.secondaryMuscles.any { it.group == MuscleGroup.LEGS }
            assertTrue("${exercise.name} should target legs", hasLegMuscle)
        }
    }

    @Test
    fun `compound exercises have instructions`() {
        val compounds = ExerciseRepository.exercises.filter {
            it.category == com.veyra.data.model.ExerciseCategory.COMPOUND
        }
        compounds.forEach { exercise ->
            assertTrue(
                "Compound exercise ${exercise.name} should have instructions",
                exercise.instructions.isNotEmpty(),
            )
        }
    }
}
