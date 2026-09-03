package com.pasindu.nursingotapp.domain.usecase

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClaimPeriodDateUseCasesTest {

    @Test
    fun buildDateRange_returnsSundayThroughSaturdayBounds() {
        val (start, end) = BuildClaimPeriodDateRangeUseCase()(LocalDate.of(2026, 9, 15))
        assertEquals(LocalDate.of(2026, 9, 6), start)
        assertEquals(LocalDate.of(2026, 9, 26), end)
    }

    @Test
    fun parseDate_returnsNullForInvalidInput() {
        assertNull(ParseClaimPeriodDateUseCase()("2026", "02", "30"))
        assertNull(ParseClaimPeriodDateUseCase()("", "02", "10"))
    }

    @Test
    fun summarizeWeeks_countsOnlyCompleteSundaySaturdayWeeks() {
        val useCase = SummarizeClaimPeriodWeeksUseCase()
        assertEquals(3, useCase(LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 26)))
        assertEquals(0, useCase(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5)))
    }
}
