package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.HospitalReferenceRepository
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
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
                        is WorkerSyncResult.NetworkError -> {
                            _uiState.value = TransferMatchUiState.Error(syncResult.message)
                        }
                        is WorkerSyncResult.AuthError -> {
                            _uiState.value = TransferMatchUiState.Error(syncResult.message)
                        }
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
        val expiryMs = runCatching { Instant.parse(expiresAt).toEpochMilli() }.getOrNull()

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

        if (matchType == "THREE_WAY" && threeWayMatch != null) {
            val tw = threeWayMatch
            // A -> B -> C -> A
            // Determine user orientation:
            val (youCurrentHosp, youGrade) = when (currentUserId) {
                tw.nurseAUid -> tw.nurseACurrentHospitalId to tw.nurseAGrade
                tw.nurseBUid -> tw.nurseBCurrentHospitalId to tw.nurseBGrade
                else -> tw.nurseCCurrentHospitalId to tw.nurseCGrade
            }

            // Nurse 2 is the nurse at your desired destination:
            // If You are A: Nurse 2 is B (destination B's hosp), Nurse 3 is C (destination C's hosp)
            // If You are B: Nurse 2 is C (destination C's hosp), Nurse 3 is A (destination A's hosp)
            // If You are C: Nurse 2 is A (destination A's hosp), Nurse 3 is B (destination B's hosp)
            val (n2Id, n2Hosp, n2Grade) = when (currentUserId) {
                tw.nurseAUid -> Triple(tw.nurseBUid, tw.nurseBCurrentHospitalId, tw.nurseBGrade)
                tw.nurseBUid -> Triple(tw.nurseCUid, tw.nurseCCurrentHospitalId, tw.nurseCGrade)
                else -> Triple(tw.nurseAUid, tw.nurseACurrentHospitalId, tw.nurseAGrade)
            }

            val (n3Id, n3Hosp, n3Grade) = when (currentUserId) {
                tw.nurseAUid -> Triple(tw.nurseCUid, tw.nurseCCurrentHospitalId, tw.nurseCGrade)
                tw.nurseBUid -> Triple(tw.nurseAUid, tw.nurseACurrentHospitalId, tw.nurseAGrade)
                else -> Triple(tw.nurseBUid, tw.nurseBCurrentHospitalId, tw.nurseBGrade)
            }

            val p2 = TransferParticipantUiModel(
                roleLabel = "NURSE 2",
                roleTitle = "Your Destination Post",
                hospitalId = n2Hosp,
                hospitalName = hospitalName(n2Hosp),
                hospitalLocation = hospitalLocation(n2Hosp),
                grade = n2Grade,
                isCurrentUser = false,
                status = MatchDecisionStatus.PENDING
            )

            val p3 = TransferParticipantUiModel(
                roleLabel = "NURSE 3",
                roleTitle = "Connecting Post",
                hospitalId = n3Hosp,
                hospitalName = hospitalName(n3Hosp),
                hospitalLocation = hospitalLocation(n3Hosp),
                grade = n3Grade,
                isCurrentUser = false,
                status = MatchDecisionStatus.PENDING
            )

            return TransferMatchUiModel(
                matchId = matchId,
                myHospitalId = youCurrentHosp,
                myHospitalName = hospitalName(youCurrentHosp),
                myHospitalLocation = hospitalLocation(youCurrentHosp),
                myGrade = youGrade,
                partnerHospitalId = n2Hosp,
                partnerHospitalName = hospitalName(n2Hosp),
                partnerHospitalLocation = hospitalLocation(n2Hosp),
                partnerGrade = n2Grade,
                isSameGrade = tw.isAllSameGrade,
                matchType = "THREE_WAY",
                compatibilityReason = tw.priorityReason,
                expiresAtMs = expiryMs,
                myStatus = MatchDecisionStatus.PENDING,
                partnerStatus = MatchDecisionStatus.PENDING,
                participant2 = p2,
                participant3 = p3
            )
        }

        val direct = match
        val isOfficerA = currentUserId == direct.nurseAUid

        val myHospitalId = if (isOfficerA) direct.nurseACurrentHospitalId else direct.nurseBCurrentHospitalId
        val partnerHospitalId = if (isOfficerA) direct.nurseBCurrentHospitalId else direct.nurseACurrentHospitalId
        val myGrade = if (isOfficerA) direct.nurseAGrade else direct.nurseBGrade
        val partnerGrade = if (isOfficerA) direct.nurseBGrade else direct.nurseAGrade

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
            isSameGrade = direct.isSameGrade,
            matchType = "DIRECT_2_WAY",
            compatibilityReason = direct.priorityReason,
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
