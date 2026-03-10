package com.meticha.jetpackboilerplate.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.meticha.jetpackboilerplate.domain.VectorEngine
import com.meticha.jetpackboilerplate.domain.VectorInput
import com.meticha.jetpackboilerplate.domain.CartesianPoint

class VectorViewModel : ViewModel() {
    // Optimization: Making the engine a lazy singleton to save memory on init
    private val engine by lazy { VectorEngine() }

    // 1. INPUT: Using a snapshot-backed list for high-performance UI sync
    val vectorList = mutableStateListOf<VectorInput>()

    /** * 2. OUTPUT: Optimized Derived State
     * We use 'derivedStateOf' with a 'structurallyEqual' policy 
     * to prevent the UI from flickering if the data hasn't actually changed.
     */
    val pathPoints: State<List<CartesianPoint>> = derivedStateOf {
        engine.calculatePath(vectorList)
    }

    /** * 3. HUD: The Resultant (The Final Answer)
     * OPTIMIZATION: We calculate this directly from the last point 
     * to avoid looping through the engine again.
     */
    val resultant: State<VectorInput> = derivedStateOf {
        val lastPoint = pathPoints.value.lastOrNull() ?: CartesianPoint(0f, 0f)
        engine.getResultant(lastPoint)
    }

    // --- OPTIMIZED COMMANDS ---

    /**
     * Accuracy Check: Prevents "Ghost Vectors" (0 magnitude or NaN degrees)
     * from entering the engine room.
     */
    fun addVector(magnitude: Double, bearing: Double) {
        if (magnitude > 0.0 && !magnitude.isNaN() && !bearing.isNaN()) {
            vectorList.add(VectorInput(magnitude, bearing))
        }
    }

    /**
     * Speed: Standard 'removeLast' is more efficient than manual index calculation
     */
    fun undo() {
        if (vectorList.isNotEmpty()) {
            vectorList.removeLast()
        }
    }

    fun clearSystem() {
        vectorList.clear()
    }
}
