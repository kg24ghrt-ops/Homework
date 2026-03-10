package com.meticha.jetpackboilerplate.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.meticha.jetpackboilerplate.domain.VectorEngine
import com.meticha.jetpackboilerplate.domain.VectorInput
import com.meticha.jetpackboilerplate.domain.CartesianPoint

// 1. DEFINE THE MEASUREMENT STANDARDS
enum class MeasurementUnit(val suffix: String, val factor: Double) {
    MILES("mi", 1.0),
    METERS("m", 1609.34),
    KILOMETERS("km", 1.60934),
    FEET("ft", 5280.0)
}

class VectorViewModel : ViewModel() {
    private val engine by lazy { VectorEngine() }

    // --- NEW: UNIT STATE ---
    var selectedUnit by mutableStateOf(MeasurementUnit.MILES)
        private set

    val vectorList = mutableStateListOf<VectorInput>()

    // --- CALCULATIONS ---
    val pathPoints: State<List<CartesianPoint>> = derivedStateOf {
        engine.calculatePath(vectorList)
    }

    /** * HUD: The Resultant with Unit Conversion
     * This automatically scales the distance based on the selected unit!
     */
    val displayResultant: State<String> = derivedStateOf {
        val lastPoint = pathPoints.value.lastOrNull() ?: CartesianPoint(0f, 0f)
        val res = engine.getResultant(lastPoint)
        
        val convertedDist = res.magnitude * selectedUnit.factor
        val unitSuffix = selectedUnit.suffix
        
        // Formats to 2 decimal places for precision
        "%.2f %s @ %.1f°".format(convertedDist, unitSuffix, res.bearing)
    }

    // --- COMMANDS ---

    fun setUnit(unit: MeasurementUnit) {
        selectedUnit = unit
    }

    fun addVector(magnitude: Double, bearing: Double) {
        if (magnitude > 0.0 && !magnitude.isNaN() && !bearing.isNaN()) {
            vectorList.add(VectorInput(magnitude, bearing))
        }
    }

    fun undo() {
        if (vectorList.isNotEmpty()) vectorList.removeLast()
    }

    fun clearSystem() {
        vectorList.clear()
    }
}
