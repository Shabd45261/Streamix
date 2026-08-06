package com.streamix.data.watchlist

import com.streamix.core.storage.WatchlistDao
import com.streamix.core.storage.WatchlistEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(
    private val dao: WatchlistDao,
    private val analyticsManager: com.streamix.core.analytics.AnalyticsManager
) {
    fun getAll() = dao.getAll()
    fun exists(id: String) = dao.exists(id)
    suspend fun insert(item: WatchlistEntity) {
        dao.insert(item)
        val bundle = android.os.Bundle().apply {
            putString("id", item.id)
            putString("title", item.title)
            putString("status", item.status)
        }
        analyticsManager.logEvent(com.streamix.core.analytics.AnalyticsEvent.WATCHLIST_ADDED, bundle)
    }
    suspend fun deleteById(id: String) {
        dao.deleteById(id)
        analyticsManager.logEvent(com.streamix.core.analytics.AnalyticsEvent.WATCHLIST_REMOVED, android.os.Bundle().apply { putString("id", id) })
    }
}
