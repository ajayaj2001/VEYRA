package com.veyra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.veyra.navigation.Routes
import com.veyra.navigation.Tab
import com.veyra.navigation.VeyraNavGraph
import com.veyra.ui.theme.V
import com.veyra.ui.theme.VeyraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeyraTheme { VeyraRoot() }
        }
    }
}

@Composable
private fun VeyraRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    val tabRoutes = Tab.entries.map { it.route }
    val showNav = route in tabRoutes

    Scaffold(
        containerColor = V.Black,
        bottomBar = {
            AnimatedVisibility(
                visible = showNav,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                BottomBar(
                    currentRoute = route,
                    onTab = { tab ->
                        if (route != tab.route) {
                            nav.navigate(tab.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onFab = { /* Quick Add */ },
                )
            }
        },
    ) { padding ->
        VeyraNavGraph(
            nav = nav,
            startDestination = Routes.SPLASH,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
private fun BottomBar(
    currentRoute: String?,
    onTab: (Tab) -> Unit,
    onFab: () -> Unit,
) {
    val navPadding = WindowInsets.navigationBars.asPaddingValues()

    Row(
        Modifier
            .fillMaxWidth()
            .background(V.Black)
            .padding(bottom = navPadding.calculateBottomPadding())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Nav pill container
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(9999.dp))
                .background(V.Surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                val selected = currentRoute == tab.route

                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9999.dp))
                        .clickable(
                            remember { MutableInteractionSource() }, null,
                            onClick = { onTab(tab) },
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        // Icon with glow behind it when selected
                        Box(contentAlignment = Alignment.Center) {
                            if (selected) {
                                // Teal glow circle behind icon
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(V.Teal.copy(alpha = 0.15f))
                                )
                            }
                            Icon(
                                tab.icon, tab.label,
                                tint = if (selected) V.Teal else V.GrayDark,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (selected) V.Teal else V.GrayDark,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // FAB
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(V.Teal)
                .clickable(onClick = onFab),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, "Quick Add", tint = V.Black, modifier = Modifier.size(24.dp))
        }
    }
}
