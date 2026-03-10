package com.meticha.jetpackboilerplate.domain

import kotlin.math.*

data class VectorInput(val magnitude: Double, val bearing: Double)
data class CartesianPoint(val x: Float, val y: Float)

class VectorEngine {

    private companion object {
        const val DEG_TO_RAD = PI / 180.0
        const val RAD_TO_DEG = 180.0 / PI
    }

    /**
     * V4 IMPROVEMENT: Clean Coordinate Mapping
     * Standardizes input so North (0°) is always "Up" on the screen.
     */
    fun calculatePath(inputs: List<VectorInput>): List<CartesianPoint> {
        val path = ArrayList<CartesianPoint>(inputs.size + 1)
        var currentX = 0.0
        var currentY = 0.0
        
        // Always start at origin
        path.add(CartesianPoint(0f, 0f))

        for (input in inputs) {
            // NORMALIZATION: Handle bearings > 360 or < 0
            val normalizedBearing = (input.bearing % 360 + 360) % 360
            
            // MATH LOGIC:
            // 0° (North) -> cos(90)=0, sin(90)=1 -> (0, -mag)
            // 90° (East) -> cos(0)=1, sin(0)=0 -> (mag, 0)
            val rad = (90.0 - normalizedBearing) * DEG_TO_RAD
            
            currentX += input.magnitude * cos(rad)
            currentY -= input.magnitude * sin(rad) // Canvas Y-Down correction
            
            path.add(CartesianPoint(currentX.toFloat(), currentY.toFloat()))
        }
        return path
    }

    /**
     * PRECISION RESULTANT: 
     * Uses atan2 for 4-quadrant accuracy to ensure the orange line points 
     * exactly where the path ends.
     */
    fun getResultant(finalPoint: CartesianPoint): VectorInput {
        val x = finalPoint.x.toDouble()
        val y = -finalPoint.y.toDouble() // Invert Canvas Y back to Math Y
        
        val magnitude = hypot(x, y)
        
        // atan2 returns radians from -PI to PI
        val angleDeg = atan2(y, x) * RAD_TO_DEG
        
        // Convert Math angle (East-based) back to Bearing (North-based)
        var bearing = 90.0 - angleDeg
        
        // FINAL NORMALIZATION: Ensure 0-359.99 range
        bearing = (bearing % 360 + 360) % 360
        
        return VectorInput(magnitude, bearing)
    }
}
