package com.aura.ui.screens.auth

import android.os.Bundle
import androidx.core.os.bundleOf

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.R
import com.aura.ui.components.StepProgress
import com.aura.ui.components.VButton
import com.aura.ui.theme.V
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val Raised = Color(0xFF1E2020)
private val Caption = TextStyle(color = V.Gray, fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp)
private val Body = TextStyle(color = V.White, fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp)
private val Heading = Body.copy(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp)
private val Action = Body.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp)
private val BirthDateFormat = DateTimeFormatter.ofPattern("dd / MM / uuuu")

@Composable
private fun SetupLayout(
    step: Int,
    action: String,
    onAction: () -> Unit,
    onBack: () -> Unit,
    enabled: Boolean = true,
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(V.Black)
            .statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 24.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                SetupImage(R.drawable.profile_setup_back, Modifier.size(24.dp), "Back")
            }
            Text("Set up your profile", Modifier.weight(1f).padding(start = 2.dp), style = Action)
            Text("$step / 3", Modifier.padding(end = 18.dp), style = Caption)
        }
        Spacer(Modifier.height(12.dp))
        StepProgress(current = step, spacing = 12.dp)

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 12.dp),
            content = content,
        )

        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (error != null) Text(error, color = V.Error, style = Caption)
            VButton(action, onAction, Modifier.height(58.dp), enabled = enabled,
                contentColor = V.Black, labelStyle = Action)
            Text("You can update these anytime.", Modifier.padding(vertical = 12.dp), style = Caption)
        }
    }
}

@Composable
private fun SetupImage(@DrawableRes image: Int, modifier: Modifier, description: String? = null) {
    Image(painterResource(image), description, modifier)
}

@Composable
private fun Intro(title: String, subtitle: String, gap: Int = 18) {
    Text(title, style = Heading)
    Spacer(Modifier.height(gap.dp))
    Text(subtitle, style = Body, color = V.Gray)
}

@Composable
private fun FieldLabel(label: String) {
    Text(label, style = Caption)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SetupInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    textStyle: TextStyle = Body,
    placeholderColor: Color = V.Gray,
    suffix: String? = null,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        textStyle = textStyle,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        cursorBrush = SolidColor(V.Teal),
        decorationBox = { input ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle, color = placeholderColor)
                    input()
                }
                if (suffix != null) Text(suffix, Modifier.padding(start = 12.dp, end = 18.dp), style = Caption)
            }
        },
    )
}

private fun Modifier.fieldSurface() =
    fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).background(Raised)

@Composable
private fun PickerField(
    label: String,
    value: String,
    placeholder: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fieldSurface().clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value.ifEmpty { placeholder }, Modifier.weight(1f), style = Body,
            color = if (value.isEmpty()) V.Gray else V.White)
        SetupImage(icon, Modifier.size(24.dp))
    }
}

