package com.meticha.jetpackboilerplate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.meticha.jetpackboilerplate.domain.CartesianPoint
import com.meticha.jetpackboilerplate.ui.theme.*

@Composable
fun TacticalViewport(
    path: List<CartesianPoint>,
    modifier: Modifier = Modifier
) {
    // 1. CALCULATE BOUNDS & SCALE (Memoized to prevent jitter)
    val bounds = remember(path) {
        val allPoints = path + CartesianPoint(0f, 0f)
        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }
        
        object {
            val minX = minX; val maxX = maxX; val minY = minY; val maxY = maxY
            val width = (maxX - minX).coerceAtLeast(0.1f)
            val height = (maxY - minY).coerceAtLeast(0.1f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(CommandBlack)
    ) {
        if (path.isEmpty()) return@Canvas

        // 2. ASPECT-RATIO FITTING
        val scale = minOf(
            size.width * 0.80f / bounds.width,
            size.height * 0.80f / bounds.height
        ).coerceIn(1f, 1000f)

        // 3. CENTERING TRANSFORM
        val centerOffset = Offset(
            size.width / 2f - ((bounds.minX + bounds.maxX) / 2f) * scale,
            size.height / 2f - ((bounds.minY + bounds.maxY) / 2f) * scale
        )

        val strokePx = 3.dp.toPx()
        val nodeRadius = 5.dp.toPx()

        // 4. DRAW NODES (Village Points)
        path.forEach { point ->
            drawCircle(
                color = CommandCyan,
                radius = nodeRadius,
                center = Offset(centerOffset.x + point.x * scale, centerOffset.y + point.y * scale)
            )
        }
        // Draw the Origin Node (Village P) in Alert Orange
        drawCircle(
            color = AlertOrange,
            radius = nodeRadius * 1.2f,
            center = centerOffset
        )

        // 5. DRAW VECTOR CHAIN
        val vectorPath = Path().apply {
            moveTo(centerOffset.x + path[0].x * scale, centerOffset.y + path[0].y * scale)
            for (i in 1 until path.size) {
                lineTo(centerOffset.x + path[i].x * scale, centerOffset.y + path[i].y * scale)
            }
        }

        drawPath(
            path = vectorPath,
            color = CommandCyan,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // 6. DRAW THE RESULTANT (The Solution)
        if (path.size >= 1) {
            val finalPoint = path.last()
            drawLine(
                color = AlertOrange,
                start = centerOffset,
                end = Offset(centerOffset.x + finalPoint.x * scale, centerOffset.y + finalPoint.y * scale),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))
            )
        }
    }
}
