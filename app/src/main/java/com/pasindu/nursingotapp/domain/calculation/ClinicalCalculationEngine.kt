package com.pasindu.nursingotapp.domain.calculation

import kotlin.math.roundToInt

object ClinicalCalculationEngine {

    // --- IV Drip Metronome Calculations ---

    /**
     * Calculates the drip rate in Drops Per Minute (gtt/min).
     * @param volumeMl Total volume to be infused in mL
     * @param timeMinutes Total time for infusion in minutes
     * @param dropFactor The calibration of the IV tubing (e.g., 15, 20, or 60 gtt/mL)
     */
    fun calculateDripRate(volumeMl: Double, timeMinutes: Double, dropFactor: Int): Int {
        if (timeMinutes <= 0.0) return 0
        return ((volumeMl * dropFactor) / timeMinutes).roundToInt()
    }

    /**
     * Calculates the interval in milliseconds between each drop for the visual metronome.
     */
    fun calculateDropIntervalMs(dropsPerMinute: Int): Long {
        if (dropsPerMinute <= 0) return 0L
        return (60_000L / dropsPerMinute)
    }

    // --- Medication Unit Conversions ---

    fun gramsToMilligrams(grams: Double): Double = grams * 1000.0
    fun milligramsToMicrograms(mg: Double): Double = mg * 1000.0
    fun microgramsToMilligrams(mcg: Double): Double = mcg / 1000.0
    fun milligramsToGrams(mg: Double): Double = mg / 1000.0

    /**
     * Calculates the liquid volume to administer based on ordered dose and on-hand concentration.
     */
    fun calculateDoseVolume(orderedDose: Double, doseOnHand: Double, volumeOnHand: Double): Double {
        if (doseOnHand <= 0.0) return 0.0
        return (orderedDose / doseOnHand) * volumeOnHand
    }

    // --- Clinical Scoring (GCS & Apgar) ---

    fun calculateGcs(eye: Int, verbal: Int, motor: Int): Int {
        return eye.coerceIn(1, 4) + verbal.coerceIn(1, 5) + motor.coerceIn(1, 6)
    }

    fun interpretGcs(score: Int): String {
        return when {
            score <= 8 -> "Severe (Coma)"
            score in 9..12 -> "Moderate"
            score >= 13 -> "Minor/Normal"
            else -> "Unknown"
        }
    }

    fun calculateApgar(appearance: Int, pulse: Int, grimace: Int, activity: Int, respiration: Int): Int {
        return appearance.coerceIn(0, 2) + pulse.coerceIn(0, 2) +
                grimace.coerceIn(0, 2) + activity.coerceIn(0, 2) +
                respiration.coerceIn(0, 2)
    }
}