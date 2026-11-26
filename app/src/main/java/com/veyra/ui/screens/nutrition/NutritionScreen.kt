package com.veyra.ui.screens.nutrition

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veyra.ui.components.AuraTextField
import com.veyra.VeyraApp
import com.veyra.data.local.entity.*
import com.veyra.ui.components.VCard
import com.veyra.ui.theme.AuraScreenInset
import com.veyra.ui.theme.V
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NutritionVM(app: Application) : AndroidViewModel(app) {
    private val dao = (app as VeyraApp).db.nutritionDao()
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    data class State(
        val date: LocalDate = LocalDate.now(),
        val goal: NutritionGoalEntity = NutritionGoalEntity(),
        val logs: List<FoodLogEntity> = emptyList(),
        val cal: Double = 0.0, val pro: Double = 0.0, val carb: Double = 0.0, val fat: Double = 0.0,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        viewModelScope.launch { dao.getGoal().collect { _state.value = _state.value.copy(goal = it ?: NutritionGoalEntity()) } }
        loadLogs()
    }

    private fun loadLogs() = viewModelScope.launch {
        val dateStr = _state.value.date.format(fmt)
        dao.getLogsForDate(dateStr).collect { logs ->
            _state.value = _state.value.copy(
                logs = logs,
                cal = logs.sumOf { it.calories },
                pro = logs.sumOf { it.protein },
                carb = logs.sumOf { it.carbs },
                fat = logs.sumOf { it.fat },
            )
        }
    }

    fun changeDate(d: LocalDate) { _state.value = _state.value.copy(date = d); loadLogs() }

    fun addQuickFood(name: String, cal: Double, pro: Double, carb: Double, fat: Double, meal: String) = viewModelScope.launch {
        val foodId = dao.insertFood(FoodItemEntity(name = name, calories = cal, protein = pro, carbs = carb, fat = fat))
        dao.insertLog(FoodLogEntity(
            foodItemId = foodId, foodName = name, mealType = meal,
            calories = cal, protein = pro, carbs = carb, fat = fat,
            date = _state.value.date.format(fmt),
        ))
    }

    fun deleteLog(log: FoodLogEntity) = viewModelScope.launch { dao.deleteLog(log) }
}

@Composable
fun NutritionScreen(nav: NavController, vm: NutritionVM = viewModel()) {
    val s by vm.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var addMealType by remember { mutableStateOf("breakfast") }

    LazyColumn(
        Modifier.fillMaxSize().background(V.Black).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Nutrition", style = MaterialTheme.typography.headlineLarge, color = V.White,
                modifier = Modifier.padding(horizontal = AuraScreenInset, vertical = 16.dp))
        }

        // Date selector
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.changeDate(s.date.minusDays(1)) }) {
                    Icon(Icons.Rounded.ChevronLeft, "Prev", tint = V.Gray)
                }
                Text(
                    if (s.date == LocalDate.now()) "Today" else s.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                    style = MaterialTheme.typography.titleLarge, color = V.White, fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { vm.changeDate(s.date.plusDays(1)) }) {
                    Icon(Icons.Rounded.ChevronRight, "Next", tint = V.Gray)
                }
            }
        }

        // Calorie ring + macros
        item {
            VCard(Modifier.padding(horizontal = AuraScreenInset)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CalRing(s.cal, s.goal.calories, Modifier.size(100.dp))
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MacroBar("Protein", s.pro, s.goal.protein, V.Teal)
                        MacroBar("Carbs", s.carb, s.goal.carbs, V.Amber)
                        MacroBar("Fat", s.fat, s.goal.fat, V.Red)
                    }
                }
            }
        }

        // Meal sections
        val meals = listOf("breakfast" to "Breakfast", "lunch" to "Lunch", "dinner" to "Dinner", "snack" to "Snacks")
        meals.forEach { (type, label) ->
            val mealLogs = s.logs.filter { it.mealType == type }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.titleLarge, color = V.White, modifier = Modifier.weight(1f))
                    Text("${mealLogs.sumOf { it.calories }.toInt()} kcal", style = MaterialTheme.typography.labelMedium, color = V.Teal)
                }
            }

            if (mealLogs.isEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = AuraScreenInset).clip(RoundedCornerShape(16.dp))
                            .background(V.SurfaceRaised).clickable { addMealType = type; showAddDialog = true }.padding(16.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = V.Gray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add food", style = MaterialTheme.typography.bodyMedium, color = V.Gray)
                    }
                }
            }

            items(mealLogs) { log ->
                VCard(Modifier.padding(horizontal = AuraScreenInset)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(log.foodName, style = MaterialTheme.typography.titleMedium, color = V.White)
                            Text("P:${log.protein.toInt()}g  C:${log.carbs.toInt()}g  F:${log.fat.toInt()}g",
                                style = MaterialTheme.typography.labelSmall, color = V.Gray)
                        }
                        Text("${log.calories.toInt()} kcal", style = MaterialTheme.typography.labelMedium, color = V.Gray)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { vm.deleteLog(log) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Delete, "Delete", tint = V.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }

    // Quick add dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var cal by remember { mutableStateOf("") }
        var pro by remember { mutableStateOf("") }
        var carb by remember { mutableStateOf("") }
        var fat by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false }, containerColor = V.Black,
            modifier = Modifier.border(1.dp, V.SurfaceBright, RoundedCornerShape(28.dp)),
            title = { Text("Add Food", color = V.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniField(name, { name = it }, "Food name")
                    MiniField(cal, { cal = it }, "Calories")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniField(pro, { pro = it }, "Protein", Modifier.weight(1f))
                        MiniField(carb, { carb = it }, "Carbs", Modifier.weight(1f))
                        MiniField(fat, { fat = it }, "Fat", Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.addQuickFood(name, cal.toDoubleOrNull() ?: 0.0, pro.toDoubleOrNull() ?: 0.0,
                            carb.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0, addMealType)
                        showAddDialog = false
                    }
                }) { Text("Add", color = V.Teal) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = V.Gray) } },
        )
    }
}

@Composable
private fun MiniField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    AuraTextField(value, onChange, placeholder, modifier)
}

@Composable
private fun CalRing(consumed: Double, goal: Double, modifier: Modifier) {
    val prog by animateFloatAsState((consumed / goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.5f), tween(1000), label = "cal")
    val left = (goal - consumed).coerceAtLeast(0.0)
    val color = if (consumed > goal) V.Red else V.Amber

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 10.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(V.SurfaceBright, -90f, 360f, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * prog.coerceAtMost(1f), false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${left.toInt()}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = color)
            Text("kcal left", style = MaterialTheme.typography.labelSmall, color = V.Gray)
        }
    }
}

@Composable
private fun MacroBar(label: String, current: Double, goal: Double, color: Color) {
    val prog by animateFloatAsState((current / goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f), tween(800), label = "macro")
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = V.Gray)
            Text("${current.toInt()} / ${goal.toInt()}g", style = MaterialTheme.typography.labelSmall, color = V.Gray)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(V.SurfaceBright)) {
            Box(Modifier.fillMaxWidth(prog).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}
