package com.aura.app.data.workout

object WorkoutMetrics {
    fun estimatedOneRepMaxKg(weightKg: Double, reps: Int): Double =
        if (weightKg <= 0.0 || reps <= 0) 0.0 else weightKg * (1.0 + reps / 30.0)

    fun volumeKg(sets: List<WorkoutSetEntity>): Double = sets
        .filter { it.completedAtEpochMs != null }
        .sumOf { (it.weightKg.toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0) }

    fun completion(sets: List<WorkoutSetEntity>): Pair<Int, Int> =
        sets.count { it.completedAtEpochMs != null } to sets.size
}
