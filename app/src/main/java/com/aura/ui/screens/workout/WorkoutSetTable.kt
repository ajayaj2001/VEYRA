package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.V

internal data class SetTableRow(val number: Int, val weightKg: Double?, val reps: Int, val gainKg: Double? = null)

@Composable
internal fun WorkoutSetTable(rows: List<SetTableRow>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            listOf("Set", "Weight", "Reps").forEach {
                Text(it, Modifier.weight(1f), color = V.Gray, style = MaterialTheme.typography.labelMedium)
            }
        }
        rows.forEachIndexed { index, row ->
            Row(
                Modifier.fillMaxWidth().background(if (index % 2 == 0) V.SurfaceBright.copy(alpha = 0.38f) else V.SurfaceRaised)
                    .heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.number.toString(), color = V.White)
                    row.gainKg?.let {
                        Icon(Icons.Rounded.EmojiEvents, "+${workoutNumber(it)} kg compared with the previous set", Modifier.size(16.dp), tint = V.Amber)
                    }
                }
                Text(row.weightKg?.let { "${workoutNumber(it)} kg" } ?: "—", Modifier.weight(1f), color = V.White)
                Text(row.reps.toString(), Modifier.weight(1f), color = V.White)
            }
        }
    }
}

@Composable
internal fun WorkoutDetailChevron() {
    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = V.Teal)
}
