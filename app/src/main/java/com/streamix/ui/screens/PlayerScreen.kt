package com.streamix.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.streamix.core.PreferenceManager
import com.streamix.core.ProfileManager
import com.streamix.scrapers.StreamResult
import com.streamix.ui.theme.AppColors
import com.streamix.ui.theme.FrostedGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import kotlin.math.abs

import androidx.navigation.NavController

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun PlayerScreen(
    contentId: String,
    sourceId: String,
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by settingsViewModel.prefs.collectAsState()

    val streams by viewModel.streams.collectAsState()
    val currentStream by viewModel.currentStream.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val stats by viewModel.stats.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var showStats by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    
    // Gesture feedback states
    var gestureType by remember { mutableStateOf<String?>(null) } // "Brightness", "Volume"
    var gestureValue by remember { mutableStateOf(0f) }

    LaunchedEffect(contentId, sourceId) {
        viewModel.loadAndPlay(
            pageUrl = URLDecoder.decode(contentId, "UTF-8"),
            sourceId = sourceId
        )
    }

    // Immersive Fullscreen Logic
    val activity = context as? Activity
    SideEffect {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Auto-hide controls after 2 seconds
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying && !isLocked) {
            delay(2000)
            controlsVisible = false
        }
    }
    val currentProfile by viewModel.profileManager.currentProfile.collectAsState()

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Adult Privacy Mode
        if (currentProfile == ProfileManager.Profile.ADULT) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    BackHandler {
        if (isLocked) return@BackHandler
        navController.popBackStack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                if (isLocked) {
                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                } else {
                    detectTapGestures(
                        onTap = { 
                            controlsVisible = !controlsVisible 
                        },
                        onDoubleTap = { 
                            viewModel.togglePlayPause()
                        },
                        onLongPress = {
                            if (prefs.longPressSpeedEnabled) {
                                viewModel.setSpeed(2.0f)
                            }
                        }
                    )
                }
            }
    ) {
        // --- ExoPlayer Surface ---
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.exoPlayer
                    useController = false
                    // Default to ZOOM to fill screen as requested
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { view ->
                 view.resizeMode = when (prefs.aspectRatio) {
                    VideoAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    VideoAspectRatio.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    VideoAspectRatio.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- Gesture Handling Layers ---
        if (!isLocked) {
            val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                gestureType = if (offset.x < size.width / 2) "Brightness" else "Volume"
                            },
                            onDragEnd = { 
                                scope.launch {
                                    delay(1000)
                                    gestureType = null
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (gestureType == "Brightness") {
                                    val attributes = activity?.window?.attributes
                                    val currentBrightness = attributes?.screenBrightness ?: 0.5f
                                    val newBrightness = (currentBrightness - (dragAmount / 1000f)).coerceIn(0.01f, 1f)
                                    attributes?.screenBrightness = newBrightness
                                    activity?.window?.attributes = attributes
                                    gestureValue = newBrightness
                                } else if (gestureType == "Volume") {
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val delta = if (dragAmount < 0) 1 else -1
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (currentVol + delta).coerceIn(0, maxVol), 0)
                                    gestureValue = (currentVol + delta).toFloat() / maxVol
                                }
                            }
                        )
                    }
            )
        }

        // --- Controls Overlay ---
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLocked) {
                    // Locked View: only show unlock button
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        IconButton(
                            onClick = { viewModel.toggleLock() },
                            modifier = Modifier
                                .padding(32.dp)
                                .size(64.dp)
                                .background(AppColors.glassStrong, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Lock, "Unlock", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                } else {
                    // Full Premium Controls
                    PremiumPlayerControls(
                        isPlaying = isPlaying,
                        progress = progress,
                        duration = duration,
                        title = "Streaming Content", 
                        isBuffering = isBuffering,
                        playbackSpeed = playbackSpeed,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onSeek = { viewModel.seekTo(it) },
                        onSeekBy = { viewModel.seekBy(it) },
                        onBack = { navController.popBackStack() },
                        onLock = { viewModel.toggleLock() },
                        onSettings = { showSettings = true },
                        onStats = { showStats = !showStats },
                        onSpeedChange = { viewModel.setSpeed(it) },
                        onAspectRatioChange = {
                            val values = VideoAspectRatio.entries
                            val next = values[(values.indexOf(prefs.aspectRatio) + 1) % values.size]
                            settingsViewModel.setAspectRatio(next)
                        }
                    )
                }
            }
        }

        // --- Gesture HUD ---
        if (gestureType != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                FrostedGlass(
                    padding = PaddingValues(16.dp),
                    tint = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (gestureType == "Brightness") Icons.Rounded.BrightnessMedium else Icons.Rounded.VolumeUp,
                            null, tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${(gestureValue * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Stats Panel ---
        if (showStats) {
            StatsPanel(stats, onClose = { showStats = false })
        }
    }
}

@Composable
fun PremiumPlayerControls(
    isPlaying: Boolean,
    progress: Long,
    duration: Long,
    title: String,
    isBuffering: Boolean,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit, // Added
    onBack: () -> Unit,
    onLock: () -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onAspectRatioChange: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrims
        Box(Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(150.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))

        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Streaming Quality: Auto", color = Color.White.copy(0.7f), fontSize = 11.sp)
            }
            IconButton(onClick = onStats) { Icon(Icons.Rounded.BarChart, null, tint = Color.White) }
            IconButton(onClick = onLock) { Icon(Icons.Rounded.LockOpen, null, tint = Color.White) }
        }

        // CENTER CONTROLS
        Box(Modifier.align(Alignment.Center)) {
            if (isBuffering) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(60.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    IconButton(onClick = { onSeekBy(-10000L) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Replay10, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    
                    Surface(
                        onClick = onPlayPause,
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                null, tint = Color.Black, modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    IconButton(onClick = { onSeekBy(10000L) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Forward10, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
            }
        }

        // BOTTOM BAR
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            // Seek bar with time
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(formatPlayerTime(progress), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                Slider(
                    value = if (duration > 0) progress.toFloat() / duration else 0f,
                    onValueChange = { onSeek((it * duration).toLong()) },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(0.3f)
                    )
                )

                Text(formatPlayerTime(duration), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Subtitles */ }) { Icon(Icons.Rounded.Subtitles, null, tint = Color.White) }
                IconButton(onClick = { /* Audio */ }) { Icon(Icons.Rounded.AudioFile, null, tint = Color.White) }
                
                Spacer(Modifier.weight(1f))
                
                TextButton(onClick = {
                    val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
                    val next = speeds[(speeds.indexOf(playbackSpeed) + 1) % speeds.size]
                    onSpeedChange(next)
                }) {
                    Text("${playbackSpeed}x", color = Color.White, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, null, tint = Color.White) }
                IconButton(onClick = onAspectRatioChange) { Icon(Icons.Rounded.Fullscreen, null, tint = Color.White) }
            }
        }
    }
}

@Composable
fun StatsPanel(stats: PlayerStats, onClose: () -> Unit) {
    FrostedGlass(
        modifier = Modifier.padding(24.dp).width(280.dp),
        tint = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Stats for Nerds", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            StatRow("Resolution", stats.resolution)
            StatRow("Bitrate", stats.bitrate)
            StatRow("FPS", "%.1f".format(stats.fps))
            StatRow("Buffer", stats.bufferHealth)
            StatRow("Decoder", stats.decoderName)
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.width(90.dp))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
