package com.pasindu.nursingotapp.ui.screens

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import com.pasindu.nursingotapp.domain.usecase.AddClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.AddIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClinicalTasksUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveShiftWorkloadUseCase
import com.pasindu.nursingotapp.domain.usecase.PurgeOldIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.RestoreClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.RestoreIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.SetClinicalTaskCompletedUseCase
import com.pasindu.nursingotapp.domain.usecase.UpdateClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.UpdateIsbarNoteUseCase
import com.pasindu.nursingotapp.ui.ClinicalPlanningViewModel
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * JVM unit tests for Clinical Task Form Validation, Priority Mapping, and Safe Persistence (Step 3).
 * Verifies required task name validation, explicit priority selection, draft preservation,
 * Bypass DND defaulting to false, safe error handling, and structured coroutine cancellation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClinicalTaskFormValidationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    data class CapturedTaskCall(
        val taskName: String,
        val description: String,
        val priority: String,
        val triggerTime: Long,
        val bypassDnd: Boolean
    )

    @Test
    fun emptyTaskName_failsValidation_doesNotInvokeSave_setsTaskNameError_preservesFields() {
        var saveInvoked = false
        var dialogDismissed = false

        val coordinator = ClinicalTaskFormCoordinator(
            saveTask = { _, _, _, _, _, _, _ -> saveInvoked = true },
            onDismissDialog = { dialogDismissed = true }
        )

        coordinator.taskName = "   " // blank
        coordinator.description = "Administer IV Cefuroxime 1.5g"
        coordinator.selectedPriority = "HIGH"
        coordinator.bypassDnd = true

        coordinator.handleSave()

        assertFalse("Save should not be called when task name is blank", saveInvoked)
        assertFalse("Dialog must remain open on validation failure", dialogDismissed)
        assertEquals("Task name is required.", coordinator.taskNameError)
        assertNull("Priority error should be null when priority is selected", coordinator.priorityError)
        assertEquals("Please provide a task name.", coordinator.generalError)

        // Strict draft preservation
        assertEquals("   ", coordinator.taskName)
        assertEquals("Administer IV Cefuroxime 1.5g", coordinator.description)
        assertEquals("HIGH", coordinator.selectedPriority)
        assertTrue("Bypass DND must remain preserved as user set it", coordinator.bypassDnd)
    }

    @Test
    fun unselectedPriority_failsValidation_doesNotInvokeSave_setsPriorityError_preservesFields() {
        var saveInvoked = false
        var dialogDismissed = false

        val coordinator = ClinicalTaskFormCoordinator(
            saveTask = { _, _, _, _, _, _, _ -> saveInvoked = true },
            onDismissDialog = { dialogDismissed = true }
        )

        coordinator.taskName = "Continuous telemetry monitoring"
        coordinator.description = "Check for arrhythmias every 2 hours"
        coordinator.selectedPriority = null // Unselected

        coordinator.handleSave()

        assertFalse("Save should not be called when priority is unselected", saveInvoked)
        assertFalse("Dialog must remain open on validation failure", dialogDismissed)
        assertNull("Task name error should be null when task name is valid", coordinator.taskNameError)
        assertEquals("Please select a priority level.", coordinator.priorityError)
        assertEquals("Please select a priority level.", coordinator.generalError)

        assertEquals("Continuous telemetry monitoring", coordinator.taskName)
        assertEquals("Check for arrhythmias every 2 hours", coordinator.description)
    }

    @Test
    fun bothTaskNameAndPriorityMissing_failsValidation_doesNotInvokeSave() {
        var saveInvoked = false

        val coordinator = ClinicalTaskFormCoordinator(
            saveTask = { _, _, _, _, _, _, _ -> saveInvoked = true },
            onDismissDialog = {}
        )

        coordinator.taskName = ""
        coordinator.selectedPriority = null
        coordinator.description = "Some notes"

        coordinator.handleSave()

        assertFalse(saveInvoked)
        assertEquals("Task name is required.", coordinator.taskNameError)
        assertEquals("Please select a priority level.", coordinator.priorityError)
        assertEquals("Please provide a task name and select a priority level.", coordinator.generalError)
        assertEquals("Some notes", coordinator.description)
    }

    @Test
    fun validInputs_invokesSaveWithExactStoredPriority_high() {
        val capturedCalls = mutableListOf<CapturedTaskCall>()
        var dialogDismissed = false

        val coordinator = ClinicalTaskFormCoordinator(
            saveTask = { name, desc, prio, time, dnd, onSuccess, _ ->
                capturedCalls.add(CapturedTaskCall(name, desc, prio, time, dnd))
                onSuccess()
            },
            onDismissDialog = { dialogDismissed = true }
        )

        coordinator.taskName = "STAT Potassium Replacement"
        coordinator.description = "20mmol KCl in 500ml Normal Saline over 2h"
        coordinator.onPrioritySelected("HIGH")
        coordinator.bypassDnd = true

        val triggerTime = 1700000000000L
        coordinator.handleSave(triggerTime)

        assertEquals(1, capturedCalls.size)
        val call = capturedCalls.first()
        assertEquals("STAT Potassium Replacement", call.taskName)
        assertEquals("20mmol KCl in 500ml Normal Saline over 2h", call.description)
        assertEquals("HIGH", call.priority)
        assertEquals(triggerTime, call.triggerTime)
        assertTrue(call.bypassDnd)

        assertTrue("Dialog must be dismissed after success", dialogDismissed)
        assertFalse(coordinator.isSubmitting)
        assertEquals("", coordinator.taskName)
        assertEquals("", coordinator.description)
        assertNull(coordinator.selectedPriority)
        assertFalse(coordinator.bypassDnd)
    }

    @Test
    fun validInputs_invokesSaveWithExactStoredPriority_mediumAndLow() {
        val capturedCalls = mutableListOf<CapturedTaskCall>()

        val coordinator = ClinicalTaskFormCoordinator(
            saveTask = { name, desc, prio, time, dnd, onSuccess, _ ->
                capturedCalls.add(CapturedTaskCall(name, desc, prio, time, dnd))
                onSuccess()
            }
        )

        // Medium priority test
        coordinator.taskName = "Wound dressing change"
        coordinator.onPrioritySelected("MEDIUM")
        coordinator.handleSave(100L)

        assertEquals(1, capturedCalls.size)
        assertEquals("MEDIUM", capturedCalls[0].priority)
        assertFalse(capturedCalls[0].bypassDnd)

        // Low priority test
        coordinator.taskName = "Discharge summary review"
        coordinator.onPrioritySelected("LOW")
        coordinator.handleSave(200L)

        assertEquals(2, capturedCalls.size)
        assertEquals("LOW", capturedCalls[1].priority)
        assertFalse(capturedCalls[1].bypassDnd)
    }

    @Test
    fun bypassDnd_defaultsToFalse_andNeverAutoEnablesOnPriority() {
        val coordinator = ClinicalTaskFormCoordinator()

        // 1. Must default to false on initialization
        assertFalse("Bypass DND must default to FALSE", coordinator.bypassDnd)
        assertNull("Selected priority must default to null", coordinator.selectedPriority)

        // 2. Selecting HIGH priority must NEVER auto-enable bypass DND
        coordinator.onPrioritySelected("HIGH")
        assertFalse("Selecting HIGH priority must NOT auto-enable bypass DND", coordinator.bypassDnd)

        // 3. Selecting MEDIUM or LOW must not alter bypass DND
        coordinator.onPrioritySelected("MEDIUM")
        assertFalse(coordinator.bypassDnd)
        coordinator.onPrioritySelected("LOW")
        assertFalse(coordinator.bypassDnd)

        // 4. Explicit user selection to true is respected
        coordinator.bypassDnd = true
        assertTrue(coordinator.bypassDnd)

        // 5. Reset restores default false
        coordinator.resetFields()
        assertFalse("Reset must restore bypass DND to false", coordinator.bypassDnd)
    }

    @Test
    fun validationFailure_doesNotClearEnteredFields() {
        val coordinator = ClinicalTaskFormCoordinator()

        coordinator.taskName = "" // will fail
        coordinator.description = "Complex clinical procedure notes"
        coordinator.selectedPriority = "MEDIUM"
        coordinator.bypassDnd = true

        coordinator.handleSave()

        // Verify draft preservation
        assertEquals("", coordinator.taskName)
        assertEquals("Complex clinical procedure notes", coordinator.description)
        assertEquals("MEDIUM", coordinator.selectedPriority)
        assertTrue(coordinator.bypassDnd)
    }

    @Test
    fun saveFailure_handledSafelyWithoutCrashing_preservesDraft() {
        var dialogDismissed = false

        val coordinator = ClinicalTaskFormCoordinator(
            saveTask = { _, _, _, _, _, _, onError ->
                onError(RuntimeException("SQLiteDatabaseCorruptException: Disk corrupt"))
            },
            onDismissDialog = { dialogDismissed = true }
        )

        coordinator.taskName = "Bedside Blood Sugar Check"
        coordinator.description = "Before dinner insulin"
        coordinator.onPrioritySelected("HIGH")
        coordinator.bypassDnd = true

        coordinator.handleSave()

        assertFalse("Dialog must not be dismissed on save error", dialogDismissed)
        assertFalse(coordinator.isSubmitting)
        assertEquals("SQLiteDatabaseCorruptException: Disk corrupt", coordinator.generalError)

        // Form draft must be preserved
        assertEquals("Bedside Blood Sugar Check", coordinator.taskName)
        assertEquals("Before dinner insulin", coordinator.description)
        assertEquals("HIGH", coordinator.selectedPriority)
        assertTrue(coordinator.bypassDnd)
    }

    @Test
    fun viewModel_addTask_withInvalidInput_callsOnErrorSafelyWithoutCrashing() = runTest {
        val fakeDao = FakeClinicalPlanningDao()
        val viewModel = createViewModel(fakeDao)

        var capturedError: Throwable? = null
        var successCalled = false

        viewModel.addTask(
            taskName = "   ", // blank
            description = "Desc",
            priority = "HIGH",
            triggerTime = 1000L,
            bypassDnd = false,
            onSuccess = { successCalled = true },
            onError = { capturedError = it }
        )

        advanceUntilIdle()

        assertFalse(successCalled)
        assertNotNull("onError must capture validation exception", capturedError)
        assertTrue(capturedError is IllegalArgumentException)
        assertEquals("Task name cannot be blank.", capturedError?.message)
        assertNull(fakeDao.insertedTask)
    }

    @Test
    fun viewModel_addTask_withValidInput_savesToDaoAndCallsOnSuccess() = runTest {
        val fakeDao = FakeClinicalPlanningDao()
        val viewModel = createViewModel(fakeDao)

        var successCalled = false
        var capturedError: Throwable? = null

        viewModel.addTask(
            taskName = "Infusion pump check",
            description = "Noradrenaline rate verification",
            priority = "HIGH",
            triggerTime = 5000L,
            bypassDnd = true,
            onSuccess = { successCalled = true },
            onError = { capturedError = it }
        )

        advanceUntilIdle()

        assertTrue(successCalled)
        assertNull(capturedError)
        assertNotNull(fakeDao.insertedTask)
        assertEquals("Infusion pump check", fakeDao.insertedTask?.taskName)
        assertEquals("Noradrenaline rate verification", fakeDao.insertedTask?.description)
        assertEquals("HIGH", fakeDao.insertedTask?.priority)
        assertEquals(5000L, fakeDao.insertedTask?.triggerTime)
        assertTrue(fakeDao.insertedTask?.bypassDnd == true)
    }

    @Test
    fun coroutineCancellation_isRethrownAndNotSwallowed() = runTest {
        val cancellingDao = CancellingClinicalPlanningDao()
        val viewModel = createViewModel(cancellingDao)

        var errorCalled = false
        var cancellationCaughtByCaller = false

        try {
            val job = viewModel.addTask(
                taskName = "Test Task",
                description = "Desc",
                priority = "HIGH",
                triggerTime = 1000L,
                bypassDnd = false,
                onSuccess = {},
                onError = { errorCalled = true }
            )
            job.join()
            if (job.isCancelled) {
                cancellationCaughtByCaller = true
            }
        } catch (e: CancellationException) {
            cancellationCaughtByCaller = true
        }

        advanceUntilIdle()

        assertFalse("CancellationException must NOT be caught and routed to onError", errorCalled)
        assertTrue("Job or caller must register cancellation", cancellationCaughtByCaller)
    }

    private fun createViewModel(dao: ClinicalPlanningDao): ClinicalPlanningViewModel {
        return ClinicalPlanningViewModel(
            observeIsbarNotesUseCase = ObserveIsbarNotesUseCase(dao),
            observeClinicalTasksUseCase = ObserveClinicalTasksUseCase(dao),
            addIsbarNoteUseCase = AddIsbarNoteUseCase(dao),
            updateIsbarNoteUseCase = UpdateIsbarNoteUseCase(dao),
            deleteIsbarNoteUseCase = DeleteIsbarNoteUseCase(dao),
            restoreIsbarNoteUseCase = RestoreIsbarNoteUseCase(dao),
            purgeOldIsbarNotesUseCase = PurgeOldIsbarNotesUseCase(dao),
            addClinicalTaskUseCase = AddClinicalTaskUseCase(dao),
            updateClinicalTaskUseCase = UpdateClinicalTaskUseCase(dao),
            deleteClinicalTaskUseCase = DeleteClinicalTaskUseCase(dao),
            restoreClinicalTaskUseCase = RestoreClinicalTaskUseCase(dao),
            setClinicalTaskCompletedUseCase = SetClinicalTaskCompletedUseCase(dao),
            observeShiftWorkloadUseCase = ObserveShiftWorkloadUseCase(StubDailyEntryDao(), StubClaimPeriodDao())
        )
    }

    private class FakeClinicalPlanningDao : ClinicalPlanningDao {
        var insertedTask: ClinicalTaskEntity? = null
        var insertedIsbar: IsbarNoteEntity? = null
        var deletedBefore: Long? = null
        var completedTask: Pair<Int, Boolean>? = null

        override suspend fun insertIsbarNote(note: IsbarNoteEntity) {
            insertedIsbar = note
        }

        override suspend fun updateIsbarNote(note: IsbarNoteEntity) {}

        override suspend fun deleteIsbarNote(note: IsbarNoteEntity) {}

        override fun getAllIsbarNotes(): Flow<List<IsbarNoteEntity>> = flowOf(emptyList())

        override suspend fun deleteOldNotes(cutoffTime: Long) {
            deletedBefore = cutoffTime
        }

        override suspend fun insertTask(task: ClinicalTaskEntity) {
            insertedTask = task
        }

        override suspend fun updateTask(task: ClinicalTaskEntity) {}

        override suspend fun deleteTask(task: ClinicalTaskEntity) {}

        override fun getAllTasks(): Flow<List<ClinicalTaskEntity>> = flowOf(emptyList())

        override suspend fun setTaskCompleted(taskId: Int, completed: Boolean) {
            completedTask = taskId to completed
        }
    }

    private class CancellingClinicalPlanningDao : ClinicalPlanningDao {
        override suspend fun insertIsbarNote(note: IsbarNoteEntity) {
            throw CancellationException("Simulated coroutine cancellation")
        }

        override suspend fun updateIsbarNote(note: IsbarNoteEntity) {
            throw CancellationException("Simulated coroutine cancellation")
        }

        override suspend fun deleteIsbarNote(note: IsbarNoteEntity) {
            throw CancellationException("Simulated coroutine cancellation")
        }

        override fun getAllIsbarNotes(): Flow<List<IsbarNoteEntity>> = flowOf(emptyList())

        override suspend fun deleteOldNotes(cutoffTime: Long) {}

        override suspend fun insertTask(task: ClinicalTaskEntity) {
            throw CancellationException("Simulated coroutine cancellation")
        }

        override suspend fun updateTask(task: ClinicalTaskEntity) {
            throw CancellationException("Simulated coroutine cancellation")
        }

        override suspend fun deleteTask(task: ClinicalTaskEntity) {
            throw CancellationException("Simulated coroutine cancellation")
        }

        override fun getAllTasks(): Flow<List<ClinicalTaskEntity>> = flowOf(emptyList())

        override suspend fun setTaskCompleted(taskId: Int, completed: Boolean) {}
    }

    private class StubDailyEntryDao : DailyEntryDao {
        override suspend fun insertEntry(entry: DailyEntryEntity) {}
        override suspend fun updateEntry(entry: DailyEntryEntity) {}
        override suspend fun upsertDailyEntry(entry: DailyEntryEntity) {}
        override fun observeEntriesForPeriod(claimPeriodId: Long): Flow<List<DailyEntryEntity>> = flowOf(emptyList())
        override fun observeAllEntries(): Flow<List<DailyEntryEntity>> = flowOf(emptyList())
        override suspend fun getEntryForDate(claimPeriodId: Long, date: LocalDate): DailyEntryEntity? = null
        override suspend fun deleteEntryById(id: Long) {}
        override suspend fun deleteEntriesForPeriod(claimPeriodId: Long) {}
        override suspend fun deleteAllEntries() {}
    }

    private class StubClaimPeriodDao : ClaimPeriodDao {
        override suspend fun insertClaimPeriod(claimPeriod: ClaimPeriodEntity): Long = 1L
        override fun observeClaimPeriods(): Flow<List<ClaimPeriodEntity>> = flowOf(emptyList())
        override suspend fun getClaimPeriodById(id: Long): ClaimPeriodEntity? = null
        override suspend fun getLatestClaimPeriod(): ClaimPeriodEntity? = null
        override suspend fun deleteClaimPeriod(claimPeriod: ClaimPeriodEntity) {}
        override suspend fun deleteAllClaimPeriods() {}
    }
}
