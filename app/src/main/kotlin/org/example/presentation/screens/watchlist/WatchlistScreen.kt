package com.openanimelib.presentation.screens.watchlist
// presentation/screens/watchlist/WatchlistScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    navController: NavController,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "All" to "📋",
        "Watching" to "👀",
        "Completed" to "✅",
        "Planned" to "📝",
        "On Hold" to "⏸️",
        "Dropped" to "❌"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("My Watchlist", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AccentPrimary,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, (title, emoji) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            val status = when (index) {
                                0 -> null
                                1 -> WatchStatus.WATCHING
                                2 -> WatchStatus.COMPLETED
                                3 -> WatchStatus.PLANNED
                                4 -> WatchStatus.ON_HOLD
                                5 -> WatchStatus.DROPPED
                                else -> null
                            }
                            viewModel.filterByStatus(status)
                        },
                        text = { Text("$emoji $title") }
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard("Total", uiState.totalCount.toString())
                StatCard("Watching", uiState.watchingCount.toString())
                StatCard("Completed", uiState.completedCount.toString())
            }

            // Watchlist items
            if (uiState.items.isEmpty()) {
                EmptyStateView(
                    emoji = "📭",
                    title = "No anime here yet!",
                    subtitle = "Start adding anime to your watchlist",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.items,
                        key = { it.anime.id }
                    ) { item ->
                        WatchlistItemCard(
                            item = item,
                            onClick = {
                                navController.navigate(
                                    Screen.AnimeDetail.createRoute(item.anime.id)
                                )
                            },
                            onStatusChange = { status ->
                                viewModel.updateStatus(item.anime.id, status)
                            },
                            onEpisodeIncrement = {
                                viewModel.incrementEpisode(item.anime.id, item.currentEpisode)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WatchlistItemCard(
    item: WatchlistItem,
    onClick: () -> Unit,
    onStatusChange: (WatchStatus) -> Unit,
    onEpisodeIncrement: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cover
            AsyncImage(
                model = item.anime.coverImage,
                contentDescription = item.anime.title.display,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(70.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(8.dp))
            )

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.anime.title.display,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Progress bar
                item.anime.episodes?.let { total ->
                    val progress = item.currentEpisode.toFloat() / total
                    Column {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = AccentPrimary,
                            trackColor = BorderColor
                        )
                        Text(
                            text = "${item.currentEpisode}/$total episodes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.anime.genres.take(2).forEach {
                        GenreChip(genre = it, compact = true)
                    }
                }
            }

            // Quick action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Increment episode button
                IconButton(
                    onClick = onEpisodeIncrement,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        "Next Episode",
                        tint = AccentPrimary
                    )
                }

                item.rating?.let { rating ->
                    Text(
                        text = "⭐${String.format("%.1f", rating)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}.၊