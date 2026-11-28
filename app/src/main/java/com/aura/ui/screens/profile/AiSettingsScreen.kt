package com.aura.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.data.food.FoodAi
import com.aura.ui.components.AuraPrimaryButton
import com.aura.ui.components.AuraTextField
import com.aura.ui.components.VCard
import com.aura.ui.theme.AuraScreenInset
import com.aura.ui.theme.V

@Composable
fun AiSettingsScreen(nav: NavController) {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(FoodAi.apiKey(context)) }
    var input by rememberSaveable { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    fun store(key: String?) {
        failed = runCatching { FoodAi.setApiKey(context, key) }.isFailure
        if (!failed) {
            saved = FoodAi.apiKey(context)
            input = ""
        }
    }

    Column(
        Modifier.fillMaxSize().background(V.Canvas).verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, "Back", tint = V.White) }
            Text("AI food estimation", style = MaterialTheme.typography.titleLarge, color = V.White)
        }

        Column(Modifier.padding(horizontal = AuraScreenInset, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Add a Gemini API key to estimate calories and macros from a meal photo or a short description. " +
                    "You can create a key at aistudio.google.com/apikey.",
                style = MaterialTheme.typography.bodyMedium, color = V.Gray,
            )
            VCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Gemini API key", style = MaterialTheme.typography.titleMedium, color = V.White)
                    Text(
                        saved?.let { "Saved · ends in ${it.takeLast(4)}" } ?: "Not set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (saved == null) V.Gray else V.Teal,
                    )
                }
            }
            AuraTextField(
                input, { input = it.trim() }, if (saved == null) "Paste your API key" else "Paste a new key to replace it",
                modifier = Modifier.semantics { contentDescription = "Gemini API key" },
            )
            if (failed) Text("Couldn’t update the key on this device. Try again.", color = V.Error)
            AuraPrimaryButton("Save key", { store(input) }, enabled = input.isNotEmpty())
            if (saved != null) {
                OutlinedButton(onClick = { store(null) }, modifier = Modifier.fillMaxWidth()) { Text("Remove key") }
            }
            Text(
                "The key is stored encrypted on this device only. Meal photos and descriptions you estimate are " +
                    "sent to Google’s Gemini service under your key.",
                style = MaterialTheme.typography.bodySmall, color = V.Gray,
            )
        }
    }
}
