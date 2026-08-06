package com.streamix.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM watchlist ORDER BY timestamp DESC")
    fun getWatchlist(): Flow<List<MovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(movie: MovieEntity)

    @Delete
    suspend fun removeFromWatchlist(movie: MovieEntity)

    @Query("SELECT EXISTS(SELECT * FROM watchlist WHERE url = :url)")
    suspend fun isInWatchlist(url: String): Boolean
}
