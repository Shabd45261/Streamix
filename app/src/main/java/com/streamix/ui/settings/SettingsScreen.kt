package com.streamix.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.streamix.ui.theme.LocalCustomColors
import com.streamix.ui.theme.ThemeViewModel
import com.streamix.ui.theme.toHex
import com.streamix.ui.navigation.Screen
import com.streamix.ui.navigation.UpdateViewModel
import com.streamix.ui.navigation.UpdateUIState
import com.streamix.BuildConfig
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: ThemeViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val colors = LocalCustomColors.current
    val scope = rememberCoroutineScope()
    
    val updateState by updateViewModel.uiState.collectAsState()
    
    val primaryColor by viewModel.primaryColor.collectAsState()
    val secondaryColor by viewModel.secondaryColor.collectAsState()
    val tertiaryColor by viewModel.tertiaryColor.collectAsState()
    
    val youtubeAccountName by viewModel.youtubeAccountName.collectAsState(initial = null)
    val autoScrollShorts by viewModel.autoScrollShorts.collectAsState()
    val floatingDockEnabled by viewModel.floatingDockEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.secondary.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = colors.secondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                "Settings",
                color = colors.secondary,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item { SettingsSectionTitle("Appearance") }
            
            item {
                ColorSelectionRow(
                    label = "Primary (Background)",
                    options = listOf("#000000", "#FFFFFF"),
                    selected = primaryColor.toHex(),
                    onSelect = { hex ->
                        viewModel.updateTheme(Color(android.graphics.Color.parseColor(hex)), secondaryColor, tertiaryColor)
                    }
                )
            }

            item {
                val tertiaryOptions = listOf(
                    "#00FFFF", "#000080", "#FFA500", "#008000", "#0000FF", "#4169E1",
                    "#000000", "#FFFFFF", "#FFFF00", "#A52A2A", "#800080", "#008080", "#FF0000"
                )
                ColorSelectionRow(
                    label = "Tertiary (Accents/Borders)",
                    options = tertiaryOptions,
                    selected = tertiaryColor.toHex(),
                    onSelect = { hex ->
                        viewModel.updateTheme(primaryColor, secondaryColor, Color(android.graphics.Color.parseColor(hex)))
                    }
                )
            }

            item { SettingsSectionTitle("Adult Profile") }
            item {
                SettingsItem(
                    title = "Reset Age Verification", 
                    subtitle = "Ask for age again next time",
                    onClick = { 
                        viewModel.setAdultVerified(false)
                    }
                )
            }

            item { SettingsSectionTitle("Video Player") }
            item {
                SettingsSwitchItem(
                    title = "Auto-scroll Shorts",
                    subtitle = "Automatically scroll to next video when current one ends",
                    checked = autoScrollShorts,
                    onCheckedChange = { viewModel.setAutoScrollShorts(it) }
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Experimental Floating Dock",
                    subtitle = "Stacked Samsung-style Dynamic Dock UI",
                    checked = floatingDockEnabled,
                    onCheckedChange = { viewModel.setFloatingDockEnabled(it) }
                )
            }

            item { SettingsSectionTitle("Accounts") }
            item {
                SettingsItem(
                    title = "YouTube Account",
                    subtitle = youtubeAccountName ?: "Not Logged In",
                    onClick = {
                        if (youtubeAccountName == null) {
                            navController.navigate(Screen.YoutubeLogin.route)
                        } else {
                            viewModel.logoutYoutube()
                        }
                    }
                )
            }
            
            item { SettingsSectionTitle("About") }
            item { 
                SettingsItem(
                    title = "Check for Updates", 
                    subtitle = when (updateState) {
                        is UpdateUIState.Checking -> "Checking..."
                        is UpdateUIState.UpToDate -> "You're up to date"
                        is UpdateUIState.UpdateAvailable -> "New version available"
                        is UpdateUIState.Error -> (updateState as UpdateUIState.Error).message
                        else -> "Current Version: ${BuildConfig.VERSION_NAME}"
                    }, 
                    onClick = { 
                        updateViewModel.checkForUpdates(isAuto = false)
                    }
                )
            }
        }
    }
}

@Composable
fun ColorSelectionRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    val colors = LocalCustomColors.current
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(label, color = colors.secondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(options) { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected.uppercase() == hex.uppercase()) 2.dp else 1.dp,
                            color = if (selected.uppercase() == hex.uppercase()) colors.tertiary else Color.White.copy(0.2f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) }
                )
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    val colors = LocalCustomColors.current
    Text(
        text = title.uppercase(),
        color = colors.secondary.copy(alpha = 0.4f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun SettingsSwitchItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalCustomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.secondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = colors.secondary.copy(alpha = 0.5f), fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.tertiary,
                checkedTrackColor = colors.tertiary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, color = colors.secondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = colors.secondary.copy(alpha = 0.5f), fontSize = 13.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = colors.secondary.copy(alpha = 0.3f))
    }
}
