package com.codingwithumair.app.vidcompose

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import com.codingwithumair.app.vidcompose.player.PlayerActivity
import com.codingwithumair.app.vidcompose.ui.theme.VidComposeTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        super.onCreate(savedInstanceState)

        setContent {
            VidComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
                    tonalElevation = 8.dp
                ) {
                    val playerActivityLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult(),
                        onResult = {}
                    )

                    RequestPermissionAndDisplayContent {
                        // Added a key to NavHost to force a clean state after permission is granted
                        key(true) {
                            AnimeNavHost(onPlayVideo = { uri ->
                                try {
                                    val playerIntent = Intent(this, PlayerActivity::class.java).apply {
                                        data = uri
                                        // Adding flags to ensure clean activity launch
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    playerActivityLauncher.launch(playerIntent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissionAndDisplayContent(appContent: @Composable () -> Unit) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val permissionState = rememberPermissionState(permission)

    // Request logic that won't trigger if already granted
    LaunchedEffect(permissionState.status.isGranted) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    if (permissionState.status.isGranted) {
        appContent()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Checking permissions...", style = MaterialTheme.typography.bodyMedium)
                // Fallback button if the auto-prompt fails
                Button(
                    onClick = { permissionState.launchPermissionRequest() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
