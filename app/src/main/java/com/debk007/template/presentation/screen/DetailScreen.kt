package com.debk007.template.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.debk007.template.model.Anime
import com.debk007.template.presentation.common.EpisodeItem
import com.debk007.template.presentation.common.RatingRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    anime: Anime,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // 1. Hero Header Section
            item {
                Box(modifier = Modifier.height(400.dp)) {
                    AsyncImage(
                        model = anime.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay to make text readable
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 300f
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = anime.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RatingRow(anime.rating)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "• ${anime.episodes.size} Episodes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // 2. Synopsis Section
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Synopsis",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = anime.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                    )
                    Spacer(Modifier.height(24.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Episodes",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // 3. Episodes List
            items(anime.episodes) { episode ->
                EpisodeItem(episode)
            }
        }
    }
}
