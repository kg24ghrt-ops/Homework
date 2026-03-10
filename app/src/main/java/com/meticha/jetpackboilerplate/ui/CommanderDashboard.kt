package com.meticha.jetpackboilerplate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meticha.jetpackboilerplate.ui.components.QuickEntryBar
import com.meticha.jetpackboilerplate.ui.components.TacticalViewport
import com.meticha.jetpackboilerplate.ui.theme.CommandCyan
import com.meticha.jetpackboilerplate.ui.theme.RadarGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommanderDashboard(viewModel: VectorViewModel) {
    // 1. Reactive States
    val path by viewModel.pathPoints
    val solveNode by viewModel.resultant
    
    // OPTIMIZATION: Precision State (False = Textbook/0.1, True = Ottoman/0.01)
    var highPrecisionMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VECTORNAV // V1.1", style = MaterialTheme.typography.labelSmall)
                        
                        // THE DYNAMIC HUD: Switches format based on the toggle
                        val distFormat = if (highPrecisionMode) "%.2f" else "%.1f"
                        val angleFormat = if (highPrecisionMode) "%.2f" else "%.0f"
                        
                        Text(
                            "R: ${distFormat.format(solveNode.magnitude)}m @ ${angleFormat.format(solveNode.bearing)}°",
                            style = MaterialTheme.typography.headlineSmall,
                            color = RadarGreen
                        )
                    }
                },
                actions = {
                    // THE PRECISION TOGGLE: Switch between Homework and Pro levels
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("OTTN", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Switch(
                            checked = highPrecisionMode,
                            onCheckedChange = { highPrecisionMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = RadarGreen)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0A0E12),
                    titleContentColor = CommandCyan
                )
            )
        },
        bottomBar = {
            QuickEntryBar(
                onAdd = { mag, brng -> viewModel.addVector(mag, brng) },
                onReset = { viewModel.clearSystem() }
            )
        }
    ) { innerPadding ->
        // OPTIMIZATION: Automatic Scaling
        // We wrap the Viewport in a Box with padding to ensure the path 
        // stays away from the screen edges.
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TacticalViewport(
                path = path,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
