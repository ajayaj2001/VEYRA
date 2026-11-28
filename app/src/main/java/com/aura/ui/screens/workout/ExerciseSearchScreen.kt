package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.AuraSearchField
import androidx.compose.ui.semantics.Role
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

@Composable
internal fun ExerciseSearchScreen(
    nav: NavController,
    picker: Boolean,
    selectedIds: List<String>,
    existingIds: List<String>,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    val exercises = ExerciseLib.all
    val equipmentOptions = remember(exercises) { exercises.map { it.equipment }.distinct().sorted() }
    val muscleOptions = remember(exercises) { exercises.flatMap { it.primaryMuscles }.distinct().sorted() }
    val results = remember(exercises, query, muscle, equipment) {
        // Every typed word must appear somewhere, so "incline chest press" finds incline presses for the chest.
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        exercises.filter { exercise ->
            val searchable = (listOf(exercise.name, exercise.equipment) + exercise.primaryMuscles +
                exercise.secondaryMuscles + exercise.searchTerms).joinToString(" ")
            words.all { searchable.contains(it, ignoreCase = true) } &&
                (muscle == null || muscle in exercise.primaryMuscles || muscle in exercise.secondaryMuscles) &&
                (equipment == null || exercise.equipment == equipment)
        }
    }

    Column(Modifier.fillMaxSize().background(V.Canvas).statusBarsPadding().navigationBarsPadding().imePadding()) {
        WorkoutHeader(if (picker) "Add exercise" else "Library", { nav.popBackStack() })
        Column(Modifier.padding(horizontal = AuraScreenInset, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuraSearchField(query, { query = it }, "Search exercises")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseFilter("Equipment", equipment, equipmentOptions, { equipment = it }, Modifier.weight(1f))
                ExerciseFilter("Muscles", muscle, muscleOptions, { muscle = it }, Modifier.weight(1f))
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
                val selected = exercise.id in selectedIds
                val alreadyAdded = picker && exercise.id in existingIds
                val openDetails = {
                    nav.navigate("exercise/${exercise.id}?picker=${picker && !alreadyAdded}")
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = V.SurfaceRaised,
                    border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, V.Teal) else null,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val action = if (picker && !alreadyAdded) {
                            Modifier.toggleable(selected, role = Role.Checkbox) { onToggle(exercise.id) }
                        } else {
                            Modifier.clickable(onClick = openDetails)
                        }
                        Row(
                            Modifier.weight(1f).heightIn(min = 84.dp).then(action).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box {
                                ExerciseThumbnail(exercise.imageUrl)
                                if (selected) {
                                    Box(
                                        Modifier.align(Alignment.TopStart).offset(x = (-4).dp, y = (-4).dp)
                                            .size(22.dp).background(V.SurfaceRaised, CircleShape).padding(2.dp)
                                            .background(V.Teal, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Check, null, Modifier.size(13.dp), tint = V.Black)
                                    }
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = V.White)
                                Text(
                                    if (alreadyAdded) "Already added" else "${exercise.primaryMuscles.joinToString()} · ${exercise.equipment}",
                                    style = MaterialTheme.typography.bodySmall, color = V.Gray,
                                )
                            }
                        }
                        IconButton(onClick = openDetails) {
                            Icon(Icons.Rounded.Info, "About ${exercise.name}", tint = V.Teal)
                        }
                    }
                }
            }
        }
        if (picker) {
            HorizontalDivider(color = V.SurfaceRaised)
            AuraPrimaryButton(
                when (selectedIds.size) {
                    0 -> "Select exercises"
                    1 -> "Add 1 exercise"
                    else -> "Add ${selectedIds.size} exercises"
                },
                onConfirm,
                Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp),
                enabled = selectedIds.isNotEmpty(),
            )
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
