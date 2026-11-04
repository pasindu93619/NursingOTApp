package com.pasindu.nursingotapp.domain.calculation

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Utility functions for rounding and formatting monetary values across the display
 * and PDF boundaries.
 *
 * Implements Step 4.5 from NURSINGOT_FIX_PLAN.md:
 * - Provides [roundMoney] using 2 decimal places with HALF_UP rounding.
 * - Standardizes screen display via [formatRs] ("Rs. 1,884.00").
 * - Standardizes PDF table formatting via [formatMoney] ("1884.00").
 */
object FinancialFormatUtils {

    /**
     * Rounds a monetary amount to 2 decimal places using standard financial HALF_UP rounding.
     */
    fun roundMoney(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Formats a monetary amount to a standard two-decimal string without thousands commas: "1884.00".
     * Ideal for pre-printed form templates with fixed box dimensions (e.g. PDF generation).
     */
    fun formatMoney(value: Double): String {
        val rounded = roundMoney(value)
        return String.format(Locale.US, "%.2f", rounded)
    }

    /**
     * Formats a monetary amount to a standard two-decimal string with thousands commas: "1,884.00".
     */
    fun formatMoneyWithCommas(value: Double): String {
        val rounded = roundMoney(value)
        return String.format(Locale.US, "%,.2f", rounded)
    }

    /**
     * Formats a monetary amount with the "Rs. " currency prefix: "Rs. 1,884.00".
     */
    fun formatRs(value: Double): String = "Rs. ${formatMoneyWithCommas(value)}"
}
