@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)
package com.streamix.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import com.streamix.core.utils.FormatUtils
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.session.MediaSession
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.cloudstream.Episode
import com.streamix.scraper.cloudstream.utils.AppUtils.toJson
import com.streamix.scraper.cloudstream.utils.AppUtils.tryParseJson
import com.streamix.core.storage.PreferencesManager
import com.streamix.core.utils.DownloadUtils
import com.streamix.core.utils.ShareUtils
import com.streamix.core.utils.UrlUtils
import com.streamix.ui.navigation.LocalBottomDockVisible
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.StreamInfo
import kotlin.math.roundToInt
import dagger.hilt.android.EntryPointAccessors

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PlayerEntryPoint {
    fun watchlistDao(): com.streamix.core.storage.WatchlistDao
}

enum class GestureIndicatorType { VOLUME, BRIGHTNESS }
enum class PlaylistState { COLLAPSED, HALF, FULL }

@Composable
fun MinimizedPlayerBar(
    title: String,
    subtitle: String,
    thumbUrl: String?,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    onClick: () -> Unit,
    onClose: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 64.dp
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Brush.horizontalGradient(listOf(Color(0xFF00796B), Color(0xFF004D40))))
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount -> offsetY = (offsetY + dragAmount).coerceAtLeast(0f) },
                    onDragEnd = { if (offsetY > 100) onClose(); offsetY = 0f }
                )
            }
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White.copy(0.6f),
                    strokeWidth = 2.dp,
                    trackColor = Color.White.copy(0.1f)
                )
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(0.3f)),
                    contentScale = ContentScale.Crop
                )
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Color.White.copy(0.7f), fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun EmbeddedPlayer(
    id: String = "",
    title: String = "",
    subtitle: String = "",
    mediaType: String = "",
    links: List<VideoLink>,
    relatedVideos: List<SearchResult> = emptyList(),
    episodes: List<Episode> = emptyList(),
    posterUrl: String? = null,
    modifier: Modifier = Modifier,
    isPlayingInitially: Boolean = false,
    initialPosition: Long = 0L,
    isMinimized: Boolean = false,
    onMinimizedChange: (Boolean) -> Unit = {},
    onFullScreenToggle: (Boolean) -> Unit = {},
    onProgressUpdate: (Long, Long) -> Unit = { _, _ -> },
    onVideoSelect: (SearchResult) -> Unit = {},
    onEpisodeSelect: (Episode) -> Unit = {},
    onChannelClick: (String) -> Unit = {},
    onClose: () -> Unit = {},
    isStacked: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val bottomDockVisible = LocalBottomDockVisible.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val backgroundPlaybackEnabled by prefs.backgroundPlaybackYoutube.collectAsState(initial = false)
    
    var currentLinkIndex by remember(links) { 
        val dashIndex = links.indexOfFirst { it.quality.contains("DASH", ignoreCase = true) }
        val preferredIndex = if (dashIndex != -1) dashIndex 
                             else links.indexOfFirst { it.quality.contains("1080") || it.quality.contains("720") }
        mutableIntStateOf(preferredIndex.coerceAtLeast(0))
    }
    val currentLink = links.getOrNull(currentLinkIndex)
    val videoUrl = currentLink?.url ?: ""

    var currentQuality by remember(currentLink) { mutableStateOf(currentLink?.quality ?: "Auto") }
    
    val activity = context as? Activity
    val isLandscape = activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    
    var isPlaying by remember { mutableStateOf(isPlayingInitially) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentPitch by remember { mutableFloatStateOf(1.0f) }
    var showControls by remember { mutableStateOf(true) }
    var hasStarted by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    var volume by remember { 
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.coerceAtLeast(1)) 
    }
    var brightness by remember { 
        val current = (context as? Activity)?.window?.attributes?.screenBrightness ?: -1f
        mutableStateOf(if (current < 0) 0.5f else current) 
    }
    var showGestureIndicator by remember { mutableStateOf<GestureIndicatorType?>(null) }
    
    var currentPosition by remember { mutableLongStateOf(initialPosition) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    var showSettings by remember { mutableStateOf(false) }
    var showCCDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showVolumeScaleDialog by remember { mutableStateOf(false) }
    var showPlayerSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSyncSubsDialog by remember { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }
    var subtitleOffset by remember { mutableLongStateOf(0L) }

    var sleepTimerMinutes by remember { mutableIntStateOf(0) }
    
    var isVolumeBoosterEnabled by remember { mutableStateOf(false) }
    var volumeScale by remember { mutableFloatStateOf(1.0f) }

    var isShuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_ONE) }

    var isSeekingHorizontally by remember { mutableStateOf(false) }
    var horizontalSeekValue by remember { mutableLongStateOf(0L) }
    var gestureAmount by remember { mutableFloatStateOf(0f) }

    var showNowPlayingList by remember { mutableStateOf(false) }
    var playlistState by remember { mutableStateOf(PlaylistState.COLLAPSED) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    var availableAudioTracks by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var availableVideoTracks by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var availableSubtitleTracks by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

    var fullInfo by remember(id) { mutableStateOf<StreamInfo?>(null) }
    val subscribedChannels by prefs.subscribedChannels.collectAsState(initial = emptySet())
    val subscribed = remember(fullInfo, subscribedChannels) {
        fullInfo?.let { info ->
            subscribedChannels.contains(info.uploaderUrl) || subscribedChannels.contains(info.uploaderName)
        } ?: false
    }

    var isLiked by remember(id) { mutableStateOf(false) }
    var isDisliked by remember(id) { mutableStateOf(false) }
    
    val baseLikeCount = remember(fullInfo) { 
        val count = fullInfo?.likeCount ?: 0L
        if (count > 0) count else (1000..50000).random().toLong()
    }
    val displayLikeCount = remember(baseLikeCount, isLiked) {
        if (isLiked) baseLikeCount + 1 else baseLikeCount
    }

    val tealColor = Color.White
    var offsetY by remember { mutableFloatStateOf(0f) }

    BackHandler(enabled = !isMinimized) {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            onFullScreenToggle(false)
        } else {
            onMinimizedChange(true)
        }
    }

    LaunchedEffect(id, mediaType) {
        if (id.isNotEmpty() && mediaType == "youtube") {
            val scraper = com.streamix.scraper.youtube.YouTubeScraper()
            fullInfo = scraper.getFullStreamInfo(id)
        }
    }

    LaunchedEffect(isLandscape) {
        if (isLandscape) { offsetY = 0f; showNowPlayingList = false }
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

    LaunchedEffect(isMinimized) { if (isMinimized) { offsetY = 0f; showNowPlayingList = false } }

    // Reset state for new video
    LaunchedEffect(id) {
        currentPosition = initialPosition
        bufferedPosition = 0L
        duration = 0L
        isBuffering = true
        playbackError = null
        hasStarted = false
    }

    val exoPlayer = remember(id) {
        ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE }
    }

    val mediaSession = remember(exoPlayer) {
        MediaSession.Builder(context, exoPlayer).build()
    }

    DisposableEffect(mediaSession) {
        onDispose {
            mediaSession.release()
        }
    }

    LaunchedEffect(isMinimized, id) {
        if (isMinimized || id.isNotEmpty()) {
            currentSpeed = 1.0f
            exoPlayer.setPlaybackSpeed(1.0f)
        }
    }

    LaunchedEffect(sleepTimerMinutes) { if (sleepTimerMinutes > 0) { delay(sleepTimerMinutes * 60 * 1000L); exoPlayer.pause(); sleepTimerMinutes = 0 } }

    val thumbUrl = remember(posterUrl, mediaType, videoUrl, id) {
        UrlUtils.resolveImageUrl(posterUrl, mediaType, videoUrl) 
    }

    LaunchedEffect(currentLink, id) {
        if (currentLink != null && currentLink!!.url.isNotBlank()) {
            val link = currentLink!!
            val url = link.url
            isBuffering = true; playbackError = null
            
            val mimeType = when {
                url.contains(".m3u8") || url.contains("m3u8") -> MimeTypes.APPLICATION_M3U8
                url.contains(".mpd") || url.contains("dash") -> MimeTypes.APPLICATION_MPD
                url.contains(".mp4") -> MimeTypes.VIDEO_MP4
                else -> null
            }

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(link.headers)
            
            val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)
            val mediaItem = MediaItem.Builder().setUri(url).setMimeType(mimeType).build()
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            
            // Start fresh for new video unless explicit initialPosition is provided
            val seekPos = if (initialPosition > 0) initialPosition else 0L
            exoPlayer.seekTo(seekPos)
            currentPosition = seekPos
            
            if ((isPlayingInitially || hasStarted) && !PlayerManager.shouldPause.value) {
                exoPlayer.playWhenReady = true
                exoPlayer.play()
                isPlaying = true
                hasStarted = true
            } else {
                hasStarted = true
            }
        } else if (links.isNotEmpty() && currentLink == null) {
            // Fallback if index somehow became invalid
            currentLinkIndex = 0
        }
    }

    LaunchedEffect(hasStarted, isPlaying) {
        PlayerManager.isPlayingState.value = isPlaying
        while (hasStarted && isPlaying) {
            val current = exoPlayer.currentPosition; val total = exoPlayer.duration
            currentPosition = current; bufferedPosition = exoPlayer.bufferedPosition
            if (total > 0 && total != androidx.media3.common.C.TIME_UNSET) { 
                duration = total; onProgressUpdate(current, total) 
                PlayerManager.playbackProgress.value = current.toFloat() / total.toFloat()
            }
            delay(1000)
        }
    }

    val shouldPause by PlayerManager.shouldPause
    LaunchedEffect(shouldPause) { 
        if (shouldPause) {
            exoPlayer.pause()
        } else if (hasStarted) {
            exoPlayer.play()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { isBuffering = state == Player.STATE_BUFFERING; if (state == Player.STATE_READY) { isBuffering = false; duration = exoPlayer.duration.coerceAtLeast(0) } }
            override fun onTracksChanged(tracks: Tracks) {
                val audioList = mutableListOf<Pair<Int, Int>>(); val videoList = mutableListOf<Pair<Int, Int>>(); val subList = mutableListOf<Pair<Int, Int>>()
                tracks.groups.forEachIndexed { groupIndex, group ->
                    when (group.type) {
                        C.TRACK_TYPE_AUDIO -> { for (i in 0 until group.length) audioList.add(groupIndex to i) }
                        C.TRACK_TYPE_VIDEO -> { for (i in 0 until group.length) videoList.add(groupIndex to i) }
                        C.TRACK_TYPE_TEXT -> { for (i in 0 until group.length) subList.add(groupIndex to i) }
                    }
                }
                val sortedVideoTracks = videoList
                    .map { it to tracks.groups[it.first].getTrackFormat(it.second) }
                    .filter { it.second.height > 0 || it.second.bitrate > 0 }
                    .sortedByDescending { it.second.height.coerceAtLeast(0) * 10000 + it.second.bitrate.coerceAtLeast(0) }
                    .map { it.first }
                availableAudioTracks = audioList; availableVideoTracks = sortedVideoTracks; availableSubtitleTracks = subList
                
                // Update current quality display from format
                exoPlayer.videoFormat?.let { format ->
                    if (format.height > 0) {
                        val fr = if (format.frameRate > 45) format.frameRate.roundToInt().toString() else ""
                        currentQuality = "${format.height}p$fr"
                    }
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.height > 0) {
                    val h = videoSize.height
                    // We can't easily get framerate here without format, but height is most important
                    exoPlayer.videoFormat?.let { format ->
                        val fr = if (format.frameRate > 45) format.frameRate.roundToInt().toString() else ""
                        currentQuality = "${h}p$fr"
                    } ?: run {
                        currentQuality = "${h}p"
                    }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { 
                val link = links.getOrNull(currentLinkIndex)
                if (link?.fallbackUrl != null) {
                    val mimeType = when {
                        link.fallbackUrl.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
                        link.fallbackUrl.contains(".mpd") -> MimeTypes.APPLICATION_MPD
                        else -> MimeTypes.VIDEO_MP4
                    }
                    val mediaItem = MediaItem.Builder().setUri(link.fallbackUrl).setMimeType(mimeType).build()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.play()
                } else {
                    playbackError = "Error: ${error.errorCodeName}"
                }
                isBuffering = false 
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    DisposableEffect(lifecycleOwner, backgroundPlaybackEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!isMinimized) {
                        val isYoutube = mediaType == "youtube"
                        if (!(isYoutube && backgroundPlaybackEnabled)) {
                            exoPlayer.pause()
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) exoPlayer.play()
                }
                Lifecycle.Event.ON_STOP -> {
                    val isYoutube = mediaType == "youtube"
                    if (!(isYoutube && backgroundPlaybackEnabled)) {
                        exoPlayer.stop()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.stop()
                    exoPlayer.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val act = context as? Activity
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            act?.window?.let { 
                val controller = WindowCompat.getInsetsController(it, view)
                controller.show(WindowInsetsCompat.Type.systemBars()) 
                it.attributes = it.attributes?.apply { screenBrightness = -1f }
            }
            bottomDockVisible.value = true
        }
    }

    LaunchedEffect(showControls, isPlaying, isLocked) { if (showControls && isPlaying && !isLocked) { delay(5000); showControls = false } }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isMinimized) 2000f else offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )

    val minimizationNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0 && !isLandscape && playlistState == PlaylistState.COLLAPSED) {
                    offsetY = (offsetY + delta).coerceAtLeast(0f)
                    return available
                }
                return Offset.Zero
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (offsetY > 0 && available.y < 0) {
                    val consumed = available.y.coerceAtLeast(-offsetY)
                    offsetY += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (offsetY > 300) {
                    onMinimizedChange(true)
                }
                offsetY = 0f
                return super.onPreFling(available)
            }
        }
    }

    val playerAlpha by animateFloatAsState(targetValue = if (isMinimized) 0f else 1f, label = "alpha")

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (isLandscape) Modifier else Modifier.offset { IntOffset(0, animatedOffset.roundToInt()) })
            .alpha(playerAlpha)
            .then(if (isMinimized) Modifier.size(0.dp) else Modifier.fillMaxSize())
    ) {
        if (!isMinimized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(Modifier.fillMaxSize()) {
                    if (!isLandscape) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            if (dragAmount > 0) {
                                                offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                                                change.consume()
                                            }
                                        },
                                        onDragEnd = {
                                            if (offsetY > 300) onMinimizedChange(true)
                                            offsetY = 0f
                                        }
                                    )
                                }, 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onMinimizedChange(true) }) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { showDownloadDialog = true }) { Icon(Icons.Default.FileDownload, null, tint = Color.White) }
                            IconButton(onClick = { showSleepTimerDialog = true }) { Icon(Icons.Default.Timer, null, tint = Color.White) }
                            Surface(onClick = { showSettings = true }, color = Color.Transparent, shape = RoundedCornerShape(4.dp)) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { Icon(Icons.Default.VideoCameraBack, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(currentQuality, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                            IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (isLandscape) 1f else 0.45f)
                    ) {
                        AndroidView(
                            factory = { PlayerView(it).apply { player = exoPlayer; useController = false; setBackgroundColor(android.graphics.Color.BLACK); this.resizeMode = resizeMode; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, android.view.Gravity.CENTER) } },
                            modifier = Modifier.fillMaxSize().rotate(rotation),
                            update = { view -> view.player = exoPlayer; view.resizeMode = resizeMode }
                        )

                        // Drag & Tap Overlay to avoid AndroidView consumption issues
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(isLandscape, isLocked) {
                                    if (isLocked) {
                                        detectTapGestures(onTap = { showControls = !showControls })
                                    } else {
                                        detectTapGestures(
                                            onTap = { showControls = !showControls },
                                            onDoubleTap = { offset ->
                                                val width = size.width
                                                if (offset.x < width / 4) exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                                                else if (offset.x > width * 3 / 4) exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                                            }
                                        )
                                    }
                                }
                                .then(if (!isLocked) Modifier.pointerInput(isLandscape) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            if (dragAmount > 0) {
                                                if (isLandscape) {
                                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                                    onFullScreenToggle(false)
                                                    change.consume()
                                                } else {
                                                    offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                                                    change.consume()
                                                }
                                            } else if (dragAmount < 0 && offsetY > 0) {
                                                offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                                                change.consume()
                                            }
                                        },
                                        onDragEnd = {
                                            if (offsetY > 300) onMinimizedChange(true)
                                            offsetY = 0f
                                        }
                                    )
                                } else Modifier)
                                .then(if (isLandscape && !isLocked) Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset: Offset ->
                                            gestureAmount = 0f
                                            val width = size.width
                                            if (offset.x < width / 3) {
                                                showGestureIndicator = GestureIndicatorType.BRIGHTNESS
                                            } else if (offset.x > width * 2 / 3) {
                                                showGestureIndicator = GestureIndicatorType.VOLUME
                                            } else {
                                                isSeekingHorizontally = true
                                                horizontalSeekValue = exoPlayer.currentPosition
                                            }
                                        },
                                        onDrag = { change, dragAmount: Offset ->
                                            change.consume()
                                            if (isSeekingHorizontally) {
                                                gestureAmount += dragAmount.x
                                                val seekChange = (gestureAmount * 100).toLong()
                                                horizontalSeekValue = (exoPlayer.currentPosition + seekChange).coerceIn(0, duration)
                                            } else if (showGestureIndicator == GestureIndicatorType.VOLUME) {
                                                gestureAmount -= dragAmount.y
                                                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                                val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                val delta = (gestureAmount / 50).toInt()
                                                if (delta != 0) {
                                                    val next = (current + delta).coerceIn(0, max)
                                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                                    volume = next.toFloat() / max
                                                    gestureAmount = 0f
                                                }
                                            } else if (showGestureIndicator == GestureIndicatorType.BRIGHTNESS) {
                                                gestureAmount -= dragAmount.y
                                                val delta = gestureAmount / 500f
                                                if (delta != 0f) {
                                                    brightness = (brightness + delta).coerceIn(0f, 1f)
                                                    (context as? Activity)?.window?.attributes = (context as? Activity)?.window?.attributes?.apply { screenBrightness = brightness }
                                                    gestureAmount = 0f
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (isSeekingHorizontally) {
                                                exoPlayer.seekTo(horizontalSeekValue)
                                                isSeekingHorizontally = false
                                            }
                                            showGestureIndicator = null
                                            gestureAmount = 0f
                                        }
                                    )
                                } else Modifier)
                        )
                        if (isBuffering) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingAnimation(tealColor) } }
                        
                        // Gesture Indicators
                        if (showGestureIndicator != null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.6f)), shape = RoundedCornerShape(12.dp)) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (showGestureIndicator == GestureIndicatorType.VOLUME) {
                                                if (volume == 0f) Icons.Default.VolumeOff else if (volume < 0.5f) Icons.Default.VolumeDown else Icons.Default.VolumeUp
                                            } else Icons.Default.Brightness6,
                                            null, tint = Color.White
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        val percent = if (showGestureIndicator == GestureIndicatorType.VOLUME) (volume * 100).toInt() else (brightness * 100).toInt()
                                        Text("$percent%", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        if (isSeekingHorizontally) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.6f)), shape = RoundedCornerShape(12.dp)) {
                                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(formatTime(horizontalSeekValue), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        Text("[ ${formatTime(horizontalSeekValue - currentPosition)} ]", color = Color.White.copy(0.7f), fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        if (isLandscape) {
                            PlayerControlsOverlay(
                                isLandscape = isLandscape,
                                showControls = showControls,
                                isLocked = isLocked,
                                isPlaying = isPlaying,
                                hasStarted = hasStarted,
                                title = title,
                                subtitle = subtitle,
                                quality = currentQuality,
                                currentPosition = currentPosition,
                                duration = duration,
                                bufferedPosition = bufferedPosition,
                                playbackSpeed = currentSpeed,
                                resizeMode = resizeMode,
                                onBack = {
                                    if (isLandscape) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    onMinimizedChange(true)
                                },
                                onPlayPause = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                                onSeek = { exoPlayer.seekTo(it) }, 
                                onRewind = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) }, 
                                onForward = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration)) },
                                onPrevious = { exoPlayer.seekToPreviousMediaItem() }, 
                                onNext = { 
                                    if (episodes.isNotEmpty() && PlayerManager.currentEpisode.value != null) {
                                        val currentIndex = episodes.indexOfFirst { it.data == PlayerManager.currentEpisode.value?.data }
                                        if (currentIndex != -1 && currentIndex < episodes.size - 1) {
                                            onEpisodeSelect(episodes[currentIndex + 1])
                                        }
                                    } else {
                                        exoPlayer.seekToNextMediaItem()
                                    }
                                }, 
                                onLock = { isLocked = !isLocked; if (isLocked) showControls = false }, 
                                onSettings = { showSettings = true }, 
                                onCC = { showCCDialog = true },
                                onResize = { 
                                    resizeMode = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                onRotate = { 
                                    activity?.requestedOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT 
                                                                     else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }, 
                                onDownload = { showDownloadDialog = true }, 
                                onSleepTimer = { showSleepTimerDialog = true }, 
                                onMore = { showMoreMenu = true },
                                onSpeedClick = { showSpeedDialog = true },
                                onSyncSubsClick = { showSyncSubsDialog = true },
                                onTracksClick = { showTracksDialog = true },
                                tealColor = tealColor
                            )
                        } else if (showControls && !isLocked) {
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f))) {
                                Row(Modifier.align(Alignment.Center).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) }) { Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
                                    IconButton(onClick = { if (isPlaying) PlayerManager.pause() else PlayerManager.resume() }, modifier = Modifier.size(64.dp)) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(64.dp)) }
                                    IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration)) }) { Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
                                }
                                IconButton(
                                    onClick = { 
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        onFullScreenToggle(true)
                                    },
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Fullscreen, null, tint = Color.White)
                                }
                            }
                        }
                    }

                    if (!isLandscape) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.55f)
                                .background(Color.Black)
                                .nestedScroll(minimizationNestedScroll)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(text = if (fullInfo != null) fullInfo!!.name else title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showDescriptionSheet = true }) {
                                    Text(text = "${formatViews(fullInfo?.viewCount ?: 0L)} views", color = Color.White.copy(0.6f), fontSize = 12.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("...more", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (mediaType == "youtube") {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { fullInfo?.uploaderUrl?.let { url -> onChannelClick(url) } }, verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = fullInfo?.uploaderAvatars?.lastOrNull()?.getUrl(), contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.1f)), contentScale = ContentScale.Crop)
                                    Spacer(Modifier.width(12.dp))
                                    Text(text = fullInfo?.uploaderName ?: subtitle, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Button(onClick = { fullInfo?.uploaderUrl?.let { url -> scope.launch { prefs.toggleSubscription(url) } } }, colors = ButtonDefaults.buttonColors(containerColor = if (subscribed) Color.White.copy(0.15f) else Color.Red), shape = RoundedCornerShape(20.dp), modifier = Modifier.height(36.dp)) { Text(if (subscribed) "Subscribed" else "Subscribe", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.1f)), verticalAlignment = Alignment.CenterVertically) {
                                    Row(modifier = Modifier.clickable { isLiked = !isLiked; if (isLiked) isDisliked = false }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(formatViews(displayLikeCount), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(Modifier.width(1.dp).height(20.dp).background(Color.White.copy(0.2f)))
                                    IconButton(onClick = { isDisliked = !isDisliked; if (isDisliked) isLiked = false }, modifier = Modifier.size(36.dp)) { Icon(if (isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                                }
                                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.1f)).clickable { ShareUtils.shareLink(context, title, "https://www.youtube.com/watch?v=$id") }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Outlined.Reply, null, tint = Color.White, modifier = Modifier.size(20.dp).rotate(180f))
                                    Spacer(Modifier.width(6.dp)); Text("Share", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.1f)).clickable { showDownloadDialog = true }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FileDownload, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp)); Text("Download", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column(Modifier.padding(horizontal = 24.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatTime(currentPosition), color = Color.White, fontSize = 13.sp); Text(formatTime(duration), color = Color.White, fontSize = 13.sp) }
                                Slider(value = currentPosition.toFloat(), onValueChange = { exoPlayer.seekTo(it.toLong()) }, valueRange = 0f..duration.toFloat().coerceAtLeast(1f), colors = SliderDefaults.colors(thumbColor = tealColor, activeTrackColor = tealColor), modifier = Modifier.fillMaxWidth().height(24.dp))
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) }) { Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
                                IconButton(onClick = { if (isPlaying) PlayerManager.pause() else PlayerManager.resume() }, modifier = Modifier.size(80.dp).background(Color.White.copy(0.05f), CircleShape)) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
                                IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration)) }) { Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
                            }
                        }
                    }
                }
            }

            if (showCCDialog) { TrackSelectionDialog("Subtitles", availableSubtitleTracks, exoPlayer, C.TRACK_TYPE_TEXT) { showCCDialog = false } }
            if (showSettings) {
                if ((mediaType == "movie" || mediaType == "tv") && links.size > 1) {
                    ResolutionSelectionDialog(
                        links = links,
                        currentIndex = currentLinkIndex,
                        onLinkSelect = { index ->
                            currentPosition = exoPlayer.currentPosition
                            currentLinkIndex = index
                            showSettings = false
                        },
                        onDismiss = { showSettings = false }
                    )
                } else {
                    val title = if (mediaType == "movie" || mediaType == "tv") "Select Resolution" else "Quality"
                    TrackSelectionDialog(title, availableVideoTracks, exoPlayer, C.TRACK_TYPE_VIDEO) { showSettings = false }
                }
            }
            if (showDownloadDialog) {
                PlayerManager.currentVideo.value?.let { video ->
                    DownloadDialog(video.title, video.studio, video.posterPath, onDownloadStart = { _, _ ->
                        DownloadUtils.downloadVideo(context, videoUrl, video.title)
                        scope.launch {
                            val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, PlayerEntryPoint::class.java)
                            entryPoint.watchlistDao().insert(com.streamix.core.storage.WatchlistEntity(video.id, video.title, video.posterPath, video.mediaType, "Downloads", ""))
                        }
                        showDownloadDialog = false
                    }, onDismiss = { showDownloadDialog = false })
                }
            }
            if (showMoreMenu) {
                Dialog(onDismissRequest = { showMoreMenu = false }) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))) {
                        LazyColumn(Modifier.padding(vertical = 8.dp).width(280.dp)) {
                            item { MoreMenuItem("Video details and comments", Icons.Default.Comment) { showMoreMenu = false; showDescriptionSheet = true } }
                            if (mediaType != "adult") {
                                item { MoreMenuItem("Audio track", Icons.Default.Audiotrack) { showMoreMenu = false; showAudioDialog = true } }
                            }
                            if (mediaType == "youtube") {
                                item { MoreMenuItem("Add to YouTube playlist", Icons.Default.PlaylistAdd) { showMoreMenu = false; showPlaylistDialog = true } }
                                item { MoreMenuItem("Search lyrics on Google", Icons.Default.Search) { showMoreMenu = false; val query = java.net.URLEncoder.encode("$title lyrics", "UTF-8"); context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=$query"))) } }
                            }
                            item { MoreMenuItem("Share", Icons.Default.Share) { showMoreMenu = false; ShareUtils.shareLink(context, title, "https://www.youtube.com/watch?v=$id") } }
                            item { Divider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 4.dp)) }
                            item { MoreMenuItem("Reload player", Icons.Default.Refresh) { showMoreMenu = false; exoPlayer.prepare(); exoPlayer.play() } }
                            item { MoreMenuItem("Equalizer", Icons.Default.Equalizer) { showMoreMenu = false; try { val intent = android.content.Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL); intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, exoPlayer.audioSessionId); context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "Equalizer not found", Toast.LENGTH_SHORT).show() } } }
                        }
                    }
                }
            }
            if (showDescriptionSheet) { DescriptionBottomSheet(title, fullInfo) { showDescriptionSheet = false } }
            if (showPlaylistDialog) { PlayerManager.currentVideo.value?.let { video -> AddToPlaylistDialog(video) { showPlaylistDialog = false } } }
            if (showAudioDialog) { TrackSelectionDialog("Audio Track", availableAudioTracks, exoPlayer, C.TRACK_TYPE_AUDIO) { showAudioDialog = false } }
            
            if (showSpeedDialog) {
                PlaybackSpeedDialog(
                    currentSpeed = currentSpeed,
                    onSpeedChange = { 
                        currentSpeed = it
                        exoPlayer.setPlaybackSpeed(it)
                    },
                    onDismiss = { showSpeedDialog = false }
                )
            }

            if (showSyncSubsDialog) {
                SyncSubsDialog(
                    offset = subtitleOffset,
                    onOffsetChange = {
                        subtitleOffset = it
                        // Placeholder for subtitle sync logic
                    },
                    onDismiss = { showSyncSubsDialog = false }
                )
            }

            if (showTracksDialog) {
                TracksSelectionDialog(
                    exoPlayer = exoPlayer,
                    onDismiss = { showTracksDialog = false }
                )
            }
        }

        // Playlist Panel (Queue)
        if (!isMinimized && !isLandscape) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(), 
                contentAlignment = Alignment.BottomCenter
            ) {
                PlaylistPanel(
                    state = playlistState,
                    onStateChange = { playlistState = it },
                    currentVideoId = id,
                    mediaType = mediaType,
                    relatedVideos = relatedVideos,
                    episodes = episodes,
                    onVideoSelect = onVideoSelect,
                    onEpisodeSelect = onEpisodeSelect
                )
            }
        }
    }
}

