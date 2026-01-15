package com.example.cahier

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // 1. Set default text to 'a' so you can see your picked PNG immediately
            var noteText by remember { mutableStateOf("a") }
            var showDialog by remember { mutableStateOf(false) }
            
            // 2. Use a SnapshotStateMap to ensure the Canvas updates when a PNG is picked
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current

            val pngPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        // Maps your picked PNG to the letter 'a'
                        glyphMap['a'] = bitmap
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }

            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            SmallFloatingActionButton(
                                onClick = { pngPickerLauncher.launch("image/png") }
                            ) {
                                Text("PNG", modifier = Modifier.padding(horizontal = 8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            FloatingActionButton(onClick = { showDialog = true }) {
                                Text("EDIT", modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        PaperView(text = noteText, glyphMap = glyphMap)

                        if (showDialog) {
                            HandwritingInputDialog(
                                initialText = noteText,
                                onDismiss = { showDialog = false },
                                onConfirm = { 
                                    noteText = it.lowercase() 
                                    showDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val width = size.width
        val height = size.height
        
        val lineBlue = Color(0xFFADCEEB)
        val horizontalMargin = 110f
        val lineSpacing = 65f
        val headerSpace = 220f

        // Draw horizontal lines
        var yPos = headerSpace
        while (yPos < height) {
            drawLine(lineBlue, Offset(0f, yPos), Offset(width, yPos), 2f)
            yPos += lineSpacing
        }

        // Render Handwriting PNGs
        var currentX = horizontalMargin + 25f
        var currentY = headerSpace - 80f // Adjusted to sit better on the line

        text.forEach { char ->
            val bitmap = glyphMap[char]
            if (bitmap != null) {
                // Scale the bitmap to fit the line height (approx 50-60 pixels)
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(currentX.toInt(), currentY.toInt())
                )
                currentX += (bitmap.width + 10f) // Move X forward
                
                // Line wrapping
                if (currentX > width - 60f) {
                    currentX = horizontalMargin + 25f
                    currentY += lineSpacing
                }
            } else if (char == ' ') {
                currentX += 40f
            }
        }
    }
}

@Composable
fun HandwritingInputDialog(initialText: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write Text") },
        text = { TextField(value = text, onValueChange = { text = it }) },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } }
    )
}