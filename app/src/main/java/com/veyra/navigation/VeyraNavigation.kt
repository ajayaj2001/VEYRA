package com.veyra.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Workout : Screen("workout")
    data object WorkoutActive : Screen("workout/active?routineId={routineId}") {
        fun createRoute(routineId: Long? = null) =
            if (routineId != null) "workout/active?routineId=$routineId"
            else "workout/active"
    }
    data object WorkoutHistory : Screen("workout/history")
    data object ExerciseDetail : Screen("exercise/{exerciseId}") {
        fun createRoute(exerciseId: String) = "exercise/$exerciseId"
    }
    data object ExerciseSearch : Screen("exercise/search")
    data object Nutrition : Screen("nutrition")
    data object FoodEntry : Screen("nutrition/entry?foodId={foodId}") {
        fun createRoute(foodId: Long? = null) =
            if (foodId != null) "nutrition/entry?foodId=$foodId"
            else "nutrition/entry"
    }
    data object Profile : Screen("profile")
    data object AiCoach : Screen("ai-coach")
    data object MedicalRecords : Screen("medical-records")
}

enum class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
) {
    HOME(Screen.Home, Icons.Rounded.Home, "Home"),
    WORKOUT(Screen.Workout, Icons.Rounded.FitnessCenter, "Workout"),
    NUTRITION(Screen.Nutrition, Icons.Rounded.Restaurant, "Nutrition"),
    PROFILE(Screen.Profile, Icons.Rounded.Person, "Profile"),
}
