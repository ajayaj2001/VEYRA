package com.aura.data.model

import com.aura.data.exercise.ExerciseStore

data class Exercise(
    val id: String,
    val name: String,
    val category: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String,
    val instructions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val bodyParts: List<String> = emptyList(),
    val imageUrl: String? = null,
    val overview: String? = null,
    val remote: Boolean = false,
)

object ExerciseLib {
    val all = listOf(
        Exercise("bench_press", "Barbell Bench Press", "Compound", listOf("Chest"), listOf("Triceps", "Front Delt"), "Barbell",
            listOf("Lie flat on bench, feet on floor", "Grip bar slightly wider than shoulders", "Lower bar to mid-chest with control", "Press back up to lockout"),
            listOf("Retract shoulder blades", "Drive feet into floor")),
        Exercise("incline_bench", "Incline Bench Press", "Compound", listOf("Upper Chest"), listOf("Triceps", "Front Delt"), "Barbell",
            listOf("Set bench to 30-45 degrees", "Grip bar wider than shoulders", "Lower to upper chest", "Press back up"),
            listOf("Keep back on bench", "Squeeze upper chest at top")),
        Exercise("db_bench", "Dumbbell Bench Press", "Compound", listOf("Chest"), listOf("Triceps", "Front Delt"), "Dumbbell",
            listOf("Lie on flat bench with dumbbells", "Lower to sides of chest", "Press up, bringing slightly together")),
        Exercise("squat", "Barbell Back Squat", "Compound", listOf("Quads", "Glutes"), listOf("Hamstrings", "Core"), "Barbell",
            listOf("Bar on upper back, feet shoulder width", "Brace core, squat to parallel", "Drive through feet to stand"),
            listOf("Knees track over toes", "Chest up throughout")),
        Exercise("deadlift", "Barbell Deadlift", "Compound", listOf("Back", "Hamstrings", "Glutes"), listOf("Quads", "Traps", "Forearms"), "Barbell",
            listOf("Stand with feet hip width, bar over mid-foot", "Hinge hips, grip bar outside legs", "Brace core, drive through heels", "Lock out hips and knees together"),
            listOf("Keep bar close to body", "Push floor away, don't pull")),
        Exercise("ohp", "Overhead Press", "Compound", listOf("Shoulders"), listOf("Triceps", "Upper Chest"), "Barbell",
            listOf("Bar at shoulder level, feet shoulder width", "Press overhead to full extension", "Lower with control")),
        Exercise("barbell_row", "Barbell Row", "Compound", listOf("Back", "Lats"), listOf("Biceps", "Rear Delt"), "Barbell",
            listOf("Hinge forward at 45 degrees", "Pull bar to lower chest/upper abdomen", "Lower with control")),
        Exercise("pull_up", "Pull Up", "Compound", listOf("Lats"), listOf("Biceps", "Upper Back"), "Bodyweight",
            listOf("Hang with palms facing away", "Pull chin over bar", "Lower with control")),
        Exercise("lat_pulldown", "Lat Pulldown", "Compound", listOf("Lats"), listOf("Biceps", "Upper Back"), "Cable",
            listOf("Sit with thighs secured", "Grip bar wider than shoulders", "Pull to upper chest", "Return slowly")),
        Exercise("leg_press", "Leg Press", "Compound", listOf("Quads", "Glutes"), listOf("Hamstrings"), "Machine",
            listOf("Feet shoulder width on platform", "Lower by bending knees", "Press back without locking knees")),
        Exercise("rdl", "Romanian Deadlift", "Compound", listOf("Hamstrings", "Glutes"), listOf("Lower Back"), "Barbell",
            listOf("Hold barbell at hip level", "Push hips back, lower along legs", "Feel hamstring stretch", "Drive hips forward to return")),
        Exercise("lateral_raise", "Lateral Raise", "Isolation", listOf("Side Delt"), emptyList(), "Dumbbell",
            listOf("Stand with dumbbells at sides", "Raise out to shoulder level", "Lower slowly")),
        Exercise("barbell_curl", "Barbell Curl", "Isolation", listOf("Biceps"), listOf("Forearms"), "Barbell",
            listOf("Stand with underhand grip", "Curl to shoulders", "Lower with control")),
        Exercise("tricep_pushdown", "Tricep Pushdown", "Isolation", listOf("Triceps"), emptyList(), "Cable",
            listOf("Stand at cable, bar at chest height", "Push down to full extension", "Return slowly")),
        Exercise("leg_curl", "Leg Curl", "Isolation", listOf("Hamstrings"), emptyList(), "Machine",
            listOf("Lie face down on machine", "Curl legs toward glutes", "Lower with control")),
        Exercise("leg_ext", "Leg Extension", "Isolation", listOf("Quads"), emptyList(), "Machine",
            listOf("Sit with ankles behind pad", "Extend legs fully", "Lower with control")),
        Exercise("calf_raise", "Calf Raise", "Isolation", listOf("Calves"), emptyList(), "Machine",
            listOf("Stand on platform edge", "Raise heels high", "Lower below platform for stretch")),
        Exercise("face_pull", "Face Pull", "Isolation", listOf("Rear Delt", "Upper Back"), emptyList(), "Cable",
            listOf("Cable at upper chest height", "Pull rope toward face", "Externally rotate at top")),
        Exercise("plank", "Plank", "Isolation", listOf("Core"), listOf("Shoulders"), "Bodyweight",
            listOf("Forearm plank position", "Keep body straight", "Hold for duration")),
        Exercise("cable_fly", "Cable Fly", "Isolation", listOf("Chest"), emptyList(), "Cable",
            listOf("Set cables to shoulder height", "Bring hands together in front", "Return with arms wide")),
    )

    fun getById(id: String) = all.find { it.id == id } ?: ExerciseStore.get(id)

    fun search(query: String): List<Exercise> {
        val q = query.lowercase()
        return all.filter {
            it.name.lowercase().contains(q) ||
            it.primaryMuscles.any { m -> m.lowercase().contains(q) } ||
            it.equipment.lowercase().contains(q)
        }
    }

    val muscleGroups = listOf("Chest", "Back", "Shoulders", "Quads", "Hamstrings", "Glutes", "Biceps", "Triceps", "Core", "Calves")
    val equipmentList = listOf("Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight")
}
