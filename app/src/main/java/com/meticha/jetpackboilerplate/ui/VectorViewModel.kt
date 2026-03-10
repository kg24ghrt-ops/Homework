package com.meticha.jetpackboilerplate.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.meticha.jetpackboilerplate.domain.VectorEngine
import com.meticha.jetpackboilerplate.domain.VectorInput
import com.meticha.jetpackboilerplate.domain.CartesianPoint
import java.util.Locale

class VectorViewModel : ViewModel() {
    private val engine = VectorEngine()

    // --- HUD STATES ---
    // Defaulting to CM for Version 4.0 as requested
    var selectedUnit by mutableStateOf(MeasurementUnit.CENTIMETERS)
        private set

    private val _vectorList = mutableStateListOf<VectorInput>()
    val vectorList: List<VectorInput> = _vectorList

    // --- OPTIMIZED PIPELINE ---
    // derivedStateOf prevents unnecessary recalculations during UI recomposition
    val pathPoints = derivedStateOf { 
        engine.calculatePath(_vectorList) 
    }

    val displayResultant = derivedStateOf {
        if (_vectorList.isEmpty()) return@derivedStateOf "AWAITING DATA"
        
        val lastPoint = pathPoints.value.lastOrNull() ?: CartesianPoint(0f, 0f)
        val res = engine.getResultant(lastPoint)
        
        // Accurate conversion: (Miles -> Meters -> Target Unit)
        val distInMeters = res.magnitude * MeasurementUnit.MILES.toMeters
        val convertedDist = distInMeters / selectedUnit.toMeters
        
        String.format(
            Locale.US, 
            "%.2f %s @ %.1f°", 
            convertedDist, 
            selectedUnit.suffix, 
            res.bearing
        )
    }

    // --- COMMANDS ---
    fun addVector(mag: Double?, brng: Double?) {
        // Validation: Only add if magnitude is valid and positive
        if (mag != null && brng != null && mag > 0) {
            _vectorList.add(VectorInput(mag, brng))
        }
    }

    fun setUnit(unit: MeasurementUnit) {
        selectedUnit = unit
    }

    fun undoLast() {
        if (_vectorList.isNotEmpty()) _vectorList.removeAt(_vectorList.size - 1)
    }

    fun clearSystem() {
        _vectorList.clear()
    }
}
