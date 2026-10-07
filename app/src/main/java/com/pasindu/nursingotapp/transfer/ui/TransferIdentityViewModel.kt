package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.TransferIdentityRepository
import com.pasindu.nursingotapp.transfer.data.model.TransferIdentityCredentialType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TransferIdentityUiState {
    data object Idle : TransferIdentityUiState
    data object Saving : TransferIdentityUiState
    data object Saved : TransferIdentityUiState
    data class Error(val message: String) : TransferIdentityUiState
}

@HiltViewModel
class TransferIdentityViewModel @Inject constructor(
    private val repository: TransferIdentityRepository
) : ViewModel() {

    private val _state = MutableStateFlow<TransferIdentityUiState>(TransferIdentityUiState.Idle)
    val state: StateFlow<TransferIdentityUiState> = _state.asStateFlow()

    fun save(
        nic: String,
        credentialType: TransferIdentityCredentialType,
        credentialNumber: String
    ) {
        if (nic.trim().isEmpty() || credentialNumber.trim().isEmpty()) {
            _state.value = TransferIdentityUiState.Error("Enter all required details")
            return
        }

        _state.value = TransferIdentityUiState.Saving
        viewModelScope.launch {
            try {
                repository.saveSelfDeclaredIdentity(
                    nic = nic,
                    credentialType = credentialType,
                    credentialNumber = credentialNumber
                )
                _state.value = TransferIdentityUiState.Saved
            } catch (error: Throwable) {
                _state.value = TransferIdentityUiState.Error(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Could not save Transfer identity"
                )
            }
        }
    }

    fun resetState() {
        _state.value = TransferIdentityUiState.Idle
    }
}
