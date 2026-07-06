package com.streamix.ui.movies

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.cloudstream.Episode
import com.streamix.scraper.cloudstream.TvType
import com.streamix.scraper.cloudstream.utils.AppUtils.tryParseJson
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
    val history by viewModel.historyItem.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val selectedEpisode by viewModel.selectedEpisode.collectAsState()
    val seasons by viewModel.seasons.collectAsState()
    val links by viewModel.videoLinks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val trailerLinks by viewModel.trailerLinks.collectAsState()
    
    val context = LocalContext.current

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
                eps = episodes
            )
            viewModel.clearLinks()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        movie?.let { data ->
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    DetailHeroSection(data, trailerLinks)
                }

                item {
                    DetailInfoContent(data, history, selectedEpisode) {
                        selectedEpisode?.let { viewModel.selectEpisode(it) } ?: viewModel.loadLinks(data.dataUrl, data.apiName, data.name)
                    }
                }

                // Removed DetailActionSection item here

                if (data.type == TvType.TvSeries && seasons.isNotEmpty()) {
                    item {
                        SeriesSelectorRow(
                            seasons = seasons,
                            selectedSeason = selectedSeason,
                            selectedEpisode = selectedEpisode,
                            onSeasonSelect = { viewModel.selectSeason(it) }
                        )
                    }

                    val seasonEpisodes = episodes.filter { it.season == selectedSeason }
                    item {
                        Text(
                            text = "${seasonEpisodes.size} Episodes",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    items(seasonEpisodes) { episode ->
                        EpisodeCardItem(
                            episode = episode,
                            loadResponse = data,
                            onClick = { viewModel.selectEpisode(episode) }
                        )
                    }
                } else if (!viewModel.relatedVideos.value.isEmpty()) {
                    item {
                        Text(
                            text = "Related Movies",
                            color = Color.White,
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
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(100.dp))
                }
            }

            // Bookmark indicator at bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .background(Color(0xFF1C1C1C), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BookmarkBorder, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("None", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Fixed Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            
            Row {
                IconButton(onClick = { /* Cast */ }) {
                    Icon(Icons.Default.Cast, contentDescription = null, tint = Color.White)
                }
                if (movie?.type == TvType.TvSeries) {
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = Color.White)
                    }
                }
                IconButton(onClick = { /* Heart */ }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = { /* Share */ }) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = { /* Browser */ }) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = { /* Search */ }) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                }
            }
        }

        if (isLoading && movie == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Red)
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
fun DetailHeroSection(data: com.streamix.scraper.cloudstream.LoadResponse, trailerLinks: List<VideoLink>) {
    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
        if (trailerLinks.isNotEmpty()) {
            DetailTrailerPlayer(trailerLinks)
        } else {
            AsyncImage(
                model = data.backgroundUrl ?: data.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f), Color.Black),
                        startY = 600f
                    )
                )
        )
        
        // Title or Logo overlay
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomStart) {
            if (!data.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = data.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.height(100.dp).widthIn(max = 240.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomStart
                )
            } else {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun DetailTrailerPlayer(links: List<VideoLink>) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 1f // UNMUTED as requested
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(links) {
        val link = links.firstOrNull()?.url ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(link))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun DetailInfoContent(
    data: com.streamix.scraper.cloudstream.LoadResponse,
    history: com.streamix.core.storage.WatchHistoryEntity?,
    selectedEpisode: Episode?,
    onPlay: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Metadata row
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (data.score != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${data.score!!.toDouble() / 10.0}", 
                        color = Color.White, 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Text(text = data.year?.toString() ?: "", color = Color.Gray, fontSize = 14.sp)
            
            if (data.duration != null && data.type == TvType.Movie) {
                val h = data.duration!! / 60
                val m = data.duration!! % 60
                Text(
                    text = if (h > 0) "${h}h ${m}m" else "${m}m", 
                    color = Color.Gray, 
                    fontSize = 14.sp
                )
            }

            Surface(
                color = Color.Red.copy(0.1f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color.Red.copy(0.5f))
            ) {
                Text(
                    text = if (data.type == TvType.TvSeries) "SERIES" else "MOVIE",
                    color = Color.Red,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Action Buttons Row (Cloudstream Style)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val buttonText = if (data.type == TvType.TvSeries) {
                if (selectedEpisode != null) "S${selectedEpisode.season}:E${selectedEpisode.episode}" 
                else "Play"
            } else {
                if (history != null && history.progress > 0) "Resume" else "Play"
            }

            DetailActionButton(
                icon = Icons.Default.PlayArrow,
                label = buttonText,
                containerColor = Color.White,
                contentColor = Color.Black,
                modifier = Modifier.weight(1.5f),
                onClick = onPlay
            )
            
            DetailActionButton(
                icon = Icons.Default.FileDownload,
                label = "Download",
                containerColor = Color(0xFF1C1C1C),
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = { /* Download */ }
            )

            DetailActionButton(
                icon = Icons.Default.Add,
                label = "My List",
                containerColor = Color(0xFF1C1C1C),
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = { /* Add to List */ }
            )
        }

        // Plot
        var isExpanded by remember { mutableStateOf(false) }
        Text(
            text = data.plot ?: "No description available.",
            color = Color.White.copy(0.7f),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            maxLines = if (isExpanded) 100 else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(bottom = 24.dp)
        )

        // Cast Section
        if (!data.actors.isNullOrEmpty()) {
            Text(
                text = "Cast",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp), 
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(data.actors!!) { actorData ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        modifier = Modifier.width(70.dp)
                    ) {
                        AsyncImage(
                            model = actorData.actor.image,
                            contentDescription = null,
                            modifier = Modifier.size(70.dp).clip(CircleShape).background(Color(0xFF1C1C1C)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = actorData.actor.name, 
                            color = Color.White, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center, 
                            maxLines = 2, 
                            modifier = Modifier.padding(top = 8.dp),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = label, color = contentColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun DetailActionSection(
    data: com.streamix.scraper.cloudstream.LoadResponse,
    history: com.streamix.core.storage.WatchHistoryEntity?,
    selectedEpisode: Episode?,
    onPlay: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Play Button (Large Pill)
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            
            val buttonText = if (data.type == TvType.TvSeries) {
                if (selectedEpisode != null) "S${selectedEpisode.season}:E${selectedEpisode.episode} ${selectedEpisode.name ?: ""}" 
                else "Play"
            } else {
                if (history != null && history.progress > 0) {
                    val remaining = history.totalDuration - history.progress
                    val h = remaining / (1000 * 60 * 60)
                    val m = (remaining / (1000 * 60)) % 60
                    if (h > 0) "${h}h ${m}m remaining" else "${m}m remaining"
                } else "Play"
            }
            
            Text(
                text = buttonText,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Download Button
        Button(
            onClick = { /* Download */ },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1C)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(text = "Download", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SeriesSelectorRow(
    seasons: List<Int>,
    selectedSeason: Int,
    selectedEpisode: Episode?,
    onSeasonSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Season Pill
        Box {
            Surface(
                onClick = { expanded = true },
                color = Color(0xFF1C1C1C),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.DarkGray)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Season $selectedSeason", color = Color.White, fontSize = 14.sp)
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

        // Episode Pill
        Surface(
            color = Color(0xFF1C1C1C),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Ep ${selectedEpisode?.episode ?: 1}", color = Color.White, fontSize = 14.sp)
                Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.weight(1f))

        IconButton(
            onClick = { /* Batch download */ },
            modifier = Modifier.clip(CircleShape).background(Color(0xFF1C1C1C))
        ) {
            Icon(Icons.Default.FileDownload, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
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
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail
            Box(modifier = Modifier.width(150.dp).height(85.dp).clip(RoundedCornerShape(8.dp))) {
                AsyncImage(
                    model = episode.posterUrl ?: loadResponse.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(36.dp)
                )
            }
            
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = "${episode.episode}. ${episode.name ?: "Episode ${episode.episode}"}", 
                    color = Color.White, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = episode.date ?: "2024", 
                    color = Color.Gray, 
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            IconButton(onClick = { /* Download episode */ }) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        
        Text(
            text = episode.description ?: loadResponse.plot ?: "No description available.",
            color = Color.LightGray.copy(alpha = 0.7f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
