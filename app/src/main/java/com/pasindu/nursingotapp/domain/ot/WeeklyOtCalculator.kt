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
 * 2. The payable normal-duty total for the week is capped at 36 hours.
 * 3. If normal-duty hours are below 36, eligible OT hours are used to fill
 *    the remaining normal-hour requirement first.
 * 4. Any OT hours not needed to fill the 36-hour normal requirement remain OT.
 * 5. Any normal-duty hours above 36 are moved to OT.
 *
 * Example:
 * - Normal = 30h, OT = 10h -> Normal payable = 36h, OT payable = 4h.
 * - Normal = 48h, OT = 6h -> Normal payable = 36h, OT payable = 18h.
 *
 * The OT rate is independently configured by the user. PH/DO payment-day
 * counts remain independent from the weekly 36-hour allocation.
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
            val normalDutyHours = weekLogs.sumOf {
                it.computedNormalHours.toDouble().coerceAtLeast(0.0)
            }
            val recordedOtHours = weekLogs.sumOf {
                it.computedOtHours.toDouble().coerceAtLeast(0.0)
            }

            // First determine how much of the normal-duty total is payable as
            // normal. Any normal-duty time above 36 hours becomes OT.
            val payableNormalHours = minOf(normalDutyHours, WEEKLY_NORMAL_LIMIT_HOURS)
            val normalDutyOverflowOt = (normalDutyHours - WEEKLY_NORMAL_LIMIT_HOURS)
                .coerceAtLeast(0.0)

            // If normal duty is below 36 hours, eligible recorded OT hours
            // first fill the missing normal-hour requirement.
            val normalDeficit = (WEEKLY_NORMAL_LIMIT_HOURS - normalDutyHours)
                .coerceAtLeast(0.0)
            val otUsedToFillNormal = minOf(recordedOtHours, normalDeficit)
            val remainingRecordedOt = recordedOtHours - otUsedToFillNormal

            val weekNormal = payableNormalHours + otUsedToFillNormal
            val weekOt = normalDutyOverflowOt + remainingRecordedOt

            // Allocate the weekly result back onto the individual days for
            // transparent UI/PDF reporting while preserving source entries.
            var normalRemaining = weekNormal
            var otRemaining = weekOt
            for (log in weekLogs.sortedBy { it.date }) {
                val recordedNormal = log.computedNormalHours.toDouble().coerceAtLeast(0.0)
                val recordedOt = log.computedOtHours.toDouble().coerceAtLeast(0.0)

                val normalAllocation = minOf(recordedNormal, normalRemaining)
                normalRemaining -= normalAllocation

                val otFromNormalOverflow = (recordedNormal - normalAllocation).coerceAtLeast(0.0)
                val otAllocation = minOf(
                    otRemaining,
                    otFromNormalOverflow + recordedOt
                )
                otRemaining -= otAllocation

                allocations += ShiftAllocation(
                    date = log.date,
                    normalHours = normalAllocation,
                    otHours = otAllocation
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
