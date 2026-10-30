package com.pasindu.nursingotapp.ui

import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.PeriodSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AdvancedFinanceUiStateTest {

    private val sampleProfile = ProfileEntity(
        id = 1,
        fullName = "Test Nurse",
        serviceNo = "N123",
        unit = "ICU",
        paySheetNo = "P456",
        grade = "Grade III",
        basicSalary = 52809.0, // 2026 basic
        otRate = 425.0,
        updatedAt = 0L
    )

    private val samplePayRateSettings = PayRateSettingsEntity(
        id = 1,
        otRate = 425.0,
        phRate = 1884.0,
        doRate = 1884.0,
        rateSource = "2027_BASIC_SALARY_DIV_30",
        basisSalary2027 = 56520.0, // 2027 basic
        updatedAt = 0L
    )

    private val sampleCompensation = ProfileCompensationEntity(
        id = 1,
        riskAllowance = 3000.0,
        claAllowance = 7800.0,
        additionalAllowancesTotal = 5000.0,
        totalDeductions = 12000.0,
        updatedAt = 0L
    )

    @Test
    fun otAmountRs_phAmountRs_doAmountRs_usePeriodSummaryFromWeeklyOtCalculator() {
        val summary = PeriodSummary(
            totalNormalHours = 36f,
            totalOTHours = 12.5f,
            totalPHDays = 2,
            totalDODays = 1,
            otAmountRs = 5312.50, // exact calculator amount
            phAmountRs = 3768.00,
            doAmountRs = 1884.00,
            totalAmountRs = 10964.50
        )

        val state = AdvancedFinanceUiState(
            profile = sampleProfile,
            payRateSettings = samplePayRateSettings,
            periodSummary = summary,
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30)
        )

        assertEquals(5312.50, state.otAmountRs, 0.001)
        assertEquals(3768.00, state.phAmountRs, 0.001)
        assertEquals(1884.00, state.doAmountRs, 0.001)
    }

    @Test
    fun fallsBackToMultiplicationWhenPeriodSummaryIsNull() {
        val state = AdvancedFinanceUiState(
            profile = sampleProfile,
            payRateSettings = samplePayRateSettings,
            periodSummary = null
        )

        assertEquals(0.0, state.otAmountRs, 0.001)
        assertEquals(0.0, state.phAmountRs, 0.001)
        assertEquals(0.0, state.doAmountRs, 0.001)
    }

    @Test
    fun currentBasicSalary_in2026_usesProfileBasicSalary() {
        val state = AdvancedFinanceUiState(
            profile = sampleProfile,
            payRateSettings = samplePayRateSettings,
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30)
        )

        // For a 2026 claim period, basic salary paid on paysheet is 2026 basic
        assertEquals(52809.0, state.currentBasicSalary, 0.001)
    }

    @Test
    fun currentBasicSalary_in2027_usesBasisSalary2027() {
        val state = AdvancedFinanceUiState(
            profile = sampleProfile,
            payRateSettings = samplePayRateSettings,
            claimStart = LocalDate.of(2027, 1, 1),
            claimEnd = LocalDate.of(2027, 1, 31)
        )

        // For a 2027 claim period, basic salary matches the 2027 revision
        assertEquals(56520.0, state.currentBasicSalary, 0.001)
    }

    @Test
    fun currentBasicSalary_in2027_fallsBackToProfileBasicIfBasis2027NullOrZero() {
        val settingsWithoutBasis = samplePayRateSettings.copy(basisSalary2027 = null)
        val state = AdvancedFinanceUiState(
            profile = sampleProfile,
            payRateSettings = settingsWithoutBasis,
            claimStart = LocalDate.of(2027, 1, 1),
            claimEnd = LocalDate.of(2027, 1, 31)
        )

        assertEquals(52809.0, state.currentBasicSalary, 0.001)
    }

    @Test
    fun grossEarnings_and_estimatedNetSalary_integrateTotalsAccurately() {
        val summary = PeriodSummary(
            totalNormalHours = 36f,
            totalOTHours = 10f,
            totalPHDays = 1,
            totalDODays = 1,
            otAmountRs = 4250.0,
            phAmountRs = 1884.0,
            doAmountRs = 1884.0,
            totalAmountRs = 8018.0
        )

        val state2026 = AdvancedFinanceUiState(
            profile = sampleProfile,
            compensation = sampleCompensation,
            payRateSettings = samplePayRateSettings,
            periodSummary = summary,
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30)
        )

        // Basic (52809) + Risk (3000) + CLA (7800) + Addl (5000) + OT (4250) + PH (1884) + DO (1884)
        val expectedGross2026 = 52809.0 + 3000.0 + 7800.0 + 5000.0 + 4250.0 + 1884.0 + 1884.0
        assertEquals(expectedGross2026, state2026.grossEarnings, 0.001)

        val expectedNet2026 = expectedGross2026 - 12000.0
        assertEquals(expectedNet2026, state2026.estimatedNetSalary, 0.001)

        // For 2027 claim period, basic changes to 56520.0
        val state2027 = state2026.copy(
            claimStart = LocalDate.of(2027, 2, 1),
            claimEnd = LocalDate.of(2027, 2, 28)
        )
        val expectedGross2027 = 56520.0 + 3000.0 + 7800.0 + 5000.0 + 4250.0 + 1884.0 + 1884.0
        assertEquals(expectedGross2027, state2027.grossEarnings, 0.001)
        assertEquals(expectedGross2027 - 12000.0, state2027.estimatedNetSalary, 0.001)
    }
}
