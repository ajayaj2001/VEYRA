package com.aura.ui.screens.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.aura.ui.components.VCard
import com.aura.ui.theme.V
import org.json.JSONObject
import java.math.BigDecimal

private enum class Nutrient(val key: String, val label: String, val unit: String) {
    Fiber("fiber", "Fiber", "g"),
    Sugars("sugars", "Total sugars", "g"),
    AddedSugar("addedSugar", "Added sugars", "g"),
    SaturatedFat("saturatedFat", "Saturated fat", "g"),
    Sodium("sodium", "Sodium", "mg"),
    Cholesterol("cholesterol", "Cholesterol", "mg"),
    Potassium("potassium", "Potassium", "mg"),
}

internal fun emptyNutrientInputs(): ArrayList<String> = ArrayList(List(Nutrient.entries.size) { "" })

internal fun validNutrients(inputs: List<String>): Boolean =
    inputs.all { it.isBlank() || foodQuantity(it) != null }

internal fun encodeNutrients(inputs: List<String>): String? {
    val json = JSONObject()
    Nutrient.entries.forEach { nutrient ->
        inputs.getOrNull(nutrient.ordinal)?.let(::foodQuantity)?.let { json.put(nutrient.key, it) }
    }
    return json.takeIf { it.length() > 0 }?.toString()
}

internal fun nutrientInputs(encoded: String?): ArrayList<String> {
    val values = readNutrients(encoded)
    return ArrayList(Nutrient.entries.map { nutrient ->
        values[nutrient]?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty()
    })
}

private fun readNutrients(encoded: String?): Map<Nutrient, Double> {
    val json = encoded?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return emptyMap()
    return buildMap {
        Nutrient.entries.forEach { nutrient ->
            val amount = json.optDouble(nutrient.key)
            if (amount.isFinite() && amount >= 0) put(nutrient, amount)
        }
    }
}

@Composable
internal fun MoreNutrients(inputs: ArrayList<String>, onChange: (ArrayList<String>) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val valid = validNutrients(inputs)
    VCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("More nutrition details", style = MaterialTheme.typography.titleLarge, color = V.White)
                    Text("Optional", style = MaterialTheme.typography.labelMedium, color = V.Gray)
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = V.Teal)
            }
            if (expanded) {
                Nutrient.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { nutrient ->
                            FoodField(
                                "${nutrient.label} (${nutrient.unit})",
                                inputs[nutrient.ordinal],
                                { value -> onChange(ArrayList(inputs).apply { this[nutrient.ordinal] = value }) },
                                Modifier.weight(1f), numeric = true, example = "Optional",
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (!valid) Text("Use numbers of zero or more for optional nutrients.", color = V.Error)
        }
    }
}

@Composable
internal fun ExtraNutrientSummary(encoded: String?) {
    val details = remember(encoded) { readNutrients(encoded) }
    if (details.isEmpty()) return
    VCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("More nutrition details", style = MaterialTheme.typography.titleLarge, color = V.White)
            details.forEach { (nutrient, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(nutrient.label, Modifier.weight(1f), color = V.Gray)
                    Text("${nutritionNumber(value)} ${nutrient.unit}", color = V.White)
                }
            }
        }
    }
}
