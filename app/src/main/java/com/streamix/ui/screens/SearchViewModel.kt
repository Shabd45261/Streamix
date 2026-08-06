package com.streamix.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.PreferenceManager
import com.streamix.core.ProfileManager
import com.streamix.core.plugin.ContentType
import com.streamix.core.plugin.SearchResult
import com.streamix.scrapers.ScraperRegistry
import com.streamix.scrapers.SourcePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Results(val results: List<SearchResult>, val hasMore: Boolean, val page: Int) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val profileManager: ProfileManager,
    private val registry: ScraperRegistry,
    private val sourcePrefs: SourcePreferences,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches = _recentSearches.asStateFlow()

    private var searchJob: Job? = null

    init {
        _recentSearches.value = preferenceManager.getRecentSearches()
    }

    fun onQueryChanged(q: String) {
        _query.value = q
        if (q.isBlank()) { _uiState.value = SearchUiState.Idle; return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(q, page = 1)
        }
    }

    fun searchNow() {
        searchJob?.cancel()
        val q = _query.value
        if (q.isNotBlank()) {
            saveRecentSearch(q)
            viewModelScope.launch { performSearch(q, page = 1) }
        }
    }

    private fun saveRecentSearch(q: String) {
        val current = _recentSearches.value.toMutableList()
        if (current.contains(q)) current.remove(q)
        current.add(0, q)
        val limit = current.take(10)
        _recentSearches.value = limit
        preferenceManager.saveRecentSearches(limit)
    }

    fun removeRecentSearch(q: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(q)
        _recentSearches.value = current
        preferenceManager.saveRecentSearches(current)
    }

    fun loadNextPage() {
        val current = _uiState.value as? SearchUiState.Results ?: return
        if (current.hasMore) {
            viewModelScope.launch { performSearch(_query.value, page = current.page + 1, append = true) }
        }
    }

    private suspend fun performSearch(q: String, page: Int, append: Boolean = false) {
        if (q.isBlank()) return
        if (!append) _uiState.value = SearchUiState.Loading

        val profile = profileManager.currentProfile.value
        val selectedId = sourcePrefs.getSelectedSource(profile.name)

        val scrapers = when (profile) {
            ProfileManager.Profile.ADULT  -> if (selectedId != null)
                listOfNotNull(registry.getScraperById(selectedId)) else registry.getAdultScrapers()
            ProfileManager.Profile.MOVIES -> if (selectedId != null)
                listOfNotNull(registry.getScraperById(selectedId)) else registry.getMovieScrapers()
            ProfileManager.Profile.SONGS  -> if (selectedId != null)
                listOfNotNull(registry.getScraperById(selectedId)) else registry.getMusicScrapers()
        }

        try {
            val all = mutableListOf<SearchResult>()
            scrapers.forEach { scraper ->
                try {
                    val results = scraper.search(q, page)
                    results.forEach { r ->
                        all.add(SearchResult(
                            id = r.id, title = r.title, posterUrl = r.posterUrl,
                            year = null, type = ContentType.MOVIE,
                            sourceId = r.sourceId,
                            description = r.description
                        ))
                    }
                } catch (_: Exception) {}
            }

            val existing = if (append) (_uiState.value as? SearchUiState.Results)?.results ?: emptyList()
                           else emptyList()
            _uiState.value = SearchUiState.Results(existing + all, all.isNotEmpty(), page)
        } catch (e: Exception) {
            _uiState.value = SearchUiState.Error("Search failed: ${e.message}")
        }
    }
}
