// domain/model/Anime.kt
package com.openanimelib.domain.model

data class Anime(
    val id: Int,
    val anilistId: Int,
    val malId: Int?,
    val title: AnimeTitle,
    val coverImage: String,
    val bannerImage: String?,
    val rating: Float?,
    val episodes: Int?,
    val type: AnimeType,
    val status: AnimeStatus,
    val genres: List<String>,
    val year: Int?,
    val season: AnimeSeason?,
    val studio: String?,
    val popularity: Int = 0
)

data class AnimeTitle(
    val english: String?,
    val romaji: String?,
    val japanese: String?
) {
    val display: String
        get() = english ?: romaji ?: japanese ?: "Unknown"
}

data class AnimeDetail(
    val anime: Anime,
    val synopsis: String?,
    val tags: List<String>,
    val duration: Int?,
    val source: String?,
    val characters: List<Character>,
    val relations: List<Anime>,
    val recommendations: List<Anime>,
    val streamingSources: List<StreamingSource>,
    val trailer: String?
)

data class StreamingSource(
    val platform: StreamingPlatform,
    val url: String,
    val availableEpisodes: List<Int> = emptyList(),
    val hasAds: Boolean = true,
    val quality: String = "720p",
    val regionRestrictions: List<String> = emptyList(),
    val isDub: Boolean = false,
    val isSub: Boolean = true
)

data class Character(
    val name: String,
    val image: String?,
    val role: String
)

data class WatchlistItem(
    val anime: Anime,
    val status: WatchStatus,
    val currentEpisode: Int,
    val rating: Float?,
    val notes: String?,
    val addedAt: Long,
    val updatedAt: Long
)

// Enums
enum class AnimeType { TV, MOVIE, OVA, ONA, SPECIAL, MUSIC }

enum class AnimeStatus { AIRING, COMPLETED, UPCOMING, HIATUS, CANCELLED }

enum class AnimeSeason { WINTER, SPRING, SUMMER, FALL }

enum class WatchStatus { WATCHING, COMPLETED, PLANNED, ON_HOLD, DROPPED }

enum class StreamingPlatform(
    val displayName: String,
    val baseUrl: String,
    val iconRes: Int = 0
) {
    CRUNCHYROLL("Crunchyroll", "https://crunchyroll.com"),
    TUBI("Tubi TV", "https://tubitv.com"),
    RETROCRUSH("RetroCrush", "https://retrocrush.tv"),
    PLUTO_TV("Pluto TV", "https://pluto.tv"),
    YOUTUBE_MUSE("Muse Asia", "https://youtube.com/@MuseAsia"),
    YOUTUBE_ANIONE("Ani-One", "https://youtube.com/@AniOneAsia"),
    YOUTUBE_OFFICIAL("YouTube", "https://youtube.com"),
    POKEMON_TV("Pokémon TV", "https://watch.pokemon.com"),
    VIZ("Viz Media", "https://viz.com"),
    ARCHIVE_ORG("Archive.org", "https://archive.org")
}