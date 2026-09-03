package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.KnowledgeHubDao
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity

/**
 * Domain boundary for Knowledge Hub / CPD persistence.
 * UI/ViewModels should depend on these use cases rather than Room directly.
 */
class ObserveCpdLogsUseCase(private val dao: KnowledgeHubDao) {
    operator fun invoke() = dao.getAllCpdLogs()
}

class AddCpdLogUseCase(private val dao: KnowledgeHubDao) {
    suspend operator fun invoke(
        title: String,
        earnedPoints: Int,
        institution: String,
        notes: String
    ) {
        require(title.isNotBlank()) { "CPD title cannot be blank." }
        require(earnedPoints >= 0) { "CPD points cannot be negative." }

        dao.insertCpdLog(
            CpdLogEntity(
                seminarTitle = title.trim(),
                date = System.currentTimeMillis(),
                earnedPoints = earnedPoints,
                speakerOrInstitution = institution.trim(),
                notes = notes.trim()
            )
        )
    }
}
