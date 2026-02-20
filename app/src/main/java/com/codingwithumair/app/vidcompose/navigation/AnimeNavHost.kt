package com.codingwithumair.app.vidcompose.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codingwithumair.app.vidcompose.ui.screens.HomeScreen
import com.codingwithumair.app.vidcompose.ui.screens.DetailScreen

@Composable
fun AnimeNavHost(onPlayVideo: (Uri) -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onAnimeClick = { id -> navController.navigate("detail/$id") })
        }
        composable(
            route = "detail/{animeId}",
            arguments = listOf(navArgument("animeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId")
            DetailScreen(
                animeId = animeId,
                onBack = { navController.popBackStack() },
                onEpisodeClick = onPlayVideo
            )
        }
    }
}
