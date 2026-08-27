// com/pasindu/nursingotapp/logic/CalculationEngine.kt
package com.pasindu.nursingotapp.logic

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.DailyLog
import com.pasindu.nursingotapp.data.model.PeriodSummary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object CalculationEngine {

    /**
     * Calculates claim data using the legacy PH/DO rule by default:
     * basic salary / 30.
     *
     * Optional pay rates allow Finance to provide a separately configured
     * OT/PH/Working-DO policy without changing DailyEntryEntity or
     * ClaimPeriodEntity.
     */
    fun processClaimData(
        profileEntity: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate,
        payRates: PayRates? = null
    ): Pair<List<DailyLog>, PeriodSummary> {

        val defaultDayRate = if (profileEntity.basicSalary > 0) {
            profileEntity.basicSalary / 30.0
        } else {
            0.0
        }

        val effectiveOtRate = payRates?.otRate?.takeIf { it > 0.0 }
            ?: profileEntity.otRate

        val effectivePhRate = payRates?.phRate?.takeIf { it > 0.0 }
            ?: defaultDayRate

        val effectiveDoRate = payRates?.doRate?.takeIf { it > 0.0 }
            ?: defaultDayRate

        // Step 1: Map Database Entities to DailyLogs
        val dailyLogs = entries.map { entity ->
            DailyLog(
                id = entity.id,
                date = entity.date,
                isPH = entity.isPH,
                isDO = entity.isDO,
                isLeave = entity.isLeave,
                leaveType = entity.leaveType,
                reason = entity.reason,
                wardOverride = entity.wardOverride,
                normalTimeInStr = entity.normalTimeIn,
                normalTimeOutStr = entity.normalTimeOut,
                otTimeInStr = entity.otTimeIn,
                otTimeOutStr = entity.otTimeOut,
                // Payable leave hours continue to count toward the 36-hour rule.
                computedNormalHours = entity.normalHours,
                computedOtHours = entity.otHours
            )
        }.sortedBy { it.date }

        // Step 2: Strict 36-hour rule, resetting every Sunday.
        val firstSunday = claimStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val lastSaturday = claimEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

        var grandTotalOTHours = 0f

        if (!firstSunday.isAfter(lastSaturday)) {
            val fullWeekLogs = dailyLogs.filter {
                !it.date.isBefore(firstSunday) && !it.date.isAfter(lastSaturday)
            }

            val weekGroups = fullWeekLogs.groupBy {
                it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            }

            for ((_, weekLogs) in weekGroups) {
                val weekNormalHours = weekLogs
                    .sumOf { it.computedNormalHours.toDouble() }
                    .toFloat()

                val weekExplicitOTHours = weekLogs
                    .sumOf { it.computedOtHours.toDouble() }
                    .toFloat()

                grandTotalOTHours += weekExplicitOTHours

                if (weekNormalHours > 36f) {
                    grandTotalOTHours += weekNormalHours - 36f
                }
            }
        }

        // Step 3: Count only worked PH and worked DO days.
        val totalWorkingPH = dailyLogs.count {
            it.isPH &&
                !it.isLeave &&
                (it.computedNormalHours > 0f || it.computedOtHours > 0f)
        }

        val totalWorkingDO = dailyLogs.count {
            it.isDO &&
                !it.isLeave &&
                (it.computedNormalHours > 0f || it.computedOtHours > 0f)
        }

        // Step 4: Money calculation using the effective policy rates.
        val otAmount = grandTotalOTHours * effectiveOtRate
        val phAmount = totalWorkingPH * effectivePhRate
        val doAmount = totalWorkingDO * effectiveDoRate

        val summary = PeriodSummary(
            totalNormalHours = dailyLogs
                .sumOf { it.computedNormalHours.toDouble() }
                .toFloat(),
            totalOTHours = grandTotalOTHours,
            totalPHDays = totalWorkingPH,
            totalDODays = totalWorkingDO,
            otAmountRs = otAmount,
            phAmountRs = phAmount,
            doAmountRs = doAmount,
            totalAmountRs = otAmount + phAmount + doAmount
        )

        return Pair(dailyLogs, summary)
    }

    data class PayRates(
        val otRate: Double,
        val phRate: Double,
        val doRate: Double
    )
}
