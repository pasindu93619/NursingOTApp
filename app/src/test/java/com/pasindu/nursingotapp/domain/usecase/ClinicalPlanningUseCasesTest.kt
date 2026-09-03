package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private class FakeClinicalPlanningDao(
        private val notes: List<IsbarNoteEntity> = emptyList(),
        private val tasks: List<ClinicalTaskEntity> = emptyList()
    ) : ClinicalPlanningDao {
        var insertedIsbar: IsbarNoteEntity? = null
        var insertedTask: ClinicalTaskEntity? = null
        var deletedBefore: Long? = null
        var completedTask: Pair<Int, Boolean>? = null

        override suspend fun insertIsbarNote(note: IsbarNoteEntity) {
            insertedIsbar = note
        }

        override fun getAllIsbarNotes(): Flow<List<IsbarNoteEntity>> = flowOf(notes)

        override suspend fun deleteOldNotes(cutoffTime: Long) {
            deletedBefore = cutoffTime
        }

        override suspend fun insertTask(task: ClinicalTaskEntity) {
            insertedTask = task
        }

        override fun getAllTasks(): Flow<List<ClinicalTaskEntity>> = flowOf(tasks)

        override suspend fun setTaskCompleted(taskId: Int, completed: Boolean) {
            completedTask = taskId to completed
        }
    }
}
