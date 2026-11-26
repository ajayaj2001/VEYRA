package com.aura.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aura.ui.screens.SplashScreen
import com.aura.ui.screens.auth.LoginScreen
import com.aura.ui.screens.auth.SetupBasicScreen
import com.aura.ui.screens.auth.SetupBodyScreen
import com.aura.ui.screens.auth.SetupGoalsScreen
import com.aura.ui.screens.home.HomeScreen
import com.aura.ui.screens.onboarding.OnboardingScreen
import com.aura.ui.screens.profile.ProfileScreen
import com.aura.ui.screens.workout.ActiveWorkoutScreen
import com.aura.ui.screens.workout.ExerciseDetailScreen
import com.aura.ui.screens.workout.ExerciseSearchScreen
import com.aura.ui.screens.workout.WorkoutScreen
import com.aura.ui.screens.nutrition.NutritionScreen
import com.aura.ui.screens.profile.CoachScreen
import com.aura.ui.screens.profile.MedicalScreen
import com.aura.data.model.ExerciseLib
import com.aura.ui.theme.AuraContentTheme

private const val ANIM = 300

@Composable
fun AuraNavGraph(
    nav: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = nav,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(ANIM)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM)
            )
        },
        exitTransition = { fadeOut(tween(ANIM)) },
        popEnterTransition = {
            fadeIn(tween(ANIM)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM)
            )
        },
        popExitTransition = { fadeOut(tween(ANIM)) },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.SPLASH) { inclusive = true } }
            })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onComplete = {
                nav.navigate(Routes.LOGIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onGoogleSignIn = {
                    nav.navigate(Routes.SETUP_BASIC)
                },
            )
        }
        composable(Routes.SETUP_BASIC) {
            SetupBasicScreen(
                onContinue = { nav.navigate(Routes.SETUP_BODY) },
                onSkip = { nav.navigate(Routes.SETUP_BODY) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETUP_BODY) {
            SetupBodyScreen(
                onContinue = { nav.navigate(Routes.SETUP_GOALS) },
                onSkip = { nav.navigate(Routes.SETUP_GOALS) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETUP_GOALS) {
            SetupGoalsScreen(
                onComplete = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.HOME) {
            AuraContentTheme { HomeScreen(nav = nav) }
        }
        composable(Routes.WORKOUT) {
            AuraContentTheme { WorkoutScreen(nav = nav) }
        }
        composable(
            route = "workout/active?routineId={routineId}",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            val routineId = entry.arguments?.getLong("routineId")?.takeIf { it != -1L }
            // Listen for exercise added from search
            val selectedId = entry.savedStateHandle?.get<String>("selectedExerciseId")
            val vm: com.aura.ui.screens.workout.ActiveWorkoutVM = androidx.lifecycle.viewmodel.compose.viewModel()
            LaunchedEffect(selectedId) {
                selectedId?.let { id ->
                    ExerciseLib.getById(id)?.let { vm.addExercise(it) }
                    entry.savedStateHandle?.remove<String>("selectedExerciseId")
                }
            }
            AuraContentTheme { ActiveWorkoutScreen(nav = nav, routineId = routineId, vm = vm) }
        }
        composable("exercise/search") {
            AuraContentTheme { ExerciseSearchScreen(nav = nav) }
        }
        composable("exercise/{exerciseId}") { entry ->
            val id = entry.arguments?.getString("exerciseId") ?: return@composable
            AuraContentTheme { ExerciseDetailScreen(nav = nav, exerciseId = id) }
        }
        composable(Routes.NUTRITION) {
            AuraContentTheme { NutritionScreen(nav = nav) }
        }
        composable(Routes.PROFILE) {
            AuraContentTheme { ProfileScreen(nav = nav) }
        }
        composable(Routes.COACH) {
            AuraContentTheme { CoachScreen(nav = nav) }
        }
        composable(Routes.MEDICAL) {
            AuraContentTheme { MedicalScreen(nav = nav) }
        }
    }
}
