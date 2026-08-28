// com/pasindu/nursingotapp/logic/CalculationEngine.kt
package com.pasindu.nursingotapp.logic

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.DailyLog
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator
import java.time.LocalDate

/**
 * Legacy claim adapter.
 *
 * Database/UI layers must not implement their own 36-hour calculation.
 * WeeklyOtCalculator is the authoritative source for weekly allocation.
 *
 * IMPORTANT:
 * - OT rate remains user/configured and is never derived from salary here.
 * - PH and DO rates remain independently configurable.
 * - 2027 salary-step matching and PH/DO policy-rate derivation remain in
 *   AdvancedFinanceViewModel/pay-rate data flow and are not changed here.
 */
object CalculationEngine {

    fun processClaimData(
        profileEntity: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate,
        payRates: PayRates? = null
    ): Pair<List<DailyLog>, PeriodSummary> {
        val effectiveOtRate = payRates?.otRate?.takeIf { it > 0.0 }
            ?: profileEntity.otRate.coerceAtLeast(0.0)

        val effectivePhRate = payRates?.phRate?.takeIf { it > 0.0 }
            ?: 0.0

        val effectiveDoRate = payRates?.doRate?.takeIf { it > 0.0 }
            ?: 0.0

        val dailyLogs = entries
            .map { entity ->
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
                    computedNormalHours = entity.normalHours,
                    computedOtHours = entity.otHours
                )
            }
            .sortedBy { it.date }

        val result = WeeklyOtCalculator.calculate(
            logs = dailyLogs,
            claimStart = claimStart,
            claimEnd = claimEnd,
            otRate = effectiveOtRate,
            dayRate = effectivePhRate,
            doRate = effectiveDoRate
        )

        val summary = PeriodSummary(
            totalNormalHours = result.totalNormalHours.toFloat(),
            totalOTHours = result.totalOtHours.toFloat(),
            totalPHDays = result.phDays,
            totalDODays = result.doDays,
            otAmountRs = result.otAmountRs,
            phAmountRs = result.phAmountRs,
            doAmountRs = result.doAmountRs,
            totalAmountRs = result.totalAmountRs
        )

        return Pair(dailyLogs, summary)
    }

    data class PayRates(
        val otRate: Double,
        val phRate: Double,
        val doRate: Double
    )
}
