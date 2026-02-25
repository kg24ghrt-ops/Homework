// domain/usecase/anime/GetTrendingAnimeUseCase.kt
package com.openanimelib.domain.usecase.anime

import androidx.paging.PagingData
import com.openanimelib.domain.model.Anime
import com.openanimelib.domain.repository.AnimeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrendingAnimeUseCase @Inject constructor(
    private val repository: AnimeRepository
) {
    operator fun invoke(): Flow<PagingData<Anime>> {
        return repository.getTrendingAnime()
    }
}

// domain/usecase/anime/GetAnimeDetailUseCase.kt
class GetAnimeDetailUseCase @Inject constructor(
    private val repository: AnimeRepository
) {
    operator fun invoke(id: Int): Flow<Resource<AnimeDetail>> {
        return repository.getAnimeDetail(id)
    }
}

// domain/usecase/search/SearchAnimeUseCase.kt
class SearchAnimeUseCase @Inject constructor(
    private val repository: AnimeRepository
) {
    operator fun invoke(
        query: String,
        genre: String? = null,
        year: Int? = null,
        season: String? = null,
        format: String? = null,
        sort: String = "POPULARITY_DESC"
    ): Flow<PagingData<Anime>> {
        return repository.searchAnime(query, genre, year, season, format, sort = sort)
    }
}

// domain/usecase/watchlist/AddToWatchlistUseCase.kt
class AddToWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository
) {
    suspend operator fun invoke(animeId: Int, status: WatchStatus) {
        repository.addToWatchlist(animeId, status)
    }
}

// domain/usecase/watchlist/GetWatchlistUseCase.kt
class GetWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository
) {
    operator fun invoke(status: WatchStatus? = null): Flow<List<WatchlistItem>> {
        return if (status != null) {
            repository.getWatchlistByStatus(status)
        } else {
            repository.getAllWatchlist()
        }
    }
}

// domain/usecase/recommendation/GetRecommendationsUseCase.kt
class GetRecommendationsUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val animeRepository: AnimeRepository
) {
    operator fun invoke(): Flow<Resource<List<Anime>>> = flow {
        emit(Resource.Loading())
        try {
            // Get user's top genres from watchlist
            val watchlist = watchlistRepository.getAllWatchlistOnce()
            val genreCounts = mutableMapOf<String, Int>()

            watchlist.forEach { item ->
                item.anime.genres.forEach { genre ->
                    genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
                }
            }

            val topGenre = genreCounts.maxByOrNull { it.value }?.key ?: "Action"
            val watchedIds = watchlist.map { it.anime.id }.toSet()

            // Search for anime in top genre not in watchlist
            // Collect first page from paging source
            val recs = animeRepository.getRecommendationsByGenre(topGenre)
                .filter { it.id !in watchedIds }
                .take(20)

            emit(Resource.Success(recs))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to get recommendations"))
        }
    }
}