// data/remote/paging/AnimePagingSource.kt
package com.openanimelib.data.remote.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.openanimelib.data.mapper.AnimeMapper
import com.openanimelib.data.remote.api.AniListApi
import com.openanimelib.data.remote.api.AniListQueries
import com.openanimelib.data.remote.api.GraphQLBody
import com.openanimelib.domain.model.Anime

class TrendingAnimePagingSource(
    private val api: AniListApi,
    private val mapper: AnimeMapper
) : PagingSource<Int, Anime>() {

    override fun getRefreshKey(state: PagingState<Int, Anime>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Anime> {
        val page = params.key ?: 1

        return try {
            val response = api.query(
                GraphQLBody(
                    query = AniListQueries.TRENDING_ANIME,
                    variables = mapOf(
                        "page" to page,
                        "perPage" to params.loadSize
                    )
                )
            )

            val mediaList = response.data?.Page?.media ?: emptyList()
            val pageInfo = response.data?.Page?.pageInfo
            val animeList = mediaList.map { mapper.dtoToDomain(it) }

            LoadResult.Page(
                data = animeList,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (pageInfo?.hasNextPage == true) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

class SearchAnimePagingSource(
    private val api: AniListApi,
    private val mapper: AnimeMapper,
    private val query: String,
    private val genre: String? = null,
    private val year: Int? = null,
    private val season: String? = null,
    private val format: String? = null,
    private val status: String? = null,
    private val sort: String = "POPULARITY_DESC"
) : PagingSource<Int, Anime>() {

    override fun getRefreshKey(state: PagingState<Int, Anime>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Anime> {
        val page = params.key ?: 1

        return try {
            val variables = mutableMapOf<String, Any?>(
                "page" to page,
                "perPage" to params.loadSize,
                "sort" to listOf(sort)
            )

            if (query.isNotBlank()) variables["search"] = query
            genre?.let { variables["genre"] = it }
            year?.let { variables["year"] = it }
            season?.let { variables["season"] = it }
            format?.let { variables["format"] = it }
            status?.let { variables["status"] = it }

            val response = api.query(
                GraphQLBody(
                    query = AniListQueries.SEARCH_ANIME,
                    variables = variables
                )
            )

            val mediaList = response.data?.Page?.media ?: emptyList()
            val pageInfo = response.data?.Page?.pageInfo
            val animeList = mediaList.map { mapper.dtoToDomain(it) }

            LoadResult.Page(
                data = animeList,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (pageInfo?.hasNextPage == true) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}