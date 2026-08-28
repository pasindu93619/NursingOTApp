package com.pasindu.nursingotapp.domain.ot

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Pure time calculation utilities for the OT claim engine.
 *
 * All times use 24-hour HH:mm notation. End times earlier than start times
 * are treated as crossing midnight.
 */
object ShiftHoursCalculator {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun minutesBetween(start: LocalTime, end: LocalTime): Long {
        var minutes = Duration.between(start, end).toMinutes()
        if (minutes < 0) minutes += 24L * 60L
        require(minutes >= 0L) { "Shift duration cannot be negative." }
        return minutes
    }

    fun hoursBetween(start: LocalTime, end: LocalTime): Double =
        minutesBetween(start, end) / 60.0

    fun hoursBetween(start: String, end: String): Double {
        return try {
            hoursBetween(LocalTime.parse(start.trim(), formatter), LocalTime.parse(end.trim(), formatter))
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Times must use HH:mm format: '$start' - '$end'", e)
        }
    }

    /**
     * Returns a payroll-friendly quarter-hour value.
     *
     * The OT form workflow should collect only whole 15-minute increments.
     * Values passed here are rounded to the nearest quarter hour so that
     * calculations remain deterministic even when input originates elsewhere.
     */
    fun snapToQuarterHour(hours: Double): Double {
        require(hours >= 0.0) { "Hours cannot be negative." }
        return kotlin.math.round(hours * 4.0) / 4.0
    }
}
