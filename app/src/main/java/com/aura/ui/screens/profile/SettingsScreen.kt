package com.aura.ui.screens.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.navigation.NavController
import com.aura.AppConfig
import com.aura.AuraApp
import com.aura.data.local.entity.NutritionGoalEntity
import com.aura.navigation.Routes
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.AuraTextField
import com.aura.ui.components.VCard
import com.aura.ui.screens.nutrition.foodQuantity
import com.aura.ui.screens.workout.WorkoutHeader
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
internal fun SettingsScreen(nav: NavController, onSignOut: () -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }

    fun openSystemSettings(intent: Intent, unavailable: String) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            error = unavailable
        } catch (_: SecurityException) {
            error = unavailable
        }
    }

    Column(Modifier.fillMaxSize().background(V.Canvas).statusBarsPadding().navigationBarsPadding()) {
        WorkoutHeader("Settings", { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AuraScreenInset),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VCard {
                Column(Modifier.fillMaxWidth()) {
                    SettingsRow("Nutrition goals", "Daily calories, protein, carbs, and fat") { nav.navigate(Routes.NUTRITION_GOALS) }
                    HorizontalDivider(color = V.Divider)
                    SettingsRow("AI food estimation", "Manage your Gemini API key") { nav.navigate(Routes.AI_SETTINGS) }
                    HorizontalDivider(color = V.Divider)
                    SettingsRow("Notifications", "Manage Aura notifications in Android") {
                        openSystemSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            "Notification settings are unavailable on this device.")
                    }
                    HorizontalDivider(color = V.Divider)
                    SettingsRow("Health Connect", "Manage health data permissions") {
                        openSystemSettings(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS),
                            "Health Connect is unavailable on this device.")
                    }
                }
            }
            VCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Aura", style = MaterialTheme.typography.titleLarge, color = V.White)
                    Text("Version ${AppConfig.APP_VERSION} · Mineral", color = V.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Sign out", color = V.Red)
            }
        }
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Settings") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun SettingsRow(title: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = V.White)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = V.Gray)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = V.Teal)
    }
}

@Composable
internal fun NutritionGoalsScreen(nav: NavController) {
    val dao = (LocalContext.current.applicationContext as AuraApp).db.nutritionDao()
    val scope = rememberCoroutineScope()
    val labels = listOf("Calories (kcal)", "Protein (g)", "Carbs (g)", "Fat (g)")
    var values by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    LaunchedEffect(dao) {
        dao.getGoal().collect { stored ->
            if (values.isEmpty()) {
                val goal = stored ?: NutritionGoalEntity()
                values = ArrayList(listOf(goal.calories, goal.protein, goal.carbs, goal.fat).map {
                    BigDecimal.valueOf(it).stripTrailingZeros().toPlainString()
                })
            }
        }
    }
    Column(Modifier.fillMaxSize().background(V.Canvas).statusBarsPadding().navigationBarsPadding().imePadding()) {
        WorkoutHeader("Nutrition goals", { if (!saving) nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AuraScreenInset),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (values.size == labels.size) {
                labels.forEachIndexed { index, label ->
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(label, color = V.White)
                        AuraTextField(values[index], { value ->
                            values = ArrayList(values).apply { this[index] = value }
                        }, label, keyboardType = KeyboardType.Decimal)
                    }
                }
                val parsed = values.map(::foodQuantity)
                val valid = parsed.all { it != null && it > 0 }
                if (!valid) Text("Enter a positive number for each goal.", color = V.Error)
                if (error) Text("Couldn’t save your goals. Try again.", color = V.Error)
                AuraPrimaryButton(if (saving) "Saving…" else "Save goals", onClick = {
                    if (!saving && valid) {
                        saving = true
                        error = false
                        scope.launch {
                            try {
                                val (calories, protein, carbs, fat) = parsed.filterNotNull()
                                dao.insertGoal(NutritionGoalEntity(calories = calories, protein = protein, carbs = carbs, fat = fat))
                                nav.popBackStack()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                error = true
                            } finally {
                                saving = false
                            }
                        }
                    }
                }, enabled = valid && !saving)
            }
        }
    }
}
