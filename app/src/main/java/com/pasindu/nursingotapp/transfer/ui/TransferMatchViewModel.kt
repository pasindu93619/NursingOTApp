package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.Decision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferMatchViewModel @Inject constructor(
    private val transferRequestRepository: TransferRequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransferMatchUiState>(
        TransferMatchUiState.MatchFound(createSampleMatchUiModel())
    )
    val uiState: StateFlow<TransferMatchUiState> = _uiState.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun acceptMatch(matchId: String) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true

        val currentMatch = (_uiState.value as? TransferMatchUiState.MatchFound)?.match
            ?: createSampleMatchUiModel().copy(matchId = matchId)

        _uiState.value = TransferMatchUiState.Loading

        viewModelScope.launch {
            val result = transferRequestRepository.respondToMatch(matchId, Decision.ACCEPT)
            result.fold(
                onSuccess = { response ->
                    _isSubmitting.value = false
                    val updatedMatch = currentMatch.copy(
                        matchId = response.matchId,
                        myStatus = MatchDecisionStatus.ACCEPTED
                    )
                    _uiState.value = TransferMatchUiState.AlreadyAccepted(updatedMatch)
                },
                onFailure = { error ->
                    _isSubmitting.value = false
                    _uiState.value = TransferMatchUiState.Error(
                        error.message ?: "Failed to accept match. Please try again."
                    )
                }
            )
        }
    }

    fun rejectMatch(matchId: String, onComplete: () -> Unit = {}) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true

        val currentMatch = (_uiState.value as? TransferMatchUiState.MatchFound)?.match
            ?: createSampleMatchUiModel().copy(matchId = matchId)

        _uiState.value = TransferMatchUiState.Loading

        viewModelScope.launch {
            val result = transferRequestRepository.respondToMatch(matchId, Decision.REJECT)
            result.fold(
                onSuccess = { response ->
                    _isSubmitting.value = false
                    val updatedMatch = currentMatch.copy(
                        matchId = response.matchId,
                        myStatus = MatchDecisionStatus.REJECTED
                    )
                    _uiState.value = TransferMatchUiState.AlreadyRejected(updatedMatch)
                    onComplete()
                },
                onFailure = { error ->
                    _isSubmitting.value = false
                    _uiState.value = TransferMatchUiState.Error(
                        error.message ?: "Failed to reject match. Please try again."
                    )
                }
            )
        }
    }

    fun resetState() {
        _isSubmitting.value = false
        _uiState.value = TransferMatchUiState.MatchFound(createSampleMatchUiModel())
    }
}
