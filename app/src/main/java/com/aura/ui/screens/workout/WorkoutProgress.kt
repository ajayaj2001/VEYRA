package com.aura.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.data.local.dao.WorkoutDao
import com.aura.data.local.entity.WorkoutEntity
import com.aura.data.local.entity.WorkoutSetEntity
import com.aura.ui.theme.V

internal suspend fun WorkoutDao.previousExerciseSets(
    workout: WorkoutEntity,
    exerciseId: String,
): Map<Int, WorkoutSetEntity> =
    getPreviousExerciseSets(exerciseId, workout.id, workout.startTime, workout.routineId)
        .associateBy { it.setNumber }

internal data class SetWeightGain(val previous: WorkoutSetEntity, val current: WorkoutSetEntity) {
    val increaseKg get() = current.weight - previous.weight
}

internal fun weightGain(current: WorkoutSetEntity, previous: WorkoutSetEntity?): SetWeightGain? {
    if (previous == null || !current.isCompleted || current.reps <= 0) return null
    val gain = SetWeightGain(previous, current)
    // Unit conversions can introduce rounding noise below the precision shown in the log.
    return gain.takeIf { it.increaseKg.isFinite() && it.increaseKg >= 0.009999999 }
}

@Composable
internal fun WeightGainBadge(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.background(V.Teal.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(16.dp), tint = V.Teal)
        Text(text, style = MaterialTheme.typography.labelMedium, color = V.Teal)
    }
}
