package com.streamix.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.plugin.*
import com.streamix.scrapers.ScraperRegistry
import com.streamix.scrapers.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val detail: ContentDetail) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val registry: ScraperRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _streams = MutableStateFlow<List<StreamResult>>(emptyList())
    val streams = _streams.asStateFlow()

    private val _streamsLoading = MutableStateFlow(false)
    val streamsLoading = _streamsLoading.asStateFlow()

    fun loadDetail(contentId: String, sourceId: String, title: String? = null, poster: String? = null, desc: String? = null) = viewModelScope.launch {
        _uiState.value = DetailUiState.Loading

        val scraper = registry.getScraperById(sourceId)
        
        // Initial state with what we have
        val initialDetail = ContentDetail(
            id = contentId,
            title = title ?: "Loading...",
            description = desc,
            posterUrl = poster,
            backdropUrl = null,
            type = ContentType.MOVIE
        )
        _uiState.value = DetailUiState.Success(initialDetail)

        if (scraper != null) {
            // Try to get more info if the scraper supports it
            // Since we don't have getDetail in BaseScraper, we just load streams
            loadStreams(contentId, sourceId)
        }
    }

    fun loadStreams(pageUrl: String, sourceId: String) = viewModelScope.launch {
        _streamsLoading.value = true
        val scraper = registry.getScraperById(sourceId)
        if (scraper != null) {
            val links = scraper.getStreamLinks(pageUrl)
            _streams.value = links
        }
        _streamsLoading.value = false
    }
}
