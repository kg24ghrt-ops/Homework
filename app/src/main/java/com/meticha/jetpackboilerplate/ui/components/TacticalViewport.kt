package com.meticha.jetpackboilerplate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.meticha.jetpackboilerplate.domain.CartesianPoint
import com.meticha.jetpackboilerplate.ui.MeasurementUnit
import com.meticha.jetpackboilerplate.ui.theme.*

@Composable
fun TacticalViewport(
    path: List<CartesianPoint>,
    selectedUnit: MeasurementUnit,
    modifier: Modifier = Modifier
) {
    // OPTIMIZATION 1: Pre-calculate geometry outside the draw loop
    val viewportData = remember(path, selectedUnit) {
        val allPoints = path + CartesianPoint(0f, 0f)
        val minX = allPoints.minOf { it.x }; val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }; val maxY = allPoints.maxOf { it.y }
        
        val width = (maxX - minX).coerceAtLeast(1f)
        val height = (maxY - minY).coerceAtLeast(1f)
        
        object {
            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f
            val rawWidth = width
            val rawHeight = height
        }
    }

    Canvas(modifier = modifier.fillMaxSize().background(CommandBlack)) {
        if (path.isEmpty()) return@Canvas

        // OPTIMIZATION 2: Intelligent Scaling
        // Provides more breathing room for CM units
        val padding = if (selectedUnit == MeasurementUnit.CENTIMETERS) 0.65f else 0.85f
        val scale = minOf(
            size.width * padding / viewportData.rawWidth,
            size.height * padding / viewportData.rawHeight
        ).coerceIn(0.1f, 10000f)

        val centerOffset = Offset(
            size.width / 2f - viewportData.centerX * scale,
            size.height / 2f - viewportData.centerY * scale
        )

        // OPTIMIZATION 3: Static Background Grid (Tactical Look)
        val gridAlpha = 0.15f
        val step = 50f * (if (selectedUnit == MeasurementUnit.CENTIMETERS) 0.5f else 1f)
        
        // Draw vertical/horizontal grid lines relative to Origin
        for (i in -10..10) {
            val pos = i * step * scale
            drawLine(Color.Gray, Offset(centerOffset.x + pos, 0f), Offset(centerOffset.x + pos, size.height), 1f, alpha = gridAlpha)
            drawLine(Color.Gray, Offset(0f, centerOffset.y + pos), Offset(size.width, centerOffset.y + pos), 1f, alpha = gridAlpha)
        }

        // OPTIMIZATION 4: Path Batching
        val drawPath = Path().apply {
            moveTo(centerOffset.x, centerOffset.y)
            path.forEach { p -> lineTo(centerOffset.x + p.x * scale, centerOffset.y + p.y * scale) }
        }

        // Draw the main vector path with a Glow effect (Shadow)
        drawPath(
            path = drawPath,
            color = CommandCyan,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw Resultant with specialized dash effect
        if (path.isNotEmpty()) {
            val last = path.last()
            drawLine(
                color = AlertOrange,
                start = centerOffset,
                end = Offset(centerOffset.x + last.x * scale, centerOffset.y + last.y * scale),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f), 0f)
            )
        }
        
        // Final Node highlight
        if (path.isNotEmpty()) {
            drawCircle(CommandCyan, 6.dp.toPx(), Offset(centerOffset.x + path.last().x * scale, centerOffset.y + path.last().y * scale))
        }
    }
}
