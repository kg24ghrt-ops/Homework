package com.debk007.template.network

import com.debk007.template.model.AnimeResponseDto // We'll define this DTO next
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("top/anime")
    suspend fun getTrendingAnime(): AnimeResponseDto

    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String
    ): AnimeResponseDto
}
