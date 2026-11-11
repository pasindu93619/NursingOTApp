package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalPlanningUseCasesTest {

    @Test
    fun addIsbarNoteTrimsInputAndPersistsValues() = runTest {
        val dao = FakeClinicalPlanningDao()

        AddIsbarNoteUseCase(dao)(
            patientId = "  P-1001  ",
            identification = "  Mr A  ",
            situation = "  Fever  ",
            background = "  Post-op day 1  ",
            assessment = "  Temp 38.4  ",
            recommendation = "  Review cultures  "
        )

        val saved = dao.insertedIsbar
        assertEquals("P-1001", saved?.patientIdentifier)
        assertEquals("Mr A", saved?.identification)
        assertEquals("Fever", saved?.situation)
        assertEquals("Post-op day 1", saved?.background)
        assertEquals("Temp 38.4", saved?.assessment)
        assertEquals("Review cultures", saved?.recommendation)
        assertTrue((saved?.timestamp ?: 0L) > 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addIsbarNoteRejectsBlankPatientId() = runTest {
        AddIsbarNoteUseCase(FakeClinicalPlanningDao())(
            patientId = "   ",
            identification = "Patient",
            situation = "Situation",
            background = "Background",
            assessment = "Assessment",
            recommendation = "Recommendation"
        )
    }

    @Test
    fun updateIsbarNoteTrimsInputAndPersistsValues() = runTest {
        val dao = FakeClinicalPlanningDao()

        UpdateIsbarNoteUseCase(dao)(
            id = 42,
            patientId = "  P-2002  ",
            identification = "  Bed 12  ",
            situation = "  Hypotension  ",
            background = "  Post-op sepsis  ",
            assessment = "  BP 85/50  ",
            recommendation = "  Fluid bolus  ",
            originalTimestamp = 9999L
        )

        val updated = dao.updatedIsbar
        assertEquals(42, updated?.id)
        assertEquals("P-2002", updated?.patientIdentifier)
        assertEquals("Bed 12", updated?.identification)
        assertEquals("Hypotension", updated?.situation)
        assertEquals("Post-op sepsis", updated?.background)
        assertEquals("BP 85/50", updated?.assessment)
        assertEquals("Fluid bolus", updated?.recommendation)
        assertEquals(9999L, updated?.timestamp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateIsbarNoteRejectsNonPositiveId() = runTest {
        UpdateIsbarNoteUseCase(FakeClinicalPlanningDao())(
            id = 0,
            patientId = "P-1",
            identification = "Patient",
            situation = "Situation",
            background = "Background",
            assessment = "Assessment",
            recommendation = "Recommendation",
            originalTimestamp = 1000L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateIsbarNoteRejectsBlankPatientId() = runTest {
        UpdateIsbarNoteUseCase(FakeClinicalPlanningDao())(
            id = 1,
            patientId = "   ",
            identification = "Patient",
            situation = "Situation",
            background = "Background",
            assessment = "Assessment",
            recommendation = "Recommendation",
            originalTimestamp = 1000L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateIsbarNoteRejectsBlankIdentification() = runTest {
        UpdateIsbarNoteUseCase(FakeClinicalPlanningDao())(
            id = 1,
            patientId = "P-1",
            identification = "   ",
            situation = "Situation",
            background = "Background",
            assessment = "Assessment",
            recommendation = "Recommendation",
            originalTimestamp = 1000L
        )
    }

    @Test
    fun deleteIsbarNoteDelegatesToDao() = runTest {
        val dao = FakeClinicalPlanningDao()
        val note = IsbarNoteEntity(
            id = 15,
            patientIdentifier = "P-15",
            identification = "Ward 4",
            situation = "Situation",
            background = "Background",
            assessment = "Assessment",
            recommendation = "Recommendation",
            timestamp = 1000L
        )

        DeleteIsbarNoteUseCase(dao)(note)

        assertEquals(15, dao.deletedIsbar?.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun deleteIsbarNoteRejectsNonPositiveId() = runTest {
        val dao = FakeClinicalPlanningDao()
        val note = IsbarNoteEntity(
            id = 0,
            patientIdentifier = "P-0",
            identification = "Ward 4",
            situation = "Situation",
            background = "Background",
            assessment = "Assessment",
            recommendation = "Recommendation",
            timestamp = 1000L
        )

        DeleteIsbarNoteUseCase(dao)(note)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addClinicalTaskRejectsBlankName() = runTest {
        AddClinicalTaskUseCase(FakeClinicalPlanningDao())(
            taskName = "   ",
            description = "Task",
            priority = "HIGH",
            triggerTime = 1L,
            bypassDnd = false
        )
    }

    @Test
    fun addClinicalTaskTrimsInputAndDefaultsToIncomplete() = runTest {
        val dao = FakeClinicalPlanningDao()

        AddClinicalTaskUseCase(dao)(
            taskName = "  Review crash cart  ",
            description = "  Check emergency tray  ",
            priority = "  HIGH  ",
            triggerTime = 1234L,
            bypassDnd = true
        )

        val saved = dao.insertedTask
        assertEquals("Review crash cart", saved?.taskName)
        assertEquals("Check emergency tray", saved?.description)
        assertEquals("HIGH", saved?.priority)
        assertEquals(1234L, saved?.triggerTime)
        assertEquals(false, saved?.isCompleted)
        assertEquals(true, saved?.bypassDnd)
    }

    @Test
    fun updateClinicalTaskTrimsInputAndPreservesCompletionStatus() = runTest {
        val dao = FakeClinicalPlanningDao()

        UpdateClinicalTaskUseCase(dao)(
            taskId = 10,
            taskName = "  Check IV lines  ",
            description = "  Ensure patent flow  ",
            priority = "  URGENT  ",
            triggerTime = 5555L,
            isCompleted = true,
            bypassDnd = false
        )

        val updated = dao.updatedTask
        assertEquals(10, updated?.id)
        assertEquals("Check IV lines", updated?.taskName)
        assertEquals("Ensure patent flow", updated?.description)
        assertEquals("URGENT", updated?.priority)
        assertEquals(5555L, updated?.triggerTime)
        assertEquals(true, updated?.isCompleted)
        assertEquals(false, updated?.bypassDnd)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateClinicalTaskRejectsNonPositiveId() = runTest {
        UpdateClinicalTaskUseCase(FakeClinicalPlanningDao())(
            taskId = 0,
            taskName = "Task",
            description = "Desc",
            priority = "NORMAL",
            triggerTime = 100L,
            isCompleted = false,
            bypassDnd = false
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateClinicalTaskRejectsBlankName() = runTest {
        UpdateClinicalTaskUseCase(FakeClinicalPlanningDao())(
            taskId = 1,
            taskName = "   ",
            description = "Desc",
            priority = "NORMAL",
            triggerTime = 100L,
            isCompleted = false,
            bypassDnd = false
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateClinicalTaskRejectsNegativeTriggerTime() = runTest {
        UpdateClinicalTaskUseCase(FakeClinicalPlanningDao())(
            taskId = 1,
            taskName = "Task",
            description = "Desc",
            priority = "NORMAL",
            triggerTime = -1L,
            isCompleted = false,
            bypassDnd = false
        )
    }

    @Test
    fun deleteClinicalTaskDelegatesToDao() = runTest {
        val dao = FakeClinicalPlanningDao()
        val task = ClinicalTaskEntity(
            id = 7,
            taskName = "Blood sugar test",
            description = "Bed 5",
            priority = "HIGH",
            triggerTime = 500L,
            isCompleted = false,
            bypassDnd = false
        )

        DeleteClinicalTaskUseCase(dao)(task)

        assertEquals(7, dao.deletedTask?.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun deleteClinicalTaskRejectsNonPositiveId() = runTest {
        val dao = FakeClinicalPlanningDao()
        val task = ClinicalTaskEntity(
            id = 0,
            taskName = "Blood sugar test",
            description = "Bed 5",
            priority = "HIGH",
            triggerTime = 500L,
            isCompleted = false,
            bypassDnd = false
        )

        DeleteClinicalTaskUseCase(dao)(task)
    }

    @Test
    fun restoreIsbarNoteReinsertsNoteWithOriginalAttributes() = runTest {
        val dao = FakeClinicalPlanningDao()
        val note = IsbarNoteEntity(
            id = 42,
            patientIdentifier = "P-42",
            identification = "Ward 3 Bed 12",
            situation = "Post-op bleeding",
            background = "Laparotomy",
            assessment = "BP 90/60",
            recommendation = "Cross match 2 units",
            timestamp = 123456789L
        )

        RestoreIsbarNoteUseCase(dao)(note)

        val restored = dao.insertedIsbar
        assertEquals(42, restored?.id)
        assertEquals("P-42", restored?.patientIdentifier)
        assertEquals("Ward 3 Bed 12", restored?.identification)
        assertEquals("Post-op bleeding", restored?.situation)
        assertEquals("Laparotomy", restored?.background)
        assertEquals("BP 90/60", restored?.assessment)
        assertEquals("Cross match 2 units", restored?.recommendation)
        assertEquals(123456789L, restored?.timestamp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun restoreIsbarNoteRejectsNonPositiveId() = runTest {
        val dao = FakeClinicalPlanningDao()
        val note = IsbarNoteEntity(
            id = 0,
            patientIdentifier = "P-42",
            identification = "Ward 3 Bed 12",
            situation = "Post-op bleeding",
            background = "Laparotomy",
            assessment = "BP 90/60",
            recommendation = "Cross match 2 units",
            timestamp = 123456789L
        )

        RestoreIsbarNoteUseCase(dao)(note)
    }

    @Test
    fun restoreClinicalTaskReinsertsTaskWithOriginalAttributes() = runTest {
        val dao = FakeClinicalPlanningDao()
        val task = ClinicalTaskEntity(
            id = 99,
            taskName = "Administer IV Vancomycin",
            description = "1g in 250mL NS over 60 min",
            priority = "HIGH",
            triggerTime = 987654321L,
            isCompleted = false,
            bypassDnd = true
        )

        RestoreClinicalTaskUseCase(dao)(task)

        val restored = dao.insertedTask
        assertEquals(99, restored?.id)
        assertEquals("Administer IV Vancomycin", restored?.taskName)
        assertEquals("1g in 250mL NS over 60 min", restored?.description)
        assertEquals("HIGH", restored?.priority)
        assertEquals(987654321L, restored?.triggerTime)
        assertEquals(false, restored?.isCompleted)
        assertEquals(true, restored?.bypassDnd)
    }

    @Test(expected = IllegalArgumentException::class)
    fun restoreClinicalTaskRejectsNonPositiveId() = runTest {
        val dao = FakeClinicalPlanningDao()
        val task = ClinicalTaskEntity(
            id = 0,
            taskName = "Check vitals",
            description = "Bed 2",
            priority = "LOW",
            triggerTime = 100L,
            isCompleted = false,
            bypassDnd = false
        )

        RestoreClinicalTaskUseCase(dao)(task)
    }

    @Test
    fun purgeOldIsbarNotesUses48HourRetentionWindow() = runTest {
        val dao = FakeClinicalPlanningDao()
        val now = 10_000_000L

        PurgeOldIsbarNotesUseCase(dao)(now)

        assertEquals(now - (48L * 60L * 60L * 1000L), dao.deletedBefore)
    }

    @Test
    fun observeUseCasesDelegateToDao() = runTest {
        val note = IsbarNoteEntity(
            id = 1,
            patientIdentifier = "P1",
            identification = "Patient",
            situation = "Situation",
            background = "Background",
            assessment = "Assessment",
            recommendation = "Recommendation",
            timestamp = 1L
        )
        val task = ClinicalTaskEntity(
            id = 1,
            taskName = "Task",
            description = "Description",
            priority = "LOW",
            triggerTime = 2L,
            isCompleted = false,
            bypassDnd = false
        )
        val dao = FakeClinicalPlanningDao(
            notes = listOf(note),
            tasks = listOf(task)
        )

        assertEquals(listOf(note), ObserveIsbarNotesUseCase(dao)().first())
        assertEquals(listOf(task), ObserveClinicalTasksUseCase(dao)().first())
    }

    @Test
    fun setClinicalTaskCompletedDelegatesToDao() = runTest {
        val dao = FakeClinicalPlanningDao()

        SetClinicalTaskCompletedUseCase(dao)(7, true)

        assertEquals(7 to true, dao.completedTask)
    }

    @Test
    fun observeShiftWorkloadUsesActiveClaimPeriodWhenAvailable() = runTest {
        val today = LocalDate.of(2026, 3, 15)
        val period = ClaimPeriodEntity(
            id = 100L,
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 3, 31),
            createdAt = 1000L
        )

        val entry1 = DailyEntryEntity(
            id = 1L,
            claimPeriodId = 100L,
            date = LocalDate.of(2026, 3, 10),
            isPH = false,
            isDO = false,
            isLeave = false,
            leaveType = null,
            normalTimeIn = "07:00",
            normalTimeOut = "13:00",
            normalHours = 6.0f,
            otTimeIn = "13:00",
            otTimeOut = "19:00",
            otHours = 6.0f,
            wardOverride = "",
            reason = ""
        )

        val dailyDao = FakeDailyEntryDao(listOf(entry1))
        val claimDao = FakeClaimPeriodDao(listOf(period))

        val useCase = ObserveShiftWorkloadUseCase(dailyDao, claimDao)
        val state = useCase(today).first()

        assertTrue(state.hasRecordedShifts)
        assertTrue(state.periodLabel.contains("Claim Period"))
        assertEquals(LocalDate.of(2026, 3, 1), state.startDate)
        assertEquals(LocalDate.of(2026, 3, 31), state.endDate)
        assertEquals(12.0f, state.totalHours, 0.01f)
    }

    @Test
    fun observeShiftWorkloadFallsBackToRollingSevenDaysWhenNoActiveClaimPeriod() = runTest {
        val today = LocalDate.of(2026, 3, 15)
        // No active claim periods matching March 2026
        val dailyDao = FakeDailyEntryDao(emptyList())
        val claimDao = FakeClaimPeriodDao(emptyList())

        val useCase = ObserveShiftWorkloadUseCase(dailyDao, claimDao)
        val state = useCase(today).first()

        assertFalse(state.hasRecordedShifts)
        assertTrue(state.periodLabel.contains("Last 7 Days"))
        assertEquals(LocalDate.of(2026, 3, 9), state.startDate)
        assertEquals(today, state.endDate)
        assertEquals(0f, state.totalHours, 0.01f)
        assertTrue(state.suggestionText.contains("No recorded duty shifts in the last 7 days"))
    }

    @Test
    fun observeShiftWorkloadComputesConsecutiveNightShiftsAndWorkloadAlert() = runTest {
        val today = LocalDate.of(2026, 3, 15)
        val period = ClaimPeriodEntity(
            id = 1L,
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 3, 7),
            createdAt = 1000L
        )

        // 4 consecutive night shifts (19:00 to 07:00 next day = 12h each = 48h total over 7 days)
        val entries = (1..4).map { day ->
            DailyEntryEntity(
                id = day.toLong(),
                claimPeriodId = 1L,
                date = LocalDate.of(2026, 3, day),
                isPH = false,
                isDO = false,
                isLeave = false,
                leaveType = null,
                normalTimeIn = "19:00",
                normalTimeOut = "07:00",
                normalHours = 12.0f,
                otTimeIn = "",
                otTimeOut = "",
                otHours = 0f,
                wardOverride = "",
                reason = ""
            )
        }

        val dailyDao = FakeDailyEntryDao(entries)
        val claimDao = FakeClaimPeriodDao(listOf(period))

        val useCase = ObserveShiftWorkloadUseCase(dailyDao, claimDao)
        val state = useCase(LocalDate.of(2026, 3, 5)).first()

        assertTrue(state.hasRecordedShifts)
        assertEquals(4, state.consecutiveNightShifts)
        assertEquals(48.0f, state.totalHours, 0.01f)
        assertTrue(state.suggestionText.contains("4 consecutive night shifts detected"))
    }

    @Test
    fun observeShiftWorkloadHandlesUnparseableTimeEntriesGracefully() = runTest {
        val today = LocalDate.of(2026, 3, 15)
        val period = ClaimPeriodEntity(
            id = 1L,
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 3, 31),
            createdAt = 1000L
        )

        // Incomplete / unparseable shift times
        val entry = DailyEntryEntity(
            id = 1L,
            claimPeriodId = 1L,
            date = LocalDate.of(2026, 3, 10),
            isPH = false,
            isDO = false,
            isLeave = false,
            leaveType = null,
            normalTimeIn = "-",
            normalTimeOut = "invalid",
            normalHours = 0f,
            otTimeIn = "",
            otTimeOut = "",
            otHours = 0f,
            wardOverride = "",
            reason = ""
        )

        val dailyDao = FakeDailyEntryDao(listOf(entry))
        val claimDao = FakeClaimPeriodDao(listOf(period))

        val useCase = ObserveShiftWorkloadUseCase(dailyDao, claimDao)
        val state = useCase(today).first()

        // Filtered shift entries will be empty because time strings cannot be parsed
        assertFalse(state.hasRecordedShifts)
        assertEquals(0f, state.totalHours, 0.01f)
        assertTrue(state.suggestionText.contains("No recorded duty shifts"))
    }

    private class FakeClinicalPlanningDao(
        private val notes: List<IsbarNoteEntity> = emptyList(),
        private val tasks: List<ClinicalTaskEntity> = emptyList()
    ) : ClinicalPlanningDao {
        var insertedIsbar: IsbarNoteEntity? = null
        var updatedIsbar: IsbarNoteEntity? = null
        var deletedIsbar: IsbarNoteEntity? = null
        var insertedTask: ClinicalTaskEntity? = null
        var updatedTask: ClinicalTaskEntity? = null
        var deletedTask: ClinicalTaskEntity? = null
        var deletedBefore: Long? = null
        var completedTask: Pair<Int, Boolean>? = null

        override suspend fun insertIsbarNote(note: IsbarNoteEntity) {
            insertedIsbar = note
        }

        override suspend fun updateIsbarNote(note: IsbarNoteEntity) {
            updatedIsbar = note
        }

        override suspend fun deleteIsbarNote(note: IsbarNoteEntity) {
            deletedIsbar = note
        }

        override fun getAllIsbarNotes(): Flow<List<IsbarNoteEntity>> = flowOf(notes)

        override suspend fun deleteOldNotes(cutoffTime: Long) {
            deletedBefore = cutoffTime
        }

        override suspend fun insertTask(task: ClinicalTaskEntity) {
            insertedTask = task
        }

        override suspend fun updateTask(task: ClinicalTaskEntity) {
            updatedTask = task
        }

        override suspend fun deleteTask(task: ClinicalTaskEntity) {
            deletedTask = task
        }

        override fun getAllTasks(): Flow<List<ClinicalTaskEntity>> = flowOf(tasks)

        override suspend fun setTaskCompleted(taskId: Int, completed: Boolean) {
            completedTask = taskId to completed
        }
    }

    private class FakeDailyEntryDao(
        private val entries: List<DailyEntryEntity> = emptyList()
    ) : DailyEntryDao {
        override suspend fun insertEntry(entry: DailyEntryEntity) {}
        override suspend fun updateEntry(entry: DailyEntryEntity) {}
        override suspend fun upsertDailyEntry(entry: DailyEntryEntity) {}
        override fun observeEntriesForPeriod(claimPeriodId: Long): Flow<List<DailyEntryEntity>> =
            flowOf(entries.filter { it.claimPeriodId == claimPeriodId })
        override fun observeAllEntries(): Flow<List<DailyEntryEntity>> = flowOf(entries)
        override suspend fun getEntryForDate(claimPeriodId: Long, date: LocalDate): DailyEntryEntity? =
            entries.firstOrNull { it.claimPeriodId == claimPeriodId && it.date == date }
        override suspend fun deleteEntryById(id: Long) {}
        override suspend fun deleteEntriesForPeriod(claimPeriodId: Long) {}
        override suspend fun deleteAllEntries() {}
    }

    private class FakeClaimPeriodDao(
        private val periods: List<ClaimPeriodEntity> = emptyList()
    ) : ClaimPeriodDao {
        override suspend fun insertClaimPeriod(claimPeriod: ClaimPeriodEntity): Long = 1L
        override fun observeClaimPeriods(): Flow<List<ClaimPeriodEntity>> = flowOf(periods)
        override suspend fun getClaimPeriodById(id: Long): ClaimPeriodEntity? =
            periods.firstOrNull { it.id == id }
        override suspend fun getLatestClaimPeriod(): ClaimPeriodEntity? = periods.firstOrNull()
        override suspend fun deleteClaimPeriod(claimPeriod: ClaimPeriodEntity) {}
        override suspend fun deleteAllClaimPeriods() {}
    }
}
