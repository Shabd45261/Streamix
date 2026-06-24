package com.streamix.data.watchlist

import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(
    private val dao: WatchlistDao
) {
    fun getAll() = dao.getAll()
    fun exists(id: String) = dao.exists(id)
    suspend fun insert(item: WatchlistEntity) = dao.insert(item)
    suspend fun deleteById(id: String) = dao.deleteById(id)
}
