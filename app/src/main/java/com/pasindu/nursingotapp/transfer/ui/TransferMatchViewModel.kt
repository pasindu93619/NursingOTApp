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
                                val uiModel = syncResult.toUiModel(userId, hospitals)
                                _uiState.value = when (uiModel.serverStatus) {
                                    "CHAT_OPEN" -> TransferMatchUiState.ChatOpen(uiModel)
                                    "CONFIRMED" -> TransferMatchUiState.Confirmed(uiModel)
                                    "CANCELLED" -> TransferMatchUiState.AlreadyRejected(uiModel)
                                    "EXPIRED" -> TransferMatchUiState.Expired(uiModel)
                                    else -> {
                                        if (uiModel.myStatus == MatchDecisionStatus.ACCEPTED) {
                                            TransferMatchUiState.AlreadyAccepted(uiModel)
                                        } else {
                                            TransferMatchUiState.MatchFound(uiModel)
                                        }
                                    }
                                }
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
        val firstResponseMs = firstResponseAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        val chatDeadlineMs = chatDeadline?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

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
            val (youCurrentHosp, youGrade) = when (currentUserId) {
                tw.nurseAUid -> tw.nurseACurrentHospitalId to tw.nurseAGrade
                tw.nurseBUid -> tw.nurseBCurrentHospitalId to tw.nurseBGrade
                else -> tw.nurseCCurrentHospitalId to tw.nurseCGrade
            }

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

            val myConfirmed = when (currentUserId) {
                tw.nurseAUid -> confirmedByA
                tw.nurseBUid -> confirmedByB
                else -> confirmedByC
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
                firstResponseAtMs = firstResponseMs,
                chatDeadlineMs = chatDeadlineMs,
                myStatus = MatchDecisionStatus.PENDING,
                partnerStatus = MatchDecisionStatus.PENDING,
                myConfirmed = myConfirmed,
                serverStatus = status,
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
        val myConfirmed = if (isOfficerA) confirmedByA else confirmedByB
        val partnerConfirmed = if (isOfficerA) confirmedByB else confirmedByA

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
            firstResponseAtMs = firstResponseMs,
            chatDeadlineMs = chatDeadlineMs,
            myStatus = MatchDecisionStatus.PENDING,
            partnerStatus = MatchDecisionStatus.PENDING,
            myConfirmed = myConfirmed,
            partnerConfirmed = partnerConfirmed,
            serverStatus = status
        )
    }

    private fun getCurrentMatchModel(): TransferMatchUiModel? {
        return when (val state = _uiState.value) {
            is TransferMatchUiState.MatchFound -> state.match
            is TransferMatchUiState.AlreadyAccepted -> state.match
            is TransferMatchUiState.ChatOpen -> state.match
            is TransferMatchUiState.Confirmed -> state.match
            is TransferMatchUiState.AlreadyRejected -> state.match
            is TransferMatchUiState.Expired -> state.match
            else -> null
        }
    }

    fun acceptMatch(matchId: String) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true

        val currentMatch = getCurrentMatchModel()
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
                    val newExpiryMs = runCatching { Instant.parse(response.expiresAt).toEpochMilli() }.getOrNull()
                    val newChatDeadlineMs = response.chatDeadline?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                    val newFirstResponseMs = response.firstResponseAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

                    val updatedMatch = currentMatch.copy(
                        matchId = response.matchId,
                        serverStatus = response.newStatus,
                        expiresAtMs = newExpiryMs ?: currentMatch.expiresAtMs,
                        chatDeadlineMs = newChatDeadlineMs ?: currentMatch.chatDeadlineMs,
                        firstResponseAtMs = newFirstResponseMs ?: currentMatch.firstResponseAtMs,
                        myStatus = MatchDecisionStatus.ACCEPTED
                    )

                    _uiState.value = when (response.newStatus) {
                        "CHAT_OPEN" -> TransferMatchUiState.ChatOpen(updatedMatch)
                        else -> TransferMatchUiState.AlreadyAccepted(updatedMatch)
                    }
                },
                onFailure = { error ->
                    _isSubmitting.value = false
                    // The server may have advanced to CHAT_OPEN after another nurse
                    // accepted while this screen was stale. Refresh authoritative state
                    // so the navigation observer can open chat; never retry ACCEPT as CONFIRM.
                    if (error.message.orEmpty().contains("INVALID_DECISION")) {
                        loadCurrentMatch()
                    } else {
                        _uiState.value = TransferMatchUiState.Error(
                            error.message ?: "Failed to accept match. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun confirmMatch(matchId: String) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true

        val currentMatch = getCurrentMatchModel()
            ?: run {
                _isSubmitting.value = false
                _uiState.value = TransferMatchUiState.Error(
                    "Current match data is not loaded. Please retry."
                )
                return
            }

        _uiState.value = TransferMatchUiState.Loading

        viewModelScope.launch {
            val result = transferRequestRepository.respondToMatch(matchId, Decision.CONFIRM)
            result.fold(
                onSuccess = { response ->
                    _isSubmitting.value = false
                    val updatedMatch = currentMatch.copy(
                        matchId = response.matchId,
                        serverStatus = response.newStatus,
                        myConfirmed = true
                    )
                    _uiState.value = when (response.newStatus) {
                        "CONFIRMED" -> TransferMatchUiState.Confirmed(updatedMatch)
                        else -> TransferMatchUiState.ChatOpen(updatedMatch)
                    }
                },
                onFailure = { error ->
                    _isSubmitting.value = false
                    _uiState.value = TransferMatchUiState.Error(
                        error.message ?: "Failed to confirm match. Please try again."
                    )
                }
            )
        }
    }

    fun rejectMatch(matchId: String, onComplete: () -> Unit = {}) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true

        val currentMatch = getCurrentMatchModel()
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
                        serverStatus = "CANCELLED",
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
