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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * One-shot navigation events emitted by [TransferChatViewModel].
 */
sealed interface TransferChatNavigationEvent {
    data class NavigateToInstructions(val matchId: String) : TransferChatNavigationEvent
}

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
 * Explanatory notice shown to the nurse when a match reaches a terminal state (CANCELLED or EXPIRED).
 */
data class TerminalNotice(
    val title: String,
    val message: String,
    val isCancelled: Boolean,
    val isSelfInitiated: Boolean
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
    val messageError: String? = null,
    val sendError: String? = null,
    val actionError: String? = null,
    val isTerminal: Boolean = false,
    val terminalNotice: TerminalNotice? = null
) {
    companion object {
        /**
         * Canonical terminal statuses: CONFIRMED, AWAITING_COMPLETION, COMPLETED, CANCELLED, EXPIRED.
         */
        val TERMINAL_STATUSES = setOf("CONFIRMED", "AWAITING_COMPLETION", "COMPLETED", "CANCELLED", "EXPIRED")

        /**
         * Statuses in which coordination chat messages can no longer be written.
         */
        val READ_ONLY_STATUSES = setOf("CONFIRMED", "AWAITING_COMPLETION", "COMPLETED", "CANCELLED", "EXPIRED")

        fun isTerminalStatus(status: String): Boolean =
            status.trim().uppercase() in TERMINAL_STATUSES

        fun isReadOnlyChat(status: String): Boolean =
            status.trim().uppercase() in READ_ONLY_STATUSES
    }

    val canSend: Boolean
        get() = !isLoading && error == null && messageError == null && serverStatus == "CHAT_OPEN" && !isSendingMessage && !isTerminal

    val canConfirm: Boolean
        get() = !isLoading && error == null && serverStatus == "CHAT_OPEN" && !isUserConfirmed && !isSubmittingAction && !isTerminal

    val canLeave: Boolean
        get() = !isLoading && error == null && serverStatus == "CHAT_OPEN" && !isSubmittingAction && !isTerminal
}

private inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

