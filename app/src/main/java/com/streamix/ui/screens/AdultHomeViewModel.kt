package com.streamix.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.plugin.ContentType
import com.streamix.core.plugin.SearchResult
import com.streamix.scrapers.BaseScraper
import com.streamix.scrapers.ScraperRegistry
import com.streamix.scrapers.SourcePreferences
import com.streamix.scrapers.ScraperHealthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdultFeedSection(
    val scraperId: String,
    val scraperLabel: String,
    val items: List<SearchResult>,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class AdultHomeState {
    object Loading : AdultHomeState()
    object NoScrapers : AdultHomeState()
    data class Loaded(
        val sections: List<AdultFeedSection>, 
        val heroItems: List<SearchResult>,
        val shortsItems: List<SearchResult> = emptyList()
    ) : AdultHomeState()
    data class Error(val message: String) : AdultHomeState()
}

@HiltViewModel
class AdultHomeViewModel @Inject constructor(
    private val registry: ScraperRegistry,
    private val sourcePrefs: SourcePreferences,
    val healthManager: ScraperHealthManager
) : ViewModel() {

    private val _state = MutableStateFlow<AdultHomeState>(AdultHomeState.Loading)
    val state: StateFlow<AdultHomeState> = _state.asStateFlow()

    // Currently selected scraper ID. null = show all scrapers
    private val _selectedSourceId = MutableStateFlow<String?>(sourcePrefs.getSelectedSource("ADULT"))
    val selectedSourceId: StateFlow<String?> = _selectedSourceId.asStateFlow()

    private val _availableScrapers = MutableStateFlow<List<BaseScraper>>(emptyList())
    val availableScrapers: StateFlow<List<BaseScraper>> = _availableScrapers.asStateFlow()

    init {
        _availableScrapers.value = registry.getAdultScrapers()
        loadHome()
    }

    fun selectSource(scraperId: String?) {
        sourcePrefs.setSelectedSource("ADULT", scraperId)
        _selectedSourceId.value = scraperId
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _state.value = AdultHomeState.Loading

            val scrapers: List<BaseScraper> = when (val id = _selectedSourceId.value) {
                null -> registry.getAdultScrapers()
                else -> listOfNotNull(registry.getScraperById(id))
            }

            if (scrapers.isEmpty()) {
                _state.value = AdultHomeState.NoScrapers
                return@launch
            }

            // Load all scrapers in PARALLEL — one failure never blocks the rest
            val sectionJobs = scrapers.map { scraper ->
                async {
                    try {
                        val items = scraper.getTrending(1)
                        if (items.isEmpty()) {
                            scraper.info.isWorking = false
                            return@async null
                        }
                        scraper.info.isWorking = true
                        AdultFeedSection(
                            scraperId    = scraper.info.id,
                            scraperLabel = "Trending on ${scraper.info.displayName}",
                            items        = items.map { r ->
                                SearchResult(
                                    id        = r.id,
                                    title     = r.title,
                                    posterUrl = r.posterUrl,
                                    year      = null,
                                    type      = ContentType.MOVIE,
                                    sourceId  = r.sourceId,
                                    description = r.description
                                )
                            }
                        )
                    } catch (_: Exception) { null }
                }
            }

            val sections = sectionJobs.awaitAll().filterNotNull()

            if (sections.isEmpty()) {
                _state.value = AdultHomeState.Error(
                    "No content loaded. Check your internet connection or try a different source."
                )
            } else {
                val allItems = sections.flatMap { it.items }.shuffled()
                
                // YouTube-like experience: Randomly pick items for Shorts Shelf from various sources
                val randomShorts = allItems.filter { 
                    it.sourceId == "fikfap" || it.sourceId == "redtube" || it.sourceId == "pornhub" 
                }.shuffled().take(15)
                
                _state.value = AdultHomeState.Loaded(
                    sections = sections,
                    heroItems = allItems.take(10),
                    shortsItems = randomShorts
                )
            }
        }
    }
}
