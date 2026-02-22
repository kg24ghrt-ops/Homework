package com.debk007.template.repository

import com.debk007.template.model.Anime
import com.debk007.template.model.toAnimeList
import com.debk007.template.network.ApiService
import com.debk007.template.util.ApiState
import javax.inject.Inject

class RepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : Repository {

    override suspend fun getTrendingAnime(): ApiState<List<Anime>> {
        return try {
            val response = apiService.getTrendingAnime()
            // Map the DTO response to our UI model using the extension function
            ApiState.Success(response.toAnimeList())
        } catch (e: Exception) {
            ApiState.Error(errorMsg = e.localizedMessage ?: "Failed to fetch trending anime")
        }
    }

    override suspend fun searchAnime(query: String): ApiState<List<Anime>> {
        return try {
            val response = apiService.searchAnime(query)
            ApiState.Success(response.toAnimeList())
        } catch (e: Exception) {
            ApiState.Error(errorMsg = e.localizedMessage ?: "Search failed")
        }
    }
}
