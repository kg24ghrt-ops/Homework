// data/remote/api/AniListQueries.kt
package com.openanimelib.data.remote.api

object AniListQueries {

    val TRENDING_ANIME = """
        query (${'$'}page: Int, ${'$'}perPage: Int) {
            Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                    total currentPage lastPage hasNextPage perPage
                }
                media(type: ANIME, sort: TRENDING_DESC) {
                    id idMal
                    title { romaji english native }
                    description
                    genres
                    episodes
                    status
                    season seasonYear
                    averageScore popularity
                    coverImage { large extraLarge }
                    bannerImage
                    studios { nodes { name } }
                    format
                }
            }
        }
    """.trimIndent()

    val SEASONAL_ANIME = """
        query (${'$'}season: MediaSeason, ${'$'}year: Int, ${'$'}page: Int, ${'$'}perPage: Int) {
            Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                    total currentPage lastPage hasNextPage
                }
                media(type: ANIME, season: ${'$'}season, seasonYear: ${'$'}year, sort: POPULARITY_DESC) {
                    id idMal
                    title { romaji english native }
                    genres episodes status
                    season seasonYear
                    averageScore popularity
                    coverImage { large extraLarge }
                    bannerImage
                    studios { nodes { name } }
                    format
                }
            }
        }
    """.trimIndent()

    val SEARCH_ANIME = """
        query (${'$'}search: String, ${'$'}page: Int, ${'$'}perPage: Int, 
               ${'$'}genre: String, ${'$'}sort: [MediaSort], ${'$'}format: MediaFormat,
               ${'$'}status: MediaStatus, ${'$'}season: MediaSeason, ${'$'}year: Int) {
            Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                    total currentPage lastPage hasNextPage
                }
                media(
                    search: ${'$'}search, type: ANIME, sort: ${'$'}sort,
                    genre: ${'$'}genre, format: ${'$'}format, status: ${'$'}status,
                    season: ${'$'}season, seasonYear: ${'$'}year
                ) {
                    id idMal
                    title { romaji english native }
                    genres episodes status
                    season seasonYear
                    averageScore popularity
                    coverImage { large extraLarge }
                    format
                    studios { nodes { name } }
                }
            }
        }
    """.trimIndent()

    val ANIME_DETAIL = """
        query (${'$'}id: Int) {
            Media(id: ${'$'}id, type: ANIME) {
                id idMal
                title { romaji english native }
                description
                genres
                tags { name rank }
                episodes duration
                status season seasonYear
                averageScore meanScore popularity
                coverImage { large extraLarge }
                bannerImage
                studios { nodes { name } }
                format source
                trailer { id site }
                characters(sort: ROLE, page: 1, perPage: 15) {
                    nodes {
                        name { full }
                        image { medium }
                    }
                }
                recommendations(sort: RATING_DESC, page: 1, perPage: 15) {
                    nodes {
                        mediaRecommendation {
                            id
                            title { romaji english }
                            coverImage { large }
                            averageScore
                            genres
                            format
                            episodes
                        }
                    }
                }
                relations {
                    nodes {
                        id
                        title { romaji english }
                        format type
                        coverImage { large }
                        averageScore
                    }
                }
            }
        }
    """.trimIndent()

    val ANIME_BY_GENRE = """
        query (${'$'}genre: String, ${'$'}page: Int, ${'$'}perPage: Int) {
            Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo { total currentPage lastPage hasNextPage }
                media(type: ANIME, genre: ${'$'}genre, sort: POPULARITY_DESC) {
                    id idMal
                    title { romaji english native }
                    genres episodes status
                    averageScore popularity
                    coverImage { large extraLarge }
                    format
                    studios { nodes { name } }
                }
            }
        }
    """.trimIndent()
}