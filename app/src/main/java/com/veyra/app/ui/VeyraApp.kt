package com.veyra.app.ui

import android.content.Intent
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri
import com.veyra.app.data.health.HealthDashboardState
import com.veyra.app.data.health.HealthSnapshot
import com.veyra.app.ui.theme.Acid
import com.veyra.app.ui.theme.Bone
import com.veyra.app.ui.theme.Hairline
import com.veyra.app.ui.theme.Ink
import com.veyra.app.ui.theme.Muted
import com.veyra.app.ui.theme.Recovery
import com.veyra.app.ui.theme.Sleep
import com.veyra.app.ui.theme.Strain
import com.veyra.app.ui.theme.Surface
import com.veyra.app.ui.theme.SurfaceHigh
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

private sealed interface VeyraRoute {
    data object Main : VeyraRoute
    data class ExerciseDetail(val exercise: Exercise) : VeyraRoute
    data object ActiveWorkout : VeyraRoute
}

@Composable
fun VeyraApp(dashboardViewModel: DashboardViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(VeyraTab.Today) }
    var route by remember { mutableStateOf<VeyraRoute>(VeyraRoute.Main) }
    val healthState by dashboardViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionsLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { dashboardViewModel.onPermissionsResult() }
    val openHealthConnect = {
        val webUri = "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata".toUri()
        val marketUri = "market://details?id=com.google.android.apps.healthdata".toUri()
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, marketUri)) }
            .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, webUri)) }
        Unit
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        AnimatedContent(
            targetState = route,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "route"
        ) { destination ->
            when (destination) {
                VeyraRoute.Main -> MainShell(
                    selected = tab,
                    onSelect = { tab = it },
                    healthState = healthState,
                    onConnectHealth = { permissionsLauncher.launch(dashboardViewModel.permissions) },
                    onRefreshHealth = dashboardViewModel::refresh,
                    onInstallHealthConnect = openHealthConnect,
                    onExercise = { route = VeyraRoute.ExerciseDetail(it) },
                    onStartWorkout = { route = VeyraRoute.ActiveWorkout }
                )
                is VeyraRoute.ExerciseDetail -> ExerciseDetailScreen(
                    exercise = destination.exercise,
                    onBack = { route = VeyraRoute.Main },
                    onStart = { route = VeyraRoute.ActiveWorkout }
                )
                VeyraRoute.ActiveWorkout -> ActiveWorkoutScreen(onBack = { route = VeyraRoute.Main })
            }
        }
    }
}

@Composable
private fun MainShell(
    selected: VeyraTab,
    onSelect: (VeyraTab) -> Unit,
    healthState: HealthDashboardState,
    onConnectHealth: () -> Unit,
    onRefreshHealth: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    onExercise: (Exercise) -> Unit,
    onStartWorkout: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        Box(Modifier.weight(1f)) {
            when (selected) {
                VeyraTab.Today -> TodayScreen(
                    healthState,
                    onConnectHealth,
                    onRefreshHealth,
                    onInstallHealthConnect,
                )
                VeyraTab.Train -> TrainScreen(onExercise, onStartWorkout)
                VeyraTab.Fuel -> ComingSoonScreen("Fuel", "Photo, describe or scan a meal.", Acid)
                VeyraTab.Coach -> ComingSoonScreen("Coach", "Your data, explained with evidence.", Recovery)
                VeyraTab.You -> ComingSoonScreen("You", "Trends, records and private settings.", Sleep)
            }
        }
        BottomBar(selected, onSelect)
    }
}

