package com.meticha.jetpackboilerplate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    val path = viewModel.pathPoints.value
    val solveNode = viewModel.resultant.value

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("VECTORNAV // COMMANDER", style = MaterialTheme.typography.labelMedium)
                        // THE REAL-TIME HUD: Shows final displacement instantly
                        Text(
                            "RESULTANT: ${"%.2f".format(solveNode.magnitude)}m @ ${"%.1f".format(solveNode.bearing)}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = RadarGreen
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
            // FEEDING THE ENGINE: Buttons talk directly to the ViewModel
            QuickEntryBar(
                onAdd = { mag, brng -> viewModel.addVector(mag, brng) },
                onReset = { viewModel.clearSystem() }
            )
        }
    ) { innerPadding ->
        // THE VIEWPORT: Hardware-accelerated drawing area
        TacticalViewport(
            path = path,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
