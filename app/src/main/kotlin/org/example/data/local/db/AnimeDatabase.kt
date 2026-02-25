// data/local/db/AnimeDatabase.kt
package com.openanimelib.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.openanimelib.data.local.db.dao.*
import com.openanimelib.data.local.db.entity.*

@Database(
    entities = [
        AnimeEntity::class,
        SourceEntity::class,
        WatchlistEntity::class,
        FavoriteEntity::class,
        EpisodeProgressEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AnimeDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
    abstract fun sourceDao(): SourceDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun episodeProgressDao(): EpisodeProgressDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}