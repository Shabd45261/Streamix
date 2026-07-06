package com.streamix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.streamix.ui.navigation.Screen
import com.streamix.ui.theme.LocalCustomColors

data class DockItem(
    val route: String, 
    val icon: ImageVector, 
    val selectedIcon: ImageVector,
    val label: String
)

val DOCK_ITEMS = listOf(
    DockItem(Screen.Home.route,     Icons.Outlined.Home,     Icons.Filled.Home,     "Home"),
    DockItem(Screen.Search.route + "/null",   Icons.Outlined.Search,   Icons.Filled.Search,   "Search"),
    DockItem(Screen.Library.route,  Icons.Outlined.Folder,   Icons.Filled.Folder,   "Library"),
    DockItem(Screen.Settings.route, Icons.Outlined.Settings, Icons.Filled.Settings, "Settings")
)

@Composable
fun StreamixBottomDock(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DOCK_ITEMS.forEach { item ->
                val selected = currentRoute?.startsWith(item.route.split("/")[0]) == true
                
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

@Composable
fun DockButton(item: DockItem, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 32.dp)
                .clip(CircleShape)
                .background(if (selected) colors.tertiary.copy(alpha = 0.15f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = if (selected) colors.tertiary else colors.secondary.copy(0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
