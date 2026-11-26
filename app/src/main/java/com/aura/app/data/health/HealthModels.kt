package com.aura.app.data.health

import java.time.Instant

sealed interface HealthDashboardState {
    data object Loading : HealthDashboardState
    data object NeedsPermissions : HealthDashboardState
    data object ProviderUpdateRequired : HealthDashboardState
    data object Unavailable : HealthDashboardState
    data class Ready(val snapshot: HealthSnapshot) : HealthDashboardState
    data class Error(val message: String) : HealthDashboardState
}

data class HealthSnapshot(
    val recoveryScore: Int?,
    val sleepScore: Int?,
    val strainScore: Int,
    val recoveryNote: String,
    val sleepNote: String,
    val strainNote: String,
    val hrvMs: Double?,
    val restingHeartRate: Long?,
    val sleepMinutes: Long?,
    val steps: Long,
    val activeCalories: Int,
    val heartRateCoverage: Float,
    val baselineDays: Int,
    val lastSourceUpdate: Instant?,
    val syncedAt: Instant,
    val sourcePackages: Set<String>,
)

internal data class SleepSummary(
    val endTime: Instant,
    val asleepMinutes: Long,
    val sessionMinutes: Long,
    val deepMinutes: Long,
    val remMinutes: Long,
    val bedtimeMinuteOfDay: Int,
)

internal data class ScoreResult(
    val recovery: Int?,
    val sleep: Int?,
    val strain: Int,
    val recoveryNote: String,
    val sleepNote: String,
    val strainNote: String,
    val coverage: Float,
    val baselineDays: Int,
)
