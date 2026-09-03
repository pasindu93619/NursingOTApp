package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.PaySheetDocumentDao
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import kotlinx.coroutines.flow.Flow

class ObservePaySheetDocumentsUseCase(
    private val dao: PaySheetDocumentDao
) {
    operator fun invoke(): Flow<List<PaySheetDocumentEntity>> = dao.observeAll()
}
