package com.pasindu.nursingotapp.domain.calculation

/**
 * Pure calculation helpers used by the Advanced Finance module.
 *
 * IMPORTANT BUSINESS RULES:
 * - OT rate is NOT derived from basic salary in the health-sector workflow.
 *   The user supplies the currently applicable OT rate.
 * - PH and DO rates are also user supplied for now.
 * - The stored/current basic salary may later be used to derive future PH/DO
 *   and OT rates when the 2027 government policy is applicable, but that rule
 *   is deliberately not activated here yet.
 */
object FinancialCalculationEngineV2 {

    fun otAmount(
        otHours: Double,
        otRate: Double
    ): Double {
        require(otHours >= 0.0) { "OT hours cannot be negative." }
        require(otRate >= 0.0) { "OT rate cannot be negative." }
        return otHours * otRate
    }

    fun phAmount(
        phDays: Double,
        phRate: Double
    ): Double {
        require(phDays >= 0.0) { "PH days cannot be negative." }
        require(phRate >= 0.0) { "PH rate cannot be negative." }
        return phDays * phRate
    }

    fun doAmount(
        doDays: Double,
        doRate: Double
    ): Double {
        require(doDays >= 0.0) { "DO days cannot be negative." }
        require(doRate >= 0.0) { "DO rate cannot be negative." }
        return doDays * doRate
    }

    fun gross(
        currentBasicSalary: Double,
        otHours: Double,
        otRate: Double,
        phDays: Double,
        phRate: Double,
        doDays: Double,
        doRate: Double,
        allowances: Double = 0.0
    ): Double {
        require(currentBasicSalary >= 0.0) {
            "Current basic salary cannot be negative."
        }
        require(allowances >= 0.0) {
            "Allowances cannot be negative."
        }

        return currentBasicSalary +
            allowances +
            otAmount(otHours, otRate) +
            phAmount(phDays, phRate) +
            doAmount(doDays, doRate)
    }

    fun net(
        gross: Double,
        deductions: Double
    ): Double {
        require(gross >= 0.0) { "Gross salary cannot be negative." }
        require(deductions >= 0.0) { "Deductions cannot be negative." }
        return gross - deductions
    }
}
