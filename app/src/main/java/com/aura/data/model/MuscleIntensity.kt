package com.aura.data.model

/**
 * How hard each muscle is worked by [exercises], keyed like the body heatmap images
 * ("Upper Back" -> "upper_back"). Primary muscles count fully, secondary ones half,
 * and a muscle keeps its highest value.
 */
fun muscleIntensity(exercises: List<Exercise>): Map<String, Float> = buildMap {
    for (exercise in exercises) {
        exercise.primaryMuscles.forEach { put(it.muscleKey(), 1f) }
        exercise.secondaryMuscles.forEach { merge(it.muscleKey(), 0.5f, ::maxOf) }
    }
}

private fun String.muscleKey() = lowercase().replace(' ', '_')