@Composable
fun SetupBasicScreen(onContinue: (Bundle) -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableIntStateOf(-1) }
    var showBirthDate by rememberSaveable { mutableStateOf(false) }

    SetupLayout(1, "Continue", {
        onContinue(bundleOf("name" to name.trim(), "birthDate" to birthDate, "gender" to gender))
    }, onBack, enabled = name.isNotBlank() && birthDate.isNotEmpty() && gender != -1) {
        Intro("Let’s get to\nknow you", "This helps us personalize\nyour experience.")
        Spacer(Modifier.height(32.dp))
        FieldLabel("Name")
        SetupInput("Name", name, { name = it }, "Alex Morgan",
            Modifier.fieldSurface().padding(horizontal = 16.dp, vertical = 16.dp))
        Spacer(Modifier.height(20.dp))
        FieldLabel("Date of birth")
        PickerField("Date of birth", birthDate, "DD / MM / YYYY", R.drawable.profile_setup_calendar) {
            showBirthDate = true
        }
        Spacer(Modifier.height(20.dp))
        FieldLabel("Gender")
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Male", "Female", "Other").forEachIndexed { index, label ->
                val chosen = gender == index
                Box(
                    Modifier.weight(1f).height(48.dp).clip(CircleShape)
                        .background(if (chosen) V.Teal else Raised)
                        .selectable(chosen, role = Role.RadioButton, onClick = { gender = index }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = Body, color = if (chosen) V.Black else V.White)
                }
            }
        }
    }

    if (showBirthDate) BirthDatePicker(birthDate, onDismiss = { showBirthDate = false }, onSelect = {
        birthDate = it
        showBirthDate = false
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePicker(value: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val today = LocalDate.now()
    val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val currentDate = value.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it, BirthDateFormat) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = currentDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        initialDisplayedMonthMillis = (currentDate ?: today.minusYears(18))
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = 1900..today.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = state.selectedDateMillis != null, onClick = {
                state.selectedDateMillis?.let { millis ->
                    onSelect(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate().format(BirthDateFormat))
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state, Modifier.verticalScroll(rememberScrollState()))
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    units: List<String>,
    unit: Int,
    onUnit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Raised).padding(12.dp)) {
        Text(label, Modifier.padding(horizontal = 4.dp), style = Caption)
        Spacer(Modifier.height(7.dp))
        SetupInput(label, value, onChange, placeholder, Modifier.padding(horizontal = 4.dp),
            KeyboardType.Decimal, Heading, V.White)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().clip(CircleShape).background(V.Black).padding(3.dp).selectableGroup()) {
            units.forEachIndexed { index, text ->
                val chosen = index == unit
                Box(
                    Modifier.weight(1f).height(28.dp).clip(CircleShape)
                        .background(if (chosen) V.Teal else V.Black)
                        .selectable(chosen, role = Role.RadioButton, onClick = { onUnit(index) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text, style = Caption, color = if (chosen) V.Black else V.Gray)
                }
            }
        }
    }
}

private data class ActivityOption(val label: String, @DrawableRes val icon: Int, @DrawableRes val badge: Int)

private val activities = listOf(
    ActivityOption("Sedentary", R.drawable.profile_setup_sedentary, R.drawable.profile_setup_sedentary_badge),
    ActivityOption("Light", R.drawable.profile_setup_light, R.drawable.profile_setup_light_badge),
    ActivityOption("Moderate", R.drawable.profile_setup_moderate, R.drawable.profile_setup_moderate_badge),
    ActivityOption("Intense", R.drawable.profile_setup_intense, R.drawable.profile_setup_intense_badge),
)

