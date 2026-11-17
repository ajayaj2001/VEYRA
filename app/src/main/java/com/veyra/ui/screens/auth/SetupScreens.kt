package com.veyra.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veyra.ui.components.StepProgress
import com.veyra.ui.components.VButton
import com.veyra.ui.theme.V

// ─── Shared input field ───
@Composable
private fun VField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = V.GrayDark) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = V.Surface,
            unfocusedContainerColor = V.Surface,
            focusedTextColor = V.White,
            unfocusedTextColor = V.White,
            cursorColor = V.Teal,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

// ─── Shared chip selector ───
@Composable
private fun ChipRow(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(if (sel) V.Teal else V.Surface)
                    .then(
                        if (!sel) Modifier.border(1.dp, V.SurfaceBright, RoundedCornerShape(9999.dp))
                        else Modifier
                    )
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (sel) V.White else V.White,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

// ─── Shared header ───
@Composable
private fun SetupHeader(step: Int, onBack: () -> Unit, onSkip: (() -> Unit)? = null) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = V.White)
            }
            Text(
                "Set up your profile",
                style = MaterialTheme.typography.titleLarge,
                color = V.White,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            if (onSkip != null) {
                Text(
                    "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = V.Gray,
                    modifier = Modifier
                        .clickable(onClick = onSkip)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        StepProgress(current = step, total = 3)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 1: BASIC INFO
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SetupBasicScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableIntStateOf(-1) } // -1 = none selected

    Column(
        Modifier
            .fillMaxSize()
            .background(V.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SetupHeader(step = 1, onBack = onBack, onSkip = onSkip)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
        ) {
            Text(
                "Let's get to know you",
                style = MaterialTheme.typography.headlineMedium,
                color = V.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "This helps us personalize your experience",
                style = MaterialTheme.typography.bodyMedium,
                color = V.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(36.dp))

            Text("Name", style = MaterialTheme.typography.labelMedium, color = V.Gray)
            Spacer(Modifier.height(8.dp))
            VField(name, { name = it }, "Your name")

            Spacer(Modifier.height(20.dp))

            Text("Date of Birth", style = MaterialTheme.typography.labelMedium, color = V.Gray)
            Spacer(Modifier.height(8.dp))
            VField(dob, { dob = it }, "DD / MM / YYYY", keyboardType = KeyboardType.Number)

            Spacer(Modifier.height(20.dp))

            Text("Gender", style = MaterialTheme.typography.labelMedium, color = V.Gray)
            Spacer(Modifier.height(10.dp))
            ChipRow(listOf("Male", "Female", "Other"), gender, { gender = it })

            Spacer(Modifier.height(20.dp))
        }

        // Bottom
        Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            VButton("Continue", onClick = onContinue)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 2: BODY METRICS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SetupBodyScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }
    var heightUnit by remember { mutableIntStateOf(0) }
    var weightUnit by remember { mutableIntStateOf(0) }
    var activityLevel by remember { mutableIntStateOf(2) }

    data class ActivityOption(
        val label: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val iconColor: Color,
    )

    val activities = listOf(
        ActivityOption("Sedentary", Icons.Rounded.SelfImprovement, V.Teal),
        ActivityOption("Light", Icons.Rounded.DirectionsWalk, V.Green),
        ActivityOption("Moderate", Icons.Rounded.DirectionsRun, V.Amber),
        ActivityOption("Intense", Icons.Rounded.LocalFireDepartment, V.Red),
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(V.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SetupHeader(step = 2, onBack = onBack, onSkip = onSkip)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
        ) {
            Text(
                "Your body metrics",
                style = MaterialTheme.typography.headlineMedium,
                color = V.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Used for accurate calorie and recovery calculations",
                style = MaterialTheme.typography.bodyMedium,
                color = V.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))

            // Height
            Text("Height", style = MaterialTheme.typography.labelMedium, color = V.Gray)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VField(height, { height = it }, "175", Modifier.weight(1f), KeyboardType.Number)
                ChipRow(listOf("cm", "ft"), heightUnit, { heightUnit = it }, Modifier.weight(0.6f))
            }

            Spacer(Modifier.height(20.dp))

            // Weight
            Text("Weight", style = MaterialTheme.typography.labelMedium, color = V.Gray)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VField(weight, { weight = it }, "70", Modifier.weight(1f), KeyboardType.Decimal)
                ChipRow(listOf("kg", "lbs"), weightUnit, { weightUnit = it }, Modifier.weight(0.6f))
            }

            Spacer(Modifier.height(20.dp))

            // Target weight
            Text("Target Weight", style = MaterialTheme.typography.labelMedium, color = V.Gray)
            Spacer(Modifier.height(8.dp))
            VField(targetWeight, { targetWeight = it }, "65", keyboardType = KeyboardType.Decimal)
            Text(
                "Optional \u2014 helps set nutrition goals",
                style = MaterialTheme.typography.labelSmall,
                color = V.GrayDark,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(24.dp))

            // Activity level — compact icon chips
            Text("Activity Level", style = MaterialTheme.typography.labelMedium, color = V.Gray)
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                activities.forEachIndexed { i, option ->
                    val sel = i == activityLevel
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) V.Teal.copy(alpha = 0.1f) else V.Surface)
                            .then(
                                if (sel) Modifier.border(1.5.dp, V.Teal, RoundedCornerShape(16.dp))
                                else Modifier.border(1.dp, V.SurfaceBright.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            )
                            .clickable { activityLevel = i }
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(option.iconColor.copy(alpha = if (sel) 0.2f else 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(option.icon, null, tint = option.iconColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sel) V.White else V.Gray,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            VButton("Continue", onClick = onContinue)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 3: GOALS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private data class GoalItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
)

private val goals = listOf(
    GoalItem("Lose Weight", Icons.Rounded.MonitorWeight, V.Red),
    GoalItem("Build Muscle", Icons.Rounded.FitnessCenter, V.Teal),
    GoalItem("Get Fitter", Icons.Rounded.Favorite, V.Green),
    GoalItem("Eat Better", Icons.Rounded.Restaurant, V.Amber),
    GoalItem("Sleep Better", Icons.Rounded.Bedtime, V.Blue),
    GoalItem("Less Stress", Icons.Rounded.SelfImprovement, V.Gray),
)

@Composable
fun SetupGoalsScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
) {
    var selected by remember { mutableStateOf(setOf(1, 2)) } // Build Muscle + Get Fitter
    var reminders by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(V.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SetupHeader(step = 3, onBack = onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp),
        ) {
            Text(
                "What are your goals?",
                style = MaterialTheme.typography.headlineMedium,
                color = V.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select all that apply",
                style = MaterialTheme.typography.bodyMedium,
                color = V.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            // 3-column visual grid
            val rows = goals.chunked(3)
            rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { goal ->
                        val idx = goals.indexOf(goal)
                        val sel = idx in selected
                        Box(
                            Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (sel) goal.color.copy(alpha = 0.08f) else V.Surface)
                                .then(
                                    if (sel) Modifier.border(2.dp, goal.color, RoundedCornerShape(18.dp))
                                    else Modifier.border(1.dp, V.SurfaceBright.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                                )
                                .clickable {
                                    selected = if (sel) selected - idx else selected + idx
                                },
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                // Icon with double-ring glow effect
                                Box(contentAlignment = Alignment.Center) {
                                    // Outer glow ring
                                    if (sel) {
                                        Box(
                                            Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(goal.color.copy(alpha = 0.08f))
                                        )
                                    }
                                    // Icon circle
                                    Box(
                                        Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(goal.color.copy(alpha = if (sel) 0.2f else 0.1f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            goal.icon, null,
                                            tint = if (sel) goal.color else goal.color.copy(alpha = 0.7f),
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    goal.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (sel) V.White else V.Gray,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                            // Checkmark badge
                            if (sel) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(goal.color),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Check, null, tint = V.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Daily reminders toggle
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(V.Surface)
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { reminders = !reminders }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Daily reminders", style = MaterialTheme.typography.titleLarge, color = V.White)
                    Text(
                        "Get nudges to log meals, workouts, and check-ins",
                        style = MaterialTheme.typography.bodySmall,
                        color = V.GrayDark,
                    )
                }
                // Simple toggle visual
                Box(
                    Modifier
                        .size(width = 48.dp, height = 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (reminders) V.Teal else V.SurfaceBright),
                    contentAlignment = if (reminders) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(
                        Modifier
                            .padding(2.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(V.White)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            VButton("Get Started", onClick = onComplete, enabled = selected.isNotEmpty())
        }
    }
}