@HiltViewModel
class TransferChatViewModel @Inject constructor(
    private val transferChatRepository: TransferChatRepository,
    private val transferRequestRepository: TransferRequestRepository,
    private val hospitalReferenceRepository: HospitalReferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferChatUiState())
    val uiState: StateFlow<TransferChatUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<TransferChatNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: kotlinx.coroutines.flow.Flow<TransferChatNavigationEvent> = _navigationEvents.receiveAsFlow()
    private var hasEmittedConfirmedNav = false

    private var messageCollectionJob: Job? = null
    private var matchStateCollectionJob: Job? = null

    init {
        loadMatchAndObserveChat()
    }

    fun loadMatchAndObserveChat() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = runSuspendCatching {
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
                            if (syncResult.status.trim().uppercase() == "CONFIRMED") {
                                hasEmittedConfirmedNav = true
                            }
                            observeMessages(syncResult.matchId)
                            observeMatchState(syncResult.matchId)
                        }
                        is WorkerSyncResult.NoMatch,
                        is WorkerSyncResult.RequestCompleted,
                        is WorkerSyncResult.RequestWithdrawn,
                        is WorkerSyncResult.ReturnedToPool -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = when (syncResult) {
                                    is WorkerSyncResult.RequestCompleted -> "This mutual transfer has already been completed."
                                    is WorkerSyncResult.RequestWithdrawn -> "Your mutual transfer request has been withdrawn."
                                    is WorkerSyncResult.ReturnedToPool -> "This match has ended. Your request has been returned to the matching pool."
                                    else -> "No active mutual transfer match found."
                                }
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

    fun retryObserveMessages() {
        val matchId = _uiState.value.matchId
        if (matchId.isNotBlank()) {
            _uiState.value = _uiState.value.copy(messageError = null)
            observeMessages(matchId)
            observeMatchState(matchId)
        }
    }

    private fun observeMatchState(matchId: String) {
        matchStateCollectionJob?.cancel()
        matchStateCollectionJob = viewModelScope.launch {
            transferChatRepository.observeMatchState(matchId).collect { matchResult ->
                matchResult.onSuccess { remoteMatch ->
                    if (remoteMatch != null) {
                        val serverStatus = remoteMatch.status.trim().uppercase()
                        val isTerminal = TransferChatUiState.isTerminalStatus(serverStatus)
                        val isAllConfirmed = serverStatus == "CONFIRMED"

                        val currentState = _uiState.value
                        val updatedParticipants = currentState.participants.map { p ->
                            val isConfirmed = when (p.roleLetter) {
                                "A" -> remoteMatch.confirmedByA
                                "B" -> remoteMatch.confirmedByB
                                "C" -> remoteMatch.confirmedByC
                                else -> p.confirmed
                            } || isAllConfirmed
                            p.copy(confirmed = isConfirmed)
                        }

                        val currentUserParticipant = updatedParticipants.find { it.isCurrentUser }
                        val isUserConfirmed = currentUserParticipant?.confirmed ?: currentState.isUserConfirmed

                        val isSelfInitiated = when (currentState.currentUserRole) {
                            "A" -> remoteMatch.rejectedByA
                            "B" -> remoteMatch.rejectedByB
                            "C" -> remoteMatch.rejectedByC
                            else -> false
                        }

                        val notice = when {
                            serverStatus == "CANCELLED" && currentState.terminalNotice == null -> {
                                if (isSelfInitiated) {
                                    TerminalNotice(
                                        title = "Transfer Team Left",
                                        message = "You have left the transfer team. Your transfer request remains active and in the matching pool to discover new matches.",
                                        isCancelled = true,
                                        isSelfInitiated = true
                                    )
                                } else {
                                    TerminalNotice(
                                        title = "Transfer Team Cancelled",
                                        message = "A participating nurse has left the transfer team. Your transfer request remains active, and your preferred destinations are preserved. You can continue searching for another mutual transfer.",
                                        isCancelled = true,
                                        isSelfInitiated = false
                                    )
                                }
                            }
                            serverStatus == "EXPIRED" && currentState.terminalNotice == null -> {
                                TerminalNotice(
                                    title = "Coordination Window Expired",
                                    message = "The mutual transfer coordination window has ended. Your transfer request remains active, and your preferred destinations are preserved. You can continue searching for another mutual transfer.",
                                    isCancelled = false,
                                    isSelfInitiated = false
                                )
                            }
                            else -> currentState.terminalNotice
                        }

                        _uiState.value = currentState.copy(
                            serverStatus = serverStatus,
                            isTerminal = isTerminal,
                            isAllConfirmed = isAllConfirmed,
                            isUserConfirmed = isUserConfirmed,
                            participants = updatedParticipants,
                            terminalNotice = notice
                        )

                        if (isAllConfirmed && !hasEmittedConfirmedNav) {
                            hasEmittedConfirmedNav = true
                            _navigationEvents.trySend(TransferChatNavigationEvent.NavigateToInstructions(matchId))
                        }

                        // If the match transitioned to a terminal status (CONFIRMED, CANCELLED, or EXPIRED),
                        // reconcile Room cache with backend authoritative state without overwriting newer state.
                        if (serverStatus == "CONFIRMED" || serverStatus == "CANCELLED" || serverStatus == "EXPIRED") {
                            runCatching {
                                transferRequestRepository.syncActiveRequest()
                            }
                        }
                    }
                }
            }
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
                            messageError = null,
                            isLoading = false
                        )
                    },
                    onFailure = { err ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            messageError = err.message ?: "Failed to listen for incoming messages."
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
        val deadlineMs = matchFound.chatDeadline?.let {
            runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
        } ?: matchFound.expiresAt.let {
            runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
        }

        val serverStatus = matchFound.status.trim().uppercase()
        val isTerminal = TransferChatUiState.isTerminalStatus(serverStatus)
        // CONFIRMED is the authoritative final state: all participants have agreed,
        // even if older cached participant flags have not caught up yet.
        val allParticipantsConfirmed = serverStatus == "CONFIRMED"

        if (matchFound.matchType == "THREE_WAY" && matchFound.threeWayMatch != null) {
            val tw = matchFound.threeWayMatch
            if (currentUid != tw.nurseAUid && currentUid != tw.nurseBUid && currentUid != tw.nurseCUid) {
                return TransferChatUiState(
                    isLoading = false,
                    matchId = matchFound.matchId,
                    matchType = "THREE_WAY",
                    serverStatus = serverStatus,
                    error = "Authentication error: You are not a recognized participant in this 3-way mutual transfer match.",
                    isTerminal = true
                )
            }
            val userRole = when (currentUid) {
                tw.nurseAUid -> "A"
                tw.nurseBUid -> "B"
                tw.nurseCUid -> "C"
                else -> ""
            }

            val participants = TransferParticipantBuilder.buildParticipants(matchFound, currentUid, hospitals)
            val currentParticipant = participants.find { it.isCurrentUser } ?: participants.firstOrNull()
            val isUserConfirmed = currentParticipant?.confirmed ?: false
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
                currentUserHospitalId = currentParticipant?.hospitalId.orEmpty(),
                currentUserGrade = currentParticipant?.grade.orEmpty(),
                isUserConfirmed = isUserConfirmed,
                isAllConfirmed = isAllConfirmed,
                isTerminal = isTerminal
            )
        } else {
            val direct = matchFound.directMatch ?: error("Direct match required")
            if (currentUid != direct.nurseAUid && currentUid != direct.nurseBUid) {
                return TransferChatUiState(
                    isLoading = false,
                    matchId = matchFound.matchId,
                    matchType = "DIRECT_2_WAY",
                    serverStatus = serverStatus,
                    error = "Authentication error: You are not a recognized participant in this mutual transfer match.",
                    isTerminal = true
                )
            }
            val isOfficerA = currentUid == direct.nurseAUid
            val userRole = if (isOfficerA) "A" else "B"

            val participants = TransferParticipantBuilder.buildParticipants(matchFound, currentUid, hospitals)
            val isUserConfirmed = if (isOfficerA) matchFound.confirmedByA else matchFound.confirmedByB
            val isAllConfirmed = allParticipantsConfirmed || (matchFound.confirmedByA && matchFound.confirmedByB)
            val currentParticipant = if (isOfficerA) participants.firstOrNull() else participants.getOrNull(1)

            return TransferChatUiState(
                isLoading = false,
                matchId = matchFound.matchId,
                matchType = "DIRECT_2_WAY",
                serverStatus = serverStatus,
                chatDeadlineMs = deadlineMs,
                participants = participants,
                currentUserUid = currentUid,
                currentUserRole = userRole,
                currentUserHospitalId = currentParticipant?.hospitalId.orEmpty(),
                currentUserGrade = currentParticipant?.grade.orEmpty(),
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
            _uiState.value = _uiState.value.copy(isSendingMessage = true, sendError = null)
            val result = transferChatRepository.sendMessage(
                matchId = state.matchId,
                text = trimmed,
                senderHospitalId = state.currentUserHospitalId,
                senderGrade = state.currentUserGrade
            )

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSendingMessage = false,
                        sendError = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSendingMessage = false,
                        sendError = error.message ?: "Failed to deliver message."
                    )
                }
            )
        }
    }

    fun clearSendError() {
        _uiState.value = _uiState.value.copy(sendError = null)
    }

    fun confirmTransfer() {
        val state = _uiState.value
        if (!state.canConfirm) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, actionError = null) }
            val result = transferRequestRepository.respondToMatch(state.matchId, Decision.CONFIRM)

            result.fold(
                onSuccess = { response ->
                    val isTerminal = TransferChatUiState.isTerminalStatus(response.newStatus)
                    val isAllConfirmed = response.newStatus == "CONFIRMED"

                    _uiState.update { current ->
                        val updatedParticipants = current.participants.map {
                            if (it.isCurrentUser) it.copy(confirmed = true) else it
                        }
                        current.copy(
                            isSubmittingAction = false,
                            serverStatus = response.newStatus,
                            isUserConfirmed = true,
                            isAllConfirmed = isAllConfirmed,
                            participants = updatedParticipants,
                            isTerminal = isTerminal,
                            actionError = null
                        )
                    }

                    if (isAllConfirmed && !hasEmittedConfirmedNav) {
                        hasEmittedConfirmedNav = true
                        _navigationEvents.trySend(TransferChatNavigationEvent.NavigateToInstructions(state.matchId))
                    }
                },
                onFailure = { error ->
                    _uiState.update { current ->
                        current.copy(
                            isSubmittingAction = false,
                            actionError = error.message ?: "Failed to confirm mutual transfer."
                        )
                    }
                }
            )
        }
    }

    fun leaveTeam(onComplete: () -> Unit) {
        val state = _uiState.value
        if (!state.canLeave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, actionError = null) }
            val result = transferRequestRepository.respondToMatch(state.matchId, Decision.LEAVE)

            result.fold(
                onSuccess = {
                    _uiState.update { current ->
                        current.copy(
                            isSubmittingAction = false,
                            serverStatus = "CANCELLED",
                            isTerminal = true,
                            actionError = null
                        )
                    }
                    onComplete()
                },
                onFailure = { error ->
                    _uiState.update { current ->
                        current.copy(
                            isSubmittingAction = false,
                            actionError = error.message ?: "Failed to leave transfer team."
                        )
                    }
                }
            )
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
