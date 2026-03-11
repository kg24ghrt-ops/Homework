package com.meticha.jetpackboilerplate.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.meticha.jetpackboilerplate.domain.VectorEngine
import com.meticha.jetpackboilerplate.domain.TextbookEngine
import com.meticha.jetpackboilerplate.domain.VectorInput
import com.meticha.jetpackboilerplate.domain.CartesianPoint
import com.meticha.jetpackboilerplate.domain.MeasurementUnit
import java.util.Locale
import kotlin.math.roundToInt

enum class BearingMode { DIRECT, NE, SE, SW, NW }

class VectorViewModel : ViewModel() {
    private val raptorEngine = VectorEngine()
    private val notebookEngine = TextbookEngine()

    // --- HUD & CONFIG STATES ---
    var selectedUnit by mutableStateOf(MeasurementUnit.METERS)
        private set

    var isTextbookMode by mutableStateOf(false)
        private set

    var currentBearingMode by mutableStateOf(BearingMode.DIRECT)
        private set

    /** * RULER PRECISION (in meters)
     * This represents the smallest line on your ruler (1mm).
     * If scale is 1cm = 100m, then 1mm = 10m.
     */
    var rulerPrecision by mutableStateOf(10.0) 
        private set

    private val _vectorList = mutableStateListOf<VectorInput>()
    val vectorList: List<VectorInput> = _vectorList

    // --- REACTIVE DATA PIPELINE ---
    val pathPoints = derivedStateOf { 
        raptorEngine.calculatePath(_vectorList) 
    }

    val displayResultant = derivedStateOf {
        if (_vectorList.isEmpty()) return@derivedStateOf "AWAITING DATA"
        
        // 1. RAPTOR ENGINE (High Precision Truth)
        // Raptor works in MILES internally.
        val raptorRes = raptorEngine.calculateScientificResultant(_vectorList)
        val totalMeters = raptorRes.magnitude * MeasurementUnit.MILES.toMeters 
        val preciseDistInUnit = totalMeters / selectedUnit.toMeters

        if (isTextbookMode) {
            // 2. NOTEBOOK ENGINE (Simulates Drawing & Snapping)
            // We pass rulerPrecision in meters to keep math consistent.
            val notebookRes = notebookEngine.calculateNotebookResultant(
                inputs = _vectorList,
                rulerSnapMeters = rulerPrecision
            )
            
            // Convert the snapped magnitude to the display unit
            val notebookDistInUnit = (notebookRes.magnitude * MeasurementUnit.MILES.toMeters) / selectedUnit.toMeters
            
            val heading = formatTextbookHeading(notebookRes.bearing)
            
            // Determine decimal places: 0 for M/FT, 1 or 2 for KM/MI
            val format = if (selectedUnit.toMeters >= 10.0) "%.0f" else "%.1f"
            String.format(Locale.US, "HW: $format %s @ %s", 
                notebookDistInUnit, selectedUnit.suffix, heading)
        } else {
            // High-precision output for development
            String.format(Locale.US, "RAPTOR: %.4f %s @ %.2f°", 
                preciseDistInUnit, selectedUnit.suffix, raptorRes.bearing)
        }
    }

    // --- LOGIC COMMANDS ---
    
    fun toggleTextbookMode() { isTextbookMode = !isTextbookMode }

    /**
     * Updates the scale. 
     * @param mPerCm The "Meters per Centimeter" value from the user (e.g. 100)
     */
    fun updateScale(mPerCm: Double) {
        // We set ruler precision to the 1mm equivalent (mPerCm / 10)
        rulerPrecision = mPerCm / 10.0
    }

    fun setBearingMode(mode: BearingMode) { currentBearingMode = mode }

    fun addVector(mag: Double?, brng: Double?) {
        if (mag != null && brng != null && mag > 0) {
            val finalBearing = when (currentBearingMode) {
                BearingMode.NE -> brng
                BearingMode.SE -> 180.0 - brng
                BearingMode.SW -> 180.0 + brng
                BearingMode.NW -> 360.0 - brng
                BearingMode.DIRECT -> brng
            }
            val normalizedBearing = (finalBearing % 360 + 360) % 360
            _vectorList.add(VectorInput(mag, normalizedBearing))
            currentBearingMode = BearingMode.DIRECT 
        }
    }

    /**
     * Formats Azimuth (0-360) into Quadrant Heading (e.g. S 45 E)
     */
    private fun formatTextbookHeading(bearing: Double): String {
        val b = bearing.roundToInt() % 360
        return when {
            b <= 90 -> "N ${b} E"
            b <= 180 -> "S ${180 - b} E"
            b <= 270 -> "S ${b - 180} W"
            else -> "N ${360 - b} W"
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
