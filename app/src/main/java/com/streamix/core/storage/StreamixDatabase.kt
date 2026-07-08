package com.streamix.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchlistEntity::class, WatchHistoryEntity::class], version = 5, exportSchema = false)
abstract class StreamixDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun watchHistoryDao(): WatchHistoryDao
}
