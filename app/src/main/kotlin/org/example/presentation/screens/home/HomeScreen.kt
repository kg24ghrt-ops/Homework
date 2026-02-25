// presentation/screens/home/HomeScreen.kt
package com.openanimelib.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.pager.*
import com.openanimelib.domain.model.Anime
import com.openanimelib.presentation.common.*
import com.openanimelib.presentation.navigation.Screen
import com.openanimelib.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "🎌 OpenAnimeLib",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Schedule.route)
                    }) {
                        Icon(Icons.Filled.CalendarMonth, "Schedule")
                    }
                }
            )
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            HomeShimmerLoading(modifier = Modifier.padding(paddingValues))
        } else if (uiState.error != null && uiState.trending.isEmpty()) {
            ErrorView(
                message = uiState.error!!,
                onRetry = { viewModel.loadHomeData() },
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Hero Banner
                item {
                    HeroBanner(
                        animeList = uiState.trending.take(5),
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                        }
                    )
                }

                // Trending Section
                item {
                    AnimeRow(
                        title = "🔥 Trending Now",
                        animeList = uiState.trending,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                        },
                        onSeeAllClick = {
                            navController.navigate(Screen.Browse.route)
                        }
                    )
                }

                // Seasonal Section
                item {
                    AnimeRow(
                        title = "🌸 This Season",
                        animeList = uiState.seasonal,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                        }
                    )
                }

                // Genre Quick Access
                item {
                    GenreQuickAccess(
                        onGenreClick = { genre ->
                            navController.navigate(Screen.Genre.createRoute(genre))
                        }
                    )
                }

                // Random Anime Picker
                item {
                    RandomPickerCard(
                        randomAnime = uiState.randomPick,
                        onPickRandom = { viewModel.pickRandomAnime() },
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                        }
                    )
                }

                // Bottom spacing
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HeroBanner(
    animeList: List<Anime>,
    onAnimeClick: (Int) -> Unit
) {
    if (animeList.isEmpty()) return

    val pagerState = rememberPagerState()

    Column {
        HorizontalPager(
            count = animeList.size,
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            itemSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) { page ->
            val anime = animeList[page]

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAnimeClick(anime.id) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = anime.bannerImage ?: anime.coverImage,
                        contentDescription = anime.title.display,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    ),
                                    startY = 100f
                                )
                            )
                    )

                    // Info overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = anime.title.display,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            anime.rating?.let { rating ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Star,
                                        null,
                                        tint = StarYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        String.format("%.1f", rating),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            anime.genres.take(3).forEach { genre ->
                                GenreChip(genre = genre, compact = true)
                            }
                        }
                    }
                }
            }
        }

        // Page indicators
        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
            activeColor = AccentPrimary,
            inactiveColor = TextMuted
        )
    }
}

@Composable
fun GenreQuickAccess(onGenreClick: (String) -> Unit) {
    val genres = listOf(
        "Action" to "⚔️", "Romance" to "💕", "Comedy" to "😂",
        "Fantasy" to "🧙", "Sci-Fi" to "🚀", "Horror" to "👻",
        "Slice of Life" to "🌻", "Mecha" to "🤖", "Sports" to "⚽",
        "Mystery" to "🔍", "Drama" to "🎭", "Adventure" to "🗺️"
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "📂 Browse by Genre",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(genres) { (name, emoji) ->
                ElevatedFilterChip(
                    selected = false,
                    onClick = { onGenreClick(name) },
                    label = { Text("$emoji $name") },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun RandomPickerCard(
    randomAnime: Anime?,
    onPickRandom: () -> Unit,
    onAnimeClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎲 Can't Decide?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Let us pick a random anime for you!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPickRandom,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Casino, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pick Random Anime!", fontWeight = FontWeight.Bold)
            }

            // Show random result
            randomAnime?.let { anime ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAnimeClick(anime.id) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = anime.coverImage,
                        contentDescription = anime.title.display,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(80.dp)
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = anime.title.display,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${anime.type.name} • ${anime.episodes ?: "?"} episodes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        anime.rating?.let {
                            Text(
                                text = "⭐ ${String.format("%.1f", it)}",
                                fontWeight = FontWeight.Bold,
                                color = StarYellow
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            anime.genres.take(3).forEach { GenreChip(it, compact = true) }
                        }
                    }
                }
            }
        }
    }
}