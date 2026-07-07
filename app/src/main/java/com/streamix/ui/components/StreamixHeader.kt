package com.streamix.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamix.core.model.Profile
import com.streamix.ui.theme.LocalCustomColors

@Composable
fun StreamixHeader(
    currentProfile: Profile,
    onSettingsTap: () -> Unit,
    onProfileSelect: (Profile) -> Unit,
    onProfileTripleTap: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val colors = LocalCustomColors.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color.Black.copy(alpha = 0.9f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile Switcher on the left like Image 4
                Box {
                    ProfileSwitcherButton(
                        currentProfile = currentProfile,
                        onLongPress = { showMenu = true },
                        onSevenTaps = onProfileTripleTap
                    )
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.Black.copy(0.9f))
                    ) {
                        val profiles = listOf(Profile.YOUTUBE, Profile.SONGS, Profile.MOVIES)
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.label, color = Color.White) },
                                leadingIcon = { Icon(profile.icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    onProfileSelect(profile)
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Streamix",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }

            IconButton(
                onClick = onSettingsTap,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings",
                    tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProfileSwitcherButton(
    currentProfile: Profile,
    onLongPress: () -> Unit,
    onSevenTaps: () -> Unit
) {
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    val tapWindow = 600L
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .size(38.dp)
            .background(colors.secondary.copy(alpha = 0.08f), CircleShape)
            .border(0.5.dp, colors.secondary.copy(alpha = 0.12f), CircleShape)
            .combinedClickable(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < tapWindow) {
                        tapCount++
                    } else {
                        tapCount = 1
                    }
                    lastTapTime = now
                    if (tapCount >= 7) {
                        tapCount = 0
                        onSevenTaps()
                    }
                },
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = currentProfile.icon,
            contentDescription = currentProfile.label,
            tint = colors.secondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
