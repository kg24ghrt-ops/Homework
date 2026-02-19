// File: Models.kt
package com.codingwithumair.app.vidcompose

data class Anime(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String, // Use placeholder URLs
    val episodeCount: Int,
    val genres: List<String>,
    val videoUrl: String // The URL to pass to your existing Media3 player
)

val sampleAnimeList = listOf(
    Anime(1, "Cyberpunk: Edgerunners", "A street kid tries to survive in a technology and body modification-obsessed city of the future.", "https://picsum.photos/seed/1/400/600", 10, listOf("Action", "Sci-Fi"), "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
    Anime(2, "Chainsaw Man", "Denji is a teenage boy living with a Chainsaw Devil named Pochita.", "https://picsum.photos/seed/2/400/600", 12, listOf("Action", "Horror"), "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
    Anime(3, "Spy x Family", "A spy on an undercover mission gets married and adopts a child as part of his cover.", "https://picsum.photos/seed/3/400/600", 25, listOf("Comedy", "Action"), "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
)
