package com.streamix.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.plugin.ContentType
import com.streamix.core.plugin.SearchResult
import com.streamix.data.ContinueWatchingEntity
import com.streamix.data.LibraryDao
import com.streamix.data.WatchlistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContinueWatchingEntry(val item: SearchResult, val progressPct: Float, val positionMs: Long)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryDao: LibraryDao
) : ViewModel() {

    val watchlist: StateFlow<List<SearchResult>> = libraryDao.getWatchlist()
        .map { list ->
            list.map { entity ->
                SearchResult(
                    id = entity.id,
                    title = entity.title,
                    posterUrl = entity.posterUrl,
                    type = entity.type,
                    sourceId = entity.sourceId,
                    year = null
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching: StateFlow<List<ContinueWatchingEntry>> = libraryDao.getContinueWatching()
        .map { list ->
            list.map { entity ->
                ContinueWatchingEntry(
                    item = SearchResult(
                        id = entity.id,
                        title = entity.title,
                        posterUrl = entity.posterUrl,
                        type = entity.type,
                        sourceId = entity.sourceId,
                        year = null
                    ),
                    progressPct = entity.progressPct,
                    positionMs = entity.positionMs
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToWatchlist(item: SearchResult) = viewModelScope.launch {
        libraryDao.addToWatchlist(
            WatchlistEntity(
                id = item.id,
                title = item.title,
                posterUrl = item.posterUrl,
                type = item.type,
                sourceId = item.sourceId
            )
        )
    }

    fun removeFromWatchlist(itemId: String) = viewModelScope.launch {
        libraryDao.removeFromWatchlist(itemId)
    }

    fun updateProgress(item: SearchResult, positionMs: Long, durationMs: Long) = viewModelScope.launch {
        val pct = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
        libraryDao.updateProgress(
            ContinueWatchingEntity(
                id = item.id,
                title = item.title,
                posterUrl = item.posterUrl,
                type = item.type,
                sourceId = item.sourceId,
                progressPct = pct,
                positionMs = positionMs,
                durationMs = durationMs
            )
        )
    }
}
