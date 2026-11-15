package com.veyra.ui.screens.nutrition

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veyra.VeyraApp
import com.veyra.data.local.entity.FoodItemEntity
import com.veyra.data.local.entity.FoodLogEntity
import com.veyra.data.local.entity.NutritionGoalEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class NutritionUiState(
    val date: LocalDate = LocalDate.now(),
    val goal: NutritionGoalEntity = NutritionGoalEntity(),
    val foodLogs: List<FoodLogWithItem> = emptyList(),
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val favorites: List<FoodItemEntity> = emptyList(),
    val mealType: String = "breakfast",
)

data class FoodLogWithItem(
    val log: FoodLogEntity,
    val foodItem: FoodItemEntity,
)

class NutritionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as VeyraApp).database.nutritionDao()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _state = MutableStateFlow(NutritionUiState())
    val state: StateFlow<NutritionUiState> = _state.asStateFlow()

    init {
        loadGoal()
        loadFoodLogs()
        loadFavorites()
    }

    private fun loadGoal() {
        viewModelScope.launch {
            dao.getNutritionGoal().collect { goal ->
                _state.value = _state.value.copy(
                    goal = goal ?: NutritionGoalEntity()
                )
            }
        }
    }

    private fun loadFoodLogs() {
        viewModelScope.launch {
            val dateStr = _state.value.date.format(dateFormatter)
            dao.getFoodLogsForDate(dateStr).collect { logs ->
                val logsWithItems = logs.mapNotNull { log ->
                    val item = dao.getFoodItemById(log.foodItemId) ?: return@mapNotNull null
                    FoodLogWithItem(log, item)
                }
                val totalCal = logsWithItems.sumOf { it.foodItem.calories * it.log.servings }
                val totalPro = logsWithItems.sumOf { it.foodItem.protein * it.log.servings }
                val totalCarbs = logsWithItems.sumOf { it.foodItem.carbs * it.log.servings }
                val totalFat = logsWithItems.sumOf { it.foodItem.fat * it.log.servings }

                _state.value = _state.value.copy(
                    foodLogs = logsWithItems,
                    totalCalories = totalCal,
                    totalProtein = totalPro,
                    totalCarbs = totalCarbs,
                    totalFat = totalFat,
                )
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            dao.getFavoriteFoods().collect { favs ->
                _state.value = _state.value.copy(favorites = favs)
            }
        }
    }

    fun changeDate(date: LocalDate) {
        _state.value = _state.value.copy(date = date)
        loadFoodLogs()
    }

    fun addFoodLog(foodItemId: Long, mealType: String, servings: Double) {
        viewModelScope.launch {
            val dateStr = _state.value.date.format(dateFormatter)
            dao.insertFoodLog(
                FoodLogEntity(
                    foodItemId = foodItemId,
                    mealType = mealType,
                    servings = servings,
                    date = dateStr,
                )
            )
        }
    }

    fun deleteFoodLog(log: FoodLogEntity) {
        viewModelScope.launch {
            dao.deleteFoodLog(log)
        }
    }

    fun updateGoal(goal: NutritionGoalEntity) {
        viewModelScope.launch {
            dao.insertNutritionGoal(goal)
        }
    }

    fun toggleFavorite(item: FoodItemEntity) {
        viewModelScope.launch {
            dao.updateFoodItem(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun setMealType(type: String) {
        _state.value = _state.value.copy(mealType = type)
    }
}
