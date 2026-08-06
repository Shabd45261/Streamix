package com.streamix.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamix.core.ProfileManager
import com.streamix.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentProfile: ProfileManager.Profile,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.prefs.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium,
                 fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
        }

        // --- APPEARANCE ---
        item {
            SettingsGroup("Appearance") {
                SettingsEnumRow(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    currentValue = prefs.theme,
                    onValueChange = viewModel::setTheme
                )
                SettingsRow(
                    icon = Icons.Default.Visibility,
                    title = "AMOLED Black",
                    subtitle = "Use pure black background in dark mode",
                    trailing = { Switch(checked = prefs.theme == AppTheme.AMOLED, onCheckedChange = { if(it) viewModel.setTheme(AppTheme.AMOLED) else viewModel.setTheme(AppTheme.DARK) }) }
                )
                SettingsEnumRow(
                    icon = Icons.Default.FormatSize,
                    title = "Font Size",
                    currentValue = prefs.fontSize,
                    onValueChange = viewModel::setFontSize
                )
            }
        }

        // --- PLAYER ---
        item {
            SettingsGroup("Player") {
                SettingsEnumRow(
                    icon = Icons.Default.AspectRatio,
                    title = "Aspect Ratio",
                    currentValue = prefs.aspectRatio,
                    onValueChange = viewModel::setAspectRatio
                )
                SettingsRow(
                    icon = Icons.Default.SkipNext,
                    title = "Skip Forward",
                    subtitle = "${prefs.skipForwardInterval} sec",
                    trailing = { Icon(Icons.Default.ChevronRight, null) }
                )
            }
        }

        // --- NAVIGATION ---
        item {
            SettingsGroup("Navigation") {
                SettingsEnumRow(
                    icon = Icons.Default.Navigation,
                    title = "Bottom Navigation Style",
                    currentValue = prefs.bottomNavStyle,
                    onValueChange = viewModel::setNavStyle
                )
                SettingsEnumRow(
                    icon = Icons.Default.Layers,
                    title = "Navigation Position",
                    currentValue = prefs.navPosition,
                    onValueChange = viewModel::setNavPosition
                )
            }
        }

        // --- ARTWORK & PERFORMANCE ---
        item {
            SettingsGroup("Artwork & Performance") {
                SettingsEnumRow(
                    icon = Icons.Default.Image,
                    title = "Artwork Quality",
                    currentValue = prefs.artworkQuality,
                    onValueChange = viewModel::setArtworkQuality
                )
                SettingsEnumRow(
                    icon = Icons.Default.Animation,
                    title = "Animation Level",
                    currentValue = prefs.animationLevel,
                    onValueChange = viewModel::setAnimationLevel
                )
            }
        }

        item {
            SettingsGroup("Profile") {
                SettingsRow(
                    icon = Icons.Default.Person,
                    title = "Active Profile",
                    subtitle = "${currentProfile.label} Profile",
                    onClick = { /* Handle cycle via viewModel if needed */ },
                    trailing = {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = currentProfile.iconResId),
                            contentDescription = null,
                            tint = currentProfile.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "Adult Profile PIN",
                    subtitle = "Protect adult content access",
                    trailing = { Switch(checked = prefs.adultPinEnabled,
                                        onCheckedChange = { viewModel.setAdultPin(it) }) }
                )
                if (prefs.adultPinEnabled) {
                    var showPinSetDialog by remember { mutableStateOf(false) }
                    SettingsRow(
                        icon = Icons.Default.Edit,
                        title = "Change PIN",
                        subtitle = if (prefs.adultPin.isBlank()) "Default: 0000" else "••••",
                        onClick = { showPinSetDialog = true }
                    )
                    if (showPinSetDialog) {
                        SetPinDialog(
                            onDismiss = { showPinSetDialog = false },
                            onSave = { 
                                viewModel.setAdultPinValue(it)
                                showPinSetDialog = false
                            }
                        )
                    }
                }
            }
        }

        item {
            SettingsGroup("Playback") {
                SettingsRow(
                    icon = Icons.Default.HighQuality,
                    title = "Default Quality",
                    subtitle = prefs.defaultQuality,
                    trailing = {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded,
                                               onExpandedChange = { expanded = it }) {
                            Text(prefs.defaultQuality, color = Color.White,
                                 modifier = Modifier.menuAnchor())
                            ExposedDropdownMenu(expanded = expanded,
                                                onDismissRequest = { expanded = false }) {
                                listOf("Auto", "4K", "1080p", "720p", "480p").forEach { q ->
                                    DropdownMenuItem(text = { Text(q) }, onClick = {
                                        viewModel.setDefaultQuality(q); expanded = false
                                    })
                                }
                            }
                        }
                    }
                )
                SettingsRow(icon = Icons.Default.SkipNext, title = "Auto-play Next",
                    subtitle = "Automatically play next episode",
                    trailing = { Switch(checked = prefs.autoplayNext,
                                        onCheckedChange = { viewModel.setAutoplay(it) }) })
                SettingsRow(icon = Icons.Default.FastForward, title = "Skip Intro",
                    subtitle = "Auto-skip opening credits when detected",
                    trailing = { Switch(checked = prefs.skipIntro,
                                        onCheckedChange = { viewModel.setSkipIntro(it) }) })
                SettingsRow(icon = Icons.Default.Subtitles, title = "Subtitles",
                    subtitle = if (prefs.subtitlesEnabled) "Enabled · ${prefs.subtitleLanguage}" else "Disabled",
                    trailing = { Switch(checked = prefs.subtitlesEnabled,
                                        onCheckedChange = { viewModel.setSubtitles(it) }) })
            }
        }

        item {
            SettingsGroup("Extensions") {
                SettingsRow(icon = Icons.Default.Update, title = "Auto-update Extensions",
                    subtitle = "Check for updates on app launch",
                    trailing = { Switch(checked = prefs.autoUpdateExtensions,
                                        onCheckedChange = { viewModel.setAutoUpdate(it) }) })
            }
        }

        item {
            SettingsGroup("TMDB") {
                SettingsRow(icon = Icons.Default.Key, title = "API Key",
                    subtitle = if (prefs.tmdbKey.isBlank()) "Not set — tap to add"
                               else "••••••${prefs.tmdbKey.takeLast(4)}",
                    onClick = { /* show dialog */ },
                    trailing = { Icon(Icons.Default.Edit, null,
                                      tint = MaterialTheme.colorScheme.primary) })
            }
        }

        item {
            SettingsGroup("Storage") {
                SettingsRow(icon = Icons.Default.DeleteSweep, title = "Clear Cache",
                    subtitle = "${viewModel.getCacheSizeMb()} MB used",
                    onClick = { viewModel.clearCache() },
                    trailing = { Text("Clear", color = MaterialTheme.colorScheme.error,
                                       fontWeight = FontWeight.SemiBold) })
                SettingsRow(icon = Icons.Default.Wifi, title = "Download over WiFi only",
                    subtitle = "Saves mobile data",
                    trailing = { Switch(checked = prefs.wifiOnly,
                                        onCheckedChange = { viewModel.setWifiOnly(it) }) })
            }
        }

        item {
            Surface(color = AppColors.black010,
                    shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About", style = MaterialTheme.typography.titleSmall,
                         color = Color.White.copy(alpha = 0.6f),
                         fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Streamix v1.0", fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("A modern movie and series streaming application powered by extensions.",
                         fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun SetPinDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Adult Profile PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter a 4-digit PIN to protect access to the adult profile.", fontSize = 13.sp)
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (newPin.length != 4) {
                    error = "PIN must be 4 digits"
                } else if (newPin != confirmPin) {
                    error = "PINs do not match"
                } else {
                    onSave(newPin)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Enum<T>> SettingsEnumRow(
    icon: ImageVector,
    title: String,
    currentValue: T,
    onValueChange: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = currentValue.name.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' '),
        onClick = { expanded = true },
        trailing = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                Box(modifier = Modifier.menuAnchor()) {
                    Icon(Icons.Default.ChevronRight, null)
                }
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    currentValue.javaClass.enumConstants?.forEach { enumValue ->
                        DropdownMenuItem(
                            text = { Text(enumValue.name.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' ')) },
                            onClick = {
                                onValueChange(enumValue)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall,
             color = Color.White.copy(alpha = 0.6f), letterSpacing = 0.08.em,
             modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = AppColors.black010,
                modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

@Composable
fun SettingsRow(
    icon     : ImageVector,
    title    : String,
    subtitle : String = "",
    onClick  : (() -> Unit)? = null,
    trailing : @Composable (() -> Unit)? = null
) {
    val mod = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier = mod.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(34.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                     tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            if (subtitle.isNotBlank())
                Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
        }
        trailing?.invoke()
    }
}
