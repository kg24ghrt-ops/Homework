// data/repository/AnimeRepositoryImpl.kt
package com.openanimelib.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.openanimelib.data.local.db.dao.AnimeDao
import com.openanimelib.data.local.db.dao.SourceDao
import com.openanimelib.data.mapper.AnimeMapper
import com.openanimelib.data.remote.api.AniListApi
import com.openanimelib.data.remote.api.AniListQueries
import com.openanimelib.data.remote.api.GraphQLBody
import com.openanimelib.data.remote.paging.SearchAnimePagingSource
import com.openanimelib.data.remote.paging.TrendingAnimePagingSource
import com.openanimelib.domain.model.*
import com.openanimelib.domain.repository.AnimeRepository
import com.openanimelib.util.Resource
import com.openanimelib.util.SeasonUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AnimeRepositoryImpl @Inject constructor(
    private val api: AniListApi,
    private val animeDao: AnimeDao,
    private val sourceDao: SourceDao,
    private val mapper: AnimeMapper
) : AnimeRepository {

    override fun getTrendingAnime(): Flow<PagingData<Anime>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                TrendingAnimePagingSource(api, mapper)
            }
        ).flow
    }

    override fun getSeasonalAnime(): Flow<Resource<List<Anime>>> = flow {
        emit(Resource.Loading())

        // Try cache first
        val currentSeason = SeasonUtils.getCurrentSeason()
        val currentYear = SeasonUtils.getCurrentYear()

        val cached = animeDao.getSeasonalAnime(currentSeason.name, currentYear)

        try {
            val response = api.query(
                GraphQLBody(
                    query = AniListQueries.SEASONAL_ANIME,
                    variables = mapOf(
                        "season" to currentSeason.name,
                        "year" to currentYear,
                        "page" to 1,
                        "perPage" to 30
                    )
                )
            )

            val animeList = response.data?.Page?.media
                ?.map { mapper.dtoToDomain(it) } ?: emptyList()

            // Cache the results
            animeList.forEach { anime ->
                animeDao.insertAnime(mapper.domainToEntity(anime))
            }

            emit(Resource.Success(animeList))
        } catch (e: Exception) {
            // Fall back to cache
            cached.collect { entities ->
                val animeList = entities.map { mapper.entityToDomain(it) }
                if (animeList.isNotEmpty()) {
                    emit(Resource.Success(animeList))
                } else {
                    emit(Resource.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }

    override fun getAnimeDetail(id: Int): Flow<Resource<AnimeDetail>> = flow {
        emit(Resource.Loading())

        try {
            val response = api.query(
                GraphQLBody(
                    query = AniListQueries.ANIME_DETAIL,
                    variables = mapOf("id" to id)
                )
            )

            val mediaDto = response.data?.Media
            if (mediaDto != null) {
                // Get streaming sources from local DB
                val sources = mutableListOf<StreamingSource>()
                sourceDao.getSourcesForAnime(id).collect { sourceEntities ->
                    sources.addAll(sourceEntities.map { mapper.sourceEntityToDomain(it) })
                }

                val detail = mapper.dtoToDetail(mediaDto, sources)

                // Cache anime data
                animeDao.insertAnime(mapper.domainToEntity(detail.anime))

                emit(Resource.Success(detail))
            } else {
                emit(Resource.Error("Anime not found"))
            }
        } catch (e: Exception) {
            // Try from cache
            val cached = animeDao.getAnimeById(id)
            if (cached != null) {
                val anime = mapper.entityToDomain(cached)
                emit(Resource.Success(
                    AnimeDetail(
                        anime = anime,
                        synopsis = cached.synopsis,
                        tags = emptyList(),
                        duration = cached.duration,
                        source = cached.source,
                        characters = emptyList(),
                        relations = emptyList(),
                        recommendations = emptyList(),
                        streamingSources = emptyList(),
                        trailer = cached.trailer
                    )
                ))
            } else {
                emit(Resource.Error(e.message ?: "Unknown error"))
            }
        }
    }

    override fun searchAnime(
        query: String,
        genre: String?,
        year: Int?,
        season: String?,
        format: String?,
        status: String?,
        sort: String
    ): Flow<PagingData<Anime>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                SearchAnimePagingSource(
                    api = api,
                    mapper = mapper,
                    query = query,
                    genre = genre,
                    year = year,
                    season = season,
                    format = format,
                    status = status,
                    sort = sort
                )
            }
        ).flow
    }

    override fun getAnimeByGenre(genre: String): Flow<PagingData<Anime>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                SearchAnimePagingSource(
                    api = api,
                    mapper = mapper,
                    query = "",
                    genre = genre
                )
            }
        ).flow
    }

    override suspend fun getRandomAnime(): Resource<Anime> {
        return try {
            val randomPage = (1..100).random()
            val response = api.query(
                GraphQLBody(
                    query = AniListQueries.TRENDING_ANIME,
                    variables = mapOf("page" to randomPage, "perPage" to 1)
                )
            )
            val media = response.data?.Page?.media?.firstOrNull()
            if (media != null) {
                Resource.Success(mapper.dtoToDomain(media))
            } else {
                Resource.Error("No anime found")
            }
        } catch (e: Exception) {
            // Fallback to local random
            val cached = animeDao.getRandomAnime()
            if (cached != null) {
                Resource.Success(mapper.entityToDomain(cached))
            } else {
                Resource.Error(e.message ?: "Unknown error")
            }
        }
    }

    override fun getSourcesForAnime(animeId: Int): Flow<List<StreamingSource>> {
        return sourceDao.getSourcesForAnime(animeId).map { entities ->
            entities.map { mapper.sourceEntityToDomain(it) }
        }
    }
}