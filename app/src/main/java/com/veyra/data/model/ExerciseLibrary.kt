package com.veyra.data.model

data class Exercise(
    val id: String,
    val name: String,
    val category: ExerciseCategory,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle> = emptyList(),
    val equipment: Equipment,
    val instructions: List<String> = emptyList(),
    val coachingCues: List<String> = emptyList(),
    val imageUrl: String? = null,
    val videoUrl: String? = null,
)

enum class ExerciseCategory(val displayName: String) {
    COMPOUND("Compound"),
    ISOLATION("Isolation"),
    CARDIO("Cardio"),
    STRETCHING("Stretching"),
    PLYOMETRIC("Plyometric"),
    OLYMPIC("Olympic"),
}

enum class Muscle(val displayName: String, val group: MuscleGroup) {
    CHEST("Chest", MuscleGroup.CHEST),
    UPPER_CHEST("Upper Chest", MuscleGroup.CHEST),
    LOWER_CHEST("Lower Chest", MuscleGroup.CHEST),
    FRONT_DELT("Front Delt", MuscleGroup.SHOULDERS),
    SIDE_DELT("Side Delt", MuscleGroup.SHOULDERS),
    REAR_DELT("Rear Delt", MuscleGroup.SHOULDERS),
    BICEPS("Biceps", MuscleGroup.ARMS),
    TRICEPS("Triceps", MuscleGroup.ARMS),
    FOREARMS("Forearms", MuscleGroup.ARMS),
    LATS("Lats", MuscleGroup.BACK),
    UPPER_BACK("Upper Back", MuscleGroup.BACK),
    LOWER_BACK("Lower Back", MuscleGroup.BACK),
    TRAPS("Traps", MuscleGroup.BACK),
    QUADS("Quads", MuscleGroup.LEGS),
    HAMSTRINGS("Hamstrings", MuscleGroup.LEGS),
    GLUTES("Glutes", MuscleGroup.LEGS),
    CALVES("Calves", MuscleGroup.LEGS),
    ABS("Abs", MuscleGroup.CORE),
    OBLIQUES("Obliques", MuscleGroup.CORE),
    HIP_FLEXORS("Hip Flexors", MuscleGroup.LEGS),
}

enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    BACK("Back"),
    LEGS("Legs"),
    CORE("Core"),
}

enum class Equipment(val displayName: String) {
    BARBELL("Barbell"),
    DUMBBELL("Dumbbell"),
    CABLE("Cable"),
    MACHINE("Machine"),
    BODYWEIGHT("Bodyweight"),
    KETTLEBELL("Kettlebell"),
    BAND("Resistance Band"),
    SMITH_MACHINE("Smith Machine"),
    EZ_BAR("EZ Bar"),
    OTHER("Other"),
}

