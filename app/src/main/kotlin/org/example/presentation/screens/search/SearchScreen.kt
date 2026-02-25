package com.openanimelib.presentation.screens.search
// presentation/screens/search/SearchScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val searchHistory by viewModel.searchHistory.collectAsState(initial = emptyList())
    var showFilters by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onSearch = { viewModel.onSearch(it) },
            active = false,
            onActiveChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search anime...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                Row {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, "Clear")
                        }
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Filled.FilterList, "Filters",
                            tint = if (showFilters) AccentPrimary
                                   else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        ) {}

        // Filters
        AnimatedVisibility(visible = showFilters) {
            SearchFilters(
                onGenreSelected = { viewModel.onGenreSelected(it) },
                onYearSelected = { viewModel.onYearSelected(it) },
                onFormatSelected = { viewModel.onFormatSelected(it) },
                onSortChanged = { viewModel.onSortChanged(it) }
            )
        }

        // Content
        if (searchQuery.isBlank()) {
            // Show search history
            SearchHistoryView(
                history = searchHistory,
                onHistoryClick = { viewModel.onSearch(it) },
                onClearHistory = { viewModel.clearHistory() }
            )
        } else {
            // Show results
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = searchResults.itemCount,
                    key = { searchResults[it]?.id ?: it }
                ) { index ->
                    searchResults[index]?.let { anime ->
                        AnimeCard(
                            anime = anime,
                            onClick = {
                                navController.navigate(
                                    Screen.AnimeDetail.createRoute(anime.id)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Loading indicator
                if (searchResults.loadState.append is LoadState.Loading) {
                    item(span = { GridItemSpan(3) }) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchFilters(
    onGenreSelected: (String?) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onFormatSelected: (String?) -> Unit,
    onSortChanged: (String) -> Unit
) {
    val genres = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Horror", "Mecha", "Music", "Mystery", "Psychological",
        "Romance", "Sci-Fi", "Slice of Life", "Sports",
        "Supernatural", "Thriller"
    )

    val formats = listOf("TV", "MOVIE", "OVA", "ONA", "SPECIAL")
    val years = (2024 downTo 1980).toList()
    val sortOptions = listOf(
        "POPULARITY_DESC" to "Popular",
        "SCORE_DESC" to "Highest Rated",
        "TRENDING_DESC" to "Trending",
        "START_DATE_DESC" to "Newest"
    )

    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedFormat by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Genre chips
        Text("Genre", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(genres) { genre ->
                FilterChip(
                    selected = selectedGenre == genre,
                    onClick = {
                        selectedGenre = if (selectedGenre == genre) null else genre
                        onGenreSelected(selectedGenre)
                    },
                    label = { Text(genre, fontSize = 12.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Format chips
        Text("Format", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            formats.forEach { format ->
                FilterChip(
                    selected = selectedFormat == format,
                    onClick = {
                        selectedFormat = if (selectedFormat == format) null else format
                        onFormatSelected(selectedFormat)
                    },
                    label = { Text(format, fontSize = 12.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
    }
}