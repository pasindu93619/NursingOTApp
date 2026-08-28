package com.pasindu.nursingotapp.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculationEngineV2Test {

    @Test
    fun otAmount_usesUserSuppliedHealthSectorRate() {
        assertEquals(
            9_000.0,
            FinancialCalculationEngineV2.otAmount(
                otHours = 12.0,
                otRate = 750.0
            ),
            0.001
        )
    }

    @Test
    fun phAmount_usesUserSuppliedRate() {
        assertEquals(
            8_000.0,
            FinancialCalculationEngineV2.phAmount(
                phDays = 2.0,
                phRate = 4_000.0
            ),
            0.001
        )
    }

    @Test
    fun doAmount_usesUserSuppliedRate() {
        assertEquals(
            4_000.0,
            FinancialCalculationEngineV2.doAmount(
                doDays = 1.0,
                doRate = 4_000.0
            ),
            0.001
        )
    }

    @Test
    fun gross_usesCurrentBasicSalaryAndSeparateUserSuppliedRates() {
        val result = FinancialCalculationEngineV2.gross(
            currentBasicSalary = 120_000.0,
            otHours = 10.0,
            otRate = 750.0,
            phDays = 1.0,
            phRate = 4_000.0,
            doDays = 1.0,
            doRate = 4_000.0,
            allowances = 15_000.0
        )

        assertEquals(150_500.0, result, 0.001)
    }

    @Test
    fun net_subtractsDeductions() {
        assertEquals(
            139_500.0,
            FinancialCalculationEngineV2.net(
                gross = 150_500.0,
                deductions = 11_000.0
            ),
            0.001
        )
    }
}
