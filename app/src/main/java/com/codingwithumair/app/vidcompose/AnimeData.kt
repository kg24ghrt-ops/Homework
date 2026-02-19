package com.codingwithumair.app.vidcompose

import android.net.Uri

data class Anime(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String,
    val genres: List<String>,
    val episodes: List<Episode>
)

data class Episode(
    val title: String,
    val videoUri: Uri
)

val sampleAnimeList = listOf(
    Anime(
        1, "Cyberpunk: Edgerunners", "In a reality of corruption and cybernetic implants, a street kid tries to survive.",
        "https://images.justwatch.com/poster/300501170/s718/cyberpunk-edgerunners.jpg",
        listOf("Action", "Sci-Fi"),
        listOf(Episode("Episode 1: Let You Down", Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")))
    ),
    Anime(
        2, "Chainsaw Man", "Denji has a simple dream—to live a happy and peaceful life, spending time with a girl he likes.",
        "https://picsum.photos/seed/anime2/400/600",
        listOf("Action", "Gore"),
        listOf(Episode("Episode 1: Dog & Chainsaw", Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")))
    )
)
