package com.veyra.app.data.workout

object WorkoutMetrics {
    fun volumeKg(sets: List<WorkoutSetEntity>): Double = sets
        .filter { it.completedAtEpochMs != null }
        .sumOf { (it.weightKg.toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0) }

    fun completion(sets: List<WorkoutSetEntity>): Pair<Int, Int> =
        sets.count { it.completedAtEpochMs != null } to sets.size
}
