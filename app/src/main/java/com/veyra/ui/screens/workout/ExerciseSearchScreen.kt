package com.veyra.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veyra.data.model.Equipment
import com.veyra.data.model.Exercise
import com.veyra.data.model.ExerciseRepository
import com.veyra.data.model.MuscleGroup
import com.veyra.ui.components.GlassCard
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens

@Composable
fun ExerciseSearchScreen(
    navController: NavController,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedEquipment by remember { mutableStateOf<Equipment?>(null) }

    val exercises = remember(searchQuery, selectedMuscleGroup, selectedEquipment) {
        var list = if (searchQuery.isNotBlank()) {
            ExerciseRepository.search(searchQuery)
        } else {
            ExerciseRepository.exercises
        }
        selectedMuscleGroup?.let { group ->
            list = list.filter { ex ->
                ex.primaryMuscles.any { it.group == group } ||
                    ex.secondaryMuscles.any { it.group == group }
            }
        }
        selectedEquipment?.let { equip ->
            list = list.filter { it.equipment == equip }
        }
        list
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
                text = "Add Exercise",
                style = MaterialTheme.typography.headlineSmall,
                color = VeyraColors.TextPrimary,
            )
        }

        // Search field
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VeyraDimens.SpaceLg),
            placeholder = { Text("Search exercises...") },
            leadingIcon = {
                Icon(Icons.Rounded.Search, null, tint = VeyraColors.TextTertiary)
            },
            singleLine = true,
            shape = RoundedCornerShape(VeyraDimens.RadiusLg),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = VeyraColors.SurfaceElevated,
                unfocusedContainerColor = VeyraColors.SurfaceElevated,
                focusedTextColor = VeyraColors.TextPrimary,
                unfocusedTextColor = VeyraColors.TextPrimary,
                cursorColor = VeyraColors.Accent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )

        Spacer(Modifier.height(VeyraDimens.SpaceSm))

        // Muscle group filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = VeyraDimens.SpaceLg),
            horizontalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceSm),
        ) {
            MuscleGroup.entries.forEach { group ->
                FilterChip(
                    selected = selectedMuscleGroup == group,
                    onClick = {
                        selectedMuscleGroup = if (selectedMuscleGroup == group) null else group
                    },
                    label = { Text(group.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = VeyraColors.SurfaceElevated,
                        labelColor = VeyraColors.TextSecondary,
                        selectedContainerColor = VeyraColors.PrimaryStart.copy(alpha = 0.2f),
                        selectedLabelColor = VeyraColors.PrimaryStart,
                    ),
                )
            }
        }

        // Equipment filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = VeyraDimens.SpaceLg, vertical = VeyraDimens.SpaceXs),
            horizontalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceSm),
        ) {
            Equipment.entries.forEach { equip ->
                FilterChip(
                    selected = selectedEquipment == equip,
                    onClick = {
                        selectedEquipment = if (selectedEquipment == equip) null else equip
                    },
                    label = { Text(equip.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = VeyraColors.SurfaceElevated,
                        labelColor = VeyraColors.TextSecondary,
                        selectedContainerColor = VeyraColors.Accent.copy(alpha = 0.2f),
                        selectedLabelColor = VeyraColors.Accent,
                    ),
                )
            }
        }

        Spacer(Modifier.height(VeyraDimens.SpaceSm))

        Text(
            text = "${exercises.size} exercises",
            style = MaterialTheme.typography.labelMedium,
            color = VeyraColors.TextTertiary,
            modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
        )

        // Exercise list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceSm),
        ) {
            items(exercises) { exercise ->
                ExerciseListItem(
                    exercise = exercise,
                    onClick = {
                        // Return to active workout with selected exercise
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedExerciseId", exercise.id)
                        navController.popBackStack()
                    },
                )
            }
            item { Spacer(Modifier.height(VeyraDimens.SpaceXxl)) }
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: Exercise,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.FitnessCenter,
                contentDescription = null,
                tint = VeyraColors.Accent,
                modifier = Modifier.size(VeyraDimens.IconLg),
            )
            Spacer(Modifier.width(VeyraDimens.SpaceMd))
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = VeyraColors.TextPrimary,
                )
                Text(
                    text = "${exercise.primaryMuscles.joinToString { it.displayName }} · ${exercise.equipment.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeyraColors.TextTertiary,
                )
            }
        }
    }
}
