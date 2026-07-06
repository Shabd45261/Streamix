package com.streamix.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import com.streamix.data.tmdb.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
    private val watchlistDao: WatchlistDao
) : ViewModel() {

    private val _trending = MutableStateFlow<List<SearchResult>>(emptyList())
    val trending = _trending.asStateFlow()

    private val _topRated = MutableStateFlow<List<SearchResult>>(emptyList())
    val topRated = _topRated.asStateFlow()

    private val _history = MutableStateFlow<List<SearchResult>>(emptyList())
    val history = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _showRefreshBanner = MutableStateFlow(false)
    val showRefreshBanner = _showRefreshBanner.asStateFlow()

    private var backPressCount = 0
    private var lastBackPressTime = 0L

    init {
        loadContent()
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            try {
                watchlistDao.getAll().collectLatest { entities ->
                    _history.value = entities.filter { 
                        (it.status == "Watching" || it.status == "On-Hold") &&
                        (it.mediaType == "movie" || it.mediaType == "tv")
                    }.map {
                        SearchResult(
                            id = it.id,
                            title = it.title,
                            posterPath = it.posterPath,
                            mediaType = it.mediaType,
                            year = it.year,
                            views = it.views,
                            rating = it.rating
                        )
                    }
                }
            } catch (e: Throwable) {
                _history.value = emptyList()
            }
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

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun loadContent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _trending.value = tmdbRepository.getTrending()
                _topRated.value = tmdbRepository.getTopRated()
            } catch (e: Exception) {
                _trending.value = emptyList()
                _topRated.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToLibrary(item: SearchResult, status: String) {
        viewModelScope.launch {
            try {
                watchlistDao.insert(
                    WatchlistEntity(
                        id = item.id,
                        title = item.title,
                        posterPath = item.posterPath,
                        mediaType = item.mediaType,
                        year = item.year ?: "",
                        views = item.views,
                        rating = item.rating,
                        status = status
                    )
                )
            } catch (e: Exception) {}
        }
    }
}
