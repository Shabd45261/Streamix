package com.streamix.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.ui.components.ContentCard
import java.net.URLEncoder

@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val watchlist by viewModel.watchlist.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val prefs by settingsViewModel.prefs.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Watchlist", "Continue Watching")

    Column(modifier = Modifier.fillMaxSize()) {
        Text("My Library", style = MaterialTheme.typography.headlineMedium,
             fontWeight = FontWeight.Bold, color = Color.White,
             modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp))

        TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Color.White) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.5f)) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                if (watchlist.isEmpty()) {
                    EmptyLibraryState(
                        icon = Icons.Default.BookmarkBorder,
                        message = "Your watchlist is empty",
                        subtitle = "Add items from the detail screen to keep track of what you want to watch."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(watchlist, key = { it.id }) { item ->
                            ContentCard(item = item, prefs = prefs, onClick = {
                                navController.navigate("detail/${
                                    URLEncoder.encode(item.id, "UTF-8")
                                }/${item.sourceId}")
                            })
                        }
                    }
                }
            }
            1 -> {
                if (continueWatching.isEmpty()) {
                    EmptyLibraryState(
                        icon = Icons.Default.History,
                        message = "Nothing to continue",
                        subtitle = "Items you've started watching will appear here."
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(12.dp),
                               verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(continueWatching, key = { it.item.id }) { entry ->
                            ContinueWatchingCard(entry = entry, onClick = {
                                navController.navigate("player/${
                                    URLEncoder.encode(entry.item.id, "UTF-8")
                                }/${entry.item.sourceId}")
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(entry: ContinueWatchingEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = entry.item.posterUrl,
                contentDescription = entry.item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 70.dp, height = 100.dp).clip(RoundedCornerShape(6.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.item.title, fontWeight = FontWeight.SemiBold,
                     maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color.White)
                Text("${(entry.progressPct * 100).toInt()}% watched",
                     fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f),
                     modifier = Modifier.padding(top = 4.dp))
                LinearProgressIndicator(
                    progress = { entry.progressPct },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline
                )
                Text("Resume", color = MaterialTheme.colorScheme.primary,
                     fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                     modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun EmptyLibraryState(icon: ImageVector, message: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
               modifier = Modifier.padding(32.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp),
                 tint = Color.White.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Text(message, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text(subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f),
                 textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