private fun convertMetric(value: String, factor: String, toMetric: Boolean): String {
    val number = value.replace(',', '.').toBigDecimalOrNull() ?: return value
    val ratio = factor.toBigDecimal()
    val converted = if (toMetric) number.multiply(ratio) else number.divide(ratio, 2, RoundingMode.HALF_UP)
    return converted.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

@Composable
fun SetupBodyScreen(onContinue: (Bundle) -> Unit, onBack: () -> Unit) {
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var heightUnit by rememberSaveable { mutableIntStateOf(0) }
    var weightUnit by rememberSaveable { mutableIntStateOf(0) }
    var activity by rememberSaveable { mutableIntStateOf(2) }
    val weightUnits = listOf("kg", "lbs")
    fun String.isMeasurement() = replace(',', '.').toBigDecimalOrNull()?.let { it.signum() > 0 } == true

    SetupLayout(2, "Continue", {
        onContinue(bundleOf(
            "height" to height, "weight" to weight, "targetWeight" to target,
            "heightUnit" to if (heightUnit == 0) "cm" else "ft",
            "weightUnit" to weightUnits[weightUnit], "activity" to activities[activity].label,
        ))
    }, onBack, enabled = height.isMeasurement() && weight.isMeasurement()) {
        Intro("Your body metrics", "Used for accurate calorie and\nrecovery calculations.", gap = 14)
        Spacer(Modifier.height(23.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Height", height, { height = it }, if (heightUnit == 0) "175" else "5.74",
                listOf("cm", "ft"), heightUnit, { unit ->
                    if (unit != heightUnit) {
                        height = convertMetric(height, "30.48", unit == 0)
                        heightUnit = unit
                    }
                }, Modifier.weight(1f))
            MetricCard("Weight", weight, { weight = it }, if (weightUnit == 0) "70" else "154",
                weightUnits, weightUnit, { unit ->
                    if (unit != weightUnit) {
                        weight = convertMetric(weight, "0.45359237", unit == 0)
                        target = convertMetric(target, "0.45359237", unit == 0)
                        weightUnit = unit
                    }
                }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Target weight", style = Caption)
            Text("Optional", style = Caption)
        }
        Spacer(Modifier.height(8.dp))
        SetupInput("Target weight", target, { target = it }, if (weightUnit == 0) "65" else "143",
            Modifier.fieldSurface().padding(horizontal = 16.dp, vertical = 16.dp),
            KeyboardType.Decimal, placeholderColor = V.White, suffix = weightUnits[weightUnit])
        Spacer(Modifier.height(14.dp))
        Text("Activity level", style = Action)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            activities.forEachIndexed { index, option ->
                val chosen = activity == index
                val shape = RoundedCornerShape(14.dp)
                Column(
                    Modifier.weight(1f).heightIn(min = 84.dp).clip(shape)
                        .background(if (chosen) V.Black else Raised)
                        .then(if (chosen) Modifier.border(1.2.dp, V.Teal, shape) else Modifier)
                        .selectable(chosen, role = Role.RadioButton, onClick = { activity = index })
                        .padding(top = 12.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SetupImage(option.badge, Modifier.size(36.dp))
                        SetupImage(option.icon, Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(option.label, style = Caption, color = if (chosen) V.White else V.Gray, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private data class Goal(val label: String, @DrawableRes val icon: Int)

private val goals = listOf(
    Goal("Lose Weight", R.drawable.profile_setup_lose),
    Goal("Build Muscle", R.drawable.profile_setup_muscle),
    Goal("Get Fitter", R.drawable.profile_setup_fitter),
    Goal("Eat Better", R.drawable.profile_setup_eat),
    Goal("Sleep Better", R.drawable.profile_setup_sleep),
    Goal("Reduce Stress", R.drawable.profile_setup_stress),
)

@Composable
fun SetupGoalsScreen(onComplete: (Bundle) -> Unit, onBack: () -> Unit, saving: Boolean, error: String?) {
    var selected by rememberSaveable { mutableIntStateOf((1 shl 1) or (1 shl 2)) }
    var reminders by rememberSaveable { mutableStateOf(true) }

    SetupLayout(3, if (saving) "Saving…" else "Get Started", {
        val chosen = goals.filterIndexed { index, _ -> selected and (1 shl index) != 0 }
        onComplete(bundleOf("selected" to ArrayList(chosen.map { it.label }), "reminders" to reminders))
    }, onBack, enabled = selected != 0 && !saving, error = error) {
        Intro("What are your\ngoals?", "Select all that apply.")
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            goals.withIndex().chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (index, goal) ->
                        val bit = 1 shl index
                        GoalCard(goal, selected and bit != 0, { selected = selected xor bit }, Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 84.dp).clip(RoundedCornerShape(16.dp))
                .background(Raised).toggleable(reminders, role = Role.Switch, onValueChange = { reminders = it })
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Daily reminders", style = Body)
                Spacer(Modifier.height(4.dp))
                Text("Get nudges to log meals,\nworkouts, and check-ins.", style = Caption)
            }
            if (reminders) {
                SetupImage(R.drawable.profile_setup_switch_on, Modifier.padding(top = 4.dp).size(48.dp, 28.dp))
            } else {
                Box(
                    Modifier.padding(top = 4.dp).size(48.dp, 28.dp).clip(CircleShape)
                        .background(V.SurfaceBright).padding(3.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(V.White))
                }
            }
        }
    }
}

@Composable
private fun GoalCard(goal: Goal, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.heightIn(min = 92.dp).clip(shape).background(if (selected) V.Black else Raised)
            .then(if (selected) Modifier.border(1.5.dp, V.Teal, shape) else Modifier)
            .toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Box(Modifier.size(34.dp).clip(CircleShape).background(V.Black), contentAlignment = Alignment.Center) {
                SetupImage(goal.icon, Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(goal.label, style = Body)
        }
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 2.dp), contentAlignment = Alignment.Center) {
                SetupImage(R.drawable.profile_setup_selected, Modifier.size(18.dp))
                SetupImage(R.drawable.profile_setup_check, Modifier.size(14.dp))
            }
        }
    }
}
