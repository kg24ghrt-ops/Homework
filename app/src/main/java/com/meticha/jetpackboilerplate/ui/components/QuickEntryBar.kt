package com.meticha.jetpackboilerplate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meticha.jetpackboilerplate.ui.BearingMode
import com.meticha.jetpackboilerplate.ui.VectorViewModel
import com.meticha.jetpackboilerplate.ui.theme.CommandCyan
import com.meticha.jetpackboilerplate.ui.theme.RadarGreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CornerSize


@Composable
fun QuickEntryBar(
    viewModel: VectorViewModel,
    onReset: () -> Unit
) {
    var magnitude by remember { mutableStateOf("") }
    var bearing by remember { mutableStateOf("") }
    var showCompass by remember { mutableStateOf(false) } // Day 2 Mission: Compass Dialog

    // --- INTEGRITY COLORS ---
    val textbookPink = Color(0xFFFF79C6)
    val activeAccent = if (viewModel.isTextbookMode) textbookPink else CommandCyan

    Surface(
        color = Color(0xFF0D1117), // Deeper tactical black
        tonalElevation = 16.dp,
        border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.2f)),
        shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
                .fillMaxWidth()
        ) {
            // 1. HEADER & MODE INDICATOR
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (viewModel.isTextbookMode) "TEXTBOOK ENTRY // ACTIVE" else "MANUAL ENTRY // PRECISE",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeAccent.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Undo",
                    modifier = Modifier.size(18.dp).clickable { viewModel.undoLast() },
                    tint = Color.Gray
                )
            }

            // 2. QUADRANT SNAP ROW (Optimized for Thumb)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BearingMode.entries.forEach { mode ->
                    val isSelected = viewModel.currentBearingMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (isSelected) activeAccent else Color(0xFF1A1F26))
                            .border(1.dp, if (isSelected) Color.White.copy(0.3f) else Color.Transparent)
                            .clickable { viewModel.setBearingMode(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.Black else Color.White.copy(0.7f)
                        )
                    }
                }
            }

            // 3. MAIN INPUT ROW
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Magnitude
                OutlinedTextField(
                    value = magnitude,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) magnitude = it },
                    label = { Text("MAGNITUDE", fontSize = 9.sp) },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeAccent,
                        unfocusedBorderColor = Color.White.copy(0.1f),
                        focusedLabelColor = activeAccent
                    )
                )

                // Angle with Compass Trigger
                OutlinedTextField(
                    value = bearing,
                    onValueChange = { if (it.all { char -> char.isDigit() }) bearing = it },
                    label = { Text("BEARING°", fontSize = 9.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { /* TODO: Trigger Compass Dialog */ }) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = activeAccent, modifier = Modifier.size(20.dp))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeAccent,
                        unfocusedBorderColor = Color.White.copy(0.1f),
                        focusedLabelColor = activeAccent
                    )
                )

                // DEPLOY BUTTON (The "Action" Glow)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            Brush.verticalGradient(
                                listOf(activeAccent, activeAccent.copy(alpha = 0.7f))
                            )
                        )
                        .clickable {
                            val mag = magnitude.toDoubleOrNull()
                            val brng = bearing.toDoubleOrNull()
                            if (mag != null && brng != null) {
                                viewModel.addVector(mag, brng)
                                magnitude = ""; bearing = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
