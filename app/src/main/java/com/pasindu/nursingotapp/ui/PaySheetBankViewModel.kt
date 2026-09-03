package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import com.pasindu.nursingotapp.domain.usecase.DeletePaySheetDocumentUseCase
import com.pasindu.nursingotapp.domain.usecase.FindPaySheetDocumentUseCase
import com.pasindu.nursingotapp.domain.usecase.ObservePaySheetDocumentsUseCase
import com.pasindu.nursingotapp.domain.usecase.SavePaySheetDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PaySheetBankViewModel @Inject constructor(
    observePaySheetDocumentsUseCase: ObservePaySheetDocumentsUseCase,
    private val findPaySheetDocumentUseCase: FindPaySheetDocumentUseCase,
    private val savePaySheetDocumentUseCase: SavePaySheetDocumentUseCase,
    private val deletePaySheetDocumentUseCase: DeletePaySheetDocumentUseCase
) : ViewModel() {

    val documents: StateFlow<List<PaySheetDocumentEntity>> = observePaySheetDocumentsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    suspend fun findByMonth(monthKey: String): PaySheetDocumentEntity? =
        findPaySheetDocumentUseCase(monthKey)

    fun save(document: PaySheetDocumentEntity) {
        viewModelScope.launch { savePaySheetDocumentUseCase(document) }
    }

    fun delete(document: PaySheetDocumentEntity) {
        viewModelScope.launch { deletePaySheetDocumentUseCase(document) }
    }
}
