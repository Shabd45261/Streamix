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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tmdbRepo: TmdbRepository,
    private val okxxx: OkxxxScraper,
    private val pornhat: PornhatScraper,
    private val youtube: YouTubeScraper,
    private val moviesRepo: MoviesScraperRepository,
    private val watchlistDao: WatchlistDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun search() {
        val q = _query.value
        if (q.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tmdbResults = async { runCatching { tmdbRepo.search(q) }.getOrDefault(emptyList()) }
                val okxxxResults = async { runCatching { okxxx.search(q) }.getOrDefault(emptyList()) }
                val pornhatResults = async { runCatching { pornhat.search(q) }.getOrDefault(emptyList()) }
                val youtubeResults = async { runCatching { youtube.search(q) }.getOrDefault(emptyList()) }
                val movieboxResults = async { runCatching { moviesRepo.search(q) }.getOrDefault(emptyList()) }
                
                val combined = awaitAll(tmdbResults, okxxxResults, pornhatResults, youtubeResults, movieboxResults)
                    .flatten()
                    .distinctBy { it.id }
                
                _results.value = combined
            } catch (e: Exception) {
                _results.value = emptyList()
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
