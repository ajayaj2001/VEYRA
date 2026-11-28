package com.aura.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aura.data.local.entity.FoodLogEntity
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.AuraTextField
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal data class FoodTotals(
    val amount: Double?,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

internal fun totalFood(main: FoodDraft, ingredients: List<FoodDraft>): FoodTotals {
    val entries = ingredients.ifEmpty { listOf(main) }
    val amounts = entries.mapNotNull { foodQuantity(it.amount) }
    return FoodTotals(
        amount = amounts.takeIf { it.size == entries.size }?.sum(),
        calories = entries.sumOf { foodQuantity(it.calories) ?: 0.0 },
        protein = entries.sumOf { foodQuantity(it.protein) ?: 0.0 },
        carbs = entries.sumOf { foodQuantity(it.carbs) ?: 0.0 },
        fat = entries.sumOf { foodQuantity(it.fat) ?: 0.0 },
    )
}

@Composable
internal fun FoodFields(draft: FoodDraft, onChange: (FoodDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FoodField("Food name", draft.name, { onChange(draft.copy(name = it)) }, example = "e.g. Oatmeal")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FoodField("Amount (g, optional)", draft.amount, { onChange(draft.copy(amount = it)) }, Modifier.weight(1f), numeric = true)
            FoodField("Calories (kcal)", draft.calories, { onChange(draft.copy(calories = it)) }, Modifier.weight(1f), numeric = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FoodField("Protein (g)", draft.protein, { onChange(draft.copy(protein = it)) }, Modifier.weight(1f), numeric = true)
            FoodField("Carbs (g)", draft.carbs, { onChange(draft.copy(carbs = it)) }, Modifier.weight(1f), numeric = true)
            FoodField("Fat (g)", draft.fat, { onChange(draft.copy(fat = it)) }, Modifier.weight(1f), numeric = true)
        }
        if (!validFoodNumbers(draft)) Text("Use numbers of zero or more.", color = V.Error)
    }
}

@Composable
private fun FoodField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    example: String = "0",
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = V.Gray, style = MaterialTheme.typography.labelMedium)
        AuraTextField(
            value, onChange, example,
            modifier = Modifier.semantics { contentDescription = label },
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
        )
    }
}

@Composable
internal fun ColumnScope.FoodReview(
    draft: FoodDraft,
    ingredients: List<FoodDraft>,
    photo: String?,
    meal: String,
    favorite: Boolean,
    busy: Boolean,
    onMeal: (String) -> Unit,
    onFavorite: (Boolean) -> Unit,
    onIngredients: (List<FoodDraft>) -> Unit,
    onLog: () -> Unit,
) {
    var editIngredient by rememberSaveable { mutableIntStateOf(-2) }
    val total = remember(draft, ingredients) { totalFood(draft, ingredients) }
    val validTotals = listOfNotNull(total.amount, total.calories, total.protein, total.carbs, total.fat).all { it.isFinite() }

    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(AuraScreenInset),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (photo != null) FoodPhoto(photo, Modifier.fillMaxWidth().height(148.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(draft.name.trim(), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, color = V.White)
            IconButton(onClick = { onFavorite(!favorite) }, enabled = !busy) {
                Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (favorite) "Remove favorite" else "Save as favorite", tint = V.Amber)
            }
        }
        MealPicker(meal, onMeal, enabled = !busy)
        Text("Calories & macros", style = MaterialTheme.typography.titleLarge, color = V.White)
        NutritionTiles(total)
        if (!validTotals) Text("The combined nutrition values are too large.", color = V.Error)
        Text("Ingredients", style = MaterialTheme.typography.titleLarge, color = V.White)
        if (ingredients.isEmpty()) {
            Text("Add ingredients to calculate the meal totals.", color = V.Gray, style = MaterialTheme.typography.bodySmall)
        }
        ingredients.forEachIndexed { index, ingredient ->
            val summary = totalFood(ingredient, emptyList())
            VCard(onClick = if (busy) null else ({ editIngredient = index })) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ingredient.name, style = MaterialTheme.typography.titleMedium, color = V.White)
                        Text(
                            listOfNotNull(summary.amount?.let { "${nutritionNumber(it)} g" }, "${nutritionNumber(summary.calories)} kcal")
                                .joinToString(" · "),
                            color = V.Gray,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "P ${nutritionNumber(summary.protein)} g · C ${nutritionNumber(summary.carbs)} g · F ${nutritionNumber(summary.fat)} g",
                            color = V.Gray,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = { onIngredients(ingredients.filterIndexed { i, _ -> i != index }) }, enabled = !busy) {
                        Icon(Icons.Rounded.RemoveCircleOutline, "Remove ${ingredient.name}", tint = V.Gray)
                    }
                }
            }
        }
        OutlinedButton(onClick = { editIngredient = -1 }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add ingredient")
        }
    }
    AuraPrimaryButton(
        if (busy) "Logging…" else "Log food",
        onLog,
        Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp),
        enabled = !busy && validTotals,
    )
    if (editIngredient != -2) {
        val index = editIngredient
        val original = ingredients.getOrNull(index)
        IngredientEditor(original, onDismiss = { editIngredient = -2 }, onSave = {
            onIngredients(if (index < 0) ingredients + it else ingredients.mapIndexed { i, entry -> if (i == index) it else entry })
            editIngredient = -2
        })
    }
}

