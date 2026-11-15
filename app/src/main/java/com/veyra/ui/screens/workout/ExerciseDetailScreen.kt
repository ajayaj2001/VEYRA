package com.veyra.ui.screens.workout

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veyra.VeyraApp
import com.veyra.data.local.entity.WorkoutSetEntity
import com.veyra.data.model.Exercise
import com.veyra.data.model.ExerciseRepository
import com.veyra.data.model.Muscle
import com.veyra.data.model.MuscleGroup
import com.veyra.ui.components.GlassCard
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExerciseDetailScreen(
    navController: NavController,
    exerciseId: String,
) {
    val exercise = remember { ExerciseRepository.getById(exerciseId) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "History", "How To")

    if (exercise == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Exercise not found", color = VeyraColors.TextSecondary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.Background)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VeyraDimens.SpaceSm, vertical = VeyraDimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = VeyraColors.TextPrimary)
            }
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.headlineSmall,
                color = VeyraColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = VeyraColors.Background,
            contentColor = VeyraColors.Accent,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = VeyraColors.Accent,
                    )
                }
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) VeyraColors.Accent else VeyraColors.TextTertiary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        AnimatedContent(targetState = selectedTab, label = "tabContent") { tab ->
            when (tab) {
                0 -> ExerciseSummaryTab(exercise)
                1 -> ExerciseHistoryTab(exerciseId)
                2 -> ExerciseHowToTab(exercise)
            }
        }
    }
}

