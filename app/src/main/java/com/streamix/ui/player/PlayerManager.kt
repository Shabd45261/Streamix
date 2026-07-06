package com.streamix.ui.player

import androidx.compose.runtime.mutableStateOf
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.cloudstream.Episode

object PlayerManager {
    val currentVideo = mutableStateOf<SearchResult?>(null)
    val videoLinks = mutableStateOf<List<VideoLink>>(emptyList())
    val relatedVideos = mutableStateOf<List<SearchResult>>(emptyList())
    val episodes = mutableStateOf<List<Episode>>(emptyList())
    val isVisible = mutableStateOf(false)
    val isMinimized = mutableStateOf(false)
    val shouldPause = mutableStateOf(false)
    val isHiddenTemporarily = mutableStateOf(false)
    
    val isLiked = mutableStateOf(false)
    val isDisliked = mutableStateOf(false)

    fun play(
        video: SearchResult,
        links: List<VideoLink>,
        related: List<SearchResult> = emptyList(),
        eps: List<Episode> = emptyList()
    ) {
        currentVideo.value = video
        videoLinks.value = links
        relatedVideos.value = related
        episodes.value = eps
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
