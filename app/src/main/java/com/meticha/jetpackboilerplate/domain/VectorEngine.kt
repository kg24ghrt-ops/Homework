package com.meticha.jetpackboilerplate.domain

import kotlin.math.*

data class VectorInput(val magnitude: Double, val bearing: Double)
data class CartesianPoint(val x: Float, val y: Float)

class VectorEngine {

    private companion object {
        const val DEG_TO_RAD = PI / 180.0
        const val RAD_TO_DEG = 180.0 / PI
        const val EPSILON = 1e-10 
    }

    /**
     * REQUIRED FOR BUILD: Bridges the ViewModel to the Engine logic.
     * This calculates the total resultant directly from the list.
     */
    fun calculateScientificResultant(inputs: List<VectorInput>): VectorInput {
        if (inputs.isEmpty()) return VectorInput(0.0, 0.0)
        
        val path = calculatePath(inputs)
        return getResultant(path.last())
    }

    /**
     * V4.2 ELITE: High-Precision Path Calculation
     */
    fun calculatePath(inputs: List<VectorInput>): List<CartesianPoint> {
        val path = ArrayList<CartesianPoint>(inputs.size + 1)
        var currentX = 0.0
        var currentY = 0.0
        
        // Start at Origin
        path.add(CartesianPoint(0f, 0f))

        for (input in inputs) {
            val brng = (input.bearing % 360 + 360) % 360
            val standardAngleRad = (90.0 - brng) * DEG_TO_RAD
            
            var dx = input.magnitude * cos(standardAngleRad)
            var dy = input.magnitude * sin(standardAngleRad)

            // Precision Cleanup: Prevents "Ghost" offsets on perfect vertical/horizontal lines
            if (abs(dx) < EPSILON) dx = 0.0
            if (abs(dy) < EPSILON) dy = 0.0
            
            currentX += dx
            currentY += dy 
            
            path.add(CartesianPoint(currentX.toFloat(), currentY.toFloat()))
        }
        return path
    }

    /**
     * PRECISION RESULTANT: 
     * Converts a Cartesian end-point back into a Vector (Magnitude/Bearing).
     */
    fun getResultant(finalPoint: CartesianPoint): VectorInput {
        val x = finalPoint.x.toDouble()
        val y = finalPoint.y.toDouble()
        
        val magnitude = hypot(x, y)
        val angleRad = atan2(y, x)
        
        // Convert Math angle back to Navigation Bearing
        val bearing = (90.0 - (angleRad * RAD_TO_DEG) + 360.0) % 360.0
        
        return VectorInput(magnitude, bearing)
    }
}
