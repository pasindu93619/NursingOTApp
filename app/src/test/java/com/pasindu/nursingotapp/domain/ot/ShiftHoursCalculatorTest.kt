package com.pasindu.nursingotapp.domain.ot

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class ShiftHoursCalculatorTest {
    @Test
    fun `day shift calculates six hours`() {
        assertEquals(6.0, ShiftHoursCalculator.hoursBetween(LocalTime.of(7, 0), LocalTime.of(13, 0)), 0.001)
    }

    @Test
    fun `cross midnight night shift calculates twelve hours`() {
        assertEquals(12.0, ShiftHoursCalculator.hoursBetween(LocalTime.of(19, 0), LocalTime.of(7, 0)), 0.001)
    }

    @Test
    fun `forty five minutes becomes zero point seven five hours`() {
        assertEquals(0.75, ShiftHoursCalculator.hoursBetween(LocalTime.of(7, 0), LocalTime.of(7, 45)), 0.001)
    }

    @Test
    fun `quarter hour snapping is deterministic`() {
        assertEquals(6.25, ShiftHoursCalculator.snapToQuarterHour(6.24), 0.001)
        assertEquals(6.5, ShiftHoursCalculator.snapToQuarterHour(6.49), 0.001)
    }
}
