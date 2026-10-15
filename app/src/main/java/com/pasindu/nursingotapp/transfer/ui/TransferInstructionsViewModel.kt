package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.HospitalReferenceRepository
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferInstructionsUiState(
    val isLoading: Boolean = true,
    val matchId: String = "",
    val matchType: String = "",
    val participants: List<TransferChatParticipant> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TransferInstructionsViewModel @Inject constructor(
    private val transferRequestRepository: TransferRequestRepository,
    private val hospitalReferenceRepository: HospitalReferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferInstructionsUiState())
    val uiState: StateFlow<TransferInstructionsUiState> = _uiState.asStateFlow()

    fun load(matchId: String) {
        if (matchId.isBlank()) {
            _uiState.value = TransferInstructionsUiState(isLoading = false, error = "Missing match reference.")
            return
        }
        if (_uiState.value.matchId == matchId && !_uiState.value.isLoading && _uiState.value.participants.isNotEmpty()) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val syncResult = transferRequestRepository.syncActiveRequest()
                val currentUid = transferRequestRepository.getCurrentUserId().orEmpty()
                val hospitals = hospitalReferenceRepository.getAll()
                Triple(syncResult, currentUid, hospitals)
            }.fold(
                onSuccess = { (syncResult, currentUid, hospitals) ->
                    if (syncResult is WorkerSyncResult.MatchFound && syncResult.matchId == matchId) {
                        val participants = TransferParticipantBuilder.buildParticipants(syncResult, currentUid, hospitals)
                        _uiState.value = TransferInstructionsUiState(
                            isLoading = false,
                            matchId = matchId,
                            matchType = syncResult.matchType,
                            participants = participants,
                            error = null
                        )
                    } else {
                        _uiState.value = TransferInstructionsUiState(
                            isLoading = false,
                            matchId = matchId,
                            matchType = "DIRECT_2_WAY",
                            error = (syncResult as? WorkerSyncResult.NoMatch)?.message ?: "Unable to load match details."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.value = TransferInstructionsUiState(
                        isLoading = false,
                        matchId = matchId,
                        matchType = "DIRECT_2_WAY",
                        error = err.message ?: "Failed to load transfer instructions."
                    )
                }
            )
        }
    }
}
