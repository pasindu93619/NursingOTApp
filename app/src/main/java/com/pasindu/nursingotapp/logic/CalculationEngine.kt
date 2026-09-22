package com.pasindu.nursingotapp.logic

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.DailyLog
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.domain.finance.FinanceCalculationEngine
import com.pasindu.nursingotapp.domain.finance.FinancialSnapshot
import java.time.LocalDate

/**
 * Compatibility adapter for legacy callers.
 *
 * No business rules live here. All weekly OT and financial math comes from
 * FinanceCalculationEngine / WeeklyOtCalculator.
 */
object CalculationEngine {

    fun processClaimData(
        profileEntity: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate,
        payRates: PayRates? = null
    ): Pair<List<DailyLog>, PeriodSummary> {
        val snapshot = FinanceCalculationEngine.calculate(
            profile = profileEntity,
            entries = entries,
            claimStart = claimStart,
            claimEnd = claimEnd,
            payRates = payRates?.let {
                com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity(
                    id = 1,
                    otRate = it.otRate,
                    phRate = it.phRate,
                    doRate = it.doRate,
                    rateSource = "LEGACY_ADAPTER"
                )
            },
            compensation = null
        )
        val logs = entries.map { entity ->
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
        return logs to snapshot.toPeriodSummary()
    }

    data class PayRates(
        val otRate: Double,
        val phRate: Double,
        val doRate: Double
    )

    private fun FinancialSnapshot.toPeriodSummary() = PeriodSummary(
        totalNormalHours = normalDutyHours.toFloat(),
        totalOTHours = totalOtHours.toFloat(),
        totalPHDays = publicHolidayDays,
        totalDODays = workingDayOffDays,
        otAmountRs = otEarnings,
        phAmountRs = phEarnings,
        doAmountRs = doEarnings,
        totalAmountRs = otEarnings + phEarnings + doEarnings
    )
}
