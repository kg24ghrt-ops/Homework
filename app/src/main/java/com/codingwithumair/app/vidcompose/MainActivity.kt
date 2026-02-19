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
import com.codingwithumair.app.vidcompose.player.PlayerActivity
import com.codingwithumair.app.vidcompose.ui.theme.VidComposeTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            VidComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val playerLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) {}

                    RequestPermissionWrapper {
                        // FIX for OpenGL swap error: Wait for surface to stabilize
                        var isReady by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(800) // Grace period for hardware buffers
                            isReady = true
                        }

                        if (isReady) {
                            AnimeNavHost(onPlayVideo = { uri ->
                                val intent = Intent(this, PlayerActivity::class.java).apply {
                                    data = uri
                                }
                                playerLauncher.launch(intent)
                            })
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
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
    LaunchedEffect(Unit) { if (!state.status.isGranted) state.launchPermissionRequest() }

    if (state.status.isGranted) content() else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { state.launchPermissionRequest() }) { Text("Grant Storage Access") }
        }
    }
}
