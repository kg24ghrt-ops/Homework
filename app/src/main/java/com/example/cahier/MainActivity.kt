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
            var noteText by remember { mutableStateOf("") }
            var showTextDialog by remember { mutableStateOf(false) }
            var showAssignDialog by remember { mutableStateOf(false) }
            var lastPickedUri by remember { mutableStateOf<Uri?>(null) }
            
            // State map to store character -> handwriting bitmap
            val glyphMap = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current

            // Launcher to select a PNG from the gallery
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    lastPickedUri = uri
                    showAssignDialog = true // Trigger assignment dialog
                }
            }

            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            SmallFloatingActionButton(onClick = { launcher.launch("image/png") }) {
                                Text("PNG", modifier = Modifier.padding(horizontal = 8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            FloatingActionButton(onClick = { showTextDialog = true }) {
                                Text("EDIT", modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        PaperView(text = noteText, glyphMap = glyphMap)

                        // Dialog to assign a character to the picked PNG
                        if (showAssignDialog) {
                            AssignGlyphDialog(
                                onDismiss = { showAssignDialog = false },
                                onConfirm = { char ->
                                    lastPickedUri?.let { uri ->
                                        try {
                                            val inputStream = context.contentResolver.openInputStream(uri)
                                            val bitmap = BitmapFactory.decodeStream(inputStream)
                                            if (bitmap != null) {
                                                glyphMap[char.lowercaseChar()] = bitmap
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                    showAssignDialog = false
                                }
                            )
                        }

                        // Dialog to type your text
                        if (showTextDialog) {
                            HandwritingInputDialog(
                                initialText = noteText,
                                onDismiss = { showTextDialog = false },
                                onConfirm = { 
                                    noteText = it.lowercase() 
                                    showTextDialog = false
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
fun AssignGlyphDialog(onDismiss: () -> Unit, onConfirm: (Char) -> Unit) {
    var charInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign to which letter?") },
        text = {
            TextField(
                value = charInput,
                onValueChange = { if (it.length <= 1) charInput = it },
                placeholder = { Text("e.g., a") }
            )
        },
        confirmButton = {
            Button(onClick = { if (charInput.isNotEmpty()) onConfirm(charInput[0]) }) {
                Text("Assign")
            }
        }
    )
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val width = size.width
        val height = size.height
        
        // Visual constants matching your notebook image
        val lineBlue = Color(0xFFADCEEB)
        val horizontalMargin = 110f
        val lineSpacing = 65f
        val headerSpace = 220f

        // Draw horizontal guidelines
        var yPos = headerSpace
        while (yPos < height) {
            drawLine(lineBlue, Offset(0f, yPos), Offset(width, yPos), 2f)
            yPos += lineSpacing
        }

        // Render mapped PNGs
        var currentX = horizontalMargin + 25f
        var currentY = headerSpace - 60f // Adjusts the 'sitting' position on the line

        text.forEach { char ->
            val bitmap = glyphMap[char]
            if (bitmap != null) {
                // Calculate scale to keep the letter within the line height
                val targetHeight = 50f
                val scale = targetHeight / bitmap.height
                val scaledWidth = bitmap.width * scale

                // Wrap to next line if width exceeded
                if (currentX + scaledWidth > width - 40f) {
                    currentX = horizontalMargin + 25f
                    currentY += lineSpacing
                }

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(currentX.toInt(), currentY.toInt())
                )
                currentX += scaledWidth + 10f
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
