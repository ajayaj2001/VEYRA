package com.veyra.ui.screens.workout

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veyra.VeyraApp
import com.veyra.data.local.entity.RoutineEntity
import com.veyra.data.local.entity.WorkoutEntity
import com.veyra.navigation.Routes
import com.veyra.ui.components.VButton
import com.veyra.ui.components.VCard
import com.veyra.ui.theme.V
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ViewModel
class WorkoutVM(app: Application) : AndroidViewModel(app) {
    private val dao = (app as VeyraApp).db.workoutDao()

    private val _routines = MutableStateFlow<List<RoutineEntity>>(emptyList())
    val routines: StateFlow<List<RoutineEntity>> = _routines

    private val _history = MutableStateFlow<List<WorkoutEntity>>(emptyList())
    val history: StateFlow<List<WorkoutEntity>> = _history

    private val _activeWorkout = MutableStateFlow<WorkoutEntity?>(null)
    val activeWorkout: StateFlow<WorkoutEntity?> = _activeWorkout

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    init {
        viewModelScope.launch { dao.getAllRoutines().collect { _routines.value = it } }
        viewModelScope.launch { dao.getWorkoutHistory().collect { _history.value = it.take(5) } }
        viewModelScope.launch { dao.observeActiveWorkout().collect { _activeWorkout.value = it } }
        viewModelScope.launch { _totalCount.value = dao.getTotalWorkoutCount() }
    }

    fun createRoutine(name: String) = viewModelScope.launch {
        dao.insertRoutine(RoutineEntity(name = name))
    }

    fun startEmptyWorkout() = viewModelScope.launch {
        dao.insertWorkout(WorkoutEntity(name = "Quick Workout", isActive = true))
    }
}

@Composable
fun WorkoutScreen(nav: NavController, vm: WorkoutVM = viewModel()) {
    val routines by vm.routines.collectAsState()
    val history by vm.history.collectAsState()
    val active by vm.activeWorkout.collectAsState()
    val total by vm.totalCount.collectAsState()
    var showNewDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(V.Black)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Workout", style = MaterialTheme.typography.headlineLarge, color = V.White)
                    Text("$total workouts completed", style = MaterialTheme.typography.bodySmall, color = V.Gray)
                }
            }
        }

        // Active workout banner
        if (active != null) {
            item {
                VCard(
                    Modifier.padding(horizontal = 16.dp),
                    color = V.Teal.copy(alpha = 0.12f),
                    onClick = { nav.navigate("workout/active") },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, null, tint = V.Teal, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Workout in Progress", style = MaterialTheme.typography.titleLarge, color = V.White, fontWeight = FontWeight.Bold)
                            Text(active!!.name, style = MaterialTheme.typography.bodySmall, color = V.Gray)
                        }
                        Icon(Icons.Rounded.PlayArrow, null, tint = V.Teal, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // Start empty workout
        item {
            VButton(
                "Start Empty Workout",
                onClick = {
                    vm.startEmptyWorkout()
                    nav.navigate("workout/active")
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // Routines section
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("My Routines", style = MaterialTheme.typography.titleLarge, color = V.White, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(V.SurfaceElevated).clickable { showNewDialog = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, "New", tint = V.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (routines.isEmpty()) {
            item {
                VCard(Modifier.padding(horizontal = 16.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.FitnessCenter, null, tint = V.GrayDark, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No routines yet", style = MaterialTheme.typography.titleMedium, color = V.Gray)
                        Text("Create a routine to get started", style = MaterialTheme.typography.bodySmall, color = V.GrayDark)
                    }
                }
            }
        }

        items(routines) { routine ->
            VCard(
                Modifier.padding(horizontal = 16.dp),
                onClick = { nav.navigate("workout/active?routineId=${routine.id}") },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(routine.name, style = MaterialTheme.typography.titleLarge, color = V.White, fontWeight = FontWeight.SemiBold)
                        if (routine.description.isNotEmpty()) {
                            Text(routine.description, style = MaterialTheme.typography.bodySmall, color = V.GrayDark, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(V.Teal.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, "Start", tint = V.Teal, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Recent workouts
        if (history.isNotEmpty()) {
            item {
                Text("Recent", style = MaterialTheme.typography.titleSmall, color = V.Gray, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }
            items(history) { w ->
                VCard(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, null, tint = V.GrayDark, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name, style = MaterialTheme.typography.titleMedium, color = V.White)
                            Text(SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(w.startTime)),
                                style = MaterialTheme.typography.bodySmall, color = V.GrayDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val m = w.durationSeconds / 60
                            Text("${m}m", style = MaterialTheme.typography.labelMedium, color = V.Teal)
                            Text("%.0f kg".format(w.totalVolume), style = MaterialTheme.typography.bodySmall, color = V.GrayDark)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }

    // New routine dialog
    if (showNewDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            containerColor = V.SurfaceElevated,
            title = { Text("New Routine", color = V.White) },
            text = {
                TextField(
                    value = name, onValueChange = { name = it },
                    placeholder = { Text("Routine name") }, singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = V.Surface, unfocusedContainerColor = V.Surface,
                        focusedTextColor = V.White, unfocusedTextColor = V.White,
                        cursorColor = V.Teal, focusedIndicatorColor = V.Teal,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) { vm.createRoutine(name.trim()); showNewDialog = false }
                }) { Text("Create", color = V.Teal) }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text("Cancel", color = V.Gray) }
            },
        )
    }
}
