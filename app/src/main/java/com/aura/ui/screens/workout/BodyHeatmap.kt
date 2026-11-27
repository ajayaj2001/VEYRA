package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aura.data.model.Exercise
import com.aura.data.model.muscleIntensity
import com.aura.ui.components.VCard
import com.aura.ui.theme.V

private const val FOLDER = "body_heatmap"

/**
 * Front and back body figures with the muscles [exercises] work highlighted: strongly for primary
 * muscles, faintly for secondary ones. Shows nothing when the images are not bundled (they are kept
 * out of the repository) or when no exercise maps to a body area.
 */
@Composable
internal fun MusclesWorkedCard(exercises: List<Exercise>) {
    val context = LocalContext.current
    val images = remember { context.assets.list(FOLDER)?.toSet().orEmpty() }
    val intensity = remember(exercises) { muscleIntensity(exercises) }
    val sides = listOf("front", "back").filter { "${it}_base.png" in images }
    val drawable = intensity.keys.filter { muscle -> sides.any { "${it}_$muscle.png" in images } }
    if (sides.isEmpty() || drawable.isEmpty()) return

    VCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Muscles worked", style = MaterialTheme.typography.titleLarge, color = V.White)
            // The figures are drawn for a light background.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(V.White).padding(16.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Body diagram highlighting ${drawable.joinToString { it.replace('_', ' ') }}"
                    },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                sides.forEach { side ->
                    // Every layer is 352×858, so they line up when stacked.
                    Box(Modifier.weight(1f).aspectRatio(352f / 858f)) {
                        AsyncImage("file:///android_asset/$FOLDER/${side}_base.png", null, Modifier.fillMaxSize())
                        intensity.forEach { (muscle, strength) ->
                            if ("${side}_$muscle.png" in images) AsyncImage(
                                "file:///android_asset/$FOLDER/${side}_$muscle.png", null,
                                Modifier.fillMaxSize(), alpha = strength,
                            )
                        }
                    }
                }
            }
        }
    }
}
