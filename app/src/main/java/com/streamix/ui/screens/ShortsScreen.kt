package com.streamix.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.core.plugin.SearchResult
import com.streamix.ui.components.ErrorFullScreen
import com.streamix.ui.components.LoadingShimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.platform.LocalContext
import com.streamix.scrapers.ScraperRegistry
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ShortsScreen(
    navController: NavController,
    startId: String? = null,
    viewModel: ShortsViewModel = hiltViewModel(),
    registry: ScraperRegistry // We need registry to get streams
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = uiState) {
            is ShortsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Loading Shorts...", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is ShortsUiState.Error   -> ErrorFullScreen(state.message) { viewModel.loadShorts() }
            is ShortsUiState.Success -> {
                ShortsPager(state.items, navController, startId, registry)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsPager(
    items: List<SearchResult>,
    navController: NavController,
    startId: String?,
    registry: ScraperRegistry
) {
    val initialPage = remember(items, startId) {
        val index = if (startId != null) items.indexOfFirst { it.id == startId } else 0
        if (index < 0) 0 else index
    }
    
    val pagerState = rememberPagerState(initialPage = initialPage) { items.size }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        // Only play if it's the current page
        val isCurrentPage = pagerState.currentPage == page
        ShortItem(items[page], navController, registry, isCurrentPage)
    }
}

@Composable
fun ShortItem(
    item: SearchResult, 
    navController: NavController, 
    registry: ScraperRegistry,
    isActive: Boolean
) {
    val context = LocalContext.current
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var showLikeAnim by remember { mutableStateOf(false) }
    
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(item.id) {
        val scraper = registry.getScraperById(item.sourceId)
        if (scraper != null) {
            val links = scraper.getStreamLinks(item.id)
            streamUrl = links.firstOrNull()?.url
        }
    }

    LaunchedEffect(streamUrl, isActive) {
        if (isActive && streamUrl != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl!!))
            exoPlayer.prepare()
            exoPlayer.play()
            isLoading = false
        } else {
            exoPlayer.pause()
            exoPlayer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isActive && streamUrl != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder/Thumbnail
            AsyncImage(
                model = item.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (isLoading && isActive) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
            }
        }
        
        // Touch interaction layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            isLiked = true
                            showLikeAnim = true
                        },
                        onTap = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }
                    )
                }
        )

        // Bottom Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
        )

        // Overlay Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Side: Title & Channel Info
            Column(modifier = Modifier.weight(1f).padding(bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.sourceId.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("@${item.sourceId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 2
                )
            }

            // Right Side: Action Buttons (YouTube Style)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                ShortsActionButton(
                    icon = if (isLiked) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbUpOffAlt,
                    label = "281K",
                    color = if (isLiked) Color.Blue else Color.White,
                    onClick = { isLiked = !isLiked; if (isLiked) { isDisliked = false; showLikeAnim = true } }
                )
                ShortsActionButton(
                    icon = if (isDisliked) Icons.Rounded.ThumbDown else Icons.Rounded.ThumbDownOffAlt,
                    label = "Dislike",
                    color = if (isDisliked) Color.Blue else Color.White,
                    onClick = { isDisliked = !isDisliked; if (isDisliked) isLiked = false }
                )
                ShortsActionButton(Icons.Default.Comment, "7,211")
                ShortsActionButton(Icons.Rounded.Share, "Share")
                ShortsActionButton(Icons.Rounded.Autorenew, "58")
            }
        }

        // Like Animation Overlay
        if (showLikeAnim) {
            LaunchedEffect(Unit) {
                delay(800)
                showLikeAnim = false
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Favorite,
                    null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(100.dp).animateScale()
                )
            }
        }
    }
}

@Composable
fun ShortsActionButton(
    icon: ImageVector,
    label: String,
    color: Color = Color.White,
    onClick: () -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        }
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Modifier.animateScale(): Modifier {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    return this.then(Modifier.size((100 * scale).dp))
}
