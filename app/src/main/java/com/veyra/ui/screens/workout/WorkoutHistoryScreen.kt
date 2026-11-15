package com.veyra.ui.screens.workout

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veyra.ui.components.GlassCard
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutHistoryScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.Background)
            .statusBarsPadding(),
    ) {
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
                text = "Workout History",
                style = MaterialTheme.typography.headlineSmall,
                color = VeyraColors.TextPrimary,
            )
        }

        if (state.recentWorkouts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = VeyraColors.TextTertiary,
                        modifier = Modifier.size(VeyraDimens.IconHuge),
                    )
                    Spacer(Modifier.height(VeyraDimens.SpaceMd))
                    Text("No workouts yet", style = MaterialTheme.typography.titleMedium, color = VeyraColors.TextSecondary)
                    Text("Start your first workout!", style = MaterialTheme.typography.bodySmall, color = VeyraColors.TextTertiary)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceSm),
            ) {
                items(state.recentWorkouts) { workout ->
                    GlassCard(
                        modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg),
                    ) {
                        Column {
                            Text(
                                text = workout.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = VeyraColors.TextPrimary,
                            )
                            Spacer(Modifier.height(VeyraDimens.SpaceXs))
                            val sdf = SimpleDateFormat("EEEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                            Text(
                                text = sdf.format(Date(workout.startTime)),
                                style = MaterialTheme.typography.bodySmall,
                                color = VeyraColors.TextTertiary,
                            )
                            Spacer(Modifier.height(VeyraDimens.SpaceSm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text("Duration", style = MaterialTheme.typography.labelSmall, color = VeyraColors.TextTertiary)
                                    val h = workout.durationSeconds / 3600
                                    val m = (workout.durationSeconds % 3600) / 60
                                    Text(
                                        if (h > 0) "${h}h ${m}m" else "${m}m",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = VeyraColors.Green,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Volume", style = MaterialTheme.typography.labelSmall, color = VeyraColors.TextTertiary)
                                    Text(
                                        "%.0f kg".format(workout.totalVolume),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = VeyraColors.Green,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(VeyraDimens.SpaceXxl)) }
            }
        }
    }
}
