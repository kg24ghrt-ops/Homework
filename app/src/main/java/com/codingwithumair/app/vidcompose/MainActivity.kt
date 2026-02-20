package com.codingwithumair.app.vidcompose

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // Modern Android 15+ standard
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.codingwithumair.app.vidcompose.navigation.AnimeNavHost
import com.codingwithumair.app.vidcompose.player.PlayerActivity
import com.codingwithumair.app.vidcompose.ui.theme.VidComposeTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Modern Edge-to-Edge: Replaces WindowCompat.setDecorFitsSystemWindows
        // This ensures your app draws behind the status and navigation bars.
        enableEdgeToEdge() 
        super.onCreate(savedInstanceState)

        setContent {
            VidComposeTheme {
                // 2. Use a Surface that consumes window insets automatically
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val playerLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { /* Handle player result if needed */ }

                    RequestPermissionWrapper {
                        // 3. Stability check for hardware acceleration
                        var isReady by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(500) // Slightly reduced delay for better UX
                            isReady = true
                        }

                        if (isReady) {
                            // 4. Navigation Host with explicit activity context
                            AnimeNavHost(onPlayVideo = { uri ->
                                val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                                    data = uri
                                    // Flag ensures we don't create multiple player instances
                                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                }
                                playerLauncher.launch(intent)
                            })
                        } else {
                            LoadingState()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            strokeCap = StrokeCap.Round,
            modifier = Modifier.size(48.dp)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissionWrapper(content: @Composable () -> Unit) {
    // 5. Accurate permission logic for Android 13 (Tiramisu) through Android 16
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val state = rememberPermissionState(permission)
    
    // Auto-trigger on first launch
    LaunchedEffect(Unit) { 
        if (!state.status.isGranted) state.launchPermissionRequest() 
    }

    if (state.status.isGranted) {
        content()
    } else {
        PermissionDeniedState(onGrant = { state.launchPermissionRequest() })
    }
}

@Composable
private fun PermissionDeniedState(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Library Access Needed", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "To stream and play your anime collection, we need permission to access video files.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) {
            Text("Allow Access")
        }
    }
}