@Composable
fun PlaylistPanel(
    state: PlaylistState,
    onStateChange: (PlaylistState) -> Unit,
    currentVideoId: String,
    mediaType: String,
    relatedVideos: List<SearchResult>,
    episodes: List<Episode>,
    onVideoSelect: (SearchResult) -> Unit,
    onEpisodeSelect: (Episode) -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val targetHeight = when (state) {
        PlaylistState.COLLAPSED -> 70.dp
        PlaylistState.HALF -> screenHeight * 0.5f
        PlaylistState.FULL -> screenHeight * 0.9f
    }
    
    val height by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "playlist_height"
    )
    
    val nestedScrollConnection = remember(state) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                if (delta < 0 && state == PlaylistState.COLLAPSED) {
                    onStateChange(PlaylistState.HALF)
                    return available
                }
                if (delta < 0 && state == PlaylistState.HALF) {
                    onStateChange(PlaylistState.FULL)
                    return available
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                if (delta > 0) { // Dragging down
                    if (state == PlaylistState.FULL) {
                        onStateChange(PlaylistState.HALF)
                        return available
                    } else if (state == PlaylistState.HALF) {
                        onStateChange(PlaylistState.COLLAPSED)
                        return available
                    }
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .nestedScroll(nestedScrollConnection),
        colors = CardDefaults.cardColors(containerColor = if (state == PlaylistState.COLLAPSED) Color.Black.copy(0.95f) else Color(0xFF1A1A1A)),
        shape = if (state == PlaylistState.COLLAPSED) RoundedCornerShape(35.dp) else RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        border = BorderStroke(if (state == PlaylistState.COLLAPSED) 0.5.dp else 1.dp, Color.White.copy(0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column {
            // Header Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clickable { 
                        if (state == PlaylistState.FULL) onStateChange(PlaylistState.COLLAPSED)
                        else onStateChange(PlaylistState.FULL)
                    },
                color = if (state == PlaylistState.COLLAPSED) Color.Transparent else Color.White.copy(0.05f),
                shape = if (state == PlaylistState.COLLAPSED) RoundedCornerShape(35.dp) else RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // Drag Handle
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.2f))
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            val headerText = when(mediaType) {
                                "adult" -> "Recommended Videos"
                                "youtube" -> "Up Next"
                                "movie", "tv" -> if (episodes.isNotEmpty()) "Episodes" else "Related Movies"
                                else -> "Now Playing"
                            }
                            Text(headerText, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            val currentIdx = if (episodes.isNotEmpty()) {
                                episodes.indexOfFirst { it.data == currentVideoId }.coerceAtLeast(0)
                            } else 0
                            
                            val subText = if (episodes.isNotEmpty()) {
                                "${currentIdx + 1} / ${episodes.size}"
                            } else if (relatedVideos.isNotEmpty()) {
                                "1 / ${relatedVideos.size + 1}"
                            } else "1 / 1"
                            Text(subText, color = Color.Gray, fontSize = 13.sp)
                        }
                        
                        IconButton(onClick = { 
                            if (state == PlaylistState.FULL) onStateChange(PlaylistState.COLLAPSED)
                            else onStateChange(PlaylistState.FULL)
                        }) {
                            Icon(
                                if (state != PlaylistState.COLLAPSED) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            
            if (state != PlaylistState.COLLAPSED) {
                Divider(color = Color.White.copy(0.1f))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    if (mediaType == "movie" || mediaType == "tv") {
                        if (episodes.isNotEmpty()) {
                            itemsIndexed(episodes) { index, ep ->
                                EpisodeItem(ep, ep.data == currentVideoId, index + 1) { 
                                    onEpisodeSelect(ep)
                                    // Collapse playlist on select if full? Maybe just to HALF
                                    if (state == PlaylistState.FULL) onStateChange(PlaylistState.HALF)
                                }
                            }
                        } else {
                            items(relatedVideos) { video ->
                                RelatedVideoItem(video) { 
                                    onVideoSelect(video)
                                    if (state == PlaylistState.FULL) onStateChange(PlaylistState.HALF)
                                }
                            }
                        }
                    } else {
                        items(relatedVideos) { video ->
                            RelatedVideoItem(video) { 
                                onVideoSelect(video)
                                if (state == PlaylistState.FULL) onStateChange(PlaylistState.HALF)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(episode: Episode, isPlaying: Boolean, number: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPlaying) Color.White.copy(0.05f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag Handle icon as seen in Image 4
        Icon(Icons.Default.Menu, null, tint = Color.Gray.copy(0.5f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        
        Box(modifier = Modifier.size(110.dp, 62.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2C2C2C))) {
            AsyncImage(model = episode.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (isPlaying) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(Modifier.weight(1f)) {
            Text(
                text = "${episode.episode ?: number}. ${episode.name ?: "Episode ${episode.episode ?: number}"}", 
                color = if (isPlaying) Color.White else Color.White,
                fontSize = 14.sp, 
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = episode.description ?: "Season ${episode.season ?: 1}", 
                color = Color.Gray, 
                fontSize = 12.sp, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = { /* Item options */ }) {
            Icon(Icons.Default.MoreVert, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun RelatedVideoItem(video: SearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = video.posterPath,
            contentDescription = null,
            modifier = Modifier.size(120.dp, 68.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(video.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(video.studio, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun DescriptionBottomSheet(title: String, fullInfo: StreamInfo?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1A1A1A), contentColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Description", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Divider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 12.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoStat(formatViews(fullInfo?.likeCount ?: 0L), "Likes")
                InfoStat(formatViews(fullInfo?.viewCount ?: 0L), "Views")
                InfoStat("2024", "Year")
            }
            Spacer(Modifier.height(20.dp))
            val descriptionText = remember(fullInfo?.description?.content) {
                val raw = fullInfo?.description?.content ?: "No description available."
                if (raw.contains("<") && raw.contains(">")) {
                    android.text.Html.fromHtml(raw, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                } else {
                    raw
                }
            }
            LinkifiedText(descriptionText, Color.LightGray, 14.sp)
            Spacer(Modifier.height(50.dp))
        }
    }
}

@Composable fun InfoStat(value: String, label: String) { Column(Modifier.background(Color.White.copy(0.05f), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(label, color = Color.Gray, fontSize = 12.sp) } }

@Composable
fun LinkifiedText(text: String, color: Color, fontSize: androidx.compose.ui.unit.TextUnit) {
    val context = LocalContext.current
    val urlPattern = remember { java.util.regex.Pattern.compile("(https?://[\\w-]+(\\.[\\w-]+)+(/\\S*)?)") }
    val annotatedString = remember(text) {
        val matcher = urlPattern.matcher(text)
        buildAnnotatedString {
            var lastIndex = 0
            while (matcher.find()) {
                val start = matcher.start(); val end = matcher.end()
                append(text.substring(lastIndex, start))
                val link = text.substring(start, end)
                pushStringAnnotation("URL", link)
                withStyle(SpanStyle(color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)) { append(link) }
                pop(); lastIndex = end
            }
            append(text.substring(lastIndex))
        }
    }
    androidx.compose.foundation.text.ClickableText(annotatedString, style = androidx.compose.ui.text.TextStyle(color = color, fontSize = fontSize), onClick = { offset -> annotatedString.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it.item))) } catch (e: Exception) {} } })
}

@Composable
fun AddToPlaylistDialog(video: SearchResult, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))) {
            Column(Modifier.padding(24.dp).width(300.dp)) {
                Text("Add to Playlist", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                val context = LocalContext.current
                TextButton(onClick = { onDismiss(); Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text("My Favorites", color = Color.White) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CANCEL", color = Color(0xFF00BCD4)) }
            }
        }
    }
}

@Composable
fun ResolutionSelectionDialog(
    links: List<VideoLink>,
    currentIndex: Int,
    onLinkSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(24.dp).width(300.dp)) {
                Text("Select Resolution", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    itemsIndexed(links) { index, link ->
                        val isSelected = index == currentIndex
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onLinkSelect(index) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onLinkSelect(index) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00BCD4))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(link.quality, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("CANCEL", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TrackSelectionDialog(title: String, tracks: List<Pair<Int, Int>>, exoPlayer: ExoPlayer, type: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))) {
            Column(Modifier.padding(24.dp).width(300.dp)) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    item {
                        val hasOverride = exoPlayer.trackSelectionParameters.overrides.values.any { override -> 
                            exoPlayer.currentTracks.groups.any { it.type == type && it.mediaTrackGroup == override.mediaTrackGroup } 
                        }
                        Row(Modifier.fillMaxWidth().clickable { 
                            val builder = exoPlayer.trackSelectionParameters.buildUpon()
                            exoPlayer.currentTracks.groups.forEach { if (it.type == type) builder.clearOverride(it.mediaTrackGroup) }
                            exoPlayer.trackSelectionParameters = builder.build()
                            onDismiss() 
                        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !hasOverride, onClick = { 
                                val builder = exoPlayer.trackSelectionParameters.buildUpon()
                                exoPlayer.currentTracks.groups.forEach { if (it.type == type) builder.clearOverride(it.mediaTrackGroup) }
                                exoPlayer.trackSelectionParameters = builder.build()
                                onDismiss() 
                            }, colors = RadioButtonDefaults.colors(selectedColor = Color.White))
                            Spacer(Modifier.width(8.dp)); Text(if (type == C.TRACK_TYPE_TEXT) "Off" else "Auto", color = Color.White)
                        }
                    }
                    items(tracks) { (groupIndex, trackIndex) ->
                        val group = exoPlayer.currentTracks.groups[groupIndex]
                        val format = group.getTrackFormat(trackIndex)
                        val isSelected = group.isTrackSelected(trackIndex)
                        
                        val label = when (type) { 
                            C.TRACK_TYPE_VIDEO -> "${format.height}p"
                            C.TRACK_TYPE_TEXT -> format.label ?: format.language ?: "Unknown"
                            else -> format.label ?: format.language ?: "Audio ${trackIndex + 1}"
                        }
                        
                        Row(Modifier.fillMaxWidth().clickable { 
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                                .clearOverridesOfType(type)
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                .build()
                            onDismiss() 
                        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSelected, onClick = { 
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                                    .clearOverridesOfType(type)
                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                    .build()
                                onDismiss() 
                            }, colors = RadioButtonDefaults.colors(selectedColor = Color.White))
                            Spacer(Modifier.width(8.dp)); Text(label, color = Color.White)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("CANCEL", color = Color(0xFF00BCD4)) }
            }
        }
    }
}

@Composable
fun DownloadDialog(title: String, uploader: String, posterUrl: String?, onDownloadStart: (String, String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.fillMaxWidth(0.9f).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(80.dp, 45.dp).background(Color.Black, RoundedCornerShape(4.dp))) { AsyncImage(model = posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) }
                    Spacer(Modifier.width(12.dp)); Column { Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(uploader, color = Color.White.copy(0.6f), fontSize = 12.sp) }
                }
                Spacer(Modifier.height(24.dp)); LazyColumn(Modifier.heightIn(max = 500.dp)) { item { DownloadSectionHeader("VIDEO DOWNLOAD LINKS", Icons.Default.VideoCameraBack); listOf("1080p", "720p", "480p", "360p").forEach { DownloadItem("MP4", it, "...") { onDownloadStart("MP4", it) } } } }
            }
        }
    }
}

@Composable fun DownloadSectionHeader(title: String, icon: ImageVector) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
@Composable fun DownloadItem(format: String, quality: String, size: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(format, color = Color.White.copy(0.6f), fontSize = 13.sp, modifier = Modifier.width(60.dp)); Text(quality, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); Text(size, color = Color.White.copy(0.6f), fontSize = 13.sp, modifier = Modifier.width(80.dp), textAlign = TextAlign.End) } }
@Composable fun MoreMenuItem(text: String, icon: ImageVector, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(16.dp)); Text(text, color = Color.White, fontSize = 14.sp) } }
@Composable fun LoadingAnimation(color: Color) { val infiniteTransition = rememberInfiniteTransition(label = "loading"); val angle by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "angle"); Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Box(Modifier.size(60.dp).rotate(angle).border(3.dp, Brush.sweepGradient(listOf(Color.Transparent, color)), CircleShape)) } }

private fun formatViews(views: Long): String { return FormatUtils.formatViews(views) }

@Composable
fun PlaybackSpeedDialog(currentSpeed: Float, onSpeedChange: (Float) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(320.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${"%.2fx".format(currentSpeed)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { onSpeedChange((currentSpeed - 0.25f).coerceAtLeast(0.25f)) }) { Icon(Icons.Default.Remove, null, tint = Color.White) }
                    Slider(
                        value = currentSpeed,
                        onValueChange = { onSpeedChange(it) },
                        valueRange = 0.25f..2.0f,
                        steps = 6,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                    )
                    IconButton(onClick = { onSpeedChange((currentSpeed + 0.25f).coerceAtMost(2.0f)) }) { Icon(Icons.Default.Add, null, tint = Color.White) }
                }
                
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(0.25f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Surface(
                            onClick = { onSpeedChange(speed) },
                            color = if (currentSpeed == speed) Color.White else Color.White.copy(0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${speed}x",
                                color = if (currentSpeed == speed) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncSubsDialog(offset: Long, onOffsetChange: (Long) -> Unit, onDismiss: () -> Unit) {
    var currentOffset by remember { mutableLongStateOf(offset) }
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Subtitle delay", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(if (currentOffset == 0L) "No subtitle delay" else "${currentOffset}ms", color = Color.Gray, fontSize = 14.sp)
                
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { currentOffset -= 100 }) { Icon(Icons.Default.Remove, null, tint = Color.White) }
                    Slider(
                        value = currentOffset.toFloat(),
                        onValueChange = { currentOffset = it.toLong() },
                        valueRange = -5000f..5000f,
                        modifier = Modifier.width(200.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                    )
                    IconButton(onClick = { currentOffset += 100 }) { Icon(Icons.Default.Add, null, tint = Color.White) }
                }
                
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
                    TextButton(onClick = { currentOffset = 0L; onOffsetChange(0L); onDismiss() }) { Text("Reset", color = Color.White) }
                    Button(onClick = { onOffsetChange(currentOffset); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                        Text("Apply", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun TracksSelectionDialog(exoPlayer: ExoPlayer, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.7f)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxSize()) {
                    // Video Tracks (Half screen)
                    Column(Modifier.weight(1f)) {
                        Text("Video tracks", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        LazyColumn {
                            // Simplified track selection for now
                            item { TrackItem("2160x1080", false) {} }
                            item { TrackItem("1440x720", true) {} }
                            item { TrackItem("960x480", false) {} }
                        }
                    }
                    
                    Divider(Modifier.fillMaxHeight().width(1.dp), color = Color.White.copy(0.1f))
                    
                    // Audio Tracks (Half screen)
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text("Audio tracks", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        LazyColumn {
                            item { TrackItem("English", true) {} }
                            item { TrackItem("Hindi", false) {} }
                        }
                    }
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                        Text("Apply", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        else Spacer(Modifier.width(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (isSelected) Color.White else Color.Gray, fontSize = 14.sp)
    }
}
private fun formatTime(millis: Long): String { val totalSeconds = millis / 1000; val hours = totalSeconds / 3600; val minutes = (totalSeconds % 3600) / 60; val seconds = totalSeconds % 60; return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds) }

@Composable
fun PlayerControlsOverlay(
    isLandscape: Boolean,
    showControls: Boolean,
    isLocked: Boolean,
    isPlaying: Boolean,
    hasStarted: Boolean,
    title: String,
    subtitle: String,
    quality: String,
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    playbackSpeed: Float,
    resizeMode: Int,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLock: () -> Unit,
    onSettings: () -> Unit,
    onCC: () -> Unit,
    onResize: () -> Unit,
    onRotate: () -> Unit,
    onDownload: () -> Unit,
    onSleepTimer: () -> Unit,
    onMore: () -> Unit,
    onSpeedClick: () -> Unit,
    onSyncSubsClick: () -> Unit,
    onTracksClick: () -> Unit,
    tealColor: Color
) {
    AnimatedVisibility(visible = showControls || isLocked, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(if (isLocked) 0f else 0.4f))) {
            if (isLocked) {
                IconButton(
                    onClick = onLock, 
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).background(Color.Black.copy(0.5f), CircleShape)
                ) { Icon(Icons.Default.Lock, null, tint = Color.White) }
            } else {
                // Top Bar
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(subtitle, color = Color.White.copy(0.7f), fontSize = 11.sp, maxLines = 1)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDownload) { Icon(Icons.Default.FileDownload, null, tint = Color.White) }
                        IconButton(onClick = onSleepTimer) { Icon(Icons.Default.Timer, null, tint = Color.White) }
                        Surface(onClick = onSettings, color = Color.Transparent, shape = RoundedCornerShape(4.dp)) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { Icon(Icons.Default.Hd, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(quality, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                        IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                    }
                }

                // Center Controls
                Row(Modifier.align(Alignment.Center).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRewind) { Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(80.dp)) { Icon(if (isPlaying && hasStarted) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(80.dp)) }
                    IconButton(onClick = onForward) { Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
                }

                // Bottom Controls
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    // Seek Bar
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(currentPosition), color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = currentPosition.toFloat(), 
                            onValueChange = { onSeek(it.toLong()) }, 
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f), 
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(0.2f)), 
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                        Text("-" + formatTime(duration - currentPosition), color = Color.White, fontSize = 12.sp)
                    }

                    // Bottom Row Buttons (Image 2 layout)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerBottomButton(Icons.Default.LockOpen, "Lock", onLock)
                        PlayerBottomButton(Icons.Default.ScreenRotation, "Rotate", onRotate)
                        PlayerBottomButton(Icons.Default.AspectRatio, "Resize", onResize)
                        PlayerBottomButton(Icons.Default.Speed, "Speed (${"%.2fx".format(playbackSpeed)})", onSpeedClick)
                        PlayerBottomButton(Icons.Default.Subtitles, "Sync subs", onSyncSubsClick)
                        PlayerBottomButton(Icons.Default.BarChart, "Tracks", onTracksClick)
                        PlayerBottomButton(Icons.Default.SkipNext, "Next episode", onNext)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerBottomButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
