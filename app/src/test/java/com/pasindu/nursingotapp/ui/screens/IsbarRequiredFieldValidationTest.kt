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
 * JVM unit tests for ISBAR Required-Field Validation (Step 2).
 * Verifies that empty patient ID or empty identification cannot trigger save,
 * displays user-friendly error messages, preserves all entered text,
 * safely handles save failures, and retains normal save behavior on valid input.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IsbarRequiredFieldValidationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    data class CapturedSaveCall(
        val patientId: String,
        val identification: String,
        val situation: String,
        val background: String,
        val assessment: String,
        val recommendation: String
    )

    @Test
    fun emptyPatientId_failsValidation_doesNotInvokeSave_setsPatientIdError_preservesFields() {
        var saveInvoked = false
        var dialogDismissed = false

        val coordinator = IsbarFormCoordinator(
            saveNote = { _, _, _, _, _, _, _, _ ->
                saveInvoked = true
            },
            onDismissDialog = { dialogDismissed = true }
        )

        coordinator.patientId = "   " // blank
        coordinator.identification = "Ward 3 Bed 4"
        coordinator.situation = "Sudden drop in oxygen saturation"
        coordinator.background = "Admitted with pneumonia"
        coordinator.assessment = "SpO2 88% on room air"
        coordinator.recommendation = "Start 4L O2 via nasal cannula"

        coordinator.handleSave()

        assertFalse("Save should not be invoked when patient ID is empty", saveInvoked)
        assertFalse("Dialog must remain open when validation fails", dialogDismissed)
        assertEquals("Patient ID is required.", coordinator.patientIdError)
        assertNull("Identification error should be null when identification is valid", coordinator.identificationError)
        assertEquals("Please provide a Patient ID.", coordinator.generalError)

        // Verify all fields are strictly preserved
        assertEquals("   ", coordinator.patientId)
        assertEquals("Ward 3 Bed 4", coordinator.identification)
        assertEquals("Sudden drop in oxygen saturation", coordinator.situation)
        assertEquals("Admitted with pneumonia", coordinator.background)
        assertEquals("SpO2 88% on room air", coordinator.assessment)
        assertEquals("Start 4L O2 via nasal cannula", coordinator.recommendation)
    }

    @Test
    fun emptyIdentification_failsValidation_doesNotInvokeSave_setsIdentificationError_preservesFields() {
        var saveInvoked = false
        var dialogDismissed = false

        val coordinator = IsbarFormCoordinator(
            saveNote = { _, _, _, _, _, _, _, _ ->
                saveInvoked = true
            },
            onDismissDialog = { dialogDismissed = true }
        )

        coordinator.patientId = "PT-88321"
        coordinator.identification = "" // empty
        coordinator.situation = "Post-operative nausea and vomiting"
        coordinator.background = "Cholecystectomy completed 4 hours ago"
        coordinator.assessment = "Vomited 150ml bilious fluid"
        coordinator.recommendation = "Administer ondansetron 4mg IV"

        coordinator.handleSave()

        assertFalse("Save should not be invoked when identification is empty", saveInvoked)
        assertFalse("Dialog must remain open when validation fails", dialogDismissed)
        assertNull("Patient ID error should be null when patient ID is valid", coordinator.patientIdError)
        assertEquals("Identification is required.", coordinator.identificationError)
        assertEquals("Please provide an Identification.", coordinator.generalError)

        // Verify all fields are preserved
        assertEquals("PT-88321", coordinator.patientId)
        assertEquals("", coordinator.identification)
        assertEquals("Post-operative nausea and vomiting", coordinator.situation)
        assertEquals("Cholecystectomy completed 4 hours ago", coordinator.background)
        assertEquals("Vomited 150ml bilious fluid", coordinator.assessment)
        assertEquals("Administer ondansetron 4mg IV", coordinator.recommendation)
    }

    @Test
    fun bothRequiredFieldsEmpty_failsValidation_doesNotInvokeSave_setsBothErrors() {
        var saveInvoked = false
        var dialogDismissed = false

        val coordinator = IsbarFormCoordinator(
            saveNote = { _, _, _, _, _, _, _, _ ->
                saveInvoked = true
            },
            onDismissDialog = { dialogDismissed = true }
        )

        coordinator.patientId = "   "
        coordinator.identification = "   "
        coordinator.situation = "Blood pressure spiking"

        coordinator.handleSave()

        assertFalse("Save must not be invoked when both required fields are empty", saveInvoked)
        assertFalse("Dialog must stay open", dialogDismissed)
        assertEquals("Patient ID is required.", coordinator.patientIdError)
        assertEquals("Identification is required.", coordinator.identificationError)
        assertEquals("Please provide both Patient ID and Identification.", coordinator.generalError)
        assertEquals("Blood pressure spiking", coordinator.situation)
    }

    @Test
    fun validRequiredFields_invokesSaving_clearsFieldsAndDismissesOnSuccess() {
        val capturedCalls = mutableListOf<CapturedSaveCall>()
        var dialogDismissed = false

        val coordinator = IsbarFormCoordinator(
            saveNote = { patientId, identification, situation, background, assessment, recommendation, onSuccess, _ ->
                capturedCalls.add(
                    CapturedSaveCall(patientId, identification, situation, background, assessment, recommendation)
                )
                onSuccess()
            },
            onDismissDialog = { dialogDismissed = true }
        )

        coordinator.patientId = "PT-90210"
        coordinator.identification = "Dr. Silva / Bed 7"
        coordinator.situation = "Post-op fever spike"
        coordinator.background = "Post-appendectomy Day 2"
        coordinator.assessment = "Temp 38.9C, HR 105"
        coordinator.recommendation = "Blood cultures and paracetamol 1g IV"

        coordinator.handleSave()

        assertEquals(1, capturedCalls.size)
        val call = capturedCalls.first()
        assertEquals("PT-90210", call.patientId)
        assertEquals("Dr. Silva / Bed 7", call.identification)
        assertEquals("Post-op fever spike", call.situation)
        assertEquals("Post-appendectomy Day 2", call.background)
        assertEquals("Temp 38.9C, HR 105", call.assessment)
        assertEquals("Blood cultures and paracetamol 1g IV", call.recommendation)

        assertTrue("Dialog must be dismissed after successful save", dialogDismissed)
        assertFalse(coordinator.isSubmitting)
        assertEquals("", coordinator.patientId)
        assertEquals("", coordinator.identification)
        assertEquals("", coordinator.situation)
        assertEquals("", coordinator.background)
        assertEquals("", coordinator.assessment)
        assertEquals("", coordinator.recommendation)
        assertNull(coordinator.patientIdError)
        assertNull(coordinator.identificationError)
        assertNull(coordinator.generalError)
    }

    @Test
    fun validationFailure_doesNotClearEnteredFields() {
        val coordinator = IsbarFormCoordinator(
            saveNote = { _, _, _, _, _, _, _, _ -> },
            onDismissDialog = {}
        )

        // Patient fills clinical details first, misses required identification
        coordinator.patientId = "PT-55443"
        coordinator.identification = ""
        coordinator.situation = "Acute chest discomfort"
        coordinator.background = "Known CAD with stents"
        coordinator.assessment = "Troponin borderline, ECG pending"
        coordinator.recommendation = "Urgent Cardiology consult"

        coordinator.handleSave()

        // Verify not a single character was lost
        assertEquals("PT-55443", coordinator.patientId)
        assertEquals("", coordinator.identification)
        assertEquals("Acute chest discomfort", coordinator.situation)
        assertEquals("Known CAD with stents", coordinator.background)
        assertEquals("Troponin borderline, ECG pending", coordinator.assessment)
        assertEquals("Urgent Cardiology consult", coordinator.recommendation)
    }

    @Test
    fun saveFailure_handledSafelyWithoutCrashing_preservesInput_keepsDialogOpen() {
        var dialogDismissed = false

        val coordinator = IsbarFormCoordinator(
            saveNote = { _, _, _, _, _, _, _, onError ->
                onError(RuntimeException("SQLiteDiskIOException: Disk is full"))
            },
            onDismissDialog = { dialogDismissed = true }
        )

        coordinator.patientId = "PT-11223"
        coordinator.identification = "ICU Bed 2"
        coordinator.situation = "Bradycardia 42 bpm"
        coordinator.background = "Beta-blocker overdose suspected"
        coordinator.assessment = "Hemodynamically unstable"
        coordinator.recommendation = "Atropine 0.5mg IV stat"

        coordinator.handleSave()

        assertFalse("Dialog must NOT be dismissed on save error", dialogDismissed)
        assertFalse(coordinator.isSubmitting)
        assertEquals("SQLiteDiskIOException: Disk is full", coordinator.generalError)

        // Form fields must be preserved for user to retry or copy
        assertEquals("PT-11223", coordinator.patientId)
        assertEquals("ICU Bed 2", coordinator.identification)
        assertEquals("Bradycardia 42 bpm", coordinator.situation)
        assertEquals("Beta-blocker overdose suspected", coordinator.background)
        assertEquals("Hemodynamically unstable", coordinator.assessment)
        assertEquals("Atropine 0.5mg IV stat", coordinator.recommendation)
    }

    @Test
    fun viewModel_addIsbarNote_withInvalidInput_callsOnErrorSafelyWithoutCrashing() = runTest {
        val fakeDao = FakeClinicalPlanningDao()
        val viewModel = createViewModel(fakeDao)

        var errorCaptured: Throwable? = null
        var successCalled = false

        viewModel.addIsbarNote(
            patientId = "",
            identification = "Valid Identification",
            situation = "Situation",
            background = "Background",
            assessment = "Assessment",
            recommendation = "Recommendation",
            onSuccess = { successCalled = true },
            onError = { errorCaptured = it }
        )

        advanceUntilIdle()

        assertFalse("onSuccess must not be called when validation fails in use case", successCalled)
        assertNotNull("onError must capture the domain validation exception", errorCaptured)
        assertTrue(
            "Exception should be IllegalArgumentException",
            errorCaptured is IllegalArgumentException
        )
        assertEquals("Patient identifier cannot be blank.", errorCaptured?.message)
        assertNull("DAO should not have received any entity", fakeDao.insertedIsbar)
    }

    @Test
    fun viewModel_addIsbarNote_withValidInput_savesToDaoAndCallsOnSuccess() = runTest {
        val fakeDao = FakeClinicalPlanningDao()
        val viewModel = createViewModel(fakeDao)

        var errorCaptured: Throwable? = null
        var successCalled = false

        viewModel.addIsbarNote(
            patientId = "PT-4455",
            identification = "Bed 9",
            situation = "Fever",
            background = "Post-op",
            assessment = "Stable",
            recommendation = "Monitor",
            onSuccess = { successCalled = true },
            onError = { errorCaptured = it }
        )

        advanceUntilIdle()

        assertTrue("onSuccess must be called when note is saved", successCalled)
        assertNull("onError must not be called on valid save", errorCaptured)
        assertNotNull("DAO must have received the note", fakeDao.insertedIsbar)
        assertEquals("PT-4455", fakeDao.insertedIsbar?.patientIdentifier)
        assertEquals("Bed 9", fakeDao.insertedIsbar?.identification)
    }

    @Test
    fun coroutineCancellation_isRethrownAndNotSwallowed() = runTest {
        val cancellingDao = CancellingClinicalPlanningDao()
        val viewModel = createViewModel(cancellingDao)

        var errorCalled = false
        var cancellationObserved = false

        try {
            val job = viewModel.addIsbarNote(
                patientId = "PT-1",
                identification = "Bed 1",
                situation = "Situation",
                background = "Background",
                assessment = "Assessment",
                recommendation = "Recommendation",
                onSuccess = {},
                onError = { errorCalled = true }
            )
            job.join()
            if (job.isCancelled) {
                cancellationObserved = true
            }
        } catch (e: CancellationException) {
            cancellationObserved = true
        }

        advanceUntilIdle()

        assertFalse("CancellationException must NOT be caught and routed to onError", errorCalled)
        assertTrue("Job or caller must observe cancellation", cancellationObserved)
    }

    private class CancellingClinicalPlanningDao : ClinicalPlanningDao {
        override suspend fun insertIsbarNote(note: IsbarNoteEntity) {
            throw CancellationException("Simulated cancellation")
        }

        override suspend fun updateIsbarNote(note: IsbarNoteEntity) {
            throw CancellationException("Simulated cancellation")
        }

        override suspend fun deleteIsbarNote(note: IsbarNoteEntity) {
            throw CancellationException("Simulated cancellation")
        }

        override fun getAllIsbarNotes(): Flow<List<IsbarNoteEntity>> = flowOf(emptyList())

        override suspend fun deleteOldNotes(cutoffTime: Long) {}

        override suspend fun insertTask(task: ClinicalTaskEntity) {
            throw CancellationException("Simulated cancellation")
        }

        override suspend fun updateTask(task: ClinicalTaskEntity) {
            throw CancellationException("Simulated cancellation")
        }

        override suspend fun deleteTask(task: ClinicalTaskEntity) {
            throw CancellationException("Simulated cancellation")
        }

        override fun getAllTasks(): Flow<List<ClinicalTaskEntity>> = flowOf(emptyList())

        override suspend fun setTaskCompleted(taskId: Int, completed: Boolean) {}
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
        var insertedIsbar: IsbarNoteEntity? = null
        var insertedTask: ClinicalTaskEntity? = null
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
