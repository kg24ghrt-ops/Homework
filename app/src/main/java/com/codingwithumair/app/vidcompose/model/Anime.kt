package com.codingwithumair.app.vidcompose.model

data class Anime(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String,
    val episodes: List<Episode>,
    val genres: List<String> = listOf("Action", "Fantasy", "Sci-Fi")
)

data class Episode(
    val id: Int,
    val title: String,
    val videoUrl: String,
    val thumbnail: String
)
