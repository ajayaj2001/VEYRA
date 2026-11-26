package com.aura.ui.screens.nutrition

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.withTransaction
import com.aura.AuraApp
import com.aura.data.local.entity.*
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val mealTypes = listOf("breakfast" to "Breakfast", "lunch" to "Lunch", "dinner" to "Dinner", "snack" to "Snack")

internal fun nutritionNumber(value: Double): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 1
}.format(value)

internal fun nutritionDate(date: LocalDate): String =
    if (date == LocalDate.now()) "Today, ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
    else date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))

class NutritionVM(app: Application) : AndroidViewModel(app) {
    private val database = (app as AuraApp).db
    private val dao = database.nutritionDao()
    val favorites = dao.getFavorites()

    data class State(
        val date: LocalDate = LocalDate.now(),
        val goal: NutritionGoalEntity = NutritionGoalEntity(),
        val logs: List<FoodLogEntity> = emptyList(),
    ) {
        val calories get() = logs.sumOf { it.calories }
        val protein get() = logs.sumOf { it.protein }
        val carbs get() = logs.sumOf { it.carbs }
        val fat get() = logs.sumOf { it.fat }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    private var dateJob: Job? = null

    init {
        viewModelScope.launch {
            dao.getGoal().collect { goal -> _state.update { it.copy(goal = goal ?: NutritionGoalEntity()) } }
        }
        changeDate(_state.value.date)
    }

    fun changeDate(date: LocalDate) {
        dateJob?.cancel()
        _state.update { it.copy(date = date, logs = emptyList()) }
        dateJob = viewModelScope.launch {
            dao.getLogsForDate(date.toString()).collect { logs ->
                _state.update { state -> if (state.date == date) state.copy(logs = logs) else state }
            }
        }
    }

    internal suspend fun logFood(food: FoodItemEntity, meal: String, date: LocalDate, favorite: Boolean) {
        database.withTransaction {
            val existing = food.id.takeIf { it != 0L }?.let { dao.getFoodById(it) }
            val matchesExisting = existing?.copy(isFavorite = food.isFavorite) == food
            val id = if (existing != null && matchesExisting) {
                if (favorite != existing.isFavorite) dao.updateFood(existing.copy(isFavorite = favorite))
                existing.id
            } else {
                dao.insertFood(food.copy(id = 0L, isFavorite = favorite))
            }
            dao.insertLog(FoodLogEntity(
                foodItemId = id, foodName = food.name, mealType = meal, calories = food.calories,
                protein = food.protein, carbs = food.carbs, fat = food.fat, date = date.toString(),
            ))
        }
    }

    internal suspend fun setFavorite(food: FoodItemEntity, favorite: Boolean) {
        dao.getFoodById(food.id)?.let { dao.updateFood(it.copy(isFavorite = favorite)) }
    }

    internal suspend fun deleteLog(log: FoodLogEntity) = dao.deleteLog(log)
}

@Composable
fun NutritionScreen(nav: NavController, vm: NutritionVM = viewModel()) {
    val state by vm.state.collectAsState()
    val favorites by vm.favorites.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<FoodLogEntity?>(null) }

    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = AuraScreenInset, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Nutrition", style = MaterialTheme.typography.headlineLarge, color = V.White)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.changeDate(state.date.minusDays(1)) }) {
                        Icon(Icons.Rounded.ChevronLeft, "Previous day", tint = V.Gray)
                    }
                    Text(nutritionDate(state.date), style = MaterialTheme.typography.titleMedium, color = V.White)
                    IconButton(onClick = { vm.changeDate(state.date.plusDays(1)) }) {
                        Icon(Icons.Rounded.ChevronRight, "Next day", tint = V.Gray)
                    }
                }
            }
            item { DailyNutrition(state) }
            item {
                Text("Food log", style = MaterialTheme.typography.headlineMedium, color = V.White)
            }
            if (state.logs.isEmpty()) item {
                VCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Restaurant, null, tint = V.Amber)
                        Text("No meals logged", style = MaterialTheme.typography.titleLarge, color = V.White)
                        Text("Add a meal to track calories and macros.", style = MaterialTheme.typography.bodyMedium, color = V.Gray)
                    }
                }
            }
            mealTypes.forEach { (type, label) ->
                val logs = state.logs.filter { it.mealType == type }
                if (logs.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.titleMedium, color = V.White)
                            Text("${nutritionNumber(logs.sumOf { it.calories })} kcal",
                                style = MaterialTheme.typography.bodySmall, color = V.Amber)
                        }
                    }
                    items(logs, key = { it.id }) { log ->
                        VCard(onClick = { selected = log }) {
                            FoodListLabel(log.foodName, log.calories)
                        }
                    }
                }
            }
        }
        AuraPrimaryButton("Add meal", { nav.navigate("nutrition/entry") },
            Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp))
    }
    selected?.let { log ->
        LoggedFoodDetails(
            log,
            favorite = favorites.any { it.id == log.foodItemId },
            onFavorite = { favored ->
                vm.setFavorite(FoodItemEntity(id = log.foodItemId, name = log.foodName), favored)
            },
            onDelete = { vm.deleteLog(log); selected = null },
            onClose = { selected = null },
        )
    }
}

@Composable
private fun DailyNutrition(state: NutritionVM.State) {
    val balance = state.goal.calories - state.calories
    VCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (balance < 0) "Above calorie goal" else "Calories remaining", color = V.Gray,
                        style = MaterialTheme.typography.bodySmall)
                    Text("${nutritionNumber(kotlin.math.abs(balance))} kcal", color = if (balance < 0) V.Red else V.White,
                        style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("${nutritionNumber(state.calories)} of ${nutritionNumber(state.goal.calories)} kcal",
                        color = V.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Rounded.Restaurant, null, Modifier.size(34.dp), tint = V.Amber)
            }
            LinearProgressIndicator(
                progress = { (state.calories / state.goal.calories.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (balance < 0) V.Red else V.Amber,
                trackColor = V.SurfaceBright,
            )
            HorizontalDivider(color = V.SurfaceBright)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DailyMacro("Protein", state.protein, state.goal.protein, V.Teal, Modifier.weight(1f))
                DailyMacro("Carbs", state.carbs, state.goal.carbs, V.Amber, Modifier.weight(1f))
                DailyMacro("Fat", state.fat, state.goal.fat, V.Green, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DailyMacro(label: String, current: Double, target: Double, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        LinearProgressIndicator(
            progress = { (current / target.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(), color = color, trackColor = V.SurfaceBright,
        )
        Text("${nutritionNumber(current)} / ${nutritionNumber(target)} g",
            style = MaterialTheme.typography.bodySmall, color = V.White)
    }
}

@Composable
internal fun FoodListLabel(name: String, calories: Double, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Rounded.Restaurant, null, Modifier.size(22.dp), tint = V.Amber)
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = V.White)
        Text("${nutritionNumber(calories)} kcal", style = MaterialTheme.typography.labelMedium, color = V.Amber)
    }
}