object ExerciseRepository {
    val exercises: List<Exercise> = listOf(
        // Chest
        Exercise(
            id = "bench_press",
            name = "Barbell Bench Press",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.CHEST),
            secondaryMuscles = listOf(Muscle.TRICEPS, Muscle.FRONT_DELT),
            equipment = Equipment.BARBELL,
            instructions = listOf(
                "Lie flat on a bench with feet flat on the floor.",
                "Grip the bar slightly wider than shoulder width.",
                "Unrack the bar and hold it above your chest with arms extended.",
                "Lower the bar to your mid-chest in a controlled motion.",
                "Press the bar back up to the starting position, driving through your chest.",
            ),
            coachingCues = listOf(
                "Retract and depress your shoulder blades throughout.",
                "Maintain a slight arch in your lower back.",
                "Drive your feet into the floor for stability.",
                "Keep your wrists straight and stacked over elbows.",
            ),
        ),
        Exercise(
            id = "incline_bench_press",
            name = "Incline Barbell Bench Press",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.UPPER_CHEST),
            secondaryMuscles = listOf(Muscle.TRICEPS, Muscle.FRONT_DELT),
            equipment = Equipment.BARBELL,
            instructions = listOf(
                "Set the bench to a 30-45 degree incline.",
                "Lie back and grip the bar slightly wider than shoulder width.",
                "Unrack and lower the bar to your upper chest.",
                "Press back up to full extension.",
            ),
            coachingCues = listOf(
                "Keep your back firmly against the bench.",
                "Focus on squeezing the upper chest at the top.",
            ),
        ),
        Exercise(
            id = "dumbbell_bench_press",
            name = "Dumbbell Bench Press",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.CHEST),
            secondaryMuscles = listOf(Muscle.TRICEPS, Muscle.FRONT_DELT),
            equipment = Equipment.DUMBBELL,
            instructions = listOf(
                "Lie on a flat bench holding dumbbells above your chest.",
                "Lower the dumbbells to the sides of your chest.",
                "Press them back up, bringing them slightly together at the top.",
            ),
            coachingCues = listOf(
                "Allow a deeper stretch at the bottom than barbell press.",
                "Keep your elbows at about 45 degrees from your body.",
            ),
        ),
        Exercise(
            id = "cable_fly",
            name = "Cable Fly",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.CHEST),
            secondaryMuscles = listOf(Muscle.FRONT_DELT),
            equipment = Equipment.CABLE,
            instructions = listOf(
                "Set cables to shoulder height. Stand in the center.",
                "With a slight bend in your elbows, bring hands together in front.",
                "Slowly return to the starting position with arms wide.",
            ),
            coachingCues = listOf("Squeeze your chest hard at the peak contraction."),
        ),
        Exercise(
            id = "push_up",
            name = "Push Up",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.CHEST),
            secondaryMuscles = listOf(Muscle.TRICEPS, Muscle.FRONT_DELT, Muscle.ABS),
            equipment = Equipment.BODYWEIGHT,
            instructions = listOf(
                "Start in a plank position, hands slightly wider than shoulders.",
                "Lower your body until your chest nearly touches the floor.",
                "Push yourself back up to the starting position.",
            ),
            coachingCues = listOf("Keep your body in a straight line from head to heels."),
        ),

        // Back
        Exercise(
            id = "deadlift",
            name = "Barbell Deadlift",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.LOWER_BACK, Muscle.HAMSTRINGS, Muscle.GLUTES),
            secondaryMuscles = listOf(Muscle.QUADS, Muscle.TRAPS, Muscle.FOREARMS, Muscle.LATS),
            equipment = Equipment.BARBELL,
            instructions = listOf(
                "Stand with feet hip-width apart, bar over mid-foot.",
                "Hinge at the hips and grip the bar just outside your legs.",
                "Brace your core, flatten your back, and drive through your heels.",
                "Stand up tall, locking out hips and knees simultaneously.",
                "Lower the bar back down by hinging at the hips first.",
            ),
            coachingCues = listOf(
                "Keep the bar close to your body throughout the lift.",
                "Think of pushing the floor away rather than pulling the bar up.",
                "Squeeze your glutes at lockout.",
            ),
        ),
        Exercise(
            id = "barbell_row",
            name = "Barbell Bent-Over Row",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.LATS, Muscle.UPPER_BACK),
            secondaryMuscles = listOf(Muscle.BICEPS, Muscle.REAR_DELT, Muscle.LOWER_BACK),
            equipment = Equipment.BARBELL,
            instructions = listOf(
                "Hinge forward at the hips to about 45 degrees.",
                "Hold the barbell with an overhand grip, arms hanging down.",
                "Pull the bar to your lower chest/upper abdomen.",
                "Lower the bar back down with control.",
            ),
            coachingCues = listOf(
                "Initiate the pull by retracting your shoulder blades.",
                "Keep your core braced and back flat.",
            ),
        ),
        Exercise(
            id = "pull_up",
            name = "Pull Up",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.LATS),
            secondaryMuscles = listOf(Muscle.BICEPS, Muscle.UPPER_BACK, Muscle.FOREARMS),
            equipment = Equipment.BODYWEIGHT,
            instructions = listOf(
                "Hang from a bar with palms facing away, hands wider than shoulders.",
                "Pull yourself up until your chin is over the bar.",
                "Lower yourself back down with control.",
            ),
            coachingCues = listOf(
                "Engage your lats by pulling your elbows down and back.",
                "Avoid swinging or kipping.",
            ),
        ),
        Exercise(
            id = "lat_pulldown",
            name = "Lat Pulldown",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.LATS),
            secondaryMuscles = listOf(Muscle.BICEPS, Muscle.UPPER_BACK),
            equipment = Equipment.CABLE,
            instructions = listOf(
                "Sit at the pulldown machine, thighs secured under the pads.",
                "Grip the bar wider than shoulder width.",
                "Pull the bar down to your upper chest.",
                "Slowly return to the starting position.",
            ),
            coachingCues = listOf("Lean back slightly and drive your elbows to your sides."),
        ),
        Exercise(
            id = "seated_cable_row",
            name = "Seated Cable Row",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.UPPER_BACK, Muscle.LATS),
            secondaryMuscles = listOf(Muscle.BICEPS, Muscle.REAR_DELT),
            equipment = Equipment.CABLE,
            instructions = listOf(
                "Sit at the cable row station with feet on the platform.",
                "Grab the handle, sit upright with arms extended.",
                "Pull the handle to your abdomen, squeezing your back.",
                "Slowly extend arms back to start.",
            ),
            coachingCues = listOf("Keep your torso stationary; don't lean back excessively."),
        ),

        // Shoulders
        Exercise(
            id = "overhead_press",
            name = "Barbell Overhead Press",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.FRONT_DELT, Muscle.SIDE_DELT),
            secondaryMuscles = listOf(Muscle.TRICEPS, Muscle.UPPER_CHEST, Muscle.TRAPS),
            equipment = Equipment.BARBELL,
            instructions = listOf(
                "Stand with feet shoulder-width apart, bar at shoulder level.",
                "Press the bar overhead until arms are fully extended.",
                "Lower the bar back to shoulder level with control.",
            ),
            coachingCues = listOf(
                "Brace your core and squeeze your glutes.",
                "Push your head through once the bar passes your forehead.",
            ),
        ),
        Exercise(
            id = "lateral_raise",
            name = "Dumbbell Lateral Raise",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.SIDE_DELT),
            equipment = Equipment.DUMBBELL,
            instructions = listOf(
                "Stand with dumbbells at your sides.",
                "Raise the dumbbells out to the sides until arms are parallel to the floor.",
                "Lower back down slowly.",
            ),
            coachingCues = listOf(
                "Lead with your elbows, not your hands.",
                "Use a slight forward lean for better delt activation.",
            ),
        ),
        Exercise(
            id = "face_pull",
            name = "Face Pull",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.REAR_DELT, Muscle.UPPER_BACK),
            secondaryMuscles = listOf(Muscle.TRAPS),
            equipment = Equipment.CABLE,
            instructions = listOf(
                "Set a cable to upper chest height with a rope attachment.",
                "Pull the rope toward your face, separating the ends.",
                "Squeeze your rear delts and upper back at the peak.",
                "Return to start with control.",
            ),
            coachingCues = listOf("Externally rotate your shoulders at the top of the movement."),
        ),

        // Legs
        Exercise(
            id = "squat",
            name = "Barbell Back Squat",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.QUADS, Muscle.GLUTES),
            secondaryMuscles = listOf(Muscle.HAMSTRINGS, Muscle.LOWER_BACK, Muscle.ABS),
            equipment = Equipment.BARBELL,
            instructions = listOf(
                "Position the bar on your upper back (high bar) or rear delts (low bar).",
                "Stand with feet shoulder-width apart, toes slightly out.",
                "Brace your core and squat down until thighs are at least parallel.",
                "Drive through your feet to stand back up.",
            ),
            coachingCues = listOf(
                "Keep your chest up and knees tracking over toes.",
                "Sit back into the squat like sitting in a chair.",
                "Drive your knees out on the way up.",
            ),
        ),
        Exercise(
            id = "leg_press",
            name = "Leg Press",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.QUADS, Muscle.GLUTES),
            secondaryMuscles = listOf(Muscle.HAMSTRINGS),
            equipment = Equipment.MACHINE,
            instructions = listOf(
                "Sit in the leg press machine with feet shoulder-width apart on the platform.",
                "Lower the platform by bending your knees toward your chest.",
                "Press the platform back up without fully locking your knees.",
            ),
            coachingCues = listOf("Don't let your lower back round off the pad."),
        ),
        Exercise(
            id = "romanian_deadlift",
            name = "Romanian Deadlift",
            category = ExerciseCategory.COMPOUND,
            primaryMuscles = listOf(Muscle.HAMSTRINGS, Muscle.GLUTES),
            secondaryMuscles = listOf(Muscle.LOWER_BACK),
            equipment = Equipment.BARBELL,
            instructions = listOf(
                "Hold the barbell at hip level with an overhand grip.",
                "Push your hips back, lowering the bar along your legs.",
                "Lower until you feel a deep stretch in your hamstrings.",
                "Drive your hips forward to return to standing.",
            ),
            coachingCues = listOf(
                "Keep the bar close to your legs throughout.",
                "Maintain a slight bend in your knees.",
            ),
        ),
        Exercise(
            id = "leg_curl",
            name = "Lying Leg Curl",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.HAMSTRINGS),
            equipment = Equipment.MACHINE,
            instructions = listOf(
                "Lie face down on the leg curl machine.",
                "Curl your legs up toward your glutes.",
                "Lower back down with control.",
            ),
            coachingCues = listOf("Don't let your hips lift off the pad."),
        ),
        Exercise(
            id = "leg_extension",
            name = "Leg Extension",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.QUADS),
            equipment = Equipment.MACHINE,
            instructions = listOf(
                "Sit on the leg extension machine with ankles behind the pad.",
                "Extend your legs until fully straight.",
                "Lower back down with control.",
            ),
            coachingCues = listOf("Squeeze your quads hard at the top."),
        ),
        Exercise(
            id = "calf_raise",
            name = "Standing Calf Raise",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.CALVES),
            equipment = Equipment.MACHINE,
            instructions = listOf(
                "Stand on the calf raise platform with the balls of your feet on the edge.",
                "Raise your heels as high as possible.",
                "Lower back down below the platform level for a full stretch.",
            ),
            coachingCues = listOf("Pause at the top for a 2-second squeeze."),
        ),

        // Arms
        Exercise(
            id = "barbell_curl",
            name = "Barbell Curl",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.BICEPS),
            secondaryMuscles = listOf(Muscle.FOREARMS),
            equipment = Equipment.BARBELL,
            instructions = listOf(
                "Stand with feet hip-width apart holding a barbell with an underhand grip.",
                "Curl the bar up toward your shoulders.",
                "Lower back down with control.",
            ),
            coachingCues = listOf(
                "Keep your elbows pinned to your sides.",
                "Don't swing the weight; use strict form.",
            ),
        ),
        Exercise(
            id = "tricep_pushdown",
            name = "Tricep Pushdown",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.TRICEPS),
            equipment = Equipment.CABLE,
            instructions = listOf(
                "Stand at a cable machine with a straight bar or rope at chest height.",
                "Push the attachment down until your arms are fully extended.",
                "Slowly return to the starting position.",
            ),
            coachingCues = listOf("Keep your elbows close to your body and stationary."),
        ),
        Exercise(
            id = "hammer_curl",
            name = "Hammer Curl",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.BICEPS),
            secondaryMuscles = listOf(Muscle.FOREARMS),
            equipment = Equipment.DUMBBELL,
            instructions = listOf(
                "Hold dumbbells with palms facing each other (neutral grip).",
                "Curl the weights up toward your shoulders.",
                "Lower with control.",
            ),
            coachingCues = listOf("Maintain the neutral grip throughout the movement."),
        ),

        // Core
        Exercise(
            id = "plank",
            name = "Plank",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.ABS),
            secondaryMuscles = listOf(Muscle.OBLIQUES, Muscle.LOWER_BACK),
            equipment = Equipment.BODYWEIGHT,
            instructions = listOf(
                "Get into a push-up position but rest on your forearms.",
                "Keep your body in a straight line from head to heels.",
                "Hold the position for the desired duration.",
            ),
            coachingCues = listOf(
                "Squeeze your glutes and brace your core.",
                "Don't let your hips sag or pike up.",
            ),
        ),
        Exercise(
            id = "cable_crunch",
            name = "Cable Crunch",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.ABS),
            equipment = Equipment.CABLE,
            instructions = listOf(
                "Kneel in front of a high cable with a rope attachment.",
                "Hold the rope behind your head.",
                "Crunch down, bringing your elbows toward your knees.",
                "Return to the starting position with control.",
            ),
            coachingCues = listOf("Curl your spine; don't just bend at the hips."),
        ),
        Exercise(
            id = "hanging_leg_raise",
            name = "Hanging Leg Raise",
            category = ExerciseCategory.ISOLATION,
            primaryMuscles = listOf(Muscle.ABS, Muscle.HIP_FLEXORS),
            equipment = Equipment.BODYWEIGHT,
            instructions = listOf(
                "Hang from a pull-up bar with arms extended.",
                "Raise your legs until they're parallel to the floor (or higher).",
                "Lower back down with control.",
            ),
            coachingCues = listOf("Avoid swinging. Curl your pelvis up at the top."),
        ),
    )

    fun getById(id: String): Exercise? = exercises.find { it.id == id }

    fun search(query: String): List<Exercise> {
        val lower = query.lowercase()
        return exercises.filter {
            it.name.lowercase().contains(lower) ||
                it.primaryMuscles.any { m -> m.displayName.lowercase().contains(lower) } ||
                it.equipment.displayName.lowercase().contains(lower) ||
                it.category.displayName.lowercase().contains(lower)
        }
    }

    fun filterByMuscle(muscle: Muscle): List<Exercise> =
        exercises.filter { muscle in it.primaryMuscles || muscle in it.secondaryMuscles }

    fun filterByEquipment(equipment: Equipment): List<Exercise> =
        exercises.filter { it.equipment == equipment }

    fun filterByMuscleGroup(group: MuscleGroup): List<Exercise> =
        exercises.filter { ex ->
            ex.primaryMuscles.any { it.group == group } ||
                ex.secondaryMuscles.any { it.group == group }
        }
}
