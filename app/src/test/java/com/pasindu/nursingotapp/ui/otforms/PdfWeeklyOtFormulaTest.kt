package com.pasindu.nursingotapp.ui.otforms

import com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfWeeklyOtFormulaTest {

    @Test
    fun weekOne_42Duty18RecordedOt_produces24TrueOt() {
        val trueOt = trueOtHours(42.0, 18.0)
        assertEquals(24.0, trueOt, 0.001)
    }

    @Test
    fun weekTwo_30Duty18RecordedOt_produces12TrueOt() {
        val trueOt = trueOtHours(30.0, 18.0)
        assertEquals(12.0, trueOt, 0.001)
    }

    @Test
    fun under36DutyWithInsufficientOtCannotBecomeNegative() {
        val trueOt = trueOtHours(30.0, 4.0)
        assertEquals(0.0, trueOt, 0.001)
    }

    private fun trueOtHours(totalDutyHours: Double, totalOtHours: Double): Double =
        (totalDutyHours - WeeklyOtCalculator.WEEKLY_NORMAL_LIMIT_HOURS + totalOtHours)
            .coerceAtLeast(0.0)
}
