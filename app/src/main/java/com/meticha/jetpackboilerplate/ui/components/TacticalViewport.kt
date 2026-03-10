package com.meticha.jetpackboilerplate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.meticha.jetpackboilerplate.domain.CartesianPoint
import com.meticha.jetpackboilerplate.ui.MeasurementUnit
import com.meticha.jetpackboilerplate.ui.theme.*
import kotlin.math.abs
import kotlin.math.max

@Composable
fun TacticalViewport(
    path: List<CartesianPoint>,
    selectedUnit: MeasurementUnit,
    modifier: Modifier = Modifier
) {
    // 1. CACHE GEOMETRY: Prevents recalculating bounds 60 times per second
    val viewportData = remember(path) {
        if (path.isEmpty()) return@remember null
        
        val allPoints = path + CartesianPoint(0f, 0f)
        val minX = allPoints.minOf { it.x }; val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }; val maxY = allPoints.maxOf { it.y }
        
        // Find the absolute maximum reach to determine scale
        val maxReach = allPoints.maxOf { max(abs(it.x), abs(it.y)) }.coerceAtLeast(1f)
        
        object {
            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f
            val span = maxReach * 2.5f // Ensure 2.5x breathing room
        }
    }

    // 2. CACHE PATHS: Optimized for GPU batching
    val vectorPath = remember(path) {
        Path().apply {
            moveTo(0f, 0f)
            path.forEach { p -> lineTo(p.x, -p.y) } // NOTE: -p.y inverts Android's Y-axis for North
        }
    }

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f) }

    Canvas(modifier = modifier.fillMaxSize().background(Color(0xFF0A0E12))) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val scale = if (viewportData == null) 50f else (size.minDimension / viewportData.span)

        // 3. ANCHORED GRID SYSTEM: Moves with the Origin (Point A)
        val gridStep = if (selectedUnit == MeasurementUnit.CENTIMETERS) 100f else 50f
        withTransform({
            translate(center.x, center.y)
        }) {
            val linesCount = (max(size.width, size.height) / gridStep).toInt()
            for (i in -linesCount..linesCount) {
                val coord = i * gridStep
                // Vertical lines
                drawLine(Color.White.copy(0.05f), Offset(coord, -size.height), Offset(coord, size.height), 1f)
                // Horizontal lines
                drawLine(Color.White.copy(0.05f), Offset(-size.width, coord), Offset(size.width, coord), 1f)
            }
        }

        // 4. TRANSFORMED VECTOR SPACE
        withTransform({
            translate(center.x, center.y)
            scale(scale, scale, Offset.Zero)
        }) {
            // Draw Main Path (Blue)
            drawPath(
                path = vectorPath,
                color = CommandCyan,
                style = Stroke(width = 3.dp.toPx() / scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw Resultant (Orange Dash)
            if (path.isNotEmpty()) {
                val last = path.last()
                drawLine(
                    color = Color(0xFFFF9800),
                    start = Offset.Zero,
                    end = Offset(last.x, -last.y),
                    strokeWidth = 2.dp.toPx() / scale,
                    pathEffect = dashEffect
                )
            }
        }

        // 5. NODE HIGHLIGHTS
        // Start Point (RadarGreen)
        drawCircle(RadarGreen, 6.dp.toPx(), center)

        // End Point (CommandCyan)
        if (path.isNotEmpty()) {
            val last = path.last()
            val finalPos = Offset(center.x + (last.x * scale), center.y - (last.y * scale))
            drawCircle(CommandCyan, 5.dp.toPx(), finalPos)
        }
    }
}
