package com.streamix.ui.adult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.core.storage.WatchHistoryDao
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import com.streamix.data.scraper.AdultScraperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class AdultHomeViewModel @Inject constructor(
    private val repo: AdultScraperRepository,
    private val historyDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao
) : ViewModel() {

    private val _trending      = MutableStateFlow<List<SearchResult>>(emptyList())
    val trending = _trending.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _history       = MutableStateFlow<List<SearchResult>>(emptyList())
    val history = _history.asStateFlow()

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
        loadTrending()
        observeHistory()
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

    private fun observeHistory() {
        viewModelScope.launch {
            historyDao.getAllHistory()
                .catch { _history.value = emptyList() }
                .collectLatest { entities ->
                    _history.value = entities.map { 
                        SearchResult(
                            id = it.id,
                            title = it.title,
                            posterPath = it.posterPath,
                            mediaType = it.mediaType,
                            duration = it.duration,
                            views = it.views,
                            rating = it.rating,
                            progress = it.progress,
                            totalDuration = it.totalDuration
                        )
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadTrending()
            _isRefreshing.value = false
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = withContext(Dispatchers.IO) {
                    repo.getTrending(1)
                        .filter { it.id.isNotBlank() }
                        .distinctBy { it.id }
                        .take(100)
                }
                _trending.value = results.shuffled()
            } catch (e: Exception) {
                _trending.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onQueryChange(q: String) {
        _searchQuery.value = q
        searchJob?.cancel()
        if (q.isBlank()) { 
            _searchResults.value = emptyList()
            return 
        }
        searchJob = viewModelScope.launch {
            delay(300)
            search(q)
        }
    }

    fun search(q: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = withContext(Dispatchers.IO) {
                    repo.search(q)
                        .filter { it.id.isNotBlank() }
                        .distinctBy { it.id }
                        .take(80)
                }
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun removeFromHistory(id: String) {
        viewModelScope.launch {
            try { historyDao.deleteHistory(id) } catch (e: Exception) {}
        }
    }

    fun addToLibrary(item: SearchResult) {
        viewModelScope.launch {
            try {
                watchlistDao.insert(
                    WatchlistEntity(
                        id = item.id,
                        title = item.title,
                        posterPath = item.posterPath,
                        mediaType = item.mediaType,
                        year = item.year,
                        views = item.views,
                        rating = item.rating,
                        status = "Plan to Watch"
                    )
                )
            } catch (e: Exception) {}
        }
    }
}
