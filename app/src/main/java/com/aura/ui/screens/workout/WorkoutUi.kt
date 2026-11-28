package com.aura.ui.screens.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.navigation.NavController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

internal fun openExercisePicker(nav: NavController, existingIds: List<String>) {
    nav.currentBackStackEntry?.savedStateHandle?.set("existingExerciseIds", ArrayList(existingIds))
    nav.navigate("exercise/search")
}

@Composable
internal fun WorkoutHeader(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 10.dp, end = AuraScreenInset, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = V.Gray)
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
            color = V.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
    HorizontalDivider(color = V.SurfaceRaised)
}

@Composable
internal fun WorkoutAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (prominent) 56.dp else 48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = V.SurfaceRaised, contentColor = V.White,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = V.Teal)
        Spacer(Modifier.width(8.dp))
        Text(label, style = if (prominent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
internal fun ExerciseGlyph(size: Dp = 44.dp) {
    Box(
        Modifier.size(size).background(V.Teal.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.FitnessCenter, null, Modifier.size(size * 0.48f), tint = V.Teal)
    }
}

@Composable
internal fun ExerciseThumbnail(imageUrl: String?, size: Dp = 52.dp) {
    if (imageUrl.isNullOrBlank()) {
        ExerciseGlyph(size)
        return
    }
    val fallback = rememberVectorPainter(Icons.Rounded.FitnessCenter)
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(V.White),
        contentScale = ContentScale.Crop,
        placeholder = fallback,
        error = fallback,
    )
}

@Composable
internal fun WorkoutMetricFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = CircleShape,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = V.Canvas,
            labelColor = V.Gray,
            selectedContainerColor = V.Teal,
            selectedLabelColor = V.Black,
        ),
    )
}

@Composable
internal fun ProgressChart(
    points: List<Pair<Long, Double>>,
    unit: String,
    emptyMessage: String,
    showLatestValue: Boolean = true,
) {
    if (points.isEmpty()) {
        Text(emptyMessage, style = MaterialTheme.typography.bodyMedium, color = V.Gray)
        return
    }

    val max = points.maxOf { it.second }.coerceAtLeast(1.0)
    if (showLatestValue) {
        Text("${workoutNumber(points.last().second)} $unit", style = MaterialTheme.typography.headlineMedium, color = V.White)
    }
    Canvas(Modifier.fillMaxWidth().height(112.dp).padding(top = 12.dp, bottom = 8.dp)) {
        repeat(3) { index ->
            val y = size.height * index / 2
            drawLine(V.SurfaceBright, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val top = 6.dp.toPx()
        val width = size.width - 2 * top
        val first = points.first().first
        val interval = (points.last().first - first).coerceAtLeast(1L).toDouble()
        val positions = points.map { (time, value) ->
            val x = if (points.size == 1) size.width / 2 else top + width * ((time - first) / interval).toFloat()
            Offset(x, (size.height - top) - (size.height - 2 * top) * (value / max).toFloat())
        }
        positions.zipWithNext().forEach { (a, b) -> drawLine(V.Teal, a, b, 2.dp.toPx()) }
        positions.forEach { drawCircle(V.Teal, 3.dp.toPx(), it) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(workoutDate(points.first().first), style = MaterialTheme.typography.labelSmall, color = V.Gray)
        if (points.size > 1) {
            Text(workoutDate(points.last().first), style = MaterialTheme.typography.labelSmall, color = V.Gray)
        }
    }
}

internal fun workoutNumber(value: Double): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 2
}.format(value)

internal fun workoutDate(time: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(time))
