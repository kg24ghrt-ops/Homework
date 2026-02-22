package com.debk007.template.repository

import com.debk007.template.model.Anime
import com.debk007.template.network.ApiService
import com.debk007.template.util.ApiState
import javax.inject.Inject

class RepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : Repository {

    override suspend fun getTrendingAnime(): ApiState<List<Anime>> = try {
        // Here we call the API and map the DTO to our UI Model
        val response = apiService.getTrendingAnime()
        ApiState.Success(response.toAnimeList()) 
    } catch (e: Exception) {
        ApiState.Error(errorMsg = e.message ?: "An unknown error occurred")
    }

    override suspend fun searchAnime(query: String): ApiState<List<Anime>> = try {
        val response = apiService.searchAnime(query)
        ApiState.Success(response.toAnimeList())
    } catch (e: Exception) {
        ApiState.Error(errorMsg = e.message ?: "Search failed")
    }
}
