package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.HospitalReferenceRepository
import com.pasindu.nursingotapp.transfer.data.TransferChatRepository
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.Decision
import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import com.pasindu.nursingotapp.transfer.data.model.TransferChatMessage
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Domain representation of a participant in the active transfer team.
 */
data class TransferChatParticipant(
    val uid: String,
    val roleLetter: String, // "A", "B", "C"
    val displayName: String, // "You (Nurse A)", "Partner (Nurse B)", etc.
    val hospitalId: String,
    val hospitalName: String,
    val hospitalLocation: String,
    val grade: String,
    val isCurrentUser: Boolean,
    val confirmed: Boolean
)

/**
 * UI State for the Transfer Chat screen.
 */
data class TransferChatUiState(
    val isLoading: Boolean = true,
    val matchId: String = "",
    val matchType: String = "DIRECT_2_WAY", // "DIRECT_2_WAY" or "THREE_WAY"
    val serverStatus: String = "LOADING", // "LOADING", "CHAT_OPEN", "CONFIRMED", "CANCELLED", "EXPIRED"
    val chatDeadlineMs: Long? = null,
    val participants: List<TransferChatParticipant> = emptyList(),
    val currentUserUid: String = "",
    val currentUserRole: String = "", // "A", "B", "C"
    val currentUserHospitalId: String = "",
    val currentUserGrade: String = "",
    val isUserConfirmed: Boolean = false,
    val isAllConfirmed: Boolean = false,
    val messages: List<TransferChatMessage> = emptyList(),
    val isSendingMessage: Boolean = false,
    val isSubmittingAction: Boolean = false,
    val error: String? = null,
    val isTerminal: Boolean = false
) {
    val canSend: Boolean
        get() = !isLoading && serverStatus == "CHAT_OPEN" && !isSendingMessage && !isTerminal

    val canConfirm: Boolean
        get() = !isLoading && serverStatus == "CHAT_OPEN" && !isUserConfirmed && !isSubmittingAction && !isTerminal

    val canLeave: Boolean
        get() = !isLoading && serverStatus == "CHAT_OPEN" && !isSubmittingAction && !isTerminal
}

