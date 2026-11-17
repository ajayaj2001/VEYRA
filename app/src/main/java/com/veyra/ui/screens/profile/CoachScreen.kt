package com.veyra.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veyra.ui.components.VCard
import com.veyra.ui.theme.V

data class ChatMsg(val text: String, val isUser: Boolean, val time: String = "")

@Composable
fun CoachScreen(nav: NavController) {
    var input by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            ChatMsg("Good morning! Your recovery score is 78 today. Based on your HRV and sleep data, you're well-recovered for a moderate to hard workout.", false, "9:14 AM"),
        )
    }
    val suggestions = listOf("How's my recovery?", "Suggest a workout", "Am I eating enough protein?", "Sleep tips")

    Column(Modifier.fillMaxSize().background(V.Black).statusBarsPadding()) {
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, "Back", tint = V.White) }
            Text("Coach", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = V.White, modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Rounded.Settings, "Settings", tint = V.GrayDark) }
        }

        // Chat
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), reverseLayout = false) {
            item {
                Text("TODAY", style = MaterialTheme.typography.labelSmall, color = V.GrayDark,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }

            items(messages) { msg ->
                if (msg.isUser) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp))
                                .background(V.Teal).padding(14.dp)
                        ) {
                            Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = V.Black)
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Box(
                            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
                                .background(V.Surface).padding(14.dp)
                        ) {
                            Column {
                                Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = V.White)
                                if (msg.time.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(msg.time, style = MaterialTheme.typography.labelSmall, color = V.GrayDark)
                                }
                            }
                        }
                    }
                }
            }

            // Suggestions
            if (messages.size <= 1) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.take(2).forEach { s ->
                            Box(
                                Modifier.clip(RoundedCornerShape(9999.dp)).background(V.Surface)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .let { m -> m.clickable { input = s } },
                            ) { Text(s, style = MaterialTheme.typography.labelMedium, color = V.White) }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        // Input bar
        Row(
            Modifier.fillMaxWidth().background(V.Black).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) { Icon(Icons.Rounded.AddPhotoAlternate, "Photo", tint = V.GrayDark) }
            TextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask your coach...", color = V.GrayDark) },
                singleLine = true, shape = RoundedCornerShape(9999.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = V.Surface, unfocusedContainerColor = V.Surface,
                    focusedTextColor = V.White, unfocusedTextColor = V.White,
                    cursorColor = V.Teal, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(V.Teal)
                    .clickable {
                        if (input.isNotBlank()) {
                            messages.add(ChatMsg(input, true))
                            val q = input; input = ""
                            messages.add(ChatMsg("I'll analyze your data and get back to you on \"$q\". This feature is coming soon with AI-powered insights!", false, "Now"))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Send, "Send", tint = V.Black, modifier = Modifier.size(20.dp)) }
        }
    }
}

// clickable already imported via foundation
