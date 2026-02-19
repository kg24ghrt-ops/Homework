package com.codingwithumair.app.vidcompose

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.codingwithumair.app.vidcompose.player.PlayerActivity
import com.codingwithumair.app.vidcompose.ui.theme.VidComposeTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

// 1. DATA MODELS (Moved inside to avoid redeclaration if files are deleted)
data class Anime(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String,
    val episodes: List<Episode>
)

data class Episode(
    val title: String,
    val videoUri: Uri
)

val sampleAnimeList = listOf(
    Anime(1, "Cyberpunk: Edgerunners", "A street kid tries to survive in a technology-obsessed city.", "https://picsum.photos/seed/anime1/400/600", listOf(Episode("Episode 1", Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")))),
    Anime(2, "Chainsaw Man", "A teenage boy living with a Chainsaw Devil.", "https://picsum.photos/seed/anime2/400/600", listOf(Episode("Episode 1", Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"))))
)

class MainActivity : ComponentActivity() {

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        super.onCreate(savedInstanceState)

        setContent {
            VidComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    tonalElevation = 8.dp
                ) {
                    val playerActivityLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult(),
                        onResult = {}
                    )

                    RequestPermissionAndDisplayContent {
                        AnimeAppNavigation(onPlayVideo = { uri ->
                            val playerIntent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                                data = uri
                            }
                            playerActivityLauncher.launch(playerIntent)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeAppNavigation(onPlayVideo: (Uri) -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onAnimeClick = { id -> navController.navigate("detail/$id") })
        }
        composable(
            "detail/{animeId}",
            arguments = listOf(navArgument("animeId") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("animeId")
            val anime = sampleAnimeList.find { it.id == id }
            anime?.let {
                AnimeDetailScreen(
                    anime = it,
                    onBack = { navController.popBackStack() },
                    onPlayEpisode = { uri -> onPlayVideo(uri) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onAnimeClick: (Int) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("AnimeApp", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(sampleAnimeList) { anime ->
                AnimeCard(anime) { onAnimeClick(anime.id) }
            }
        }
    }
}

@Composable
fun AnimeCard(anime: Anime, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.padding(16.dp, 8.dp).fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp).height(120.dp)) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = null,
                modifier = Modifier.width(90.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(anime.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(anime.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                SuggestionChip(onClick = {}, label = { Text("${anime.episodes.size} Eps") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(anime: Anime, onBack: () -> Unit, onPlayEpisode: (Uri) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(anime.title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AsyncImage(model = anime.posterUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(250.dp), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.titleLarge)
                Text(anime.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Text("Episodes", style = MaterialTheme.typography.titleLarge)
                anime.episodes.forEach { episode ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = { onPlayEpisode(episode.videoUri) }) {
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestPermissionAndDisplayContent(appContent: @Composable () -> Unit) {
    val readVideoPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        rememberPermissionState(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    LaunchedEffect(Unit) { if (!readVideoPermissionState.status.isGranted) readVideoPermissionState.launchPermissionRequest() }

    if (readVideoPermissionState.status.isGranted) {
        appContent()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Permissions required to browse local media.", color = MaterialTheme.colorScheme.error)
        }
    }
}
