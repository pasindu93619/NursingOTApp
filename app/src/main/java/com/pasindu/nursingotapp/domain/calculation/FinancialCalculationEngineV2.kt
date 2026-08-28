package com.pasindu.nursingotapp.domain.calculation

/**
 * Pure calculation helpers used by the Advanced Finance module.
 *
 * This class deliberately does not own any UI state or Room state.
 */
object FinancialCalculationEngineV2 {

    fun otRateFromBasicSalary(basicSalary: Double): Double {
        require(basicSalary >= 0.0) { "Basic salary cannot be negative." }
        return basicSalary / 240.0
    }

    fun dayRateFromBasicSalary(basicSalary: Double): Double {
        require(basicSalary >= 0.0) { "Basic salary cannot be negative." }
        return basicSalary / 30.0
    }

    fun otAmount(otHours: Double, otRate: Double): Double {
        require(otHours >= 0.0) { "OT hours cannot be negative." }
        require(otRate >= 0.0) { "OT rate cannot be negative." }
        return otHours * otRate
    }

    fun phAmount(phDays: Double, dayRate: Double): Double {
        require(phDays >= 0.0) { "PH days cannot be negative." }
        require(dayRate >= 0.0) { "PH day rate cannot be negative." }
        return phDays * dayRate
    }

    fun doAmount(doDays: Double, dayRate: Double): Double {
        require(doDays >= 0.0) { "DO days cannot be negative." }
        require(dayRate >= 0.0) { "DO day rate cannot be negative." }
        return doDays * dayRate
    }

    fun gross(
        basicSalary: Double,
        otHours: Double,
        otRate: Double,
        phDays: Double,
        phRate: Double,
        doDays: Double,
        doRate: Double,
        allowances: Double = 0.0
    ): Double {
        require(allowances >= 0.0) { "Allowances cannot be negative." }
        return basicSalary +
            allowances +
            otAmount(otHours, otRate) +
            phAmount(phDays, phRate) +
            doAmount(doDays, doRate)
    }

    fun net(gross: Double, deductions: Double): Double {
        require(gross >= 0.0) { "Gross salary cannot be negative." }
        require(deductions >= 0.0) { "Deductions cannot be negative." }
        return gross - deductions
    }
}
