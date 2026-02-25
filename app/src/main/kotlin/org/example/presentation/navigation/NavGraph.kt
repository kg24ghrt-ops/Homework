// presentation/navigation/NavGraph.kt
package com.openanimelib.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.openanimelib.presentation.screens.browse.BrowseScreen
import com.openanimelib.presentation.screens.detail.AnimeDetailScreen
import com.openanimelib.presentation.screens.genre.GenreScreen
import com.openanimelib.presentation.screens.home.HomeScreen
import com.openanimelib.presentation.screens.search.SearchScreen
import com.openanimelib.presentation.screens.settings.SettingsScreen
import com.openanimelib.presentation.screens.watchlist.WatchlistScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Browse,
        BottomNavItem.Search,
        BottomNavItem.Watchlist,
        BottomNavItem.Settings
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon
                                                  else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }

            composable(Screen.Browse.route) {
                BrowseScreen(navController = navController)
            }

            composable(Screen.Search.route) {
                SearchScreen(navController = navController)
            }

            composable(Screen.Watchlist.route) {
                WatchlistScreen(navController = navController)
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.AnimeDetail.route,
                arguments = listOf(
                    navArgument("animeId") { type = NavType.IntType }
                )
            ) {
                AnimeDetailScreen(navController = navController)
            }

            composable(
                route = Screen.Genre.route,
                arguments = listOf(
                    navArgument("genreName") { type = NavType.StringType }
                )
            ) {
                GenreScreen(navController = navController)
            }
        }
    }
}