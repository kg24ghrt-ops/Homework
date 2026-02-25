// presentation/screens/detail/AnimeDetailScreen.kt
package com.openanimelib.presentation.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.openanimelib.domain.model.*
import com.openanimelib.presentation.common.*
import com.openanimelib.presentation.navigation.Screen
import com.openanimelib.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    navController: NavController,
    viewModel: AnimeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Watchlist dialog
    if (uiState.showWatchlistDialog) {
        WatchlistStatusDialog(
            currentStatus = uiState.watchlistStatus,
            onStatusSelected = { viewModel.onAddToWatchlist(it) },
            onDismiss = { viewModel.dismissWatchlistDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onToggleFavorite() }) {
                        Icon(
                            if (uiState.isFavorite) Icons.Filled.Favorite
                            else Icons.Outlined.FavoriteBorder,
                            "Favorite",
                            tint = if (uiState.isFavorite) AccentPrimary else Color.White
                        )
                    }
                    IconButton(onClick = {
                        val anime = uiState.detail?.anime ?: return@IconButton
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT,
                                "Check out ${anime.title.display} on OpenAnimeLib!")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share"))
                    }) {
                        Icon(Icons.Filled.Share, "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->

        when {
            uiState.isLoading -> {
                DetailShimmerLoading(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null -> {
                ErrorView(
                    message = uiState.error!!,
                    onRetry = { /* reload */ },
                    modifier = Modifier.padding(paddingValues).fillMaxSize()
                )
            }
            uiState.detail != null -> {
                val detail = uiState.detail!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Banner + Cover Image Header
                    DetailHeader(detail = detail)

                    // Action Buttons
                    ActionButtonsRow(
                        isInWatchlist = uiState.isInWatchlist,
                        watchlistStatus = uiState.watchlistStatus,
                        onWatchlistClick = {
                            if (uiState.isInWatchlist) viewModel.onRemoveFromWatchlist()
                            else viewModel.showWatchlistDialog()
                        },
                        onTrailerClick = {
                            detail.trailer?.let { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        hasTrailer = detail.trailer != null
                    )

                    // Info Section
                    InfoSection(detail = detail)

                    // Synopsis
                    detail.synopsis?.let { synopsis ->
                        SynopsisSection(synopsis = synopsis)
                    }

                    // 🎬 WHERE TO WATCH FREE
                    if (detail.streamingSources.isNotEmpty()) {
                        SourcesSection(
                            sources = detail.streamingSources,
                            onSourceClick = { source ->
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                                )
                            }
                        )
                    }

                    // Characters
                    if (detail.characters.isNotEmpty()) {
                        CharactersSection(characters = detail.characters)
                    }

                    // Episode Progress (if in watchlist)
                    if (uiState.isInWatchlist && detail.anime.episodes != null) {
                        EpisodeProgressSection(
                            totalEpisodes = detail.anime.episodes!!,
                            currentEpisode = uiState.currentEpisode,
                            onEpisodeUpdate = { viewModel.onUpdateEpisode(it) }
                        )
                    }

                    // Related Anime
                    if (detail.relations.isNotEmpty()) {
                        RelatedSection(
                            relatedAnime = detail.relations,
                            onAnimeClick = { id ->
                                navController.navigate(Screen.AnimeDetail.createRoute(id))
                            }
                        )
                    }

                    // Recommendations
                    if (detail.recommendations.isNotEmpty()) {
                        RecommendationsSection(
                            recommendations = detail.recommendations,
                            onAnimeClick = { id ->
                                navController.navigate(Screen.AnimeDetail.createRoute(id))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun DetailHeader(detail: AnimeDetail) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Banner image
        AsyncImage(
            model = detail.anime.bannerImage ?: detail.anime.coverImage,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 150f
                    )
                )
        )

        // Cover image + Title
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cover image
            AsyncImage(
                model = detail.anime.coverImage,
                contentDescription = detail.anime.title.display,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        2.dp,
                        AccentPrimary,
                        RoundedCornerShape(12.dp)
                    )
            )

            // Title + quick info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = detail.anime.title.display,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                detail.anime.title.japanese?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    detail.anime.rating?.let { rating ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = StarYellow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                String.format("%.1f", rating),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text("•", color = Color.White.copy(alpha = 0.5f))

                    Text(
                        text = detail.anime.type.name,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )

                    detail.anime.episodes?.let { eps ->
                        Text("•", color = Color.White.copy(alpha = 0.5f))
                        Text(
                            text = "$eps eps",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SourcesSection(
    sources: List<StreamingSource>,
    onSourceClick: (StreamingSource) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "🎬 Watch Free On",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        sources.forEach { source ->
            SourceButton(
                source = source,
                onClick = { onSourceClick(source) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SourceButton(
    source: StreamingSource,
    onClick: () -> Unit
) {
    val platformColor = when (source.platform) {
        StreamingPlatform.CRUNCHYROLL -> CrunchyrollOrange
        StreamingPlatform.YOUTUBE_MUSE,
        StreamingPlatform.YOUTUBE_ANIONE,
        StreamingPlatform.YOUTUBE_OFFICIAL -> YouTubeRed
        StreamingPlatform.TUBI -> TubiRed
        StreamingPlatform.RETROCRUSH -> RetroCrushPink
        StreamingPlatform.PLUTO_TV -> PlutoBlue
        else -> AccentPrimary
    }

    val platformEmoji = when (source.platform) {
        StreamingPlatform.CRUNCHYROLL -> "🍊"
        StreamingPlatform.YOUTUBE_MUSE,
        StreamingPlatform.YOUTUBE_ANIONE,
        StreamingPlatform.YOUTUBE_OFFICIAL -> "▶️"
        StreamingPlatform.TUBI -> "📺"
        StreamingPlatform.RETROCRUSH -> "🎌"
        StreamingPlatform.PLUTO_TV -> "📡"
        StreamingPlatform.POKEMON_TV -> "⚡"
        StreamingPlatform.VIZ -> "📖"
        StreamingPlatform.ARCHIVE_ORG -> "🏛️"
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, platformColor.copy(alpha = 0.3f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = platformColor.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Platform color indicator
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(platformColor)
                )

                Text(text = platformEmoji, fontSize = 28.sp)

                Column {
                    Text(
                        text = source.platform.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = source.quality,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (source.hasAds) "With Ads" else "No Ads",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (source.hasAds) WarningYellow else SuccessGreen
                        )
                        if (source.isSub) Text("SUB", style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary, fontWeight = FontWeight.Bold)
                        if (source.isDub) Text("DUB", style = MaterialTheme.typography.labelSmall,
                            color = AccentSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Icon(
                Icons.Filled.OpenInNew,
                contentDescription = "Open",
                tint = platformColor
            )
        }
    }
}

@Composable
fun SynopsisSection(synopsis: String) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "📝 Synopsis",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = synopsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )

        TextButton(onClick = { expanded = !expanded }) {
            Text(
                if (expanded) "Show Less" else "Read More",
                color = AccentPrimary
            )
        }
    }
}

@Composable
fun CharactersSection(characters: List<Character>) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp)) {
        Text(
            text = "👥 Characters",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(characters) { character ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(80.dp)
                ) {
                    AsyncImage(
                        model = character.image,
                        contentDescription = character.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, AccentPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeProgressSection(
    totalEpisodes: Int,
    currentEpisode: Int,
    onEpisodeUpdate: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "📊 Your Progress",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        val progress = if (totalEpisodes > 0) currentEpisode.toFloat() / totalEpisodes else 0f

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = AccentPrimary,
            trackColor = BorderColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$currentEpisode / $totalEpisodes episodes",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { if (currentEpisode > 0) onEpisodeUpdate(currentEpisode - 1) }
                ) {
                    Icon(Icons.Filled.Remove, "Decrease")
                }

                IconButton(
                    onClick = {
                        if (currentEpisode < totalEpisodes) onEpisodeUpdate(currentEpisode + 1)
                    }
                ) {
                    Icon(Icons.Filled.Add, "Increase")
                }
            }
        }
    }
}

@Composable
fun WatchlistStatusDialog(
    currentStatus: WatchStatus?,
    onStatusSelected: (WatchStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Watchlist") },
        text = {
            Column {
                WatchStatus.entries.forEach { status ->
                    val emoji = when (status) {
                        WatchStatus.WATCHING -> "👀"
                        WatchStatus.COMPLETED -> "✅"
                        WatchStatus.PLANNED -> "📋"
                        WatchStatus.ON_HOLD -> "⏸️"
                        WatchStatus.DROPPED -> "❌"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onStatusSelected(status) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = status.name.replace("_", " "),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (status == currentStatus) FontWeight.Bold
                                        else FontWeight.Normal,
                            color = if (status == currentStatus) AccentPrimary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}