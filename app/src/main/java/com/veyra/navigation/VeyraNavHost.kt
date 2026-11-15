package com.veyra.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.veyra.ui.screens.home.HomeScreen
import com.veyra.ui.screens.nutrition.FoodEntryScreen
import com.veyra.ui.screens.nutrition.NutritionScreen
import com.veyra.ui.screens.profile.AiCoachScreen
import com.veyra.ui.screens.profile.MedicalRecordsScreen
import com.veyra.ui.screens.profile.ProfileScreen
import com.veyra.ui.screens.workout.ActiveWorkoutScreen
import com.veyra.ui.screens.workout.ExerciseDetailScreen
import com.veyra.ui.screens.workout.ExerciseSearchScreen
import com.veyra.ui.screens.workout.WorkoutHistoryScreen
import com.veyra.ui.screens.workout.WorkoutScreen

private const val NAV_ANIM_DURATION = 350

@Composable
fun VeyraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(NAV_ANIM_DURATION)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(NAV_ANIM_DURATION),
            )
        },
        exitTransition = {
            fadeOut(tween(NAV_ANIM_DURATION)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(NAV_ANIM_DURATION),
            )
        },
        popEnterTransition = {
            fadeIn(tween(NAV_ANIM_DURATION)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(NAV_ANIM_DURATION),
            )
        },
        popExitTransition = {
            fadeOut(tween(NAV_ANIM_DURATION)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(NAV_ANIM_DURATION),
            )
        },
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.Workout.route) {
            WorkoutScreen(navController = navController)
        }

        composable(
            route = "workout/active?routineId={routineId}",
            arguments = listOf(
                navArgument("routineId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId")
                ?.takeIf { it != -1L }
            ActiveWorkoutScreen(
                navController = navController,
                routineId = routineId,
            )
        }

        composable(Screen.WorkoutHistory.route) {
            WorkoutHistoryScreen(navController = navController)
        }

        composable(
            route = "exercise/{exerciseId}",
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: return@composable
            ExerciseDetailScreen(
                navController = navController,
                exerciseId = exerciseId,
            )
        }

        composable(Screen.ExerciseSearch.route) {
            ExerciseSearchScreen(navController = navController)
        }

        composable(Screen.Nutrition.route) {
            NutritionScreen(navController = navController)
        }

        composable(
            route = "nutrition/entry?foodId={foodId}",
            arguments = listOf(
                navArgument("foodId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getLong("foodId")
                ?.takeIf { it != -1L }
            FoodEntryScreen(
                navController = navController,
                foodId = foodId,
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        composable(Screen.AiCoach.route) {
            AiCoachScreen(navController = navController)
        }

        composable(Screen.MedicalRecords.route) {
            MedicalRecordsScreen(navController = navController)
        }
    }
}
