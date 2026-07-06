package com.streamix.ui.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import com.streamix.scraper.youtube.YouTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class YoutubeDetailViewModel @Inject constructor(
    private val youtubeScraper: YouTubeScraper,
    private val watchlistDao: WatchlistDao,
    private val historyDao: com.streamix.core.storage.WatchHistoryDao,
    private val prefs: com.streamix.core.storage.PreferencesManager
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _videoTitle = MutableStateFlow("")
    val videoTitle = _videoTitle.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _videoLinks = MutableStateFlow<List<VideoLink>>(emptyList())
    val videoLinks = _videoLinks.asStateFlow()
    
    private val _relatedVideos = MutableStateFlow<List<SearchResult>>(emptyList())
    val relatedVideos = _relatedVideos.asStateFlow()

    private val _currentVideo = MutableStateFlow<SearchResult?>(null)
    private val _channelUrl = MutableStateFlow("")
    private val _channelName = MutableStateFlow("")
    private val _channelAvatar = MutableStateFlow("")

    val isSubscribed = kotlinx.coroutines.flow.combine(watchlistDao.getAll(), _channelUrl) { list, channelUrl ->
        list.any { it.id == channelUrl && it.status == "Subscribed" }
    }

    fun load(videoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val url = if (videoId.startsWith("http")) videoId else "https://www.youtube.com/watch?v=$videoId"
                
                val streamInfo = withContext(Dispatchers.IO) {
                    org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
                        org.schabi.newpipe.extractor.ServiceList.YouTube, url
                    )
                }
                
                _videoTitle.value = streamInfo.name
                _description.value = streamInfo.description?.content ?: ""
                _channelUrl.value = streamInfo.uploaderUrl ?: ""
                _channelName.value = streamInfo.uploaderName ?: ""
                _channelAvatar.value = streamInfo.uploaderAvatars?.lastOrNull()?.getUrl() ?: ""
                
                val item = SearchResult(
                    id = videoId,
                    title = streamInfo.name,
                    posterPath = streamInfo.thumbnails?.lastOrNull()?.getUrl() ?: streamInfo.thumbnails?.firstOrNull()?.getUrl(),
                    mediaType = "youtube",
                    duration = formatDuration(streamInfo.duration),
                    views = formatViews(streamInfo.viewCount),
                    studio = streamInfo.uploaderName
                )
                _currentVideo.value = item

                // Save to history
                saveToHistory(item)

                // Optimization: Use already loaded streamInfo to get links
                val links = youtubeScraper.getVideoLinksFromInfo(streamInfo)
                _videoLinks.value = links
                
                // Use related items from streamInfo
                val related = streamInfo.relatedItems?.filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()?.map {
                    youtubeScraper.run { it.toSearchResult() }
                } ?: emptyList()
                
                _relatedVideos.value = related

                // Save topics to interests for personalization
                viewModelScope.launch {
                    streamInfo.uploaderName?.let { prefs.addInterest(it) }
                    // Also try to extract keywords from title/description
                    val keywords = listOf("literature", "beauty", "poem", "art", "music", "movie", "webseries")
                    keywords.forEach { keyword ->
                        if (streamInfo.name.lowercase().contains(keyword) || (streamInfo.description?.content?.lowercase()?.contains(keyword) == true)) {
                            prefs.addInterest(keyword)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("YoutubeDetailViewModel", "Error loading video: $videoId", e)
                _videoTitle.value = "Error: ${e.message ?: "Could not load video"}"
                
                // Fallback: try to get links anyway if possible
                try {
                    val links = youtubeScraper.getVideoLinks(videoId, "youtube")
                    if (links.isNotEmpty()) {
                        _videoLinks.value = links
                        _videoTitle.value = "Video Loaded (Limited Info)"
                    }
                } catch (inner: Exception) {}
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveToHistory(video: SearchResult) {
        viewModelScope.launch {
            historyDao.insertHistory(
                com.streamix.core.storage.WatchHistoryEntity(
                    id = video.id,
                    title = video.title,
                    posterPath = video.posterPath,
                    mediaType = "youtube",
                    duration = video.duration,
                    views = video.views,
                    lastWatchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun formatViews(views: Long): String {
        return when {
            views >= 1_000_000_000 -> "%.1fB".format(views / 1_000_000_000.0)
            views >= 1_000_000 -> "%.1fM".format(views / 1_000_000.0)
            views >= 1_000 -> "%.1fK".format(views / 1_000.0)
            else -> views.toString()
        }
    }

    fun toggleSubscription() {
        val channelUrl = _channelUrl.value.ifBlank { return }
        val channelName = _channelName.value
        viewModelScope.launch {
            if (watchlistDao.existsSync(channelUrl)) {
                watchlistDao.deleteById(channelUrl)
            } else {
                watchlistDao.insert(
                    WatchlistEntity(
                        id = channelUrl,
                        title = channelName,
                        posterPath = _channelAvatar.value,
                        mediaType = "youtube",
                        status = "Subscribed"
                    )
                )
            }
        }
    }
}
