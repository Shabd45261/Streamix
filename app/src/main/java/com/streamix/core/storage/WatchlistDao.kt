package com.streamix.core.storage

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE status = :status ORDER BY addedAt DESC")
    fun getByStatus(status: String): Flow<List<WatchlistEntity>>

    @Query("UPDATE watchlist SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Delete
    suspend fun delete(item: WatchlistEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id LIMIT 1)")
    fun exists(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id LIMIT 1)")
    suspend fun existsSync(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id AND status = 'Liked')")
    fun isLiked(id: String): Flow<Boolean>

    @Query("DELETE FROM watchlist WHERE id = :id AND status = 'Liked'")
    suspend fun removeLike(id: String)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteById(id: String)
}
