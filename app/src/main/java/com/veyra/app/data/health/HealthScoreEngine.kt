package com.veyra.app.data.health

import androidx.health.connect.client.records.HeartRateRecord
import java.time.Duration
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

internal object HealthScoreEngine {
    private const val calibrationDays = 14

    fun calculate(
        sleeps: List<SleepSummary>,
        hrvValues: List<Double>,
        restingHeartRates: List<Long>,
        todayHeartRate: List<HeartRateRecord.Sample>,
        minutesSinceDayStart: Long,
        maxHeartRate: Int = 190,
    ): ScoreResult {
        val latestSleep = sleeps.maxByOrNull { it.endTime }
        val latestHrv = hrvValues.firstOrNull()
        val latestRhr = restingHeartRates.firstOrNull()
        val baselineDays = minOf(sleeps.size, hrvValues.size, restingHeartRates.size)

        val sleepScore = latestSleep?.let { sleepScore(it, sleeps) }
        val recoveryScore = if (
            baselineDays >= calibrationDays && latestHrv != null && latestRhr != null && latestSleep != null
        ) {
            recoveryScore(latestHrv, latestRhr, latestSleep, hrvValues, restingHeartRates)
        } else null

        val (strain, coverage) = strainScore(todayHeartRate, minutesSinceDayStart, maxHeartRate)

        val recoveryNote = when {
            recoveryScore == null -> "Calibrating · $baselineDays/$calibrationDays valid days"
            recoveryScore >= 67 -> "Ready for a productive session"
            recoveryScore >= 34 -> "Normal load; keep some reserve"
            else -> "Prioritise recovery and light movement"
        }
        val sleepNote = latestSleep?.let {
            "${formatDuration(it.asleepMinutes)} · ${sleepBand(sleepScore ?: 0)}"
        } ?: "Waiting for last night's sleep"
        val strainNote = when {
            coverage < .25f -> "Partial day · low HR coverage"
            strain >= 14 -> "High cardiovascular load"
            strain >= 8 -> "Moderate load so far"
            else -> "Target 13–16 today"
        }

        return ScoreResult(
            recovery = recoveryScore,
            sleep = sleepScore,
            strain = strain,
            recoveryNote = recoveryNote,
            sleepNote = sleepNote,
            strainNote = strainNote,
            coverage = coverage,
            baselineDays = baselineDays,
        )
    }

    private fun recoveryScore(
        latestHrv: Double,
        latestRhr: Long,
        latestSleep: SleepSummary,
        hrvValues: List<Double>,
        rhrValues: List<Long>,
    ): Int {
        val logHrv = hrvValues.drop(1).map { ln(it.coerceAtLeast(1.0)) }
        val rhr = rhrValues.drop(1).map(Long::toDouble)
        val zHrv = robustZ(ln(latestHrv.coerceAtLeast(1.0)), logHrv)
        val zRhr = -robustZ(latestRhr.toDouble(), rhr)
        val zSleep = ((latestSleep.asleepMinutes - 480.0) / 90.0).coerceIn(-2.0, 2.0)
        val signal = 0.45 + .6 * zHrv + .3 * zRhr + .1 * zSleep
        return (100.0 / (1.0 + exp(-1.35 * signal))).toInt().coerceIn(0, 100)
    }

    private fun sleepScore(latest: SleepSummary, history: List<SleepSummary>): Int {
        val duration = 50.0 * (latest.asleepMinutes / 480.0).coerceIn(0.0, 1.0)
        val efficiency = if (latest.sessionMinutes > 0) {
            latest.asleepMinutes.toDouble() / latest.sessionMinutes
        } else 0.0
        val stageShare = if (latest.asleepMinutes > 0) {
            (latest.deepMinutes + latest.remMinutes).toDouble() / latest.asleepMinutes
        } else 0.0
        val consistency = bedtimeConsistency(latest.bedtimeMinuteOfDay, history.drop(1).take(7))
        return (duration + 20 * efficiency.coerceIn(0.0, 1.0) +
            15 * (stageShare / .40).coerceIn(0.0, 1.0) + 15 * consistency)
            .toInt().coerceIn(0, 100)
    }

    private fun bedtimeConsistency(latest: Int, history: List<SleepSummary>): Double {
        if (history.size < 3) return .7
        val distances = history.map { circularMinuteDistance(latest, it.bedtimeMinuteOfDay) }
        val medianDistance = median(distances.map(Int::toDouble))
        return (1.0 - medianDistance / 120.0).coerceIn(0.0, 1.0)
    }

    private fun strainScore(
        input: List<HeartRateRecord.Sample>,
        elapsedDayMinutes: Long,
        maxHeartRate: Int,
    ): Pair<Int, Float> {
        val samples = input.sortedBy { it.time }
        if (samples.size < 2) return 0 to 0f

        var trimp = 0.0
        var coveredMinutes = 0.0
        samples.zipWithNext().forEach { (current, next) ->
            val gap = Duration.between(current.time, next.time).toMillis() / 60_000.0
            if (gap <= 0 || gap > 10) return@forEach
            val minutes = gap.coerceAtMost(5.0)
            val percent = current.beatsPerMinute.toDouble() / maxHeartRate
            val zone = when {
                percent >= .90 -> 5
                percent >= .80 -> 4
                percent >= .70 -> 3
                percent >= .60 -> 2
                percent >= .50 -> 1
                else -> 0
            }
            trimp += zone * minutes
            coveredMinutes += minutes
        }
        val strain = (21 * (1 - exp(-trimp / 150.0))).toInt().coerceIn(0, 21)
        val coverage = if (elapsedDayMinutes > 0) {
            (coveredMinutes / elapsedDayMinutes).toFloat().coerceIn(0f, 1f)
        } else 0f
        return strain to coverage
    }

    private fun robustZ(value: Double, population: List<Double>): Double {
        if (population.size < 5) return 0.0
        val center = median(population)
        val mad = median(population.map { abs(it - center) }) * 1.4826
        return if (mad > .0001) ((value - center) / mad).coerceIn(-3.0, 3.0) else 0.0
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2 else sorted[middle]
    }

    private fun circularMinuteDistance(a: Int, b: Int): Int {
        val raw = abs(a - b)
        return min(raw, 1440 - raw)
    }

    private fun sleepBand(score: Int) = when {
        score >= 85 -> "excellent sleep"
        score >= 70 -> "good sleep"
        score >= 50 -> "fair sleep"
        else -> "short or disrupted"
    }

    private fun formatDuration(minutes: Long): String = "${minutes / 60}h ${minutes % 60}m"
}
