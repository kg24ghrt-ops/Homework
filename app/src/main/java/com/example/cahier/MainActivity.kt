package com.example.cahier

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var noteText by remember { mutableStateOf("") }
            var showDialog by remember { mutableStateOf(false) }
            val glyphs = rememberGlyphs()

            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        // Using a simple Text label to avoid missing icon library errors
                        FloatingActionButton(onClick = { showDialog = true }) {
                            Text("EDIT", modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        PaperView(text = noteText, glyphMap = glyphs)

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
fun HandwritingInputDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Text (a-z)") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type or paste here...") }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) { Text("Write") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun rememberGlyphs(): Map<Char, Bitmap> {
    val context = LocalContext.current
    var glyphMap by remember { mutableStateOf<Map<Char, Bitmap>>(emptyMap()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val map = mutableMapOf<Char, Bitmap>()
            try {
                // Iterates a-z to find matching bitmaps in app/src/main/assets/glyphs/
                ('a'..'z').forEach { char ->
                    context.assets.open("glyphs/$char.png").use { stream ->
                        map[char] = BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (e: Exception) { 
                e.printStackTrace() 
            }
            glyphMap = map
        }
    }
    return glyphMap
}

@Composable
fun PaperView(text: String, glyphMap: Map<Char, Bitmap>) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val width = size.width
        val height = size.height
        
        // Notebook Aesthetics
        val lineBlue = Color(0xFFADCEEB)
        val marginRed = Color(0xFFFFB3B3)
        val horizontalMargin = 110f
        val lineSpacing = 65f
        val headerSpace = 220f
        val letterSpacing = 4f
        val wordSpacing = 24f

        // 1. Draw Paper Background
        drawLine(marginRed, Offset(horizontalMargin, 0f), Offset(horizontalMargin, height), 3f)
        var yPos = headerSpace
        while (yPos < height) {
            drawLine(lineBlue, Offset(0f, yPos), Offset(width, yPos), 2f)
            yPos += lineSpacing
        }

        // 2. Render Bitmap Glyphs
        var currentX = horizontalMargin + 25f
        var currentY = headerSpace - 55f // Aligns bitmap to sit on the blue line

        text.forEach { char ->
            when (char) {
                '\n' -> {
                    currentX = horizontalMargin + 25f
                    currentY += lineSpacing
                }
                ' ' -> currentX += wordSpacing
                else -> {
                    val bitmap = glyphMap[char]
                    if (bitmap != null) {
                        val glyphW = bitmap.width.toFloat()
                        
                        // Wrap text at the right margin
                        if (currentX + glyphW > width - 40f) {
                            currentX = horizontalMargin + 25f
                            currentY += lineSpacing
                        }
                        
                        // Prevent drawing if we exceed the bottom of the page
                        if (currentY < height) {
                            drawImage(
                                image = bitmap.asImageBitmap(),
                                dstOffset = IntOffset(currentX.toInt(), currentY.toInt())
                            )
                            currentX += glyphW + letterSpacing
                        }
                    } else {
                        // Fallback for missing glyphs: simple space
                        currentX += 15f
                    }
                }
            }
        }
    }
}