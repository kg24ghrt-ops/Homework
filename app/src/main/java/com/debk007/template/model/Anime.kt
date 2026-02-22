package com.debk007.template.model

import kotlinx.serialization.Serializable

@Serializable
data class Anime(
    val id: String,
    val title: String,
    val imageUrl: String,
    val rating: Double,
    val description: String,
    val episodes: List<Episode> = emptyList()
)

@Serializable
data class Episode(
    val number: Int,
    val title: String
)

@Serializable
data class AnimeResponseDto(
    val data: List<AnimeDataDto>
)

@Serializable
data class AnimeDataDto(
    @kotlinx.serialization.SerialName("mal_id") val id: Int,
    val title: String,
    val images: ImageDto,
    val score: Double?,
    val synopsis: String?,
    @kotlinx.serialization.SerialName("episodes") val episodesCount: Int?
)

@Serializable
data class ImageDto(val webp: WebpDto)

@Serializable
data class WebpDto(@kotlinx.serialization.SerialName("image_url") val url: String)

// The mapper function the Repository is looking for
fun AnimeResponseDto.toAnimeList(): List<Anime> {
    return data.map { dto ->
        Anime(
            id = dto.id.toString(),
            title = dto.title,
            imageUrl = dto.images.webp.url,
            rating = dto.score ?: 0.0,
            description = dto.synopsis ?: "",
            episodes = (1..(dto.episodesCount ?: 0)).map { Episode(it, "Episode $it") }
        )
    }
}
