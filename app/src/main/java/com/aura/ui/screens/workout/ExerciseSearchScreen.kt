package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.AuraTextField
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

@Composable
fun ExerciseSearchScreen(nav: NavController, picker: Boolean) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    val results = remember(query, muscle, equipment) {
        ExerciseLib.all.filter { exercise ->
            val text = query.trim()
            (exercise.name.contains(text, ignoreCase = true) ||
                exercise.primaryMuscles.any { it.contains(text, ignoreCase = true) }) &&
                (muscle == null || muscle in exercise.primaryMuscles || muscle in exercise.secondaryMuscles) &&
                (equipment == null || exercise.equipment == equipment)
        }
    }

    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding().navigationBarsPadding().imePadding()) {
        WorkoutHeader(if (picker) "Add exercise" else "Exercise library", { nav.popBackStack() })
        Column(Modifier.padding(horizontal = AuraScreenInset, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuraTextField(query, { query = it }, "Search exercises", leadingIcon = {
                Icon(Icons.Rounded.Search, null, tint = V.Gray)
            })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseFilter("Equipment", equipment, ExerciseLib.equipmentList, { equipment = it }, Modifier.weight(1f))
                ExerciseFilter("Muscles", muscle, ExerciseLib.muscleGroups, { muscle = it }, Modifier.weight(1f))
            }
        }

        Text(
            "${results.size} exercises",
            Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = V.Gray,
        )
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = AuraScreenInset, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (results.isEmpty()) item {
                Text("No exercises match these filters.", color = V.Gray, style = MaterialTheme.typography.bodyMedium)
            }
            items(results, key = { it.id }) { exercise ->
                val openDetails = { nav.navigate("exercise/${exercise.id}?picker=$picker") }
                Surface(shape = RoundedCornerShape(16.dp), color = V.SurfaceRaised) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.weight(1f).clickable {
                                if (picker) {
                                    nav.previousBackStackEntry?.savedStateHandle?.set("selectedExerciseId", exercise.id)
                                    nav.popBackStack()
                                } else {
                                    openDetails()
                                }
                            }.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExerciseGlyph()
                            Column {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = V.White)
                                Text(
                                    "${exercise.primaryMuscles.joinToString()} · ${exercise.equipment}",
                                    style = MaterialTheme.typography.bodySmall, color = V.Gray,
                                )
                            }
                        }
                        if (picker) {
                            IconButton(onClick = openDetails) {
                                Icon(Icons.Rounded.Info, "About ${exercise.name}", tint = V.Teal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseFilter(
    label: String,
    selected: String?,
    options: List<String>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        WorkoutAction(selected ?: label, Icons.Rounded.ExpandMore, { expanded = true }, Modifier.fillMaxWidth())
        DropdownMenu(expanded, { expanded = false }) {
            (listOf<String?>(null) + options).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option ?: "All ${label.lowercase()}", color = if (option == selected) V.Teal else V.White) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
