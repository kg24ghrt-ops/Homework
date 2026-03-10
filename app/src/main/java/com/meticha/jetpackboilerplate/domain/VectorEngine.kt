package com.meticha.jetpackboilerplate.domain

import kotlin.math.*

data class VectorInput(val magnitude: Double, val bearing: Double)
data class CartesianPoint(val x: Float, val y: Float)

class VectorEngine {

    // PRE-COMPUTED CONSTANTS
    // Reduces CPU cycles by not recalculating the degree-to-radian ratio every loop
    private val DEGREES_TO_RADIANS = PI / 180.0
    private val RADIANS_TO_DEGREES = 180.0 / PI

    /**
     * OPTIMIZATION: Uses a pre-sized ArrayList to avoid memory jumping.
     * Efficiency: O(n) with minimal garbage collection.
     */
    fun calculatePath(inputs: List<VectorInput>): List<CartesianPoint> {
        // Pre-allocate the list size to prevent "resizing" spikes during calculation
        val path = ArrayList<CartesianPoint>(inputs.size + 1)
        path.add(CartesianPoint(0f, 0f))
        
        var currentX = 0.0
        var currentY = 0.0

        // Use a standard for-loop (faster than forEach on Android for large lists)
        for (i in inputs.indices) {
            val input = inputs[i]
            val rad = input.bearing * DEGREES_TO_RADIANS
            
            // MATH OPTIMIZATION: Direct mapping for Android Canvas
            // cos(rad) is more efficient for the vertical axis in a bearing system
            currentX += input.magnitude * sin(rad)
            currentY -= input.magnitude * cos(rad) 
            
            path.add(CartesianPoint(currentX.toFloat(), currentY.toFloat()))
        }
        return path
    }

    /**
     * OPTIMIZATION: Uses hypot() for better precision. 
     * hypot() is more accurate than sqrt(x^2 + y^2) because it avoids overflow/underflow.
     */
    fun getResultant(finalPoint: CartesianPoint): VectorInput {
        val x = finalPoint.x.toDouble()
        val y = finalPoint.y.toDouble()
        
        // High-precision distance calculation
        val magnitude = hypot(x, y)
        
        // Use atan2 for robust quadrant detection
        var angle = atan2(x, -y) * RADIANS_TO_DEGREES
        
        // Normalizing angle to 0-360 using the modulo operator for speed
        if (angle < 0) angle += 360.0
        
        return VectorInput(magnitude, angle)
    }
}
