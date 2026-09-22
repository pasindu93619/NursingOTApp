package com.pasindu.nursingotapp.domain.finance

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceCalculationEngineTest {
    @Test
    fun totalOtReconcilesIntoMoney() {
        val start = LocalDate.of(2026, 8, 30)
        val end = LocalDate.of(2026, 9, 26)
        val entries = listOf(
            DailyEntryEntity(1, 1, start, false, false, false, null, "07:00", "13:00", 42f, "13:00", "19:00", 72f, "", ""),
            DailyEntryEntity(2, 1, start.plusDays(7), false, false, false, null, "07:00", "13:00", 42f, "13:00", "19:00", 0f, "", ""),
            DailyEntryEntity(3, 1, start.plusDays(14), false, false, false, null, "07:00", "13:00", 42f, "13:00", "19:00", 0f, "", ""),
            DailyEntryEntity(4, 1, start.plusDays(21), false, false, false, null, "07:00", "13:00", 42f, "13:00", "19:00", 0f, "", "")
        )
        val profile = ProfileEntity(
            fullName = "Test Nurse",
            serviceNo = "T001",
            unit = "",
            paySheetNo = "",
            grade = "III",
            basicSalary = 52809.0,
            otRate = 283.0
        )
        val snapshot = FinanceCalculationEngine.calculate(
            profile = profile,
            entries = entries,
            claimStart = start,
            claimEnd = end,
            payRates = PayRateSettingsEntity(otRate = 283.0, phRate = 1884.0, doRate = 1884.0, basisSalary2027 = 56520.0, rateSource = "2027_BASIC_SALARY_DIV_30"),
            compensation = ProfileCompensationEntity(riskAllowance = 0.0, claAllowance = 0.0, additionalAllowancesTotal = 0.0, totalDeductions = 0.0)
        )
        assertEquals(78.0, snapshot.totalOtHours, 0.001)
        assertEquals(78.0 * 283.0, snapshot.otEarnings, 0.001)
        assertEquals(snapshot.dutyGeneratedOtHours + snapshot.recordedOtHours, snapshot.totalOtHours, 0.001)
        assertEquals(snapshot.grossEarnings, snapshot.basicEarnings + snapshot.riskAllowance + snapshot.claAllowance + snapshot.additionalAllowances + snapshot.otEarnings + snapshot.phEarnings + snapshot.doEarnings, 0.001)
        assertEquals(snapshot.grossEarnings - snapshot.totalDeductions, snapshot.netPay, 0.001)
    }
}
