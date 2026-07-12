package com.streamix.ui.adult

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.ui.player.PlayerManager
import com.streamix.ui.theme.LocalCustomColors
import com.streamix.ui.movies.DetailTopBar
import com.streamix.core.model.SearchResult
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.delay
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AdultDetailScreen(
    pageUrl: String,
    navController: NavController,
    viewModel: AdultDetailViewModel = hiltViewModel()
) {
    val detail    by viewModel.detail.collectAsState()
    val links     by viewModel.videoLinks.collectAsState()
    val trending  by viewModel.trending.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val initialPos by viewModel.initialPosition.collectAsState()
    
    val colors = LocalCustomColors.current
    var shouldPlayAfterLoad by remember { mutableStateOf(false) }

    val isPlayerVisible by PlayerManager.isVisible
    val isPlayerMinimized by PlayerManager.isMinimized

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF 
            volume = 0f 
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(isPlayerVisible, isPlayerMinimized) {
        if (isPlayerVisible && !isPlayerMinimized) {
            exoPlayer.pause()
        } else {
            if (links.isNotEmpty()) exoPlayer.play()
        }
    }

    LaunchedEffect(links) {
        val streamUrl = links.firstOrNull()?.url ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        exoPlayer.play()
        
        while (true) {
            val duration = exoPlayer.duration
            if (duration > 0) {
                val intervals = listOf(0L, duration / 4, duration / 2, duration * 3 / 4)
                for (startTime in intervals) {
                    exoPlayer.seekTo(startTime)
                    delay(5000)
                }
            } else {
                delay(2000) 
            }
        }
    }

    LaunchedEffect(pageUrl) {
        val decoded = try { URLDecoder.decode(pageUrl, "UTF-8") } catch (_: Exception) { pageUrl }
        viewModel.load(decoded)
    }

    LaunchedEffect(links, shouldPlayAfterLoad) {
        if (shouldPlayAfterLoad && links.isNotEmpty() && detail != null) {
            PlayerManager.play(
                video = com.streamix.core.model.SearchResult(
                    id = pageUrl,
                    title = detail!!.title,
                    posterPath = detail!!.posterUrl,
                    mediaType = "adult",
                    studio = detail!!.studio,
                    description = detail!!.description ?: ""
                ),
                links = links,
                related = trending,
                startPosition = initialPos
            )
            shouldPlayAfterLoad = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        detail?.let { data ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    AdultDetailHeroSection(data, links.firstOrNull()?.url, exoPlayer, isPlayerVisible, isPlayerMinimized)
                }

                item {
                    AdultDetailInfoContent(data, initialPos) {
                        if (links.isNotEmpty()) {
                            PlayerManager.play(
                                video = com.streamix.core.model.SearchResult(
                                    id = pageUrl,
                                    title = data.title,
                                    posterPath = data.posterUrl,
                                    mediaType = "adult",
                                    studio = data.studio,
                                    description = data.description ?: ""
                                ),
                                links = links,
                                related = trending,
                                startPosition = initialPos
                            )
                        } else {
                            shouldPlayAfterLoad = true
                            viewModel.loadLinks()
                        }
                    }
                }

                if (trending.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recommended Videos",
                            color = colors.secondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(trending) { result ->
                                Column(modifier = Modifier.width(160.dp).clickable { 
                                    val encoded = URLEncoder.encode(result.id, "UTF-8")
                                    navController.navigate("adult_detail/$encoded") 
                                }) {
                                    AsyncImage(
                                        model = result.posterPath,
                                        contentDescription = null,
                                        modifier = Modifier.height(90.dp).clip(RoundedCornerShape(8.dp)),
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
        }

        // Fixed Top Bar
        val isLiked by viewModel.isLiked(pageUrl).collectAsState(initial = false)
        DetailTopBar(
            navController = navController,
            title = detail?.title ?: "",
            url = pageUrl,
            isLiked = isLiked,
            onLikeToggle = {
                detail?.let { data ->
                    viewModel.toggleLike(
                        SearchResult(
                            id = pageUrl,
                            title = data.title,
                            posterPath = data.posterUrl,
                            mediaType = "adult",
                            studio = data.studio
                        )
                    )
                }
            }
        )

        if (isLoading && detail == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = colors.tertiary)
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun AdultDetailHeroSection(
    data: com.streamix.scraper.adult.AdultVideoDetail,
    streamUrl: String?,
    exoPlayer: ExoPlayer,
    isPlayerVisible: Boolean,
    isPlayerMinimized: Boolean
) {
    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
        if (streamUrl != null && (!isPlayerVisible || isPlayerMinimized)) {
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
        } else {
            AsyncImage(
                model = data.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(0.7f)),
                        startY = 400f
                    )
                )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdultDetailInfoContent(
    data: com.streamix.scraper.adult.AdultVideoDetail,
    initialPos: Long,
    onPlay: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = data.title,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 32.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = data.views, color = Color.Gray, fontSize = 14.sp)
            if (data.rating.isNotBlank()) {
                Text(text = data.rating, color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = data.description ?: "No description available.",
            color = Color.White.copy(0.9f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (data.tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.tags.forEach { tag ->
                    Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = tag,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        val buttonText = if (initialPos > 0) formatTime(initialPos) else "Play"

        Button(
            onClick = onPlay,
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
    val totalSeconds = millis / 1000 
    val hours = totalSeconds / 3600 
    val minutes = (totalSeconds % 3600) / 60 
    val seconds = totalSeconds % 60 
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}
