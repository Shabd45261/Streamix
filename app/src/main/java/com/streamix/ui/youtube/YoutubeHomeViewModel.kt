package com.streamix.ui.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.scraper.youtube.YouTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YoutubeHomeViewModel @Inject constructor(
    private val youtubeScraper: YouTubeScraper
) : ViewModel() {

    private val _trending      = MutableStateFlow<List<YoutubeVideoItem>>(emptyList())
    val trending = _trending.asStateFlow()

    private val _searchResults = MutableStateFlow<List<YoutubeVideoItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searchQuery   = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading     = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _showRefreshBanner = MutableStateFlow(false)
    val showRefreshBanner = _showRefreshBanner.asStateFlow()

    private var backPressCount = 0
    private var lastBackPressTime = 0L

    private var searchJob: Job? = null

    init { loadTrending() }

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

    fun loadTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _trending.value = youtubeScraper.getTrending().take(30)
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
            delay(800)
            search(q)
        }
    }

    fun search(q: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _searchResults.value = youtubeScraper.search(q).map {
                    YoutubeVideoItem(
                        id = it.id,
                        title = it.title,
                        thumbnailUrl = it.posterPath ?: ""
                    )
                }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
