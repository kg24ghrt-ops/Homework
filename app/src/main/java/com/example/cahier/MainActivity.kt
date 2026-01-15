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
            var showInputByTyping by remember { mutableStateOf(false) }
            
            // This holds the handwriting glyphs in memory
            val customGlyphs = remember { mutableStateMapOf<Char, Bitmap>() }
            val context = LocalContext.current

            // Launcher for picking a PNG from the gallery
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    // For now, let's map it to 'a'. In a full app, you'd pick the letter.
                    customGlyphs['a'] = bitmap 
                }
            }

            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        Column {
                            // Button to Add your Handwriting (PNG)
                            SmallFloatingActionButton(onClick = { launcher.launch("image/png") }) {
                                Text("PNG")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Button to Type
                            FloatingActionButton(onClick = { showInputByTyping = true }) {
                                Text("EDIT")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        PaperView(text = noteText, glyphMap = customGlyphs)

                        if (showInputByTyping) {
                            HandwritingInputDialog(
                                initialText = noteText,
                                onDismiss = { showInputByTyping = false },
                                onConfirm = { 
                                    noteText = it.lowercase() 
                                    showInputByTyping = false
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

        // Draw Notebook Lines
        var yPos = headerSpace
        while (yPos < height) {
            drawLine(lineBlue, Offset(0f, yPos), Offset(width, yPos), 2f)
            yPos += lineSpacing
        }

        // Render Handwriting
        var currentX = horizontalMargin + 25f
        var currentY = headerSpace - 60f 

        text.forEach { char ->
            val bitmap = glyphMap[char]
            if (bitmap != null) {
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(currentX.toInt(), currentY.toInt())
                )
                currentX += bitmap.width + 10f
            } else if (char == ' ') {
                currentX += 30f
            }
        }
    }
}

@Composable
fun HandwritingInputDialog(initialText: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Text") },
        text = { TextField(value = text, onValueChange = { text = it }) },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Write") } }
    )
}