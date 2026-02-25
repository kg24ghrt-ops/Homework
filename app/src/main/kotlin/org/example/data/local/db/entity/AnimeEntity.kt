// data/local/db/entity/AnimeEntity.kt
package com.openanimelib.data.local.db.entity

import androidx.room.*

@Entity(tableName = "anime")
data class AnimeEntity(
    @PrimaryKey val id: Int,
    val anilistId: Int,
    val malId: Int?,
    val titleEnglish: String?,
    val titleRomaji: String?,
    val titleJapanese: String?,
    val synopsis: String?,
    val coverImage: String,
    val bannerImage: String?,
    val rating: Float?,
    val episodes: Int?,
    val type: String,
    val status: String,
    val genres: String,       // JSON string of List<String>
    val tags: String?,        // JSON string of List<String>
    val year: Int?,
    val season: String?,
    val studio: String?,
    val popularity: Int = 0,
    val duration: Int?,
    val source: String?,
    val trailer: String?,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "streaming_sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val animeId: Int,
    val platform: String,
    val url: String,
    val hasAds: Boolean,
    val quality: String,
    val isDub: Boolean,
    val isSub: Boolean,
    val regionRestrictions: String,  // JSON string
    val availableEpisodes: String,   // JSON string
    val lastChecked: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val animeId: Int,
    val status: String,              // WatchStatus enum name
    val currentEpisode: Int = 0,
    val rating: Float? = null,
    val notes: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val animeId: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "episode_progress")
data class EpisodeProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val animeId: Int,
    val episodeNumber: Int,
    val watched: Boolean = false,
    val watchedAt: Long? = null
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)