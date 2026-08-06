package com.streamix.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.streamix.core.PreferenceManager
import com.streamix.core.ProfileManager
import com.streamix.data.ContinueWatchingEntity
import com.streamix.data.LibraryDao
import com.streamix.scrapers.ScraperRegistry
import com.streamix.scrapers.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class PlayerStats(
    val resolution: String = "Unknown",
    val bitrate: String = "0 kbps",
    val fps: Float = 0f,
    val networkSpeed: String = "0 KB/s",
    val bufferHealth: String = "0s",
    val decoderName: String = "Unknown",
    val droppedFrames: Int = 0
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ScraperRegistry,
    private val libraryDao: LibraryDao,
    private val preferenceManager: PreferenceManager,
    val profileManager: ProfileManager
) : ViewModel() {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private val prefs get() = preferenceManager.prefs.value

    private val _streams = MutableStateFlow<List<StreamResult>>(emptyList())
    val streams = _streams.asStateFlow()

    private val _currentStream = MutableStateFlow<StreamResult?>(null)
    val currentStream = _currentStream.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _isBuffering = MutableStateFlow(true)
    val isBuffering = _isBuffering.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Premium Features States
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked = _isLocked.asStateFlow()

    private val _stats = MutableStateFlow(PlayerStats())
    val stats = _stats.asStateFlow()

    private val _tracks = MutableStateFlow<Tracks?>(null)
    val tracks = _tracks.asStateFlow()

    private var progressJob: Job? = null
    private var statsJob: Job? = null
    
    // Metadata for saving progress
    private var currentContentId: String = ""
    private var currentTitle: String = ""
    private var currentPoster: String? = null
    private var currentSourceId: String = ""

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                _isBuffering.value = false
            }

            override fun onPlaybackStateChanged(state: Int) {
                _isBuffering.value = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration.coerceAtLeast(0)
                    _isBuffering.value = false
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _error.value = "Playback error: ${error.message}"
                _isBuffering.value = false
            }

            override fun onTracksChanged(tracks: Tracks) {
                _tracks.value = tracks
            }
        })

        startProgressUpdates()
        startStatsUpdates()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0)
                    _progress.value = pos
                    
                    // Save progress every 5 seconds
                    if (pos > 0 && currentContentId.isNotEmpty()) {
                        saveProgressLocally(pos)
                    }
                }
                delay(1000)
            }
        }
    }

    private fun startStatsUpdates() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val format = exoPlayer.videoFormat
                    _stats.value = PlayerStats(
                        resolution = if (format != null) "${format.width}x${format.height}" else "Unknown",
                        bitrate = if (format != null) "${format.bitrate / 1000} kbps" else "0 kbps",
                        fps = format?.frameRate ?: 0f,
                        bufferHealth = "${(exoPlayer.bufferedPosition - exoPlayer.currentPosition) / 1000}s",
                        decoderName = "Hardware", // Simplified
                        droppedFrames = 0 // Needs more complex listener
                    )
                }
                delay(2000)
            }
        }
    }

    private fun saveProgressLocally(pos: Long) {
        if (duration.value <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            libraryDao.updateProgress(
                ContinueWatchingEntity(
                    id = currentContentId,
                    title = currentTitle,
                    posterUrl = currentPoster,
                    type = com.streamix.core.plugin.ContentType.MOVIE,
                    sourceId = currentSourceId,
                    progressPct = pos.toFloat() / duration.value,
                    positionMs = pos,
                    durationMs = duration.value
                )
            )
        }
    }

    fun loadAndPlay(pageUrl: String, sourceId: String, title: String = "", poster: String? = null) = viewModelScope.launch {
        currentContentId = pageUrl
        currentSourceId = sourceId
        currentTitle = title
        currentPoster = poster

        _isBuffering.value = true
        _error.value = null

        val scraper = registry.getScraperById(sourceId)
        val links = scraper?.getStreamLinks(pageUrl) ?: emptyList()

        if (links.isEmpty()) {
            // Fallback: try playing URL directly if it looks like a media file
            if (pageUrl.startsWith("http")) {
                playStream(StreamResult(url = pageUrl, quality = "Auto", format = if (pageUrl.contains(".m3u8")) "hls" else "mp4"))
            } else {
                _error.value = "No streams found for this content."
                _isBuffering.value = false
            }
            return@launch
        }

        _streams.value = links
        val best = links.firstOrNull { it.format == "hls" } ?: links.first()
        playStream(best)
    }

    fun playStream(stream: StreamResult) {
        _currentStream.value = stream
        _error.value = null
        _isBuffering.value = true

        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(stream.headers)
            setUserAgent("Mozilla/5.0 (Linux; Android 14) Streamix/1.0")
        }

        val mediaItem = MediaItem.Builder()
            .setUri(stream.url)
            .apply {
                if (stream.format.lowercase() == "hls") setMimeType(MimeTypes.APPLICATION_M3U8)
                else if (stream.format.lowercase() == "dash") setMimeType(MimeTypes.APPLICATION_MPD)
            }
            .build()

        val mediaSource = when (stream.format.lowercase()) {
            "hls"  -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            "dash" -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            else   -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        
        // Restore speed if enabled
        if (prefs.rememberSpeed) {
            setSpeed(prefs.lastSpeed)
        }
        
        exoPlayer.playWhenReady = true
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun seekTo(posMs: Long) {
        exoPlayer.seekTo(posMs.coerceIn(0, duration.value))
    }

    fun seekBy(deltaMs: Long) {
        exoPlayer.seekTo((exoPlayer.currentPosition + deltaMs).coerceIn(0, duration.value))
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.setPlaybackSpeed(speed)
        if (prefs.rememberSpeed) {
            preferenceManager.updatePrefs { it.copy(lastSpeed = speed) }
        }
    }

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
    }

    fun setAspectRatio(ratio: VideoAspectRatio) {
        // This is mostly handled by the PlayerView in the UI
        preferenceManager.updatePrefs { it.copy(aspectRatio = ratio) }
    }

    fun selectTrack(groupIndex: Int, trackIndex: Int) {
        val tracks = _tracks.value ?: return
        val group = tracks.groups[groupIndex]
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
            )
            .build()
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        statsJob?.cancel()
        exoPlayer.release()
    }
}
