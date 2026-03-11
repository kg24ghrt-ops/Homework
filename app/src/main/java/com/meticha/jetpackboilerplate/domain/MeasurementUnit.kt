package com.meticha.jetpackboilerplate.domain

/**
 * Measurement systems for VectorNav.
 * All units are mapped to a Meter base for 100% precision in the Raptor Engine.
 */
enum class MeasurementUnit(
    val suffix: String, 
    val label: String,
    val toMeters: Double
) {
    METERS(
        suffix = "m", 
        label = "Meters", 
        toMeters = 1.0
    ),
    KILOMETERS(
        suffix = "km", 
        label = "Kilometers", 
        toMeters = 1000.0
    ),
    CENTIMETERS(
        suffix = "cm", 
        label = "Centimeters", 
        toMeters = 0.01
    ),
    MILES(
        suffix = "mi", 
        label = "Miles", 
        toMeters = 1609.34
    ),
    FEET(
        suffix = "ft", 
        label = "Feet", 
        toMeters = 0.3048
    ),
    INCHES(
        suffix = "in", 
        label = "Inches", 
        toMeters = 0.0254
    ),
    NAUTICAL_MILES(
        suffix = "nm", 
        label = "Nautical Miles", 
        toMeters = 1852.0
    );

    companion object {
        /**
         * Converts a value from one unit to another with high precision.
         */
        fun convert(value: Double, from: MeasurementUnit, to: MeasurementUnit): Double {
            val valueInMeters = value * from.toMeters
            return valueInMeters / to.toMeters
        }
    }
}
