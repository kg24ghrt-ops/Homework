package com.meticha.jetpackboilerplate.ui

enum class MeasurementUnit(val label: String, val suffix: String, val toMeters: Double) {
    MILES("Miles", "mi", 1609.34),
    METERS("Meters", "m", 1.0),
    KILOMETERS("Kilometers", "km", 1000.0),
    CENTIMETERS("Centimeters", "cm", 0.01),
    FEET("Feet", "ft", 0.3048)
}
