package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain boundary for clinical planning persistence.
 * UI/ViewModels depend on these use cases rather than Room directly.
 */
class ObserveIsbarNotesUseCase(private val dao: ClinicalPlanningDao) {
    operator fun invoke(): Flow<List<IsbarNoteEntity>> = dao.getAllIsbarNotes()
}

class ObserveClinicalTasksUseCase(private val dao: ClinicalPlanningDao) {
    operator fun invoke(): Flow<List<ClinicalTaskEntity>> = dao.getAllTasks()
}

class AddIsbarNoteUseCase(private val dao: ClinicalPlanningDao) {
    suspend operator fun invoke(
        patientId: String,
        identification: String,
        situation: String,
        background: String,
        assessment: String,
        recommendation: String
    ) {
        require(patientId.isNotBlank()) { "Patient identifier cannot be blank." }
        require(identification.isNotBlank()) { "Identification cannot be blank." }

        dao.insertIsbarNote(
            IsbarNoteEntity(
                patientIdentifier = patientId.trim(),
                identification = identification.trim(),
                situation = situation.trim(),
                background = background.trim(),
                assessment = assessment.trim(),
                recommendation = recommendation.trim(),
                timestamp = System.currentTimeMillis()
            )
        )
    }
}

class PurgeOldIsbarNotesUseCase(private val dao: ClinicalPlanningDao) {
    companion object {
        const val RETENTION_HOURS = 48L
        const val MILLIS_PER_HOUR = 60L * 60L * 1000L
    }

    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()) {
        val cutoff = nowMillis - (RETENTION_HOURS * MILLIS_PER_HOUR)
        dao.deleteOldNotes(cutoff)
    }
}

class AddClinicalTaskUseCase(private val dao: ClinicalPlanningDao) {
    suspend operator fun invoke(
        taskName: String,
        description: String,
        priority: String,
        triggerTime: Long,
        bypassDnd: Boolean
    ) {
        require(taskName.isNotBlank()) { "Task name cannot be blank." }
        require(priority.isNotBlank()) { "Task priority cannot be blank." }
        require(triggerTime >= 0L) { "Task trigger time cannot be negative." }

        dao.insertTask(
            ClinicalTaskEntity(
                taskName = taskName.trim(),
                description = description.trim(),
                priority = priority.trim(),
                triggerTime = triggerTime,
                isCompleted = false,
                bypassDnd = bypassDnd
            )
        )
    }
}

class SetClinicalTaskCompletedUseCase(private val dao: ClinicalPlanningDao) {
    suspend operator fun invoke(taskId: Int, completed: Boolean) {
        require(taskId > 0) { "Clinical task id must be positive." }
        dao.setTaskCompleted(taskId, completed)
    }
}
