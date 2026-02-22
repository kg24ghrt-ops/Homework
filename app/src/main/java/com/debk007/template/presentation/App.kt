package com.debk007.template.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.debk007.template.presentation.screen.DetailScreen
import com.debk007.template.presentation.screen.HomeScreen
import com.debk007.template.presentation.screen.SearchScreen
import com.debk007.template.presentation.viewmodel.AnimeViewModel

@Composable
fun App() {
    val navController = rememberNavController()
    val viewModel = hiltViewModel<AnimeViewModel>()

    NavHost(
        navController = navController, 
        startDestination = "home"
    ) {
        composable("home") {
            val state by viewModel.animeListState.collectAsState()
            HomeScreen(
                animeState = state, // Renamed from animeList to match HomeScreen.kt
                onAnimeClick = { animeId -> 
                    navController.navigate("detail/$animeId") 
                },
                onSearchClick = { 
                    navController.navigate("search") 
                }
            )
        }

        composable("search") {
            val searchResults by viewModel.searchResultsState.collectAsState()
            SearchScreen(
                animeState = searchResults, // Pass state to the new SearchScreen
                onQueryChange = { query -> viewModel.searchAnime(query) },
                onAnimeClick = { animeId -> 
                    navController.navigate("detail/$animeId") 
                }
            )
        }

        composable(
            route = "detail/{animeId}",
            arguments = listOf(navArgument("animeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId")
            val anime = viewModel.getAnimeById(animeId) 
            
            anime?.let {
                DetailScreen(
                    anime = it,
                    onBackClick = { navController.popBackStack() } // Added missing back click
                )
            }
        }
    }
}
