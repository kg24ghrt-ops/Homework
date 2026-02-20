package com.codingwithumair.app.vidcompose.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage // Updated for Coil 3.0
import com.codingwithumair.app.vidcompose.data.AnimeRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onAnimeClick: (String) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Discover Anime", 
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                // Added WindowInsets support to prevent overlapping with status bar
                windowInsets = TopAppBarDefaults.windowInsets,
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        // Use the padding provided by Scaffold to handle top/bottom safe areas
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp, // Extra space at bottom
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = AnimeRepository.animeList,
                key = { it.id } // Adding a key improves LazyColumn performance
            ) { anime ->
                AnimeItemCard(
                    title = anime.title,
                    rating = anime.rating,
                    genre = anime.genres.firstOrNull() ?: "Action",
                    description = anime.description,
                    posterUrl = anime.posterUrl,
                    onClick = { onAnimeClick(anime.id) }
                )
            }
        }
    }
}

@Composable
fun AnimeItemCard(
    title: String,
    rating: Double,
    genre: String,
    description: String,
    posterUrl: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(modifier = Modifier.height(160.dp)) {
            AsyncImage(
                model = posterUrl,
                contentDescription = "Poster for $title",
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$rating ★ • $genre", 
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description, 
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
