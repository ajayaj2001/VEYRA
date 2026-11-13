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
