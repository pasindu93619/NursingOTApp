package com.pasindu.nursingotapp.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculationEngineV2Test {

    @Test
    fun otRateFromBasicSalary_uses240() {
        assertEquals(500.0, FinancialCalculationEngineV2.otRateFromBasicSalary(120_000.0), 0.001)
    }

    @Test
    fun dayRateFromBasicSalary_uses30() {
        assertEquals(4_000.0, FinancialCalculationEngineV2.dayRateFromBasicSalary(120_000.0), 0.001)
    }

    @Test
    fun otAmount_isHoursTimesRate() {
        assertEquals(6_000.0, FinancialCalculationEngineV2.otAmount(12.0, 500.0), 0.001)
    }

    @Test
    fun phAmount_isDaysTimesDayRate() {
        assertEquals(8_000.0, FinancialCalculationEngineV2.phAmount(2.0, 4_000.0), 0.001)
    }

    @Test
    fun doAmount_isDaysTimesDayRate() {
        assertEquals(4_000.0, FinancialCalculationEngineV2.doAmount(1.0, 4_000.0), 0.001)
    }

    @Test
    fun gross_combinesSalaryAllowancesAndServiceAmounts() {
        val result = FinancialCalculationEngineV2.gross(
            basicSalary = 120_000.0,
            otHours = 10.0,
            otRate = 500.0,
            phDays = 1.0,
            phRate = 4_000.0,
            doDays = 1.0,
            doRate = 4_000.0,
            allowances = 15_000.0
        )

        assertEquals(148_000.0, result, 0.001)
    }

    @Test
    fun net_subtractsDeductions() {
        assertEquals(137_000.0, FinancialCalculationEngineV2.net(148_000.0, 11_000.0), 0.001)
    }
}
