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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.veyra.navigation.BottomNavItem
import com.veyra.navigation.Screen
import com.veyra.navigation.VeyraNavHost
import com.veyra.ui.theme.VeyraColors
import com.veyra.ui.theme.VeyraDimens
import com.veyra.ui.theme.VeyraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeyraTheme {
                VeyraMainScreen()
            }
        }
    }
}

@Composable
fun VeyraMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavRoutes = BottomNavItem.entries.map { it.screen.route }
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        containerColor = VeyraColors.Background,
        contentColor = VeyraColors.TextPrimary,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                VeyraBottomBar(
                    items = BottomNavItem.entries.toList(),
                    currentRoute = currentDestination?.route,
                    onItemClick = { item ->
                        if (currentDestination?.route != item.screen.route) {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        VeyraNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

/**
 * Samsung Health-style bottom navigation bar.
 * Selected tab gets a dark gray pill background. Clean, minimal.
 */
@Composable
fun VeyraBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.Background)
            .padding(bottom = navBarPadding.calculateBottomPadding())
            .padding(horizontal = VeyraDimens.SpaceSm)
            .height(VeyraDimens.BottomNavHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.screen.route
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.05f else 1f,
                animationSpec = tween(150),
                label = "navScale",
            )

            // Samsung-style: selected item gets pill background
            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clip(RoundedCornerShape(VeyraDimens.PillRadius))
                    .then(
                        if (selected) Modifier.background(VeyraColors.NavPill)
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onItemClick(item) },
                    )
                    .padding(vertical = VeyraDimens.SpaceSm),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) VeyraColors.TextPrimary else VeyraColors.TextTertiary,
                        modifier = Modifier.size(VeyraDimens.IconLg),
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = if (selected) VeyraColors.TextPrimary else VeyraColors.TextTertiary,
                    )
                }
            }
        }
    }
}
