package com.veyra.navigation

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
import com.veyra.ui.screens.SplashScreen
import com.veyra.ui.screens.auth.LoginScreen
import com.veyra.ui.screens.auth.SetupBasicScreen
import com.veyra.ui.screens.auth.SetupBodyScreen
import com.veyra.ui.screens.auth.SetupGoalsScreen
import com.veyra.ui.screens.home.HomeScreen
import com.veyra.ui.screens.onboarding.OnboardingScreen
import com.veyra.ui.screens.profile.ProfileScreen
import com.veyra.ui.screens.workout.ActiveWorkoutScreen
import com.veyra.ui.screens.workout.ExerciseSearchScreen
import com.veyra.ui.screens.workout.WorkoutScreen
import com.veyra.data.model.ExerciseLib

private const val ANIM = 300

@Composable
fun VeyraNavGraph(
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
                    nav.navigate(Routes.SETUP_BASIC) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onExistingUser = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
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
                    nav.navigate(Routes.HOME) { popUpTo(Routes.SETUP_BASIC) { inclusive = true } }
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(nav = nav)
        }
        composable(Routes.WORKOUT) {
            WorkoutScreen(nav = nav)
        }
        composable(
            route = "workout/active?routineId={routineId}",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            val routineId = entry.arguments?.getLong("routineId")?.takeIf { it != -1L }
            // Listen for exercise added from search
            val selectedId = entry.savedStateHandle?.get<String>("selectedExerciseId")
            val vm: com.veyra.ui.screens.workout.ActiveWorkoutVM = androidx.lifecycle.viewmodel.compose.viewModel()
            LaunchedEffect(selectedId) {
                selectedId?.let { id ->
                    ExerciseLib.getById(id)?.let { vm.addExercise(it) }
                    entry.savedStateHandle?.remove<String>("selectedExerciseId")
                }
            }
            ActiveWorkoutScreen(nav = nav, routineId = routineId, vm = vm)
        }
        composable("exercise/search") {
            ExerciseSearchScreen(nav = nav)
        }
        composable(Routes.NUTRITION) {
            // Placeholder
            HomeScreen(nav = nav)
        }
        composable(Routes.PROFILE) {
            ProfileScreen(nav = nav)
        }
    }
}
