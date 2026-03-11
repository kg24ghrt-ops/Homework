package com.meticha.jetpackboilerplate.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meticha.jetpackboilerplate.domain.CartesianPoint
import com.meticha.jetpackboilerplate.ui.MeasurementUnit
import com.meticha.jetpackboilerplate.ui.theme.*
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalTextApi::class)
@Composable
fun TacticalViewport(
    path: List<CartesianPoint>,
    selectedUnit: MeasurementUnit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    // 1. DYNAMIC AUTO-SCALE: Automatically fits all houses on screen
    val maxReach = remember(path) {
        if (path.isEmpty()) 500f 
        else (path + CartesianPoint(0f, 0f)).maxOf { max(abs(it.x), abs(it.y)) }.coerceAtLeast(100f)
    }
    
    // Smoothly animate scale changes so the map "zooms" in and out nicely
    val animatedScale by animateFloatAsState(
        targetValue = maxReach,
        animationSpec = tween(durationMillis = 800),
        label = "ZoomLevel"
    )

    Canvas(modifier = modifier.fillMaxSize().background(Color(0xFF080B0F))) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val zoomFactor = (size.minDimension / (animatedScale * 2.8f))

        // 2. RADAR CIRCLES (Visual Polish)
        for (i in 1..4) {
            drawCircle(
                color = CommandCyan.copy(alpha = 0.03f),
                radius = (size.minDimension / 4) * i,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 3. THE VECTOR SPACE
        withTransform({
            translate(center.x, center.y)
            scale(zoomFactor, zoomFactor, Offset.Zero)
        }) {
            // Draw X & Y Axis
            drawLine(Color.White.copy(0.1f), Offset(-10000f, 0f), Offset(10000f, 0f), 0.5f / zoomFactor)
            drawLine(Color.White.copy(0.1f), Offset(0f, -10000f), Offset(0f, 10000f), 0.5f / zoomFactor)

            // Draw Vector Path (A -> B -> C -> D)
            val strokePath = Path().apply {
                moveTo(0f, 0f)
                path.forEach { p -> lineTo(p.x, -p.y) }
            }
            
            drawPath(
                path = strokePath,
                color = CommandCyan,
                style = Stroke(
                    width = 4.dp.toPx() / zoomFactor,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw Resultant (A to D) in Orange
            if (path.isNotEmpty()) {
                val last = path.last()
                drawLine(
                    color = Color(0xFFFF9800),
                    start = Offset.Zero,
                    end = Offset(last.x, -last.y),
                    strokeWidth = 2.dp.toPx() / zoomFactor,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f / zoomFactor, 10f / zoomFactor), 0f)
                )
            }
        }

        // 4. HOUSE LABELS (A, B, C, D)
        val labels = listOf("A") + path.indices.map { ('B' + it).toString() }
        val points = listOf(Offset.Zero) + path.map { Offset(it.x * zoomFactor, -it.y * zoomFactor) }
        
        points.forEachIndexed { index, point ->
            val screenPos = center + point
            
            // Draw Glow Point
            drawCircle(
                Brush.radialGradient(listOf(CommandCyan.copy(0.5f), Color.Transparent), center = screenPos, radius = 15.dp.toPx()),
                radius = 15.dp.toPx(),
                center = screenPos
            )
            drawCircle(if (index == 0) RadarGreen else CommandCyan, 4.dp.toPx(), screenPos)

            // Draw Text Label (House Name)
            if (index < labels.size) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = labels[index],
                    topLeft = screenPos + Offset(8.dp.toPx(), -20.dp.toPx()),
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
