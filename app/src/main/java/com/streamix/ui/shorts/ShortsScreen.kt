package com.streamix.ui.shorts

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.itemsIndexed as lazyItemsIndexed
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.streamix.ui.theme.LocalCustomColors
import kotlinx.coroutines.delay

import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

import com.streamix.ui.navigation.LocalBottomDockVisible
import com.streamix.core.utils.ShareUtils

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    context:        ShortsContext,
    onClose:        () -> Unit = {},
    isScreenActive: Boolean = true,
    viewModel:      ShortsViewModel = hiltViewModel()
) {
    val items     by viewModel.shorts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    
    val androidContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = LocalCustomColors.current
    val bottomDockVisible = LocalBottomDockVisible.current

    // Hide bottom dock when shorts are active
    LaunchedEffect(isScreenActive) {
        bottomDockVisible.value = !isScreenActive
    }
    
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
        
        ExoPlayer.Builder(androidContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
            else if (event == Lifecycle.Event.ON_RESUME && isScreenActive) exoPlayer.play()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
            // ENSURE orientation is reset to portrait when leaving shorts
            (androidContext as? android.app.Activity)?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(isScreenActive) {
        if (!isScreenActive) exoPlayer.pause() else exoPlayer.play()
    }

    LaunchedEffect(context) { viewModel.load(context) }

    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    
    LaunchedEffect(showControls, isScreenActive, isLocked) {
        if (showControls && isScreenActive && !isLocked) {
            delay(5000)
            showControls = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.primary)
            .pointerInput(isLocked) {
                if (context == ShortsContext.ADULT) {
                    detectTapGestures { } // consume taps
                    // 2 fingers swipe down to lock
                    // Actually, detectTapGestures doesn't handle multi-touch well.
                    // I'll use a more generic pointerInput if needed.
                }
            }
    ) {
        if (isLoading && items.isEmpty()) {
            CircularProgressIndicator(color = colors.tertiary, modifier = Modifier.align(Alignment.Center))
        } else if (items.isEmpty()) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PlayCircleOutline, null, tint = colors.secondary.copy(0.3f), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("No shorts available", color = colors.secondary.copy(0.4f), fontSize = 16.sp)
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { items.size })

            LaunchedEffect(pagerState.currentPage, items.size) {
                if (items.isNotEmpty() && pagerState.currentPage >= items.size - 3) {
                    viewModel.loadMore(context)
                }
            }

            val currentItem = remember(pagerState.currentPage, items) { items.getOrNull(pagerState.currentPage) }
            var currentUri by remember { mutableStateOf("") }
            
            LaunchedEffect(currentItem?.id, currentItem?.streamUrl, isScreenActive) {
                val item = currentItem ?: return@LaunchedEffect
                if (!isScreenActive) {
                    exoPlayer.pause()
                    return@LaunchedEffect
                }

                // Resolve current AND buffer next items
                viewModel.resolveStreamUrl(item.id)

                if (item.streamUrl.isNotEmpty() && currentUri != item.streamUrl) {
                    currentUri = item.streamUrl
                    val videoUrl = item.streamUrl
                    
                    // Support DASH, HLS and MP4
                    val mediaItem = MediaItem.Builder()
                        .setUri(videoUrl)
                        .setMimeType(when {
                            videoUrl.contains(".m3u8") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                            videoUrl.contains(".mpd") || videoUrl.contains("manifest.google-video.com") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                            videoUrl.contains(".mp4") -> androidx.media3.common.MimeTypes.VIDEO_MP4
                            else -> null
                        })
                        .build()

                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                } else if (item.streamUrl.isEmpty()) {
                    if (currentUri.isNotEmpty()) {
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        currentUri = ""
                    }
                }
            }

            VerticalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 0,
                userScrollEnabled = !isLocked
            ) { page ->
                val item = items.getOrNull(page) ?: return@VerticalPager
                val isActive = pagerState.currentPage == page
                
                ShortVideoItem(
                    item      = item,
                    isActive  = isActive && isScreenActive,
                    player    = if (isActive && isScreenActive) exoPlayer else null,
                    onLike    = { viewModel.toggleLike(item.id) },
                    onDislike = { viewModel.toggleDislike(item.id) },
                    context   = context,
                    showControls = showControls && !isLocked,
                    onToggleControls = { if (!isLocked) showControls = !showControls },
                    isLocked = isLocked,
                    onLockToggle = { isLocked = it }
                )
            }

            if (isSearching && context == ShortsContext.YOUTUBE) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.95f)).statusBarsPadding().padding(top = 64.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        lazyItemsIndexed(searchResults, key = { _, item -> item.id }) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { viewModel.playSearchShort(index); isSearching = false },
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(modifier = Modifier.width(160.dp).aspectRatio(16f/9f).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(0.1f))) {
                                    SubcomposeAsyncImage(model = item.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(item.channelName, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(item.views + " views", color = Color.White.copy(0.4f), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                            color = Color.Red,
                            trackColor = Color.Transparent
                        )
                    }

                    if (!isLoading && searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, null, tint = Color.White.copy(0.2f), modifier = Modifier.size(64.dp))
                            Text("No shorts found for \"$searchQuery\"", color = Color.White.copy(0.4f), fontSize = 14.sp)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showControls || isSearching,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                    if (!isSearching) {
                        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart)) {
                            Box(Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape), Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    
                    if (isSearching && context == ShortsContext.YOUTUBE) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { 
                                viewModel.searchShorts(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp)
                                .height(52.dp),
                            placeholder = { Text("Search shorts...", color = Color.White.copy(0.6f)) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { 
                                    viewModel.searchShorts(searchQuery)
                                }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(0.6f),
                                unfocusedContainerColor = Color.Black.copy(0.4f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.Red,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(26.dp),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { 
                                    isSearching = false
                                    viewModel.searchShorts("") // Clear search
                                }) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                            }
                        )
                    } else {
                        Text("Shorts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.Center))

                        Row(modifier = Modifier.align(Alignment.TopEnd)) {
                            if (context == ShortsContext.YOUTUBE) {
                                IconButton(onClick = { isSearching = true }) {
                                    Box(Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape), Alignment.Center) {
                                        Icon(Icons.Default.Search, "Search", tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                            IconButton(
                                onClick = { 
                                    val activity = androidContext as? android.app.Activity
                                    if (activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                        activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                }
                            ) {
                                Box(Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape), Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ShortVideoItem(
    item:      ShortsItem,
    isActive:  Boolean,
    player:    ExoPlayer?,
    onLike:    () -> Unit,
    onDislike: () -> Unit,
    context:   ShortsContext,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    isLocked: Boolean,
    onLockToggle: (Boolean) -> Unit
) {
    val colors = LocalCustomColors.current
    val androidContext = LocalContext.current
    var isPlayerReady by remember(item.id, item.streamUrl) { mutableStateOf(false) }
    var playbackError by remember(item.id, item.streamUrl) { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    
    val activity = androidContext as? android.app.Activity
    val isLandscape = activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    
    var showHeart by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (showHeart) 1.5f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heartScale",
        finishedListener = { if (it > 0f) showHeart = false }
    )

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        isPlayerReady = true
                        playbackError = null
                    }
                    Player.STATE_BUFFERING -> {
                        isPlayerReady = false
                    }
                    Player.STATE_IDLE -> {
                        isPlayerReady = false
                    }
                    Player.STATE_ENDED -> {
                        isPlayerReady = false
                    }
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = "Unable to play video: ${error.message}"
                isPlayerReady = false
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
            isPlayerReady = false
            playbackError = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                if (isLocked) {
                    detectTapGestures { } // Locked
                } else {
                    detectTapGestures(
                        onTap = { onToggleControls() },
                        onDoubleTap = {
                            if (item.isLiked) {
                                onDislike()
                            } else {
                                onLike()
                                showHeart = true
                            }
                        }
                    )
                }
            }
            .pointerInput(isLocked) {
                if (!isLocked && context == ShortsContext.ADULT) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.size == 2) { // 2 fingers
                                val dragAmount = event.changes.map { it.position.y - it.previousPosition.y }.average()
                                if (dragAmount > 50) { // Swipe down
                                    onLockToggle(true)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        if (isActive && player != null) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        this.player = player
                        useController = false
                        resizeMode = if (context == ShortsContext.ADULT || isLandscape) {
                            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } else {
                            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view -> 
                    view.player = player 
                    view.resizeMode = if (context == ShortsContext.ADULT || isLandscape) {
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                }
            )

            if (!isPlayerReady && playbackError == null) {
                SubcomposeAsyncImage(model = item.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color.Red.copy(0.5f))
                }
            }
            
            // Video Controls (Play/Pause, Seek)
            if (showControls && !isLocked) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { player.seekTo(player.currentPosition - 10000) }) {
                            Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                        IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                            Icon(
                                if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                null, tint = Color.White, modifier = Modifier.size(64.dp)
                            )
                        }
                        IconButton(onClick = { player.seekTo(player.currentPosition + 10000) }) {
                            Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                    }
                }
            }
            
            if (isLocked) {
                IconButton(
                    onClick = { onLockToggle(false) },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding()
                ) {
                    Box(Modifier.size(48.dp).background(Color.Black.copy(0.5f), CircleShape), Alignment.Center) {
                        Icon(Icons.Default.LockOpen, null, tint = Color.White)
                    }
                }
            }

            playbackError?.let { err ->
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(err, color = Color.White.copy(0.4f), fontSize = 14.sp)
                    }
                }
            }
            
            var progress by remember { mutableStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }

            LaunchedEffect(player, isPlayerReady, isDragging) {
                while (isActive && isPlayerReady && !isDragging) {
                    val duration = player.duration.coerceAtLeast(1)
                    val current = player.currentPosition
                    progress = current.toFloat() / duration.toFloat()
                    delay(500)
                }
            }
        } else {
            SubcomposeAsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = Color.White.copy(0.2f))
                    }
                }
            )
        }

        if (heartScale > 0f) {
            Icon(Icons.Default.Favorite, null, tint = Color.Red, modifier = Modifier.align(Alignment.Center).size(100.dp).scale(heartScale))
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut()
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)), startY = 800f)))

                // Adjusted UI for landscape
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = if (isLandscape) 40.dp else 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 12.dp)
                ) {
                    ActionButton(icon = if (item.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, label = item.likes, color = if (item.isLiked) Color(0xFFE53935) else Color.White, onClick = onLike)
                    ActionButton(icon = if (item.isDisliked) Icons.Default.ThumbDownAlt else Icons.Default.ThumbDownOffAlt, label = "Dislike", color = if (item.isDisliked) Color.Gray else Color.White, onClick = onDislike)
                    ActionButton(icon = Icons.Default.Share, label = "Share", onClick = { 
                        ShareUtils.shareLink(androidContext, item.title, "https://www.youtube.com/watch?v=${item.id}")
                    })
                }

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 100.dp, bottom = if (isLandscape) 20.dp else 40.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                            Text(item.channelName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(item.channelName, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = if (isLandscape) 1 else 2, overflow = TextOverflow.Ellipsis)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Progression Bar moved below text
                    var progress by remember { mutableStateOf(0f) }
                    var isDragging by remember { mutableStateOf(false) }

                    LaunchedEffect(player, isPlayerReady, isDragging) {
                        while (isActive && isPlayerReady && !isDragging) {
                            val duration = player!!.duration.coerceAtLeast(1)
                            val current = player.currentPosition
                            progress = current.toFloat() / duration.toFloat()
                            delay(500)
                        }
                    }

                    Slider(
                        value = progress,
                        onValueChange = { 
                            isDragging = true
                            progress = it 
                        },
                        onValueChangeFinished = {
                            isDragging = false
                            player?.seekTo((progress * player.duration).toLong())
                        },
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Red,
                            activeTrackColor = Color.Red,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, label: String, color: Color = Color.White, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp).background(Color.White.copy(0.08f), CircleShape)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
