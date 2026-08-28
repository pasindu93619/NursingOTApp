package com.pasindu.nursingotapp.domain.calculation

import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity

/**
 * Finds the salary-step row that corresponds to a nurse's current basic salary.
 * No salary-step number is requested from the nurse.
 */
object SalaryStepMatcher {

    fun findExact(
        currentBasicSalary: Double,
        rows: List<SalaryStep2027Entity>
    ): SalaryStep2027Entity? {
        if (currentBasicSalary <= 0.0) return null

        return rows.firstOrNull {
            kotlin.math.abs(it.basicSalary2027 - currentBasicSalary) < 0.01
        }
    }

    fun calculateFutureDayRate(
        basicSalary2027: Double
    ): Double? {
        if (basicSalary2027 <= 0.0) return null
        return basicSalary2027 / 30.0
    }
}
