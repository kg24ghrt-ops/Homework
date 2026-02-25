// data/remote/dto/anilist/AniListResponse.kt
package com.openanimelib.data.remote.dto.anilist

import kotlinx.serialization.Serializable

@Serializable
data class AniListResponse(
    val data: AniListData? = null
)

@Serializable
data class AniListData(
    val Page: PageData? = null,
    val Media: MediaDto? = null
)

@Serializable
data class PageData(
    val pageInfo: PageInfoDto? = null,
    val media: List<MediaDto>? = null
)

@Serializable
data class PageInfoDto(
    val total: Int? = null,
    val currentPage: Int? = null,
    val lastPage: Int? = null,
    val hasNextPage: Boolean? = null,
    val perPage: Int? = null
)

@Serializable
data class MediaDto(
    val id: Int,
    val idMal: Int? = null,
    val title: TitleDto? = null,
    val description: String? = null,
    val genres: List<String>? = null,
    val tags: List<TagDto>? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val coverImage: CoverImageDto? = null,
    val bannerImage: String? = null,
    val studios: StudioConnectionDto? = null,
    val format: String? = null,
    val source: String? = null,
    val trailer: TrailerDto? = null,
    val characters: CharacterConnectionDto? = null,
    val recommendations: RecommendationConnectionDto? = null,
    val relations: RelationConnectionDto? = null
)

@Serializable
data class TitleDto(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class CoverImageDto(
    val large: String? = null,
    val extraLarge: String? = null
)

@Serializable
data class TagDto(
    val name: String,
    val rank: Int? = null
)

@Serializable
data class TrailerDto(
    val id: String? = null,
    val site: String? = null
)

@Serializable
data class StudioConnectionDto(
    val nodes: List<StudioDto>? = null
)

@Serializable
data class StudioDto(
    val name: String
)

@Serializable
data class CharacterConnectionDto(
    val nodes: List<CharacterDto>? = null
)

@Serializable
data class CharacterDto(
    val name: CharacterNameDto? = null,
    val image: CharacterImageDto? = null
)

@Serializable
data class CharacterNameDto(val full: String? = null)

@Serializable
data class CharacterImageDto(val medium: String? = null)

@Serializable
data class RecommendationConnectionDto(
    val nodes: List<RecommendationNodeDto>? = null
)

@Serializable
data class RecommendationNodeDto(
    val mediaRecommendation: MediaDto? = null
)

@Serializable
data class RelationConnectionDto(
    val nodes: List<MediaDto>? = null
)