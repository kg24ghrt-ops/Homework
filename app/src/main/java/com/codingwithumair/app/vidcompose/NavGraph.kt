// File: NavGraph.kt
package com.codingwithumair.app.vidcompose

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AnimeNavHost(onPlayVideo: (String) -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onAnimeClick = { animeId ->
                navController.navigate("detail/$animeId")
            })
        }
        composable(
            "detail/{animeId}",
            arguments = listOf(navArgument("animeId") { type = NavType.IntType })
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getInt("animeId")
            val anime = sampleAnimeList.find { it.id == animeId }
            anime?.let {
                AnimeDetailScreen(
                    anime = it,
                    onBack = { navController.popBackStack() },
                    onEpisodeClick = { url -> onPlayVideo(url) }
                )
            }
        }
    }
}
