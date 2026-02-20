package com.codingwithumair.app.vidcompose.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codingwithumair.app.vidcompose.data.AnimeRepository
// Add this import to fix "Unresolved reference: clickable"
import androidx.compose.foundation.clickable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(animeId: String?, onBack: () -> Unit, onEpisodeClick: (Uri) -> Unit) {
    val anime = AnimeRepository.animeList.find { it.id == animeId } ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                AsyncImage(
                    model = anime.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.padding(16.dp)) {
                    Text(anime.title, style = MaterialTheme.typography.headlineMedium)
                    Row(Modifier.padding(vertical = 8.dp)) {
                        anime.genres.forEach { genre ->
                            SuggestionChip(onClick = {}, label = { Text(genre) }, modifier = Modifier.padding(end = 4.dp))
                        }
                    }
                    Text(anime.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(24.dp))
                    Text("Episodes", style = MaterialTheme.typography.titleLarge)
                }
            }
            items(anime.episodes) { episode ->
                ListItem(
                    headlineContent = { Text("Episode ${episode.number}: ${episode.title}") },
                    supportingContent = { Text(episode.duration) },
                    trailingContent = { Icon(Icons.Default.PlayArrow, null) },
                    modifier = Modifier.padding(horizontal = 8.dp).clickable { onEpisodeClick(episode.videoUri) }
                )
            }
        }
    }
}
