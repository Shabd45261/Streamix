package com.streamix.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.PreferenceManager
import com.streamix.core.ProfileManager
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

data class ContentRow(
    val title: String,
    val items: List<SearchResult>
)

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val featured: List<SearchResult>, val rows: List<ContentRow>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val registry: ScraperRegistry,
    private val sourcePrefs: SourcePreferences,
    private val profileManager: ProfileManager,
    private val preferenceManager: PreferenceManager,
    val healthManager: ScraperHealthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedSourceId = MutableStateFlow<String?>(null)
    val selectedSourceId: StateFlow<String?> = _selectedSourceId.asStateFlow()

    // All scrapers available for current profile (for the source selector list)
    private val _availableScrapers = MutableStateFlow<List<BaseScraper>>(emptyList())
    val availableScrapers: StateFlow<List<BaseScraper>> = _availableScrapers.asStateFlow()

    fun loadHome(profile: ProfileManager.Profile) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val profileKey = profile.name
            val scrapers = registry.getScrapersForProfile(profileKey)
            _availableScrapers.value = scrapers

            val selectedId = sourcePrefs.getSelectedSource(profileKey)
            _selectedSourceId.value = selectedId

            val activeScrapers = if (selectedId != null) {
                listOfNotNull(registry.getScraperById(selectedId))
            } else scrapers

            if (activeScrapers.isEmpty()) {
                _uiState.value = HomeUiState.Error("No scrapers available for this profile.")
                return@launch
            }

            try {
                // If "All Sources" (selectedId == null), show segregated sections
                val rows = mutableListOf<ContentRow>()
                
                if (selectedId == null) {
                    // Sections inspired by user images
                    val sectionTitles = listOf(
                        "Latest Movies", "Korean Series", "Netflix", 
                        "Amazon Prime Video", "JioHotstar", "Disney+", 
                        "Apple TV+", "Anime", "4K HDR", "Top IMDb", 
                        "Hindi Movies", "English Movies"
                    )
                    
                    val jobs = sectionTitles.map { title ->
                        async {
                            // Distribution: pick a scraper based on title or random
                            val scraper = activeScrapers.random()
                            try {
                                val items = scraper.getTrending(1)
                                if (items.isNotEmpty()) {
                                    ContentRow(
                                        title = title,
                                        items = items.shuffled().take(12).map { r ->
                                            SearchResult(
                                                id = r.id, title = r.title, posterUrl = r.posterUrl,
                                                year = null, type = ContentType.MOVIE, sourceId = r.sourceId,
                                                description = r.description
                                            )
                                        }
                                    )
                                } else null
                            } catch (_: Exception) { null }
                        }
                    }
                    rows.addAll(jobs.awaitAll().filterNotNull())
                } else {
                    // Show sections for the specific source
                    val items = activeScrapers.first().getTrending(1)
                    if (items.isNotEmpty()) {
                        rows.add(ContentRow(
                            title = "Trending on ${activeScrapers.first().info.displayName}",
                            items = items.map { r ->
                                SearchResult(
                                    id = r.id, title = r.title, posterUrl = r.posterUrl,
                                    year = null, type = ContentType.MOVIE, sourceId = r.sourceId,
                                    description = r.description
                                )
                            }
                        ))
                    }
                }

                val featured = rows.firstOrNull()?.items?.take(5) ?: emptyList()
                _uiState.value = HomeUiState.Success(featured, rows)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Failed to load content: ${e.message}")
            }
        }
    }

    fun selectSource(scraperId: String?, profile: ProfileManager.Profile) {
        sourcePrefs.setSelectedSource(profile.name, scraperId)
        _selectedSourceId.value = scraperId
        loadHome(profile)
    }
}
