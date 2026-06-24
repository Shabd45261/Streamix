package com.streamix.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterPath: String?,
    val mediaType: String,
    val year: String = "",
    val views: String = "",
    val rating: String = "",
    val status: String = "Plan to Watch", // Watching, Completed, On-Hold, Dropped, Plan to Watch, Favorites, Subscribed
    val addedAt: Long = System.currentTimeMillis()
)
