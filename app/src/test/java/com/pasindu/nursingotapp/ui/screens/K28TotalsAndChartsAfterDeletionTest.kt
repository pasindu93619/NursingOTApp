package com.pasindu.nursingotapp.ui.screens

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.domain.model.DailyLog
import com.pasindu.nursingotapp.domain.usecase.CalculateDailyEntryHoursUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteClaimPeriodUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteDailyEntryByIdUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveAllDailyEntriesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimPeriodsForAnalyticsUseCase
import com.pasindu.nursingotapp.ui.AnalyticsViewModel
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class K28TotalsAndChartsAfterDeletionTest {

    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeClaimPeriodDao(initial: List<ClaimPeriodEntity>) : ClaimPeriodDao {
        val flow = MutableStateFlow(initial)

        override suspend fun insertClaimPeriod(claimPeriod: ClaimPeriodEntity): Long {
            val id = if (claimPeriod.id == 0L) (flow.value.maxOfOrNull { it.id } ?: 0L) + 1L else claimPeriod.id
            flow.value = listOf(claimPeriod.copy(id = id)) + flow.value.filterNot { it.id == id }
            return id
        }

        override fun observeClaimPeriods(): Flow<List<ClaimPeriodEntity>> = flow
        override suspend fun getClaimPeriodById(id: Long): ClaimPeriodEntity? = flow.value.firstOrNull { it.id == id }
        override suspend fun getLatestClaimPeriod(): ClaimPeriodEntity? = flow.value.firstOrNull()
        override suspend fun deleteClaimPeriod(claimPeriod: ClaimPeriodEntity) {
            flow.value = flow.value.filterNot { it.id == claimPeriod.id }
        }
        override suspend fun deleteAllClaimPeriods() {
            flow.value = emptyList()
        }
    }

    private class FakeDailyEntryDao(initial: List<DailyEntryEntity>) : DailyEntryDao {
        val flow = MutableStateFlow(initial)

        override suspend fun insertEntry(entry: DailyEntryEntity) {
            val id = if (entry.id == 0L) (flow.value.maxOfOrNull { it.id } ?: 0L) + 1L else entry.id
            flow.value = flow.value.filterNot { it.id == id } + entry.copy(id = id)
        }

        override suspend fun updateEntry(entry: DailyEntryEntity) = insertEntry(entry)
        override suspend fun upsertDailyEntry(entry: DailyEntryEntity) = insertEntry(entry)
        override fun observeEntriesForPeriod(claimPeriodId: Long): Flow<List<DailyEntryEntity>> =
            flow.map { entries -> entries.filter { it.claimPeriodId == claimPeriodId } }
        override fun observeAllEntries(): Flow<List<DailyEntryEntity>> = flow
        override suspend fun getEntryForDate(claimPeriodId: Long, date: LocalDate): DailyEntryEntity? =
            flow.value.firstOrNull { it.claimPeriodId == claimPeriodId && it.date == date }
        override suspend fun deleteEntryById(id: Long) {
            flow.value = flow.value.filterNot { it.id == id }
        }
        override suspend fun deleteEntriesForPeriod(claimPeriodId: Long) {
            flow.value = flow.value.filterNot { it.claimPeriodId == claimPeriodId }
        }
        override suspend fun deleteAllEntries() {
            flow.value = emptyList()
        }
    }

    private fun entry(
        id: Long,
        periodId: Long,
        date: LocalDate,
        normalHours: Float,
        otHours: Float
    ) = DailyEntryEntity(
        id = id,
        claimPeriodId = periodId,
        date = date,
        isPH = false,
        isDO = false,
        isLeave = false,
        leaveType = null,
        normalTimeIn = "07.00",
        normalTimeOut = "19.00",
        normalHours = normalHours,
        otTimeIn = if (otHours > 0f) "19.00" else "",
        otTimeOut = if (otHours > 0f) "21.00" else "",
        otHours = otHours,
        wardOverride = "Normal",
        reason = ""
    )

    @Test
    fun deletingDailyEntry_updatesAnalyticsChartBreakdownAndDailyTotals() = runTest(mainDispatcher) {
        val sept = ClaimPeriodEntity(
            id = 1L,
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            createdAt = 100L,
            wardType = "Normal"
        )
        val claimDao = FakeClaimPeriodDao(listOf(sept))
        val entryDao = FakeDailyEntryDao(
            listOf(
                entry(1L, 1L, LocalDate.of(2026, 9, 6), normalHours = 12f, otHours = 0f),
                entry(2L, 1L, LocalDate.of(2026, 9, 7), normalHours = 12f, otHours = 0f),
                entry(3L, 1L, LocalDate.of(2026, 9, 8), normalHours = 12f, otHours = 2f),
                entry(4L, 1L, LocalDate.of(2026, 9, 9), normalHours = 12f, otHours = 3f)
            )
        )
        val viewModel = AnalyticsViewModel(
            observeAllDailyEntriesUseCase = ObserveAllDailyEntriesUseCase(entryDao),
            observeClaimPeriodsForAnalyticsUseCase = ObserveClaimPeriodsForAnalyticsUseCase(claimDao)
        )
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.data.collect {}
        }
        advanceUntilIdle()

        val chartBefore = calculateClaimPeriodOtBreakdown(sept, viewModel.data.value.dailyEntries)
        assertEquals(12f, chartBefore.dutyOtHours, 0.001f)
        assertEquals(5f, chartBefore.additionalOtHours, 0.001f)
        assertEquals(17f, chartBefore.totalOtHours, 0.001f)

        DeleteDailyEntryByIdUseCase(entryDao)(4L)
        advanceUntilIdle()

        val afterEntries = viewModel.data.value.dailyEntries
        val chartAfter = calculateClaimPeriodOtBreakdown(sept, afterEntries)
        assertEquals(0f, chartAfter.dutyOtHours, 0.001f)
        assertEquals(2f, chartAfter.additionalOtHours, 0.001f)
        assertEquals(2f, chartAfter.totalOtHours, 0.001f)

        val dailyHoursAfter = CalculateDailyEntryHoursUseCase()(
            logs = afterEntries.map {
                DailyLog(
                    id = it.id,
                    date = it.date,
                    isPH = it.isPH,
                    isDO = it.isDO,
                    isLeave = it.isLeave,
                    leaveType = it.leaveType,
                    reason = it.reason,
                    wardOverride = it.wardOverride,
                    normalTimeInStr = it.normalTimeIn,
                    normalTimeOutStr = it.normalTimeOut,
                    computedNormalHours = it.normalHours,
                    otTimeInStr = it.otTimeIn,
                    otTimeOutStr = it.otTimeOut,
                    computedOtHours = it.otHours
                )
            },
            claimStart = sept.startDate,
            claimEnd = sept.endDate
        )
        assertEquals(36f, dailyHoursAfter.totalNormalHours, 0.001f)
        assertEquals(2f, dailyHoursAfter.totalOtHours, 0.001f)
        collectJob.cancel()
    }

    @Test
    fun deletingClaimPeriod_removesPeriodAndEntriesFromAnalyticsCharts() = runTest(mainDispatcher) {
        val sept = ClaimPeriodEntity(
            id = 1L,
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            createdAt = 100L,
            wardType = "Normal"
        )
        val oct = ClaimPeriodEntity(
            id = 2L,
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 31),
            createdAt = 200L,
            wardType = "Normal"
        )
        val claimDao = FakeClaimPeriodDao(listOf(oct, sept))
        val entryDao = FakeDailyEntryDao(
            listOf(
                entry(1L, 1L, LocalDate.of(2026, 9, 8), normalHours = 8f, otHours = 4f),
                entry(2L, 2L, LocalDate.of(2026, 10, 6), normalHours = 8f, otHours = 2f)
            )
        )
        val viewModel = AnalyticsViewModel(
            observeAllDailyEntriesUseCase = ObserveAllDailyEntriesUseCase(entryDao),
            observeClaimPeriodsForAnalyticsUseCase = ObserveClaimPeriodsForAnalyticsUseCase(claimDao)
        )
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.data.collect {}
        }
        advanceUntilIdle()

        assertEquals(2, viewModel.data.value.claimPeriods.size)
        assertEquals(2, viewModel.data.value.dailyEntries.size)

        DeleteClaimPeriodUseCase(claimDao, entryDao)(oct)
        advanceUntilIdle()

        val remaining = viewModel.data.value
        assertEquals(listOf(sept), remaining.claimPeriods)
        assertEquals(1, remaining.dailyEntries.size)
        assertTrue(remaining.dailyEntries.none { it.claimPeriodId == oct.id })

        val remainingChart = remaining.claimPeriods.map { period ->
            calculateClaimPeriodOtBreakdown(period, remaining.dailyEntries)
        }
        assertEquals(1, remainingChart.size)
        assertEquals(4f, remainingChart.single().totalOtHours, 0.001f)
        collectJob.cancel()
    }
}
