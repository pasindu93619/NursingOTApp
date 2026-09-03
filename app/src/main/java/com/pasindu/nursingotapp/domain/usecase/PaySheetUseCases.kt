package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.PaySheetDocumentDao
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import kotlinx.coroutines.flow.Flow

class ObservePaySheetDocumentsUseCase(
    private val dao: PaySheetDocumentDao
) {
    operator fun invoke(): Flow<List<PaySheetDocumentEntity>> = dao.observeAll()
}

class FindPaySheetDocumentUseCase(
    private val dao: PaySheetDocumentDao
) {
    suspend operator fun invoke(monthKey: String): PaySheetDocumentEntity? = dao.findByMonth(monthKey)
}

class SavePaySheetDocumentUseCase(
    private val dao: PaySheetDocumentDao
) {
    suspend operator fun invoke(document: PaySheetDocumentEntity): Long = dao.upsert(document)
}

class DeletePaySheetDocumentUseCase(
    private val dao: PaySheetDocumentDao
) {
    suspend operator fun invoke(document: PaySheetDocumentEntity) = dao.delete(document)
}
