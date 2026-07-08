package com.streamix.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterPath: String?,
    val mediaType: String,
    val duration: String = "",
    val views: String = "",
    val rating: String = "",
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val progress: Long = 0,
    val totalDuration: Long = 0,
    val isShort: Boolean = false,
    val studio: String = "",
    val year: String = "",
    
    // For Series
    val lastEpisodeData: String? = null,
    val lastEpisodeName: String? = null,
    val lastEpisodeSeason: Int? = null,
    val lastEpisodeNumber: Int? = null
)
