// presentation/navigation/Screen.kt
package com.openanimelib.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Browse : Screen("browse")
    object Search : Screen("search")
    object Watchlist : Screen("watchlist")
    object Settings : Screen("settings")
    object Schedule : Screen("schedule")

    object AnimeDetail : Screen("anime/{animeId}") {
        fun createRoute(animeId: Int) = "anime/$animeId"
    }

    object Genre : Screen("genre/{genreName}") {
        fun createRoute(genre: String) = "genre/$genre"
    }
}

// presentation/navigation/BottomNavItem.kt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        Screen.Home.route, "Home",
        Icons.Filled.Home, Icons.Outlined.Home
    )
    object Browse : BottomNavItem(
        Screen.Browse.route, "Browse",
        Icons.Filled.Explore, Icons.Outlined.Explore
    )
    object Search : BottomNavItem(
        Screen.Search.route, "Search",
        Icons.Filled.Search, Icons.Outlined.Search
    )
    object Watchlist : BottomNavItem(
        Screen.Watchlist.route, "Watchlist",
        Icons.Filled.BookmarkAdded, Icons.Outlined.BookmarkAdd
    )
    object Settings : BottomNavItem(
        Screen.Settings.route, "Settings",
        Icons.Filled.Settings, Icons.Outlined.Settings
    )
}