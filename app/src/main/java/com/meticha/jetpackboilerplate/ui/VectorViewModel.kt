package com.meticha.jetpackboilerplate.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.meticha.jetpackboilerplate.domain.VectorEngine
import com.meticha.jetpackboilerplate.domain.VectorInput
import com.meticha.jetpackboilerplate.domain.CartesianPoint
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Navigation shortcuts for fast entry
enum class BearingMode { DIRECT, NE, SE, SW, NW }

class VectorViewModel : ViewModel() {
    private val engine = VectorEngine()

    // --- HUD & CONFIG STATES ---
    // Persistent state for UI toggles
    var selectedUnit by mutableStateOf(MeasurementUnit.CENTIMETERS)
        private set

    var isTextbookMode by mutableStateOf(false)
        private set

    var currentBearingMode by mutableStateOf(BearingMode.DIRECT)
        private set

    // Internal state list for vectors
    private val _vectorList = mutableStateListOf<VectorInput>()
    val vectorList: List<VectorInput> = _vectorList

    // --- REACTIVE DATA PIPELINE ---
    // Derived state prevents recalculation unless the list changes
    val pathPoints = derivedStateOf { 
        engine.calculatePath(_vectorList) 
    }

    val displayResultant = derivedStateOf {
        if (_vectorList.isEmpty()) return@derivedStateOf "AWAITING DATA"
        
        val lastPoint = pathPoints.value.lastOrNull() ?: CartesianPoint(0f, 0f)
        val res = engine.getResultant(lastPoint)
        
        // 1. Precise Conversion Logic
        val distInMeters = res.magnitude * MeasurementUnit.MILES.toMeters
        val convertedDist = distInMeters / selectedUnit.toMeters
        
        // 2. Formatting based on "Textbook Mode"
        if (isTextbookMode) {
            // Round to nearest whole number to match ruler math (Grade 8 standard)
            val roundedDist = convertedDist.roundToInt()
            val roundedBrng = res.bearing.roundToInt() % 360
            String.format(Locale.US, "%d %s @ %d°", roundedDist, selectedUnit.suffix, roundedBrng)
        } else {
            // High-precision mode for development and engineering
            String.format(Locale.US, "%.2f %s @ %.1f°", convertedDist, selectedUnit.suffix, res.bearing)
        }
    }

    // --- LOGIC COMMANDS ---
    
    fun toggleTextbookMode() { isTextbookMode = !isTextbookMode }

    fun setBearingMode(mode: BearingMode) { currentBearingMode = mode }

    /**
     * Adds a vector using quadrant shortcuts.
     * Example: Mag 10, Brng 30 in SE mode -> 150° Azimuth
     */
    fun addVector(mag: Double?, brng: Double?) {
        if (mag != null && brng != null && mag > 0) {
            val finalBearing = when (currentBearingMode) {
                BearingMode.NE -> brng          // North -> East
                BearingMode.SE -> 180.0 - brng  // South -> East
                BearingMode.SW -> 180.0 + brng  // South -> West
                BearingMode.NW -> 360.0 - brng  // North -> West
                BearingMode.DIRECT -> brng      // Standard 0-360
            }
            
            // Normalize bearing to 0-359 range
            val normalizedBearing = (finalBearing % 360 + 360) % 360
            _vectorList.add(VectorInput(mag, normalizedBearing))
            
            // Auto-reset to DIRECT to prevent user entry errors
            currentBearingMode = BearingMode.DIRECT 
        }
    }

    fun setUnit(unit: MeasurementUnit) { selectedUnit = unit }

    fun undoLast() {
        if (_vectorList.isNotEmpty()) _vectorList.removeAt(_vectorList.size - 1)
    }

    fun clearSystem() {
        _vectorList.clear()
        currentBearingMode = BearingMode.DIRECT
    }
}