@Composable
private fun ExerciseSummaryTab(exercise: Exercise) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VeyraDimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd),
    ) {
        // Exercise illustration placeholder
        GlassCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.FitnessCenter,
                    contentDescription = null,
                    tint = VeyraColors.Accent,
                    modifier = Modifier.size(80.dp),
                )
                Spacer(Modifier.height(VeyraDimens.SpaceSm))
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = VeyraColors.TextPrimary,
                )
                Text(
                    text = "${exercise.category.displayName} · ${exercise.equipment.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeyraColors.TextTertiary,
                )
            }
        }

        // Muscle impact diagram
        Text(
            text = "Muscle Impact",
            style = MaterialTheme.typography.titleLarge,
            color = VeyraColors.TextPrimary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MuscleBodyDiagram(
                label = "Front",
                highlightedPrimary = exercise.primaryMuscles.filter { it.isFront() },
                highlightedSecondary = exercise.secondaryMuscles.filter { it.isFront() },
            )
            MuscleBodyDiagram(
                label = "Back",
                highlightedPrimary = exercise.primaryMuscles.filter { !it.isFront() },
                highlightedSecondary = exercise.secondaryMuscles.filter { !it.isFront() },
            )
        }

        // Target muscles
        GlassCard {
            Column {
                Text(
                    text = "Target Muscles",
                    style = MaterialTheme.typography.titleMedium,
                    color = VeyraColors.TextPrimary,
                )
                Spacer(Modifier.height(VeyraDimens.SpaceSm))
                Text(
                    text = "Primary: ${exercise.primaryMuscles.joinToString { it.displayName }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeyraColors.Accent,
                )
                if (exercise.secondaryMuscles.isNotEmpty()) {
                    Text(
                        text = "Secondary: ${exercise.secondaryMuscles.joinToString { it.displayName }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeyraColors.TextTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseHistoryTab(exerciseId: String) {
    val context = LocalContext.current
    var history by remember { mutableStateOf<List<WorkoutSetEntity>>(emptyList()) }

    LaunchedEffect(exerciseId) {
        withContext(Dispatchers.IO) {
            val app = context.applicationContext as VeyraApp
            history = app.database.workoutDao().getRecentSetsForExercise(exerciseId, 50)
        }
    }

    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No history yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = VeyraColors.TextSecondary,
                )
                Text(
                    text = "Complete sets to see your progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeyraColors.TextTertiary,
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(VeyraDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceSm),
        ) {
            // Simple progression chart (volume over time)
            GlassCard {
                Column {
                    Text(
                        text = "Volume Progression",
                        style = MaterialTheme.typography.titleMedium,
                        color = VeyraColors.TextPrimary,
                    )
                    Spacer(Modifier.height(VeyraDimens.SpaceMd))
                    SimpleVolumeChart(
                        sets = history,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    )
                }
            }

            // Personal records
            val bestWeight = history.maxByOrNull { it.weight }
            val bestVolume = history.maxByOrNull { it.weight * it.reps }
            val best1RM = history.maxByOrNull {
                if (it.reps > 0) it.weight * (36.0 / (37.0 - it.reps)) else 0.0
            }

            GlassCard {
                Column {
                    Text(
                        text = "Personal Records",
                        style = MaterialTheme.typography.titleMedium,
                        color = VeyraColors.TextPrimary,
                    )
                    Spacer(Modifier.height(VeyraDimens.SpaceSm))
                    if (bestWeight != null) {
                        RecordRow("Heaviest", "${bestWeight.weight}kg x ${bestWeight.reps}")
                    }
                    if (best1RM != null) {
                        val e1rm = best1RM.weight * (36.0 / (37.0 - best1RM.reps))
                        RecordRow("Est. 1RM", "${"%.1f".format(e1rm)}kg")
                    }
                    if (bestVolume != null) {
                        RecordRow("Best Volume", "${"%.0f".format(bestVolume.weight * bestVolume.reps)}kg")
                    }
                }
            }

            // Recent sets
            history.take(20).forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VeyraDimens.RadiusSm))
                        .background(VeyraColors.SurfaceElevated)
                        .padding(VeyraDimens.SpaceMd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Set ${set.setNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeyraColors.TextSecondary,
                    )
                    Text(
                        "${set.weight}kg x ${set.reps}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeyraColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = VeyraColors.TextTertiary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = VeyraColors.Accent, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SimpleVolumeChart(
    sets: List<WorkoutSetEntity>,
    modifier: Modifier = Modifier,
) {
    val volumes = sets.reversed().map { it.weight * it.reps }
    if (volumes.isEmpty()) return

    val maxVolume = volumes.max().coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val barWidth = size.width / volumes.size.coerceAtLeast(1)
        volumes.forEachIndexed { index, volume ->
            val barHeight = (volume / maxVolume * size.height * 0.9f).toFloat()
            drawRoundRect(
                color = VeyraColors.Accent.copy(alpha = 0.6f),
                topLeft = Offset(index * barWidth + barWidth * 0.1f, size.height - barHeight),
                size = Size(barWidth * 0.8f, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
            )
        }
    }
}

@Composable
private fun ExerciseHowToTab(exercise: Exercise) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VeyraDimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd),
    ) {
        // Instructions
        if (exercise.instructions.isNotEmpty()) {
            Text(
                text = "Instructions",
                style = MaterialTheme.typography.titleLarge,
                color = VeyraColors.TextPrimary,
            )
            exercise.instructions.forEachIndexed { index, instruction ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VeyraDimens.RadiusMd))
                        .background(VeyraColors.SurfaceElevated)
                        .padding(VeyraDimens.SpaceMd),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(VeyraColors.PrimaryStart.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = VeyraColors.PrimaryStart,
                        )
                    }
                    Spacer(Modifier.width(VeyraDimens.SpaceMd))
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeyraColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Coaching cues
        if (exercise.coachingCues.isNotEmpty()) {
            Spacer(Modifier.height(VeyraDimens.SpaceSm))
            Text(
                text = "Coaching Cues",
                style = MaterialTheme.typography.titleLarge,
                color = VeyraColors.TextPrimary,
            )
            exercise.coachingCues.forEach { cue ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VeyraDimens.RadiusMd))
                        .background(VeyraColors.Accent.copy(alpha = 0.08f))
                        .padding(VeyraDimens.SpaceMd),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = VeyraColors.Accent,
                    )
                    Spacer(Modifier.width(VeyraDimens.SpaceSm))
                    Text(
                        text = cue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeyraColors.Accent,
                    )
                }
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceXxl))
    }
}

