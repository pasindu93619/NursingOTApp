package com.pasindu.nursingotapp.domain.ot

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftHoursCalculatorTest {
    @Test
    fun `same day shift calculates correctly`() {
        assertEquals(6.0, ShiftHoursCalculator.hoursBetween("07:00", "13:00"), 0.001)
    }

    @Test
    fun `cross midnight shift calculates correctly`() {
        assertEquals(12.0, ShiftHoursCalculator.hoursBetween("19:00", "07:00"), 0.001)
    }

    @Test
    fun `quarter hour snapping is deterministic`() {
        assertEquals(6.0, ShiftHoursCalculator.snapToQuarterHour(6.0), 0.001)
        assertEquals(6.25, ShiftHoursCalculator.snapToQuarterHour(6.24), 0.001)
        assertEquals(6.25, ShiftHoursCalculator.snapToQuarterHour(6.26), 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative hours are rejected`() {
        ShiftHoursCalculator.snapToQuarterHour(-0.25)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid time format is rejected`() {
        ShiftHoursCalculator.hoursBetween("7:00", "13:00")
    }
}
