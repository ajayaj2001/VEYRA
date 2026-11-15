package com.veyra.ui.screens.nutrition

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veyra.VeyraApp
import com.veyra.data.local.entity.FoodItemEntity
import com.veyra.ui.components.GlassCard
import com.veyra.ui.components.VeyraButton
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FoodEntryScreen(
    navController: NavController,
    foodId: Long?,
    viewModel: NutritionViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Search", "Manual", "Favorites")

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FoodItemEntity>>(emptyList()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Manual entry fields
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var servings by remember { mutableDoubleStateOf(1.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.Background)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VeyraDimens.SpaceSm, vertical = VeyraDimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = VeyraColors.TextPrimary)
            }
            Text(
                text = "Add Food",
                style = MaterialTheme.typography.headlineSmall,
                color = VeyraColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            // Camera placeholder
            IconButton(onClick = { /* Photo capture placeholder */ }) {
                Icon(Icons.Rounded.CameraAlt, "Camera", tint = VeyraColors.Green)
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = VeyraColors.Background,
            contentColor = VeyraColors.Green,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = VeyraColors.Green,
                    )
                }
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) VeyraColors.Green else VeyraColors.TextTertiary,
                        )
                    },
                )
            }
        }

        when (selectedTab) {
            // Search tab
            0 -> {
                TextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        scope.launch {
                            val app = context.applicationContext as VeyraApp
                            searchResults = withContext(Dispatchers.IO) {
                                app.database.nutritionDao().searchFoodItems(query)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(VeyraDimens.SpaceLg),
                    placeholder = { Text("Search foods...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = VeyraColors.TextTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(VeyraDimens.CardRadiusSm),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VeyraColors.SurfaceElevated,
                        unfocusedContainerColor = VeyraColors.SurfaceElevated,
                        focusedTextColor = VeyraColors.TextPrimary,
                        unfocusedTextColor = VeyraColors.TextPrimary,
                        cursorColor = VeyraColors.Green,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )

                if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(VeyraDimens.SpaceXxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("No results found", style = MaterialTheme.typography.titleMedium, color = VeyraColors.TextSecondary)
                        Text("Try manual entry instead", style = MaterialTheme.typography.bodySmall, color = VeyraColors.TextTertiary)
                    }
                }

                LazyColumn {
                    items(searchResults) { food ->
                        FoodResultItem(
                            food = food,
                            onClick = {
                                viewModel.addFoodLog(food.id, state.mealType, 1.0)
                                navController.popBackStack()
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(food) },
                        )
                    }
                }
            }

            // Manual entry tab
            1 -> {
                LazyColumn(
                    modifier = Modifier.padding(VeyraDimens.SpaceLg),
                    verticalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd),
                ) {
                    item {
                        NutritionTextField("Food name", name, { name = it })
                    }
                    item {
                        NutritionTextField("Calories", calories, { calories = it }, KeyboardType.Decimal)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(VeyraDimens.SpaceMd)) {
                            NutritionTextField("Protein (g)", protein, { protein = it }, KeyboardType.Decimal, Modifier.weight(1f))
                            NutritionTextField("Carbs (g)", carbs, { carbs = it }, KeyboardType.Decimal, Modifier.weight(1f))
                            NutritionTextField("Fat (g)", fat, { fat = it }, KeyboardType.Decimal, Modifier.weight(1f))
                        }
                    }
                    item {
                        Spacer(Modifier.height(VeyraDimens.SpaceMd))
                        VeyraButton(
                            text = "Add Food",
                            enabled = name.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    val app = context.applicationContext as VeyraApp
                                    val itemId = withContext(Dispatchers.IO) {
                                        app.database.nutritionDao().insertFoodItem(
                                            FoodItemEntity(
                                                name = name.trim(),
                                                calories = calories.toDoubleOrNull() ?: 0.0,
                                                protein = protein.toDoubleOrNull() ?: 0.0,
                                                carbs = carbs.toDoubleOrNull() ?: 0.0,
                                                fat = fat.toDoubleOrNull() ?: 0.0,
                                            )
                                        )
                                    }
                                    viewModel.addFoodLog(itemId, state.mealType, servings)
                                    navController.popBackStack()
                                }
                            },
                        )
                    }
                }
            }

            // Favorites tab
            2 -> {
                if (state.favorites.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(VeyraDimens.SpaceXxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.FavoriteBorder, null, tint = VeyraColors.TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(VeyraDimens.SpaceMd))
                        Text("No favorites yet", style = MaterialTheme.typography.titleMedium, color = VeyraColors.TextSecondary)
                    }
                }

                LazyColumn {
                    items(state.favorites) { food ->
                        FoodResultItem(
                            food = food,
                            onClick = {
                                viewModel.addFoodLog(food.id, state.mealType, 1.0)
                                navController.popBackStack()
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(food) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodResultItem(
    food: FoodItemEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.padding(horizontal = VeyraDimens.SpaceLg, vertical = VeyraDimens.SpaceXs),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.titleMedium, color = VeyraColors.TextPrimary)
                if (food.brand.isNotEmpty()) {
                    Text(food.brand, style = MaterialTheme.typography.bodySmall, color = VeyraColors.TextTertiary)
                }
                Text(
                    "${"%.0f".format(food.calories)} kcal · P: ${"%.0f".format(food.protein)}g  C: ${"%.0f".format(food.carbs)}g  F: ${"%.0f".format(food.fat)}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeyraColors.TextTertiary,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (food.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "Favorite",
                    tint = if (food.isFavorite) VeyraColors.Coral else VeyraColors.TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun NutritionTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(VeyraDimens.CardRadiusSm),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = VeyraColors.SurfaceElevated,
            unfocusedContainerColor = VeyraColors.SurfaceElevated,
            focusedTextColor = VeyraColors.TextPrimary,
            unfocusedTextColor = VeyraColors.TextPrimary,
            focusedLabelColor = VeyraColors.Green,
            unfocusedLabelColor = VeyraColors.TextTertiary,
            cursorColor = VeyraColors.Green,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
