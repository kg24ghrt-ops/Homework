package com.codingwithumair.app.vidcompose

import android.net.Uri

data class Anime(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String,
    val episodes: List<Episode>
)

data class Episode(
    val title: String,
    val videoUri: Uri
)

val sampleAnimeList = listOf(
    Anime(
        id = 1,
        title = "Cyberpunk: Edgerunners",
        description = "A street kid tries to survive in a technology-obsessed city.",
        posterUrl = "https://picsum.photos/seed/anime1/400/600",
        episodes = listOf(Episode("Episode 1", Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")))
    ),
    Anime(
        id = 2,
        title = "Chainsaw Man",
        description = "A teenage boy living with a Chainsaw Devil.",
        posterUrl = "https://picsum.photos/seed/anime2/400/600",
        episodes = listOf(Episode("Episode 1", Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")))
    )
)
