package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.ui.components.AuraTextField
import com.aura.data.model.Exercise
import com.aura.data.model.ExerciseLib
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

@Composable
fun ExerciseSearchScreen(nav: NavController) {
    var query by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var selectedEquip by remember { mutableStateOf<String?>(null) }

    val results = remember(query, selectedMuscle, selectedEquip) {
        var list = if (query.isNotBlank()) ExerciseLib.search(query) else ExerciseLib.all
        selectedMuscle?.let { m -> list = list.filter { it.primaryMuscles.any { p -> p == m } } }
        selectedEquip?.let { e -> list = list.filter { it.equipment == e } }
        list
    }

    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding()) {
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, "Back", tint = V.White) }
            Text("Add Exercise", style = MaterialTheme.typography.titleLarge, color = V.White)
        }

        // Search
        AuraTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search exercises…",
            modifier = Modifier.padding(horizontal = AuraScreenInset),
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = V.Gray) },
        )

        Spacer(Modifier.height(8.dp))

        // Muscle filters
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = AuraScreenInset), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseLib.muscleGroups.forEach { m ->
                FilterChip(
                    selected = selectedMuscle == m,
                    onClick = { selectedMuscle = if (selectedMuscle == m) null else m },
                    label = { Text(m) },
                    shape = CircleShape,
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = V.SurfaceRaised, labelColor = V.Gray,
                        selectedContainerColor = V.Teal, selectedLabelColor = V.Black,
                    ),
                )
            }
        }

        // Equipment filters
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = AuraScreenInset, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseLib.equipmentList.forEach { e ->
                FilterChip(
                    selected = selectedEquip == e,
                    onClick = { selectedEquip = if (selectedEquip == e) null else e },
                    label = { Text(e) },
                    shape = CircleShape,
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = V.SurfaceRaised, labelColor = V.Gray,
                        selectedContainerColor = V.Teal, selectedLabelColor = V.Black,
                    ),
                )
            }
        }

        Text("${results.size} exercises", style = MaterialTheme.typography.labelMedium, color = V.Gray, modifier = Modifier.padding(horizontal = AuraScreenInset, vertical = 4.dp))

        // Results
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { ex ->
                VCard(
                    Modifier.padding(horizontal = AuraScreenInset),
                    onClick = {
                        nav.previousBackStackEntry?.savedStateHandle?.set("selectedExerciseId", ex.id)
                        nav.popBackStack()
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(V.Teal.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.FitnessCenter, null, tint = V.Teal, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.titleMedium, color = V.White)
                            Text("${ex.primaryMuscles.joinToString()} · ${ex.equipment}", style = MaterialTheme.typography.bodySmall, color = V.Gray)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
