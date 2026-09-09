package com.pasindu.nursingotapp.ui.screens

import kotlin.math.floor
import kotlin.math.round

/** Deterministic, side-effect-free calculation engine for the Phase 3.4 high-alert tools. */
internal object HighAlertCalculatorMath {
    fun insulinSlidingScaleUnits(bloodGlucoseMgDl: String): Int {
        val bg = bloodGlucoseMgDl.toFloatOrNull() ?: 0f
        return if (bg > 100f) round((bg - 100f) / 10f).toInt() else 0
    }

    fun insulinIvRate(weightKg: String, orderedUnitsPerKgPerHr: String, concentrationUnitsPerMl: String): Double {
        val weight = weightKg.toDoubleOrNull() ?: 0.0
        val dose = orderedUnitsPerKgPerHr.toDoubleOrNull() ?: 0.0
        val concentration = concentrationUnitsPerMl.toDoubleOrNull() ?: 0.0
        if (weight <= 0.0 || dose <= 0.0 || concentration <= 0.0) return 0.0
        return round((weight * dose / concentration) * 10.0) / 10.0
    }

    fun heparinUnitsPerHour(weightKg: String, orderedUnitsPerKgPerHr: String): Double {
        val weight = weightKg.toDoubleOrNull() ?: 0.0
        val dose = orderedUnitsPerKgPerHr.toDoubleOrNull() ?: 0.0
        if (weight <= 0.0 || dose <= 0.0) return 0.0
        return round(weight * dose).toDouble()
    }

    fun heparinPumpRate(unitsPerHour: Double, bagUnits: String, bagVolumeMl: String): Double {
        val units = bagUnits.toDoubleOrNull() ?: 0.0
        val volume = bagVolumeMl.toDoubleOrNull() ?: 0.0
        if (unitsPerHour <= 0.0 || units <= 0.0 || volume <= 0.0) return 0.0
        return round(unitsPerHour * volume / units * 10.0) / 10.0
    }

    fun pcaTheoreticalLockoutCapacity(bolusDose: String, lockoutMinutes: String): Double {
        val dose = bolusDose.toDoubleOrNull() ?: 0.0
        val lockout = lockoutMinutes.toDoubleOrNull() ?: 0.0
        if (dose <= 0.0 || lockout <= 0.0) return 0.0
        val eventsPerHour = floor(60.0 / lockout)
        return round(eventsPerHour * dose * 100.0) / 100.0
    }

    fun pcaTheoreticalEventsPerHour(lockoutMinutes: String): Int {
        val lockout = lockoutMinutes.toDoubleOrNull() ?: 0.0
        return if (lockout > 0.0) floor(60.0 / lockout).toInt() else 0
    }
}
