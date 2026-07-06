package com.streamix.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.VideoLink
import com.streamix.core.network.TmdbMovieDetail
import com.streamix.core.network.TmdbTvDetail
import com.streamix.core.storage.WatchlistEntity
import com.streamix.data.scraper.ScraperRepository
import com.streamix.data.tmdb.TmdbRepository
import com.streamix.data.watchlist.WatchlistRepository
import com.streamix.ui.player.VlcPlayerLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val tmdbRepo: TmdbRepository,
    private val scraperRepo: ScraperRepository,
    private val watchlistRepo: WatchlistRepository
) : ViewModel() {

    private val _movieDetail = MutableStateFlow<TmdbMovieDetail?>(null)
    val movieDetail = _movieDetail.asStateFlow()

    private val _tvDetail = MutableStateFlow<TmdbTvDetail?>(null)
    val tvDetail = _tvDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _videoLinks = MutableStateFlow<List<VideoLink>>(emptyList())
    val videoLinks = _videoLinks.asStateFlow()

    private val _selectedServer = MutableStateFlow(0)
    val selectedServer = _selectedServer.asStateFlow()

    fun isWatchlisted(id: String): StateFlow<Boolean> = 
        watchlistRepo.exists(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun load(id: String, type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            if (type == "movie") {
                _movieDetail.value = tmdbRepo.getMovieDetail(id)
            } else {
                _tvDetail.value = tmdbRepo.getTvDetail(id)
            }
            _isLoading.value = false
        }
    }

    fun playVideo(context: Context, id: String, type: String, title: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = scraperRepo.getVideoLinks(id, type, title)
            result.onSuccess { links ->
                _videoLinks.value = links
                links.firstOrNull()?.let {
                    VlcPlayerLauncher.launch(context, it.url, title)
                }
            }
            _isLoading.value = false
        }
    }

    fun switchServer(context: Context, id: String, type: String, title: String, index: Int) {
        _selectedServer.value = index
        viewModelScope.launch {
            _isLoading.value = true
            val result = scraperRepo.getVideoLinksFromServer(id, type, index) // Note: getServer links doesn't search by title yet, could be added
            result.onSuccess { links ->
                _videoLinks.value = links
                links.firstOrNull()?.let {
                    VlcPlayerLauncher.launch(context, it.url, title)
                }
            }
            _isLoading.value = false
        }
    }
    
    fun toggleLibrary(id: String, type: String) {
        viewModelScope.launch {
            val movie = _movieDetail.value
            val tv = _tvDetail.value
            
            val exists = watchlistRepo.exists(id).first()
            if (exists) {
                watchlistRepo.deleteById(id)
            } else {
                val entity = if (type == "movie" && movie != null) {
                    WatchlistEntity(id, movie.title, movie.poster_path, "movie", movie.release_date.take(4))
                } else if (type == "tv" && tv != null) {
                    WatchlistEntity(id, tv.name, tv.poster_path, "tv", tv.first_air_date.take(4))
                } else null
                
                entity?.let { watchlistRepo.insert(it) }
            }
        }
    }
}
