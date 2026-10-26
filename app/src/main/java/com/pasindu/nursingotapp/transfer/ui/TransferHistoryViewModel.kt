package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.TransferMatchHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferHistoryUiState(
    val isLoading: Boolean = true,
    val items: List<TransferMatchHistoryItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TransferHistoryViewModel @Inject constructor(
    private val repository: TransferRequestRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransferHistoryUiState())
    val uiState: StateFlow<TransferHistoryUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getMatchHistory().fold(
                onSuccess = { items ->
                    _uiState.value = TransferHistoryUiState(isLoading = false, items = items)
                },
                onFailure = { error ->
                    _uiState.value = TransferHistoryUiState(
                        isLoading = false,
                        error = error.message ?: "Could not load match history."
                    )
                }
            )
        }
    }
}
