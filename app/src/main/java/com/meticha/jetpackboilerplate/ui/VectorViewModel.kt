package com.meticha.jetpackboilerplate.ui

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.meticha.jetpackboilerplate.domain.VectorEngine
import com.meticha.jetpackboilerplate.domain.VectorInput
import com.meticha.jetpackboilerplate.domain.CartesianPoint

class VectorViewModel : ViewModel() {
    private val engine = VectorEngine()

    // 1. INPUT: The Linked Vector Chain
    val vectorList = mutableStateListOf<VectorInput>()

    // 2. OUTPUT: The Path (Coordinates for the Canvas)
    // Automatically recalculates whenever vectorList changes
    val pathPoints = derivedStateOf {
        engine.calculatePath(vectorList)
    }

    // 3. HUD: The "Solve" Node (Final Resultant)
    // This gives you the total distance and bearing from Point A to the current end
    val resultant = derivedStateOf {
        val finalPoint = pathPoints.value.lastOrNull() ?: CartesianPoint(0f, 0f)
        engine.getResultant(finalPoint)
    }

    // --- COMMANDS ---

    fun addVector(magnitude: Double, bearing: Double) {
        if (magnitude > 0.0) {
            vectorList.add(VectorInput(magnitude, bearing))
        }
    }

    fun undo() {
        if (vectorList.isNotEmpty()) {
            vectorList.removeAt(vectorList.size - 1)
        }
    }

    fun clearSystem() {
        vectorList.clear()
    }
}
