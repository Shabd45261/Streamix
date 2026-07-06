package com.streamix.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamix.core.network.UpdateInfo
import com.streamix.ui.theme.LocalCustomColors

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalCustomColors.current
    
    AlertDialog(
        onDismissRequest = { if (!info.isMandatory) onDismiss() },
        containerColor = colors.primary,
        shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Default.SystemUpdate, null, tint = colors.tertiary, modifier = Modifier.size(48.dp)) },
        title = {
            Text(
                "Update Available", 
                color = colors.secondary, 
                fontSize = 22.sp, 
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column {
                Text(
                    "A new version (${info.latestVersionName}) is ready.", 
                    color = colors.secondary.copy(0.7f),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(16.dp))
                
                Text("What's New:", fontWeight = FontWeight.Bold, color = colors.secondary)
                Text(info.changelog, color = colors.secondary.copy(0.6f), fontSize = 14.sp)
                
                info.instructions?.let { 
                    Spacer(Modifier.height(16.dp))
                    Text("Instructions:", fontWeight = FontWeight.Bold, color = colors.secondary)
                    Text(it, color = colors.secondary.copy(0.6f), fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.updateUrl))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary)
            ) {
                Text("Update Now", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!info.isMandatory) {
                TextButton(onClick = onDismiss) {
                    Text("Maybe Later", color = colors.secondary.copy(0.5f))
                }
            }
        }
    )
}
