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

    fun play(
        video: SearchResult,
        links: List<VideoLink>,
        related: List<SearchResult> = emptyList(),
        eps: List<Episode> = emptyList(),
        startPosition: Long = 0L,
        episode: Episode? = null,
        isShort: Boolean = false
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
