package com.streamix.ui.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.scraper.adult.*
import com.streamix.scraper.youtube.YouTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

import com.streamix.scraper.cloudstream.ProviderRegistry
import com.streamix.scraper.cloudstream.ExtractorLinkType

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val youtubeScraper: YouTubeScraper,
    private val okxxxScraper: OkxxxScraper,
    private val pornhatScraper: PornhatScraper
) : ViewModel() {

    private val providers = ProviderRegistry.providers
    // ... rest of fields ...

    private val _shorts    = MutableStateFlow<List<ShortsItem>>(emptyList())
    val shorts = _shorts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val likedIds   = mutableSetOf<String>()
    private val dislikedIds = mutableSetOf<String>()
    private var currentAdultPage = 1
    private var isFetchingMore = false

    fun load(context: ShortsContext) {
        viewModelScope.launch {
            _isLoading.value = true
            currentAdultPage = 1
            try {
                val result = when (context) {
                    ShortsContext.YOUTUBE -> loadYoutubeShorts()
                    ShortsContext.ADULT   -> loadAdultShorts(1)
                }
                _shorts.value = result.shuffled()
            } catch (e: Exception) {
                _shorts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore(context: ShortsContext) {
        if (isFetchingMore) return
        
        isFetchingMore = true
        viewModelScope.launch {
            try {
                val more = when (context) {
                    ShortsContext.YOUTUBE -> loadYoutubeShorts()
                    ShortsContext.ADULT   -> {
                        currentAdultPage++
                        loadAdultShorts(currentAdultPage)
                    }
                }
                if (more.isNotEmpty()) {
                    _shorts.value = (_shorts.value + more).distinctBy { it.id }
                }
            } catch (e: Exception) {
                // handle
            } finally {
                isFetchingMore = false
            }
        }
    }

    private suspend fun loadYoutubeShorts(): List<ShortsItem> {
        return try {
            val youtubeItems = youtubeScraper.searchShorts("")
            youtubeItems.map {
                ShortsItem(
                    id = it.id,
                    title = it.title,
                    thumbnailUrl = it.thumbnailUrl,
                    streamUrl = "https://www.youtube.com/watch?v=${it.id}",
                    channelName = it.channelName,
                    views = it.viewCount,
                    source = ShortsContext.YOUTUBE
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun loadAdultShorts(page: Int): List<ShortsItem> = coroutineScope {
        val tasks = ProviderRegistry.providers.map { provider ->
            async {
                try {
                    val mainPage = provider.mainPage.firstOrNull() ?: return@async emptyList()
                    val response = provider.getMainPage(page, mainPage)
                    response?.items?.flatMap { it.list }?.map {
                        ShortsItem(
                            id = it.url,
                            title = it.name,
                            thumbnailUrl = it.posterUrl ?: "",
                            streamUrl = "",
                            channelName = provider.name,
                            views = "",
                            source = ShortsContext.ADULT
                        )
                    } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
        
        val okxxxAsync = async { try { okxxxScraper.search("") } catch(e: Exception) { emptyList() } }
        val pornhatAsync = async { try { pornhatScraper.search("") } catch(e: Exception) { emptyList() } }

        val oldItems = (okxxxAsync.await() + pornhatAsync.await()).map {
            ShortsItem(
                id = it.id,
                title = it.title,
                thumbnailUrl = it.posterPath ?: "",
                streamUrl = "",
                channelName = if (it.id.contains("ok")) "Ok.xxx" else "PornHat",
                views = it.views,
                source = ShortsContext.ADULT
            )
        }

        val combined = (tasks.awaitAll().flatten() + oldItems)
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .shuffled()
        
        val displayList = combined.take(60)

        // Pre-resolve first 5 items
        displayList.mapIndexed { index, item ->
            if (index < 3) {
                async {
                    try {
                        var resolvedUrl = ""
                        val provider = ProviderRegistry.providers.find { item.id.startsWith(it.mainUrl) || item.id.contains(it.name, ignoreCase = true) }
                        if (provider != null) {
                            provider.loadLinks(item.id, false, {}) { link ->
                                resolvedUrl = link.url
                            }
                        }
                        
                        if (resolvedUrl.isEmpty()) {
                            val links = when {
                                item.id.contains("ok.xxx") || item.id.contains("okxxx") -> okxxxScraper.getVideoLinks(item.id, "adult")
                                item.id.contains("pornhat") -> pornhatScraper.getVideoLinks(item.id, "adult")
                                else -> emptyList()
                            }
                            resolvedUrl = links.maxByOrNull { it.quality.contains("720") || it.isM3u8 }?.url ?: links.firstOrNull()?.url ?: ""
                        }
                        
                        item.copy(streamUrl = resolvedUrl)
                    } catch (e: Exception) { item }
                }
            } else {
                async { item }
            }
        }.awaitAll()
    }

    fun resolveStreamUrl(id: String) {
        val currentItem = _shorts.value.find { it.id == id } ?: return
        if (currentItem.streamUrl.isNotEmpty()) return

        viewModelScope.launch {
            try {
                var url = ""
                if (currentItem.source == ShortsContext.YOUTUBE) {
                    val links = youtubeScraper.getVideoLinks(id, "youtube")
                    url = links.maxByOrNull { it.quality.contains("720") || it.quality.contains("1080") }?.url ?: links.firstOrNull()?.url ?: ""
                } else {
                    val provider = ProviderRegistry.providers.find { currentItem.id.startsWith(it.mainUrl) }
                    if (provider != null) {
                        provider.loadLinks(currentItem.id, false, {}) { link ->
                            if (url.isEmpty()) url = link.url
                        }
                    }
                    
                    if (url.isEmpty()) {
                        val links = when {
                            currentItem.id.contains("ok.xxx") || currentItem.id.contains("okxxx") -> okxxxScraper.getVideoLinks(currentItem.id, "adult")
                            currentItem.id.contains("pornhat") -> pornhatScraper.getVideoLinks(currentItem.id, "adult")
                            else -> emptyList()
                        }
                        url = links.maxByOrNull { it.quality.contains("720") || it.isM3u8 }?.url ?: links.firstOrNull()?.url ?: ""
                    }
                }

                if (url.isNotEmpty()) {
                    _shorts.value = _shorts.value.map {
                        if (it.id == id) it.copy(streamUrl = url) else it
                    }
                }
            } catch (e: Exception) { /* log */ }
        }
    }

    fun toggleLike(id: String) {
        if (likedIds.contains(id)) likedIds.remove(id) else {
            likedIds.add(id)
            dislikedIds.remove(id)
        }
        updateItemState(id)
    }

    fun toggleDislike(id: String) {
        if (dislikedIds.contains(id)) dislikedIds.remove(id) else {
            dislikedIds.add(id)
            likedIds.remove(id)
        }
        updateItemState(id)
    }

    private fun updateItemState(id: String) {
        _shorts.value = _shorts.value.map {
            if (it.id == id) it.copy(
                isLiked = likedIds.contains(id),
                isDisliked = dislikedIds.contains(id)
            ) else it
        }
    }
}
