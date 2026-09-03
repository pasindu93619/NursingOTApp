package com.pasindu.nursingotapp.domain.ot

import com.pasindu.nursingotapp.data.model.DailyLog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class NursingWeeklyRuleTest {

    private val sunday = LocalDate.of(2026, 9, 6)
    private val saturday = LocalDate.of(2026, 9, 12)

    @Test
    fun normalDuty30_andOt12_resultsNormal36_andOt6() {
        val result = calculate(
            log(sunday, 12f, 0f),
            log(sunday.plusDays(1), 12f, 0f),
            log(sunday.plusDays(2), 6f, 6f),
            log(sunday.plusDays(3), 0f, 6f)
        )
        assertEquals(36.0, result.totalNormalHours, 0.001)
        assertEquals(6.0, result.totalOtHours, 0.001)
    }

    @Test
    fun normalDuty48_andOt6_resultsNormal36_andOt18() {
        val result = calculate(
            log(sunday, 12f, 0f),
            log(sunday.plusDays(1), 12f, 0f),
            log(sunday.plusDays(2), 12f, 0f),
            log(sunday.plusDays(3), 12f, 0f),
            log(sunday.plusDays(4), 0f, 6f)
        )
        assertEquals(36.0, result.totalNormalHours, 0.001)
        assertEquals(18.0, result.totalOtHours, 0.001)
    }

    @Test
    fun normalDuty36_andOt6_keepsAllOt() {
        val result = calculate(
            log(sunday, 12f, 0f),
            log(sunday.plusDays(1), 12f, 0f),
            log(sunday.plusDays(2), 12f, 0f),
            log(sunday.plusDays(3), 0f, 6f)
        )
        assertEquals(36.0, result.totalNormalHours, 0.001)
        assertEquals(6.0, result.totalOtHours, 0.001)
    }

    @Test
    fun sundayStartsANewWeek() {
        val nextSunday = sunday.plusDays(7)
        val result = WeeklyOtCalculator.calculate(
            logs = listOf(
                log(sunday, 12f, 0f),
                log(sunday.plusDays(1), 12f, 0f),
                log(sunday.plusDays(2), 12f, 0f),
                log(sunday.plusDays(3), 12f, 0f),
                log(nextSunday, 12f, 0f)
            ),
            claimStart = sunday,
            claimEnd = nextSunday,
            otRate = 1.0,
            dayRate = 0.0
        )
        assertEquals(48.0, result.totalNormalHours, 0.001)
        assertEquals(12.0, result.totalOtHours, 0.001)
    }

    @Test
    fun userConfiguredOtRateIsUsedForFinalOtHours() {
        val result = calculate(
            log(sunday, 12f, 0f),
            log(sunday.plusDays(1), 12f, 0f),
            log(sunday.plusDays(2), 12f, 0f),
            log(sunday.plusDays(3), 12f, 0f)
        , otRate = 350.0)
        assertEquals(12.0, result.totalOtHours, 0.001)
        assertEquals(4200.0, result.otAmountRs, 0.001)
    }

    private fun calculate(vararg logs: DailyLog, otRate: Double = 350.0): WeeklyOtCalculator.ClaimSummary =
        WeeklyOtCalculator.calculate(
            logs = logs.toList(),
            claimStart = sunday,
            claimEnd = saturday,
            otRate = otRate,
            dayRate = 0.0
        )

    private fun log(date: LocalDate, normal: Float, ot: Float): DailyLog =
        DailyLog(
            date = date,
            computedNormalHours = normal,
            computedOtHours = ot
        )
}
