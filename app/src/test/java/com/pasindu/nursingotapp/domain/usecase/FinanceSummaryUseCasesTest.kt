package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceSummaryUseCasesTest {

    @Test
    fun calculateFinanceSummary_usesUserConfiguredOtRate() {
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
        val entry = DailyEntryEntity(
            id = 1,
            claimPeriodId = 1,
            date = LocalDate.of(2026, 9, 6),
            isPH = false,
            isDO = false,
            isLeave = false,
            leaveType = null,
            normalTimeIn = "07.00",
            normalTimeOut = "13.00",
            normalHours = 6f,
            otTimeIn = "13.00",
            otTimeOut = "15.00",
            normalHours = 6f,
            otHours = 2f,
            wardOverride = "Normal",
            reason = ""
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
            entries = listOf(entry),
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30),
            payRates = rates
        )

        assertEquals(8.0f, summary.totalNormalHours)
        assertEquals(0.0f, summary.totalOTHours)
        assertEquals(0.0, summary.otAmountRs, 0.001)
    }
}
