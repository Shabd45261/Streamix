package com.streamix.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.streamix.ui.navigation.Screen
import com.streamix.ui.theme.LocalCustomColors

import com.streamix.core.model.Profile
import com.streamix.core.storage.PreferencesManager
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext

data class DockItem(
    val route: String, 
    val icon: ImageVector, 
    val selectedIcon: ImageVector,
    val label: String
)

@Composable
fun StreamixBottomDock(navController: NavController, profileOverride: Profile? = null, includePadding: Boolean = true) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val profileFromPrefs by prefs.currentProfile.collectAsState(initial = Profile.MOVIES)
    val profile = profileOverride ?: profileFromPrefs
    
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    val pagerIndex     = com.streamix.ui.navigation.LocalMainPagerIndex.current.value

    val dockItems = remember {
        listOf(
            DockItem(Screen.Home.route, Icons.Outlined.Home, Icons.Filled.Home, "Home"),
            DockItem(Screen.Search.route.replace("{query}", "null"), Icons.Outlined.Search, Icons.Filled.Search, "Search"),
            DockItem(Screen.Library.route, Icons.Outlined.Folder, Icons.Filled.Folder, "Library"),
            DockItem(Screen.Settings.route, Icons.Outlined.Settings, Icons.Filled.Settings, "Settings")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (includePadding) Modifier.navigationBarsPadding() else Modifier)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            shape = RoundedCornerShape(35.dp),
            color = Color.Black.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, Color.Red), // Red outline as requested
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockItems.forEachIndexed { index, item ->
                    val baseRoute = item.route.split("?")[0].split("/")[0]
                    val currentBase = currentRoute?.split("?")?.get(0)?.split("/")?.get(0)
                    
                    val selected = if (pagerIndex != -1) {
                        val mappedPagerIndex = if (profile == Profile.YOUTUBE || profile == Profile.ADULT) {
                            when (pagerIndex) {
                                0 -> 0 // Home
                                1 -> -1 // Shorts (Nothing in dock)
                                2 -> 1 // Search
                                3 -> 2 // Library
                                4 -> 3 // Settings
                                else -> -1
                            }
                        } else {
                            pagerIndex
                        }
                        index == mappedPagerIndex
                    } else {
                        baseRoute == currentBase || (item.route == Screen.Home.route && currentRoute == null)
                    }
                    
                    DockButton(
                        item = item,
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                if (item.route == Screen.Library.route) {
                                    com.streamix.ui.player.PlayerManager.pause()
                                }
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DockButton(item: DockItem, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(50.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = if (selected) Color.White else Color.White.copy(0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            color = if (selected) Color.White else Color.White.copy(0.4f),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
