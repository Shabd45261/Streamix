package com.streamix.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamix.core.ProfileManager
import com.streamix.ui.theme.AppColors
import kotlinx.coroutines.delay

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StreamixTopBar(
    onSearchClick: () -> Unit,
    profile: ProfileManager.Profile,
    onProfileCycle: () -> Unit,
    onProfileSelect: (ProfileManager.Profile) -> Unit,
    onRefresh: (() -> Unit)? = null,
    isSearchScreen: Boolean = false,
    searchQuery: String = "",
    onQueryChange: (String) -> Unit = {}
) {
    var lastTapTime by remember { mutableStateOf(0L) }
    var showProfileToast by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (showProfileToast) {
        LaunchedEffect(showProfileToast) {
            delay(1200)
            showProfileToast = false
        }
    }

    CenterAlignedTopAppBar(
        title = {
            if (isSearchScreen) {
                // Real Search TextField when on Search Screen
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 4.dp),
                    placeholder = { Text("Search...", color = Color.White.copy(0.4f), fontSize = 15.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(0.4f)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }
                    },
                    shape = RoundedCornerShape(26.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(0.2f),
                        unfocusedBorderColor = Color.White.copy(0.1f),
                        cursorColor = Color.White,
                        focusedContainerColor = Color.White.copy(0.05f),
                        unfocusedContainerColor = Color.White.copy(0.05f)
                    )
                )
            } else {
                // Transparent Search Button elsewhere
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { onSearchClick() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Search...", 
                            color = Color.White.copy(alpha = 0.5f), 
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
        },
        actions = {
            // Profile Icon on the right
            Box(modifier = Modifier.padding(end = 8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(profile.color)
                        .combinedClickable(
                            onClick = {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTime < 400) {
                                    onProfileCycle()
                                    showProfileToast = true
                                }
                                lastTapTime = now
                            },
                            onLongClick = {
                                showMenu = true
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = profile.iconResId),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(AppColors.black010).width(160.dp)
                ) {
                    ProfileManager.Profile.entries.forEach { p ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = p.iconResId),
                                        contentDescription = null,
                                        tint = p.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(p.label, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            },
                            onClick = {
                                onProfileSelect(p)
                                showMenu = false
                                showProfileToast = true
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent, // Make it blend with background
            titleContentColor = Color.White
        ),
        modifier = Modifier.statusBarsPadding()
    )

    if (showProfileToast) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Surface(
                color = profile.color.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(top = 64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = profile.iconResId),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${profile.label} Profile",
                        color = Color.White, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
