package com.streamix.ui.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import com.streamix.core.model.SearchResult
import com.streamix.scraper.adult.*
import com.streamix.scraper.youtube.YouTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.streamix.core.storage.PreferencesManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Provider
import android.util.Log

import com.streamix.scraper.cloudstream.ProviderRegistry
import com.streamix.scraper.cloudstream.ExtractorLinkType

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val youtubeScraper: YouTubeScraper,
    private val okxxxScraperProvider: Provider<OkxxxScraper>,
    private val pornhatScraperProvider: Provider<PornhatScraper>,
    private val watchlistDao: WatchlistDao,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val okxxxScraper get() = okxxxScraperProvider.get()
    private val pornhatScraper get() = pornhatScraperProvider.get()

    private val providers = ProviderRegistry.providers

    private val _shorts    = MutableStateFlow<List<ShortsItem>>(emptyList())
    val shorts = _shorts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ShortsItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private var channelPlaylistInfo: Pair<String, String>? = null // channelUrl, startVideoId

    private val likedIds   = mutableSetOf<String>()
    private val dislikedIds = mutableSetOf<String>()
    private var currentAdultPage = 1
    private var isFetchingMore = false

    private var youtubeFeedPage = 0

    init {
        viewModelScope.launch {
            ShortsPlaylistManager.playlistRequest.collect { request ->
                request?.let { (url, videoId) ->
                    loadChannelShorts(url, videoId)
                    ShortsPlaylistManager.clear()
                }
            }
        }
    }

    fun loadChannelShorts(channelUrl: String, startVideoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val channelInfo = withContext(Dispatchers.IO) {
                    org.schabi.newpipe.extractor.channel.ChannelInfo.getInfo(org.schabi.newpipe.extractor.ServiceList.YouTube, channelUrl)
                }
                val shortsTab = channelInfo.tabs.find { it.contentFilters?.any { f -> f.contains("shorts", true) } == true }
                val channelShorts = if (shortsTab != null) {
                    withContext(Dispatchers.IO) {
                        org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo.getInfo(org.schabi.newpipe.extractor.ServiceList.YouTube, shortsTab)
                    }.relatedItems.filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>().map { 
                        youtubeScraper.run { it.toSearchResult().toShortsItem() }
                    }
                } else emptyList()

                val startIndex = channelShorts.indexOfFirst { it.id == startVideoId }.coerceAtLeast(0)
                val sortedShorts = channelShorts.drop(startIndex).take(21)
                
                val regularFeed = loadYoutubeShortsFeed()
                _shorts.value = (sortedShorts + regularFeed).distinctBy { it.id }
            } catch (e: Exception) {
                Log.e("ShortsVM", "Failed to load channel shorts", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun load(context: ShortsContext) {
        viewModelScope.launch {
            _isLoading.value = true
            currentAdultPage = 1
            youtubeFeedPage = 0
            _searchQuery.value = ""
            try {
                val result = when (context) {
                    ShortsContext.YOUTUBE -> loadYoutubeShortsFeed()
                    ShortsContext.ADULT   -> loadAdultShorts(1)
                }
                _shorts.value = if (context == ShortsContext.ADULT) result.shuffled() else result
            } catch (e: Exception) {
                _shorts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private var searchShortsJob: Job? = null

    fun searchShorts(query: String) {
        _searchQuery.value = query
        searchShortsJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isLoading.value = false
            return
        }
        
        searchShortsJob = viewModelScope.launch {
            // Highly aggressive debounce for fast typing
            val debounceTime = if (query.length < 3) 300L else 150L
            delay(debounceTime)

            _isLoading.value = true
            try {
                // Save search query to history for personalization
                viewModelScope.launch { prefs.addShortsSearch(query) }
                
                // Use the strictly filtered shorts search from the scraper
                // targetTotal=150 ensures we get at least 100 distinct items
                val results = mutableListOf<ShortsItem>()
                youtubeScraper.searchShortsIncremental(query, targetTotal = 150) { newResults ->
                    results.addAll(newResults.map { it.toShortsItem() })
                    _searchResults.value = results.distinctBy { it.id }
                }
                
                if (results.isEmpty()) {
                    // One last try with normal search if incremental failed or was empty
                    val fallback = youtubeScraper.search(query)
                        .filter { it.duration.isEmpty() || !it.duration.contains(":") || (it.duration.split(":").getOrNull(0)?.toIntOrNull() ?: 0) < 2 }
                        .map { it.toShortsItem() }
                    _searchResults.value = fallback
                }
            } catch (e: Exception) {
                Log.e("ShortsVM", "Search failed for: $query", e)
                // Don't clear results on error if we already have some
                if (_searchResults.value.isEmpty()) {
                    _searchResults.value = emptyList()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playSearchShort(index: Int) {
        val results = _searchResults.value
        if (results.isEmpty() || index < 0 || index >= results.size) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Feature: Start from clicked item, then play everything from the start of the list
                val clickedItem = results[index]
                val remainingResults = results.filterIndexed { i, _ -> i != index }
                
                // Construct playlist as [ClickedItem, Item0, Item1, ..., ItemN]
                val playlist = (listOf(clickedItem) + remainingResults).distinctBy { it.id }
                
                _shorts.value = playlist
                _searchQuery.value = "" // Reset search UI
                _searchResults.value = emptyList()
            } catch (e: Exception) {
                Log.e("ShortsVM", "Failed to start search playlist", e)
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
                    ShortsContext.YOUTUBE -> {
                        youtubeFeedPage++
                        loadYoutubeShortsFeed()
                    }
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

    private suspend fun loadYoutubeShortsFeed(): List<ShortsItem> {
        return try {
            val cookies = prefs.youtubeCookies.first()
            val localSubs = prefs.subscribedChannels.first()
            val interests = prefs.userInterests.first()
            val searchHistory = prefs.shortsSearchHistory.first()
            
            val baseShorts = youtubeScraper.getShortsFeed()
            
            // Fetch some shorts from interests and search history to personalize the feed
            val personalizedShorts = mutableListOf<SearchResult>()
            
            val combinedTopics = (interests + searchHistory).toList().shuffled().take(5)
            if (combinedTopics.isNotEmpty()) {
                coroutineScope {
                    combinedTopics.map { topic ->
                        async {
                            try {
                                // Use limit=10 and fast search for personalization to avoid long loading times
                                youtubeScraper.searchShorts(topic, limit = 10)
                            } catch (e: Exception) { emptyList<SearchResult>() }
                        }
                    }.awaitAll().forEach { personalizedShorts.addAll(it) }
                }
            }
            
            // Fetch some shorts from subscribed channels to boost them in the feed
            val localSubsShorts = if (localSubs.isNotEmpty()) {
                localSubs.take(5).flatMap { channelName ->
                    try { 
                        youtubeScraper.searchShorts(channelName).take(4) 
                    } catch (e: Exception) { emptyList() }
                }.map { it.toShortsItem() }
            } else emptyList()

            val combined = if (!cookies.isNullOrBlank() || localSubsShorts.isNotEmpty() || personalizedShorts.isNotEmpty()) {
                // If logged in or has local subs/interests, mix them in with standard topics
                val topics = listOf("shorts", "trending shorts", "recommended shorts", "funny shorts")
                val standardResults = youtubeScraper.search(topics.random()).map { it.toShortsItem() }
                
                (baseShorts.map { it.toShortsItem() } + standardResults + localSubsShorts + personalizedShorts.map { it.toShortsItem() }).distinctBy { it.id }
            } else {
                baseShorts.map { it.toShortsItem() }
            }
            
            // Shuffling ensures regular and personalized content are mixed
            combined.shuffled()
        } catch (e: Exception) { emptyList() }
    }

    private fun com.streamix.core.model.SearchResult.toShortsItem(): ShortsItem {
        return ShortsItem(
            id = this.id,
            title = this.title,
            thumbnailUrl = this.posterPath ?: "",
            streamUrl = "", // resolve on demand
            channelName = this.studio,
            views = this.views,
            source = ShortsContext.YOUTUBE
        )
    }

    private suspend fun loadYoutubeShorts(): List<ShortsItem> {
        return loadYoutubeShortsFeed()
    }

    private suspend fun loadAdultShorts(page: Int): List<ShortsItem> = coroutineScope {
        val pornhat = ProviderRegistry.getProviderByName("PornHat")
        val mainPage = pornhat?.mainPage?.firstOrNull() ?: return@coroutineScope emptyList()
        
        val pornhatItems = try {
            val response = pornhat.getMainPage(page, mainPage)
            response?.items?.flatMap { it.list }?.map {
                ShortsItem(
                    id = it.url,
                    title = it.name,
                    thumbnailUrl = it.posterUrl ?: "",
                    streamUrl = "",
                    channelName = "PornHat",
                    views = "",
                    source = ShortsContext.ADULT
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        
        val directPornhatItems = try { 
            pornhatScraper.search("")
                .filter { it.id.isNotBlank() }
                .map {
                    ShortsItem(
                        id = it.id,
                        title = it.title,
                        thumbnailUrl = it.posterPath ?: "",
                        streamUrl = "",
                        channelName = "PornHat",
                        views = it.views,
                        source = ShortsContext.ADULT
                    )
                }
        } catch (e: Exception) { emptyList() }

        val combined = (pornhatItems + directPornhatItems)
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .shuffled()
        
        val displayList = combined.take(60)

        // Pre-resolve first 3 items
        displayList.mapIndexed { index, item ->
            if (index < 3) {
                async {
                    try {
                        val links = pornhatScraper.getVideoLinks(item.id, "adult")
                        val resolvedUrl = links.maxByOrNull { it.quality.contains("720") || it.isM3u8 }?.url 
                            ?: links.firstOrNull()?.url ?: ""
                        item.copy(streamUrl = resolvedUrl)
                    } catch (e: Exception) { item }
                }
            } else {
                async { item }
            }
        }.awaitAll()
    }

    fun resolveStreamUrl(id: String) {
        val currentIndex = _shorts.value.indexOfFirst { it.id == id }
        if (currentIndex == -1) return
        
        // Resolve current and next 2 items
        (currentIndex until (currentIndex + 3).coerceAtMost(_shorts.value.size)).forEach { index ->
            val item = _shorts.value[index]
            if (item.streamUrl.isEmpty()) {
                viewModelScope.launch {
                    try {
                        var url = ""
                        if (item.source == ShortsContext.YOUTUBE) {
                            val links = youtubeScraper.getVideoLinks(item.id, "youtube")
                            url = links.maxByOrNull { it.quality.contains("720") || it.quality.contains("1080") }?.url ?: links.firstOrNull()?.url ?: ""
                        } else {
                            // ... existing adult resolution ...
                            val provider = ProviderRegistry.providers.find { item.id.startsWith(it.mainUrl) }
                            if (provider != null) {
                                provider.loadLinks(item.id, false, {}) { link ->
                                    if (url.isEmpty()) url = link.url
                                }
                            }
                            
                            if (url.isEmpty()) {
                                val links = when {
                                    item.id.contains("ok.xxx") || item.id.contains("okxxx") -> okxxxScraper.getVideoLinks(item.id, "adult")
                                    item.id.contains("pornhat") -> pornhatScraper.getVideoLinks(item.id, "adult")
                                    else -> emptyList()
                                }
                                url = links.maxByOrNull { it.quality.contains("720") || it.isM3u8 }?.url ?: links.firstOrNull()?.url ?: ""
                            }
                        }

                        if (url.isNotEmpty()) {
                            _shorts.value = _shorts.value.map {
                                if (it.id == item.id) it.copy(streamUrl = url) else it
                            }
                        }
                    } catch (e: Exception) { /* log */ }
                }
            }
        }
    }

    fun toggleLike(id: String) {
        val item = _shorts.value.find { it.id == id }
        if (likedIds.contains(id)) {
            likedIds.remove(id)
            viewModelScope.launch { watchlistDao.deleteById(id) }
        } else {
            likedIds.add(id)
            dislikedIds.remove(id)
            item?.let {
                viewModelScope.launch {
                    watchlistDao.insert(
                        WatchlistEntity(
                            id = it.id,
                            title = it.title,
                            posterPath = it.thumbnailUrl,
                            mediaType = if (it.source == ShortsContext.YOUTUBE) "youtube" else "adult",
                            status = "Liked",
                            isShort = true
                        )
                    )
                }
            }
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
