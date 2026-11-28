package com.aura.navigation

import android.os.Bundle
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aura.BuildConfig
import com.aura.data.cloud.CloudProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
import com.aura.ui.screens.workout.RoutineDetailScreen
import com.aura.ui.screens.workout.RoutineEditorScreen
import com.aura.ui.screens.workout.WorkoutSummaryScreen
import com.aura.ui.screens.nutrition.NutritionScreen
import com.aura.ui.screens.nutrition.NutritionEntryScreen
import com.aura.ui.screens.nutrition.NutritionVM
import com.aura.ui.screens.profile.AiSettingsScreen
import com.aura.ui.screens.profile.CoachScreen
import com.aura.ui.screens.profile.MedicalScreen
import com.aura.ui.theme.AuraContentTheme

private const val ANIM = 300
private data class ProfileLookup(val value: String?)

@Composable
fun AuraNavGraph(
    nav: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    var basic by rememberSaveable { mutableStateOf(Bundle()) }
    var body by rememberSaveable { mutableStateOf(Bundle()) }
    var profileName by rememberSaveable { mutableStateOf("") }

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
            val scope = rememberCoroutineScope()
            // Firebase keeps a signed-in user signed in across launches, so they skip onboarding and
            // login. Their profile is looked up while the splash plays.
            val destination = remember {
                scope.async {
                    val user = FirebaseAuth.getInstance().currentUser ?: return@async Routes.ONBOARDING
                    val name = try {
                        withTimeoutOrNull(5_000) { ProfileLookup(CloudProfile.loadName(user.uid)) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    when {
                        // Offline or slow: still let them in, under their Google account name.
                        name == null -> profileName = user.displayName.orEmpty()
                        // Signed in but never finished setup; login sends them on to it.
                        name.value == null -> return@async Routes.LOGIN
                        else -> profileName = name.value
                    }
                    Routes.HOME
                }
            }
            SplashScreen(onFinished = {
                scope.launch {
                    nav.navigate(destination.await()) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onComplete = {
                nav.navigate(Routes.LOGIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.LOGIN) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var busy by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            LoginScreen(
                signingIn = busy,
                error = error,
                onDebugPreview = if (BuildConfig.DEBUG && !busy) {
                    {
                        FirebaseAuth.getInstance().signOut()
                        basic = Bundle()
                        body = Bundle()
                        profileName = "ajayaj7397@gmail.com"
                        nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    }
                } else null,
                onGoogleSignIn = {
                    if (!busy) {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val uid = CloudProfile.signIn(context)
                                val name = withTimeoutOrNull(15_000) {
                                    ProfileLookup(CloudProfile.loadName(uid))
                                } ?: error("Profile lookup timed out")
                                if (name.value == null) {
                                    basic = Bundle()
                                    body = Bundle()
                                    nav.navigate(Routes.SETUP_BASIC)
                                } else {
                                    profileName = name.value
                                    nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                                }
                            } catch (_: GetCredentialCancellationException) {
                                // The user dismissed the account chooser.
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                error = "Couldn’t sign in or load your profile. Try again."
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
            )
        }
        composable(Routes.SETUP_BASIC) {
            SetupBasicScreen(
                onContinue = { basic = it; nav.navigate(Routes.SETUP_BODY) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETUP_BODY) {
            SetupBodyScreen(
                onContinue = { body = it; nav.navigate(Routes.SETUP_GOALS) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETUP_GOALS) {
            val scope = rememberCoroutineScope()
            var saving by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            SetupGoalsScreen(
                saving = saving,
                error = error,
                onComplete = { goals ->
                    if (!saving) {
                        saving = true
                        error = null
                        scope.launch {
                            try {
                                val saved = withTimeoutOrNull(15_000) {
                                    CloudProfile.save(basic, body, goals)
                                    true
                                } ?: false
                                if (saved) {
                                    profileName = basic.getString("name").orEmpty()
                                    basic = Bundle()
                                    body = Bundle()
                                    nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                                } else {
                                    error = "Sync is taking longer than expected. Check your connection and retry."
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                error = "Couldn’t save your profile. Try again."
                            } finally {
                                saving = false
                            }
                        }
                    }
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
            val selectedIds by entry.savedStateHandle.getStateFlow("selectedExerciseIds", arrayListOf<String>()).collectAsState()
            val vm: com.aura.ui.screens.workout.ActiveWorkoutVM = androidx.lifecycle.viewmodel.compose.viewModel()
            AuraContentTheme {
                ActiveWorkoutScreen(nav = nav, routineId = routineId, vm = vm, selectedExerciseIds = selectedIds,
                    onExerciseHandled = { entry.savedStateHandle["selectedExerciseIds"] = arrayListOf<String>() })
            }
        }
        composable(
            "workout/routine/{routineId}",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType }),
        ) { entry ->
            val routineId = entry.arguments?.getLong("routineId") ?: return@composable
            AuraContentTheme { RoutineDetailScreen(nav, routineId) }
        }
        composable(
            "workout/routine/{routineId}/edit",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType }),
        ) { entry ->
            val routineId = entry.arguments?.getLong("routineId") ?: return@composable
            val selectedIds by entry.savedStateHandle.getStateFlow("selectedExerciseIds", arrayListOf<String>()).collectAsState()
            AuraContentTheme {
                RoutineEditorScreen(nav, routineId, selectedIds, onSelectionHandled = {
                    entry.savedStateHandle["selectedExerciseIds"] = arrayListOf<String>()
                })
            }
        }
        composable(
            "workout/summary/{workoutId}?celebrate={celebrate}",
            arguments = listOf(
                navArgument("workoutId") { type = NavType.LongType },
                navArgument("celebrate") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val workoutId = entry.arguments?.getLong("workoutId") ?: return@composable
            AuraContentTheme {
                WorkoutSummaryScreen(nav, workoutId, celebrate = entry.arguments?.getBoolean("celebrate") == true)
            }
        }
        composable(
            "exercise/search?picker={picker}",
            arguments = listOf(navArgument("picker") { type = NavType.BoolType; defaultValue = true }),
        ) { entry ->
            val picking = entry.arguments?.getBoolean("picker") == true
            val draft by entry.savedStateHandle.getStateFlow("exercisePickerDraft", arrayListOf<String>()).collectAsState()
            val parent = remember(entry) { nav.previousBackStackEntry?.savedStateHandle }
            val existing = parent?.get<ArrayList<String>>("existingExerciseIds").orEmpty()
            AuraContentTheme {
                ExerciseSearchScreen(
                    nav, picking, draft, existing,
                    onToggle = { id ->
                        val current = entry.savedStateHandle.get<ArrayList<String>>("exercisePickerDraft").orEmpty()
                        entry.savedStateHandle["exercisePickerDraft"] = ArrayList(
                            if (id in current) current - id else current + id,
                        )
                    },
                    onConfirm = {
                        parent?.set("selectedExerciseIds", ArrayList(draft))
                        nav.popBackStack()
                    },
                )
            }
        }
        composable(
            "exercise/{exerciseId}?picker={picker}",
            arguments = listOf(navArgument("picker") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            val id = entry.arguments?.getString("exerciseId") ?: return@composable
            val picking = entry.arguments?.getBoolean("picker") == true
            val picker = remember(entry) { nav.previousBackStackEntry?.savedStateHandle }
            val draft = picker?.get<ArrayList<String>>("exercisePickerDraft").orEmpty()
            val toggle: (() -> Unit)? = if (picking && picker != null) {
                {
                    picker["exercisePickerDraft"] = ArrayList(if (id in draft) draft - id else draft + id)
                    nav.popBackStack()
                }
            } else null
            AuraContentTheme {
                ExerciseDetailScreen(nav, exerciseId = id, onAdd = toggle, isSelected = id in draft)
            }
        }
        composable(Routes.NUTRITION) {
            AuraContentTheme { NutritionScreen(nav = nav) }
        }
        composable("nutrition/entry") { entry ->
            val parent = remember(entry) { nav.getBackStackEntry(Routes.NUTRITION) }
            val vm: NutritionVM = androidx.lifecycle.viewmodel.compose.viewModel(parent)
            AuraContentTheme { NutritionEntryScreen(vm, onClose = { nav.popBackStack() }) }
        }
        composable(Routes.PROFILE) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            AuraContentTheme {
                ProfileScreen(nav = nav, profileName = profileName, onSignOut = {
                    scope.launch {
                        try {
                            CloudProfile.signOut(context)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            // Firebase has signed out even if the credential provider fails to clear.
                        }
                        profileName = ""
                        nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                })
            }
        }
        composable(Routes.COACH) {
            AuraContentTheme { CoachScreen(nav = nav) }
        }
        composable(Routes.MEDICAL) {
            AuraContentTheme { MedicalScreen(nav = nav) }
        }
        composable(Routes.AI_SETTINGS) {
            AuraContentTheme { AiSettingsScreen(nav = nav) }
        }
    }
}
