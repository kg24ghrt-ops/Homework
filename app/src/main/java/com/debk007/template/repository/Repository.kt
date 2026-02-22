package com.debk007.template.repository

import com.debk007.template.model.Anime
import com.debk007.template.util.ApiState

interface Repository {
    suspend fun getTrendingAnime(): ApiState<List<Anime>>
    suspend fun searchAnime(query: String): ApiState<List<Anime>>
}
