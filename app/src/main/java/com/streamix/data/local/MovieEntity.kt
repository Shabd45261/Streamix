package com.streamix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class MovieEntity(
    @PrimaryKey val url: String,
    val name: String,
    val posterUrl: String?,
    val type: String?,
    val timestamp: Long = System.currentTimeMillis()
)
