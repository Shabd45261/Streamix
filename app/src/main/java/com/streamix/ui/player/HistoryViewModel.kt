package com.streamix.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.storage.WatchHistoryDao
import com.streamix.core.storage.WatchHistoryEntity
import com.streamix.core.model.SearchResult
import com.streamix.core.model.Profile
import com.streamix.core.storage.PreferencesManager
import com.streamix.scraper.cloudstream.Episode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.map

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: WatchHistoryDao,
    private val watchlistDao: com.streamix.core.storage.WatchlistDao,
    private val prefs: PreferencesManager
) : ViewModel() {

    fun addToWatchlist(video: SearchResult, status: String) {
        viewModelScope.launch {
            watchlistDao.insert(
                com.streamix.core.storage.WatchlistEntity(
                    id = video.id,
                    title = video.title,
                    posterPath = video.posterPath,
                    mediaType = video.mediaType,
                    status = status,
                    isShort = video.isShort
                )
            )
        }
    }

    fun isLiked(id: String): kotlinx.coroutines.flow.Flow<Boolean> {
        return watchlistDao.getAll().map { list ->
            list.any { it.id == id && (it.status == "Liked" || it.status == "Favorites") }
        }
    }

    fun updateProgress(
        video: SearchResult,
        position: Long,
        duration: Long,
        lastEpisode: Episode? = null
    ) {
        viewModelScope.launch {
            // Requirement: Shorts history only after 5 seconds for YouTube/Adult profiles
            if (video.isShort) {
                val profile = prefs.currentProfile.first()
                if ((profile == Profile.YOUTUBE || profile == Profile.ADULT) && position < 5000) {
                    return@launch
                }
            }

            val entity = WatchHistoryEntity(
                id = video.id,
                title = video.title,
                posterPath = video.posterPath,
                mediaType = video.mediaType,
                lastWatchedAt = System.currentTimeMillis(),
                progress = position,
                totalDuration = duration,
                isShort = video.isShort,
                studio = video.studio,
                year = video.year,
                lastEpisodeData = lastEpisode?.data,
                lastEpisodeName = lastEpisode?.name,
                lastEpisodeSeason = lastEpisode?.season,
                lastEpisodeNumber = lastEpisode?.episode
            )
            historyDao.insertHistory(entity)
        }
    }
}
