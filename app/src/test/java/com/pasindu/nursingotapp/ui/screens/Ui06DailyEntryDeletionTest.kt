package com.pasindu.nursingotapp.ui.screens

import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity

import com.pasindu.nursingotapp.domain.usecase.DeleteDailyEntryByIdUseCase
import com.pasindu.nursingotapp.domain.usecase.GetDailyEntryForDateUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimDailyEntriesUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveDailyEntryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class Ui06DailyEntryDeletionTest {

    private class TestDailyEntryDao : DailyEntryDao {
        val entriesFlow = MutableStateFlow<List<DailyEntryEntity>>(emptyList())
        var deleteCallCount = 0

        override suspend fun insertEntry(entry: DailyEntryEntity) {
            val current = entriesFlow.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.id == entry.id }
            if (existingIndex >= 0) {
                current[existingIndex] = entry
            } else {
                val assignedId = if (entry.id == 0L) (current.maxOfOrNull { it.id } ?: 0L) + 1L else entry.id
                current.add(entry.copy(id = assignedId))
            }
            entriesFlow.value = current
        }

        override suspend fun updateEntry(entry: DailyEntryEntity) {
            insertEntry(entry)
        }

        override suspend fun upsertDailyEntry(entry: DailyEntryEntity) {
            insertEntry(entry)
        }

        override fun observeEntriesForPeriod(claimPeriodId: Long): Flow<List<DailyEntryEntity>> {
            return entriesFlow
        }

        override fun observeAllEntries(): Flow<List<DailyEntryEntity>> {
            return entriesFlow
        }

        override suspend fun getEntryForDate(claimPeriodId: Long, date: LocalDate): DailyEntryEntity? {
            return entriesFlow.value.firstOrNull { it.claimPeriodId == claimPeriodId && it.date == date }
        }

        override suspend fun deleteEntryById(id: Long) {
            deleteCallCount++
            entriesFlow.value = entriesFlow.value.filterNot { it.id == id }
        }

        override suspend fun deleteEntriesForPeriod(claimPeriodId: Long) {
            entriesFlow.value = entriesFlow.value.filterNot { it.claimPeriodId == claimPeriodId }
        }

        override suspend fun deleteAllEntries() {
            entriesFlow.value = emptyList()
        }
    }

    @Test
    fun deletingASavedEntry_removesThatDateRecordEntirelyFromRoom() = runTest {
        val dao = TestDailyEntryDao()
        val deleteUseCase = DeleteDailyEntryByIdUseCase(dao)

        val dateA = LocalDate.of(2026, 11, 2)
        val entryA = DailyEntryEntity(
            id = 101L,
            claimPeriodId = 1L,
            date = dateA,
            isPH = false,
            isDO = false,
            isLeave = false,
            leaveType = null,
            normalTimeIn = "07.00",
            normalTimeOut = "13.00",
            normalHours = 6f,
            otTimeIn = "",
            otTimeOut = "",
            otHours = 0f,
            wardOverride = "",
            reason = "Need for service"
        )
        dao.insertEntry(entryA)

        assertEquals(1, dao.entriesFlow.value.size)
        assertNotNull(dao.getEntryForDate(1L, dateA))

        // Perform deliberate deletion
        deleteUseCase(101L)

        // Must be completely removed from Room, not zero-hour placeholder!
        assertEquals(0, dao.entriesFlow.value.size)
        assertNull(dao.getEntryForDate(1L, dateA))
        assertEquals(1, dao.deleteCallCount)
    }

    @Test
    fun deletingEntry_leavesOtherDatesUnchanged() = runTest {
        val dao = TestDailyEntryDao()
        val deleteUseCase = DeleteDailyEntryByIdUseCase(dao)

        val dateA = LocalDate.of(2026, 11, 2)
        val dateB = LocalDate.of(2026, 11, 3)
        val entryA = DailyEntryEntity(
            id = 101L,
            claimPeriodId = 1L,
            date = dateA,
            isPH = false,
            isDO = false,
            isLeave = false,
            leaveType = null,
            normalTimeIn = "07.00",
            normalTimeOut = "13.00",
            normalHours = 6f,
            otTimeIn = "",
            otTimeOut = "",
            otHours = 0f,
            wardOverride = "",
            reason = ""
        )
        val entryB = DailyEntryEntity(
            id = 102L,
            claimPeriodId = 1L,
            date = dateB,
            isPH = false,
            isDO = false,
            isLeave = false,
            leaveType = null,
            normalTimeIn = "13.00",
            normalTimeOut = "19.00",
            normalHours = 6f,
            otTimeIn = "",
            otTimeOut = "",
            otHours = 0f,
            wardOverride = "",
            reason = ""
        )
        dao.insertEntry(entryA)
        dao.insertEntry(entryB)

        deleteUseCase(101L)

        assertEquals(1, dao.entriesFlow.value.size)
        assertNull(dao.getEntryForDate(1L, dateA))
        val remainingB = dao.getEntryForDate(1L, dateB)
        assertNotNull(remainingB)
        assertEquals(102L, remainingB?.id)
        assertEquals(6f, remainingB?.normalHours)
    }

    @Test
    fun deletion_updatesObservedDataStreamImmediately() = runTest {
        val dao = TestDailyEntryDao()
        val observeUseCase = ObserveClaimDailyEntriesUseCase(dao)
        val deleteUseCase = DeleteDailyEntryByIdUseCase(dao)

        val entry = DailyEntryEntity(
            id = 200L,
            claimPeriodId = 5L,
            date = LocalDate.of(2026, 11, 5),
            isPH = false,
            isDO = false,
            isLeave = false,
            leaveType = null,
            normalTimeIn = "07.00",
            normalTimeOut = "13.00",
            normalHours = 6f,
            otTimeIn = "",
            otTimeOut = "",
            otHours = 0f,
            wardOverride = "",
            reason = ""
        )
        dao.insertEntry(entry)
        assertEquals(1, observeUseCase(5L).first().size)

        deleteUseCase(200L)
        assertEquals(0, observeUseCase(5L).first().size)
    }

    @Test
    fun undo_restoresOriginalEntryAccurately() = runTest {
        val dao = TestDailyEntryDao()
        val deleteUseCase = DeleteDailyEntryByIdUseCase(dao)
        val saveUseCase = SaveDailyEntryUseCase(dao)

        val date = LocalDate.of(2026, 11, 15)
        val original = DailyEntryEntity(
            id = 303L,
            claimPeriodId = 2L,
            date = date,
            isPH = true,
            isDO = false,
            isLeave = false,
            leaveType = "Holiday",
            normalTimeIn = "07.00",
            normalTimeOut = "17.00",
            normalHours = 10f,
            otTimeIn = "17.00",
            otTimeOut = "20.00",
            otHours = 3f,
            wardOverride = "ICU",
            reason = "Emergency cover"
        )
        dao.insertEntry(original)

        // Delete entry
        deleteUseCase(303L)
        assertNull(dao.getEntryForDate(2L, date))

        // User taps "Undo" -> restores exact entity
        saveUseCase(original)

        val restored = dao.getEntryForDate(2L, date)
        assertNotNull(restored)
        assertEquals(303L, restored?.id)
        assertEquals(date, restored?.date)
        assertEquals(true, restored?.isPH)
        assertEquals(false, restored?.isDO)
        assertEquals("Holiday", restored?.leaveType)
        assertEquals("07.00", restored?.normalTimeIn)
        assertEquals("17.00", restored?.normalTimeOut)
        assertEquals(10f, restored?.normalHours)
        assertEquals("17.00", restored?.otTimeIn)
        assertEquals("20.00", restored?.otTimeOut)
        assertEquals(3f, restored?.otHours)
        assertEquals("ICU", restored?.wardOverride)
        assertEquals("Emergency cover", restored?.reason)
    }

    @Test
    fun deletingMissingEntry_isHandledSafelyWithoutCrashOrGhostRecords() = runTest {
        val dao = TestDailyEntryDao()
        val deleteUseCase = DeleteDailyEntryByIdUseCase(dao)

        // Deleting non-existent ID should be a no-op in Room
        deleteUseCase(99999L)
        assertEquals(0, dao.entriesFlow.value.size)
        assertEquals(1, dao.deleteCallCount)
    }

    @Test
    fun repeatedDeletion_doesNotCreateGhostRecords() = runTest {
        val dao = TestDailyEntryDao()
        val deleteUseCase = DeleteDailyEntryByIdUseCase(dao)

        val entry = DailyEntryEntity(
            id = 404L,
            claimPeriodId = 1L,
            date = LocalDate.of(2026, 11, 20),
            isPH = false,
            isDO = false,
            isLeave = false,
            leaveType = null,
            normalTimeIn = "07.00",
            normalTimeOut = "13.00",
            normalHours = 6f,
            otTimeIn = "",
            otTimeOut = "",
            otHours = 0f,
            wardOverride = "",
            reason = ""
        )
        dao.insertEntry(entry)
        assertEquals(1, dao.entriesFlow.value.size)

        deleteUseCase(404L)
        deleteUseCase(404L)
        deleteUseCase(404L)

        assertEquals(0, dao.entriesFlow.value.size)
        assertEquals(3, dao.deleteCallCount)
    }
}