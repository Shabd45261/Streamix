package com.streamix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.streamix.ui.theme.FrostedGlass
import com.streamix.ui.theme.AppColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.core.plugin.ContentType
import com.streamix.core.plugin.Episode
import com.streamix.ui.components.ErrorFullScreen
import com.streamix.ui.components.LoadingShimmer
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun DetailScreen(
    contentId: String,
    sourceId: String,
    title: String? = null,
    poster: String? = null,
    desc: String? = null,
    navController: NavController,
    viewModel: DetailViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs by settingsViewModel.prefs.collectAsState()
    val decodedId = URLDecoder.decode(contentId, "UTF-8")

    LaunchedEffect(decodedId, sourceId) { 
        viewModel.loadDetail(decodedId, sourceId, title, poster, desc) 
    }

    when (val state = uiState) {
        is DetailUiState.Loading -> LoadingShimmer()
        is DetailUiState.Error   -> ErrorFullScreen(state.message) { viewModel.loadDetail(decodedId, sourceId) }
        is DetailUiState.Success -> {
            val detail = state.detail
            var selectedSeason by remember { mutableIntStateOf(0) }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Backdrop
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        AsyncImage(
                            model = detail.backdropUrl ?: detail.posterUrl,
                            contentDescription = detail.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black)
                        ))
                        // Back button
                        FrostedGlass(
                            modifier = Modifier.padding(12.dp).size(40.dp),
                            borderRadius = CircleShape,
                            blur = 20.dp,
                            tint = Color.Black.copy(0.3f)
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                            }
                        }
                    }
                }

                // Title + metadata
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(detail.title, style = MaterialTheme.typography.titleMedium,
                             fontWeight = FontWeight.Bold, color = Color.White,
                             fontSize = if (prefs.fontSize == FontSize.LARGE) 18.sp else if (prefs.fontSize == FontSize.EXTRA_LARGE) 20.sp else 15.sp)
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            detail.year?.let { Text(it.toString(), fontSize = 13.sp,
                                                    color = Color.White.copy(alpha = 0.7f)) }
                            detail.rating?.let {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null,
                                         tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                    Text("%.1f".format(it), fontSize = 13.sp,
                                         color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                        if (detail.genres.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                detail.genres.take(4).forEach { genre ->
                                    Surface(
                                        color = Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(if (prefs.cornerStyle == CornerStyle.SHARP) 2.dp else 4.dp)
                                    ) {
                                        Text(genre, fontSize = 11.sp,
                                             modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                             color = Color.White)
                                    }
                                }
                            }
                        }
                        detail.description?.let {
                            Text(it, fontSize = 13.sp, lineHeight = 19.sp,
                                 color = Color.White.copy(alpha = 0.8f),
                                 modifier = Modifier.padding(top = 12.dp),
                                 maxLines = 5, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Play button (for movies / music)
                if (detail.type == ContentType.MOVIE || detail.type == ContentType.MUSIC) {
                    item {
                        FrostedGlass(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable {
                                    navController.navigate("player/${
                                        URLEncoder.encode(detail.id, "UTF-8")
                                    }/$sourceId")
                                },
                            borderRadius = RoundedCornerShape(12.dp),
                            blur = 20.dp,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            hasBorder = false
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("Watch Now", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }

                // Season selector (for series)
                if (detail.type == ContentType.SERIES && detail.seasons.isNotEmpty()) {
                    item {
                        ScrollableTabRow(
                            selectedTabIndex = selectedSeason,
                            modifier = Modifier.padding(vertical = 8.dp),
                            edgePadding = 16.dp,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedSeason]),
                                    color = Color.White
                                )
                            }
                        ) {
                            detail.seasons.forEachIndexed { index, season ->
                                Tab(
                                    selected = selectedSeason == index,
                                    onClick = { selectedSeason = index },
                                    text = { Text(season.title, color = if (selectedSeason == index) Color.White else Color.White.copy(alpha = 0.5f)) }
                                )
                            }
                        }
                    }

                    // Episode list for selected season
                    val episodes = detail.seasons.getOrNull(selectedSeason)?.episodes ?: emptyList()
                    items(episodes, key = { it.id }) { episode ->
                        EpisodeRow(
                            episode = episode,
                            onClick = {
                                navController.navigate("player/${
                                    URLEncoder.encode(episode.id, "UTF-8")
                                }/$sourceId")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeRow(episode: Episode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or episode number
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            if (episode.thumbUrl != null) {
                AsyncImage(model = episode.thumbUrl, contentDescription = null,
                           contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text("E${episode.episodeNum}", fontWeight = FontWeight.Bold,
                     color = Color.White.copy(alpha = 0.5f))
            }
            // Play icon overlay
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayCircle, contentDescription = null,
                     tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(28.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("${episode.episodeNum}. ${episode.title}",
                 fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                 overflow = TextOverflow.Ellipsis, color = Color.White)
            episode.description?.let {
                Text(it, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f),
                     maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            if (episode.durationMs > 0) {
                Text(formatDuration(episode.durationMs), fontSize = 11.sp,
                     color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 128.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
}

fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
