package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceSummaryUseCasesTest {

    @Test
    fun calculateFinanceSummary_usesUserConfiguredOtRateAfterWeekly36HourSplit() {
        val profile = ProfileEntity(
            id = 1,
            fullName = "Test Nurse",
            serviceNo = "N1",
            unit = "Ward",
            paySheetNo = "P1",
            grade = "Grade III",
            basicSalary = 100000.0,
            otRate = 0.0,
            updatedAt = 0L
        )

        val normalEntries = listOf(
            entry(2026, 9, 6, normalHours = 12f, otHours = 0f),  // Sunday night
            entry(2026, 9, 7, normalHours = 12f, otHours = 0f),  // Monday night
            entry(2026, 9, 8, normalHours = 12f, otHours = 0f),  // Tuesday night
            entry(2026, 9, 9, normalHours = 12f, otHours = 0f)   // Wednesday night
        )

        val userConfiguredOtRate = 350.0
        val rates = PayRateSettingsEntity(
            id = 1,
            otRate = userConfiguredOtRate,
            phRate = 1000.0,
            doRate = 1000.0,
            rateSource = "MANUAL",
            basisSalary2027 = null
        )

        val summary = CalculateFinanceSummaryUseCase()(
            profile = profile,
            entries = normalEntries,
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30),
            payRates = rates
        )

        assertEquals(36.0f, summary.totalNormalHours)
        assertEquals(12.0f, summary.totalOTHours)
        assertEquals(userConfiguredOtRate * 12.0, summary.otAmountRs, 0.001)
    }

    private fun entry(
        year: Int,
        month: Int,
        day: Int,
        normalHours: Float,
        otHours: Float
    ) = DailyEntryEntity(
        id = 0,
        claimPeriodId = 1,
        date = LocalDate.of(year, month, day),
        isPH = false,
        isDO = false,
        isLeave = false,
        leaveType = null,
        normalTimeIn = "19.00",
        normalTimeOut = "07.00",
        normalHours = normalHours,
        otTimeIn = "",
        otTimeOut = "",
        otHours = otHours,
        wardOverride = "Normal",
        reason = ""
    )
}
