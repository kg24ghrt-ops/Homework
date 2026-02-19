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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.google.accompanist.permissions.shouldShowRationale

class MainActivity : ComponentActivity() {

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Edge-to-edge and Cutout setup
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
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
                        // Launch the Navigation Host from NavGraph.kt
                        AnimeNavHost(onPlayVideo = { uri ->
                            try {
                                val playerIntent = Intent(this, PlayerActivity::class.java).apply {
                                    data = uri
                                }
                                playerActivityLauncher.launch(playerIntent)
                            } catch (e: Exception) {
                                // Prevent crash if PlayerActivity isn't registered correctly
                                e.printStackTrace()
                            }
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissionAndDisplayContent(appContent: @Composable () -> Unit) {
    // Select correct permission based on Android Version
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val permissionState = rememberPermissionState(permission)

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted && !permissionState.status.shouldShowRationale) {
            permissionState.launchPermissionRequest()
        }
    }

    // Use a 'when' block to prevent state-flicker crashes
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            permissionState.status.isGranted -> {
                appContent()
            }
            permissionState.status.shouldShowRationale -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Storage access is needed to play videos.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { permissionState.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }
            else -> {
                // Initial state or request in progress
                CircularProgressIndicator()
            }
        }
    }
}
