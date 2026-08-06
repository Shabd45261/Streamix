package com.streamix.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.plugin.SearchResult
import com.streamix.scrapers.ScraperRegistry
import com.streamix.scrapers.adult.FikFapScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ShortsUiState {
    object Loading : ShortsUiState()
    data class Success(val items: List<SearchResult>) : ShortsUiState()
    data class Error(val message: String) : ShortsUiState()
}

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val registry: ScraperRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShortsUiState>(ShortsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadShorts()
    }

    fun loadShorts() = viewModelScope.launch {
        _uiState.value = ShortsUiState.Loading
        try {
            val sources = listOf("fikfap", "redtube", "pornhub", "xvideos")
            val allItems = mutableListOf<SearchResult>()
            
            sources.forEach { sourceId ->
                val scraper = registry.getScraperById(sourceId)
                if (scraper != null) {
                    try {
                        val results = scraper.getTrending(1)
                        allItems.addAll(results.map { r ->
                            SearchResult(
                                id = r.id,
                                title = r.title,
                                posterUrl = r.posterUrl,
                                year = null,
                                type = com.streamix.core.plugin.ContentType.MOVIE,
                                sourceId = r.sourceId,
                                description = r.description
                            )
                        })
                    } catch (_: Exception) {}
                }
            }

            if (allItems.isNotEmpty()) {
                _uiState.value = ShortsUiState.Success(allItems.shuffled())
            } else {
                _uiState.value = ShortsUiState.Error("No shorts content found from any source.")
            }
        } catch (e: Exception) {
            _uiState.value = ShortsUiState.Error("Failed to load shorts: ${e.message}")
        }
    }
}
