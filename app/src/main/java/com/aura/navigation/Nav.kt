package com.aura.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val SETUP_BASIC = "setup/basic"
    const val SETUP_BODY = "setup/body"
    const val SETUP_GOALS = "setup/goals"
    const val HOME = "home"
    const val WORKOUT = "workout"
    const val NUTRITION = "nutrition"
    const val PROFILE = "profile"
    const val COACH = "coach"
    const val MEDICAL = "medical"
    const val SETTINGS = "settings"
    const val NUTRITION_GOALS = "settings/nutrition"
    const val AI_SETTINGS = "settings/ai"
    const val HEALTH_CONNECT = "health_connect"
}

enum class Tab(val route: String, val icon: ImageVector, val label: String) {
    HOME(Routes.HOME, Icons.Rounded.Home, "Home"),
    WORKOUT(Routes.WORKOUT, Icons.Rounded.FitnessCenter, "Workout"),
    NUTRITION(Routes.NUTRITION, Icons.Rounded.Restaurant, "Nutrition"),
    PROFILE(Routes.PROFILE, Icons.Rounded.Person, "Profile"),
}
