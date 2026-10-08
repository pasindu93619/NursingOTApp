package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.HospitalReferenceRepository
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.WorkerDirectMatch
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import java.time.Instant
import com.pasindu.nursingotapp.transfer.data.model.Decision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferMatchViewModel @Inject constructor(
    private val transferRequestRepository: TransferRequestRepository,
    private val hospitalReferenceRepository: HospitalReferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransferMatchUiState>(
        TransferMatchUiState.Loading
    )
    val uiState: StateFlow<TransferMatchUiState> = _uiState.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    init {
        loadCurrentMatch()
    }

    private fun loadCurrentMatch() {
        viewModelScope.launch {
            val result = runCatching {
                val syncResult = transferRequestRepository.syncActiveRequest()
                val userId = transferRequestRepository.getCurrentUserId()
                val hospitals = hospitalReferenceRepository.getAll()
                syncResult to userId to hospitals
            }

            result.fold(
                onSuccess = { (payload, hospitals) ->
                    val (syncResult, userId) = payload
                    when (syncResult) {
                        is WorkerSyncResult.MatchFound -> {
                            if (userId.isNullOrBlank()) {
                                _uiState.value = TransferMatchUiState.Error(
                                    "Unable to identify the signed-in nurse."
                                )
                            } else {
                                _uiState.value = TransferMatchUiState.MatchFound(
                                    syncResult.toUiModel(userId, hospitals)
                                )
                            }
                        }
                        is WorkerSyncResult.NoMatch -> {
                            _uiState.value = TransferMatchUiState.Error(
                                "No active mutual-transfer match is available."
                            )
                        }
                        is WorkerSyncResult.NetworkError,
                        is WorkerSyncResult.AuthError,
                        is WorkerSyncResult.Conflict -> {
                            _uiState.value = TransferMatchUiState.Error(syncResult.message)
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = TransferMatchUiState.Error(
                        error.message ?: "Unable to load the current mutual-transfer match."
                    )
                }
            )
        }
    }

    private fun WorkerSyncResult.MatchFound.toUiModel(
        currentUserId: String,
        hospitals: List<com.pasindu.nursingotapp.transfer.data.model.HospitalReference>
    ): TransferMatchUiModel {
        val match = match
        val isOfficerA = currentUserId == match.nurseAUid

        val myHospitalId = if (isOfficerA) match.nurseACurrentHospitalId else match.nurseBCurrentHospitalId
        val partnerHospitalId = if (isOfficerA) match.nurseBCurrentHospitalId else match.nurseACurrentHospitalId
        val myGrade = if (isOfficerA) match.nurseAGrade else match.nurseBGrade
        val partnerGrade = if (isOfficerA) match.nurseBGrade else match.nurseAGrade

        fun hospitalName(id: String): String =
            hospitals.firstOrNull { it.hospitalId == id }?.name?.takeIf { it.isNotBlank() }
                ?: id

        fun hospitalLocation(id: String): String {
            val hospital = hospitals.firstOrNull { it.hospitalId == id }
            if (hospital == null) return "Hospital reference unavailable"
            return listOfNotNull(
                hospital.district?.takeIf { it.isNotBlank() }?.let { "$it District" },
                hospital.province.takeIf { it.isNotBlank() }?.let { "$it Province" }
            ).joinToString(" • ").ifBlank { "Sri Lanka" }
        }

        val expiryMs = runCatching { Instant.parse(expiresAt).toEpochMilli() }.getOrNull()

        return TransferMatchUiModel(
            matchId = matchId,
            myHospitalId = myHospitalId,
            myHospitalName = hospitalName(myHospitalId),
            myHospitalLocation = hospitalLocation(myHospitalId),
            myGrade = myGrade,
            partnerHospitalId = partnerHospitalId,
            partnerHospitalName = hospitalName(partnerHospitalId),
            partnerHospitalLocation = hospitalLocation(partnerHospitalId),
            partnerGrade = partnerGrade,
            isSameGrade = match.isSameGrade,
            matchType = "DIRECT_2_WAY",
            compatibilityReason = match.priorityReason,
            expiresAtMs = expiryMs,
            myStatus = MatchDecisionStatus.PENDING,
            partnerStatus = MatchDecisionStatus.PENDING
        )
    }

    fun acceptMatch(matchId: String) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true

        val currentMatch = (_uiState.value as? TransferMatchUiState.MatchFound)?.match
            ?: run {
                _isSubmitting.value = false
                _uiState.value = TransferMatchUiState.Error(
                    "Current match data is not loaded. Please retry."
                )
                return
            }

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
            ?: run {
                _isSubmitting.value = false
                _uiState.value = TransferMatchUiState.Error(
                    "Current match data is not loaded. Please retry."
                )
                return
            }

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
        _uiState.value = TransferMatchUiState.Loading
        loadCurrentMatch()
    }
}
