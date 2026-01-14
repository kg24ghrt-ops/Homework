package com.example.cahier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Define a local theme to avoid "Unresolved reference 'theme'" errors
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    PaperView()
                }
            }
        }
    }
}

@Composable
fun PaperView() {
    Canvas(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
        val width = size.width
        val height = size.height
        
        val marginSize = 80f
        val lineSpacing = 60f
        val lightGray = Color(0xFFE0E0E0)

        // Draw Vertical Margins
        drawLine(
            color = lightGray,
            start = Offset(marginSize, 0f),
            end = Offset(marginSize, height),
            strokeWidth = 2f
        )
        drawLine(
            color = lightGray,
            start = Offset(width - marginSize, 0f),
            end = Offset(width - marginSize, height),
            strokeWidth = 2f
        )

        // Draw Horizontal Lines
        var currentY = marginSize
        while (currentY < height) {
            drawLine(
                color = lightGray,
                start = Offset(marginSize, currentY),
                end = Offset(width - marginSize, currentY),
                strokeWidth = 2f
            )
            currentY += lineSpacing
        }
    }
}