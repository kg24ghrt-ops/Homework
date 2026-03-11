package com.meticha.jetpackboilerplate.domain

import kotlin.math.*

/**
 * The TextbookEngine simulates physical measurement limitations.
 * It mimics a student using a ruler and a protractor to "snap" values
 * to the nearest measurable increment.
 */
class TextbookEngine {
    
    /**
     * @param inputs Raw vector data from the Raptor Engine
     * @param rulerSnapMeters The value of the smallest ruler mark (1mm) in meters
     * @param angleSnap The smallest degree a student can measure (usually 1.0)
     */
    fun calculateNotebookResultant(
        inputs: List<VectorInput>,
        rulerSnapMeters: Double,
        angleSnap: Double = 1.0
    ): VectorInput {
        var totalX = 0.0
        var totalY = 0.0

        for (input in inputs) {
            // 1. SNAP INPUTS: Simulate the student drawing the individual vectors
            // We convert to meters, snap to the ruler precision, and convert back if needed
            val snappedMag = (input.magnitude / rulerSnapMeters).roundToInt() * rulerSnapMeters
            val snappedBrng = (input.bearing / angleSnap).roundToInt() * angleSnap
            
            // Convert to Cartesian (Standard math uses 0° at East, we use 0° at North)
            val rad = (90.0 - snappedBrng) * (PI / 180.0)
            totalX += snappedMag * cos(rad)
            totalY += snappedMag * sin(rad)
        }

        val magnitude = hypot(totalX, totalY)
        val rawBearing = (90.0 - (atan2(totalY, totalX) * 180.0 / PI) + 360.0) % 360.0
        
        // 2. SNAP OUTPUTS: Simulate the student measuring the final resultant line
        val finalMag = (magnitude / rulerSnapMeters).roundToInt() * rulerSnapMeters
        val finalBrng = (rawBearing / angleSnap).roundToInt().toDouble()
        
        return VectorInput(finalMag, finalBrng)
    }
}
