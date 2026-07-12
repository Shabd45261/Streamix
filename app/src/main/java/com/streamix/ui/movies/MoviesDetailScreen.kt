package com.streamix.ui.movies

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.core.utils.*
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.streamix.scraper.cloudstream.Episode
import com.streamix.scraper.cloudstream.TvType
import com.streamix.ui.player.PlayerManager
import com.streamix.ui.theme.LocalCustomColors
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout

@Composable
fun MoviesDetailScreen(
    navController: NavController,
    movieId: String,
    apiName: String,
    fallbackUrl: String? = null,
    viewModel: MoviesDetailViewModel = hiltViewModel()
) {
    val movie by viewModel.currentMovie.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val selectedEpisode by viewModel.selectedEpisode.collectAsState()
    val seasons by viewModel.seasons.collectAsState()
    val links by viewModel.videoLinks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val trailerLinks by viewModel.trailerLinks.collectAsState()
    
    val colors = LocalCustomColors.current

    val isPlayerVisible by PlayerManager.isVisible
    val isPlayerMinimized by PlayerManager.isMinimized
    
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 1f
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(isPlayerVisible, isPlayerMinimized) {
        if (isPlayerVisible && !isPlayerMinimized) {
            exoPlayer.pause()
            exoPlayer.volume = 0f
        } else {
            exoPlayer.volume = 1f
            if (trailerLinks.isNotEmpty()) exoPlayer.play()
        }
    }

    LaunchedEffect(trailerLinks) {
        val link = trailerLinks.firstOrNull()?.url
        if (link != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(link))
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    LaunchedEffect(movieId, apiName, fallbackUrl) {
        viewModel.load(movieId, apiName, fallbackUrl)
    }

    LaunchedEffect(links) {
        if (links.isNotEmpty()) {
            val current = movie ?: return@LaunchedEffect
            val title = if (current.type == TvType.TvSeries) {
                "${current.name} - S${selectedEpisode?.season}E${selectedEpisode?.episode} ${selectedEpisode?.name ?: ""}"
            } else {
                current.name
            }
            
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            
            PlayerManager.play(
                video = SearchResult(
                    id = movieId,
                    title = title,
                    posterPath = current.posterUrl,
                    mediaType = if (current.type == TvType.TvSeries) "tv" else "movie",
                    description = current.plot ?: "",
                    studio = current.apiName
                ),
                links = links,
                related = emptyList(),
                eps = episodes,
                startPosition = viewModel.startPosition,
                episode = selectedEpisode
            )
            viewModel.clearLinks()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        movie?.let { data ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    DetailHeroSection(data, trailerLinks, exoPlayer, isPlayerVisible, isPlayerMinimized)
                }

                item {
                    val history by viewModel.historyItem.collectAsState()
                    DetailInfoContent(data, selectedEpisode, history) { episode, pos ->
                        episode?.let { viewModel.selectEpisode(it, pos) }
                            ?: viewModel.loadLinks(data.dataUrl, data.apiName, data.name, null, pos)
                    }
                }

                if (data.type == TvType.TvSeries && seasons.isNotEmpty()) {
                    item {
                        SeriesSelectorRow(
                            seasons = seasons,
                            selectedSeason = selectedSeason,
                            totalEpisodes = episodes.filter { it.season == selectedSeason }.size,
                            onSeasonSelect = { viewModel.selectSeason(it) }
                        )
                    }

                    val seasonEpisodes = episodes.filter { it.season == selectedSeason }
                    items(seasonEpisodes) { episode ->
                        EpisodeCardItem(
                            episode = episode,
                            loadResponse = data,
                            onClick = { viewModel.selectEpisode(episode) }
                        )
                    }
                } else if (viewModel.relatedVideos.value.isNotEmpty()) {
                    item {
                        Text(
                            text = "Related Movies",
                            color = colors.secondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(viewModel.relatedVideos.value) { result ->
                                Column(modifier = Modifier.width(110.dp).clickable { viewModel.load(result.id, result.studio, result.id) }) {
                                    AsyncImage(
                                        model = result.posterPath,
                                        contentDescription = null,
                                        modifier = Modifier.height(160.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Text(
                                        text = result.title,
                                        color = colors.secondary,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Bookmark Box at bottom right (as seen in Image 3)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .background(Color(0xFF1C1C1C), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(24.dp))
                    .clickable { /* Open bookmark options */ }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BookmarkBorder, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("None", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Fixed Top Bar
        val isLiked by viewModel.isLiked(movieId).collectAsState(initial = false)
        DetailTopBar(
            navController = navController,
            title = movie?.name ?: "",
            url = movie?.dataUrl ?: "",
            isLiked = isLiked,
            onLikeToggle = {
                movie?.let { data ->
                    viewModel.toggleLike(
                        SearchResult(
                            id = movieId,
                            title = data.name,
                            posterPath = data.posterUrl,
                            mediaType = if (data.type == TvType.TvSeries) "tv" else "movie",
                            studio = data.apiName
                        )
                    )
                }
            }
        )

        if (isLoading && movie == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = colors.tertiary)
        } else if (!isLoading && movie == null) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ErrorOutline, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Failed to load details", color = Color.Gray)
                Button(onClick = { viewModel.load(movieId, apiName, fallbackUrl) }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun DetailTopBar(
    navController: NavController,
    title: String,
    url: String,
    isLiked: Boolean,
    onLikeToggle: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
        }
        
        Row {
            IconButton(onClick = { 
                Toast.makeText(context, "Looking for casting devices...", Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Default.Cast, null, tint = Color.White) }
            
            IconButton(onClick = { 
                ReminderUtils.showReminderPicker(context, title)
            }) { Icon(Icons.Default.NotificationsNone, null, tint = Color.White) }
            
            IconButton(onClick = onLikeToggle) { 
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    null, 
                    tint = if (isLiked) Color.Red else Color.White
                ) 
            }
            
            IconButton(onClick = { 
                ShareUtils.shareLink(context, title, url)
            }) { Icon(Icons.Default.Share, null, tint = Color.White) }
            
            IconButton(onClick = { 
                if (url.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            }) { Icon(Icons.Default.Language, null, tint = Color.White) }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun DetailHeroSection(
    data: com.streamix.scraper.cloudstream.LoadResponse,
    trailerLinks: List<VideoLink>,
    exoPlayer: ExoPlayer,
    isPlayerVisible: Boolean,
    isPlayerMinimized: Boolean
) {
    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        // Poster as background layer
        AsyncImage(
            model = data.backgroundUrl ?: data.posterUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (trailerLinks.isNotEmpty() && (!isPlayerVisible || isPlayerMinimized)) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(0.5f), Color.Black),
                        startY = 300f
                    )
                )
        )
        
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (!data.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = data.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.height(100.dp).widthIn(max = 260.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomStart
                )
            } else {
                Text(
                    text = data.name,
                    color = Color.White,
                    fontSize = 32.sp, // Reduced slightly for better fit
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailInfoContent(
    data: com.streamix.scraper.cloudstream.LoadResponse,
    selectedEpisode: Episode?,
    history: com.streamix.core.storage.WatchHistoryEntity?,
    onPlay: (Episode?, Long) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Metadata Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val metaInfo = mutableListOf<String>()
            metaInfo.add(if (data.type == TvType.TvSeries) "Series" else "Movie")
            data.year?.let { metaInfo.add(it.toString()) }
            if (data.score != null) {
                metaInfo.add("${data.score!!.toDouble() / 10.0}/10.0")
            }
            
            Text(
                text = metaInfo.joinToString("   "),
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Plot
        var isExpanded by remember { mutableStateOf(false) }
        Text(
            text = data.plot ?: "No description available.",
            color = Color.White.copy(0.9f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            maxLines = if (isExpanded) 100 else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(bottom = 24.dp)
        )

        // Cast Section
        if (!data.actors.isNullOrEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp), 
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(data.actors!!) { actorData ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        modifier = Modifier.width(90.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(actorData.actor.image)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF1C1C1C)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = actorData.actor.name, 
                            color = Color.White, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, 
                            maxLines = 1, 
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = actorData.roleString ?: "", 
                            color = Color.Gray, 
                            fontSize = 11.sp, 
                            textAlign = TextAlign.Center, 
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Genres Section
        if (!data.tags.isNullOrEmpty()) {
            FlowRow(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.tags!!.forEach { tag ->
                    Surface(
                        color = Color.White.copy(0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tag,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Main Play Button
        val buttonText = remember(data, selectedEpisode, history) {
            if (data.type == TvType.TvSeries) {
                if (history?.lastEpisodeName != null) {
                    "S${history.lastEpisodeSeason}:E${history.lastEpisodeNumber} ${history.lastEpisodeName}"
                } else if (selectedEpisode != null) {
                    "S${selectedEpisode.season}:E${selectedEpisode.episode} ${selectedEpisode.name ?: ""}" 
                } else "Play"
            } else {
                if (history != null && history.progress > 0) {
                    formatTime(history.progress)
                } else "Play"
            }
        }

        Button(
            onClick = {
                if (data.type == TvType.TvSeries && history?.lastEpisodeData != null) {
                    // Try to find the episode in current list
                    val episode = (com.streamix.scraper.cloudstream.utils.AppUtils.tryParseJson<List<Episode>>(data.dataUrl) ?: emptyList())
                        .find { it.data == history.lastEpisodeData }
                    onPlay(episode, history.progress)
                } else {
                    onPlay(selectedEpisode, history?.progress ?: 0L)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2E4)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = buttonText, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

private fun formatTime(millis: Long): String { 
    val totalSeconds = millis / 1000; 
    val hours = totalSeconds / 3600; 
    val minutes = (totalSeconds % 3600) / 60; 
    val seconds = totalSeconds % 60; 
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

@Composable
fun SeriesSelectorRow(
    seasons: List<Int>,
    selectedSeason: Int,
    totalEpisodes: Int,
    onSeasonSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Season Dropdown
        Box {
            Surface(
                onClick = { expanded = true },
                color = Color(0xFF1C1C1C),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Season $selectedSeason", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF1C1C1C))) {
                seasons.forEach { s ->
                    DropdownMenuItem(
                        text = { Text("Season $s", color = Color.White) },
                        onClick = {
                            onSeasonSelect(s)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Ep Selector
        Surface(
            color = Color(0xFF1C1C1C),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Ep ↑", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        IconButton(onClick = { /* Download All */ }) {
            Icon(Icons.Default.FileDownload, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }

        Spacer(Modifier.weight(1f))

        Text(text = "$totalEpisodes Episodes", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EpisodeCardItem(
    episode: Episode,
    loadResponse: com.streamix.scraper.cloudstream.LoadResponse,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail with Play Icon
            Box(modifier = Modifier.width(140.dp).aspectRatio(16/9f).clip(RoundedCornerShape(8.dp))) {
                AsyncImage(
                    model = episode.posterUrl ?: loadResponse.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                Surface(
                    color = Color.Black.copy(0.4f),
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.Center).size(36.dp),
                    border = BorderStroke(1.dp, Color.White)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
                Text(
                    text = "${episode.episode}. ${episode.name ?: "Episode ${episode.episode}"}", 
                    color = Color.White, 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episode.date ?: "April 11, 2024", 
                    color = Color.Gray, 
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            IconButton(onClick = { /* Download Episode */ }) {
                Icon(Icons.Outlined.FileDownload, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        
        Text(
            text = episode.description ?: "Episode description unavailable.",
            color = Color.Gray,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
