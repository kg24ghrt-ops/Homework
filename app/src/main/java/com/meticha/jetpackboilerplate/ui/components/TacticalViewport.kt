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
import com.meticha.jetpackboilerplate.ui.theme.CommandCyan
import com.meticha.jetpackboilerplate.ui.theme.AlertOrange
import com.meticha.jetpackboilerplate.ui.theme.CommandBlack

@Composable
fun TacticalViewport(
    path: List<CartesianPoint>,
    modifier: Modifier = Modifier
) {
    // OPTIMIZATION 1: Path Memoization
    // Instead of looping every frame, we build the "Drawing Instruction" once.
    // The GPU can draw a Path significantly faster than individual lines.
    val cachedPath = remember(path) {
        Path().apply {
            if (path.isNotEmpty()) {
                moveTo(path[0].x, path[0].y)
                for (i in 1 until path.size) {
                    lineTo(path[i].x, path[i].y)
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(CommandBlack)
    ) {
        val strokePx = 3.dp.toPx()
        val nodeRadius = 4.dp.toPx()

        // 1. Draw the Vector Chain (Single GPU Call)
        drawPath(
            path = cachedPath,
            color = CommandCyan,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // 2. Draw Nodes (Optimization: Only draw start and end to reduce clutter)
        if (path.isNotEmpty()) {
            path.forEach { point ->
                drawCircle(
                    color = CommandCyan,
                    radius = nodeRadius,
                    center = Offset(point.x, point.y)
                )
            }
        }

        // 3. The "Solve" Node (Resultant)
        if (path.size > 1) {
            val finalPoint = path.last()
            drawLine(
                color = AlertOrange,
                start = Offset(0f, 0f),
                end = Offset(finalPoint.x, finalPoint.y),
                strokeWidth = 2.dp.toPx(),
                // Optimization: Pre-allocate floatArray for the dash effect outside if possible
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))
            )
        }
    }
}
