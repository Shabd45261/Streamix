package com.streamix.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.streamix.core.plugin.ContentType

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String?,
    val type: ContentType,
    val sourceId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "continue_watching")
data class ContinueWatchingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String?,
    val type: ContentType,
    val sourceId: String,
    val progressPct: Float,
    val positionMs: Long,
    val durationMs: Long,
    val lastWatchedAt: Long = System.currentTimeMillis()
)
