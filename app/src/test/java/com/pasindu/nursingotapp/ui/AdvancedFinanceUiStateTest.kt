package com.pasindu.nursingotapp.ui

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDeductionDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileDeductionEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.domain.usecase.ApplyFinancePolicyRatesUseCase
import com.pasindu.nursingotapp.domain.usecase.ApplyMatched2027DayRateUseCase
import com.pasindu.nursingotapp.domain.usecase.CalculateFinanceSummaryUseCase
import com.pasindu.nursingotapp.domain.usecase.EnsureManualPayRateRecordUseCase
import com.pasindu.nursingotapp.domain.usecase.MatchSalaryStepUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimDailyEntriesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimPeriodsUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveOtRateUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveFinanceCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveFinanceRatesUseCase
import com.pasindu.nursingotapp.ui.screens.shouldSyncCommitmentFieldFromState
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun ui02_intermediateInputText_isPreservedForEmptyTrailingDotAndDecimals() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val env = createTestEnvironment(initialPeriods = listOf(periodSept2026))
            val viewModel = env.createViewModel()
            advanceUntilIdle()

            // Initial empty state
            assertEquals("", viewModel.uiState.value.apitInput)
            assertEquals(0.0, viewModel.uiState.value.apit, 0.001)

            // Type "1200"
            viewModel.updateApit("1200")
            advanceUntilIdle()
            assertEquals("1200", viewModel.uiState.value.apitInput)
            assertEquals(1200.0, viewModel.uiState.value.apit, 0.001)
            assertFalse(shouldSyncCommitmentFieldFromState("1200", viewModel.uiState.value.apit, viewModel.uiState.value.apitInput))

            // Type trailing decimal point "1200." -> must NOT reset to "1200.0" or "1200"
            viewModel.updateApit("1200.")
            advanceUntilIdle()
            assertEquals("1200.", viewModel.uiState.value.apitInput)
            assertEquals(1200.0, viewModel.uiState.value.apit, 0.001)
            assertFalse(shouldSyncCommitmentFieldFromState("1200.", viewModel.uiState.value.apit, viewModel.uiState.value.apitInput))

            // Type trailing zero "1200.0" -> must NOT reset to "1200"
            viewModel.updateApit("1200.0")
            advanceUntilIdle()
            assertEquals("1200.0", viewModel.uiState.value.apitInput)
            assertEquals(1200.0, viewModel.uiState.value.apit, 0.001)
            assertFalse(shouldSyncCommitmentFieldFromState("1200.0", viewModel.uiState.value.apit, viewModel.uiState.value.apitInput))

            // Type two decimal places "1200.05"
            viewModel.updateApit("1200.05")
            advanceUntilIdle()
            assertEquals("1200.05", viewModel.uiState.value.apitInput)
            assertEquals(1200.05, viewModel.uiState.value.apit, 0.001)
            assertFalse(shouldSyncCommitmentFieldFromState("1200.05", viewModel.uiState.value.apit, viewModel.uiState.value.apitInput))

            // Clear back to empty string "" -> must NOT reset to "0.0"
            viewModel.updateApit("")
            advanceUntilIdle()
            assertEquals("", viewModel.uiState.value.apitInput)
            assertEquals(0.0, viewModel.uiState.value.apit, 0.001)
            assertFalse(shouldSyncCommitmentFieldFromState("", viewModel.uiState.value.apit, viewModel.uiState.value.apitInput))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ui02_apit_persistsToRoomAndRestoresWhenViewModelRecreated() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val env = createTestEnvironment(initialPeriods = listOf(periodSept2026))
            val vm1 = env.createViewModel()
            advanceUntilIdle()

            vm1.updateApit("1250.75")
            advanceUntilIdle()

            val persisted = env.deductionDao.observeForClaimPeriod(periodSept2026.id).first()
            assertEquals(1, persisted.size)
            assertEquals(DEDUCTION_NAME_APIT, persisted.single().name)
            assertEquals(1250.75, persisted.single().amount, 0.001)

            // Recreate ViewModel from the same backing DAO
            val vm2 = env.createViewModel()
            advanceUntilIdle()

            assertEquals(1250.75, vm2.uiState.value.apit, 0.001)
            assertEquals("1250.75", vm2.uiState.value.apitInput)
            assertTrue(shouldSyncCommitmentFieldFromState("", vm2.uiState.value.apit, vm2.uiState.value.apitInput))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ui02_wop_persistsToRoomAndRestoresWhenViewModelRecreated() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val env = createTestEnvironment(initialPeriods = listOf(periodSept2026))
            val vm1 = env.createViewModel()
            advanceUntilIdle()

            vm1.updateWop("3400.5")
            advanceUntilIdle()

            val persisted = env.deductionDao.observeForClaimPeriod(periodSept2026.id).first()
            assertEquals(1, persisted.size)
            assertEquals(DEDUCTION_NAME_WOP, persisted.single().name)
            assertEquals(3400.5, persisted.single().amount, 0.001)

            val vm2 = env.createViewModel()
            advanceUntilIdle()

            assertEquals(3400.5, vm2.uiState.value.wop, 0.001)
            assertEquals("3400.5", vm2.uiState.value.wopInput)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ui02_loanAdvance_persistsToRoomAndRestoresWhenViewModelRecreated() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val env = createTestEnvironment(initialPeriods = listOf(periodSept2026))
            val vm1 = env.createViewModel()
            advanceUntilIdle()

            vm1.updateLoanDeduction("8500")
            advanceUntilIdle()

            val persisted = env.deductionDao.observeForClaimPeriod(periodSept2026.id).first()
            assertEquals(1, persisted.size)
            assertEquals(DEDUCTION_NAME_LOAN, persisted.single().name)
            assertEquals(8500.0, persisted.single().amount, 0.001)

            val vm2 = env.createViewModel()
            advanceUntilIdle()

            assertEquals(8500.0, vm2.uiState.value.loanDeduction, 0.001)
            assertEquals("8500", vm2.uiState.value.loanDeductionInput)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ui02_otherDeduction_persistsToRoomAndRestoresWhenViewModelRecreated() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val env = createTestEnvironment(initialPeriods = listOf(periodSept2026))
            val vm1 = env.createViewModel()
            advanceUntilIdle()

            vm1.updateOtherDeduction("450.25")
            advanceUntilIdle()

            val persisted = env.deductionDao.observeForClaimPeriod(periodSept2026.id).first()
            assertEquals(1, persisted.size)
            assertEquals(DEDUCTION_NAME_OTHER, persisted.single().name)
            assertEquals(450.25, persisted.single().amount, 0.001)

            val vm2 = env.createViewModel()
            advanceUntilIdle()

            assertEquals(450.25, vm2.uiState.value.otherDeduction, 0.001)
            assertEquals("450.25", vm2.uiState.value.otherDeductionInput)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ui02_customNamedDeductions_remainUnchangedAndNoDuplicatesAreCreated() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val env = createTestEnvironment(initialPeriods = listOf(periodSept2026))
            // Seed custom deduction rows created through ProfileScreen + duplicate legacy APIT row
            env.deductionDao.upsertAll(
                listOf(
                    ProfileDeductionEntity(id = 10L, claimPeriodId = periodSept2026.id, name = "Union Fee", amount = 300.0),
                    ProfileDeductionEntity(id = 11L, claimPeriodId = periodSept2026.id, name = "Welfare Society", amount = 750.0),
                    ProfileDeductionEntity(id = 12L, claimPeriodId = periodSept2026.id, name = "APIT", amount = 500.0),
                    ProfileDeductionEntity(id = 13L, claimPeriodId = periodSept2026.id, name = "apit", amount = 500.0)
                )
            )

            val vm = env.createViewModel()
            advanceUntilIdle()

            // Update all 4 managed commitments and update APIT multiple times
            vm.updateApit("1200")
            vm.updateApit("1500.50")
            vm.updateWop("2000")
            vm.updateLoanDeduction("4000")
            vm.updateOtherDeduction("250")
            advanceUntilIdle()

            val rows = env.deductionDao.observeForClaimPeriod(periodSept2026.id).first()
            val customUnion = rows.filter { it.name == "Union Fee" }
            val customWelfare = rows.filter { it.name == "Welfare Society" }
            val apitRows = rows.filter { it.name.equals(DEDUCTION_NAME_APIT, ignoreCase = true) }
            val wopRows = rows.filter { it.name.equals(DEDUCTION_NAME_WOP, ignoreCase = true) }
            val loanRows = rows.filter { it.name.equals(DEDUCTION_NAME_LOAN, ignoreCase = true) }
            val otherRows = rows.filter { it.name.equals(DEDUCTION_NAME_OTHER, ignoreCase = true) }

            assertEquals(1, customUnion.size)
            assertEquals(10L, customUnion.single().id)
            assertEquals(300.0, customUnion.single().amount, 0.001)
            assertEquals(1, customWelfare.size)
            assertEquals(11L, customWelfare.single().id)
            assertEquals(750.0, customWelfare.single().amount, 0.001)

            // Managed rows must be deduplicated (1 row each)
            assertEquals(1, apitRows.size)
            assertEquals(1500.50, apitRows.single().amount, 0.001)
            assertEquals(1, wopRows.size)
            assertEquals(2000.0, wopRows.single().amount, 0.001)
            assertEquals(1, loanRows.size)
            assertEquals(4000.0, loanRows.single().amount, 0.001)
            assertEquals(1, otherRows.size)
            assertEquals(250.0, otherRows.single().amount, 0.001)

            // Clearing APIT to 0 / empty removes only APIT while preserving custom rows and other commitments
            vm.updateApit("0")
            advanceUntilIdle()
            val rowsAfterClear = env.deductionDao.observeForClaimPeriod(periodSept2026.id).first()
            assertTrue(rowsAfterClear.none { it.name.equals(DEDUCTION_NAME_APIT, ignoreCase = true) })
            assertEquals(1, rowsAfterClear.count { it.name == "Union Fee" && it.id == 10L })
            assertEquals(1, rowsAfterClear.count { it.name == "Welfare Society" && it.id == 11L })
            assertEquals(1, rowsAfterClear.count { it.name == DEDUCTION_NAME_WOP })

            // Verify ProfileCompensationEntity.totalDeductions was NOT overwritten or double-counted
            assertEquals(12000.0, vm.uiState.value.paysheetDeductions, 0.001)
            assertEquals(12000.0, env.compensationDao.getOnce()?.totalDeductions ?: 0.0, 0.001)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ui02_claimPeriodSwitching_doesNotLeakValuesOrStaleWritesBetweenPeriods() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val env = createTestEnvironment(initialPeriods = listOf(periodSept2026))
            val vm = env.createViewModel()
            advanceUntilIdle()

            // Save values in Period 1 (Sept 2026)
            vm.updateApit("1200")
            vm.updateLoanDeduction("6000")
            advanceUntilIdle()

            assertEquals(1200.0, vm.uiState.value.apit, 0.001)
            assertEquals("1200", vm.uiState.value.apitInput)
            assertEquals(6000.0, vm.uiState.value.loanDeduction, 0.001)

            // Trigger an in-flight async write in Period 1 and switch to Period 2 (Oct 2026) while write is suspended
            env.deductionDao.simulatedWriteDelayMs = 50L
            vm.updateWop("9999")
            runCurrent() // starts the Period 1 write coroutine, which suspends mid-transaction on simulatedWriteDelayMs
            env.claimPeriodDao.setPeriods(listOf(periodOct2026, periodSept2026))
            runCurrent() // switches active period to Period 2 without cancelling Period 1's atomic write
            env.deductionDao.simulatedWriteDelayMs = 0L
            advanceUntilIdle()

            // Period 2 must NOT have any values or inputs leaked from Period 1
            assertEquals(periodOct2026.id, vm.uiState.value.claimPeriod?.id)
            assertEquals(0.0, vm.uiState.value.apit, 0.001)
            assertEquals("", vm.uiState.value.apitInput)
            assertEquals(0.0, vm.uiState.value.wop, 0.001)
            assertEquals("", vm.uiState.value.wopInput)
            assertEquals(0.0, vm.uiState.value.loanDeduction, 0.001)
            assertEquals("", vm.uiState.value.loanDeductionInput)
            assertTrue(env.deductionDao.observeForClaimPeriod(periodOct2026.id).first().isEmpty())

            // Save distinct values in Period 2
            vm.updateWop("2200.50")
            vm.updateOtherDeduction("350")
            advanceUntilIdle()

            assertEquals(2200.50, vm.uiState.value.wop, 0.001)
            assertEquals(350.0, vm.uiState.value.otherDeduction, 0.001)

            // Switch back to Period 1 -> must restore Period 1's saved values including its completed final edit (APIT=1200, Loan=6000, WOP=9999, Other=0)
            env.claimPeriodDao.setPeriods(listOf(periodSept2026, periodOct2026))
            advanceUntilIdle()

            assertEquals(periodSept2026.id, vm.uiState.value.claimPeriod?.id)
            assertEquals(1200.0, vm.uiState.value.apit, 0.001)
            assertEquals("1200", vm.uiState.value.apitInput)
            assertEquals(6000.0, vm.uiState.value.loanDeduction, 0.001)
            assertEquals("6000", vm.uiState.value.loanDeductionInput)
            assertEquals(9999.0, vm.uiState.value.wop, 0.001)
            assertEquals("9999", vm.uiState.value.wopInput)
            assertEquals(0.0, vm.uiState.value.otherDeduction, 0.001)
            assertEquals("", vm.uiState.value.otherDeductionInput)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ui02_transactionRollback_whenUpsertFailsAfterDelete_preservesAllOriginalManagedAndCustomRows() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val env = createTestEnvironment(initialPeriods = listOf(periodSept2026))
            val initialRows = listOf(
                ProfileDeductionEntity(id = 10L, claimPeriodId = periodSept2026.id, name = "Union Fee", amount = 300.0, updatedAt = 100L),
                ProfileDeductionEntity(id = 11L, claimPeriodId = periodSept2026.id, name = "Welfare Society", amount = 750.0, updatedAt = 100L),
                ProfileDeductionEntity(id = 12L, claimPeriodId = periodSept2026.id, name = DEDUCTION_NAME_APIT, amount = 500.0, updatedAt = 100L),
                ProfileDeductionEntity(id = 13L, claimPeriodId = periodSept2026.id, name = DEDUCTION_NAME_WOP, amount = 1500.0, updatedAt = 100L)
            )
            env.deductionDao.upsertAll(initialRows)

            val vm = env.createViewModel()
            advanceUntilIdle()

            assertEquals(500.0, vm.uiState.value.apit, 0.001)
            assertEquals(1500.0, vm.uiState.value.wop, 0.001)

            // Simulate failure during upsertAll AFTER deleteForClaimPeriod has already run inside the transaction
            env.deductionDao.failOnNextUpsert = true
            vm.updateApit("9999.99")
            advanceUntilIdle()

            // Confirm transaction rolled back and all 4 original rows (custom and managed) remain intact with original IDs
            val rowsAfterFailedWrite = env.deductionDao.getForClaimPeriodOnce(periodSept2026.id)
            assertEquals(initialRows, rowsAfterFailedWrite)
            assertNotNull(vm.uiState.value.errorMessage)
            assertEquals(500.0, vm.uiState.value.apit, 0.001)
            assertEquals(1500.0, vm.uiState.value.wop, 0.001)

            // Confirm a subsequent retry succeeds and still preserves all custom rows and prior managed rows
            vm.updateWop("1800")
            advanceUntilIdle()

            val rowsAfterRetry = env.deductionDao.getForClaimPeriodOnce(periodSept2026.id)
            assertEquals(4, rowsAfterRetry.size)
            assertTrue(rowsAfterRetry.any { it.id == 10L && it.name == "Union Fee" && it.amount == 300.0 })
            assertTrue(rowsAfterRetry.any { it.id == 11L && it.name == "Welfare Society" && it.amount == 750.0 })
            assertTrue(rowsAfterRetry.any { it.id == 12L && it.name == DEDUCTION_NAME_APIT && it.amount == 500.0 })
            assertTrue(rowsAfterRetry.any { it.id == 13L && it.name == DEDUCTION_NAME_WOP && it.amount == 1800.0 })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ui02_periodSwitchDuringMidTransactionSuspension_preservesPeriodAStoredRowsAndIsolatesPeriodB() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val env = createTestEnvironment(initialPeriods = listOf(periodSept2026, periodOct2026))
            // Seed Period A (Sept 2026) and Period B (Oct 2026) with custom and managed rows
            env.deductionDao.upsertAll(
                listOf(
                    ProfileDeductionEntity(id = 10L, claimPeriodId = periodSept2026.id, name = "Union Fee", amount = 300.0, updatedAt = 100L),
                    ProfileDeductionEntity(id = 11L, claimPeriodId = periodSept2026.id, name = DEDUCTION_NAME_APIT, amount = 1200.0, updatedAt = 100L),
                    ProfileDeductionEntity(id = 12L, claimPeriodId = periodSept2026.id, name = DEDUCTION_NAME_LOAN, amount = 6000.0, updatedAt = 100L),
                    ProfileDeductionEntity(id = 20L, claimPeriodId = periodOct2026.id, name = "Festival Advance", amount = 1000.0, updatedAt = 200L),
                    ProfileDeductionEntity(id = 21L, claimPeriodId = periodOct2026.id, name = DEDUCTION_NAME_WOP, amount = 2200.50, updatedAt = 200L)
                )
            )

            val vm = env.createViewModel()
            advanceUntilIdle()

            // Begin a write for Period A that suspends AFTER deleteForClaimPeriod and BEFORE upsertAll
            env.deductionDao.simulatedWriteDelayMs = 50L
            vm.updateOtherDeduction("450")
            runCurrent()

            // Switch to Period B while Period A's transaction is suspended between delete and upsert
            env.claimPeriodDao.setPeriods(listOf(periodOct2026, periodSept2026))
            runCurrent()

            env.deductionDao.simulatedWriteDelayMs = 0L
            advanceUntilIdle()

            // Confirm Period B receives only its own values
            assertEquals(periodOct2026.id, vm.uiState.value.claimPeriod?.id)
            assertEquals(0.0, vm.uiState.value.apit, 0.001)
            assertEquals(2200.50, vm.uiState.value.wop, 0.001)
            assertEquals(0.0, vm.uiState.value.loanDeduction, 0.001)
            assertEquals(0.0, vm.uiState.value.otherDeduction, 0.001)

            val periodBRows = env.deductionDao.getForClaimPeriodOnce(periodOct2026.id)
            assertEquals(2, periodBRows.size)
            assertTrue(periodBRows.any { it.id == 20L && it.name == "Festival Advance" && it.amount == 1000.0 })
            assertTrue(periodBRows.any { it.id == 21L && it.name == DEDUCTION_NAME_WOP && it.amount == 2200.50 })

            // Confirm Period A's stored rows (including custom row and final edit) remain completely intact
            val periodARows = env.deductionDao.getForClaimPeriodOnce(periodSept2026.id)
            assertEquals(4, periodARows.size)
            assertTrue(periodARows.any { it.id == 10L && it.name == "Union Fee" && it.amount == 300.0 })
            assertTrue(periodARows.any { it.id == 11L && it.name == DEDUCTION_NAME_APIT && it.amount == 1200.0 })
            assertTrue(periodARows.any { it.id == 12L && it.name == DEDUCTION_NAME_LOAN && it.amount == 6000.0 })
            assertTrue(periodARows.any { it.name == DEDUCTION_NAME_OTHER && it.amount == 450.0 })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ui02_customRowConcurrency_readsCustomRowsInsideTransactionAndPreservesConcurrentCustomAdditions() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val env = createTestEnvironment(initialPeriods = listOf(periodSept2026))
            env.deductionDao.upsertAll(
                listOf(
                    ProfileDeductionEntity(id = 10L, claimPeriodId = periodSept2026.id, name = "Union Fee", amount = 300.0, updatedAt = 100L),
                    ProfileDeductionEntity(id = 12L, claimPeriodId = periodSept2026.id, name = DEDUCTION_NAME_APIT, amount = 500.0, updatedAt = 100L)
                )
            )

            val vm = env.createViewModel()
            advanceUntilIdle()

            // Simulate another writer inserting a custom row right before the replacement transaction reads existing rows
            env.deductionDao.beforeTransactionReadHook = { claimPeriodId ->
                env.deductionDao.insertCustomRowDirectly(
                    ProfileDeductionEntity(
                        id = 42L,
                        claimPeriodId = claimPeriodId,
                        name = "Benevolent Fund",
                        amount = 400.0,
                        updatedAt = 150L
                    )
                )
            }

            vm.updateApit("1500")
            advanceUntilIdle()

            assertEquals(0, env.deductionDao.readsOutsideTransactionCount)
            assertTrue(env.deductionDao.readsInsideTransactionCount > 0)

            val persistedRows = env.deductionDao.getForClaimPeriodOnce(periodSept2026.id)
            assertEquals(3, persistedRows.size)
            assertTrue(persistedRows.any { it.id == 10L && it.name == "Union Fee" && it.amount == 300.0 })
            assertTrue(persistedRows.any { it.id == 42L && it.name == "Benevolent Fund" && it.amount == 400.0 })
            assertTrue(persistedRows.any { it.id == 12L && it.name == DEDUCTION_NAME_APIT && it.amount == 1500.0 })
        } finally {
            Dispatchers.resetMain()
        }
    }

    private val periodSept2026 = ClaimPeriodEntity(
        id = 101L,
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 9, 30),
        createdAt = 1000L
    )

    private val periodOct2026 = ClaimPeriodEntity(
        id = 202L,
        startDate = LocalDate.of(2026, 10, 1),
        endDate = LocalDate.of(2026, 10, 31),
        createdAt = 2000L
    )

    private class TestEnvironment(
        val profileDao: FakeProfileDao,
        val claimPeriodDao: FakeClaimPeriodDao,
        val payRateDao: FakePayRateSettingsDao,
        val compensationDao: FakeProfileCompensationDao,
        val deductionDao: FakeProfileDeductionDao,
        val salaryDao: FakeSalaryStep2027Dao,
        val dailyEntryDao: FakeDailyEntryDao
    ) {
        fun createViewModel(): AdvancedFinanceViewModel =
            AdvancedFinanceViewModel(
                observeProfile = ObserveProfileUseCase(profileDao),
                observeClaimPeriod = ObserveClaimPeriodsUseCase(claimPeriodDao),
                observePayRates = ObserveOtRateUseCase(payRateDao),
                observeCompensation = ObserveProfileCompensationUseCase(compensationDao),
                profileDeductionDao = deductionDao,
                ensureManualPayRateRecordUseCase = EnsureManualPayRateRecordUseCase(payRateDao),
                applyFinancePolicyRatesUseCase = ApplyFinancePolicyRatesUseCase(
                    MatchSalaryStepUseCase(salaryDao),
                    ApplyMatched2027DayRateUseCase(payRateDao)
                ),
                observeClaimDailyEntriesUseCase = ObserveClaimDailyEntriesUseCase(dailyEntryDao),
                calculateFinanceSummaryUseCase = CalculateFinanceSummaryUseCase(),
                saveFinanceCompensationUseCase = SaveFinanceCompensationUseCase(compensationDao),
                saveFinanceRatesUseCase = SaveFinanceRatesUseCase(payRateDao)
            )
    }

    private fun createTestEnvironment(
        initialPeriods: List<ClaimPeriodEntity>
    ): TestEnvironment {
        return TestEnvironment(
            profileDao = FakeProfileDao(sampleProfile),
            claimPeriodDao = FakeClaimPeriodDao(initialPeriods),
            payRateDao = FakePayRateSettingsDao(samplePayRateSettings),
            compensationDao = FakeProfileCompensationDao(sampleCompensation),
            deductionDao = FakeProfileDeductionDao(),
            salaryDao = FakeSalaryStep2027Dao(),
            dailyEntryDao = FakeDailyEntryDao()
        )
    }

    private class FakeProfileDao(initial: ProfileEntity?) : ProfileDao {
        private val flow = MutableStateFlow(initial)
        override suspend fun upsert(profile: ProfileEntity) {
            flow.value = profile
        }
        override fun observeProfile(): Flow<ProfileEntity?> = flow
        override suspend fun getProfileOnce(): ProfileEntity? = flow.value
    }

    private class FakeClaimPeriodDao(initial: List<ClaimPeriodEntity>) : ClaimPeriodDao {
        private val flow = MutableStateFlow(initial)
        fun setPeriods(periods: List<ClaimPeriodEntity>) {
            flow.value = periods
        }
        override suspend fun insertClaimPeriod(claimPeriod: ClaimPeriodEntity): Long {
            flow.value = listOf(claimPeriod) + flow.value.filterNot { it.id == claimPeriod.id }
            return claimPeriod.id
        }
        override fun observeClaimPeriods(): Flow<List<ClaimPeriodEntity>> = flow
        override suspend fun getClaimPeriodById(id: Long): ClaimPeriodEntity? =
            flow.value.firstOrNull { it.id == id }
        override suspend fun getLatestClaimPeriod(): ClaimPeriodEntity? =
            flow.value.firstOrNull()
        override suspend fun deleteClaimPeriod(claimPeriod: ClaimPeriodEntity) {
            flow.value = flow.value.filterNot { it.id == claimPeriod.id }
        }
        override suspend fun deleteAllClaimPeriods() {
            flow.value = emptyList()
        }
    }

    private class FakePayRateSettingsDao(initial: PayRateSettingsEntity?) : PayRateSettingsDao {
        private val flow = MutableStateFlow(initial)
        override fun observe(): Flow<PayRateSettingsEntity?> = flow
        override suspend fun upsert(settings: PayRateSettingsEntity) {
            flow.value = settings
        }
    }

    private class FakeProfileCompensationDao(initial: ProfileCompensationEntity?) : ProfileCompensationDao {
        private val flow = MutableStateFlow(initial)
        override suspend fun upsert(compensation: ProfileCompensationEntity) {
            flow.value = compensation
        }
        override fun observe(): Flow<ProfileCompensationEntity?> = flow
        override suspend fun getOnce(): ProfileCompensationEntity? = flow.value
    }

    private class FakeProfileDeductionDao : ProfileDeductionDao {
        private val transactionMutex = Mutex()
        private var inTransaction = false
        private var nextId = 100L
        private val rowsFlow = MutableStateFlow<List<ProfileDeductionEntity>>(emptyList())

        var simulatedWriteDelayMs: Long = 0L
        var failOnNextUpsert: Boolean = false
        var beforeTransactionReadHook: ((Long) -> Unit)? = null
        var readsInsideTransactionCount: Int = 0
            private set
        var readsOutsideTransactionCount: Int = 0
            private set

        fun insertCustomRowDirectly(entity: ProfileDeductionEntity) {
            val current = rowsFlow.value.toMutableList()
            current.add(entity)
            rowsFlow.value = current
        }

        override fun observeForClaimPeriod(claimPeriodId: Long): Flow<List<ProfileDeductionEntity>> =
            rowsFlow.map { list ->
                list.filter { it.claimPeriodId == claimPeriodId }.sortedBy { it.id }
            }

        override suspend fun getForClaimPeriodOnce(claimPeriodId: Long): List<ProfileDeductionEntity> {
            if (inTransaction) {
                readsInsideTransactionCount++
            } else {
                readsOutsideTransactionCount++
            }
            return rowsFlow.value
                .filter { it.claimPeriodId == claimPeriodId }
                .sortedBy { it.id }
        }

        override suspend fun deleteForClaimPeriod(claimPeriodId: Long) {
            rowsFlow.value = rowsFlow.value.filterNot { it.claimPeriodId == claimPeriodId }
            if (simulatedWriteDelayMs > 0L) {
                kotlinx.coroutines.delay(simulatedWriteDelayMs)
            }
        }

        override suspend fun upsertAll(items: List<ProfileDeductionEntity>) {
            if (failOnNextUpsert) {
                failOnNextUpsert = false
                throw IllegalStateException("Simulated database write failure during upsertAll")
            }
            val current = rowsFlow.value.toMutableList()
            items.forEach { item ->
                val resolvedId = if (item.id > 0L) item.id else nextId++
                val entity = item.copy(id = resolvedId)
                val existingIndex = current.indexOfFirst { it.id == resolvedId }
                if (existingIndex >= 0) {
                    current[existingIndex] = entity
                } else {
                    current.add(entity)
                }
            }
            rowsFlow.value = current
        }

        override suspend fun replaceManagedCommitmentsForClaimPeriod(
            claimPeriodId: Long,
            apit: Double,
            wop: Double,
            loanDeduction: Double,
            otherDeduction: Double,
            updatedAt: Long
        ): List<ProfileDeductionEntity> = transactionMutex.withLock {
            beforeTransactionReadHook?.invoke(claimPeriodId)
            beforeTransactionReadHook = null
            val snapshotRows = rowsFlow.value
            val snapshotNextId = nextId
            inTransaction = true
            try {
                super.replaceManagedCommitmentsForClaimPeriod(
                    claimPeriodId = claimPeriodId,
                    apit = apit,
                    wop = wop,
                    loanDeduction = loanDeduction,
                    otherDeduction = otherDeduction,
                    updatedAt = updatedAt
                )
            } catch (t: Throwable) {
                rowsFlow.value = snapshotRows
                nextId = snapshotNextId
                throw t
            } finally {
                inTransaction = false
            }
        }
    }

    private class FakeSalaryStep2027Dao : SalaryStep2027Dao {
        private val row = SalaryStep2027Entity(
            id = 3L,
            grade = "III",
            salaryStep = 3,
            currentBasicSalary2026 = 52809.0,
            basicSalary2027 = 56520.0
        )
        override fun observeAll(): Flow<List<SalaryStep2027Entity>> = flowOf(listOf(row))
        override fun observeForGrade(grade: String): Flow<List<SalaryStep2027Entity>> = flowOf(listOf(row))
        override suspend fun find(grade: String, salaryStep: Int): SalaryStep2027Entity? = row
        override suspend fun findByCurrentBasic(grade: String, currentBasicSalary: Double): SalaryStep2027Entity? = row
        override suspend fun findByBasicSalary(currentBasicSalary: Double): List<SalaryStep2027Entity> = listOf(row)
        override suspend fun count(): Int = 1
        override suspend fun clearAll() = Unit
        override suspend fun insertAll(rows: List<SalaryStep2027Entity>) = Unit
    }

    private class FakeDailyEntryDao : DailyEntryDao {
        override suspend fun insertEntry(entry: DailyEntryEntity) = Unit
        override suspend fun updateEntry(entry: DailyEntryEntity) = Unit
        override suspend fun upsertDailyEntry(entry: DailyEntryEntity) = Unit
        override fun observeEntriesForPeriod(claimPeriodId: Long): Flow<List<DailyEntryEntity>> = flowOf(emptyList())
        override fun observeAllEntries(): Flow<List<DailyEntryEntity>> = flowOf(emptyList())
        override suspend fun getEntryForDate(claimPeriodId: Long, date: LocalDate): DailyEntryEntity? = null
        override suspend fun deleteEntryById(id: Long) = Unit
        override suspend fun deleteEntriesForPeriod(claimPeriodId: Long) = Unit
        override suspend fun deleteAllEntries() = Unit
    }
}
