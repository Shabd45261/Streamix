package com.streamix.ui.shorts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ShortsPlaylistManager {
    private val _playlistRequest = MutableStateFlow<Pair<String, String>?>(null)
    val playlistRequest = _playlistRequest.asStateFlow()

    fun setPlaylist(channelUrl: String, startVideoId: String) {
        _playlistRequest.value = channelUrl to startVideoId
    }

    fun clear() {
        _playlistRequest.value = null
    }
}
