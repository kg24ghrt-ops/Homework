package com.example.cahier

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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var inputText by remember { mutableStateOf("") }

            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        // Simple Input Field at the top
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Type or paste text here...") },
                            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = Color.White
                    ) {
                        PaperView(text = inputText)
                    }
                }
            }
        }
    }
}

@Composable
fun PaperView(text: String) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val width = size.width
        val height = size.height
        
        // Textbook Configuration
        val horizontalMargin = 80f
        val lineSpacing = 60f
        val topPadding = 100f
        val linePaintColor = Color(0xFFE0E0E0)
        
        // 1. Draw Paper Background (Guidelines)
        drawLine(
            color = linePaintColor,
            start = Offset(horizontalMargin, 0f),
            end = Offset(horizontalMargin, height),
            strokeWidth = 2f
        )
        drawLine(
            color = linePaintColor,
            start = Offset(width - horizontalMargin, 0f),
            end = Offset(width - horizontalMargin, height),
            strokeWidth = 2f
        )

        var gridY = topPadding
        while (gridY < height) {
            drawLine(
                color = linePaintColor,
                start = Offset(horizontalMargin, gridY),
                end = Offset(width - horizontalMargin, gridY),
                strokeWidth = 2f
            )
            gridY += lineSpacing
        }

        // 2. Render Text inside Margins (Manual Line Wrapping)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 40f
            isAntiAlias = true
        }

        val maxWidth = width - (horizontalMargin * 2) - 20f
        val words = text.split(" ")
        var currentX = horizontalMargin + 10f
        var currentY = topPadding - 10f // Start slightly above the first line
        
        drawContext.canvas.nativeCanvas.apply {
            val spaceWidth = paint.measureText(" ")
            
            words.forEach { word ->
                val wordWidth = paint.measureText(word)
                
                // If word exceeds margin, move to next line
                if (currentX + wordWidth > width - horizontalMargin) {
                    currentX = horizontalMargin + 10f
                    currentY += lineSpacing
                }
                
                // Prevent drawing off the bottom of the paper
                if (currentY < height) {
                    drawText(word, currentX, currentY, paint)
                    currentX += wordWidth + spaceWidth
                }
            }
        }
    }
}