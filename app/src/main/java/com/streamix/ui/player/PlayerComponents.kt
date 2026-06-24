package com.streamix.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.streamix.core.model.VideoLink
import com.streamix.core.utils.UrlUtils
import com.streamix.ui.theme.LocalCustomColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

import com.streamix.ui.navigation.LocalBottomDockVisible

@OptIn(UnstableApi::class)
@Composable
fun EmbeddedPlayer(
    id: String = "",
    links: List<VideoLink>,
    posterUrl: String? = null,
    modifier: Modifier = Modifier,
    isPlayingInitially: Boolean = false,
    initialPosition: Long = 0L,
    onFullScreenToggle: (Boolean) -> Unit = {},
    onProgressUpdate: (Long, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val colors = LocalCustomColors.current
    val bottomDockVisible = LocalBottomDockVisible.current
    
    var currentLinkIndex by remember { mutableIntStateOf(0) }
    val currentLink = links.getOrNull(currentLinkIndex)
    val videoUrl = currentLink?.url ?: ""

    // Orientation state
    val activity = context as? Activity
    val isLandscape = activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    
    LaunchedEffect(isLandscape) {
        bottomDockVisible.value = !isLandscape
        val window = (context as? Activity)?.window
        window?.let {
            val controller = WindowCompat.getInsetsController(it, view)
            if (isLandscape) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val exoPlayer = remember(links) {
        val referer = when {
            id.startsWith("http") -> id
            videoUrl.isNotBlank() -> UrlUtils.getDomain(videoUrl)
            else -> "https://www.google.com/"
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf(
                "Referer" to referer,
                "Origin" to UrlUtils.getDomain(referer),
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "cross-site"
            ))

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    var isPlaying by remember { mutableStateOf(isPlayingInitially) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var showControls by remember { mutableStateOf(true) }
    var hasStarted by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    var volume by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(1f) }
    var showGestureIndicator by remember { mutableStateOf<GestureIndicatorType?>(null) }
    
    var currentPosition by remember { mutableLongStateOf(initialPosition) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showSettings by remember { mutableStateOf(false) }

    val thumbUrl = remember(posterUrl, videoUrl) {
        UrlUtils.resolveImageUrl(posterUrl, "adult", videoUrl)
    }

    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotEmpty()) {
            isBuffering = true
            playbackError = null
            
            val mimeType = when {
                videoUrl.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
                videoUrl.contains(".mp4") -> MimeTypes.VIDEO_MP4
                else -> null
            }
            
            val mediaItem = if (mimeType != null) {
                MediaItem.Builder().setUri(videoUrl).setMimeType(mimeType).build()
            } else {
                MediaItem.fromUri(videoUrl)
            }

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (currentPosition > 0) exoPlayer.seekTo(currentPosition)
            
            if (isPlayingInitially || hasStarted) {
                exoPlayer.playWhenReady = true
                isPlaying = true
                hasStarted = true
            }
        }
    }

    LaunchedEffect(hasStarted, isPlaying) {
        while (hasStarted && isPlaying) {
            val current = exoPlayer.currentPosition
            val total = exoPlayer.duration
            
            currentPosition = current
            bufferedPosition = exoPlayer.bufferedPosition
            
            if (total > 0 && total != androidx.media3.common.C.TIME_UNSET) {
                duration = total
                onProgressUpdate(current, total)
            }
            delay(1000)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    isBuffering = false
                    duration = exoPlayer.duration.coerceAtLeast(0)
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = "Error: ${error.errorCodeName}"
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer.play()
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.stop()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
            
            // RESET orientation, system UI and dock visibility
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let {
                val controller = WindowCompat.getInsetsController(it, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            bottomDockVisible.value = true
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(5000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (!isLocked) showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val isForward = offset.x > size.width / 2
                            if (isForward) {
                                exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                            } else {
                                exoPlayer.seekTo(exoPlayer.currentPosition - 10000)
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        if (!isLocked) {
                            val isLeft = change.position.x < size.width / 2
                            if (isLeft) {
                                brightness = (brightness - dragAmount / 500f).coerceIn(0f, 1f)
                                showGestureIndicator = GestureIndicatorType.BRIGHTNESS
                            } else {
                                volume = (volume - dragAmount / 500f).coerceIn(0f, 1f)
                                exoPlayer.volume = volume
                                showGestureIndicator = GestureIndicatorType.VOLUME
                            }
                        }
                    },
                    onDragEnd = { showGestureIndicator = null }
                )
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize().rotate(rotation),
            update = { view ->
                view.player = exoPlayer
                if (view.resizeMode != resizeMode) {
                    view.resizeMode = resizeMode
                    view.requestLayout()
                }
            }
        )

        // Loading/Error Overlay
        if (!hasStarted || isBuffering || playbackError != null) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = if (playbackError != null) 0.5f else 1f
                )
                
                if (isBuffering && hasStarted) {
                    LoadingAnimation(colors.tertiary)
                }
                
                playbackError?.let { err ->
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(err, color = Color.White, fontSize = 14.sp)
                        Button(
                            onClick = { 
                                exoPlayer.prepare()
                                exoPlayer.play()
                                playbackError = null
                                hasStarted = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Retry", color = colors.primary)
                        }
                    }
                }
            }
        }

        // Gesture Indicators
        showGestureIndicator?.let { type ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (type == GestureIndicatorType.VOLUME) Icons.Default.VolumeUp else Icons.Default.Brightness6,
                        null, tint = Color.White, modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    val value = if (type == GestureIndicatorType.VOLUME) volume else brightness
                    LinearProgressIndicator(
                        progress = { value },
                        modifier = Modifier.width(100.dp).height(4.dp),
                        color = colors.tertiary,
                        trackColor = Color.White.copy(0.3f)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isLocked) 0f else 0.4f))
            ) {
                // Lock Button - Moved to not overlap with back button
                IconButton(
                    onClick = { isLocked = !isLocked },
                    modifier = Modifier
                        .align(Alignment.CenterStart) // Moved to center left
                        .padding(16.dp)
                        .background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(
                        if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        null, tint = if (isLocked) colors.tertiary else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (!isLocked) {
                    // Top Bar
                    Row(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { 
                            val activity = context as? Activity
                            if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        }, modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)) {
                            Icon(Icons.Default.RotateRight, null, tint = Color.White)
                        }
                        
                        IconButton(onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }, modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)) {
                            Icon(Icons.Default.AspectRatio, null, tint = Color.White)
                        }

                        IconButton(onClick = { showSettings = true }, modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)) {
                            Icon(Icons.Default.Settings, null, tint = Color.White)
                        }
                        
                        IconButton(onClick = { 
                            exoPlayer.stop()
                            hasStarted = false
                            isPlaying = false
                        }, modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)) {
                            Icon(Icons.Default.Stop, null, tint = Color.White)
                        }
                    }

                    // Center Controls
                    Row(
                        Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { exoPlayer.seekTo(exoPlayer.currentPosition - 10000) }) {
                            Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                        
                        IconButton(
                            onClick = {
                                if (!hasStarted) {
                                    exoPlayer.play()
                                    hasStarted = true
                                    isPlaying = true
                                } else {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                        isPlaying = false
                                    } else {
                                        exoPlayer.play()
                                        isPlaying = true
                                    }
                                }
                            },
                            modifier = Modifier.size(80.dp).background(colors.tertiary.copy(0.8f), CircleShape)
                        ) {
                            Icon(
                                if (isPlaying && hasStarted) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null, tint = colors.primary, modifier = Modifier.size(48.dp)
                            )
                        }
                        
                        IconButton(onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10000) }) {
                            Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }

    // Bottom Bar
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
                            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                    ) {
                        // YouTube-style responsive Slider
                        val interactionSource = remember { MutableInteractionSource() }
                        val isDragged by interactionSource.collectIsDraggedAsState()
                        val sliderHeight by animateDpAsState(if (isDragged) 6.dp else 2.dp, label = "sliderHeight")
                        
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth().height(24.dp)) {
                            // Buffered Progress
                            LinearProgressIndicator(
                                progress = { if (duration > 0) bufferedPosition.toFloat() / duration.toFloat() else 0f },
                                modifier = Modifier.fillMaxWidth().height(sliderHeight).clip(CircleShape),
                                color = Color.White.copy(0.3f),
                                trackColor = Color.White.copy(0.1f)
                            )
                            Slider(
                                value = currentPosition.toFloat(),
                                onValueChange = { 
                                    exoPlayer.seekTo(it.toLong())
                                    currentPosition = it.toLong()
                                },
                                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                interactionSource = interactionSource,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.Red,
                                    activeTrackColor = Color.Red,
                                    inactiveTrackColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth().height(24.dp)
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${formatTime(currentPosition)} / ${formatTime(duration)}",
                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium
                            )
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = {
                                    currentSpeed = when(currentSpeed) {
                                        1f -> 1.5f
                                        1.5f -> 2f
                                        2f -> 0.5f
                                        else -> 1f
                                    }
                                    exoPlayer.playbackParameters = PlaybackParameters(currentSpeed)
                                }, contentPadding = PaddingValues(0.dp)) {
                                    Text("${currentSpeed}x", color = Color.White, fontSize = 12.sp)
                                }
                                
                                IconButton(onClick = { 
                                    val activity = context as? Activity
                                    val isLandscape = activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    if (isLandscape) {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        onFullScreenToggle(false)
                                    } else {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        onFullScreenToggle(true)
                                    }
                                }) {
                                    Icon(Icons.Default.Fullscreen, null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Playback Settings", color = colors.secondary) },
                containerColor = colors.primary,
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item { 
                            Text("Quality", fontWeight = FontWeight.Bold, color = colors.secondary, modifier = Modifier.padding(bottom = 8.dp)) 
                        }
                        items(links.size) { index ->
                            val link = links[index]
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    currentLinkIndex = index
                                    showSettings = false
                                }.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(link.quality, color = if (index == currentLinkIndex) colors.tertiary else colors.secondary.copy(0.7f))
                                if (index == currentLinkIndex) Icon(Icons.Default.Check, null, tint = colors.tertiary)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Close", color = colors.tertiary) } }
            )
        }
    }
}

@Composable
fun LoadingAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "angle"
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(60.dp)
                .rotate(angle)
                .border(3.dp, Brush.sweepGradient(listOf(Color.Transparent, color)), CircleShape)
        )
    }
}

enum class GestureIndicatorType { VOLUME, BRIGHTNESS }

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
