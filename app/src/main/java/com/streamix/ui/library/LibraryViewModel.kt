package com.streamix.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val watchlistDao: WatchlistDao
) : ViewModel() {

    private val _status = MutableStateFlow("Watching")
    
    val items: StateFlow<List<WatchlistEntity>> = _status
        .flatMapLatest { status -> watchlistDao.getByStatus(status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadStatus(status: String) {
        _status.value = status
    }
    
    fun addToLibrary(item: WatchlistEntity) {
        viewModelScope.launch {
            watchlistDao.insert(item)
        }
    }
}
