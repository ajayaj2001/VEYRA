package com.veyra.app.ui

import androidx.compose.ui.graphics.Color
import com.veyra.app.ui.theme.Acid
import com.veyra.app.ui.theme.Recovery
import com.veyra.app.ui.theme.Sleep
import com.veyra.app.ui.theme.Strain

enum class VeyraTab(val label: String) { Today("Today"), Train("Train"), Fuel("Fuel"), Coach("Coach"), You("You") }

data class Score(
    val label: String,
    val value: Int?,
    val unit: String = "",
    val note: String,
    val color: Color
)

data class Exercise(
    val name: String,
    val equipment: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val cue: String,
    val accent: Color = Strain
)

data class WorkoutExercise(
    val exercise: Exercise,
    val sets: List<WorkoutSet>
)

data class WorkoutSet(
    val previous: String,
    val weight: String,
    val reps: String
)

val dashboardScores = listOf(
    Score("Recovery", 72, note = "Ready for a productive session", color = Recovery),
    Score("Sleep", 84, note = "7h 48m · steady timing", color = Sleep),
    Score("Strain", 6, "/21", "Target 13–16 today", Strain)
)

val exercises = listOf(
    Exercise(
        name = "Barbell bench press",
        equipment = "Barbell · Bench",
        primaryMuscles = listOf("Chest"),
        secondaryMuscles = listOf("Triceps", "Front delts"),
        cue = "Lower with control. Keep shoulder blades pinned and press slightly back."
    ),
    Exercise(
        name = "Back squat",
        equipment = "Barbell · Rack",
        primaryMuscles = listOf("Quadriceps", "Glutes"),
        secondaryMuscles = listOf("Adductors", "Core"),
        cue = "Brace before descending. Keep the bar over mid-foot and drive the floor away.",
        accent = Acid
    ),
    Exercise(
        name = "Chest-supported row",
        equipment = "Dumbbells · Incline bench",
        primaryMuscles = listOf("Upper back", "Lats"),
        secondaryMuscles = listOf("Rear delts", "Biceps"),
        cue = "Keep the chest planted. Pull elbows toward the hips without shrugging."
    )
)

val sampleWorkout = listOf(
    WorkoutExercise(
        exercises[0],
        listOf(
            WorkoutSet("70 × 10", "70", "10"),
            WorkoutSet("75 × 8", "75", "8"),
            WorkoutSet("75 × 7", "75", "8")
        )
    ),
    WorkoutExercise(
        Exercise("Incline dumbbell press", "Dumbbells · Bench", listOf("Upper chest"), listOf("Triceps"), "Set the bench to 30°. Keep wrists stacked over elbows."),
        listOf(
            WorkoutSet("24 × 12", "24", "12"),
            WorkoutSet("26 × 10", "26", "10"),
            WorkoutSet("26 × 9", "26", "10")
        )
    )
)
