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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.aura.data.exercise.ExerciseStore
import com.aura.data.model.ExerciseLib
import java.io.IOException
import com.aura.ui.components.AuraTextField
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

@Composable
fun ExerciseSearchScreen(nav: NavController, picker: Boolean) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var catalog by remember { mutableStateOf(ExerciseStore.cached()) }
    var loading by remember { mutableStateOf(true) }
    var offline by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            // The first download arrives a page at a time, so the list fills in as it goes.
            catalog = ExerciseStore.loadCatalog { catalog = it }
        } catch (_: IOException) {
            offline = true
        } finally {
            loading = false
        }
    }
    // The built-in list keeps the library usable when the online one has never loaded.
    val exercises = catalog.ifEmpty { ExerciseLib.all }
    val equipmentOptions = remember(exercises) { exercises.map { it.equipment }.distinct().sorted() }
    val muscleOptions = remember(exercises) {
        exercises.flatMap { it.bodyParts.ifEmpty { it.primaryMuscles } }.distinct().sorted()
    }
    val results = remember(exercises, query, muscle, equipment) {
        // Every typed word must appear somewhere, so "incline chest press" finds incline presses for the chest.
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        exercises.filter { exercise ->
            val searchable = (listOf(exercise.name, exercise.equipment) + exercise.bodyParts +
                exercise.primaryMuscles + exercise.secondaryMuscles).joinToString(" ")
            words.all { searchable.contains(it, ignoreCase = true) } &&
                (muscle == null || muscle in exercise.bodyParts || muscle in exercise.primaryMuscles ||
                    muscle in exercise.secondaryMuscles) &&
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
                ExerciseFilter("Equipment", equipment, equipmentOptions, { equipment = it }, Modifier.weight(1f))
                ExerciseFilter("Muscles", muscle, muscleOptions, { muscle = it }, Modifier.weight(1f))
            }
        }

        Text(
            when {
                loading && catalog.isEmpty() -> "Loading exercise library…"
                loading -> "${results.size} exercises · still downloading the library…"
                offline -> "${results.size} built-in exercises · online library unavailable"
                else -> "${results.size} exercises"
            },
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
                            if (exercise.imageUrl == null) ExerciseGlyph() else AsyncImage(
                                // A still first frame: animating every row's GIF would make scrolling heavy.
                                ImageRequest.Builder(LocalContext.current).data(exercise.imageUrl)
                                    .decoderFactory(BitmapFactoryDecoder.Factory()).build(),
                                null,
                                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(V.White),
                                contentScale = ContentScale.Crop,
                            )
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
