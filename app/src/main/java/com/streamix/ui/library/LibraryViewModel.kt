package com.streamix.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.Profile
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val historyDao: com.streamix.core.storage.WatchHistoryDao
) : ViewModel() {

    private val _status = MutableStateFlow("Watching")
    private val _currentProfile = MutableStateFlow(Profile.YOUTUBE)
    
    val items: StateFlow<List<WatchlistEntity>> = combine(_status, _currentProfile) { status, profile ->
        status to profile
    }.flatMapLatest { (status, profile) -> 
        if (status == "Watching") {
            historyDao.getAllHistory().map { historyList ->
                historyList.filter { item ->
                    when (profile) {
                        Profile.YOUTUBE -> item.mediaType == "youtube"
                        Profile.ADULT   -> item.mediaType == "adult"
                        Profile.MOVIES  -> item.mediaType == "movie" || item.mediaType == "tv"
                        Profile.SONGS   -> item.mediaType == "song"
                    }
                }.map { it.toWatchlistEntity() }
            }
        } else {
            watchlistDao.getByStatus(status).map { list ->
                list.filter { item ->
                    when (profile) {
                        Profile.YOUTUBE -> item.mediaType == "youtube"
                        Profile.ADULT   -> item.mediaType == "adult"
                        Profile.MOVIES  -> item.mediaType == "movie" || item.mediaType == "tv"
                        Profile.SONGS   -> item.mediaType == "song"
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun com.streamix.core.storage.WatchHistoryEntity.toWatchlistEntity() = WatchlistEntity(
        id = id,
        title = title,
        posterPath = posterPath,
        mediaType = mediaType,
        status = "Watching",
        year = ""
    )

    fun loadStatus(status: String) {
        _status.value = status
    }

    fun setProfile(profile: Profile) {
        _currentProfile.value = profile
    }
    
    fun addToLibrary(item: WatchlistEntity) {
        viewModelScope.launch {
            watchlistDao.insert(item)
        }
    }
}
