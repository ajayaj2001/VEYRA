package com.aura.ui.screens.nutrition

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.withTransaction
import com.aura.AuraApp
import com.aura.R
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
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

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
                nutrientsJson = food.nutrientsJson,
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

    Column(Modifier.fillMaxSize().background(V.Canvas).statusBarsPadding()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = AuraScreenInset, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nutrition", style = MaterialTheme.typography.headlineLarge, color = V.White)
                    NutritionDayStrip(state.date, vm::changeDate)
                }
            }
            item { CalorieCard(state) }
            item { MacroSummary(state) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Food log", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, color = V.White)
                    if (state.logs.isNotEmpty()) {
                        val count = state.logs.size
                        Text(if (count == 1) "1 entry" else "$count entries", style = MaterialTheme.typography.labelMedium, color = V.Gray)
                    }
                }
            }
            if (state.logs.isEmpty()) item {
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        MealIcon(Icons.Rounded.Restaurant, V.Amber)
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("No food logged", style = MaterialTheme.typography.titleLarge, color = V.White)
                            Text("Use Add meal to start your log.", style = MaterialTheme.typography.bodySmall, color = V.Gray)
                        }
                    }
                }
            }
            val mealLabels = mealTypes.toMap()
            val logsByMeal = state.logs.groupBy { it.mealType }
            val mealOrder = (mealTypes.map { it.first } + logsByMeal.keys).distinct()
            mealOrder.forEach { type ->
                val logs = logsByMeal[type].orEmpty()
                if (logs.isNotEmpty()) item(key = "meal-$type") {
                    MealGroup(type, mealLabels[type] ?: "Other", logs) { selected = it }
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
private fun NutritionDayStrip(date: LocalDate, onSelect: (LocalDate) -> Unit) {
    val locale = Locale.getDefault()
    val start = date.with(TemporalAdjusters.previousOrSame(WeekFields.of(locale).firstDayOfWeek))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSelect(date.minusDays(1)) }) {
                Icon(Icons.Rounded.ChevronLeft, "Previous day", tint = V.Gray)
            }
            Text(nutritionDate(date), Modifier.weight(1f), color = V.White,
                style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { onSelect(date.plusDays(1)) }) {
                Icon(Icons.Rounded.ChevronRight, "Next day", tint = V.Gray)
            }
        }
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(7) { offset ->
                val day = start.plusDays(offset.toLong())
                val selected = day == date
                Column(
                    Modifier.weight(1f).heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (selected) V.TealDim else V.Surface)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(day) })
                        .semantics { contentDescription = day.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale)) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall, color = if (selected) V.ButtonText else V.Gray)
                    Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium,
                        color = if (selected) V.ButtonText else V.White)
                }
            }
        }
    }
}

private val CalorieSurface = Color(0xFF10201B)
private val NutritionAmber = Color(0xFFFAC580)

@Composable
private fun CalorieCard(state: NutritionVM.State) {
    val balance = state.goal.calories - state.calories
    val color = if (balance < 0) V.Red else NutritionAmber
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CalorieSurface)) {
        Image(
            painterResource(R.drawable.nutrition_citrus),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
        )
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to CalorieSurface,
            0.46f to CalorieSurface.copy(alpha = 0.97f),
            0.78f to CalorieSurface.copy(alpha = 0.35f),
            1f to CalorieSurface.copy(alpha = 0.12f),
        )))
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (balance < 0) "Above calorie goal" else "Calories remaining",
                    color = V.White, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(nutritionNumber(kotlin.math.abs(balance)), color = color,
                        style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("kcal", Modifier.padding(bottom = 3.dp), style = MaterialTheme.typography.labelMedium, color = V.White)
                }
                Text("${nutritionNumber(state.calories)} of ${nutritionNumber(state.goal.calories)} kcal logged",
                    color = V.White, style = MaterialTheme.typography.labelMedium)
            }
            LinearProgressIndicator(
                progress = { nutritionProgress(state.calories, state.goal.calories) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = color,
                trackColor = V.SurfaceBright,
            )
        }
    }
}

@Composable
private fun MacroSummary(state: NutritionVM.State) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroTile("Protein", state.protein, state.goal.protein, V.Teal, Modifier.weight(1f))
        MacroTile("Carbs", state.carbs, state.goal.carbs, NutritionAmber, Modifier.weight(1f))
        MacroTile("Fat", state.fat, state.goal.fat, V.Purple, Modifier.weight(1f))
    }
}

@Composable
private fun MacroTile(label: String, current: Double, target: Double, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(V.SurfaceRaised).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Text("${nutritionNumber(current)} g", style = MaterialTheme.typography.titleLarge, color = V.White)
        LinearProgressIndicator(
            progress = { nutritionProgress(current, target) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = color, trackColor = V.SurfaceBright,
        )
        Text("${nutritionNumber(target)} g goal", style = MaterialTheme.typography.labelSmall, color = V.Gray)
    }
}

private fun nutritionProgress(current: Double, goal: Double): Float =
    (current / goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

@Composable
private fun MealGroup(type: String, label: String, logs: List<FoodLogEntity>, onSelect: (FoodLogEntity) -> Unit) {
    val (icon, color) = when (type) {
        "breakfast" -> Icons.Rounded.WbSunny to NutritionAmber
        "lunch" -> Icons.Rounded.LunchDining to V.Teal
        "dinner" -> Icons.Rounded.NightsStay to V.Purple
        else -> Icons.Rounded.Restaurant to NutritionAmber
    }
    VCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MealIcon(icon, color)
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = V.White)
                Text("${nutritionNumber(logs.sumOf { it.calories })} kcal",
                    style = MaterialTheme.typography.labelMedium, color = color)
            }
            logs.forEach { log ->
                HorizontalDivider(color = V.Divider)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClickLabel = "View food details") { onSelect(log) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(log.foodName, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = V.White)
                    Text("${nutritionNumber(log.calories)} kcal", style = MaterialTheme.typography.labelMedium, color = V.Gray)
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = V.Gray)
                }
            }
        }
    }
}

@Composable
private fun MealIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color)
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
