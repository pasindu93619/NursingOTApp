package com.pasindu.nursingotapp.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialFormatUtilsTest {

    @Test
    fun roundMoney_roundsToTwoDecimalPlaces_halfUp() {
        assertEquals(1884.00, FinancialFormatUtils.roundMoney(1884.0), 0.0001)
        assertEquals(1830.67, FinancialFormatUtils.roundMoney(1830.6666667), 0.0001)
        assertEquals(100.01, FinancialFormatUtils.roundMoney(100.005), 0.0001)
        assertEquals(100.00, FinancialFormatUtils.roundMoney(100.004), 0.0001)
        assertEquals(0.00, FinancialFormatUtils.roundMoney(0.0), 0.0001)
        assertEquals(0.00, FinancialFormatUtils.roundMoney(Double.NaN), 0.0001)
        assertEquals(0.00, FinancialFormatUtils.roundMoney(Double.POSITIVE_INFINITY), 0.0001)
    }

    @Test
    fun formatMoney_producesCleanTwoDecimalStringWithoutCommas() {
        assertEquals("1884.00", FinancialFormatUtils.formatMoney(1884.0))
        assertEquals("56520.00", FinancialFormatUtils.formatMoney(56520.0))
        assertEquals("1830.67", FinancialFormatUtils.formatMoney(1830.6666667))
        assertEquals("0.00", FinancialFormatUtils.formatMoney(0.0))
    }

    @Test
    fun formatMoneyWithCommas_producesStandardGrouping() {
        assertEquals("1,884.00", FinancialFormatUtils.formatMoneyWithCommas(1884.0))
        assertEquals("56,520.00", FinancialFormatUtils.formatMoneyWithCommas(56520.0))
        assertEquals("1,830.67", FinancialFormatUtils.formatMoneyWithCommas(1830.6666667))
    }

    @Test
    fun formatRs_prependsCurrencyPrefix() {
        assertEquals("Rs. 1,884.00", FinancialFormatUtils.formatRs(1884.0))
        assertEquals("Rs. 56,520.00", FinancialFormatUtils.formatRs(56520.0))
        assertEquals("Rs. 0.00", FinancialFormatUtils.formatRs(0.0))
    }
}
