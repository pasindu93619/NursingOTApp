package com.pasindu.nursingotapp.domain.ot

import com.pasindu.nursingotapp.data.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyOtCalculatorTest {
    private val start = LocalDate.of(2026, 8, 2) // Sunday
    private val end = LocalDate.of(2026, 8, 8)   // Saturday

    @Test
    fun `first 36 hours are normal and remainder is overtime`() {
        val logs = listOf(
            log(start.plusDays(0), 12f),
            log(start.plusDays(1), 12f),
            log(start.plusDays(2), 12f),
            log(start.plusDays(3), 6f)
        )

        val result = WeeklyOtCalculator.calculate(
            logs = logs,
            claimStart = start,
            claimEnd = end,
            otRate = 100.0,
            dayRate = 1000.0
        )

        assertEquals(36.0, result.totalNormalHours, 0.001)
        assertEquals(6.0, result.totalOtHours, 0.001)
        assertEquals(600.0, result.otAmountRs, 0.001)
    }

    @Test
    fun `weekly counter resets on Sunday`() {
        val firstWeekSaturday = start.plusDays(6)
        val nextSunday = start.plusDays(7)
        val logs = listOf(
            log(start, 12f),
            log(start.plusDays(1), 12f),
            log(start.plusDays(2), 12f),
            log(firstWeekSaturday, 12f),
            log(nextSunday, 12f)
        )

        val result = WeeklyOtCalculator.calculate(
            logs = logs,
            claimStart = start,
            claimEnd = nextSunday,
            otRate = 100.0,
            dayRate = 1000.0
        )

        // Week 1 has 48 hours: 36 normal + 12 OT.
        // The following Sunday starts a new week, so its 12 hours are normal.
        assertEquals(48.0, result.totalNormalHours, 0.001)
        assertEquals(12.0, result.totalOtHours, 0.001)
        assertEquals(2, result.weeklySummaries.size)
        assertEquals(36.0, result.weeklySummaries[0].normalHours, 0.001)
        assertEquals(12.0, result.weeklySummaries[0].otHours, 0.001)
        assertEquals(12.0, result.weeklySummaries[1].normalHours, 0.001)
        assertEquals(0.0, result.weeklySummaries[1].otHours, 0.001)
        assertTrue(result.weeklySummaries.all { it.normalHours <= 36.0 })
    }

    @Test
    fun `late entry after 36 hours is entirely overtime`() {
        val logs = listOf(
            log(start, 12f),
            log(start.plusDays(1), 12f),
            log(start.plusDays(2), 12f),
            log(start.plusDays(3), 12f)
        )

        val result = WeeklyOtCalculator.calculate(
            logs = logs,
            claimStart = start,
            claimEnd = end,
            otRate = 250.0,
            dayRate = 2000.0
        )

        val lastAllocation = result.allocations.last()
        assertEquals(0.0, lastAllocation.normalHours, 0.001)
        assertEquals(12.0, lastAllocation.otHours, 0.001)
        assertEquals(48.0, result.totalNormalHours + result.totalOtHours, 0.001)
    }

    @Test
    fun `ph and do payment counts stay independent from weekly hour split`() {
        val logs = listOf(
            log(start, 6f, isPH = true),
            log(start.plusDays(1), 6f, isDO = true),
            log(start.plusDays(2), 6f),
            log(start.plusDays(3), 6f),
            log(start.plusDays(4), 6f),
            log(start.plusDays(5), 6f),
            log(start.plusDays(6), 6f)
        )

        val result = WeeklyOtCalculator.calculate(
            logs = logs,
            claimStart = start,
            claimEnd = end,
            otRate = 100.0,
            dayRate = 1000.0
        )

        assertEquals(1, result.phDays)
        assertEquals(1, result.doDays)
        assertEquals(1000.0, result.phAmountRs, 0.001)
        assertEquals(1000.0, result.doAmountRs, 0.001)
    }

    @Test
    fun `separate configured ph and do rates are preserved`() {
        val logs = listOf(
            log(start, 6f, isPH = true),
            log(start.plusDays(1), 6f, isDO = true)
        )

        val result = WeeklyOtCalculator.calculate(
            logs = logs,
            claimStart = start,
            claimEnd = end,
            otRate = 750.0,
            dayRate = 1200.0,
            doRate = 1800.0
        )

        assertEquals(1, result.phDays)
        assertEquals(1, result.doDays)
        assertEquals(1200.0, result.phAmountRs, 0.001)
        assertEquals(1800.0, result.doAmountRs, 0.001)
        assertEquals(3000.0, result.totalAmountRs, 0.001)
    }

    @Test
    fun `claim boundaries exclude entries outside selected period`() {
        val logs = listOf(
            log(start.minusDays(1), 20f),
            log(start, 12f),
            log(start.plusDays(1), 12f),
            log(start.plusDays(2), 12f),
            log(end.plusDays(1), 20f)
        )

        val result = WeeklyOtCalculator.calculate(
            logs = logs,
            claimStart = start,
            claimEnd = end,
            otRate = 100.0,
            dayRate = 1000.0
        )

        assertEquals(36.0, result.totalNormalHours, 0.001)
        assertEquals(0.0, result.totalOtHours, 0.001)
        assertEquals(3, result.allocations.size)
        assertTrue(result.allocations.all { !it.date.isBefore(start) && !it.date.isAfter(end) })
    }

    private fun log(
        date: LocalDate,
        hours: Float,
        isPH: Boolean = false,
        isDO: Boolean = false
    ): DailyLog =
        DailyLog(
            date = date,
            isPH = isPH,
            isDO = isDO,
            computedNormalHours = hours,
            computedOtHours = 0f
        )
}
