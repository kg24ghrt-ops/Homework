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
    // 1. REACTIVE STATES (Synced with the new ViewModel)
    val path by viewModel.pathPoints
    val displayResult by viewModel.displayResultant 
    val currentUnit = viewModel.selectedUnit

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VECTORNAV // AUGUST", style = MaterialTheme.typography.labelSmall)
                        
                        // THE DYNAMIC HUD: Now uses the ViewModel's measurement engine
                        Text(
                            text = displayResult,
                            style = MaterialTheme.typography.headlineSmall,
                            color = RadarGreen
                        )
                    }
                },
                // ACTION: Added Unit Selection right in the top bar for pro access
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MeasurementUnit.entries.forEach { unit ->
                            InputChip(
                                selected = currentUnit == unit,
                                onClick = { viewModel.setUnit(unit) },
                                label = { Text(unit.suffix) },
                                modifier = Modifier.padding(horizontal = 2.dp),
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = RadarGreen,
                                    labelColor = Color.White
                                )
                            )
                        }
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TacticalViewport(
                path = path,
                // Passing the unit to the viewport allows for scale-aware drawing
                selectedUnit = currentUnit, 
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
