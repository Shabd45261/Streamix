package com.streamix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import java.net.URLEncoder

@Composable
fun DownloadsScreen(
    navController: NavController,
    viewModel: DownloadsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsState()
    val prefs by settingsViewModel.prefs.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Downloads", style = MaterialTheme.typography.headlineMedium,
             fontWeight = FontWeight.Bold,
             modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp))

        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                       modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.DownloadDone, contentDescription = null,
                         modifier = Modifier.size(64.dp),
                         tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No Downloads Yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Downloaded content will appear here for offline viewing.",
                         fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                         textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadCard(
                        item = item,
                        prefs = prefs,
                        onPlay   = { navController.navigate("player/${URLEncoder.encode(item.id, "UTF-8")}/${item.sourceId}") },
                        onDelete = { viewModel.deleteDownload(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadCard(item: DownloadItem, prefs: StreamixPrefs, onPlay: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (prefs.cornerStyle == CornerStyle.ROUNDED) 10.dp else if (prefs.cornerStyle == CornerStyle.EXTRA_ROUNDED) 18.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 60.dp, height = 85.dp).clip(RoundedCornerShape(if (prefs.cornerStyle == CornerStyle.SHARP) 0.dp else 6.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold,
                     fontSize = if (prefs.fontSize == FontSize.SMALL) 13.sp else 14.sp,
                     maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.quality, fontSize = if (prefs.fontSize == FontSize.SMALL) 11.sp else 12.sp,
                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                     modifier = Modifier.padding(top = 2.dp))
                Text(item.fileSizeLabel, fontSize = if (prefs.fontSize == FontSize.SMALL) 11.sp else 12.sp,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Download progress (if still downloading)
                if (item.status == DownloadStatus.DOWNLOADING) {
                    LinearProgressIndicator(
                        progress = { item.progressPct },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(3.dp)
                                          .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${(item.progressPct * 100).toInt()}%", fontSize = 11.sp,
                         color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 3.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.status == DownloadStatus.COMPLETE) {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play",
                             tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete",
                         tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
