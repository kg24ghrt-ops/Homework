package com.codingwithumair.app.vidcompose

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.codingwithumair.app.vidcompose.navigation.AnimeNavHost
import com.codingwithumair.app.vidcompose.player.PlayerActivity
import com.codingwithumair.app.vidcompose.ui.theme.VidComposeTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
// Add this import to fix "Unresolved reference: dp"
import androidx.compose.ui.unit.dp


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Enable Edge-to-Edge for that modern Material 3 look
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            VidComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 2. Setup the bridge to your existing PlayerActivity
                    val playerLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) {}

                    RequestPermissionWrapper {
                        // 3. Keep your OpenGL stability fix
                        var isReady by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(800) // Grace period for hardware buffers
                            isReady = true
                        }

                        if (isReady) {
                            // 4. Inject the new Anime Discovery Navigation
                            AnimeNavHost(onPlayVideo = { uri ->
                                val intent = Intent(this, PlayerActivity::class.java).apply {
                                    data = uri
                                }
                                playerLauncher.launch(intent)
                            })
                        } else {
                            // Professional loading state while the UI stabilizes
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissionWrapper(content: @Composable () -> Unit) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
        Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
    
    val state = rememberPermissionState(permission)
    
    // Auto-launch request if not granted
    LaunchedEffect(Unit) { 
        if (!state.status.isGranted) state.launchPermissionRequest() 
    }

    if (state.status.isGranted) {
        content()
    } else {
        // Modern M3 Empty State for permissions
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Storage Access Required", 
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "To play local anime files, we need storage access.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = { state.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}
