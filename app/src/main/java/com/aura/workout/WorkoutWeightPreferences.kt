package com.aura.workout

import android.content.Context

internal class WorkoutWeightPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("workout_display_units", Context.MODE_PRIVATE)

    fun poundExercises(workoutId: Long): Set<String> =
        preferences.getStringSet(key(workoutId), null)?.toSet().orEmpty()

    fun save(workoutId: Long, exerciseIds: Set<String>) {
        preferences.edit().apply {
            if (exerciseIds.isEmpty()) remove(key(workoutId))
            else putStringSet(key(workoutId), exerciseIds)
        }.apply()
    }

    private fun key(workoutId: Long) = "pounds_exercises_$workoutId"
}
