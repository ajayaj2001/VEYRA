package com.aura.ui.screens.nutrition

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.aura.data.local.entity.FoodItemEntity
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

internal data class FoodDraft(
    val name: String = "",
    val amount: String = "",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
)

private val DraftSaver = listSaver<FoodDraft, String>(
    save = { listOf(it.name, it.amount, it.calories, it.protein, it.carbs, it.fat) },
    restore = { FoodDraft(it[0], it[1], it[2], it[3], it[4], it[5]) },
)

private val IngredientsSaver = listSaver<List<FoodDraft>, String>(
    save = { list -> list.flatMap { listOf(it.name, it.amount, it.calories, it.protein, it.carbs, it.fat) } },
    restore = { saved -> saved.chunked(6).map { FoodDraft(it[0], it[1], it[2], it[3], it[4], it[5]) } },
)

internal fun foodQuantity(input: String): Double? =
    input.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

internal fun validFoodNumbers(draft: FoodDraft): Boolean =
    listOf(draft.amount, draft.calories, draft.protein, draft.carbs, draft.fat)
        .all { it.isBlank() || foodQuantity(it) != null }

@Composable
internal fun NutritionEntryScreen(vm: NutritionVM, onClose: () -> Unit) {
    val state by vm.state.collectAsState()
    val favorites by vm.favorites.collectAsState(initial = emptyList())
    val dateKey = rememberSaveable { state.date.toString() }
    val date = LocalDate.parse(dateKey)
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var review by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(FoodDraft()) }
    var ingredients by rememberSaveable(stateSaver = IngredientsSaver) { mutableStateOf(emptyList<FoodDraft>()) }
    var selectedFavorite by rememberSaveable { mutableLongStateOf(0L) }
    var favorite by rememberSaveable { mutableStateOf(false) }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var meal by rememberSaveable { mutableStateOf(mealTypes.first().first) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = busy || review) { if (!busy) review = false }
    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(end = AuraScreenInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { if (review) review = false else onClose() },
                enabled = !busy,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = V.Gray)
            }
            Text(if (review) "Review meal" else "Add meal", Modifier.weight(1f),
                color = V.White, style = MaterialTheme.typography.titleLarge)
            Text(nutritionDate(date), color = V.Gray, style = MaterialTheme.typography.labelMedium)
        }
        HorizontalDivider(color = V.SurfaceRaised)

        if (review) {
            FoodReview(
                draft, ingredients, photo, meal, favorite,
                busy = busy,
                onMeal = { meal = it },
                onFavorite = { favorite = it },
                onIngredients = { ingredients = it },
                onLog = {
                    if (!busy) {
                        busy = true
                        val original = favorites.firstOrNull { it.id == selectedFavorite }
                        val values = totalFood(draft, ingredients)
                        val food = (original ?: FoodItemEntity(name = draft.name.trim())).copy(
                            name = draft.name.trim(),
                            calories = values.calories, protein = values.protein, carbs = values.carbs, fat = values.fat,
                        )
                        scope.launch {
                            try {
                                vm.logFood(food, meal, date, favorite)
                                onClose()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                message = "Couldn’t log this meal. Try again."
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
            )
        } else {
            TabRow(selectedTabIndex = mode, containerColor = V.Black, contentColor = V.Teal) {
                listOf("Manual", "Photo", "Favorites").forEachIndexed { index, name ->
                    Tab(
                        selected = mode == index, onClick = { mode = index },
                        selectedContentColor = V.Teal, unselectedContentColor = V.Gray,
                        text = { Text(name) },
                    )
                }
            }
            when (mode) {
                0 -> {
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(AuraScreenInset),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (photo != null) {
                            FoodPhoto(photo, Modifier.fillMaxWidth().height(128.dp))
                        }
                        Text("Food details", color = V.White, style = MaterialTheme.typography.headlineMedium)
                        FoodFields(draft, { draft = it })
                    }
                    AuraPrimaryButton(
                        "Review meal", { review = true },
                        Modifier.padding(horizontal = AuraScreenInset, vertical = 8.dp),
                        enabled = draft.name.isNotBlank() && validFoodNumbers(draft),
                    )
                }
                1 -> PhotoMode(photo, { photo = it }, { mode = 0 })
                2 -> FavoriteFoods(
                    favorites,
                    onSelect = { item ->
                        selectedFavorite = item.id
                        favorite = true
                        draft = FoodDraft(
                            name = item.name,
                            calories = item.calories.toString(),
                            protein = item.protein.toString(),
                            carbs = item.carbs.toString(),
                            fat = item.fat.toString(),
                        )
                        ingredients = emptyList()
                        photo = null
                        review = true
                    },
                    onRemove = { item ->
                        scope.launch {
                            try {
                                vm.setFavorite(item, false)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                message = "Couldn’t remove this favorite. Try again."
                            }
                        }
                    },
                )
            }
        }
    }

    message?.let { error ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Nutrition") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun FavoriteFoods(
    favorites: List<FoodItemEntity>,
    onSelect: (FoodItemEntity) -> Unit,
    onRemove: (FoodItemEntity) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(AuraScreenInset),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Favorite foods", style = MaterialTheme.typography.headlineMedium, color = V.White) }
        if (favorites.isEmpty()) item {
            VCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.FavoriteBorder, null, tint = V.Amber)
                    Text("No favorites yet", style = MaterialTheme.typography.titleLarge, color = V.White)
                    Text("Save a food as a favorite when you review or view a meal.", color = V.Gray)
                }
            }
        }
        items(favorites, key = { it.id }) { food ->
            VCard(onClick = { onSelect(food) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FoodListLabel(food.name, food.calories, Modifier.weight(1f))
                    IconButton(onClick = { onRemove(food) }) {
                        Icon(Icons.Rounded.Favorite, "Remove ${food.name} from favorites", tint = V.Amber)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoMode(photo: String?, onPhoto: (String?) -> Unit, onManual: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var showCamera by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var capturing by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        showCamera = allowed
        if (!allowed) message = "Camera access was denied. You can choose a photo or enter the meal manually."
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            showCamera = false
            message = null
            onPhoto(uri.toString())
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(AuraScreenInset),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Meal photo", style = MaterialTheme.typography.headlineMedium, color = V.White)
        Box(
            Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(20.dp))
                .background(V.SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                FoodPhoto(photo, Modifier.matchParentSize())
            } else if (showCamera) {
                val view = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
                AndroidView(factory = { view }, modifier = Modifier.matchParentSize())
                DisposableEffect(view, lifecycle) {
                    val future = ProcessCameraProvider.getInstance(context)
                    var provider: ProcessCameraProvider? = null
                    var preview: Preview? = null
                    var boundCapture: ImageCapture? = null
                    var disposed = false
                    future.addListener({
                        if (!disposed) {
                            try {
                                val cameraProvider = future.get()
                                val cameraPreview = Preview.Builder().build().apply { setSurfaceProvider(view.surfaceProvider) }
                                val imageCapture = ImageCapture.Builder().build()
                                cameraProvider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, cameraPreview, imageCapture)
                                provider = cameraProvider
                                preview = cameraPreview
                                boundCapture = imageCapture
                                capture = imageCapture
                            } catch (_: Exception) {
                                message = "The camera preview is unavailable. Try choosing a photo."
                                showCamera = false
                            }
                        }
                    }, ContextCompat.getMainExecutor(context))
                    onDispose {
                        disposed = true
                        val useCases = listOfNotNull(preview, boundCapture)
                        provider?.unbind(*useCases.toTypedArray())
                        capture = null
                    }
                }
            } else {
                Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(52.dp), tint = V.Amber)
            }
        }
        Text("Use a photo as a reference while entering nutrition values.", color = V.Gray)
        message?.let { Text(it, color = V.Error) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f),
            ) { Text("Choose photo") }
            OutlinedButton(
                onClick = {
                    message = null
                    if (photo != null) onPhoto(null)
                    else if (capture != null) {
                        val imageCapture = capture ?: return@OutlinedButton
                        val file = try {
                            File.createTempFile("meal-", ".jpg", context.cacheDir)
                        } catch (_: Exception) {
                            message = "Couldn’t prepare the photo. Try again."
                            return@OutlinedButton
                        }
                        capturing = true
                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    capturing = false
                                    showCamera = false
                                    onPhoto(Uri.fromFile(file).toString())
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    file.delete()
                                    capturing = false
                                    message = "Couldn’t take a photo. Try again."
                                }
                            },
                        )
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        showCamera = true
                    } else {
                        requestPermission.launch(Manifest.permission.CAMERA)
                    }
                },
                enabled = !capturing && (!showCamera || capture != null),
                modifier = Modifier.weight(1f),
            ) {
                Text(when {
                    photo != null -> "Remove photo"
                    capturing -> "Capturing…"
                    showCamera -> "Take photo"
                    else -> "Open camera"
                })
            }
        }
        AuraPrimaryButton("Enter food details", onManual)
    }
}

@Composable
internal fun FoodPhoto(uri: String?, modifier: Modifier) {
    AsyncImage(
        model = uri,
        contentDescription = "Meal photo",
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
    )
}
