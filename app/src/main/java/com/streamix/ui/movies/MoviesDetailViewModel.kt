package com.streamix.ui.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.core.storage.WatchHistoryDao
import com.streamix.core.storage.WatchHistoryEntity
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import com.streamix.data.scraper.MoviesScraperRepository
import com.streamix.scraper.cloudstream.Episode
import com.streamix.scraper.cloudstream.LoadResponse
import com.streamix.scraper.cloudstream.TvType
import com.streamix.scraper.cloudstream.utils.AppUtils.toJson
import com.streamix.scraper.cloudstream.utils.AppUtils.tryParseJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoviesDetailViewModel @Inject constructor(
    private val repo: MoviesScraperRepository,
    private val watchlistDao: WatchlistDao,
    private val historyDao: WatchHistoryDao
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _videoTitle = MutableStateFlow("")
    val videoTitle = _videoTitle.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _videoLinks = MutableStateFlow<List<VideoLink>>(emptyList())
    val videoLinks = _videoLinks.asStateFlow()

    private val _posterUrl = MutableStateFlow("")
    val posterUrl = _posterUrl.asStateFlow()

    private val _relatedVideos = MutableStateFlow<List<SearchResult>>(emptyList())
    val relatedVideos = _relatedVideos.asStateFlow()

    private val _currentMovie = MutableStateFlow<LoadResponse?>(null)
    val currentMovie = _currentMovie.asStateFlow()

    private val _historyItem = MutableStateFlow<WatchHistoryEntity?>(null)
    val historyItem = _historyItem.asStateFlow()

    private val _trailerLinks = MutableStateFlow<List<VideoLink>>(emptyList())
    val trailerLinks = _trailerLinks.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes = _episodes.asStateFlow()

    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason = _selectedSeason.asStateFlow()

    private val _selectedEpisode = MutableStateFlow<Episode?>(null)
    val selectedEpisode = _selectedEpisode.asStateFlow()

    private val _seasons = MutableStateFlow<List<Int>>(emptyList())
    val seasons = _seasons.asStateFlow()

    private var _apiName: String = ""
    private var _fallbackUrl: String? = null

    fun load(movieId: String, apiName: String = "MovieBoxPh", fallbackUrl: String? = null) {
        _apiName = apiName
        _fallbackUrl = fallbackUrl
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch history
                _historyItem.value = historyDao.getHistoryItem(movieId)

                val detail = repo.getDetail(movieId, apiName)
                if (detail != null) {
                    _currentMovie.value = detail
                    _videoTitle.value = detail.name
                    _description.value = detail.plot ?: ""
                    _posterUrl.value = detail.posterUrl ?: ""
                    
                    var initialSeason: Int? = null
                    if (detail.type == TvType.TvSeries) {
                        val eps = tryParseJson<List<Episode>>(detail.dataUrl) ?: emptyList()
                        _episodes.value = eps
                        val uniqueSeasons = eps.mapNotNull { it.season }.distinct().sorted()
                        _seasons.value = uniqueSeasons
                        if (uniqueSeasons.isNotEmpty()) {
                            initialSeason = uniqueSeasons.first()
                            _selectedSeason.value = initialSeason
                            _selectedEpisode.value = eps.find { it.season == initialSeason }
                        }
                    }
                    
                    loadTrailer(detail, initialSeason)

                    // REMOVED: Auto-loading video links. 
                    // Links should only load when Play is clicked.
                    if (detail.type != TvType.TvSeries) {
                        val eps = tryParseJson<List<Episode>>(detail.dataUrl) ?: emptyList()
                        _selectedEpisode.value = eps.firstOrNull()
                    }
                } else {
                    Log.e("MoviesDetailVM", "Detail is null for $movieId from $apiName")
                }
                
                val detailRecs = detail?.recommendations?.map { it.toSearchResult() } ?: emptyList()
                val homeData = repo.getHomeData()
                val combined = (detailRecs + homeData.filter { it.id != movieId }).distinctBy { it.id }
                _relatedVideos.value = combined.shuffled().take(20)
                
            } catch (e: Exception) {
                Log.e("MoviesDetailVM", "Error loading movie: $movieId", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun com.streamix.scraper.cloudstream.SearchResponse.toSearchResult(): SearchResult {
        val q = this.quality ?: ""
        val isYear = q.matches(Regex("\\d{4}"))
        return SearchResult(
            id = this.url,
            title = this.name,
            posterPath = this.posterUrl,
            mediaType = "movie",
            rating = if (isYear) "" else q,
            studio = this.apiName,
            year = if (isYear) q else "",
            duration = if (q.contains(":")) q else ""
        )
    }

    fun selectSeason(season: Int) {
        _selectedSeason.value = season
        _selectedEpisode.value = _episodes.value.find { it.season == season }
        _currentMovie.value?.let { loadTrailer(it, season) }
    }

    fun selectEpisode(episode: Episode) {
        _selectedEpisode.value = episode
        loadLinks(episode.data, _apiName, episode.name ?: "Episode", _fallbackUrl)
    }

    fun clearLinks() {
        _videoLinks.value = emptyList()
    }

    private fun loadTrailer(detail: LoadResponse, season: Int? = null) {
        viewModelScope.launch {
            _trailerLinks.value = emptyList()
            
            // Try to find season specific trailer first if season is provided
            val query = if (season != null && detail.type == TvType.TvSeries) {
                "${detail.name} season $season official trailer"
            } else {
                "${detail.name} official trailer"
            }

            val trailer = detail.trailers.firstOrNull { 
                season == null || it.extractorUrl.contains("season $season", ignoreCase = true) 
            } ?: detail.trailers.firstOrNull()

            if (trailer != null && season == null) { // Only use pre-loaded if no specific season requested or we found a match
                val ytId = if (trailer.extractorUrl.contains("v=")) {
                    trailer.extractorUrl.substringAfter("v=").substringBefore("&")
                } else if (trailer.extractorUrl.contains("youtu.be/")) {
                    trailer.extractorUrl.substringAfter("youtu.be/").substringBefore("?")
                } else {
                    trailer.extractorUrl
                }
                
                try {
                    val ytScraper = com.streamix.scraper.youtube.YouTubeScraper()
                    val links = ytScraper.getVideoLinks(ytId, "youtube")
                    if (links.isNotEmpty()) {
                        _trailerLinks.value = links
                        return@launch
                    }
                } catch (e: Exception) {}
            }

            // Fallback or Season specific search
            try {
                val ytScraper = com.streamix.scraper.youtube.YouTubeScraper()
                val searchResults = ytScraper.search(query)
                val firstResult = searchResults.firstOrNull()
                if (firstResult != null) {
                    val links = ytScraper.getVideoLinks(firstResult.id, "youtube")
                    _trailerLinks.value = links
                }
            } catch (e: Exception) {
                Log.e("MoviesDetailVM", "Failed to search for trailer: $query", e)
            }
        }
    }

    fun loadLinks(data: String, apiName: String, title: String, fallbackUrl: String? = null) {
        viewModelScope.launch {
            _videoLinks.value = emptyList()
            val links = repo.getVideoLinks(data, apiName, title)
            _videoLinks.value = links
        }
    }

    fun saveToHistory(video: SearchResult) {
        viewModelScope.launch {
            historyDao.insertHistory(
                WatchHistoryEntity(
                    id = video.id,
                    title = video.title,
                    posterPath = video.posterPath,
                    mediaType = "movie",
                    lastWatchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleWatchlist(item: SearchResult) {
        viewModelScope.launch {
            if (watchlistDao.existsSync(item.id)) {
                watchlistDao.deleteById(item.id)
            } else {
                watchlistDao.insert(
                    WatchlistEntity(
                        id = item.id,
                        title = item.title,
                        posterPath = item.posterPath,
                        mediaType = "movie",
                        status = "Watchlist"
                    )
                )
            }
        }
    }
}
