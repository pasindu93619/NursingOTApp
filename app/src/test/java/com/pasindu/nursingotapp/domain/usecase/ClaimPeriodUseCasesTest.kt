package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

class ClaimPeriodUseCasesTest {

    @Test
    fun createClaimPeriod_rejectsInvalidDateRange() = runBlocking {
        val dao = FakeClaimPeriodDao()
        val useCase = CreateClaimPeriodUseCase(dao)

        assertFailsWith<IllegalArgumentException> {
            useCase(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1), "Normal")
        }
    }

    @Test
    fun createClaimPeriod_persistsTrimmedWardType() = runBlocking {
        val dao = FakeClaimPeriodDao()
        val useCase = CreateClaimPeriodUseCase(dao)

        val id = useCase(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30),
            "  Normal  "
        )

        assertEquals(1L, id)
        assertEquals("Normal", dao.items.value.single().wardType)
    }

    @Test
    fun deleteClaimPeriod_deletesEntriesBeforePeriod() = runBlocking {
        val period = ClaimPeriodEntity(
            id = 7L,
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            createdAt = 1L,
            wardType = "Normal"
        )
        val claimDao = FakeClaimPeriodDao()
        val dailyDao = FakeDailyEntryDao()
        claimDao.items.value = listOf(period)
        dailyDao.entries.value = listOf(
            DailyEntryEntity(
                id = 1L,
                claimPeriodId = 7L,
                date = LocalDate.of(2026, 9, 1),
                isPH = false,
                isDO = false,
                isLeave = false,
                leaveType = null,
                normalTimeIn = "08:00",
                normalTimeOut = "14:00",
                normalHours = 6f,
                otTimeIn = "",
                otTimeOut = "",
                otHours = 0f,
                wardOverride = "Normal",
                reason = ""
            )
        )

        DeleteClaimPeriodUseCase(claimDao, dailyDao)(period)

        assertEquals(1, dailyDao.deletePeriodCalls)
        assertEquals(0, claimDao.items.value.size)
    }

    private class FakeClaimPeriodDao : ClaimPeriodDao {
        val items = MutableStateFlow<List<ClaimPeriodEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insertClaimPeriod(claimPeriod: ClaimPeriodEntity): Long {
            val id = if (claimPeriod.id == 0L) nextId++ else claimPeriod.id
            items.value = items.value + claimPeriod.copy(id = id)
            return id
        }

        override fun observeClaimPeriods(): Flow<List<ClaimPeriodEntity>> = items

        override suspend fun getClaimPeriodById(id: Long): ClaimPeriodEntity? =
            items.value.firstOrNull { it.id == id }

        override suspend fun deleteClaimPeriod(claimPeriod: ClaimPeriodEntity) {
            items.value = items.value.filterNot { it.id == claimPeriod.id }
        }

        override suspend fun deleteAllClaimPeriods() {
            items.value = emptyList()
        }
    }

    private class FakeDailyEntryDao : DailyEntryDao {
        val entries = MutableStateFlow<List<DailyEntryEntity>>(emptyList())
        var deletePeriodCalls = 0

        override suspend fun insertEntry(entry: DailyEntryEntity) = Unit
        override suspend fun updateEntry(entry: DailyEntryEntity) = Unit
        override suspend fun upsertDailyEntry(entry: DailyEntryEntity) = Unit
        override fun observeEntriesForPeriod(claimPeriodId: Long): Flow<List<DailyEntryEntity>> = flowOf(
            entries.value.filter { it.claimPeriodId == claimPeriodId }
        )
        override fun observeAllEntries(): Flow<List<DailyEntryEntity>> = entries
        override suspend fun getEntryForDate(claimPeriodId: Long, date: LocalDate): DailyEntryEntity? = null
        override suspend fun deleteEntriesForPeriod(claimPeriodId: Long) {
            deletePeriodCalls++
            entries.value = entries.value.filterNot { it.claimPeriodId == claimPeriodId }
        }
        override suspend fun deleteAllEntries() {
            entries.value = emptyList()
        }
    }
}
