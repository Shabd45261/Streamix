package com.streamix.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.Profile
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LibraryViewModel @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val historyDao: com.streamix.core.storage.WatchHistoryDao
) : ViewModel() {

    private val _status = MutableStateFlow("Watching")
    private val _currentProfile = MutableStateFlow(Profile.YOUTUBE)
    private val _searchQuery = MutableStateFlow("")
    private val _historyFilter = MutableStateFlow("All") // All, Videos, Shorts, Music
    
    val searchQuery = _searchQuery.asStateFlow()
    val historyFilter = _historyFilter.asStateFlow()

    val items: StateFlow<List<WatchlistEntity>> = combine(_status, _currentProfile, _searchQuery) { status, profile, query ->
        Triple(status, profile, query)
    }.flatMapLatest { (status, profile, query) -> 
        if (status == "Watching") {
            historyDao.getAllHistory().map { historyList ->
                historyList.filter { item ->
                    val matchesProfile = when (profile) {
                        Profile.YOUTUBE -> item.mediaType == "youtube"
                        Profile.ADULT   -> item.mediaType == "adult"
                        Profile.MOVIES  -> item.mediaType == "movie" || item.mediaType == "tv"
                        Profile.SONGS   -> item.mediaType == "song"
                    }
                    val matchesQuery = query.isBlank() || item.title.contains(query, ignoreCase = true)
                    matchesProfile && matchesQuery
                }.map { it.toWatchlistEntity() }
            }
        } else {
            watchlistDao.getByStatus(status).map { list ->
                list.filter { item ->
                    val matchesProfile = when (profile) {
                        Profile.YOUTUBE -> item.mediaType == "youtube"
                        Profile.ADULT   -> item.mediaType == "adult"
                        Profile.MOVIES  -> item.mediaType == "movie" || item.mediaType == "tv"
                        Profile.SONGS   -> item.mediaType == "song"
                    }
                    val matchesQuery = query.isBlank() || item.title.contains(query, ignoreCase = true)
                    matchesProfile && matchesQuery
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedHistory: StateFlow<Map<String, List<com.streamix.core.storage.WatchHistoryEntity>>> = 
        combine(_currentProfile, _searchQuery, _historyFilter) { profile, query, filter ->
            Triple(profile, query, filter)
        }.flatMapLatest { (profile, query, filter) ->
            historyDao.getAllHistory().map { historyList ->
                historyList.filter { item ->
                    val matchesProfile = when (profile) {
                        Profile.YOUTUBE -> item.mediaType == "youtube"
                        Profile.ADULT   -> item.mediaType == "adult"
                        Profile.MOVIES  -> item.mediaType == "movie" || item.mediaType == "tv"
                        Profile.SONGS   -> item.mediaType == "song"
                    }
                    val matchesQuery = query.isBlank() || item.title.contains(query, ignoreCase = true)
                    val matchesFilter = when (filter) {
                        "Videos" -> !item.isShort
                        "Shorts" -> item.isShort
                        "Music"  -> item.mediaType == "song"
                        else -> true
                    }
                    matchesProfile && matchesQuery && matchesFilter
                }.groupBy { item ->
                    formatDate(item.lastWatchedAt)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun formatDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return when {
            isSameDay(now, date) -> "Today"
            isYesterday(now, date) -> "Yesterday"
            else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(date.time)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, date: Calendar): Boolean {
        val yesterday = now.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(yesterday, date)
    }

    private fun com.streamix.core.storage.WatchHistoryEntity.toWatchlistEntity() = WatchlistEntity(
        id = id,
        title = title,
        posterPath = posterPath,
        mediaType = mediaType,
        status = "Watching",
        isShort = isShort,
        studio = studio,
        views = views,
        year = year
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun loadStatus(status: String) {
        _status.value = status
    }

    fun setProfile(profile: Profile) {
        _currentProfile.value = profile
    }
    
    fun removeFromHistory(id: String) {
        viewModelScope.launch {
            historyDao.deleteHistory(id)
        }
    }
    
    fun addToLibrary(item: WatchlistEntity) {
        viewModelScope.launch {
            watchlistDao.insert(item)
        }
    }
}
