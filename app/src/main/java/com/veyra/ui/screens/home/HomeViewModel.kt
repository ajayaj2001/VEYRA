package com.veyra.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veyra.VeyraApp
import com.veyra.health.HealthData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val healthData: HealthData = HealthData(),
    val recoveryScore: Int = 0,
    val sleepScore: Int = 0,
    val strainScore: Int = 0,
    val recoveryExplanation: String = "",
    val sleepExplanation: String = "",
    val strainExplanation: String = "",
    val recoveryTip: String = "",
    val sleepTip: String = "",
    val strainTip: String = "",
    val isLoading: Boolean = true,
    val isHealthConnectAvailable: Boolean = false,
    val hasPermissions: Boolean = false,
    val greeting: String = "",
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VeyraApp
    private val healthManager = app.healthConnectManager

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                isHealthConnectAvailable = healthManager.isAvailable(),
                greeting = getGreeting(),
            )

            if (healthManager.isAvailable()) {
                val hasPerms = healthManager.hasAllPermissions()
                _state.value = _state.value.copy(hasPermissions = hasPerms)

                if (hasPerms) {
                    val data = healthManager.readTodayData()
                    val recovery = calculateRecoveryScore(data)
                    val sleep = calculateSleepScore(data)
                    val strain = calculateStrainScore(data)

                    _state.value = _state.value.copy(
                        healthData = data,
                        recoveryScore = recovery,
                        sleepScore = sleep,
                        strainScore = strain,
                        recoveryExplanation = getRecoveryExplanation(recovery, data),
                        sleepExplanation = getSleepExplanation(sleep, data),
                        strainExplanation = getStrainExplanation(strain, data),
                        recoveryTip = getRecoveryTip(recovery),
                        sleepTip = getSleepTip(sleep),
                        strainTip = getStrainTip(strain),
                        isLoading = false,
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false)
                }
            } else {
                // Demo data when Health Connect isn't available
                val demoData = HealthData(
                    steps = 8452,
                    calories = 1847.0,
                    activeCalories = 523.0,
                    restingHeartRate = 62,
                    heartRateAvg = 74,
                    heartRateMin = 55,
                    heartRateMax = 142,
                    hrvRmssd = 48.5,
                    sleepDurationMinutes = 432,
                )
                val recovery = calculateRecoveryScore(demoData)
                val sleep = calculateSleepScore(demoData)
                val strain = calculateStrainScore(demoData)

                _state.value = _state.value.copy(
                    healthData = demoData,
                    recoveryScore = recovery,
                    sleepScore = sleep,
                    strainScore = strain,
                    recoveryExplanation = getRecoveryExplanation(recovery, demoData),
                    sleepExplanation = getSleepExplanation(sleep, demoData),
                    strainExplanation = getStrainExplanation(strain, demoData),
                    recoveryTip = getRecoveryTip(recovery),
                    sleepTip = getSleepTip(sleep),
                    strainTip = getStrainTip(strain),
                    isLoading = false,
                )
            }
        }
    }

    private fun getGreeting(): String {
        val hour = java.time.LocalTime.now().hour
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun calculateRecoveryScore(data: HealthData): Int {
        var score = 50
        // HRV contribution (higher = better recovery)
        if (data.hrvRmssd > 0) {
            score += when {
                data.hrvRmssd >= 60 -> 25
                data.hrvRmssd >= 45 -> 15
                data.hrvRmssd >= 30 -> 5
                else -> -10
            }
        }
        // Resting HR contribution (lower = better)
        if (data.restingHeartRate > 0) {
            score += when {
                data.restingHeartRate <= 55 -> 20
                data.restingHeartRate <= 65 -> 10
                data.restingHeartRate <= 75 -> 0
                else -> -15
            }
        }
        // Sleep contribution
        val sleepHours = data.sleepDurationMinutes / 60.0
        score += when {
            sleepHours >= 8 -> 10
            sleepHours >= 7 -> 5
            sleepHours >= 6 -> 0
            else -> -15
        }
        return score.coerceIn(0, 100)
    }

    private fun calculateSleepScore(data: HealthData): Int {
        if (data.sleepDurationMinutes == 0L) return 0
        val hours = data.sleepDurationMinutes / 60.0
        var score = when {
            hours >= 8.5 -> 90
            hours >= 8 -> 85
            hours >= 7.5 -> 75
            hours >= 7 -> 65
            hours >= 6.5 -> 50
            hours >= 6 -> 40
            else -> 25
        }
        // Bonus for good HRV during sleep
        if (data.hrvRmssd >= 50) score += 5
        if (data.restingHeartRate in 1..60) score += 5
        return score.coerceIn(0, 100)
    }

    private fun calculateStrainScore(data: HealthData): Int {
        var score = 0
        // Steps contribution
        score += when {
            data.steps >= 12000 -> 30
            data.steps >= 10000 -> 25
            data.steps >= 7500 -> 20
            data.steps >= 5000 -> 15
            data.steps >= 2500 -> 10
            else -> 5
        }
        // Active calories
        score += when {
            data.activeCalories >= 600 -> 35
            data.activeCalories >= 400 -> 25
            data.activeCalories >= 300 -> 20
            data.activeCalories >= 200 -> 15
            data.activeCalories >= 100 -> 10
            else -> 5
        }
        // Heart rate range
        if (data.heartRateMax > 0) {
            score += when {
                data.heartRateMax >= 160 -> 25
                data.heartRateMax >= 140 -> 20
                data.heartRateMax >= 120 -> 15
                data.heartRateMax >= 100 -> 10
                else -> 5
            }
        } else {
            score += 10
        }
        return score.coerceIn(0, 100)
    }

    private fun getRecoveryExplanation(score: Int, data: HealthData): String {
        val hrv = if (data.hrvRmssd > 0) "HRV at ${data.hrvRmssd.toInt()}ms" else "HRV data unavailable"
        val rhr = if (data.restingHeartRate > 0) "resting HR of ${data.restingHeartRate} bpm" else "resting HR unavailable"
        return when {
            score >= 80 -> "Excellent recovery. Your $hrv and $rhr indicate your body is well-rested and ready for intense training."
            score >= 60 -> "Good recovery. Your $hrv and $rhr suggest you're recovering well. A moderate to hard workout is appropriate."
            score >= 40 -> "Moderate recovery. Your $hrv and $rhr indicate your body is still recovering. Consider a lighter workout today."
            else -> "Low recovery. Your $hrv and $rhr suggest your body needs rest. Prioritize sleep and light movement."
        }
    }

    private fun getSleepExplanation(score: Int, data: HealthData): String {
        val hours = data.sleepDurationMinutes / 60
        val mins = data.sleepDurationMinutes % 60
        val duration = "${hours}h ${mins}m"
        return when {
            score >= 80 -> "Great sleep! You got $duration of quality rest, supporting optimal recovery and performance."
            score >= 60 -> "Decent sleep with $duration. Close to optimal, but a bit more consistency would boost recovery."
            score >= 40 -> "Fair sleep at $duration. Try to get closer to 7-8 hours for better recovery."
            score > 0 -> "Poor sleep with only $duration. This will impact your recovery and performance significantly."
            else -> "No sleep data recorded. Connect a wearable to track your sleep patterns."
        }
    }

    private fun getStrainExplanation(score: Int, data: HealthData): String {
        val steps = "%,d".format(data.steps)
        return when {
            score >= 80 -> "High strain today! $steps steps and ${data.activeCalories.toInt()} active calories burned. Your body is working hard."
            score >= 60 -> "Moderate-high strain. $steps steps and ${data.activeCalories.toInt()} active calories. Good activity level."
            score >= 40 -> "Moderate strain with $steps steps. Consider adding more movement or a workout."
            else -> "Low strain so far. $steps steps recorded. A workout or walk would increase your daily strain."
        }
    }

    private fun getRecoveryTip(score: Int): String = when {
        score >= 80 -> "Go all out today — your body is primed for peak performance."
        score >= 60 -> "Good day for strength training or moderate-intensity cardio."
        score >= 40 -> "Stick to light exercise, yoga, or active recovery today."
        else -> "Rest day recommended. Focus on hydration, nutrition, and sleep."
    }

    private fun getSleepTip(score: Int): String = when {
        score >= 80 -> "Keep up the great sleep habits. Consistency is key."
        score >= 60 -> "Try to go to bed 30 minutes earlier tonight."
        score >= 40 -> "Limit screen time before bed and keep your room cool and dark."
        score > 0 -> "Prioritize 7-8 hours tonight. Avoid caffeine after 2pm."
        else -> "Sync a wearable to track sleep and unlock personalized insights."
    }

    private fun getStrainTip(score: Int): String = when {
        score >= 80 -> "Great effort! Make sure to fuel and hydrate for recovery."
        score >= 60 -> "Solid activity. A short walk or stretch session would round out the day."
        score >= 40 -> "Add a 30-minute workout to boost your strain and fitness."
        else -> "Try to hit 7,500 steps and get at least 200 active calories today."
    }
}
