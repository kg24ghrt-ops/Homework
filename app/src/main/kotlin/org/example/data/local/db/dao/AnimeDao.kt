// data/local/db/dao/AnimeDao.kt
package com.openanimelib.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.openanimelib.data.local.db.entity.AnimeEntity
import com.openanimelib.data.local.db.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnime(anime: AnimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAnime(anime: List<AnimeEntity>)

    @Query("SELECT * FROM anime WHERE id = :id")
    suspend fun getAnimeById(id: Int): AnimeEntity?

    @Query("SELECT * FROM anime WHERE id = :id")
    fun observeAnimeById(id: Int): Flow<AnimeEntity?>

    @Query("""
        SELECT * FROM anime 
        ORDER BY popularity DESC 
    """)
    fun getTrendingAnimePaged(): PagingSource<Int, AnimeEntity>

    @Query("""
        SELECT * FROM anime 
        WHERE season = :season AND year = :year 
        ORDER BY popularity DESC
    """)
    fun getSeasonalAnime(season: String, year: Int): Flow<List<AnimeEntity>>

    @Query("""
        SELECT * FROM anime 
        WHERE genres LIKE '%' || :genre || '%' 
        ORDER BY rating DESC
    """)
    fun getAnimeByGenre(genre: String): PagingSource<Int, AnimeEntity>

    @Query("""
        SELECT * FROM anime 
        WHERE titleEnglish LIKE '%' || :query || '%' 
           OR titleRomaji LIKE '%' || :query || '%' 
           OR titleJapanese LIKE '%' || :query || '%'
           OR genres LIKE '%' || :query || '%'
        ORDER BY popularity DESC
    """)
    fun searchAnime(query: String): PagingSource<Int, AnimeEntity>

    @Query("SELECT * FROM anime ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomAnime(): AnimeEntity?

    @Query("SELECT * FROM anime WHERE status = 'AIRING' ORDER BY popularity DESC")
    fun getCurrentlyAiring(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentlyUpdated(limit: Int = 20): Flow<List<AnimeEntity>>

    @Query("DELETE FROM anime WHERE updatedAt < :threshold")
    suspend fun deleteStaleAnime(threshold: Long)

    @Query("SELECT COUNT(*) FROM anime")
    suspend fun getAnimeCount(): Int
}

@Dao
interface SourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<SourceEntity>)

    @Query("SELECT * FROM streaming_sources WHERE animeId = :animeId")
    fun getSourcesForAnime(animeId: Int): Flow<List<SourceEntity>>

    @Query("DELETE FROM streaming_sources WHERE animeId = :animeId")
    suspend fun deleteSourcesForAnime(animeId: Int)

    @Query("""
        SELECT * FROM streaming_sources 
        WHERE animeId = :animeId 
        AND (regionRestrictions = '[]' OR regionRestrictions LIKE '%' || :region || '%')
    """)
    fun getSourcesForRegion(animeId: Int, region: String): Flow<List<SourceEntity>>
}