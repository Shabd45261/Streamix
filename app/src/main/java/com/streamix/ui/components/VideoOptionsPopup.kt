package com.streamix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.streamix.core.model.SearchResult
import com.streamix.core.utils.ShareUtils
import com.streamix.ui.theme.LocalCustomColors

@Composable
fun VideoOptionsPopup(
    expanded: Boolean, 
    onDismiss: () -> Unit, 
    onSelect: (String) -> Unit, 
    item: SearchResult
) {
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    var showAddToListSubMenu by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(colors.primary)
    ) {
        DropdownMenuItem(
            text = { Text("Share", color = colors.secondary) },
            onClick = { 
                ShareUtils.shareLink(context, item.title, item.id)
                onDismiss() 
            },
            leadingIcon = { Icon(Icons.Default.Share, null, tint = colors.secondary) }
        )
        DropdownMenuItem(
            text = { Text("Add to List", color = colors.secondary) },
            onClick = { showAddToListSubMenu = true },
            leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = colors.secondary) }
        )
        DropdownMenuItem(
            text = { Text("Watch Later", color = colors.secondary) },
            onClick = { 
                onSelect("Plan to Watch")
                onDismiss() 
            },
            leadingIcon = { Icon(Icons.Default.Schedule, null, tint = colors.secondary) }
        )
    }

    if (showAddToListSubMenu) {
        AlertDialog(
            onDismissRequest = { showAddToListSubMenu = false; onDismiss() },
            title = { Text("Select List", color = colors.secondary) },
            containerColor = colors.primary,
            text = {
                Column {
                    listOf("Watching", "Completed", "On-Hold", "Dropped", "Plan to Watch", "Favorites", "Subscribed").forEach { status ->
                        TextButton(
                            onClick = { 
                                onSelect(status)
                                showAddToListSubMenu = false
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(status, color = colors.secondary)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
