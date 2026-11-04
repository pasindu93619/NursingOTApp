package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferCompletionUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val status: TransferCompletionStatus? = null,
    val error: String? = null
) {
    val friendlyStatusText: String
        get() = when (status?.status?.trim()?.uppercase()) {
            "CONFIRMED" -> "Agreement Confirmed"
            "AWAITING_COMPLETION" -> "Awaiting Physical Transfer Completion"
            "COMPLETED" -> "Transfer Finalized & Completed"
            "CANCELLED" -> "Match Cancelled"
            "EXPIRED" -> "Match Expired"
            else -> status?.status?.replace('_', ' ') ?: "Unknown"
        }
}

internal fun friendlyCompletionError(error: Throwable): String {
    val detail = error.message.orEmpty()
    return when {
        detail.contains("HTTP 404", ignoreCase = true) ||
            detail.contains("NotFound", ignoreCase = true) ||
            detail.contains("Endpoint '/api/matching/completion' not found", ignoreCase = true) ->
            "The transfer status service is not available right now. Your completion report has not been confirmed by the server. Please try again later."
        detail.contains("HTTP 401", ignoreCase = true) ||
            detail.contains("Unauthorized", ignoreCase = true) ->
            "Your session needs to be refreshed before we can check the transfer. Please sign in again and retry."
        detail.contains("HTTP 403", ignoreCase = true) ->
            "This transfer record could not be verified for your account. Please return to the match and try again."
        else ->
            "We couldn't update your transfer status just now. Your report has not been confirmed by the server. Check your connection and try again."
    }
}

@HiltViewModel
class TransferCompletionViewModel @Inject constructor(
    private val repository: TransferRequestRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransferCompletionUiState())
    val uiState: StateFlow<TransferCompletionUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    val isAutoRefreshActive: Boolean
        get() = autoRefreshJob?.isActive == true

    fun load(matchId: String, isManualOrInitial: Boolean = true) {
        if (matchId.isBlank()) {
            _uiState.value = TransferCompletionUiState(isLoading = false, error = "Missing match reference.")
            return
        }
        viewModelScope.launch {
            if (isManualOrInitial && _uiState.value.status == null) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            repository.getCompletionStatus(matchId).fold(
                onSuccess = { status ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        status = status,
                        error = null
                    )
                    val terminal = status.status.trim().uppercase()
                    if (terminal == "COMPLETED" || terminal == "CANCELLED" || terminal == "EXPIRED") {
                        stopAutoRefresh()
                    }
                },
                onFailure = { error ->
                    val friendlyError = friendlyCompletionError(error)
                    // Preserve existing status if we already loaded it, showing transient error banner
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = friendlyError
                    )
                    val detail = error.message.orEmpty()
                    if (detail.contains("HTTP 401", ignoreCase = true) ||
                        detail.contains("HTTP 403", ignoreCase = true) ||
                        detail.contains("HTTP 404", ignoreCase = true) ||
                        detail.contains("NotFound", ignoreCase = true)
                    ) {
                        stopAutoRefresh()
                    }
                }
            )
        }
    }

    fun startAutoRefresh(matchId: String) {
        if (autoRefreshJob?.isActive == true || matchId.isBlank()) return
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                val currentStatus = _uiState.value.status?.status?.trim()?.uppercase()
                if (currentStatus == "COMPLETED" || currentStatus == "CANCELLED" || currentStatus == "EXPIRED") {
                    break
                }
                delay(20_000L)
                val statusAfterDelay = _uiState.value.status?.status?.trim()?.uppercase()
                if (statusAfterDelay == "COMPLETED" || statusAfterDelay == "CANCELLED" || statusAfterDelay == "EXPIRED") {
                    break
                }
                load(matchId, isManualOrInitial = false)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }

    fun reportComplete(matchId: String) {
        val current = _uiState.value
        if (current.isSubmitting || current.status?.callerReportedComplete == true || current.status?.status == "COMPLETED") return
        viewModelScope.launch {
            _uiState.value = current.copy(isSubmitting = true, error = null)
            repository.reportCompletion(matchId).fold(
                onSuccess = { status ->
                    _uiState.value = _uiState.value.copy(isSubmitting = false, status = status, error = null)
                    val terminal = status.status.trim().uppercase()
                    if (terminal == "COMPLETED" || terminal == "CANCELLED" || terminal == "EXPIRED") {
                        stopAutoRefresh()
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isSubmitting = false, error = friendlyCompletionError(error))
                }
            )
        }
    }
}
