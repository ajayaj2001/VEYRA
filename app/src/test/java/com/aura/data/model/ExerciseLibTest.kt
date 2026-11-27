package com.aura.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ExerciseLibTest {
    private val file = File("src/main/assets/exercises/exercises.json")
    private val library by lazy { file.bufferedReader().use(ExerciseLib::parse) }

    // The data file is kept out of the repository, so a fresh clone has nothing to check.
    @Before
    fun requireDataFile() = assumeTrue(file.exists())

    @Test
    fun readsEveryBundledExercise() {
        assertEquals(455, library.size)
        assertEquals(library.size, library.map { it.id }.toSet().size)
        assertTrue(library.all { it.name.isNotBlank() && it.primaryMuscles.isNotEmpty() })
    }

    @Test
    fun readsAllFieldsOfAnExercise() {
        val press = library.single { it.name == "Incline Chest Press (Machine)" }

        assertEquals("Weight & Reps", press.category)
        assertEquals("Machine", press.equipment)
        assertEquals(listOf("Chest"), press.primaryMuscles)
        assertTrue(press.instructions.isNotEmpty())
        assertTrue(press.imageUrl!!.endsWith(".jpg"))
        assertTrue(press.videoUrl!!.endsWith(".mp4"))
    }

    @Test
    fun keepsExercisesWithoutMediaAndListsPopularOnesFirst() {
        val climbing = library.single { it.name == "Climbing" }
        assertNull(climbing.imageUrl)
        assertNull(climbing.videoUrl)
        assertEquals(library.maxOf { it.priority }, library.first().priority)
        assertTrue("Farmers Carry" in library.flatMap { it.searchTerms })
    }
}
