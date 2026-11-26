package com.aura.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class HealthConnectRepository(private val context: Context) {
    val availability: Int
        get() = HealthConnectClient.getSdkStatus(context)

    private val corePermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    )

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    @OptIn(ExperimentalFeatureAvailabilityApi::class)
    val requestPermissions: Set<String>
        get() = buildSet {
            addAll(corePermissions)
            if (client.features.getFeatureStatus(
                    HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY
                ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
            ) {
                add(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY)
            }
        }

    suspend fun hasRequiredPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(corePermissions)
    }

    suspend fun readSnapshot(now: Instant = Instant.now()): HealthSnapshot = coroutineScope {
        val zone = ZoneId.systemDefault()
        val historyStart = now.minus(Duration.ofDays(29))
        val dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()

        val sleepDeferred = async {
            client.readRecords(
                ReadRecordsRequest<SleepSessionRecord>(
                    timeRangeFilter = TimeRangeFilter.between(historyStart, now),
                    ascendingOrder = false,
                    pageSize = 1000,
                )
            ).records
        }
        val hrvDeferred = async {
            client.readRecords(
                ReadRecordsRequest<HeartRateVariabilityRmssdRecord>(
                    timeRangeFilter = TimeRangeFilter.between(historyStart, now),
                    ascendingOrder = false,
                    pageSize = 1000,
                )
            ).records
        }
        val rhrDeferred = async {
            client.readRecords(
                ReadRecordsRequest<RestingHeartRateRecord>(
                    timeRangeFilter = TimeRangeFilter.between(historyStart, now),
                    ascendingOrder = false,
                    pageSize = 1000,
                )
            ).records
        }
        val heartRateDeferred = async {
            client.readRecords(
                ReadRecordsRequest<HeartRateRecord>(
                    timeRangeFilter = TimeRangeFilter.between(dayStart, now),
                    ascendingOrder = true,
                    pageSize = 1000,
                )
            ).records
        }
        val activityDeferred = async {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(dayStart, now),
                )
            )
        }

        val sleepRecords = sleepDeferred.await()
        val hrvRecords = hrvDeferred.await()
        val rhrRecords = rhrDeferred.await()
        val heartRateRecords = heartRateDeferred.await()
        val activity = activityDeferred.await()

        // Use the longest sleep ending on each date so a nap cannot replace the nightly session.
        val sleeps = sleepRecords.map { it.toSummary(zone) }
            .groupBy { it.endTime.atZone(zone).toLocalDate() }
            .values
            .map { sessions -> sessions.maxBy { it.asleepMinutes } }
            .sortedByDescending { it.endTime }
        // Fitbit generally emits one nightly HRV/RHR value. De-duplicate by local date defensively.
        val hrvValues = hrvRecords.sortedByDescending { it.time }
            .distinctBy { it.time.atZone(zone).toLocalDate() }
            .map { it.heartRateVariabilityMillis }
        val rhrValues = rhrRecords.sortedByDescending { it.time }
            .distinctBy { it.time.atZone(zone).toLocalDate() }
            .map { it.beatsPerMinute }
        val heartRateSamples = heartRateRecords.flatMap { it.samples }.sortedBy { it.time }
        val minutesSinceDayStart = Duration.between(dayStart, now).toMinutes().coerceAtLeast(1)

        val score = HealthScoreEngine.calculate(
            sleeps = sleeps,
            hrvValues = hrvValues,
            restingHeartRates = rhrValues,
            todayHeartRate = heartRateSamples,
            minutesSinceDayStart = minutesSinceDayStart,
        )

        val sourcePackages = buildSet {
            sleepRecords.mapTo(this) { it.metadata.dataOrigin.packageName }
            hrvRecords.mapTo(this) { it.metadata.dataOrigin.packageName }
            rhrRecords.mapTo(this) { it.metadata.dataOrigin.packageName }
            heartRateRecords.mapTo(this) { it.metadata.dataOrigin.packageName }
        }.filter(String::isNotBlank).toSet()

        val latestSourceUpdate = buildList {
            sleepRecords.mapTo(this) { it.endTime }
            hrvRecords.mapTo(this) { it.time }
            rhrRecords.mapTo(this) { it.time }
            heartRateRecords.mapTo(this) { it.endTime }
        }.maxOrNull()

        val latestSleep = sleeps.firstOrNull()
        HealthSnapshot(
            recoveryScore = score.recovery,
            sleepScore = score.sleep,
            strainScore = score.strain,
            recoveryNote = score.recoveryNote,
            sleepNote = score.sleepNote,
            strainNote = score.strainNote,
            hrvMs = hrvValues.firstOrNull(),
            restingHeartRate = rhrValues.firstOrNull(),
            sleepMinutes = latestSleep?.asleepMinutes,
            steps = activity[StepsRecord.COUNT_TOTAL] ?: 0L,
            activeCalories = activity[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                ?.inKilocalories?.toInt() ?: 0,
            heartRateCoverage = score.coverage,
            baselineDays = score.baselineDays,
            lastSourceUpdate = latestSourceUpdate,
            syncedAt = now,
            sourcePackages = sourcePackages,
        )
    }

    private fun SleepSessionRecord.toSummary(zone: ZoneId): SleepSummary {
        val sessionMinutes = Duration.between(startTime, endTime).toMinutes().coerceAtLeast(0)
        val awakeTypes = setOf(
            SleepSessionRecord.STAGE_TYPE_AWAKE,
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
        )
        val awakeMinutes = stages.filter { it.stage in awakeTypes }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        val asleepMinutes = if (stages.isEmpty()) sessionMinutes else {
            (sessionMinutes - awakeMinutes).coerceAtLeast(0)
        }
        val deepMinutes = stages.filter { it.stage == SleepSessionRecord.STAGE_TYPE_DEEP }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        val remMinutes = stages.filter { it.stage == SleepSessionRecord.STAGE_TYPE_REM }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        val localStart = startTime.atZone(zone)
        return SleepSummary(
            endTime = endTime,
            asleepMinutes = asleepMinutes,
            sessionMinutes = sessionMinutes,
            deepMinutes = deepMinutes,
            remMinutes = remMinutes,
            bedtimeMinuteOfDay = localStart.hour * 60 + localStart.minute,
        )
    }
}
