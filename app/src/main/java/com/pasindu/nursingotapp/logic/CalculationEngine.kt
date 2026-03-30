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

    fun processClaimData(
        profileEntity: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate
    ): Pair<List<DailyLog>, PeriodSummary> {

        val dayRate = if (profileEntity.basicSalary > 0) profileEntity.basicSalary / 30.0 else 0.0

        // Step 1: Map Database Entities to DailyLogs
        val dailyLogs = entries.map { entity ->
            DailyLog(
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
                // CRITICAL FIX: We no longer zero out leave hours! Payable leaves MUST count towards the 36H rule.
                computedNormalHours = entity.normalHours,
                computedOtHours = entity.otHours
            )
        }.sortedBy { it.date }

        // Step 2: 36-Hour Rule Calculation (Strictly Sunday to Saturday)
        val firstSunday = claimStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val lastSaturday = claimEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

        var grandTotalOTHours = 0f

        if (!firstSunday.isAfter(lastSaturday)) {
            val fullWeekLogs = dailyLogs.filter { !it.date.isBefore(firstSunday) && !it.date.isAfter(lastSaturday) }
            val weekGroups = fullWeekLogs.groupBy { it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }

            for ((_, weekLogs) in weekGroups) {
                // Sum all normal hours (including 6H/8H from payable leaves!)
                val weekNormalHours = weekLogs.sumOf { it.computedNormalHours.toDouble() }.toFloat()
                val weekExplicitOTHours = weekLogs.sumOf { it.computedOtHours.toDouble() }.toFloat()

                // Add any explicit OT shifts
                grandTotalOTHours += weekExplicitOTHours

                // Rule: If total normal hours exceed 36, the remainder spills into OT!
                if (weekNormalHours > 36f) {
                    grandTotalOTHours += (weekNormalHours - 36f)
                }
            }
        }

        // Step 3: Count ONLY Worked DO and PH days for the extra day-rate payments
        val totalWorkingPH = dailyLogs.count { it.isPH && !it.isLeave && (it.computedNormalHours > 0f || it.computedOtHours > 0f) }
        val totalWorkingDO = dailyLogs.count { it.isDO && !it.isLeave && (it.computedNormalHours > 0f || it.computedOtHours > 0f) }

        // Step 4: Final Money Calculation
        val summary = PeriodSummary(
            totalNormalHours = dailyLogs.sumOf { it.computedNormalHours.toDouble() }.toFloat(),
            totalOTHours = grandTotalOTHours,
            totalPHDays = totalWorkingPH,
            totalDODays = totalWorkingDO,
            otAmountRs = grandTotalOTHours * profileEntity.otRate,
            phAmountRs = totalWorkingPH * dayRate,
            doAmountRs = totalWorkingDO * dayRate,
            totalAmountRs = (grandTotalOTHours * profileEntity.otRate) + (totalWorkingPH * dayRate) + (totalWorkingDO * dayRate)
        )

        return Pair(dailyLogs, summary)
    }
}