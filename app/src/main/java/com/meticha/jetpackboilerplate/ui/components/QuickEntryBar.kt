package com.meticha.jetpackboilerplate.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meticha.jetpackboilerplate.ui.BearingMode
import com.meticha.jetpackboilerplate.ui.VectorViewModel
import com.meticha.jetpackboilerplate.ui.theme.CommandCyan
import com.meticha.jetpackboilerplate.ui.theme.RadarGreen

@Composable
fun QuickEntryBar(
    viewModel: VectorViewModel, // Pass the ViewModel to control BearingMode
    onReset: () -> Unit
) {
    var magnitude by remember { mutableStateOf("") }
    var bearing by remember { mutableStateOf("") }

    Surface(
        color = Color(0xFF12171D), // Deep tactical background
        tonalElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .navigationBarsPadding()
                .fillMaxWidth()
        ) {
            // 1. THE SHORTCUT ROW: Fast Quadrant Entry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BearingMode.entries.forEach { mode ->
                    val isSelected = viewModel.currentBearingMode == mode
                    InputChip(
                        selected = isSelected,
                        onClick = { viewModel.setBearingMode(mode) },
                        label = { 
                            Text(
                                mode.name, 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = RadarGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1A1F26),
                            labelColor = CommandCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. INPUT ROW: Distance, Bearing, and Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Magnitude Input
                OutlinedTextField(
                    value = magnitude,
                    onValueChange = { if (it.length <= 6) magnitude = it },
                    label = { Text("DISTANCE", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CommandCyan,
                        unfocusedBorderColor = Color.Gray.copy(0.4f)
                    )
                )

                // Bearing Input
                OutlinedTextField(
                    value = bearing,
                    onValueChange = { if (it.length <= 3) bearing = it },
                    label = { Text("ANGLE°", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("0-360", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (viewModel.currentBearingMode == BearingMode.DIRECT) CommandCyan else RadarGreen,
                        unfocusedBorderColor = Color.Gray.copy(0.4f)
                    )
                )

                // Deploy Button
                Button(
                    onClick = {
                        val mag = magnitude.toDoubleOrNull()
                        val brng = bearing.toDoubleOrNull()
                        if (mag != null && brng != null) {
                            viewModel.addVector(mag, brng)
                            magnitude = ""
                            bearing = ""
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = CommandCyan),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                }

                // Reset Button
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.Gray)
                }
            }
        }
    }
}
