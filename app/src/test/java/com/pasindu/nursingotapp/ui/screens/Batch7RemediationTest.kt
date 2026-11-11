package com.pasindu.nursingotapp.ui.screens

import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import com.pasindu.nursingotapp.domain.usecase.RestoreClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.RestoreIsbarNoteUseCase
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch7RemediationTest {

    @Test
    fun verifySharedCornerRadiusTokens() {
        assertEquals(8.dp, NursingDimensions.Radius.small)
        assertEquals(12.dp, NursingDimensions.Radius.medium)
        assertEquals(16.dp, NursingDimensions.Radius.large)
        assertEquals(20.dp, NursingDimensions.Radius.extraLarge)
        assertEquals(24.dp, NursingDimensions.Radius.hero)
        assertEquals(999.dp, NursingDimensions.Radius.pill)
    }

    @Test
    fun verifyRestoreIsbarNotePreservesAllFields() = runTest {
        var insertedNote: IsbarNoteEntity? = null
        val fakeDao = object : ClinicalPlanningDao {
            override suspend fun insertIsbarNote(note: IsbarNoteEntity) {
                insertedNote = note
            }
            override suspend fun updateIsbarNote(note: IsbarNoteEntity) {}
            override suspend fun deleteIsbarNote(note: IsbarNoteEntity) {}
            override fun getAllIsbarNotes(): Flow<List<IsbarNoteEntity>> = emptyFlow()
            override suspend fun deleteOldNotes(cutoffTime: Long) {}
            override suspend fun insertTask(task: ClinicalTaskEntity) {}
            override suspend fun updateTask(task: ClinicalTaskEntity) {}
            override suspend fun deleteTask(task: ClinicalTaskEntity) {}
            override fun getAllTasks(): Flow<List<ClinicalTaskEntity>> = emptyFlow()
            override suspend fun setTaskCompleted(taskId: Int, completed: Boolean) {}
        }

        val originalNote = IsbarNoteEntity(
            id = 501,
            patientIdentifier = "PT-8821",
            identification = "ICU Bed 4",
            situation = "Acute respiratory distress",
            background = "COPD exacerbation",
            assessment = "SpO2 88% on 4L O2",
            recommendation = "Escalate to BiPAP",
            timestamp = 1700000000000L
        )

        val useCase = RestoreIsbarNoteUseCase(fakeDao)
        useCase(originalNote)

        assertNotNull(insertedNote)
        assertEquals(501, insertedNote?.id)
        assertEquals("PT-8821", insertedNote?.patientIdentifier)
        assertEquals("ICU Bed 4", insertedNote?.identification)
        assertEquals("Acute respiratory distress", insertedNote?.situation)
        assertEquals("COPD exacerbation", insertedNote?.background)
        assertEquals("SpO2 88% on 4L O2", insertedNote?.assessment)
        assertEquals("Escalate to BiPAP", insertedNote?.recommendation)
        assertEquals(1700000000000L, insertedNote?.timestamp)
    }

    @Test
    fun verifyRestoreClinicalTaskPreservesAllFieldsAndDndBypass() = runTest {
        var insertedTask: ClinicalTaskEntity? = null
        val fakeDao = object : ClinicalPlanningDao {
            override suspend fun insertIsbarNote(note: IsbarNoteEntity) {}
            override suspend fun updateIsbarNote(note: IsbarNoteEntity) {}
            override suspend fun deleteIsbarNote(note: IsbarNoteEntity) {}
            override fun getAllIsbarNotes(): Flow<List<IsbarNoteEntity>> = emptyFlow()
            override suspend fun deleteOldNotes(cutoffTime: Long) {}
            override suspend fun insertTask(task: ClinicalTaskEntity) {
                insertedTask = task
            }
            override suspend fun updateTask(task: ClinicalTaskEntity) {}
            override suspend fun deleteTask(task: ClinicalTaskEntity) {}
            override fun getAllTasks(): Flow<List<ClinicalTaskEntity>> = emptyFlow()
            override suspend fun setTaskCompleted(taskId: Int, completed: Boolean) {}
        }

        val originalTask = ClinicalTaskEntity(
            id = 777,
            taskName = "Administer IV Potassium Chloride",
            description = "Slow infusion via infusion pump over 4 hours",
            priority = "HIGH",
            triggerTime = 1711122233000L,
            isCompleted = false,
            bypassDnd = true
        )

        val useCase = RestoreClinicalTaskUseCase(fakeDao)
        useCase(originalTask)

        assertNotNull(insertedTask)
        assertEquals(777, insertedTask?.id)
        assertEquals("Administer IV Potassium Chloride", insertedTask?.taskName)
        assertEquals("Slow infusion via infusion pump over 4 hours", insertedTask?.description)
        assertEquals("HIGH", insertedTask?.priority)
        assertEquals(1711122233000L, insertedTask?.triggerTime)
        assertEquals(false, insertedTask?.isCompleted)
        assertTrue(insertedTask?.bypassDnd == true)
    }
}
