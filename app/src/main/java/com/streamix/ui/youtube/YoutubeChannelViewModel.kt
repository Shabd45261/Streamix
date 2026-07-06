package com.streamix.ui.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.scraper.youtube.YouTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class YoutubeChannelViewModel @Inject constructor(
    private val youtubeScraper: YouTubeScraper
) : ViewModel() {

    private val _channelInfo = MutableStateFlow<ChannelInfo?>(null)
    val channelInfo = _channelInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _tabItems = MutableStateFlow<Map<Int, List<SearchResult>>>(emptyMap())
    val tabItems = _tabItems.asStateFlow()

    private val service = ServiceList.YouTube

    fun loadChannel(channelUrl: String) {
        if (channelUrl.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Jetpack navigation already decodes the argument once.
                // We only decode if it looks like it's still encoded (contains %)
                val finalUrl = if (channelUrl.contains("%")) {
                    try { java.net.URLDecoder.decode(channelUrl, "UTF-8") } catch (e: Exception) { channelUrl }
                } else {
                    channelUrl
                }
                
                val info = withContext(Dispatchers.IO) {
                    ChannelInfo.getInfo(service, finalUrl)
                }
                _channelInfo.value = info
                
                // Load first tab items by default
                val tabs = info.tabs
                if (!tabs.isNullOrEmpty()) {
                    loadTab(0)
                }
            } catch (e: Exception) {
                Log.e("YoutubeChannelVM", "Failed to load channel: $channelUrl", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTab(tabIndex: Int) {
        val info = _channelInfo.value ?: return
        val tabs = info.tabs
        if (tabs.isNullOrEmpty()) return
        val tab = tabs.getOrNull(tabIndex) ?: return
        
        if (_tabItems.value.containsKey(tabIndex)) return

        viewModelScope.launch {
            try {
                val tabInfo = withContext(Dispatchers.IO) {
                    ChannelTabInfo.getInfo(service, tab)
                }
                val items = tabInfo.relatedItems.mapNotNull { item ->
                    when (item) {
                        is StreamInfoItem -> youtubeScraper.run { item.toSearchResult() }
                        is org.schabi.newpipe.extractor.playlist.PlaylistInfoItem -> youtubeScraper.run { item.toSearchResult() }
                        else -> null
                    }
                }
                _tabItems.value = _tabItems.value + (tabIndex to items)
            } catch (e: Exception) {
                Log.e("YoutubeChannelVM", "Failed to load tab $tabIndex", e)
            }
        }
    }
}
