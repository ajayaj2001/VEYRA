package com.veyra.ui.screens.workout

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veyra.VeyraApp
import com.veyra.data.local.entity.WorkoutSetEntity
import com.veyra.data.model.ExerciseLib
import com.veyra.ui.components.VButton
import com.veyra.ui.components.VCard
import com.veyra.ui.theme.V
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExerciseDetailScreen(nav: NavController, exerciseId: String) {
    val exercise = remember { ExerciseLib.getById(exerciseId) }
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "History", "How to")

    if (exercise == null) {
        Box(Modifier.fillMaxSize().background(V.Black), contentAlignment = Alignment.Center) {
            Text("Exercise not found", color = V.Gray)
        }
        return
    }

    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, "Back", tint = V.White) }
            Text(exercise.name, style = MaterialTheme.typography.titleLarge, color = V.White, modifier = Modifier.weight(1f), maxLines = 1)
        }

        TabRow(
            selectedTabIndex = tab, containerColor = V.Black, contentColor = V.Teal,
            indicator = { positions ->
                if (tab < positions.size) SecondaryIndicator(Modifier.tabIndicatorOffset(positions[tab]), color = V.Teal)
            },
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { tab = i }, text = {
                    Text(title, color = if (tab == i) V.Teal else V.GrayDark, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal)
                })
            }
        }

        AnimatedContent(tab, label = "tab") { t ->
            when (t) {
                0 -> SummaryTab(exercise)
                1 -> HistoryTab(exerciseId)
                2 -> HowToTab(exercise)
            }
        }
    }
}

@Composable
private fun SummaryTab(ex: com.veyra.data.model.Exercise) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        VCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.FitnessCenter, null, tint = V.Teal, modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(8.dp))
                Text(ex.name, style = MaterialTheme.typography.headlineMedium, color = V.White)
                Text("${ex.category} · ${ex.equipment}", style = MaterialTheme.typography.bodyMedium, color = V.Gray)
            }
        }
        VCard {
            Column {
                Text("Target Muscles", style = MaterialTheme.typography.titleLarge, color = V.White)
                Spacer(Modifier.height(8.dp))
                Text("Primary: ${ex.primaryMuscles.joinToString()}", style = MaterialTheme.typography.bodyMedium, color = V.Teal)
                if (ex.secondaryMuscles.isNotEmpty())
                    Text("Secondary: ${ex.secondaryMuscles.joinToString()}", style = MaterialTheme.typography.bodyMedium, color = V.Gray)
            }
        }
    }
}

@Composable
private fun HistoryTab(exerciseId: String) {
    val ctx = LocalContext.current
    var history by remember { mutableStateOf<List<WorkoutSetEntity>>(emptyList()) }

    LaunchedEffect(exerciseId) {
        withContext(Dispatchers.IO) {
            history = (ctx.applicationContext as VeyraApp).db.workoutDao().getRecentSetsForExercise(exerciseId, 50)
        }
    }

    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No history yet", style = MaterialTheme.typography.titleMedium, color = V.Gray)
                Text("Complete sets to see progress", style = MaterialTheme.typography.bodySmall, color = V.GrayDark)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Simple volume chart
            VCard {
                Column {
                    Text("Volume Progression", style = MaterialTheme.typography.titleLarge, color = V.White)
                    Spacer(Modifier.height(12.dp))
                    val vols = history.reversed().map { it.weight * it.reps }
                    if (vols.isNotEmpty()) {
                        val max = vols.max().coerceAtLeast(1.0)
                        Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                            val bw = size.width / vols.size
                            val w = bw * 0.4f
                            vols.forEachIndexed { i, v ->
                                val h = (v / max * size.height * 0.85f).toFloat()
                                drawRoundRect(
                                    V.Teal.copy(alpha = 0.6f),
                                    Offset(i * bw + (bw - w) / 2, size.height - h),
                                    Size(w, h), CornerRadius(w / 2, w / 2),
                                )
                            }
                        }
                    }
                }
            }

            // PR
            val best = history.maxByOrNull { it.weight }
            if (best != null) {
                VCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Personal Best", style = MaterialTheme.typography.titleMedium, color = V.Gray)
                        Text("${best.weight}kg × ${best.reps}", style = MaterialTheme.typography.titleMedium, color = V.Teal, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Recent sets
            history.take(15).forEach { set ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(V.Surface).padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Set ${set.setNumber}", style = MaterialTheme.typography.bodyMedium, color = V.Gray)
                    Text("${set.weight}kg × ${set.reps}", style = MaterialTheme.typography.bodyMedium, color = V.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun HowToTab(ex: com.veyra.data.model.Exercise) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (ex.instructions.isNotEmpty()) {
            Text("Instructions", style = MaterialTheme.typography.titleLarge, color = V.White)
            ex.instructions.forEachIndexed { i, inst ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(V.Surface).padding(14.dp),
                ) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(V.Teal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = V.Teal)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(inst, style = MaterialTheme.typography.bodyMedium, color = V.White, modifier = Modifier.weight(1f))
                }
            }
        }
        if (ex.tips.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Coaching Tips", style = MaterialTheme.typography.titleLarge, color = V.White)
            ex.tips.forEach { tip ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(V.Teal.copy(alpha = 0.06f)).padding(14.dp),
                ) {
                    Text("•", style = MaterialTheme.typography.bodyLarge, color = V.Teal)
                    Spacer(Modifier.width(8.dp))
                    Text(tip, style = MaterialTheme.typography.bodyMedium, color = V.Teal)
                }
            }
        }
    }
}
