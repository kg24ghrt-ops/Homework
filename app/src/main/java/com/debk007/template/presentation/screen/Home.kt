package com.debk007.template.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debk007.template.model.Anime
import com.debk007.template.presentation.common.AnimeCard
import com.debk007.template.util.ApiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    animeState: ApiState<List<Anime>>,
    onAnimeClick: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AnimeGo") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (animeState) {
                is ApiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(48.dp)
                    )
                }

                is ApiState.Error -> {
                    Text(
                        text = "Error: ${animeState.errorMsg}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }

                is ApiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = animeState.data, key = { it.id }) { anime ->
                            AnimeCard(anime = anime, onClick = onAnimeClick)
                        }
                    }
                }
                else -> {} // Handle Idle state if necessary
            }
        }
    }
}
