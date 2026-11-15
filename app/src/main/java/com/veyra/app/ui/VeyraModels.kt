package com.veyra.app.ui

import androidx.compose.ui.graphics.Color
import com.veyra.app.ui.theme.Acid
import com.veyra.app.ui.theme.Recovery
import com.veyra.app.ui.theme.Sleep
import com.veyra.app.ui.theme.Strain
import com.veyra.app.data.workout.WorkoutCatalog

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
    val instructions: List<String> = emptyList(),
    val accent: Color = Strain,
    val key: String = name.lowercase().replace(" ", "_"),
)

val dashboardScores = listOf(
    Score("Recovery", 72, note = "Ready for a productive session", color = Recovery),
    Score("Sleep", 84, note = "7h 48m · steady timing", color = Sleep),
    Score("Strain", 6, "/21", "Target 13–16 today", Strain)
)

val exercises = WorkoutCatalog.exercises.map { template ->
    Exercise(
        name = template.name,
        equipment = template.equipment,
        primaryMuscles = template.primaryMuscles,
        secondaryMuscles = template.secondaryMuscles,
        cue = template.cue,
        instructions = template.instructions,
        accent = if (template.key == "squat") Acid else Strain,
        key = template.key,
    )
}
