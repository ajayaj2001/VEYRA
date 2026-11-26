package com.aura.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.food.DailyNutrition
import com.aura.app.data.food.FoodDraft
import com.aura.app.data.food.FoodRepository
import com.aura.app.data.food.FoodWithIngredients
import com.aura.app.data.workout.AuraDatabase
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FoodDayState(
    val date: LocalDate,
    val entries: List<FoodWithIngredients>,
    val totals: DailyNutrition,
    val calorieGoal: Int = 3_000,
    val proteinGoal: Int = 135,
    val carbsGoal: Int = 405,
    val fatGoal: Int = 93,
)

class FoodViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FoodRepository(AuraDatabase.get(application))
    private val today = LocalDate.now()

    val state: StateFlow<FoodDayState> = repository.entriesFor(today)
        .map { entries ->
            FoodDayState(
                date = today,
                entries = entries,
                totals = DailyNutrition(
                    calories = entries.sumOf { it.food.calories },
                    carbsGrams = entries.sumOf { it.food.carbsGrams },
                    proteinGrams = entries.sumOf { it.food.proteinGrams },
                    fatGrams = entries.sumOf { it.food.fatGrams },
                ),
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FoodDayState(today, emptyList(), DailyNutrition(0, 0.0, 0.0, 0.0)),
        )

    fun log(draft: FoodDraft, onLogged: () -> Unit) {
        if (draft.name.isBlank() || draft.calories < 0) return
        viewModelScope.launch {
            repository.log(draft)
            onLogged()
        }
    }

    fun delete(entryId: Long) {
        viewModelScope.launch { repository.delete(entryId) }
    }
}
