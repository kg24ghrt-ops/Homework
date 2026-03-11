package com.meticha.jetpackboilerplate.domain

import kotlin.math.*

data class VectorInput(val magnitude: Double, val bearing: Double)
data class CartesianPoint(val x: Float, val y: Float)

class VectorEngine {

    private companion object {
        // High-precision constants
        const val DEG_TO_RAD = PI / 180.0
        const val RAD_TO_DEG = 180.0 / PI
        const val EPSILON = 1e-10 // Used to clean up "tiny" math errors
    }

    /**
     * V4.1 ELITE: High-Precision Path Calculation
     * Optimized to ensure 90°, 180°, 270° are perfectly flat/vertical.
     */
    fun calculatePath(inputs: List<VectorInput>): List<CartesianPoint> {
        val path = ArrayList<CartesianPoint>(inputs.size + 1)
        var currentX = 0.0
        var currentY = 0.0
        
        path.add(CartesianPoint(0f, 0f))

        for (input in inputs) {
            // 1. NORMALIZE
            val brng = (input.bearing % 360 + 360) % 360
            
            // 2. CONVERT BEARING TO STANDARD UNIT CIRCLE
            // Bearing 0 (N) -> 90° on unit circle
            // Bearing 90 (E) -> 0° on unit circle
            val standardAngleRad = (90.0 - brng) * DEG_TO_RAD
            
            // 3. CALCULATION WITH PRECISION CLEANUP
            var dx = input.magnitude * cos(standardAngleRad)
            var dy = input.magnitude * sin(standardAngleRad)

            // Fix for "almost zero" values (e.g. 1.2e-15) which confuse the UI
            if (abs(dx) < EPSILON) dx = 0.0
            if (abs(dy) < EPSILON) dy = 0.0
            
            currentX += dx
            currentY += dy // Using standard math Y (Up is Positive)
            
            // We store raw math coordinates; the UI (Viewport) handles the inversion
            path.add(CartesianPoint(currentX.toFloat(), currentY.toFloat()))
        }
        return path
    }

    /**
     * PRECISION RESULTANT: 
     * Calculates the vector from (0,0) to the last point.
     */
    fun getResultant(finalPoint: CartesianPoint): VectorInput {
        val x = finalPoint.x.toDouble()
        val y = finalPoint.y.toDouble()
        
        val magnitude = hypot(x, y)
        
        // atan2 handles all 4 quadrants perfectly
        val angleRad = atan2(y, x)
        var bearing = 90.0 - (angleRad * RAD_TO_DEG)
        
        return VectorInput(magnitude, (bearing % 360 + 360) % 360)
    }
}