@Composable
private fun TodayScreen(
    healthState: HealthDashboardState,
    onConnectHealth: () -> Unit,
    onRefreshHealth: () -> Unit,
    onInstallHealthConnect: () -> Unit,
) {
    val snapshot = (healthState as? HealthDashboardState.Ready)?.snapshot
    val scores = snapshot?.toScores()
    val dateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMM")).uppercase()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Label(dateLabel)
                    Text("Good morning, Ajay", color = Bone, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                }
                SyncPill(snapshot?.syncedAt, healthState is HealthDashboardState.Loading, onRefreshHealth)
            }
        }
        if (snapshot == null || scores == null) {
            item {
                HealthConnectCard(
                    state = healthState,
                    onConnect = onConnectHealth,
                    onRefresh = onRefreshHealth,
                    onInstall = onInstallHealthConnect,
                )
            }
        } else {
            item { RecoveryHero(scores[0], snapshot) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactScore(scores[1], Modifier.weight(1f), Icons.Outlined.Bedtime)
                    CompactScore(scores[2], Modifier.weight(1f), Icons.Outlined.Bolt)
                }
            }
            item { CoachBrief(snapshot) }
            item { SectionHeader("TODAY'S SIGNALS", "Live") }
            item { SignalStrip(snapshot) }
        }
        item { LastWorkoutCard() }
    }
}

@Composable
private fun RecoveryHero(score: Score, snapshot: HealthSnapshot) {
    PremiumCard(Modifier.fillMaxWidth().height(300.dp), border = score.color.copy(alpha = .35f)) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxHeight().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Label("RECOVERY", score.color)
                    Text("Strong", color = score.color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(score.value?.toString() ?: "—", style = androidx.compose.material3.MaterialTheme.typography.displayLarge, color = Bone)
                    Text(score.note, color = Bone, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        listOfNotNull(
                            snapshot.hrvMs?.let { "HRV ${it.toInt()} ms" },
                            snapshot.restingHeartRate?.let { "RHR $it bpm" },
                            snapshot.sleepMinutes?.let { "Sleep ${it / 60}h ${it % 60}m" },
                        ).joinToString(" · ").ifEmpty { "Waiting for nightly signals" },
                        color = Muted,
                        fontSize = 13.sp,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(score.color))
                    Spacer(Modifier.width(8.dp))
                    Text("TRAINING TARGET  13–16", color = Bone, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            ScoreArc((score.value ?: 0) / 100f, score.color, Modifier.align(Alignment.CenterEnd).size(175.dp).graphicsLayer { translationX = 36.dp.toPx() })
        }
    }
}

