package com.streamix.ui.adult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.VideoLink
import com.streamix.core.model.SearchResult
import com.streamix.core.storage.WatchHistoryDao
import com.streamix.core.storage.WatchHistoryEntity
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import com.streamix.data.scraper.AdultScraperRepository
import com.streamix.scraper.adult.AdultVideoDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdultDetailViewModel @Inject constructor(
    private val adultRepo: AdultScraperRepository,
    private val historyDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao
) : ViewModel() {

    private val _detail     = MutableStateFlow<AdultVideoDetail?>(null)
    val detail = _detail.asStateFlow()

    private val _videoLinks = MutableStateFlow<List<VideoLink>>(emptyList())
    val videoLinks = _videoLinks.asStateFlow()

    private val _isLoading  = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _trending = MutableStateFlow<List<SearchResult>>(emptyList())
    val trending = _trending.asStateFlow()

    private var currentPageUrl = ""

    private val _initialPosition = MutableStateFlow(0L)
    val initialPosition = _initialPosition.asStateFlow()

    fun updateProgress(current: Long, total: Long) {
        viewModelScope.launch {
            _detail.value?.let { detailResult ->
                // Insert into watch history
                historyDao.insertHistory(
                    WatchHistoryEntity(
                        id = currentPageUrl,
                        title = detailResult.title,
                        posterPath = detailResult.posterUrl,
                        mediaType = "adult",
                        duration = detailResult.views,
                        views = detailResult.views,
                        rating = detailResult.rating,
                        progress = current,
                        totalDuration = total
                    )
                )

                // Automatic Library Management
                if (total > 0) {
                    val progressPercent = current.toDouble() / total.toDouble()
                    val status = if (progressPercent > 0.9) "Completed" else "Watching"
                    
                    watchlistDao.insert(
                        WatchlistEntity(
                            id = currentPageUrl,
                            title = detailResult.title,
                            posterPath = detailResult.posterUrl,
                            mediaType = "adult",
                            year = detailResult.date ?: "2024",
                            views = detailResult.views,
                            rating = detailResult.rating,
                            status = status
                        )
                    )
                }
            }
        }
    }

    fun load(pageUrl: String) {
        if (pageUrl == currentPageUrl && _detail.value != null) return
        
        currentPageUrl = pageUrl
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val detailAsync = async { try { adultRepo.getDetail(pageUrl) } catch(e: Exception) { null } }
                val linksAsync = async { try { adultRepo.getVideoLinks(pageUrl) } catch(e: Exception) { emptyList() } }
                val trendingAsync = async { try { adultRepo.getTrending(1) } catch(e: Exception) { emptyList() } }
                
                val detailResult = detailAsync.await()
                if (detailResult != null) {
                    _detail.value = detailResult
                    
                    val existing = historyDao.getHistoryItem(pageUrl)
                    _initialPosition.value = existing?.progress ?: 0L

                    // Save to history
                    historyDao.insertHistory(
                        WatchHistoryEntity(
                            id = pageUrl,
                            title = detailResult.title,
                            posterPath = detailResult.posterUrl,
                            mediaType = "adult",
                            duration = detailResult.views, 
                            views = detailResult.views,
                            rating = detailResult.rating,
                            progress = existing?.progress ?: 0L,
                            totalDuration = existing?.totalDuration ?: 0L
                        )
                    )
                }
                
                _videoLinks.value = linksAsync.await()
                
                val trendingResult = trendingAsync.await()
                if (trendingResult.isNotEmpty()) {
                    _trending.value = trendingResult.take(20)
                } else {
                    // Fallback to searching for a part of the title
                    val query = detailResult?.title?.split(" ")?.take(2)?.joinToString(" ") ?: ""
                    if (query.isNotEmpty()) {
                        _trending.value = adultRepo.search(query).take(20)
                    }
                }
            } catch (e: Exception) {
                // handle
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadLinks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _videoLinks.value = adultRepo.getVideoLinks(currentPageUrl)
            } catch (e: Exception) {
                // error
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
