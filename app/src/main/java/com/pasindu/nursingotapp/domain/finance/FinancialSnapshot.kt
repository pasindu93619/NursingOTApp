package com.pasindu.nursingotapp.domain.finance

import java.time.LocalDate

/**
 * Immutable authoritative result consumed by all finance-facing presentation/reporting layers.
 *
 * This type contains results, not business-rule implementations.
 */
data class FinancialSnapshot(
    val claimStart: LocalDate,
    val claimEnd: LocalDate,

    // Work inputs/results
    val normalDutyHours: Double,
    val dutyGeneratedOtHours: Double,
    val recordedOtHours: Double,
    val totalOtHours: Double,
    val totalDutyHours: Double,

    // Special-day results
    val publicHolidayDays: Int,
    val workingDayOffDays: Int,

    // Salary / rates
    val currentBasicSalary: Double,
    val basisSalary2027: Double?,
    val otRate: Double,
    val phRate: Double,
    val doRate: Double,

    // Earnings
    val basicEarnings: Double,
    val riskAllowance: Double,
    val claAllowance: Double,
    val additionalAllowances: Double,
    val otEarnings: Double,
    val phEarnings: Double,
    val doEarnings: Double,
    val grossEarnings: Double,

    // Deductions
    val totalDeductions: Double,

    // Final result
    val netPay: Double
) {
    init {
        require(!claimEnd.isBefore(claimStart)) { "Claim end date cannot be before claim start date." }
        require(normalDutyHours >= 0.0)
        require(dutyGeneratedOtHours >= 0.0)
        require(recordedOtHours >= 0.0)
        require(totalOtHours >= 0.0)
        require(publicHolidayDays >= 0)
        require(workingDayOffDays >= 0)
        require(currentBasicSalary >= 0.0)
        require(otRate >= 0.0)
        require(phRate >= 0.0)
        require(doRate >= 0.0)
        require(totalDeductions >= 0.0)

        val expectedOt = dutyGeneratedOtHours + recordedOtHours
        require(kotlin.math.abs(totalOtHours - expectedOt) < 0.001) {
            "FinancialSnapshot OT components must reconcile."
        }

        require(kotlin.math.abs(
            grossEarnings - (
                basicEarnings +
                    riskAllowance +
                    claAllowance +
                    additionalAllowances +
                    otEarnings +
                    phEarnings +
                    doEarnings
                )
        ) < 0.01) {
            "FinancialSnapshot gross earnings do not reconcile."
        }

        require(kotlin.math.abs(netPay - (grossEarnings - totalDeductions)) < 0.01) {
            "FinancialSnapshot net pay does not reconcile."
        }
    }

    val overtimeTotalHours: Double get() = totalOtHours
    val otAmountRs: Double get() = otEarnings
    val phAmountRs: Double get() = phEarnings
    val doAmountRs: Double get() = doEarnings
    val totalSpecialDayEarnings: Double get() = phEarnings + doEarnings
}