@Composable
private fun MuscleBodyDiagram(
    label: String,
    highlightedPrimary: List<Muscle>,
    highlightedSecondary: List<Muscle>,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = VeyraColors.TextTertiary,
        )
        Spacer(Modifier.height(VeyraDimens.SpaceXs))

        // Simplified body outline with muscle regions highlighted
        Canvas(
            modifier = Modifier
                .size(120.dp, 200.dp)
                .clip(RoundedCornerShape(VeyraDimens.RadiusMd))
                .background(VeyraColors.SurfaceElevated),
        ) {
            val w = size.width
            val h = size.height

            // Body outline
            drawCircle(
                color = VeyraColors.TextTertiary.copy(alpha = 0.2f),
                radius = w * 0.12f,
                center = Offset(w / 2, h * 0.1f),
            )
            // Torso
            drawRoundRect(
                color = VeyraColors.TextTertiary.copy(alpha = 0.1f),
                topLeft = Offset(w * 0.25f, h * 0.18f),
                size = Size(w * 0.5f, h * 0.35f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f),
            )
            // Legs
            drawRoundRect(
                color = VeyraColors.TextTertiary.copy(alpha = 0.1f),
                topLeft = Offset(w * 0.25f, h * 0.55f),
                size = Size(w * 0.2f, h * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
            )
            drawRoundRect(
                color = VeyraColors.TextTertiary.copy(alpha = 0.1f),
                topLeft = Offset(w * 0.55f, h * 0.55f),
                size = Size(w * 0.2f, h * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
            )

            // Highlight primary muscles
            highlightedPrimary.forEach { muscle ->
                val (offset, mSize) = getMusclePosition(muscle, w, h)
                drawRoundRect(
                    color = VeyraColors.Accent.copy(alpha = 0.6f),
                    topLeft = offset,
                    size = mSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                )
            }

            // Highlight secondary muscles
            highlightedSecondary.forEach { muscle ->
                val (offset, mSize) = getMusclePosition(muscle, w, h)
                drawRoundRect(
                    color = VeyraColors.Accent.copy(alpha = 0.25f),
                    topLeft = offset,
                    size = mSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                )
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceXs))

        // Muscle labels
        (highlightedPrimary + highlightedSecondary).forEach { muscle ->
            Text(
                text = muscle.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = if (muscle in highlightedPrimary) VeyraColors.Accent else VeyraColors.TextTertiary,
            )
        }
    }
}

private fun getMusclePosition(muscle: Muscle, w: Float, h: Float): Pair<Offset, Size> {
    return when (muscle) {
        Muscle.CHEST, Muscle.UPPER_CHEST, Muscle.LOWER_CHEST -> Offset(w * 0.28f, h * 0.2f) to Size(w * 0.44f, h * 0.12f)
        Muscle.FRONT_DELT, Muscle.SIDE_DELT, Muscle.REAR_DELT -> Offset(w * 0.18f, h * 0.17f) to Size(w * 0.15f, h * 0.08f)
        Muscle.BICEPS -> Offset(w * 0.15f, h * 0.28f) to Size(w * 0.12f, h * 0.1f)
        Muscle.TRICEPS -> Offset(w * 0.73f, h * 0.28f) to Size(w * 0.12f, h * 0.1f)
        Muscle.FOREARMS -> Offset(w * 0.12f, h * 0.38f) to Size(w * 0.1f, h * 0.1f)
        Muscle.LATS -> Offset(w * 0.25f, h * 0.25f) to Size(w * 0.5f, h * 0.15f)
        Muscle.UPPER_BACK, Muscle.TRAPS -> Offset(w * 0.3f, h * 0.18f) to Size(w * 0.4f, h * 0.1f)
        Muscle.LOWER_BACK -> Offset(w * 0.32f, h * 0.4f) to Size(w * 0.36f, h * 0.1f)
        Muscle.ABS, Muscle.OBLIQUES -> Offset(w * 0.32f, h * 0.35f) to Size(w * 0.36f, h * 0.15f)
        Muscle.QUADS -> Offset(w * 0.25f, h * 0.55f) to Size(w * 0.5f, h * 0.15f)
        Muscle.HAMSTRINGS -> Offset(w * 0.25f, h * 0.6f) to Size(w * 0.5f, h * 0.12f)
        Muscle.GLUTES -> Offset(w * 0.28f, h * 0.5f) to Size(w * 0.44f, h * 0.1f)
        Muscle.CALVES -> Offset(w * 0.28f, h * 0.78f) to Size(w * 0.44f, h * 0.1f)
        Muscle.HIP_FLEXORS -> Offset(w * 0.3f, h * 0.52f) to Size(w * 0.4f, h * 0.06f)
    }
}

private fun Muscle.isFront(): Boolean = when (this) {
    Muscle.CHEST, Muscle.UPPER_CHEST, Muscle.LOWER_CHEST,
    Muscle.FRONT_DELT, Muscle.SIDE_DELT,
    Muscle.BICEPS, Muscle.FOREARMS,
    Muscle.QUADS, Muscle.ABS, Muscle.OBLIQUES,
    Muscle.HIP_FLEXORS -> true
    else -> false
}
