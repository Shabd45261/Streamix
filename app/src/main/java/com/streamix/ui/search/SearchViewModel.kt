package com.streamix.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import com.streamix.data.tmdb.TmdbRepository
import com.streamix.scraper.adult.OkxxxScraper
import com.streamix.scraper.adult.PornhatScraper
import com.streamix.scraper.youtube.YouTubeScraper
import com.streamix.data.scraper.MoviesScraperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.streamix.core.analytics.AnalyticsEvent
import com.streamix.core.analytics.logSearch
import com.streamix.core.analytics.logError
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tmdbRepo: TmdbRepository,
    private val okxxx: OkxxxScraper,
    private val pornhat: PornhatScraper,
    private val youtube: YouTubeScraper,
    private val moviesRepo: MoviesScraperRepository,
    private val watchlistDao: WatchlistDao,
    private val prefs: com.streamix.core.storage.PreferencesManager,
    private val analyticsManager: com.streamix.core.analytics.AnalyticsManager,
    private val searchHistoryManager: com.streamix.core.storage.SearchHistoryManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val currentProfile: StateFlow<com.streamix.core.model.Profile> = prefs.currentProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.streamix.core.model.Profile.YOUTUBE)

    val searchHistory: StateFlow<List<String>> = currentProfile.flatMapLatest { profile ->
        searchHistoryManager.getHistory(profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun removeHistoryItem(q: String) {
        viewModelScope.launch { 
            searchHistoryManager.removeSearch(currentProfile.value, q)
        }
    }

    fun search() {
        val q = _query.value
        if (q.isBlank()) return
        
        viewModelScope.launch {
            val profile = currentProfile.value
            searchHistoryManager.addSearch(profile, q)

            _isLoading.value = true
            analyticsManager.logEvent(AnalyticsEvent.SEARCH_STARTED)
            try {
                val results = if (q.startsWith("@")) {
                    val actualQuery = q.removePrefix("@").trim()
                    val okxxxResults = async { runCatching { okxxx.search(actualQuery) }.getOrDefault(emptyList()) }
                    val pornhatResults = async { runCatching { pornhat.search(actualQuery) }.getOrDefault(emptyList()) }
                    (okxxxResults.await() + pornhatResults.await()).filter { it.mediaType == "adult" }.distinctBy { it.id }
                } else {
                    val youtubeResults = async { runCatching { youtube.search(q) }.getOrDefault(emptyList()) }
                    val tmdbResults = async { runCatching { tmdbRepo.search(q) }.getOrDefault(emptyList()) }
                    val movieboxResults = async { runCatching { moviesRepo.search(q) }.getOrDefault(emptyList()) }
                    
                    val combined = (youtubeResults.await() + tmdbResults.await() + movieboxResults.await())
                    combined.filter { it.mediaType != "adult" }.distinctBy { it.id }
                }
                
                _results.value = results
                analyticsManager.logSearch(q, results.size)
            } catch (e: Exception) {
                _results.value = emptyList()
                analyticsManager.logError(AnalyticsEvent.SEARCH_FAILED, e.message ?: "Unknown error")
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
