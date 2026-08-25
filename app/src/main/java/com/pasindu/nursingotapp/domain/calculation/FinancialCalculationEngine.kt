package com.pasindu.nursingotapp.domain.calculation

import kotlin.math.max
import kotlin.math.pow

object FinancialCalculationEngine {

    /**
     * Calculates the Advance Personal Income Tax (APIT) based on Sri Lanka Inland Revenue
     * progressive tax brackets (Effective April 2025 onwards).
     * Tax-free allowance: LKR 150,000 per month.
     * Progressive slabs:
     * - Next LKR 83,333.33 @ 6%
     * - Next LKR 41,666.67 @ 18%
     * - Next LKR 41,666.67 @ 24%
     * - Next LKR 41,666.67 @ 30%
     * - Balance @ 36%
     */
    fun calculateApitTax(monthlyTotalIncome: Double): Double {
        if (monthlyTotalIncome <= 150_000.0) return 0.0

        var tax = 0.0
        var taxableIncome = monthlyTotalIncome - 150_000.0

        // Slab 1: 6% on first 83,333.33
        val slab1 = minOf(taxableIncome, 83_333.33)
        tax += slab1 * 0.06
        taxableIncome -= slab1
        if (taxableIncome <= 0) return tax

        // Slab 2: 18% on next 41,666.67
        val slab2 = minOf(taxableIncome, 41_666.67)
        tax += slab2 * 0.18
        taxableIncome -= slab2
        if (taxableIncome <= 0) return tax

        // Slab 3: 24% on next 41,666.67
        val slab3 = minOf(taxableIncome, 41_666.67)
        tax += slab3 * 0.24
        taxableIncome -= slab3
        if (taxableIncome <= 0) return tax

        // Slab 4: 30% on next 41,666.67
        val slab4 = minOf(taxableIncome, 41_666.67)
        tax += slab4 * 0.30
        taxableIncome -= slab4
        if (taxableIncome <= 0) return tax

        // Slab 5: 36% on the balance
        tax += taxableIncome * 0.36

        return tax
    }

    /**
     * Calculates the Widows' and Orphans' Pension (W&OP) deduction.
     * Standard deduction is usually 6% or 7% of the basic salary.
     */
    fun calculateWopDeduction(basicSalary: Double, rate: Double = 0.07): Double {
        return max(0.0, basicSalary * rate)
    }

    /**
     * Estimates monthly loan repayment using the standard amortization formula:
     * A = P * (r(1+r)^n) / ((1+r)^n - 1)
     */
    fun calculateLoanAmortization(principal: Double, annualInterestRate: Double, years: Int): Double {
        if (principal <= 0 || years <= 0) return 0.0
        if (annualInterestRate == 0.0) return principal / (years * 12)

        val monthlyRate = (annualInterestRate / 100) / 12
        val totalMonths = years * 12
        val mathPower = (1 + monthlyRate).pow(totalMonths)

        return principal * (monthlyRate * mathPower) / (mathPower - 1)
    }
}