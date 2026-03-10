package com.meticha.jetpackboilerplate.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue 
import androidx.compose.runtime.setValue 
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meticha.jetpackboilerplate.ui.components.QuickEntryBar
import com.meticha.jetpackboilerplate.ui.components.TacticalViewport
import com.meticha.jetpackboilerplate.ui.theme.CommandCyan
import com.meticha.jetpackboilerplate.ui.theme.RadarGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommanderDashboard(viewModel: VectorViewModel) {
    val path by viewModel.pathPoints
    val displayResult by viewModel.displayResultant 
    val currentUnit = viewModel.selectedUnit 

    Scaffold(
        containerColor = Color(0xFF0A0E12),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VECTORNAV // V4.0", 
                            style = MaterialTheme.typography.labelSmall,
                            color = CommandCyan.copy(alpha = 0.6f)
                        )
                        
                        AnimatedContent(targetState = displayResult, label = "ResultUpdate") { result ->
                            Text(
                                text = result,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = RadarGreen
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent, 
                    titleContentColor = CommandCyan
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp, 
                color = Color(0xFF12171D),
                shadowElevation = 10.dp
            ) {
                // FIXED LINE: Passing the viewModel directly
                QuickEntryBar(
                    viewModel = viewModel,
                    onReset = { viewModel.clearSystem() }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TacticalViewport(
                path = path,
                selectedUnit = currentUnit, 
                modifier = Modifier.fillMaxSize()
            )

            // Top Right Overlay Controls
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Textbook Mode Switch
                Surface(
                    color = Color(0xFF1A1F26).copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (viewModel.isTextbookMode) "TEXTBOOK" else "PRECISE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (viewModel.isTextbookMode) RadarGreen else Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = viewModel.isTextbookMode,
                            onCheckedChange = { viewModel.toggleTextbookMode() },
                            modifier = Modifier.size(width = 40.dp, height = 24.dp)
                        )
                    }
                }

                // Unit Selector
                MeasurementUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = currentUnit == unit,
                        onClick = { viewModel.setUnit(unit) },
                        label = { Text(unit.suffix.uppercase()) }
                    )
                }
            }
        }
    }
}
