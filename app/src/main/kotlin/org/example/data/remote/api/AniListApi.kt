// data/remote/api/AniListApi.kt
package com.openanimelib.data.remote.api

import com.openanimelib.data.remote.dto.anilist.AniListResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AniListApi {

    @POST("https://graphql.anilist.co")
    suspend fun query(@Body body: GraphQLBody): AniListResponse

    companion object {
        const val BASE_URL = "https://graphql.anilist.co/"
    }
}

@kotlinx.serialization.Serializable
data class GraphQLBody(
    val query: String,
    val variables: Map<String, @kotlinx.serialization.Serializable Any?> = emptyMap()
)