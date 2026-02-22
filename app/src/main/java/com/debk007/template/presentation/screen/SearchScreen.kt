package com.debk007.template.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debk007.template.model.Anime
import com.debk007.template.presentation.common.AnimeCard
import com.debk007.template.util.ApiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    animeState: ApiState<List<Anime>>, // Matches the ViewModel state name
    onQueryChange: (String) -> Unit,
    onAnimeClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onQueryChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search for anime...") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (animeState) {
                is ApiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ApiState.Success -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(animeState.data) { anime ->
                            AnimeCard(anime = anime, onClick = onAnimeClick)
                        }
                    }
                }
                is ApiState.Error -> {
                    Text(
                        text = animeState.errorMsg,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ApiState.Idle -> {
                    Text(
                        text = "Start typing to search!",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
