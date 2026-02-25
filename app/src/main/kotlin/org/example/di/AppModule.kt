// di/AppModule.kt
package com.openanimelib.di

import android.content.Context
import androidx.room.Room
import com.openanimelib.data.local.db.AnimeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AnimeDatabase {
        return Room.databaseBuilder(
            context,
            AnimeDatabase::class.java,
            "openanimelib.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideAnimeDao(db: AnimeDatabase) = db.animeDao()
    @Provides fun provideSourceDao(db: AnimeDatabase) = db.sourceDao()
    @Provides fun provideWatchlistDao(db: AnimeDatabase) = db.watchlistDao()
    @Provides fun provideFavoriteDao(db: AnimeDatabase) = db.favoriteDao()
    @Provides fun provideEpisodeProgressDao(db: AnimeDatabase) = db.episodeProgressDao()
    @Provides fun provideSearchHistoryDao(db: AnimeDatabase) = db.searchHistoryDao()
}

// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RateLimitInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAniListApi(client: OkHttpClient): AniListApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(AniListApi.BASE_URL)
            .client(client)
            .addConverterFactory(Json {
                ignoreUnknownKeys = true
                isLenient = true
            }.asConverterFactory(contentType))
            .build()
            .create(AniListApi::class.java)
    }
}

// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAnimeRepository(impl: AnimeRepositoryImpl): AnimeRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}