package com.aura.ui.screens.workout

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aura.ui.components.AuraTextField
import com.aura.AuraApp
import com.aura.data.local.entity.RoutineEntity
import com.aura.data.local.entity.WorkoutEntity
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ViewModel
class WorkoutVM(app: Application) : AndroidViewModel(app) {
    private val dao = (app as AuraApp).db.workoutDao()

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
        viewModelScope.launch {
            dao.getWorkoutHistory().collect {
                _history.value = it.take(5)
                _totalCount.value = it.size
            }
        }
        viewModelScope.launch { dao.observeActiveWorkout().collect { _activeWorkout.value = it } }
    }

    fun createRoutine(name: String) = viewModelScope.launch {
        dao.insertRoutine(RoutineEntity(name = name))
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
                Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset).padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Workout", style = MaterialTheme.typography.headlineLarge, color = V.White)
                    Text("$total workouts completed", style = MaterialTheme.typography.bodySmall, color = V.Gray)
                }
            }
        }

        item {
            AuraPrimaryButton(
                if (active == null) "Start empty workout" else "Resume workout",
                onClick = { nav.navigate("workout/active") },
                modifier = Modifier.padding(horizontal = AuraScreenInset),
            )
        }

        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Routines", style = MaterialTheme.typography.headlineMedium, color = V.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutAction("New routine", Icons.Rounded.Add, { showNewDialog = true }, Modifier.weight(1f))
                    WorkoutAction("Exercise library", Icons.Rounded.Search,
                        { nav.navigate("exercise/search?picker=false") }, Modifier.weight(1f))
                }
                Text("My routines · ${routines.size}", style = MaterialTheme.typography.labelMedium, color = V.Gray)
            }
        }

        if (routines.isEmpty()) {
            item {
                VCard(Modifier.padding(horizontal = AuraScreenInset)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.FitnessCenter, null, tint = V.Gray, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No routines yet", style = MaterialTheme.typography.titleMedium, color = V.Gray)
                        Text("Create a routine to get started", style = MaterialTheme.typography.bodySmall, color = V.Gray)
                    }
                }
            }
        }

        items(routines, key = { it.id }) { routine ->
            VCard(Modifier.padding(horizontal = AuraScreenInset)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(routine.name, style = MaterialTheme.typography.titleLarge, color = V.White, fontWeight = FontWeight.SemiBold)
                    if (routine.description.isNotBlank()) {
                        Text(routine.description, style = MaterialTheme.typography.bodySmall, color = V.Gray)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkoutAction("View routine", Icons.Rounded.FitnessCenter,
                            { nav.navigate("workout/routine/${routine.id}") }, Modifier.weight(1f))
                        if (active == null) {
                            WorkoutAction("Start", Icons.Rounded.PlayArrow,
                                { nav.navigate("workout/active?routineId=${routine.id}") }, Modifier.weight(1f))
                        }
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
                VCard(Modifier.padding(horizontal = AuraScreenInset)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, null, tint = V.Gray, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name, style = MaterialTheme.typography.titleMedium, color = V.White)
                            Text(SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(w.startTime)),
                                style = MaterialTheme.typography.bodySmall, color = V.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val m = w.durationSeconds / 60
                            Text("${m}m", style = MaterialTheme.typography.labelMedium, color = V.Teal)
                            Text("%.0f kg".format(w.totalVolume), style = MaterialTheme.typography.bodySmall, color = V.Gray)
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
            containerColor = V.Black,
            modifier = Modifier.border(1.dp, V.SurfaceBright, RoundedCornerShape(28.dp)),
            title = { Text("New Routine", color = V.White) },
            text = {
                AuraTextField(name, { name = it }, "Routine name")
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
