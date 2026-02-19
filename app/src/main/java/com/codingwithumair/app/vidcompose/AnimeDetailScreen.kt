package com.codingwithumair.app.vidcompose

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    anime: Anime,
    onBack: () -> Unit,
    onPlayEpisode: (Uri) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(anime.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(250.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.titleLarge)
                Text(anime.description, style = MaterialTheme.typography.bodyMedium)
                
                Spacer(Modifier.height(24.dp))
                Text("Episodes", style = MaterialTheme.typography.titleLarge)
                
                anime.episodes.forEach { episode ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onPlayEpisode(episode.videoUri) }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(12.dp))
                            Text(episode.title)
                        }
                    }
                }
            }
        }
    }
}