@Composable
private fun ScoreArc(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawArc(Hairline, 110f, 320f, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
        drawArc(color, 110f, 320f * progress, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun CompactScore(score: Score, modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PremiumCard(modifier.height(168.dp)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Label(score.label.uppercase(), score.color)
                Icon(icon, null, tint = score.color, modifier = Modifier.size(18.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(score.value?.toString() ?: "—", style = androidx.compose.material3.MaterialTheme.typography.displayMedium, color = Bone)
                Text(score.unit, color = Muted, modifier = Modifier.padding(bottom = 8.dp, start = 2.dp))
            }
            Text(score.note, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun CoachBrief(snapshot: HealthSnapshot) {
    val message = when {
        snapshot.recoveryScore == null -> "I'm learning your baseline. Wear Fitbit Air overnight and keep Google Health synced."
        snapshot.recoveryScore >= 67 -> "Your recovery supports the planned session. Keep the final working set around RPE 8."
        snapshot.recoveryScore >= 34 -> "Train as planned, but keep two reps in reserve and avoid adding extra volume."
        else -> "Recovery is suppressed today. Choose walking or mobility and move the hard session forward."
    }
    PremiumCard(Modifier.fillMaxWidth(), border = Recovery.copy(alpha = .25f)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Recovery.copy(.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Recovery, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Label("COACH", Recovery)
                Spacer(Modifier.height(4.dp))
                Text(message, color = Bone, fontSize = 14.sp, lineHeight = 20.sp)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun SignalStrip(snapshot: HealthSnapshot) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
            Metric("%,d".format(snapshot.steps), "STEPS", "Today", Modifier.weight(1f))
            DividerLine()
            Metric("%,d".format(snapshot.activeCalories), "ACTIVE KCAL", "Today", Modifier.weight(1f))
            DividerLine()
            Metric(
                snapshot.restingHeartRate?.toString() ?: "—",
                "RESTING HR",
                "${(snapshot.heartRateCoverage * 100).toInt()}% HR cover",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HealthConnectCard(
    state: HealthDashboardState,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onInstall: () -> Unit,
) {
    val content = when (state) {
        HealthDashboardState.Loading -> Triple(
            "SYNCING HEALTH DATA",
            "Reading your latest Fitbit signals from Health Connect.",
            null,
        )
        HealthDashboardState.NeedsPermissions -> Triple(
            "CONNECT YOUR HEALTH DATA",
            "Allow sleep, HRV, resting heart rate, heart rate, steps and active calories. Veyra reads them on this phone.",
            "CONNECT HEALTH",
        )
        HealthDashboardState.ProviderUpdateRequired -> Triple(
            "HEALTH CONNECT REQUIRED",
            "Install or update Health Connect to securely share Google Health data with Veyra.",
            "OPEN PLAY STORE",
        )
        HealthDashboardState.Unavailable -> Triple(
            "HEALTH CONNECT UNAVAILABLE",
            "Veyra requires Android 9 or newer and a supported personal phone profile.",
            null,
        )
        is HealthDashboardState.Error -> Triple(
            "SYNC NEEDS ATTENTION",
            state.message,
            "TRY AGAIN",
        )
        is HealthDashboardState.Ready -> return
    }
    val action = when (state) {
        HealthDashboardState.NeedsPermissions -> onConnect
        HealthDashboardState.ProviderUpdateRequired -> onInstall
        is HealthDashboardState.Error -> onRefresh
        else -> null
    }

    PremiumCard(Modifier.fillMaxWidth(), border = Sleep.copy(.30f)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Sleep.copy(.12f)), contentAlignment = Alignment.Center) {
                    if (state is HealthDashboardState.Loading) {
                        CircularProgressIndicator(Modifier.size(19.dp), color = Sleep, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Sync, null, tint = Sleep, modifier = Modifier.size(19.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Label(content.first, Sleep)
            }
            Text(content.second, color = Bone, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 14.dp, bottom = if (action != null) 16.dp else 0.dp))
            if (action != null && content.third != null) {
                PopButton(content.third!!, Icons.Outlined.Sync, Sleep, action)
            }
        }
    }
}

@Composable
private fun RowScope.DividerLine() = Box(Modifier.width(1.dp).height(45.dp).background(Hairline))

@Composable
private fun Metric(value: String, label: String, delta: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Bone, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
        Text(delta, color = Recovery, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun LastWorkoutCard() {
    PremiumCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(Strain.copy(.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.FitnessCenter, null, tint = Strain)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Label("LAST WORKOUT", Strain)
                Text("Upper strength", color = Bone, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Yesterday · 56 min · 7,840 kg", color = Muted, fontSize = 12.sp)
            }
            Text("2 PR", color = Acid, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TrainScreen(onExercise: (Exercise) -> Unit, onStartWorkout: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 18.dp, 20.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Label("TRAIN")
            Text("Built to progress.", color = Bone, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
            Text("Previous performance, precise rest and form guidance—without the noise.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
        }
        item {
            PopButton("START PUSH DAY", Icons.Outlined.PlayArrow, Acid, onStartWorkout)
        }
        item { SectionHeader("YOUR ROUTINES", "Edit") }
        item { RoutineCard("Push · Strength", "6 exercises · 18 sets", "Last: yesterday", onStartWorkout) }
        item { RoutineCard("Pull · Hypertrophy", "7 exercises · 21 sets", "Last: 4 days ago", onStartWorkout) }
        item { SectionHeader("EXERCISE LIBRARY", "View all") }
        items(exercises) { exercise -> ExerciseCard(exercise) { onExercise(exercise) } }
    }
}

@Composable
private fun RoutineCard(name: String, detail: String, last: String, onClick: () -> Unit) {
    PremiumCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, color = Bone, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(last, color = Muted, fontSize = 11.sp)
                Icon(Icons.Outlined.ChevronRight, null, tint = Bone, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, onClick: () -> Unit) {
    PremiumCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)).background(SurfaceHigh)) {
                ExerciseMotionDemo(exercise, Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.BottomEnd).padding(7.dp).size(22.dp).clip(CircleShape).background(Ink.copy(.75f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PlayArrow, null, tint = Bone, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, color = Bone, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(exercise.equipment, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                Text(exercise.primaryMuscles.joinToString(), color = exercise.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 7.dp))
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun ExerciseDetailScreen(exercise: Exercise, onBack: () -> Unit, onStart: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(Ink),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 30.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { TopBack("EXERCISE", onBack) }
        item {
            Text(exercise.name, color = Bone, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
            Text(exercise.equipment, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp))
        }
        item {
            PremiumCard(Modifier.fillMaxWidth().height(330.dp), border = exercise.accent.copy(.32f)) {
                Box(Modifier.fillMaxSize().background(SurfaceHigh)) {
                    ExerciseMotionDemo(exercise, Modifier.fillMaxSize().padding(24.dp))
                    Row(Modifier.align(Alignment.TopStart).padding(15.dp).clip(CircleShape).background(Ink.copy(.75f)).padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(exercise.accent))
                        Spacer(Modifier.width(7.dp))
                        Text("FORM DEMO", color = Bone, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
        item {
            PremiumCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Label("COACHING CUE", exercise.accent)
                    Spacer(Modifier.height(7.dp))
                    Text(exercise.cue, color = Bone, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }
        item {
            SectionHeader("MUSCLES WORKED", "Front · back")
            PremiumCard(Modifier.fillMaxWidth().height(280.dp)) {
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MuscleMap(exercise, false, Modifier.weight(1f).fillMaxHeight())
                    MuscleMap(exercise, true, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MuscleLegend("PRIMARY", exercise.primaryMuscles.joinToString(), exercise.accent, Modifier.weight(1f))
                MuscleLegend("SECONDARY", exercise.secondaryMuscles.joinToString(), exercise.accent.copy(.45f), Modifier.weight(1f))
            }
        }
        item { PopButton("ADD TO WORKOUT", Icons.Outlined.Add, exercise.accent, onStart) }
    }
}

@Composable
private fun ActiveWorkoutScreen(onBack: () -> Unit) {
    val completed = remember { mutableStateListOf<String>() }
    var restSeconds by rememberSaveable { mutableIntStateOf(0) }
    var running by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(running, restSeconds) {
        if (running && restSeconds > 0) {
            delay(1000)
            restSeconds -= 1
        } else if (restSeconds == 0) running = false
    }

    Column(
        Modifier.fillMaxSize().background(Ink)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Surface).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Bone, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Label("LIVE · 32:18", Strain)
                Text("Push · Strength", color = Bone, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("FINISH", color = Acid, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 8.dp, 20.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(sampleWorkout) { workoutExercise ->
                WorkoutExerciseCard(workoutExercise, completed) { key ->
                    if (completed.contains(key)) completed.remove(key) else {
                        completed.add(key)
                        restSeconds = 90
                        running = true
                    }
                }
            }
            item { PopButton("ADD EXERCISE", Icons.Outlined.Add, SurfaceHigh) {} }
        }

        AnimatedVisibility(running, enter = fadeIn(), exit = fadeOut()) {
            RestTimerBar(restSeconds) { restSeconds = (restSeconds + it).coerceAtLeast(0) }
        }
    }
}

@Composable
private fun WorkoutExerciseCard(workoutExercise: WorkoutExercise, completed: List<String>, toggle: (String) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceHigh)) {
                ExerciseMotionDemo(workoutExercise.exercise, Modifier.fillMaxSize().padding(4.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(workoutExercise.exercise.name, color = Bone, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Last session beside each set", color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 10.dp)) {
            SetHeader("SET", Modifier.width(38.dp))
            SetHeader("PREVIOUS", Modifier.weight(1f))
            SetHeader("KG", Modifier.width(64.dp))
            SetHeader("REPS", Modifier.width(64.dp))
            Spacer(Modifier.width(36.dp))
        }
        workoutExercise.sets.forEachIndexed { index, set ->
            val key = "${workoutExercise.exercise.name}-$index"
            val isDone = completed.contains(key)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDone) Recovery.copy(.09f) else Color.Transparent)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}", color = if (isDone) Recovery else Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp))
                Text(set.previous, color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                SetValue(set.weight, isDone, Modifier.width(64.dp))
                SetValue(set.reps, isDone, Modifier.width(64.dp))
                SetCheck(isDone) { toggle(key) }
            }
        }
    }
}

@Composable
private fun SetHeader(text: String, modifier: Modifier) = Text(text, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, modifier = modifier)

@Composable
private fun SetValue(text: String, done: Boolean, modifier: Modifier) {
    Box(modifier.height(36.dp).padding(end = 6.dp).clip(RoundedCornerShape(9.dp)).background(if (done) Recovery.copy(.08f) else SurfaceHigh), contentAlignment = Alignment.Center) {
        Text(text, color = if (done) Recovery else Bone, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SetCheck(done: Boolean, onClick: () -> Unit) {
    val view = LocalView.current
    Box(
        Modifier.size(34.dp).clip(CircleShape)
            .background(if (done) Recovery else SurfaceHigh)
            .border(1.dp, if (done) Recovery else Hairline, CircleShape)
            .clickable {
                view.performConfirmHaptic()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) { if (done) Icon(Icons.Outlined.Check, null, tint = Ink, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun RestTimerBar(seconds: Int, adjust: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Surface).border(1.dp, Hairline)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Label("REST TIMER", Strain)
            Text("%d:%02d".format(seconds / 60, seconds % 60), color = Bone, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        TimerAdjust("−15") { adjust(-15) }
        Spacer(Modifier.width(8.dp))
        TimerAdjust("+15") { adjust(15) }
    }
}

@Composable
private fun TimerAdjust(label: String, onClick: () -> Unit) {
    Box(Modifier.height(42.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceHigh).clickable(onClick = onClick).padding(horizontal = 15.dp), contentAlignment = Alignment.Center) {
        Text(label, color = Bone, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExerciseMotionDemo(exercise: Exercise, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "exercise-motion")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1150), repeatMode = RepeatMode.Reverse),
        label = "phase"
    )
    Canvas(modifier) {
        val stroke = Stroke(width = (size.minDimension * .055f).coerceAtLeast(3f), cap = StrokeCap.Round)
        val person = Bone.copy(.92f)
        val accent = exercise.accent
        val cx = size.width * .50f
        val cy = size.height * .48f
        val isSquat = exercise.name.contains("squat", true)
        val isRow = exercise.name.contains("row", true)
        val bend = if (isSquat) phase * size.height * .17f else 0f
        val torsoStart = Offset(cx, cy - size.height * .20f + bend)
        val hip = Offset(cx + if (isRow) size.width * .09f else 0f, cy + bend)
        drawCircle(person, size.minDimension * .075f, Offset(cx - if (isRow) size.width * .08f else 0f, torsoStart.y - size.height * .10f))
        drawLine(person, torsoStart, hip, strokeWidth = stroke.width, cap = StrokeCap.Round)
        if (isSquat) {
            val kneeL = Offset(cx - size.width * .16f, hip.y + size.height * .14f)
            val kneeR = Offset(cx + size.width * .16f, hip.y + size.height * .14f)
            drawLine(accent, hip, kneeL, strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(accent, hip, kneeR, strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(person, kneeL, Offset(cx - size.width * .18f, size.height * .86f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(person, kneeR, Offset(cx + size.width * .18f, size.height * .86f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(person, Offset(cx - size.width * .28f, torsoStart.y + 4), Offset(cx + size.width * .28f, torsoStart.y + 4), strokeWidth = stroke.width * .7f, cap = StrokeCap.Round)
        } else if (isRow) {
            val shoulder = torsoStart
            val handY = cy + size.height * (.10f - .13f * phase)
            drawLine(accent, shoulder, Offset(cx + size.width * .21f, handY), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(person, hip, Offset(cx - size.width * .08f, size.height * .82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(person, hip, Offset(cx + size.width * .18f, size.height * .82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        } else {
            val shoulderY = torsoStart.y + size.height * .04f
            val handY = shoulderY + size.height * (.25f - .21f * phase)
            drawLine(accent, Offset(cx, shoulderY), Offset(cx - size.width * .25f, handY), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(accent, Offset(cx, shoulderY), Offset(cx + size.width * .25f, handY), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(person, hip, Offset(cx - size.width * .13f, size.height * .84f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(person, hip, Offset(cx + size.width * .13f, size.height * .84f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(Bone.copy(.45f), Offset(cx - size.width * .34f, handY - 3), Offset(cx + size.width * .34f, handY - 3), strokeWidth = stroke.width * .55f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun MuscleMap(exercise: Exercise, back: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val body = Bone.copy(.22f)
        val primary = exercise.accent
        val secondary = exercise.accent.copy(.45f)
        val cx = size.width / 2
        val headY = size.height * .11f
        drawCircle(body, size.width * .10f, Offset(cx, headY))
        drawRoundRect(body, Offset(cx - size.width * .15f, size.height * .22f), Size(size.width * .30f, size.height * .34f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * .10f))
        drawLine(body, Offset(cx - size.width * .13f, size.height * .27f), Offset(cx - size.width * .29f, size.height * .58f), size.width * .10f, StrokeCap.Round)
        drawLine(body, Offset(cx + size.width * .13f, size.height * .27f), Offset(cx + size.width * .29f, size.height * .58f), size.width * .10f, StrokeCap.Round)
        drawLine(body, Offset(cx - size.width * .08f, size.height * .53f), Offset(cx - size.width * .13f, size.height * .91f), size.width * .12f, StrokeCap.Round)
        drawLine(body, Offset(cx + size.width * .08f, size.height * .53f), Offset(cx + size.width * .13f, size.height * .91f), size.width * .12f, StrokeCap.Round)

        val names = (exercise.primaryMuscles + exercise.secondaryMuscles).joinToString(" ").lowercase()
        if (names.contains("chest") && !back) {
            drawCircle(primary, size.width * .10f, Offset(cx - size.width * .075f, size.height * .31f))
            drawCircle(primary, size.width * .10f, Offset(cx + size.width * .075f, size.height * .31f))
        }
        if ((names.contains("back") || names.contains("lat")) && back) {
            drawOval(
                primary,
                topLeft = Offset(cx - size.width * .14f, size.height * .28f),
                size = Size(size.width * .28f, size.height * .20f)
            )
        }
        if (names.contains("quad") && !back) {
            drawOval(primary, Offset(cx - size.width * .16f, size.height * .56f), Size(size.width * .14f, size.height * .20f))
            drawOval(primary, Offset(cx + size.width * .02f, size.height * .56f), Size(size.width * .14f, size.height * .20f))
        }
        if (names.contains("glute") && back) {
            drawCircle(primary, size.width * .10f, Offset(cx - size.width * .075f, size.height * .54f))
            drawCircle(primary, size.width * .10f, Offset(cx + size.width * .075f, size.height * .54f))
        }
        if (names.contains("tricep") && back) {
            drawCircle(secondary, size.width * .055f, Offset(cx - size.width * .22f, size.height * .40f))
            drawCircle(secondary, size.width * .055f, Offset(cx + size.width * .22f, size.height * .40f))
        }
        if ((names.contains("delt") || names.contains("shoulder"))) {
            drawCircle(secondary, size.width * .065f, Offset(cx - size.width * .17f, size.height * .28f))
            drawCircle(secondary, size.width * .065f, Offset(cx + size.width * .17f, size.height * .28f))
        }
    }
}

@Composable
private fun MuscleLegend(label: String, muscles: String, color: Color, modifier: Modifier) {
    PremiumCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(7.dp))
                Label(label, color)
            }
            Text(muscles, color = Bone, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun BottomBar(selected: VeyraTab, onSelect: (VeyraTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Surface).border(1.dp, Hairline)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        VeyraTab.entries.forEach { tab ->
            val icon = when (tab) {
                VeyraTab.Today -> Icons.Outlined.Home
                VeyraTab.Train -> Icons.Outlined.FitnessCenter
                VeyraTab.Fuel -> Icons.Outlined.Restaurant
                VeyraTab.Coach -> Icons.Outlined.ChatBubbleOutline
                VeyraTab.You -> Icons.Outlined.PersonOutline
            }
            val active = selected == tab
            Column(
                Modifier.width(64.dp).clip(RoundedCornerShape(14.dp)).clickable { onSelect(tab) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, tab.label, tint = if (active) Acid else Muted, modifier = Modifier.size(21.dp))
                Text(tab.label, color = if (active) Bone else Muted, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(title: String, message: String, accent: Color) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = accent)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = Bone, fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
        Text(message, color = Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 7.dp))
        Text("FOUNDATION READY · NEXT PHASE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
private fun PremiumCard(modifier: Modifier = Modifier, border: Color = Hairline, content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(21.dp)).background(Surface).border(1.dp, border, RoundedCornerShape(21.dp))) { content() }
}

@Composable
private fun PopButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val view = LocalView.current
    Box(Modifier.fillMaxWidth().height(62.dp).padding(bottom = 4.dp)) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = 4.dp.toPx() }.background(color.copy(.35f), RoundedCornerShape(13.dp)))
        Row(
            Modifier.fillMaxSize().graphicsLayer { translationY = if (pressed) 4.dp.toPx() else 0f }
                .clip(RoundedCornerShape(13.dp)).background(color)
                .clickable(interactionSource = interaction, indication = null) {
                    view.performConfirmHaptic()
                    onClick()
                }.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = if (color == SurfaceHigh) Bone else Ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Text(label, color = if (color == SurfaceHigh) Bone else Ink, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SyncPill(syncedAt: Instant?, syncing: Boolean, onRefresh: () -> Unit) {
    val text = when {
        syncing -> "SYNCING"
        syncedAt != null -> syncedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
        else -> "OFFLINE"
    }
    Row(
        Modifier.clip(CircleShape).background(Surface).border(1.dp, Hairline, CircleShape)
            .clickable(enabled = !syncing, onClick = onRefresh)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Sync, null, tint = if (syncedAt != null || syncing) Recovery else Muted, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun HealthSnapshot.toScores(): List<Score> = listOf(
    Score("Recovery", recoveryScore, note = recoveryNote, color = Recovery),
    Score("Sleep", sleepScore, note = sleepNote, color = Sleep),
    Score("Strain", strainScore, "/21", strainNote, Strain),
)

private fun View.performConfirmHaptic() {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
    )
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Label(title)
        Text(action, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun Label(text: String, color: Color = Muted) {
    Text(text, color = color, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
}

@Composable
private fun TopBack(label: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Surface).border(1.dp, Hairline, CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Bone, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Label(label)
    }
}
