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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.meticha.jetpackboilerplate.ui.theme.CommandCyan
import com.meticha.jetpackboilerplate.ui.theme.RadarGreen

@Composable
fun QuickEntryBar(
    onAdd: (Double, Double) -> Unit,
    onReset: () -> Unit
) {
    var magnitude by remember { mutableStateOf("") }
    var bearing by remember { mutableStateOf("") }

    Surface(
        color = Color(0xFF1C2227), // PanelGrey
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Magnitude Input
            OutlinedTextField(
                value = magnitude,
                onValueChange = { magnitude = it },
                label = { Text("DIST", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CommandCyan)
            )

            // Bearing Input
            OutlinedTextField(
                value = bearing,
                onValueChange = { bearing = it },
                label = { Text("BRNG°", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CommandCyan)
            )

            // Deploy Vector Button
            IconButton(
                onClick = {
                    val mag = magnitude.toDoubleOrNull() ?: 0.0
                    val brng = bearing.toDoubleOrNull() ?: 0.0
                    if (mag > 0) {
                        onAdd(mag, brng)
                        magnitude = "" // Clear for next entry
                        bearing = ""
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = CommandCyan)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Deploy", tint = Color.Black)
            }

            // System Reset
            IconButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.Gray)
            }
        }
    }
}
