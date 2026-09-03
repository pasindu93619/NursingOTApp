package com.pasindu.nursingotapp.domain.ot

import com.pasindu.nursingotapp.data.model.DailyLog
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Single source of truth for the nursing weekly normal/OT split.
 *
 * Weekly rule (Sunday-Saturday):
 * 1. Sum recorded normal-duty hours separately.
 * 2. Pay up to 36 hours as normal.
 * 3. When recorded normal duty is below 36 hours, recorded OT fills the
 *    missing normal hours first.
 * 4. Any OT not used for that deficit remains OT.
 * 5. Any normal-duty hours above 36 become OT.
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

        for ((weekStart, weekLogsRaw) in grouped.toSortedMap()) {
            val weekLogs = weekLogsRaw.sortedBy { it.date }
            val normalDutyHours = weekLogs.sumOf { it.computedNormalHours.toDouble().coerceAtLeast(0.0) }
            val recordedOtHours = weekLogs.sumOf { it.computedOtHours.toDouble().coerceAtLeast(0.0) }

            val weekNormal = minOf(normalDutyHours, WEEKLY_NORMAL_LIMIT_HOURS) +
                minOf(recordedOtHours, (WEEKLY_NORMAL_LIMIT_HOURS - normalDutyHours).coerceAtLeast(0.0))
            val weekOt =
                (normalDutyHours - WEEKLY_NORMAL_LIMIT_HOURS).coerceAtLeast(0.0) +
                    (recordedOtHours - minOf(recordedOtHours, (WEEKLY_NORMAL_LIMIT_HOURS - normalDutyHours).coerceAtLeast(0.0)))

            // Allocate the exact weekly payable result back to days.
            // The day-level allocation is only a reporting representation;
            // DailyLog source values remain untouched.
            var normalRemaining = weekNormal
            var otRemaining = weekOt

            for (log in weekLogs) {
                val recordedNormal = log.computedNormalHours.toDouble().coerceAtLeast(0.0)
                val recordedOt = log.computedOtHours.toDouble().coerceAtLeast(0.0)

                val normalFromNormal = minOf(recordedNormal, normalRemaining)
                normalRemaining -= normalFromNormal

                val normalFromOt = minOf(recordedOt, normalRemaining)
                normalRemaining -= normalFromOt

                val normalOverflow = (recordedNormal - normalFromNormal).coerceAtLeast(0.0)
                val remainingOt = (recordedOt - normalFromOt).coerceAtLeast(0.0)

                val otFromNormalOverflow = minOf(normalOverflow, otRemaining)
                otRemaining -= otFromNormalOverflow

                val otFromRecordedOt = minOf(remainingOt, otRemaining)
                otRemaining -= otFromRecordedOt

                allocations += ShiftAllocation(
                    date = log.date,
                    normalHours = normalFromNormal + normalFromOt,
                    otHours = otFromNormalOverflow + otFromRecordedOt
                )
            }

            check(kotlin.math.abs(normalRemaining) < 0.001) {
                "Weekly normal allocation could not be mapped to daily entries."
            }
            check(kotlin.math.abs(otRemaining) < 0.001) {
                "Weekly OT allocation could not be mapped to daily entries."
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

        val phDays = filtered.count {
            it.isPH && !it.isLeave &&
                (it.computedNormalHours > 0f || it.computedOtHours > 0f)
        }
        val doDays = filtered.count {
            it.isDO && !it.isLeave &&
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
