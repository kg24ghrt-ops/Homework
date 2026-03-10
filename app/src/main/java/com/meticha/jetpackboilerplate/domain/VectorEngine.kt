package com.meticha.jetpackboilerplate.domain

import kotlin.math.*

data class VectorInput(val magnitude: Double, val bearing: Double)
data class CartesianPoint(val x: Float, val y: Float)

class VectorEngine {

    // HIGH-PRECISION CONSTANTS
    private companion object {
        const val DEG_TO_RAD = PI / 180.0
        const val RAD_TO_DEG = 180.0 / PI
        const val NORTH_OFFSET = 90.0 // Corrects math-East to navigation-North
    }

    /**
     * OPTIMIZATION: Sequence-based calculation.
     * We've adjusted the trigonometry to match Android's Canvas (where Y increases DOWN).
     */
    fun calculatePath(inputs: List<VectorInput>): List<CartesianPoint> {
        val path = ArrayList<CartesianPoint>(inputs.size + 1)
        path.add(CartesianPoint(0f, 0f))
        
        var currentX = 0.0
        var currentY = 0.0

        for (input in inputs) {
            // PROFESSIONAL MAPPING: 
            // In navigation, 0° is North. In math, 0° is East.
            // We use (90 - bearing) to align your 8th-grade homework with the screen.
            val rad = (NORTH_OFFSET - input.bearing) * DEG_TO_RAD
            
            currentX += input.magnitude * cos(rad)
            currentY -= input.magnitude * sin(rad) // Subtract because Canvas Y-up is negative
            
            path.add(CartesianPoint(currentX.toFloat(), currentY.toFloat()))
        }
        return path
    }

    /**
     * POWERFUL RESULTANT SOLVER
     * Uses IEEE 754 precision for displacement math.
     */
    fun getResultant(finalPoint: CartesianPoint): VectorInput {
        val x = finalPoint.x.toDouble()
        val y = -finalPoint.y.toDouble() // Flip back to standard Cartesian for atan2
        
        val magnitude = hypot(x, y)
        
        // Convert Cartesian angle back to Bearing angle
        var bearing = NORTH_OFFSET - (atan2(y, x) * RAD_TO_DEG)
        
        // Normalize to 0-360 range
        while (bearing < 0) bearing += 360.0
        while (bearing >= 360) bearing -= 360.0
        
        return VectorInput(magnitude, bearing)
    }
}