@HiltViewModel
class TransferChatViewModel @Inject constructor(
    private val transferChatRepository: TransferChatRepository,
    private val transferRequestRepository: TransferRequestRepository,
    private val hospitalReferenceRepository: HospitalReferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferChatUiState())
    val uiState: StateFlow<TransferChatUiState> = _uiState.asStateFlow()

    private var messageCollectionJob: Job? = null

    init {
        loadMatchAndObserveChat()
    }

    fun loadMatchAndObserveChat() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = runCatching {
                val syncResult = transferRequestRepository.syncActiveRequest()
                val currentUid = transferRequestRepository.getCurrentUserId().orEmpty()
                val hospitals = hospitalReferenceRepository.getAll()
                Triple(syncResult, currentUid, hospitals)
            }

            result.fold(
                onSuccess = { (syncResult, currentUid, hospitals) ->
                    when (syncResult) {
                        is WorkerSyncResult.MatchFound -> {
                            val state = buildStateFromMatch(syncResult, currentUid, hospitals)
                            _uiState.value = state
                            observeMessages(syncResult.matchId)
                        }
                        is WorkerSyncResult.NoMatch -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "No active mutual transfer match found."
                            )
                        }
                        is WorkerSyncResult.NetworkError -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = syncResult.message
                            )
                        }
                        is WorkerSyncResult.AuthError -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = syncResult.message
                            )
                        }
                        is WorkerSyncResult.Conflict -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = syncResult.message
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load mutual transfer data."
                    )
                }
            )
        }
    }

    private fun observeMessages(matchId: String) {
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            transferChatRepository.observeMessages(matchId).collect { messageResult ->
                messageResult.fold(
                    onSuccess = { list ->
                        _uiState.value = _uiState.value.copy(
                            messages = list,
                            isLoading = false
                        )
                    },
                    onFailure = { err ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to listen for incoming messages."
                        )
                    }
                )
            }
        }
    }

    private fun buildStateFromMatch(
        matchFound: WorkerSyncResult.MatchFound,
        currentUid: String,
        hospitals: List<HospitalReference>
    ): TransferChatUiState {
        fun hospitalName(id: String): String =
            hospitals.firstOrNull { it.hospitalId == id }?.name?.takeIf { it.isNotBlank() } ?: id

        fun hospitalLocation(id: String): String {
            val hospital = hospitals.firstOrNull { it.hospitalId == id } ?: return "Sri Lanka"
            return listOfNotNull(
                hospital.district?.takeIf { it.isNotBlank() }?.let { "$it District" },
                hospital.province.takeIf { it.isNotBlank() }?.let { "$it Province" }
            ).joinToString(" • ").ifBlank { "Sri Lanka" }
        }

        val deadlineMs = matchFound.chatDeadline?.let {
            runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
        } ?: matchFound.expiresAt.let {
            runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
        }

        val serverStatus = matchFound.status.trim().uppercase()
        val isTerminal = serverStatus in listOf("CONFIRMED", "CANCELLED", "EXPIRED")
        // CONFIRMED is the authoritative final state: all participants have agreed,
        // even if older cached participant flags have not caught up yet.
        val allParticipantsConfirmed = serverStatus == "CONFIRMED"

        if (matchFound.matchType == "THREE_WAY" && matchFound.threeWayMatch != null) {
            val tw = matchFound.threeWayMatch
            val userRole = when (currentUid) {
                tw.nurseAUid -> "A"
                tw.nurseBUid -> "B"
                tw.nurseCUid -> "C"
                else -> "A"
            }

            val pA = TransferChatParticipant(
                uid = tw.nurseAUid,
                roleLetter = "A",
                displayName = if (tw.nurseAUid == currentUid) "You (Nurse A)" else "Nurse A",
                hospitalId = tw.nurseACurrentHospitalId,
                hospitalName = hospitalName(tw.nurseACurrentHospitalId),
                hospitalLocation = hospitalLocation(tw.nurseACurrentHospitalId),
                grade = tw.nurseAGrade,
                isCurrentUser = tw.nurseAUid == currentUid,
                confirmed = allParticipantsConfirmed || matchFound.confirmedByA
            )

            val pB = TransferChatParticipant(
                uid = tw.nurseBUid,
                roleLetter = "B",
                displayName = if (tw.nurseBUid == currentUid) "You (Nurse B)" else "Nurse B",
                hospitalId = tw.nurseBCurrentHospitalId,
                hospitalName = hospitalName(tw.nurseBCurrentHospitalId),
                hospitalLocation = hospitalLocation(tw.nurseBCurrentHospitalId),
                grade = tw.nurseBGrade,
                isCurrentUser = tw.nurseBUid == currentUid,
                confirmed = allParticipantsConfirmed || matchFound.confirmedByB
            )

            val pC = TransferChatParticipant(
                uid = tw.nurseCUid,
                roleLetter = "C",
                displayName = if (tw.nurseCUid == currentUid) "You (Nurse C)" else "Nurse C",
                hospitalId = tw.nurseCCurrentHospitalId,
                hospitalName = hospitalName(tw.nurseCCurrentHospitalId),
                hospitalLocation = hospitalLocation(tw.nurseCCurrentHospitalId),
                grade = tw.nurseCGrade,
                isCurrentUser = tw.nurseCUid == currentUid,
                confirmed = allParticipantsConfirmed || matchFound.confirmedByC
            )

            val participants = listOf(pA, pB, pC)
            val currentParticipant = participants.find { it.isCurrentUser } ?: pA
            val isUserConfirmed = currentParticipant.confirmed
            val isAllConfirmed = allParticipantsConfirmed || (matchFound.confirmedByA && matchFound.confirmedByB && matchFound.confirmedByC)

            return TransferChatUiState(
                isLoading = false,
                matchId = matchFound.matchId,
                matchType = "THREE_WAY",
                serverStatus = serverStatus,
                chatDeadlineMs = deadlineMs,
                participants = participants,
                currentUserUid = currentUid,
                currentUserRole = userRole,
                currentUserHospitalId = currentParticipant.hospitalId,
                currentUserGrade = currentParticipant.grade,
                isUserConfirmed = isUserConfirmed,
                isAllConfirmed = isAllConfirmed,
                isTerminal = isTerminal
            )
        } else {
            val direct = matchFound.directMatch ?: error("Direct match required")
            val isOfficerA = currentUid == direct.nurseAUid
            val userRole = if (isOfficerA) "A" else "B"

            val pA = TransferChatParticipant(
                uid = direct.nurseAUid,
                roleLetter = "A",
                displayName = if (isOfficerA) "You (Nurse A)" else "Partner (Nurse A)",
                hospitalId = direct.nurseACurrentHospitalId,
                hospitalName = hospitalName(direct.nurseACurrentHospitalId),
                hospitalLocation = hospitalLocation(direct.nurseACurrentHospitalId),
                grade = direct.nurseAGrade,
                isCurrentUser = isOfficerA,
                confirmed = allParticipantsConfirmed || matchFound.confirmedByA
            )

            val pB = TransferChatParticipant(
                uid = direct.nurseBUid,
                roleLetter = "B",
                displayName = if (!isOfficerA) "You (Nurse B)" else "Partner (Nurse B)",
                hospitalId = direct.nurseBCurrentHospitalId,
                hospitalName = hospitalName(direct.nurseBCurrentHospitalId),
                hospitalLocation = hospitalLocation(direct.nurseBCurrentHospitalId),
                grade = direct.nurseBGrade,
                isCurrentUser = !isOfficerA,
                confirmed = allParticipantsConfirmed || matchFound.confirmedByB
            )

            val participants = listOf(pA, pB)
            val isUserConfirmed = if (isOfficerA) matchFound.confirmedByA else matchFound.confirmedByB
            val isAllConfirmed = allParticipantsConfirmed || (matchFound.confirmedByA && matchFound.confirmedByB)

            val currentParticipant = if (isOfficerA) pA else pB

            return TransferChatUiState(
                isLoading = false,
                matchId = matchFound.matchId,
                matchType = "DIRECT_2_WAY",
                serverStatus = serverStatus,
                chatDeadlineMs = deadlineMs,
                participants = participants,
                currentUserUid = currentUid,
                currentUserRole = userRole,
                currentUserHospitalId = currentParticipant.hospitalId,
                currentUserGrade = currentParticipant.grade,
                isUserConfirmed = isUserConfirmed,
                isAllConfirmed = isAllConfirmed,
                isTerminal = isTerminal
            )
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _uiState.value.isSendingMessage) return

        val state = _uiState.value
        if (!state.canSend) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingMessage = true)
            val result = transferChatRepository.sendMessage(
                matchId = state.matchId,
                text = trimmed,
                senderHospitalId = state.currentUserHospitalId,
                senderGrade = state.currentUserGrade
            )

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSendingMessage = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSendingMessage = false,
                        error = error.message ?: "Failed to deliver message."
                    )
                }
            )
        }
    }

    fun confirmTransfer() {
        val state = _uiState.value
        if (!state.canConfirm) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingAction = true, error = null)
            val result = transferRequestRepository.respondToMatch(state.matchId, Decision.CONFIRM)

            result.fold(
                onSuccess = { response ->
                    val updatedParticipants = state.participants.map {
                        if (it.isCurrentUser) it.copy(confirmed = true) else it
                    }
                    val isTerminal = response.newStatus in listOf("CONFIRMED", "CANCELLED", "EXPIRED")
                    val isAllConfirmed = response.newStatus == "CONFIRMED"

                    _uiState.value = _uiState.value.copy(
                        isSubmittingAction = false,
                        serverStatus = response.newStatus,
                        isUserConfirmed = true,
                        isAllConfirmed = isAllConfirmed,
                        participants = updatedParticipants,
                        isTerminal = isTerminal
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmittingAction = false,
                        error = error.message ?: "Failed to confirm mutual transfer."
                    )
                }
            )
        }
    }

    fun leaveTeam(onComplete: () -> Unit) {
        val state = _uiState.value
        if (!state.canLeave) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingAction = true, error = null)
            val result = transferRequestRepository.respondToMatch(state.matchId, Decision.LEAVE)

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingAction = false,
                        serverStatus = "CANCELLED",
                        isTerminal = true
                    )
                    onComplete()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmittingAction = false,
                        error = error.message ?: "Failed to leave transfer team."
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
