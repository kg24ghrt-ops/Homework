// data/mapper/AnimeMapper.kt
package com.openanimelib.data.mapper

import com.openanimelib.data.local.db.entity.AnimeEntity
import com.openanimelib.data.local.db.entity.SourceEntity
import com.openanimelib.data.remote.dto.anilist.MediaDto
import com.openanimelib.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AnimeMapper @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    // DTO -> Domain
    fun dtoToDomain(dto: MediaDto): Anime {
        return Anime(
            id = dto.id,
            anilistId = dto.id,
            malId = dto.idMal,
            title = AnimeTitle(
                english = dto.title?.english,
                romaji = dto.title?.romaji,
                japanese = dto.title?.native
            ),
            coverImage = dto.coverImage?.extraLarge ?: dto.coverImage?.large ?: "",
            bannerImage = dto.bannerImage,
            rating = dto.averageScore?.toFloat()?.div(10f),
            episodes = dto.episodes,
            type = parseAnimeType(dto.format),
            status = parseAnimeStatus(dto.status),
            genres = dto.genres ?: emptyList(),
            year = dto.seasonYear,
            season = dto.season?.let { parseAnimeSeason(it) },
            studio = dto.studios?.nodes?.firstOrNull()?.name,
            popularity = dto.popularity ?: 0
        )
    }

    fun dtoToDetail(dto: MediaDto, sources: List<StreamingSource> = emptyList()): AnimeDetail {
        return AnimeDetail(
            anime = dtoToDomain(dto),
            synopsis = dto.description?.replace(Regex("<[^>]*>"), ""),
            tags = dto.tags?.map { it.name } ?: emptyList(),
            duration = dto.duration,
            source = dto.source,
            characters = dto.characters?.nodes?.map {
                Character(
                    name = it.name?.full ?: "Unknown",
                    image = it.image?.medium,
                    role = "Character"
                )
            } ?: emptyList(),
            relations = dto.relations?.nodes?.map { dtoToDomain(it) } ?: emptyList(),
            recommendations = dto.recommendations?.nodes
                ?.mapNotNull { it.mediaRecommendation }
                ?.map { dtoToDomain(it) } ?: emptyList(),
            streamingSources = sources,
            trailer = dto.trailer?.let {
                if (it.site == "youtube") "https://youtube.com/watch?v=${it.id}" else null
            }
        )
    }

    // Domain -> Entity
    fun domainToEntity(anime: Anime): AnimeEntity {
        return AnimeEntity(
            id = anime.id,
            anilistId = anime.anilistId,
            malId = anime.malId,
            titleEnglish = anime.title.english,
            titleRomaji = anime.title.romaji,
            titleJapanese = anime.title.japanese,
            synopsis = null,
            coverImage = anime.coverImage,
            bannerImage = anime.bannerImage,
            rating = anime.rating,
            episodes = anime.episodes,
            type = anime.type.name,
            status = anime.status.name,
            genres = json.encodeToString(anime.genres),
            tags = null,
            year = anime.year,
            season = anime.season?.name,
            studio = anime.studio,
            popularity = anime.popularity,
            duration = null,
            source = null,
            trailer = null
        )
    }

    // Entity -> Domain
    fun entityToDomain(entity: AnimeEntity): Anime {
        return Anime(
            id = entity.id,
            anilistId = entity.anilistId,
            malId = entity.malId,
            title = AnimeTitle(
                english = entity.titleEnglish,
                romaji = entity.titleRomaji,
                japanese = entity.titleJapanese
            ),
            coverImage = entity.coverImage,
            bannerImage = entity.bannerImage,
            rating = entity.rating,
            episodes = entity.episodes,
            type = AnimeType.valueOf(entity.type),
            status = AnimeStatus.valueOf(entity.status),
            genres = try {
                json.decodeFromString<List<String>>(entity.genres)
            } catch (e: Exception) {
                emptyList()
            },
            year = entity.year,
            season = entity.season?.let { AnimeSeason.valueOf(it) },
            studio = entity.studio,
            popularity = entity.popularity
        )
    }

    fun sourceEntityToDomain(entity: SourceEntity): StreamingSource {
        return StreamingSource(
            platform = StreamingPlatform.valueOf(entity.platform),
            url = entity.url,
            hasAds = entity.hasAds,
            quality = entity.quality,
            isDub = entity.isDub,
            isSub = entity.isSub,
            regionRestrictions = try {
                json.decodeFromString(entity.regionRestrictions)
            } catch (e: Exception) { emptyList() },
            availableEpisodes = try {
                json.decodeFromString(entity.availableEpisodes)
            } catch (e: Exception) { emptyList() }
        )
    }

    private fun parseAnimeType(format: String?): AnimeType {
        return when (format) {
            "TV" -> AnimeType.TV
            "MOVIE" -> AnimeType.MOVIE
            "OVA" -> AnimeType.OVA
            "ONA" -> AnimeType.ONA
            "SPECIAL" -> AnimeType.SPECIAL
            "MUSIC" -> AnimeType.MUSIC
            else -> AnimeType.TV
        }
    }

    private fun parseAnimeStatus(status: String?): AnimeStatus {
        return when (status) {
            "RELEASING" -> AnimeStatus.AIRING
            "FINISHED" -> AnimeStatus.COMPLETED
            "NOT_YET_RELEASED" -> AnimeStatus.UPCOMING
            "HIATUS" -> AnimeStatus.HIATUS
            "CANCELLED" -> AnimeStatus.CANCELLED
            else -> AnimeStatus.COMPLETED
        }
    }

    private fun parseAnimeSeason(season: String): AnimeSeason {
        return when (season) {
            "WINTER" -> AnimeSeason.WINTER
            "SPRING" -> AnimeSeason.SPRING
            "SUMMER" -> AnimeSeason.SUMMER
            "FALL" -> AnimeSeason.FALL
            else -> AnimeSeason.SPRING
        }
    }
}