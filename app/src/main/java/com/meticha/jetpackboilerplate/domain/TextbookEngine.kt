package com.meticha.jetpackboilerplate.domain

import kotlin.math.*

class TextbookEngine {
    
    /**
     * @param inputs Raw vector data
     * @param rulerSnap The smallest unit a student can measure (e.g., 10.0 for 1mm on 1:100 scale)
     * @param angleSnap The smallest degree a student can measure (usually 1.0)
     */
    fun calculateNotebookResultant(
        inputs: List<VectorInput>,
        rulerSnap: Double,
        angleSnap: Double = 1.0
    ): VectorInput {
        var totalX = 0.0
        var totalY = 0.0

        for (input in inputs) {
            // 1. SNAP INPUTS: Simulate student drawing lines
            val snappedMag = (input.magnitude / rulerSnap).roundToInt() * rulerSnap
            val snappedBrng = (input.bearing / angleSnap).roundToInt() * angleSnap
            
            val rad = (90.0 - snappedBrng) * (PI / 180.0)
            totalX += snappedMag * cos(rad)
            totalY += snappedMag * sin(rad)
        }

        val magnitude = hypot(totalX, totalY)
        val rawBearing = (90.0 - (atan2(totalY, totalX) * 180.0 / PI) + 360.0) % 360.0
        
        // 2. SNAP OUTPUTS: Simulate student reading the final result
        val finalMag = (magnitude / rulerSnap).roundToInt() * rulerSnap
        val finalBrng = (rawBearing / angleSnap).roundToInt() * angleSnap
        
        return VectorInput(finalMag, finalBrng.toDouble())
    }
}
