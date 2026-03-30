package com.pasindu.nursingotapp.domain.calculation

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Represents a single shift entry for a given calendar date.
 *
 * @property date Shift date.
 * @property startTime Shift start time.
 * @property endTime Shift end time.
 * @property isPH Whether the shift is on a public holiday.
 * @property isDO Whether the shift is on a day off.
 */
data class ShiftEntry(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isPH: Boolean = false,
    val isDO: Boolean = false
)

/**
 * Calculated weekly summary for a full Sunday to Saturday week.
 */
data class WeeklyResult(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val totalWorkedHours: Double,
    val normalHours: Double,
    val otHours: Double
)

/**
 * Core weekly hour calculation utilities.
 */
object WeeklyCalculationEngine {

    /**
     * Calculates shift duration in decimal hours.
     * If the end time is before the start time, the shift crosses midnight.
     */
    fun calculateShiftHours(start: LocalTime, end: LocalTime): Double {
        val startMinutes = start.toSecondOfDay() / 60
        var endMinutes = end.toSecondOfDay() / 60

        if (end < start) {
            endMinutes += 24 * 60
        }

        return (endMinutes - startMinutes) / 60.0
    }

    /**
     * Returns all full Sunday-Saturday weeks fully contained in a claim period.
     * Partial weeks at the beginning or end are ignored (Used for the official PDF Form).
     */
    fun getFullWeeks(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Pair<LocalDate, LocalDate>> {
        if (endDate < startDate) return emptyList()

        val firstSunday = if (startDate.dayOfWeek == DayOfWeek.SUNDAY) {
            startDate
        } else {
            startDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
        }

        val lastSaturday = if (endDate.dayOfWeek == DayOfWeek.SATURDAY) {
            endDate
        } else {
            endDate.with(TemporalAdjusters.previous(DayOfWeek.SATURDAY))
        }

        if (lastSaturday < firstSunday) return emptyList()

        val weeks = mutableListOf<Pair<LocalDate, LocalDate>>()
        var currentStart = firstSunday

        while (!currentStart.isAfter(lastSaturday)) {
            val currentEnd = currentStart.plusDays(6)
            if (!currentEnd.isAfter(lastSaturday)) {
                weeks.add(currentStart to currentEnd)
            }
            currentStart = currentStart.plusWeeks(1)
        }

        return weeks
    }

    /**
     * Calculates weekly worked, normal, and overtime hours for full weeks in a claim period.
     * Uses the strict 36-hour rule (Used for the official PDF Form).
     */
    fun calculateWeeklyHours(
        shifts: List<ShiftEntry>,
        claimStart: LocalDate,
        claimEnd: LocalDate
    ): List<WeeklyResult> {
        val fullWeeks = getFullWeeks(claimStart, claimEnd)

        return fullWeeks.map { (weekStart, weekEnd) ->
            val totalHours = shifts
                .asSequence()
                .filter { shift -> !shift.date.isBefore(weekStart) && !shift.date.isAfter(weekEnd) }
                .sumOf { shift -> calculateShiftHours(shift.startTime, shift.endTime) }

            val normalHours = minOf(totalHours, 36.0)
            val otHours = maxOf(totalHours - 36.0, 0.0)

            WeeklyResult(
                weekStart = weekStart,
                weekEnd = weekEnd,
                totalWorkedHours = totalHours,
                normalHours = normalHours,
                otHours = otHours
            )
        }
    }

    // =========================================================================================
    // SMART INSIGHTS & BURNOUT METER LOGIC (Real Calendar Data)
    // =========================================================================================

    /**
     * NEW: Calculates the actual total worked hours for the exact calendar date range selected.
     * This ignores the strict "Full Week / 36 Hour" PDF rule and simply sums all hours worked
     * between the start and end dates (including partial weeks at the start/end of the month).
     */
    fun calculateTotalHoursForCalendarPeriod(
        shifts: List<ShiftEntry>,
        claimStart: LocalDate,
        claimEnd: LocalDate
    ): Double {
        return shifts
            .filter { !it.date.isBefore(claimStart) && !it.date.isAfter(claimEnd) }
            .sumOf { calculateShiftHours(it.startTime, it.endTime) }
    }

    /**
     * NEW: Calculates the maximum consecutive night shifts in a given list of daily entries.
     * A standard night shift starts in the evening (>= 18:00) and crosses midnight.
     * This checks calendar dates to ensure shifts are truly consecutive.
     */
    fun calculateMaxConsecutiveNightShifts(entries: List<ShiftEntry>): Int {
        if (entries.isEmpty()) return 0

        var maxConsecutive = 0
        var currentConsecutive = 0
        var lastNightShiftDate: LocalDate? = null

        // Sort by date to ensure accurate chronological checking
        val sortedEntries = entries.sortedBy { it.date }

        for (entry in sortedEntries) {
            val start = entry.startTime
            val end = entry.endTime

            // Definition of a Night Shift: Starts after 18:00 (6 PM) and ends the next day
            val isNightShift = start.hour >= 18 && end < start

            if (isNightShift) {
                // If it's the first night shift, OR it's exactly one day after the last night shift
                if (lastNightShiftDate == null || lastNightShiftDate == entry.date.minusDays(1)) {
                    currentConsecutive++
                } else {
                    // There was a gap of unlogged days between night shifts, reset the counter
                    currentConsecutive = 1
                }

                lastNightShiftDate = entry.date

                if (currentConsecutive > maxConsecutive) {
                    maxConsecutive = currentConsecutive
                }
            } else {
                // If they worked a standard day shift or morning shift, break the consecutive chain
                currentConsecutive = 0
                lastNightShiftDate = null
            }
        }
        return maxConsecutive
    }
}