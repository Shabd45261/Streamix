package com.streamix.ui.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.core.storage.WatchHistoryDao
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import com.streamix.core.storage.PreferencesManager
import com.streamix.scraper.youtube.YouTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class YoutubeHomeViewModel @Inject constructor(
    private val youtubeScraper: YouTubeScraper,
    private val historyDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao,
    private val prefs: PreferencesManager,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _trending      = MutableStateFlow<List<SearchResult>>(emptyList())
    val trending = _trending.asStateFlow()

    private val _history       = MutableStateFlow<List<SearchResult>>(emptyList())
    val history = _history.asStateFlow()

    private val _subscribed    = MutableStateFlow<List<SearchResult>>(emptyList())
    val subscribed = _subscribed.asStateFlow()

    private val _recommended   = MutableStateFlow<List<SearchResult>>(emptyList())
    val recommended = _recommended.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searchQuery   = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading     = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _showRefreshBanner = MutableStateFlow(false)
    val showRefreshBanner = _showRefreshBanner.asStateFlow()

    private var searchJob: Job? = null
    private var backPressCount = 0
    private var lastBackPressTime = 0L

    init {
        val hasLoaded = savedStateHandle.get<Boolean>("has_loaded") ?: false
        if (!hasLoaded) {
            loadHomeData()
            savedStateHandle["has_loaded"] = true
        }
        observeHistory()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val localSubs = prefs.subscribedChannels.first()
                coroutineScope {
                    val homeJob = async { 
                        try { youtubeScraper.getHomeFeed() } catch (e: Exception) { emptyList() }
                    }
                    val trendingJob = async { 
                        try { youtubeScraper.getTrending() } catch (e: Exception) { emptyList() }
                    }
                    val subsJob = async { 
                        try { youtubeScraper.getSubscriptionFeed() } catch (e: Exception) { emptyList() }
                    }
                    val recJob = async { 
                        try { youtubeScraper.getRecommended() } catch (e: Exception) { emptyList() }
                    }
                    
                    // Inject local subscriptions if any
                    val localSubsJobs = localSubs.take(5).map { channelName ->
                        async {
                            try { youtubeScraper.search(channelName).take(6) } catch (e: Exception) { emptyList() }
                        }
                    }

                    val homeFeed = homeJob.await()
                    val trendFeed = trendingJob.await()
                    val subsFeed = subsJob.await()
                    val recFeed = recJob.await()
                    val localSubsVideos = localSubsJobs.awaitAll().flatten()

                    val combined = (homeFeed + trendFeed + localSubsVideos).distinctBy { it.id }
                    if (combined.isNotEmpty()) {
                        _trending.value = combined.shuffled()
                    } else {
                        _trending.value = youtubeScraper.search("trending")
                    }

                    val finalSubs = (subsFeed + localSubsVideos).distinctBy { it.id }
                    _subscribed.value = if (finalSubs.isNotEmpty()) finalSubs else youtubeScraper.search("popular channels")
                    
                    val finalRec = (recFeed + localSubsVideos.shuffled()).distinctBy { it.id }
                    _recommended.value = if (finalRec.isNotEmpty()) finalRec else youtubeScraper.search("suggested videos")
                }
            } catch (e: Exception) {
                Log.e("YoutubeVM", "Load failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            historyDao.getAllHistory()
                .map { entities -> 
                    entities.filter { it.mediaType == "youtube" }
                        .map { it.toSearchResult() } 
                }
                .collect { _history.value = it }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadHomeData()
            _isRefreshing.value = false
        }
    }

    fun onBackPress(onExit: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime > 2000) {
            backPressCount = 0
        }
        backPressCount++
        lastBackPressTime = now

        if (backPressCount >= 1) {
            _showRefreshBanner.value = true
        }
        
        if (backPressCount >= 2) {
            onExit()
        }
    }

    fun dismissRefreshBanner() {
        _showRefreshBanner.value = false
        backPressCount = 0
    }

    fun onQueryChange(q: String) {
        _searchQuery.value = q
        searchJob?.cancel()
        if (q.isBlank()) { 
            _searchResults.value = emptyList()
            return 
        }
        searchJob = viewModelScope.launch {
            delay(500)
            search(q)
        }
    }

    fun search(q: String) {
        if (q.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = _searchResults.value.isEmpty() // Only show full loader if no results yet
            try {
                Log.d("YoutubeVM", "Searching for: $q")
                val results = youtubeScraper.search(q)
                
                // Group results: Channels first, then videos
                val channels = results.filter { it.mediaType == "youtube_channel" }
                val videos = results.filter { it.mediaType != "youtube_channel" }
                
                _searchResults.value = channels + videos
            } catch (e: Exception) {
                Log.e("YoutubeVM", "Search failed", e)
                if (_searchResults.value.isEmpty()) _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToLibrary(item: SearchResult, status: String) {
        viewModelScope.launch {
            watchlistDao.insert(
                WatchlistEntity(
                    id = item.id,
                    title = item.title,
                    posterPath = item.posterPath,
                    mediaType = "youtube",
                    status = status
                )
            )
        }
    }

    private fun com.streamix.core.storage.WatchHistoryEntity.toSearchResult() = SearchResult(
        id = id,
        title = title,
        posterPath = posterPath,
        mediaType = mediaType,
        duration = duration,
        views = views,
        rating = rating,
        progress = progress,
        totalDuration = totalDuration
    )
}
