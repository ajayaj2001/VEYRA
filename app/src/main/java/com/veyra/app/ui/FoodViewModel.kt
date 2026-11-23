package com.veyra.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veyra.app.data.food.DailyNutrition
import com.veyra.app.data.food.FoodDraft
import com.veyra.app.data.food.FoodRepository
import com.veyra.app.data.food.FoodWithIngredients
import com.veyra.app.data.workout.VeyraDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FoodDayState(
    val date: LocalDate,
    val today: LocalDate,
    val entries: List<FoodWithIngredients>,
    val totals: DailyNutrition,
    val calorieGoal: Int = 3_000,
    val proteinGoal: Int = 135,
    val carbsGoal: Int = 405,
    val fatGoal: Int = 93,
)

class FoodViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FoodRepository(VeyraDatabase.get(application))
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val localDay = flow {
        while (true) {
            val zone = ZoneId.systemDefault()
            emit(LocalDate.now(zone) to zone)
            delay(30_000)
        }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<FoodDayState> = combine(selectedDate, localDay) { selected, (today, zone) ->
        Triple(selected?.coerceAtMost(today) ?: today, today, zone)
    }.flatMapLatest { (date, today, zone) ->
        repository.entriesFor(date, zone).map { entries ->
            FoodDayState(
                date = date,
                today = today,
                entries = entries,
                totals = DailyNutrition(
                    calories = entries.sumOf { it.food.calories },
                    carbsGrams = entries.sumOf { it.food.carbsGrams },
                    proteinGrams = entries.sumOf { it.food.proteinGrams },
                    fatGrams = entries.sumOf { it.food.fatGrams },
                ),
            )
        }
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LocalDate.now().let { today ->
                FoodDayState(today, today, emptyList(), DailyNutrition(0, 0.0, 0.0, 0.0))
            },
        )

    fun selectDate(date: LocalDate?) {
        selectedDate.value = date?.takeIf { it.isBefore(LocalDate.now()) }
    }

    fun log(draft: FoodDraft, date: LocalDate, onLogged: () -> Unit) {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        if (draft.name.isBlank() || draft.calories < 0 || date.isAfter(today)) return
        val loggedAt = if (date == today) now else date.atTime(12, 0).atZone(zone).toInstant()
        viewModelScope.launch {
            repository.log(draft, loggedAt.toEpochMilli())
            selectDate(date)
            onLogged()
        }
    }

    fun delete(entryId: Long) {
        viewModelScope.launch { repository.delete(entryId) }
    }
}
