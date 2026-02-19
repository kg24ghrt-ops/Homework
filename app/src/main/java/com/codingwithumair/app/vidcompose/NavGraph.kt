package com.codingwithumair.app.vidcompose

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

@Composable
fun AnimeNavHost(onPlayVideo: (Uri) -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onAnimeClick = { id -> navController.navigate("detail/$id") })
        }
        composable(
            route = "detail/{animeId}",
            arguments = listOf(navArgument("animeId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("animeId")
            val anime = sampleAnimeList.find { it.id == id }
            anime?.let {
                AnimeDetailScreen(
                    anime = it,
                    onBack = { navController.popBackStack() },
                    onPlayEpisode = { uri -> onPlayVideo(uri) }
                )
            }
        }
    }
}