@Composable
private fun MealPicker(selected: String, onSelect: (String) -> Unit, enabled: Boolean) {
    var menu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { menu = true }, enabled = enabled) {
            Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(mealTypes.first { it.first == selected }.second)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ExpandMore, "Select meal", Modifier.size(18.dp))
        }
        DropdownMenu(menu, { menu = false }) {
            mealTypes.forEach { (key, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(key); menu = false })
            }
        }
    }
}

@Composable
private fun IngredientEditor(initial: FoodDraft?, onDismiss: () -> Unit, onSave: (FoodDraft) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var amount by rememberSaveable { mutableStateOf(initial?.amount.orEmpty()) }
    var calories by rememberSaveable { mutableStateOf(initial?.calories.orEmpty()) }
    var protein by rememberSaveable { mutableStateOf(initial?.protein.orEmpty()) }
    var carbs by rememberSaveable { mutableStateOf(initial?.carbs.orEmpty()) }
    var fat by rememberSaveable { mutableStateOf(initial?.fat.orEmpty()) }
    val draft = FoodDraft(name, amount, calories, protein, carbs, fat)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add ingredient" else "Edit ingredient") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FoodFields(draft, {
                    name = it.name
                    amount = it.amount
                    calories = it.calories
                    protein = it.protein
                    carbs = it.carbs
                    fat = it.fat
                })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft.copy(name = name.trim())) },
                enabled = name.isNotBlank() && validFoodNumbers(draft)) { Text("Save ingredient") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NutritionTiles(total: FoodTotals) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            total.amount?.let { MetricTile("Amount", it, "g", V.Gray, Modifier.weight(1f)) }
            MetricTile("Calories", total.calories, "kcal", V.Amber, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Protein", total.protein, "g", V.Teal, Modifier.weight(1f))
            MetricTile("Carbs", total.carbs, "g", V.Amber, Modifier.weight(1f))
            MetricTile("Fat", total.fat, "g", V.Green, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricTile(label: String, number: Double, unit: String, accent: Color, modifier: Modifier) {
    Column(
        modifier.background(V.SurfaceRaised, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
        Text("${nutritionNumber(number)} $unit", style = MaterialTheme.typography.titleLarge, color = V.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoggedFoodDetails(
    log: FoodLogEntity,
    favorite: Boolean,
    onFavorite: suspend (Boolean) -> Unit,
    onDelete: suspend () -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = { if (!busy) onClose() }, containerColor = V.Canvas, contentColor = V.White) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = AuraScreenInset, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(log.foodName, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = {
                    if (!busy) {
                        busy = true
                        scope.launch {
                            try {
                                onFavorite(!favorite)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                error = "Couldn’t update this favorite."
                            } finally {
                                busy = false
                            }
                        }
                    }
                }, enabled = !busy) {
                    Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (favorite) "Remove favorite" else "Save as favorite", tint = V.Amber)
                }
            }
            val meal = mealTypes.firstOrNull { it.first == log.mealType }?.second ?: log.mealType
            Text("$meal · ${nutritionDate(java.time.LocalDate.parse(log.date))}", color = V.Gray)
            Text("Calories & macros", style = MaterialTheme.typography.titleLarge)
            NutritionTiles(FoodTotals(null, log.calories, log.protein, log.carbs, log.fat))
            error?.let { Text(it, color = V.Error) }
            TextButton(onClick = { confirmDelete = true }, enabled = !busy) { Text("Remove from food log", color = V.Red) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = false },
            title = { Text("Remove this food?") },
            text = { Text("Its calories and macros will be removed from this day.") },
            confirmButton = {
                TextButton(onClick = {
                    if (!busy) {
                        busy = true
                        scope.launch {
                            try {
                                onDelete()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                error = "Couldn’t remove this food."
                            } finally {
                                busy = false
                                confirmDelete = false
                            }
                        }
                    }
                }, enabled = !busy) { Text("Remove", color = V.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !busy) { Text("Cancel") } },
        )
    }
}
