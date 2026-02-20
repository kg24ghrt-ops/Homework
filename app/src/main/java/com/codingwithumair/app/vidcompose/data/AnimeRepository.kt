package com.codingwithumair.app.vidcompose.data

import android.net.Uri

data class Anime(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val rating: String,
    val genres: List<String>,
    val episodes: List<Episode>
)

data class Episode(
    val number: Int,
    val title: String,
    val videoUri: Uri,
    val duration: String
)

object AnimeRepository {
    val animeList = listOf(
        Anime(
            id = "1",
            title = "Cyberpunk: Edgerunners",
            description = "In a dystopia riddled with corruption and cybernetic implants, a talented but reckless street kid strives to become an edgerunner.",
            posterUrl = "https://images.alphacoders.com/126/1264421.jpg",
            rating = "9.5",
            genres = listOf("Action", "Sci-Fi", "Gore"),
            episodes = List(10) { i -> 
                Episode(i + 1, "Episode ${i + 1}: Let You Down", Uri.parse("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"), "24:00")
            }
        ),
        Anime(
            id = "2",
            title = "Jujutsu Kaisen",
            description = "A boy swallows a cursed talisman - the finger of a demon - and becomes cursed himself. He enters a shaman's school to be able to locate the demon's other body parts and thus exorcise himself.",
            posterUrl = "https://images.alphacoders.com/133/1331005.png",
            rating = "9.0",
            genres = listOf("Fantasy", "Action"),
            episodes = List(24) { i -> 
                Episode(i + 1, "Episode ${i + 1}: Ryomen Sukuna", Uri.parse("https://storage.googleapis.com/exoplayer-test-media-0/android-screens-erase.mp4"), "23:45")
            }
        )
    )
}
