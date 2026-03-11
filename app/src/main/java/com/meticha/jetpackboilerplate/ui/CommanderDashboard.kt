package com.meticha.jetpackboilerplate.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
        containerColor = Color(0xFF080B0F), // Slightly deeper black
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "VECTORNAV // TACTICAL INTERFACE",
                                style = MaterialTheme.typography.labelSmall,
                                color = CommandCyan.copy(alpha = 0.5f),
                                letterSpacing = 2.sp
                            )
                            AnimatedContent(
                                targetState = displayResult,
                                transitionSpec = { fadeIn() with fadeOut() }
                            ) { result ->
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = if (viewModel.isTextbookMode) Color(0xFFFF79C6) else RadarGreen
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                // Tactical scanline separator
                LinearProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxWidth().height(1.dp),
                    color = CommandCyan.copy(alpha = 0.2f),
                    trackColor = Color.Transparent
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 12.dp,
                color = Color(0xFF101419),
                border = BoxWithConstraints { 
                    // Add a top border for that "Panel" feel
                    Modifier.background(CommandCyan.copy(alpha = 0.1f))
                }.let { null } 
            ) {
                QuickEntryBar(viewModel = viewModel, onReset = { viewModel.clearSystem() })
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            // 1. TACTICAL GRID BACKGROUND
            TacticalGrid()

            // 2. MAIN VIEWPORT
            TacticalViewport(
                path = path,
                selectedUnit = currentUnit,
                modifier = Modifier.fillMaxSize()
            )

            // 3. RIGHT PANEL CONTROLS (Floating Glass)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .width(100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Precision Toggle
                ModeToggle(
                    isTextbook = viewModel.isTextbookMode,
                    onToggle = { viewModel.toggleTextbookMode() }
                )

                // Unit Selector (Vertical Stack)
                Surface(
                    color = Color(0xFF1A1F26).copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CommandCyan.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        MeasurementUnit.entries.forEach { unit ->
                            val isSelected = currentUnit == unit
                            TextButton(
                                onClick = { viewModel.setUnit(unit) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (isSelected) CommandCyan else Color.Gray
                                )
                            ) {
                                Text(unit.suffix.uppercase(), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalGrid() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSpacing = 40.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
        
        // Vertical lines
        for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
            drawLine(
                color = CommandCyan.copy(alpha = 0.05f),
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), size.height),
                pathEffect = pathEffect
            )
        }
        // Horizontal lines
        for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
            drawLine(
                color = CommandCyan.copy(alpha = 0.05f),
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                pathEffect = pathEffect
            )
        }
    }
}

@Composable
fun ModeToggle(isTextbook: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        color = if (isTextbook) Color(0xFFFF79C6).copy(alpha = 0.15f) else CommandCyan.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isTextbook) Color(0xFFFF79C6).copy(alpha = 0.5f) else CommandCyan.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isTextbook) "TEXTBOOK" else "PRECISE",
                style = MaterialTheme.typography.labelSmall,
                color = if (isTextbook) Color(0xFFFF79C6) else CommandCyan
            )
            Text(
                text = if (isTextbook) "800m SNAP" else "RAW DATA",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
