package com.streamix.ui.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.core.storage.WatchHistoryDao
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import com.streamix.data.scraper.MoviesScraperRepository
import com.streamix.data.scraper.MovieHomeRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MoviesHomeViewModel @Inject constructor(
    private val repo: MoviesScraperRepository,
    private val historyDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao
) : ViewModel() {

    private val _homeRows = MutableStateFlow<List<MovieHomeRow>>(emptyList())
    val homeRows = _homeRows.asStateFlow()

    private val _selectedProvider = MutableStateFlow("MovieBoxPh")
    val selectedProvider = _selectedProvider.asStateFlow()

    private val _history = MutableStateFlow<List<SearchResult>>(emptyList())
    val history = _history.asStateFlow()

    private val _watchlist = MutableStateFlow<List<SearchResult>>(emptyList())
    val watchlist = _watchlist.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _showRefreshBanner = MutableStateFlow(false)
    val showRefreshBanner = _showRefreshBanner.asStateFlow()

    private var searchJob: Job? = null
    private var backPressCount = 0
    private var lastBackPressTime = 0L

    init {
        viewModelScope.launch {
            loadHomeData()
        }
        observeHistory()
        observeWatchlist()
    }

    private suspend fun loadHomeData() {
        _isLoading.value = true
        try {
            Log.d("MoviesVM", "Loading home data rows...")
            val rows = repo.getHomeRows()
            Log.d("MoviesVM", "Loaded ${rows.size} rows")
            _homeRows.value = rows
        } catch (e: Exception) {
            Log.e("MoviesVM", "Load failed", e)
        } finally {
            _isLoading.value = false
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
            _isLoading.value = true
            try {
                Log.d("MoviesVM", "Searching for: $q")
                val results = repo.search(q)
                Log.d("MoviesVM", "Found ${results.size} results")
                _searchResults.value = results
            } catch (e: Exception) {
                Log.e("MoviesVM", "Search failed", e)
                _searchResults.value = emptyList()
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
                    mediaType = item.mediaType,
                    status = status
                )
            )
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            historyDao.getAllHistory()
                .map { entities -> 
                    entities.filter { it.mediaType == "movie" || it.mediaType == "tv" }
                        .map { it.toSearchResult() } 
                }
                .collect { _history.value = it }
        }
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            watchlistDao.getAll()
                .map { entities ->
                    entities.filter { it.mediaType == "movie" || it.mediaType == "tv" }
                        .map { it.toSearchResult() }
                }
                .collect { _watchlist.value = it }
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

    private fun WatchlistEntity.toSearchResult() = SearchResult(
        id = id,
        title = title,
        posterPath = posterPath,
        mediaType = mediaType
    )
}
