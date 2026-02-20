package com.codingwithumair.app.vidcompose.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage // Coil 3.0 update
import com.codingwithumair.app.vidcompose.data.AnimeRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    animeId: String?, 
    onBack: () -> Unit, 
    onEpisodeClick: (Uri) -> Unit
) {
    // 1. Data Safety: Handle null IDs or missing data gracefully
    val anime = AnimeRepository.animeList.find { it.id == animeId } ?: run {
        Box(Modifier.fillMaxSize()) { Text("Anime not found", Modifier.align(androidx.compose.ui.Alignment.Center)) }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // 2. Glassmorphism TopBar: Stays readable even over bright posters
            TopAppBar(
                title = { },
                navigationIcon = {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // 3. Inset Awareness: padding from Scaffold + extra bottom spacing for gesture nav
            contentPadding = PaddingValues(
                bottom = padding.calculateBottomPadding() + 64.dp 
            )
        ) {
            item {
                // Large Poster with fixed aspect ratio
                AsyncImage(
                    model = anime.posterUrl,
                    contentDescription = "Poster for ${anime.title}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentScale = ContentScale.Crop
                )

                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = anime.title, 
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // 4. Material 3 FlowRow: Automatically wraps genres to the next line
                    FlowRow(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        anime.genres.forEach { genre ->
                            SuggestionChip(
                                onClick = { /* Search by genre? */ },
                                label = { Text(genre) }
                            )
                        }
                    }

                    Text(
                        text = anime.description, 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    Text("Episodes", style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), alpha = 0.5f)
                }
            }

            // 5. Episode List
            items(anime.episodes, key = { it.number }) { episode ->
                ListItem(
                    headlineContent = { 
                        Text("Episode ${episode.number}: ${episode.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) 
                    },
                    supportingContent = { Text(episode.duration) },
                    trailingContent = { 
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEpisodeClick(episode.videoUri) }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}
