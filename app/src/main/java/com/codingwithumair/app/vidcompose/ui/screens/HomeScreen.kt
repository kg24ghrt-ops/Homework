package com.codingwithumair.app.vidcompose.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codingwithumair.app.vidcompose.data.AnimeRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onAnimeClick: (String) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("AnimeApp", style = MaterialTheme.typography.headlineLarge) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            items(AnimeRepository.animeList) { anime ->
                ElevatedCard(
                    onClick = { onAnimeClick(anime.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(modifier = Modifier.height(160.dp)) {
                        AsyncImage(
                            model = anime.posterUrl,
                            contentDescription = null,
                            modifier = Modifier.width(110.dp).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(anime.title, style = MaterialTheme.typography.titleLarge)
                            Text("${anime.rating} ★ • ${anime.genres.first()}", 
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(anime.description, 
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3)
                        }
                    }
                }
            }
        }
    }
}
