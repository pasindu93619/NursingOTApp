package com.pasindu.nursingotapp.domain.ot

import com.pasindu.nursingotapp.data.model.DailyLog
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Single source of truth for the legacy 36-hour weekly split.
 *
 * Week boundary is Sunday -> Saturday. Only entries inside the claim period
 * are considered. For each Sunday-based week, the first 36 worked hours are
 * normal and all subsequent worked hours are OT.
 *
 * PH/DO payment-day counts remain independent from the weekly hour split.
 */
object WeeklyOtCalculator {
    const val WEEKLY_NORMAL_LIMIT_HOURS = 36.0

    data class ShiftAllocation(
        val date: LocalDate,
        val normalHours: Double,
        val otHours: Double
    )

    data class WeeklySummary(
        val weekStart: LocalDate,
        val weekEnd: LocalDate,
        val normalHours: Double,
        val otHours: Double
    )

    data class ClaimSummary(
        val allocations: List<ShiftAllocation>,
        val weeklySummaries: List<WeeklySummary>,
        val totalNormalHours: Double,
        val totalOtHours: Double,
        val phDays: Int,
        val doDays: Int,
        val otAmountRs: Double,
        val phAmountRs: Double,
        val doAmountRs: Double,
        val totalAmountRs: Double
    )

    /**
     * Recalculates all logs from scratch. This makes edits safe: changing a
     * Monday entry automatically changes later OT spillover in that
     * Sunday-based week.
     *
     * otRate remains an independently configured Health-sector OT rate.
     * PH and DO may use separate configured rates; when doRate is omitted,
     * the historical single dayRate is used for both.
     */
    fun calculate(
        logs: List<DailyLog>,
        claimStart: LocalDate,
        claimEnd: LocalDate,
        otRate: Double,
        dayRate: Double,
        doRate: Double = dayRate
    ): ClaimSummary {
        require(!claimEnd.isBefore(claimStart)) {
            "Claim end date cannot be before claim start date."
        }
        require(otRate >= 0.0) { "OT rate cannot be negative." }
        require(dayRate >= 0.0) { "PH rate cannot be negative." }
        require(doRate >= 0.0) { "DO rate cannot be negative." }

        val filtered = logs
            .filter { !it.date.isBefore(claimStart) && !it.date.isAfter(claimEnd) }
            .sortedBy { it.date }

        val grouped = filtered.groupBy { sundayOfWeek(it.date) }
        val allocations = mutableListOf<ShiftAllocation>()
        val weeklySummaries = mutableListOf<WeeklySummary>()

        for ((weekStart, weekLogs) in grouped.toSortedMap()) {
            var normalRemaining = WEEKLY_NORMAL_LIMIT_HOURS
            var weekNormal = 0.0
            var weekOt = 0.0

            for (log in weekLogs.sortedBy { it.date }) {
                val workedHours = ShiftHoursCalculator.snapToQuarterHour(
                    (log.computedNormalHours + log.computedOtHours)
                        .toDouble()
                        .coerceAtLeast(0.0)
                )

                val normal = minOf(workedHours, normalRemaining)
                val ot = workedHours - normal
                normalRemaining -= normal
                weekNormal += normal
                weekOt += ot

                allocations += ShiftAllocation(
                    date = log.date,
                    normalHours = normal,
                    otHours = ot
                )
            }

            weeklySummaries += WeeklySummary(
                weekStart = weekStart,
                weekEnd = weekStart.plusDays(6),
                normalHours = weekNormal,
                otHours = weekOt
            )
        }

        val totalNormal = allocations.sumOf { it.normalHours }
        val totalOt = allocations.sumOf { it.otHours }

        // Preserve legacy payment semantics: PH/DO count only when the
        // corresponding day was actually worked, not merely flagged.
        val phDays = filtered.count {
            it.isPH &&
                !it.isLeave &&
                (it.computedNormalHours > 0f || it.computedOtHours > 0f)
        }
        val doDays = filtered.count {
            it.isDO &&
                !it.isLeave &&
                (it.computedNormalHours > 0f || it.computedOtHours > 0f)
        }

        val otAmount = totalOt * otRate
        val phAmount = phDays * dayRate
        val doAmount = doDays * doRate

        return ClaimSummary(
            allocations = allocations,
            weeklySummaries = weeklySummaries,
            totalNormalHours = totalNormal,
            totalOtHours = totalOt,
            phDays = phDays,
            doDays = doDays,
            otAmountRs = otAmount,
            phAmountRs = phAmount,
            doAmountRs = doAmount,
            totalAmountRs = otAmount + phAmount + doAmount
        )
    }

    private fun sundayOfWeek(date: LocalDate): LocalDate {
        val daysFromSunday = when (date.dayOfWeek) {
            DayOfWeek.SUNDAY -> 0L
            DayOfWeek.MONDAY -> 1L
            DayOfWeek.TUESDAY -> 2L
            DayOfWeek.WEDNESDAY -> 3L
            DayOfWeek.THURSDAY -> 4L
            DayOfWeek.FRIDAY -> 5L
            DayOfWeek.SATURDAY -> 6L
        }
        return date.minusDays(daysFromSunday)
    }
}
