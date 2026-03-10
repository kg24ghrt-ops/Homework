package com.meticha.jetpackboilerplate.domain

import kotlin.math.*

// Simple data holders for our Reactive System
data class VectorInput(val magnitude: Double, val bearing: Double)
data class CartesianPoint(val x: Float, val y: Float)

class VectorEngine {

    /**
     * Converts a list of movements into a chain of (x, y) points.
     * Uses Unit Circle math: x = d*sin(theta), y = -d*cos(theta)
     */
    fun calculatePath(inputs: List<VectorInput>): List<CartesianPoint> {
        val path = mutableListOf(CartesianPoint(0f, 0f)) // Start at Origin
        var currentX = 0.0
        var currentY = 0.0

        inputs.forEach { input ->
            // Convert degrees to Radians for Kotlin's math library
            val rad = Math.toRadians(input.bearing)
            
            // Map Bearing to Android Canvas: 
            // 0° (North) = Up (-Y), 90° (East) = Right (+X)
            currentX += input.magnitude * sin(rad)
            currentY -= input.magnitude * cos(rad) 
            
            path.add(CartesianPoint(currentX.toFloat(), currentY.toFloat()))
        }
        return path
    }

    /**
     * The "Solve" Node: Calculates the final Resultant 
     * from the start point to the last point in the chain.
     */
    fun getResultant(finalPoint: CartesianPoint): VectorInput {
        val magnitude = sqrt(finalPoint.x.toDouble().pow(2) + finalPoint.y.toDouble().pow(2))
        
        // Atan2 gives the angle in radians; we convert back to degrees
        var angle = Math.toDegrees(atan2(finalPoint.x.toDouble(), -finalPoint.y.toDouble()))
        
        // Ensure the angle is always between 0 and 360
        if (angle < 0) angle += 360.0
        
        return VectorInput(magnitude, angle)
    }
}
