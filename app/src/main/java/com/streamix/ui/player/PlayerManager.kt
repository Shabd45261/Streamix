package com.streamix.ui.player

import androidx.compose.runtime.mutableStateOf
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.cloudstream.Episode

object PlayerManager {
    val currentVideo = mutableStateOf<SearchResult?>(null)
    val currentEpisode = mutableStateOf<Episode?>(null)
    val videoLinks = mutableStateOf<List<VideoLink>>(emptyList())
    val relatedVideos = mutableStateOf<List<SearchResult>>(emptyList())
    val episodes = mutableStateOf<List<Episode>>(emptyList())
    val isVisible = mutableStateOf(false)
    val isMinimized = mutableStateOf(false)
    val shouldPause = mutableStateOf(false)
    val isHiddenTemporarily = mutableStateOf(false)
    
    val playbackProgress = mutableStateOf(0f)
    val isPlayingState = mutableStateOf(false)
    val initialPlaybackPosition = mutableStateOf(0L)

    val isLiked = mutableStateOf(false)
    val isDisliked = mutableStateOf(false)

    // Current playlist/queue tracking
    private var playlist = listOf<SearchResult>()
    private var currentIndex = -1

    fun play(
        video: SearchResult,
        links: List<VideoLink>,
        related: List<SearchResult> = emptyList(),
        eps: List<Episode> = emptyList(),
        startPosition: Long = 0L,
        episode: Episode? = null,
        isShort: Boolean = false,
        fullPlaylist: List<SearchResult> = emptyList()
    ) {
        currentVideo.value = video.copy(isShort = isShort)
        currentEpisode.value = episode
        videoLinks.value = links
        relatedVideos.value = related
        episodes.value = eps
        initialPlaybackPosition.value = startPosition
        isVisible.value = true
        isMinimized.value = false
        shouldPause.value = false
        isHiddenTemporarily.value = false
        isLiked.value = false
        isDisliked.value = false

        if (fullPlaylist.isNotEmpty()) {
            playlist = fullPlaylist
            currentIndex = playlist.indexOfFirst { it.id == video.id }
        } else if (related.isNotEmpty()) {
            playlist = listOf(video) + related
            currentIndex = 0
        } else {
            playlist = listOf(video)
            currentIndex = 0
        }
    }

    fun playNext() {
        if (episodes.value.isNotEmpty() && currentEpisode.value != null) {
            val nextIdx = episodes.value.indexOfFirst { it.data == currentEpisode.value?.data } + 1
            if (nextIdx < episodes.value.size) {
                playEpisode(episodes.value[nextIdx])
            }
            return
        }

        if (currentIndex != -1 && currentIndex < playlist.size - 1) {
            val nextVideo = playlist[currentIndex + 1]
            // We need to trigger navigation or loading for the next video
            // Since PlayerManager is just a state holder, we might need a callback or observe this change elsewhere
            // For now, let's just update the state, but usually we need to fetch links.
            // This logic is often handled by the UI or a ViewModel observing PlayerManager.
        }
    }

    fun playPrevious() {
        if (episodes.value.isNotEmpty() && currentEpisode.value != null) {
            val prevIdx = episodes.value.indexOfFirst { it.data == currentEpisode.value?.data } - 1
            if (prevIdx >= 0) {
                playEpisode(episodes.value[prevIdx])
            }
            return
        }

        if (currentIndex > 0) {
            val prevVideo = playlist[currentIndex - 1]
        }
    }

    fun playEpisode(episode: Episode) {
        currentEpisode.value = episode
        // This will trigger a re-load in EmbeddedPlayer because it's observing the episode index or links
    }

    fun pause() {
        shouldPause.value = true
    }

    fun resume() {
        shouldPause.value = false
        isHiddenTemporarily.value = false
    }

    fun minimize() {
        isMinimized.value = true
    }

    fun maximize() {
        isMinimized.value = false
    }

    fun close() {
        isVisible.value = false
        currentVideo.value = null
    }
}
