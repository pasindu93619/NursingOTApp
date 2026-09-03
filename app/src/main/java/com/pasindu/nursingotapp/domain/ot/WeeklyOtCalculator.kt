package com.pasindu.nursingotapp.domain.ot

import com.pasindu.nursingotapp.data.model.DailyLog
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Single source of truth for the nursing weekly normal/OT split.
 *
 * Nursing duty data stores normal-duty hours and OT hours separately because
 * a normal morning/evening/night shift is different from an OT morning/
 * evening/night shift. At claim calculation time, the weekly rule is applied:
 *
 * 1. Sum NORMAL DUTY hours for each Sunday-Saturday week.
 * 2. Pay up to 36 hours as normal.
 * 3. If normal duty is below 36 hours, recorded OT fills the deficit first.
 * 4. Any OT not used for the deficit remains OT.
 * 5. Any normal-duty hours above 36 become OT.
 *
 * The allocation list mirrors the weekly payable totals back to individual
 * days for transparent reporting. Source DailyLog values are never mutated.
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

        for ((weekStart, weekLogs) in grouped.toSortedMap()) {
            val sortedLogs = weekLogs.sortedBy { it.date }
            val normalDutyHours = sortedLogs.sumOf {
                it.computedNormalHours.toDouble().coerceAtLeast(0.0)
            }
            val recordedOtHours = sortedLogs.sumOf {
                it.computedOtHours.toDouble().coerceAtLeast(0.0)
            }

            val payableNormalFromNormalDuty = minOf(
                normalDutyHours,
                WEEKLY_NORMAL_LIMIT_HOURS
            )
            val normalDeficit = (
                WEEKLY_NORMAL_LIMIT_HOURS - normalDutyHours
            ).coerceAtLeast(0.0)
            val otUsedToFillNormal = minOf(recordedOtHours, normalDeficit)
            val remainingRecordedOt = recordedOtHours - otUsedToFillNormal
            val normalDutyOverflowOt = (
                normalDutyHours - WEEKLY_NORMAL_LIMIT_HOURS
            ).coerceAtLeast(0.0)

            val weekNormal = payableNormalFromNormalDuty + otUsedToFillNormal
            val weekOt = normalDutyOverflowOt + remainingRecordedOt

            // Map the exact weekly payable totals back to the source days.
            // Recorded normal hours are consumed into normal first. Recorded OT
            // then fills any remaining normal deficit. Normal overflow and any
            // remaining recorded OT are allocated into payable OT.
            var normalRemaining = weekNormal
            var otRemaining = weekOt

            for (log in sortedLogs) {
                val recordedNormal = log.computedNormalHours
                    .toDouble()
                    .coerceAtLeast(0.0)
                val recordedOt = log.computedOtHours
                    .toDouble()
                    .coerceAtLeast(0.0)

                val normalFromNormalDuty = minOf(recordedNormal, normalRemaining)
                normalRemaining -= normalFromNormalDuty

                val normalFromOt = minOf(recordedOt, normalRemaining)
                normalRemaining -= normalFromOt

                val normalDutyOverflow = (
                    recordedNormal - normalFromNormalDuty
                ).coerceAtLeast(0.0)
                val remainingRecordedOtForDay = (
                    recordedOt - normalFromOt
                ).coerceAtLeast(0.0)

                val otFromNormalOverflow = minOf(normalDutyOverflow, otRemaining)
                otRemaining -= otFromNormalOverflow

                val otFromRecordedOt = minOf(remainingRecordedOtForDay, otRemaining)
                otRemaining -= otFromRecordedOt

                allocations += ShiftAllocation(
                    date = log.date,
                    normalHours = normalFromNormalDuty + normalFromOt,
                    otHours = otFromNormalOverflow + otFromRecordedOt
                )
            }

            check(normalRemaining == 0.0) {
                "Weekly normal allocation could not be mapped to daily entries."
            }
            check(otRemaining == 0.0) {
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
