package com.pasindu.nursingotapp.transfer.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import com.pasindu.nursingotapp.transfer.data.model.TransferWorkerApiException
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

private inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

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

    private var matchPollingJob: kotlinx.coroutines.Job? = null

    val isPollingActive: Boolean
        get() = matchPollingJob?.isActive == true

    init {
        loadCurrentMatch()
    }

    fun startMatchPolling() {
        if (matchPollingJob?.isActive == true) return
        matchPollingJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(3_000L)
                val currentState = _uiState.value
                val isWaitingState = currentState is TransferMatchUiState.AlreadyAccepted ||
                    currentState is TransferMatchUiState.MatchFound
                if (!isWaitingState) {
                    break
                }
                val currentMatchId = when (currentState) {
                    is TransferMatchUiState.AlreadyAccepted -> currentState.match.matchId
                    is TransferMatchUiState.MatchFound -> currentState.match.matchId
                    else -> null
                }
                try {
                    val syncResult = transferRequestRepository.syncActiveRequest()
                    if (!isActive) return@launch
                    val userId = transferRequestRepository.getCurrentUserId()
                    val hospitals = hospitalReferenceRepository.getAll()
                    if (!isActive) return@launch
                    when (syncResult) {
                        is WorkerSyncResult.MatchFound -> {
                            if (!userId.isNullOrBlank()) {
                                try {
                                    val uiModel = syncResult.toUiModel(userId, hospitals)
                                    // If matchId changed, this is a brand new match proposal
                                    if (currentMatchId != null && uiModel.matchId != currentMatchId) {
                                        if (uiModel.myStatus == MatchDecisionStatus.ACCEPTED) {
                                            _uiState.value = TransferMatchUiState.AlreadyAccepted(uiModel)
                                        } else {
                                            _uiState.value = TransferMatchUiState.MatchFound(uiModel)
                                        }
                                    } else {
                                        when (uiModel.serverStatus) {
                                            "CHAT_OPEN" -> {
                                                stopMatchPolling()
                                                _uiState.value = TransferMatchUiState.ChatOpen(uiModel)
                                            }
                                            "CONFIRMED" -> {
                                                stopMatchPolling()
                                                _uiState.value = TransferMatchUiState.Confirmed(uiModel)
                                            }
                                            "CANCELLED" -> {
                                                stopMatchPolling()
                                                _uiState.value = TransferMatchUiState.AlreadyRejected(uiModel)
                                            }
                                            "EXPIRED" -> {
                                                stopMatchPolling()
                                                _uiState.value = TransferMatchUiState.Expired(uiModel)
                                            }
                                            else -> {
                                                if (uiModel.myStatus == MatchDecisionStatus.ACCEPTED) {
                                                    _uiState.value = TransferMatchUiState.AlreadyAccepted(uiModel)
                                                } else {
                                                    _uiState.value = TransferMatchUiState.MatchFound(uiModel)
                                                }
                                            }
                                        }
                                    }
                                } catch (e: IllegalArgumentException) {
                                    stopMatchPolling()
                                    _uiState.value = TransferMatchUiState.Error("Signed-in nurse is not a participant in this match.")
                                }
                            }
                        }
                        is WorkerSyncResult.ReturnedToPool -> {
                            stopMatchPolling()
                            _uiState.value = TransferMatchUiState.Error(
                                "This match has ended. Your request has been returned to the matching pool."
                            )
                        }
                        is WorkerSyncResult.RequestCompleted -> {
                            stopMatchPolling()
                            _uiState.value = TransferMatchUiState.Error(
                                "This mutual transfer has already been completed."
                            )
                        }
                        is WorkerSyncResult.RequestWithdrawn -> {
                            stopMatchPolling()
                            _uiState.value = TransferMatchUiState.Error(
                                "Your mutual transfer request has been withdrawn."
                            )
                        }
                        is WorkerSyncResult.NoMatch -> {
                            // Match ended (e.g. partner declined or match expired) and local row is back in pool
                            stopMatchPolling()
                            _uiState.value = TransferMatchUiState.Error(
                                "This match has ended. Your request has been returned to the matching pool."
                            )
                        }
                        else -> {
                            // On network error or conflict during polling, do not wipe state
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    // Ignore transient exceptions during polling
                }
            }
        }
    }

    private fun stopMatchPolling() {
        matchPollingJob?.cancel()
        matchPollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMatchPolling()
    }

    fun onScreenExit() {
        stopMatchPolling()
    }

    fun onScreenResume() {
        val currentState = _uiState.value
        if (currentState is TransferMatchUiState.AlreadyAccepted ||
            currentState is TransferMatchUiState.MatchFound
        ) {
            startMatchPolling()
        }
    }

    private fun loadCurrentMatch() {
        viewModelScope.launch {
            val result = runSuspendCatching {
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
                                stopMatchPolling()
                                _uiState.value = TransferMatchUiState.Error(
                                    "Unable to identify the signed-in nurse."
                                )
                            } else {
                                try {
                                    val uiModel = syncResult.toUiModel(userId, hospitals)
                                    _uiState.value = when (uiModel.serverStatus) {
                                        "CHAT_OPEN" -> {
                                            stopMatchPolling()
                                            TransferMatchUiState.ChatOpen(uiModel)
                                        }
                                        "CONFIRMED" -> {
                                            stopMatchPolling()
                                            TransferMatchUiState.Confirmed(uiModel)
                                        }
                                        "CANCELLED" -> {
                                            stopMatchPolling()
                                            TransferMatchUiState.AlreadyRejected(uiModel)
                                        }
                                        "EXPIRED" -> {
                                            stopMatchPolling()
                                            TransferMatchUiState.Expired(uiModel)
                                        }
                                        else -> {
                                            startMatchPolling()
                                            if (uiModel.myStatus == MatchDecisionStatus.ACCEPTED) {
                                                TransferMatchUiState.AlreadyAccepted(uiModel)
                                            } else {
                                                TransferMatchUiState.MatchFound(uiModel)
                                            }
                                        }
                                    }
                                } catch (e: IllegalArgumentException) {
                                    stopMatchPolling()
                                    _uiState.value = TransferMatchUiState.Error("Signed-in nurse is not a participant in this match.")
                                }
                            }
                        }
                        is WorkerSyncResult.RequestCompleted -> {
                            stopMatchPolling()
                            _uiState.value = TransferMatchUiState.Error(
                                "This mutual transfer has already been completed."
                            )
                        }
                        is WorkerSyncResult.RequestWithdrawn -> {
                            stopMatchPolling()
                            _uiState.value = TransferMatchUiState.Error(
                                "Your mutual transfer request has been withdrawn."
                            )
                        }
                        is WorkerSyncResult.ReturnedToPool -> {
                            stopMatchPolling()
                            _uiState.value = TransferMatchUiState.Error(
                                "This match has ended. Your request has been returned to the matching pool."
                            )
                        }
                        is WorkerSyncResult.NoMatch -> {
                            stopMatchPolling()
                            _uiState.value = TransferMatchUiState.Error(
                                "No active mutual-transfer match is available."
                            )
                        }
                        is WorkerSyncResult.NetworkError -> {
                            // Do not cancel polling if already active on transient network failure
                            if (_uiState.value !is TransferMatchUiState.AlreadyAccepted &&
                                _uiState.value !is TransferMatchUiState.MatchFound
                            ) {
                                _uiState.value = TransferMatchUiState.Error(syncResult.message)
                            }
                        }
                        is WorkerSyncResult.AuthError -> {
                            stopMatchPolling()
                            _uiState.value = TransferMatchUiState.Error(syncResult.message)
                        }
                        is WorkerSyncResult.Conflict -> {
                            stopMatchPolling()
                            _uiState.value = TransferMatchUiState.Error(syncResult.message)
                        }
                    }
                },
                onFailure = { error ->
                    stopMatchPolling()
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
            require(currentUserId == tw.nurseAUid || currentUserId == tw.nurseBUid || currentUserId == tw.nurseCUid) {
                "User $currentUserId is not a participant in 3-way match $matchId"
            }
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

            fun decisionStatus(accepted: Boolean, rejected: Boolean): MatchDecisionStatus = when {
                rejected -> MatchDecisionStatus.REJECTED
                accepted -> MatchDecisionStatus.ACCEPTED
                else -> MatchDecisionStatus.PENDING
            }

            val statusA = decisionStatus(acceptedByA, rejectedByA)
            val statusB = decisionStatus(acceptedByB, rejectedByB)
            val statusC = decisionStatus(acceptedByC, rejectedByC)

            val (myStatus, p2Status, p3Status) = when (currentUserId) {
                tw.nurseAUid -> Triple(statusA, statusB, statusC)
                tw.nurseBUid -> Triple(statusB, statusC, statusA)
                else -> Triple(statusC, statusA, statusB)
            }

            val p2 = TransferParticipantUiModel(
                roleLabel = "NURSE 2",
                roleTitle = "Your Destination Post",
                hospitalId = n2Hosp,
                hospitalName = hospitalName(n2Hosp),
                hospitalLocation = hospitalLocation(n2Hosp),
                grade = n2Grade,
                isCurrentUser = false,
                status = p2Status
            )

            val p3 = TransferParticipantUiModel(
                roleLabel = "NURSE 3",
                roleTitle = "Connecting Post",
                hospitalId = n3Hosp,
                hospitalName = hospitalName(n3Hosp),
                hospitalLocation = hospitalLocation(n3Hosp),
                grade = n3Grade,
                isCurrentUser = false,
                status = p3Status
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
                myStatus = myStatus,
                partnerStatus = p2Status,
                myConfirmed = myConfirmed,
                serverStatus = status,
                participant2 = p2,
                participant3 = p3
            )
        }

        val direct = match
        require(currentUserId == direct.nurseAUid || currentUserId == direct.nurseBUid) {
            "User $currentUserId is not a participant in 2-way match $matchId"
        }
        val isOfficerA = currentUserId == direct.nurseAUid

        val myHospitalId = if (isOfficerA) direct.nurseACurrentHospitalId else direct.nurseBCurrentHospitalId
        val partnerHospitalId = if (isOfficerA) direct.nurseBCurrentHospitalId else direct.nurseACurrentHospitalId
        val myGrade = if (isOfficerA) direct.nurseAGrade else direct.nurseBGrade
        val partnerGrade = if (isOfficerA) direct.nurseBGrade else direct.nurseAGrade
        val myConfirmed = if (isOfficerA) confirmedByA else confirmedByB
        val partnerConfirmed = if (isOfficerA) confirmedByB else confirmedByA

        val myAccepted = if (isOfficerA) acceptedByA else acceptedByB
        val myRejected = if (isOfficerA) rejectedByA else rejectedByB
        val partnerAccepted = if (isOfficerA) acceptedByB else acceptedByA
        val partnerRejected = if (isOfficerA) rejectedByB else rejectedByA

        val myStatus = when {
            myRejected -> MatchDecisionStatus.REJECTED
            myAccepted -> MatchDecisionStatus.ACCEPTED
            else -> MatchDecisionStatus.PENDING
        }
        val partnerStatus = when {
            partnerRejected -> MatchDecisionStatus.REJECTED
            partnerAccepted -> MatchDecisionStatus.ACCEPTED
            else -> MatchDecisionStatus.PENDING
        }

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
            myStatus = myStatus,
            partnerStatus = partnerStatus,
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
                        "CHAT_OPEN" -> {
                            stopMatchPolling()
                            TransferMatchUiState.ChatOpen(updatedMatch)
                        }
                        else -> {
                            startMatchPolling()
                            TransferMatchUiState.AlreadyAccepted(updatedMatch)
                        }
                    }
                },
                onFailure = { error ->
                    _isSubmitting.value = false
                    // The server may have advanced to CHAT_OPEN after another nurse
                    // accepted while this screen was stale. Refresh authoritative state
                    // so the navigation observer can open chat; never retry ACCEPT as CONFIRM.
                    val isInvalidDecision = (error is TransferWorkerApiException && error.errorCode == "INVALID_DECISION") ||
                        (error.cause is TransferWorkerApiException && (error.cause as TransferWorkerApiException).errorCode == "INVALID_DECISION") ||
                        error.message.orEmpty().contains("INVALID_DECISION")
                    if (isInvalidDecision) {
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
