package com.streamix.ui.components

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primary)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // New Stylized "S" Logo with Play Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        
                        // Draw "S" shape or similar
                        // For simplicity, let's draw a nice black "S" background and a white triangle
                        drawRect(color = Color.Black, size = size)
                        
                        // Draw Triangle (Play)
                        val path = Path().apply {
                            moveTo(w * 0.35f, h * 0.25f)
                            lineTo(w * 0.75f, h * 0.5f)
                            lineTo(w * 0.35f, h * 0.75f)
                            close()
                        }
                        drawPath(path, color = Color.White, style = Fill)
                    }
                }
                
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Streamix",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        letterSpacing = (-1).sp
                    ),
                    color = colors.secondary,
                    fontWeight = FontWeight.Black
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSettingsTap,
                    modifier = Modifier
                        .size(38.dp)
                        .background(colors.secondary.copy(alpha = 0.08f), CircleShape)
                        .border(0.5.dp, colors.secondary.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings",
                        tint = colors.secondary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                
                Box {
                    ProfileSwitcherButton(
                        currentProfile = currentProfile,
                        onLongPress = { showMenu = true },
                        onSevenTaps = onProfileTripleTap
                    )
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(colors.primary.copy(0.9f))
                    ) {
                        val profiles = listOf(Profile.YOUTUBE, Profile.SONGS, Profile.MOVIES)
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.label, color = colors.secondary) },
                                leadingIcon = { Icon(profile.icon, null, tint = colors.secondary, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    onProfileSelect(profile)
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
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
