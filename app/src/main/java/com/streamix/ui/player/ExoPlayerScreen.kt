package com.streamix.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.Activity
import com.streamix.ui.navigation.LocalBottomDockVisible
import com.streamix.core.utils.SystemUiUtils
import android.provider.Settings
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.VolumeUp

enum class GestureType {
    VOLUME, BRIGHTNESS
}

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerScreen(
    videoUrl: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val bottomDockVisible = LocalBottomDockVisible.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        bottomDockVisible.value = false
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    
    var gestureType by remember { mutableStateOf<GestureType?>(null) }
    var gestureValue by remember { mutableFloatStateOf(0f) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                }
                Lifecycle.Event.ON_DESTROY -> exoPlayer.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.let { SystemUiUtils.showSystemBars(it) }
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            totalDuration = exoPlayer.duration.coerceAtLeast(0)
            isPlaying = exoPlayer.isPlaying
            delay(1000)
        }
    }

    // Fullscreen handling
    val isLandscape = activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    LaunchedEffect(isLandscape) {
        activity?.let {
            if (isLandscape) SystemUiUtils.hideSystemBars(it)
            else SystemUiUtils.showSystemBars(it)
        }
    }

    BackHandler {
        if (isLandscape) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width / 2) {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                        } else {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        gestureType = if (offset.x < size.width / 2) GestureType.BRIGHTNESS else GestureType.VOLUME
                    },
                    onDragEnd = { gestureType = null },
                    onDragCancel = { gestureType = null },
                    onVerticalDrag = { change, dragAmount ->
                        val sensitivity = 0.005f
                        when (gestureType) {
                            GestureType.VOLUME -> {
                                volume = (volume - dragAmount * sensitivity).coerceIn(0f, 1f)
                                exoPlayer.volume = volume
                                gestureValue = volume
                            }
                            GestureType.BRIGHTNESS -> {
                                brightness = (brightness - dragAmount * sensitivity).coerceIn(0f, 1f)
                                activity?.window?.attributes = activity?.window?.attributes?.apply {
                                    screenBrightness = brightness
                                }
                                gestureValue = brightness
                            }
                            null -> {}
                        }
                    }
                )
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.Gravity.CENTER
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Overlay
        gestureType?.let { type ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(0.6f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (type == GestureType.VOLUME) Icons.Default.VolumeUp else Icons.Default.BrightnessLow,
                        null, tint = Color.White, modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { gestureValue },
                        modifier = Modifier.width(100.dp).height(4.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(0.3f)
                    )
                }
            }
        }

        if (showControls) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f))) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Text(title, color = Color.White, fontSize = 18.sp, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    // Speed Toggle
                    TextButton(onClick = {
                        val currentSpeed = exoPlayer.playbackParameters.speed
                        exoPlayer.setPlaybackSpeed(if (currentSpeed >= 2f) 1f else currentSpeed + 0.5f)
                    }) {
                        Text("${exoPlayer.playbackParameters.speed}x", color = Color.White)
                    }
                }

                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { exoPlayer.seekTo(exoPlayer.currentPosition - 10000) }) {
                        Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    IconButton(onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null, tint = Color.White, modifier = Modifier.size(64.dp)
                        )
                    }
                    IconButton(onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10000) }) {
                        Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isDragged by interactionSource.collectIsDraggedAsState()
                    val sliderHeight by animateDpAsState(if (isDragged) 6.dp else 2.dp, label = "sliderHeight")

                    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth().height(24.dp)) {
                        LinearProgressIndicator(
                            progress = { if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth().height(sliderHeight).clip(CircleShape),
                            color = Color.Red.copy(0.3f),
                            trackColor = Color.White.copy(0.1f)
                        )
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { 
                                exoPlayer.seekTo(it.toLong())
                                currentPosition = it.toLong()
                            },
                            valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                            interactionSource = interactionSource,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Red,
                                activeTrackColor = Color.Red,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(currentPosition), color = Color.White, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatTime(totalDuration), color = Color.White, fontSize = 12.sp)
                            Spacer(Modifier.width(16.dp))
                            IconButton(onClick = {
                                (context as? android.app.Activity)?.let {
                                    it.requestedOrientation = if (it.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Fullscreen, null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
