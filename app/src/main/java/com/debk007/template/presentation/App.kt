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
    // We'll use a single ViewModel for now, or separate ones per screen as needed
    val viewModel = hiltViewModel<AnimeViewModel>()

    NavHost(
        navController = navController, 
        startDestination = "home"
    ) {
        // 1. Home Screen
        composable("home") {
            val state by viewModel.animeListState.collectAsState()
            HomeScreen(
                animeList = state,
                onAnimeClick = { animeId -> 
                    navController.navigate("detail/$animeId") 
                },
                onSearchClick = { 
                    navController.navigate("search") 
                }
            )
        }

        // 2. Search Screen
        composable("search") {
            val searchResults by viewModel.searchResultsState.collectAsState()
            SearchScreen(
                results = searchResults,
                onQueryChange = { query -> viewModel.searchAnime(query) },
                onAnimeClick = { animeId -> 
                    navController.navigate("detail/$animeId") 
                }
            )
        }

        // 3. Detail Screen (with Argument)
        composable(
            route = "detail/{animeId}",
            arguments = listOf(navArgument("animeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId")
            // Fetch the specific anime details from state or repo
            val anime = viewModel.getAnimeById(animeId) 
            
            anime?.let {
                DetailScreen(anime = it)
            }
        }
    }
}
