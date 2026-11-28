package com.aura

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aura.data.local.dao.WorkoutDao
import com.aura.navigation.Routes
import com.aura.navigation.Tab
import com.aura.navigation.AuraNavGraph
import com.aura.ui.theme.AuraCaption
import com.aura.ui.theme.V
import com.aura.ui.theme.AuraTheme
import com.aura.workout.WorkoutNotification

class MainActivity : ComponentActivity() {
    private var openWorkout by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openWorkout = savedInstanceState?.getBoolean("openWorkout")
            ?: (intent.action == WorkoutNotification.OPEN_WORKOUT)
        enableEdgeToEdge()
        setContent {
            AuraTheme {
                AuraRoot(openWorkout, (application as AuraApp).db.workoutDao()) { openWorkout = false }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == WorkoutNotification.OPEN_WORKOUT) openWorkout = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("openWorkout", openWorkout)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        (application as AuraApp).workoutNotification.refresh()
    }
}

@Composable
private fun AuraRoot(
    openWorkout: Boolean,
    workoutDao: WorkoutDao,
    onWorkoutOpened: () -> Unit,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    LaunchedEffect(openWorkout, route) {
        if (!openWorkout || route == null) return@LaunchedEffect
        // Wait for the usual sign-in and profile routing before showing workout data.
        if (runCatching { nav.getBackStackEntry(Routes.HOME) }.isFailure) return@LaunchedEffect
        val workout = workoutDao.getActiveWorkout()
        val activeRoute = "workout/active?routineId={routineId}"
        if (workout != null && route != activeRoute && !nav.popBackStack(activeRoute, inclusive = false)) {
            nav.navigate("workout/active") { launchSingleTop = true }
        }
        onWorkoutOpened()
    }

    val tabRoutes = Tab.entries.map { it.route }
    val showNav = route in tabRoutes

    Scaffold(
        containerColor = V.Canvas,
        bottomBar = {
            AnimatedVisibility(
                visible = showNav,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                BottomBar(
                    currentRoute = route,
                    onTab = { tab ->
                        if (route != tab.route) {
                            nav.navigate(tab.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        AuraNavGraph(
            nav = nav,
            startDestination = Routes.SPLASH,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
private fun BottomBar(
    currentRoute: String?,
    onTab: (Tab) -> Unit,
) {
    val navPadding = WindowInsets.navigationBars.asPaddingValues()

    Row(
        Modifier
            .fillMaxWidth()
            .background(V.Canvas)
            .padding(bottom = navPadding.calculateBottomPadding())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(V.Surface)
                .border(1.dp, V.Divider, RoundedCornerShape(20.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                val selected = currentRoute == tab.route

                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .selectable(selected, role = Role.Tab, onClick = { onTab(tab) })
                        .heightIn(min = 64.dp)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Icon(tab.icon, contentDescription = null, tint = if (selected) V.Teal else V.Gray,
                                modifier = Modifier.size(24.dp))
                        }
                        Text(
                            tab.label,
                            style = AuraCaption,
                            color = if (selected) V.White else V.Gray,
                        )
                        Box(
                            Modifier.width(20.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
                                .background(if (selected) V.Teal else androidx.compose.ui.graphics.Color.Transparent),
                        )
                    }
                }
            }
        }

    }
}
