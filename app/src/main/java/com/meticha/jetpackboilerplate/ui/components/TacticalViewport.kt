package com.meticha.jetpackboilerplate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(CommandBlack)
    ) {
        if (path.isEmpty()) return@Canvas

        // 1. CALCULATE THE "WORLD BOUNDS" (The professional way)
        // We find the smallest and largest X/Y to determine the map's size
        val allPoints = path + CartesianPoint(0f, 0f) // Always include the start point
        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }

        val worldWidth = (maxX - minX).coerceAtLeast(0.1f)
        val worldHeight = (maxY - minY).coerceAtLeast(0.1f)

        // 2. ASPECT-RATIO FITTING
        // We leave a 15% margin so lines don't hit the screen edge
        val scale = minOf(
            size.width * 0.85f / worldWidth,
            size.height * 0.85f / worldHeight
        ).coerceIn(1f, 1000f)

        // 3. THE CAMERA TRANSFORM (Translation)
        // This centers the "Village Cluster" in the middle of your phone
        val centerOffset = Offset(
            size.width / 2f - ((minX + maxX) / 2f) * scale,
            size.height / 2f - ((minY + maxY) / 2f) * scale
        )

        // 4. RENDERING THE VECTOR CHAIN
        val vectorPath = Path().apply {
            moveTo(
                centerOffset.x + path[0].x * scale, 
                centerOffset.y + path[0].y * scale
            )
            for (i in 1 until path.size) {
                lineTo(
                    centerOffset.x + path[i].x * scale, 
                    centerOffset.y + path[i].y * scale
                )
            }
        }

        drawPath(
            path = vectorPath,
            color = CommandCyan,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // 5. THE "RESULTANT" (The professional solved vector)
        if (path.size >= 1) {
            val finalPoint = path.last()
            drawLine(
                color = AlertOrange,
                start = centerOffset, // The centered (0,0)
                end = Offset(
                    centerOffset.x + finalPoint.x * scale, 
                    centerOffset.y + finalPoint.y * scale
                ),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))
            )
        }
    }
}
