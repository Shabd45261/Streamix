package com.streamix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streamix.core.model.UpdateInfo
import com.streamix.ui.theme.LocalCustomColors

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onDownload: () -> Unit,
    onIgnore: () -> Unit,
    onExit: () -> Unit
) {
    val colors = LocalCustomColors.current
    
    AlertDialog(
        onDismissRequest = { if (!info.mandatory) onIgnore() },
        containerColor = colors.primary,
        shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Default.SystemUpdate, null, tint = colors.tertiary, modifier = Modifier.size(48.dp)) },
        title = {
            Text(
                "New Update Available", 
                color = colors.secondary, 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Version: ${info.versionName}", 
                    color = colors.secondary.copy(0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))
                
                Text("What's New:", fontWeight = FontWeight.Bold, color = colors.secondary, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(info.releaseNotes) { note ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("• ", color = colors.tertiary)
                            Text(note, color = colors.secondary.copy(0.7f), fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Download", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!info.mandatory) {
                TextButton(onClick = onIgnore) {
                    Text("Ignore", color = colors.secondary.copy(0.6f))
                }
            } else {
                TextButton(onClick = onExit) {
                    Text("Exit App", color = colors.secondary.copy(0.6f))
                }
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !info.mandatory,
            dismissOnClickOutside = !info.mandatory
        )
    )
}

@Composable
fun DownloadProgressDialog(
    progress: Int,
    onCancel: () -> Unit
) {
    val colors = LocalCustomColors.current
    
    Dialog(onDismissRequest = { }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.primary,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Downloading Update...", 
                    color = colors.secondary, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(24.dp))
                
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = colors.tertiary,
                    trackColor = colors.tertiary.copy(0.2f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    "$progress%", 
                    color = colors.secondary.copy(0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(Modifier.height(24.dp))
                
                TextButton(onClick = onCancel) {
                    Text("Run in Background", color = colors.tertiary)
                }
            }
        }
    }
}
