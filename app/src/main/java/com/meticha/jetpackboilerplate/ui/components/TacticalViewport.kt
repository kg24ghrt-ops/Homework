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
import com.meticha.jetpackboilerplate.domain.MeasurementUnit // FIXED: Import from domain
import com.meticha.jetpackboilerplate.ui.theme.*
import kotlin.math.abs
import kotlin.math.max
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalTextApi::class)
@Composable
fun TacticalViewport(
    path: List<CartesianPoint>,
    selectedUnit: MeasurementUnit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    // 1. DYNAMIC AUTO-SCALE: Automatically fits all coordinates on screen
    val maxReach = remember(path) {
        if (path.isEmpty()) 500f 
        else (path + CartesianPoint(0f, 0f)).maxOf { 
            max(abs(it.x), abs(it.y)) 
        }.coerceAtLeast(100f)
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = maxReach,
        animationSpec = tween(durationMillis = 800),
        label = "ZoomLevel"
    )

    Canvas(modifier = modifier.fillMaxSize().background(Color(0xFF080B0F))) {
        val center = Offset(size.width / 2f, size.height / 2f)
        // Adjust zoomFactor to leave room for labels at the edges
        val zoomFactor = (size.minDimension / (animatedScale * 2.8f))

        // 2. RADAR CIRCLES
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
            // Draw Axis lines
            drawLine(Color.White.copy(0.1f), Offset(-10000f, 0f), Offset(10000f, 0f), 1f / zoomFactor)
            drawLine(Color.White.copy(0.1f), Offset(0f, -10000f), Offset(0f, 10000f), 1f / zoomFactor)

            // Draw Vector Path
            val strokePath = Path().apply {
                moveTo(0f, 0f)
                path.forEach { p -> lineTo(p.x.toFloat(), -p.y.toFloat()) }
            }
            
            drawPath(
                path = strokePath,
                color = CommandCyan,
                style = Stroke(
                    width = 3.dp.toPx() / zoomFactor,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw Resultant (Origin to Final Point)
            if (path.isNotEmpty()) {
                val last = path.last()
                drawLine(
                    color = Color(0xFFFF9800),
                    start = Offset.Zero,
                    end = Offset(last.x.toFloat(), -last.y.toFloat()),
                    strokeWidth = 2.dp.toPx() / zoomFactor,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(10f / zoomFactor, 10f / zoomFactor), 0f
                    )
                )
            }
        }

        // 4. COORDINATE LABELS
        // Generates A, B, C... and handles overflow (AA, AB...) if path is very long
        val points = listOf(Offset.Zero) + path.map { Offset(it.x.toFloat() * zoomFactor, -it.y.toFloat() * zoomFactor) }
        
        points.forEachIndexed { index, point ->
            val screenPos = center + point
            val label = if (index == 0) "ORIGIN" else ('A' + (index - 1)).toString()
            
            // Draw Node Glow
            drawCircle(
                Brush.radialGradient(
                    colors = listOf(CommandCyan.copy(0.4f), Color.Transparent),
                    center = screenPos,
                    radius = 12.dp.toPx()
                ),
                radius = 12.dp.toPx(),
                center = screenPos
            )
            
            drawCircle(
                color = if (index == 0) RadarGreen else CommandCyan,
                radius = 4.dp.toPx(),
                center = screenPos
            )

            // Draw Label Text
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = screenPos + Offset(8.dp.toPx(), -20.dp.toPx()),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            )
        }
    }
}
