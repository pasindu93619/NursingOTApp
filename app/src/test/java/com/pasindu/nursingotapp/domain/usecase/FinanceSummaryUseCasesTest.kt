package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceSummaryUseCasesTest {

    @Test
    fun calculateFinanceSummary_usesExistingCalculationEngineContract() {
        val profile = ProfileEntity(
            id = 1,
            fullName = "Test Nurse",
            serviceNo = "N1",
            unit = "Ward",
            paySheetNo = "P1",
            grade = "III",
            basicSalary = 100000.0,
            otRate = 283.0
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
            otHours = 2f,
            wardOverride = "Normal",
            reason = ""
        )
        val rates = PayRateSettingsEntity(
            id = 1,
            otRate = 283.0,
            phRate = 1000.0,
            doRate = 1000.0,
            rateSource = "MANUAL",
            basisSalary2027 = null,
            updatedAt = System.currentTimeMillis()
        )

        val summary = CalculateFinanceSummaryUseCase()(
            profile = profile,
            entries = listOf(entry),
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30),
            payRates = rates
        )

        assertEquals(6.0f, summary.totalNormalHours)
        assertEquals(2.0f, summary.totalOTHours)
    }
}
