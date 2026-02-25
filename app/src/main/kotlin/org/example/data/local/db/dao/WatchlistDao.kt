// data/local/db/dao/WatchlistDao.kt
package com.openanimelib.data.local.db.dao

import androidx.room.*
import com.openanimelib.data.local.db.entity.WatchlistEntity
import com.openanimelib.data.local.db.entity.AnimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Delete
    suspend fun removeFromWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE animeId = :animeId")
    suspend fun removeByAnimeId(animeId: Int)

    @Query("UPDATE watchlist SET currentEpisode = :episode, updatedAt = :time WHERE animeId = :animeId")
    suspend fun updateProgress(animeId: Int, episode: Int, time: Long = System.currentTimeMillis())

    @Query("UPDATE watchlist SET status = :status, updatedAt = :time WHERE animeId = :animeId")
    suspend fun updateStatus(animeId: Int, status: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE watchlist SET rating = :rating, updatedAt = :time WHERE animeId = :animeId")
    suspend fun updateRating(animeId: Int, rating: Float, time: Long = System.currentTimeMillis())

    @Query("SELECT * FROM watchlist WHERE animeId = :animeId")
    fun getWatchlistItem(animeId: Int): Flow<WatchlistEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE animeId = :animeId)")
    fun isInWatchlist(animeId: Int): Flow<Boolean>

    @Query("""
        SELECT w.*, a.* FROM watchlist w 
        INNER JOIN anime a ON w.animeId = a.id 
        WHERE w.status = :status 
        ORDER BY w.updatedAt DESC
    """)
    fun getWatchlistByStatus(status: String): Flow<List<WatchlistWithAnime>>

    @Query("""
        SELECT w.*, a.* FROM watchlist w 
        INNER JOIN anime a ON w.animeId = a.id 
        ORDER BY w.updatedAt DESC
    """)
    fun getAllWatchlist(): Flow<List<WatchlistWithAnime>>

    @Query("SELECT COUNT(*) FROM watchlist")
    fun getWatchlistCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM watchlist WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>
}

data class WatchlistWithAnime(
    @Embedded val watchlist: WatchlistEntity,
    @Embedded val anime: AnimeEntity
)