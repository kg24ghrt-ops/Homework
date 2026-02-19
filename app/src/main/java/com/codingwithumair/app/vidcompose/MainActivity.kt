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
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Set up edge-to-edge and cutout handling
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        super.onCreate(savedInstanceState)

        setContent {
            VidComposeTheme {
                // 2. Surface with explicit color satisfies OpenGL swap behavior errors
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val playerActivityLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult(),
                        onResult = {}
                    )

                    RequestPermissionAndDisplayContent {
                        // 3. Deferred initialization to let BLASTBufferQueue stabilize 
                        // after the permission dialog closes
                        var isSurfaceReady by remember { mutableStateOf(false) }
                        
                        LaunchedEffect(Unit) {
                            delay(600) // Slightly increased to 600ms for extra safety
                            isSurfaceReady = true
                        }

                        if (isSurfaceReady) {
                            AnimeNavHost(onPlayVideo = { uri ->
                                try {
                                    val playerIntent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                                        data = uri
                                        // Essential flag for clean activity transitions
                                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    }
                                    playerActivityLauncher.launch(playerIntent)
                                } catch (e: Exception) {
                                    // Log the error to prevent silent failures
                                    e.printStackTrace()
                                }
                            })
                        } else {
                            // Show a lightweight loader during the 600ms grace period
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeWidth = 3.dp)
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
fun RequestPermissionAndDisplayContent(appContent: @Composable () -> Unit) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val permissionState = rememberPermissionState(permission)

    // Trigger request on first launch if not granted
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    if (permissionState.status.isGranted) {
        appContent()
    } else {
        // Clean UI for the permission state
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Video Access Required", 
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { permissionState.launchPermissionRequest() }) {
                    Text("Allow Access")
                }
            }
        }
    }
}
