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
import kotlinx.coroutines.flow.*
import com.streamix.core.storage.PreferencesManager
import javax.inject.Inject
import javax.inject.Provider
import android.util.Log

import com.streamix.scraper.cloudstream.ProviderRegistry
import com.streamix.scraper.cloudstream.ExtractorLinkType

enum class ShortsSource { FEED, SEARCH, CHANNEL }

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val youtubeScraper: YouTubeScraper,
    private val okxxxScraperProvider: Provider<OkxxxScraper>,
    private val pornhatScraperProvider: Provider<PornhatScraper>,
    private val watchlistDao: WatchlistDao,
    private val historyDao: com.streamix.core.storage.WatchHistoryDao,
    private val prefs: PreferencesManager,
    private val searchHistoryManager: com.streamix.core.storage.SearchHistoryManager
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

    private var currentContext: ShortsContext = ShortsContext.YOUTUBE

    val searchHistory: StateFlow<List<String>> = _searchQuery.flatMapLatest {
        searchHistoryManager.getShortsHistory(currentContext == ShortsContext.ADULT)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeHistoryItem(q: String) {
        viewModelScope.launch { 
            searchHistoryManager.removeShortsSearch(currentContext == ShortsContext.ADULT, q)
        }
    }

    private var currentSource = ShortsSource.FEED

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
            currentSource = ShortsSource.CHANNEL
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
            currentContext = context
            currentSource = ShortsSource.FEED
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

    fun searchShorts(query: String, addToHistory: Boolean = false) {
        _searchQuery.value = query
        searchShortsJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isLoading.value = false
            return
        }
        
        searchShortsJob = viewModelScope.launch {
            if (addToHistory) {
                searchHistoryManager.addShortsSearch(currentContext == ShortsContext.ADULT, query)
            }

            // Highly aggressive debounce for fast typing
            val debounceTime = if (query.length < 3) 300L else 150L
            delay(debounceTime)

            _isLoading.value = true
            try {
                // Use the strictly filtered shorts search from the scraper
                // targetTotal=150 ensures we get at least 100 distinct items
                val results = mutableListOf<ShortsItem>()
                youtubeScraper.searchShortsIncremental(query, targetTotal = 100) { newResults ->
                    val mapped = newResults.map { it.toShortsItem() }
                    results.addAll(mapped)
                    // Update UI as we get results, with a minimum chunk size or periodically
                    _searchResults.value = results.distinctBy { it.id }
                }
                
                val finalResults = results.distinctBy { it.id }
                if (finalResults.isEmpty()) {
                    // One last try with normal search if incremental failed or was empty
                    val fallback = youtubeScraper.search(query)
                        .filter { it.duration.isEmpty() || !it.duration.contains(":") || (it.duration.split(":").getOrNull(0)?.toIntOrNull() ?: 0) < 2 }
                        .map { it.toShortsItem() }
                    _searchResults.value = fallback
                } else {
                    _searchResults.value = finalResults
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
            currentSource = ShortsSource.SEARCH
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
        if (isFetchingMore || currentSource == ShortsSource.SEARCH) return
        
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

    private suspend fun loadYoutubeShortsFeed(): List<ShortsItem> = coroutineScope {
        try {
            val watchedIds = historyDao.getAllHistory()
                .first()
                .filter { it.mediaType == "youtube" && it.isShort }
                .map { it.id }
                .toSet()

            val likedIdsFromDb = watchlistDao.getByStatus("Liked")
                .first()
                .filter { it.mediaType == "youtube" && it.isShort }
                .map { it.id }
                .toSet()
            
            val seenIds = watchedIds + likedIdsFromDb

            val localSubs = prefs.subscribedChannels.first().toList().shuffled()
            val shortsSearchHistory = prefs.ytShortsSearchHistory.first().take(10).shuffled()
            
            val subShortsDeferred = localSubs.take(20).map { url ->
                async {
                    try { youtubeScraper.getChannelShorts(url).map { it.toShortsItem() } } catch (e: Exception) { emptyList() }
                }
            }
            
            val searchShortsDeferred = shortsSearchHistory.take(10).map { query ->
                async {
                    try { youtubeScraper.searchShorts(query, limit = 20).map { it.toShortsItem() } } catch (e: Exception) { emptyList() }
                }
            }
            
            val subShortsLists = subShortsDeferred.awaitAll()
            val searchShortsLists = searchShortsDeferred.awaitAll()
            
            val allSources = (subShortsLists + searchShortsLists).filter { it.isNotEmpty() }
            
            val resultList = mutableListOf<ShortsItem>()
            val addedIds = mutableSetOf<String>()

            // 1. Interleave Fresh from all sources for equal representation
            val freshSources = allSources.map { list -> list.filter { it.id !in seenIds } }.filter { it.isNotEmpty() }
            if (freshSources.isNotEmpty()) {
                interleave(freshSources).forEach { 
                    if (addedIds.add(it.id)) resultList.add(it)
                }
            }
            
            // 2. Add from general feed (fresh)
            if (resultList.size < 50) {
                youtubeScraper.getShortsFeed()
                    .map { it.toShortsItem() }
                    .filter { it.id !in seenIds && it.id !in addedIds }
                    .shuffled()
                    .forEach { if (addedIds.add(it.id)) resultList.add(it) }
            }
            
            // 3. Fallback to Seen ONLY if too few total items collected
            if (resultList.size < 20) {
                val seenShorts = allSources.flatten().filter { it.id in seenIds && it.id !in addedIds }.shuffled()
                seenShorts.forEach {
                    if (addedIds.add(it.id)) resultList.add(it)
                }
                
                if (resultList.size < 20) {
                    youtubeScraper.getShortsFeed()
                        .map { it.toShortsItem() }
                        .filter { it.id !in addedIds }
                        .shuffled()
                        .forEach { if (addedIds.add(it.id)) resultList.add(it) }
                }
            }

            resultList.take(150)
        } catch (e: Exception) {
            Log.e("ShortsVM", "Error loading personalized shorts feed", e)
            youtubeScraper.getShortsFeed().map { it.toShortsItem() }.shuffled()
        }
    }

    private fun <T> interleave(lists: List<List<T>>): List<T> {
        val result = mutableListOf<T>()
        val iterators = lists.filter { it.isNotEmpty() }.map { it.iterator() }.toMutableList()
        while (iterators.isNotEmpty()) {
            val it = iterators.iterator()
            while (it.hasNext()) {
                val listIt = it.next()
                if (listIt.hasNext()) {
                    result.add(listIt.next())
                } else {
                    it.remove()
                }
            }
        }
        return result
    }

    private fun com.streamix.core.model.SearchResult.toShortsItem(): ShortsItem {
        return ShortsItem(
            id = this.id,
            title = this.title,
            thumbnailUrl = this.posterPath ?: "",
            streamUrl = "", // resolve on demand
            channelName = this.studio,
            channelAvatarUrl = this.studioAvatarUrl ?: "",
            channelUrl = this.studioUrl ?: "",
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

    fun saveToHistory(id: String, context: ShortsContext) {
        val item = _shorts.value.find { it.id == id } ?: return
        viewModelScope.launch {
            historyDao.insertHistory(
                com.streamix.core.storage.WatchHistoryEntity(
                    id = item.id,
                    title = item.title,
                    posterPath = item.thumbnailUrl,
                    mediaType = if (context == ShortsContext.YOUTUBE) "youtube" else "adult",
                    isShort = true,
                    lastWatchedAt = System.currentTimeMillis()
                )
            )
        }
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
                        var avatarUrl = ""
                        var chUrl = ""
                        if (item.source == ShortsContext.YOUTUBE) {
                            val info = youtubeScraper.getFullStreamInfo(item.id)
                            val links = info?.let { youtubeScraper.getVideoLinksFromInfo(it) } ?: emptyList()
                            url = links.maxByOrNull { it.quality.contains("720") || it.quality.contains("1080") }?.url ?: links.firstOrNull()?.url ?: ""
                            avatarUrl = info?.uploaderAvatars?.lastOrNull()?.getUrl() ?: ""
                            chUrl = info?.uploaderUrl ?: ""
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

                        if (url.isNotEmpty() || avatarUrl.isNotEmpty()) {
                            _shorts.value = _shorts.value.map {
                                if (it.id == item.id) it.copy(
                                    streamUrl = if (url.isNotEmpty()) url else it.streamUrl,
                                    channelAvatarUrl = if (avatarUrl.isNotEmpty()) avatarUrl else it.channelAvatarUrl,
                                    channelUrl = if (chUrl.isNotEmpty()) chUrl else it.channelUrl
                                ) else it
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
