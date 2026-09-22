package com.pasindu.nursingotapp.domain.finance

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.domain.model.DailyLog
import com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator
import java.time.LocalDate

/**
 * Authoritative deterministic finance calculator.
 *
 * All money and weekly OT results exposed to finance UI/reporting must come from
 * this engine. It never stores data and never reads from Compose state.
 */
object FinanceCalculationEngine {

    fun calculate(
        profile: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate,
        payRates: PayRateSettingsEntity?,
        compensation: ProfileCompensationEntity?
    ): FinancialSnapshot {
        require(!claimEnd.isBefore(claimStart)) {
            "Claim end date cannot be before claim start date."
        }

        val effectiveOtRate = (payRates?.otRate ?: profile.otRate).coerceAtLeast(0.0)
        val effectivePhRate = (payRates?.phRate ?: 0.0).coerceAtLeast(0.0)
        val effectiveDoRate = (payRates?.doRate ?: 0.0).coerceAtLeast(0.0)

        val logs = entries
            .map { it.toDailyLog() }
            .sortedBy { it.date }

        val weekly = WeeklyOtCalculator.calculate(
            logs = logs,
            claimStart = claimStart,
            claimEnd = claimEnd,
            otRate = effectiveOtRate,
            dayRate = effectivePhRate,
            doRate = effectiveDoRate
        )

        val recordedOtHours = logs
            .filter { !it.date.isBefore(claimStart) && !it.date.isAfter(claimEnd) }
            .sumOf { it.computedOtHours.toDouble().coerceAtLeast(0.0) }

        val dutyGeneratedOtHours = (weekly.totalOtHours - recordedOtHours).coerceAtLeast(0.0)
        val normalDutyHours = weekly.totalNormalHours
        val totalDutyHours = logs
            .filter { !it.date.isBefore(claimStart) && !it.date.isAfter(claimEnd) }
            .sumOf {
                it.computedNormalHours.toDouble().coerceAtLeast(0.0) +
                    it.computedOtHours.toDouble().coerceAtLeast(0.0)
            }

        val riskAllowance = compensation?.riskAllowance?.coerceAtLeast(0.0) ?: 0.0
        val claAllowance = compensation?.claAllowance?.coerceAtLeast(0.0) ?: 0.0
        val additionalAllowances = compensation?.additionalAllowancesTotal?.coerceAtLeast(0.0) ?: 0.0
        val deductions = compensation?.totalDeductions?.coerceAtLeast(0.0) ?: 0.0
        val basicSalary = profile.basicSalary.coerceAtLeast(0.0)

        val basicEarnings = basicSalary
        val otEarnings = weekly.totalOtHours * effectiveOtRate
        val phEarnings = weekly.phDays * effectivePhRate
        val doEarnings = weekly.doDays * effectiveDoRate
        val gross = basicEarnings +
            riskAllowance +
            claAllowance +
            additionalAllowances +
            otEarnings +
            phEarnings +
            doEarnings
        val net = gross - deductions

        return FinancialSnapshot(
            claimStart = claimStart,
            claimEnd = claimEnd,
            normalDutyHours = normalDutyHours,
            dutyGeneratedOtHours = dutyGeneratedOtHours,
            recordedOtHours = recordedOtHours,
            totalOtHours = weekly.totalOtHours,
            totalDutyHours = totalDutyHours,
            publicHolidayDays = weekly.phDays,
            workingDayOffDays = weekly.doDays,
            currentBasicSalary = basicSalary,
            basisSalary2027 = payRates?.basisSalary2027,
            otRate = effectiveOtRate,
            phRate = effectivePhRate,
            doRate = effectiveDoRate,
            basicEarnings = basicEarnings,
            riskAllowance = riskAllowance,
            claAllowance = claAllowance,
            additionalAllowances = additionalAllowances,
            otEarnings = otEarnings,
            phEarnings = phEarnings,
            doEarnings = doEarnings,
            grossEarnings = gross,
            totalDeductions = deductions,
            netPay = net
        )
    }

    private fun DailyEntryEntity.toDailyLog(): DailyLog = DailyLog(
        id = id,
        date = date,
        isPH = isPH,
        isDO = isDO,
        isLeave = isLeave,
        leaveType = leaveType,
        reason = reason,
        wardOverride = wardOverride,
        normalTimeInStr = normalTimeIn,
        normalTimeOutStr = normalTimeOut,
        otTimeInStr = otTimeIn,
        otTimeOutStr = otTimeOut,
        computedNormalHours = normalHours,
        computedOtHours = otHours
    )
}
