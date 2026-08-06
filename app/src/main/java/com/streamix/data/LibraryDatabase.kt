package com.streamix.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchlistEntity::class, ContinueWatchingEntity::class], version = 1, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
