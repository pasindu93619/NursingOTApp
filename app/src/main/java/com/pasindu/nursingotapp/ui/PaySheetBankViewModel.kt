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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Transient operation state for Pay Sheet Bank actions.
 * The document stream remains the source of truth for persisted data.
 */
data class PaySheetBankUiState(
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

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

    private val _uiState = MutableStateFlow(PaySheetBankUiState())
    val uiState: StateFlow<PaySheetBankUiState> = _uiState.asStateFlow()

    suspend fun findByMonth(monthKey: String): PaySheetDocumentEntity? =
        findPaySheetDocumentUseCase(monthKey)

    fun save(document: PaySheetDocumentEntity, successMessage: String = "Paysheet saved securely in your private vault.") {
        _uiState.value = _uiState.value.copy(
            isSaving = true,
            errorMessage = null,
            successMessage = null
        )
        viewModelScope.launch {
            runCatching { savePaySheetDocumentUseCase(document) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = successMessage
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Unable to save paysheet."
                    )
                }
        }
    }

    fun delete(document: PaySheetDocumentEntity) {
        _uiState.value = _uiState.value.copy(
            isDeleting = true,
            errorMessage = null,
            successMessage = null
        )
        viewModelScope.launch {
            runCatching { deletePaySheetDocumentUseCase(document) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        successMessage = "Paysheet removed from your private vault."
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Unable to delete paysheet."
                    )
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
