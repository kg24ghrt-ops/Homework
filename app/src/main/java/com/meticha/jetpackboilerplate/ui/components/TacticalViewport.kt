package com.meticha.jetpackboilerplate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(CommandBlack)
    ) {
        val strokeWidth = 3.dp.toPx()
        
        // 1. Draw the Vector Chain
        for (i in 0 until path.size - 1) {
            val start = Offset(path[i].x, path[i].y)
            val end = Offset(path[i+1].x, path[i+1].y)
            
            drawLine(
                color = CommandCyan,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            
            // Draw a small node at each point
            drawCircle(
                color = CommandCyan,
                radius = 4.dp.toPx(),
                center = end
            )
        }

        // 2. The "Solve" Node (Resultant Line)
        // Dotted line from start (0,0) to the current active point
        if (path.size > 1) {
            val finalPoint = Offset(path.last().x, path.last().y)
            drawLine(
                color = AlertOrange,
                start = Offset(0f, 0f),
                end = finalPoint,
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
            )
        }
    }
}
