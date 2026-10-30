package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.HospitalReferenceRepository
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import com.pasindu.nursingotapp.transfer.data.model.RankedPreferences
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import com.pasindu.nursingotapp.transfer.worker.NoOpTransferSyncScheduler
import com.pasindu.nursingotapp.transfer.worker.TransferSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

@HiltViewModel
class TransferRequestViewModel @Inject constructor(
    private val hospitalReferenceRepository: HospitalReferenceRepository,
    private val transferRequestRepository: TransferRequestRepository,
    private val syncScheduler: TransferSyncScheduler,
    private val logger: com.pasindu.nursingotapp.transfer.data.TransferSyncLogger = com.pasindu.nursingotapp.transfer.data.NoOpTransferSyncLogger
) : ViewModel() {

    constructor(
        hospitalReferenceRepository: HospitalReferenceRepository,
        transferRequestRepository: TransferRequestRepository
    ) : this(
        hospitalReferenceRepository = hospitalReferenceRepository,
        transferRequestRepository = transferRequestRepository,
        syncScheduler = NoOpTransferSyncScheduler,
        logger = com.pasindu.nursingotapp.transfer.data.NoOpTransferSyncLogger
    )

    private var consecutivePollFailures = 0
    private var hasAttemptedAuthRefresh = false
    private val _pollWarning = MutableStateFlow<String?>(null)
    val pollWarning: StateFlow<String?> = _pollWarning.asStateFlow()

    companion object {
        private const val TAG = "TransferRequestViewModel"
    }

    // ---------------------------------------------------------------------------
    // Hospital directory (existing, unchanged behaviour)
    // ---------------------------------------------------------------------------

    private val _hospitalOptions = MutableStateFlow<List<HospitalReference>>(emptyList())
    val hospitalOptions: StateFlow<List<HospitalReference>> = _hospitalOptions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    // ---------------------------------------------------------------------------
    // Active transfer request (Phase 1.5.3A & 1.5.3B)
    // ---------------------------------------------------------------------------

    private val _activeRequest = MutableStateFlow<TransferRequest?>(null)
    val activeRequest: StateFlow<TransferRequest?> = _activeRequest.asStateFlow()

    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // ---------------------------------------------------------------------------
    // Initialisation
    // ---------------------------------------------------------------------------

    private var automaticPollingJob: kotlinx.coroutines.Job? = null

    val isPollingActive: Boolean
        get() = automaticPollingJob?.isActive == true

    init {
        loadHospitals()
        observeActiveRequest()
    }

    /**
     * Keeps an active SEARCHING request synchronized with the server while this
     * destination is active. The server is authoritative; syncActiveRequest() first
     * checks for a remote MATCHED state before publishing local SEARCHING data.
     */
    fun startAutomaticMatchPolling() {
        if (automaticPollingJob?.isActive == true) return
        automaticPollingJob = viewModelScope.launch {
            while (isActive) {
                val request = _activeRequest.value
                val pollIntervalMs = if (request != null && (
                    request.matchStatus.equals("PENDING_CONFIRMATION", ignoreCase = true) ||
                    request.matchStatus.equals("CHAT_OPEN", ignoreCase = true)
                )) {
                    5_000L
                } else if (request != null && (
                    request.matchStatus.equals("CONFIRMED", ignoreCase = true) ||
                    request.matchStatus.equals("AWAITING_COMPLETION", ignoreCase = true)
                )) {
                    30_000L
                } else {
                    15_000L
                }

                val shouldPoll = request != null && (
                    request.requestStatus == com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus.PENDING ||
                    (request.requestStatus == com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus.MATCHED &&
                        !request.matchStatus.equals("COMPLETED", ignoreCase = true))
                )

                if (shouldPoll) {
                    try {
                        val syncResult = transferRequestRepository.syncActiveRequest()
                        when (syncResult) {
                            is WorkerSyncResult.MatchFound,
                            is WorkerSyncResult.NoMatch,
                            is WorkerSyncResult.ReturnedToPool,
                            is WorkerSyncResult.RequestCompleted,
                            is WorkerSyncResult.RequestWithdrawn -> {
                                consecutivePollFailures = 0
                                hasAttemptedAuthRefresh = false
                                _pollWarning.value = null
                            }
                            is WorkerSyncResult.AuthError -> {
                                logger.e(TAG, "AuthError during pool polling: ${syncResult.message}")
                                if (!hasAttemptedAuthRefresh) {
                                    hasAttemptedAuthRefresh = true
                                    transferRequestRepository.refreshAuthToken()
                                } else {
                                    consecutivePollFailures++
                                    if (consecutivePollFailures >= 3) {
                                        _pollWarning.value = POLL_WARNING_MESSAGE
                                    }
                                }
                            }
                            is WorkerSyncResult.NetworkError -> {
                                logger.e(TAG, "NetworkError during pool polling: ${syncResult.message}", syncResult.cause)
                                consecutivePollFailures++
                                if (consecutivePollFailures >= 3) {
                                    _pollWarning.value = POLL_WARNING_MESSAGE
                                }
                            }
                            else -> {}
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        logger.e(TAG, "poll failed", e)
                        consecutivePollFailures++
                        if (consecutivePollFailures >= 3) {
                            _pollWarning.value = POLL_WARNING_MESSAGE
                        }
                    }
                }
                val delayMs = if (consecutivePollFailures > 0) {
                    calculatePollBackoff(consecutivePollFailures, pollIntervalMs)
                } else {
                    pollIntervalMs
                }
                delay(delayMs)
            }
        }
    }

    fun stopAutomaticMatchPolling() {
        automaticPollingJob?.cancel()
        automaticPollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutomaticMatchPolling()
    }

    // ---------------------------------------------------------------------------
    // Hospital directory (existing, unchanged)
    // ---------------------------------------------------------------------------

    private fun loadHospitals() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null

            runSuspendCatching {
                hospitalReferenceRepository.getAll()
            }.onSuccess { hospitals ->
                _hospitalOptions.value = hospitals
            }.onFailure { error ->
                _hospitalOptions.value = emptyList()
                _loadError.value =
                    error.message ?: "Unable to load hospital reference data"
            }

            _isLoading.value = false
        }
    }

    fun retry() {
        loadHospitals()
    }

    // ---------------------------------------------------------------------------
    // Active request observation
    // ---------------------------------------------------------------------------

    private fun observeActiveRequest() {
        viewModelScope.launch {
            transferRequestRepository.observeActiveRequest().collect { request ->
                _activeRequest.value = request
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Submit / save / update request
    // ---------------------------------------------------------------------------

    /**
     * Saves or updates the nurse's active transfer request in Room.
     *
     * @param currentHospitalId Canonical hospitalId of the nurse's current posting.
     * @param preferenceHospitalIds Ranked list of preferred hospital IDs.
     *   Order is preserved exactly: index 0 = 1st preference.
     * @param onSuccess Invoked on the main dispatcher only when persistence succeeds.
     *
     * Validation failures and Room errors are surfaced via [submitError].
     */
    fun submitRequest(
        currentHospitalId: String,
        preferenceHospitalIds: List<String>,
        onSuccess: () -> Unit = {}
    ) {
        if (_isSubmitting.value) return

        viewModelScope.launch {
            _isSubmitting.value = true
            _submitError.value = null

            runSuspendCatching {
                val prefs = RankedPreferences(preferenceHospitalIds)
                transferRequestRepository.saveRequest(currentHospitalId, prefs)
            }.onSuccess {
                // Schedule WorkManager resilient offline-first sync
                syncScheduler.scheduleSync()
                // Also trigger immediate background sync attempt if device is already online
                viewModelScope.launch {
                    runSuspendCatching { transferRequestRepository.syncActiveRequest() }
                }
                onSuccess()
            }.onFailure { error ->
                _submitError.value =
                    error.message ?: "Unable to save transfer request"
            }

            _isSubmitting.value = false
        }
    }

    // ---------------------------------------------------------------------------
    // Withdraw / clear request
    // ---------------------------------------------------------------------------

    /**
     * Clears the active transfer request from Room.
     *
     * After this call [activeRequest] will emit `null`.
     * Errors are surfaced via [submitError].
     *
     * @param onSuccess Invoked on the main dispatcher only when clear succeeds.
     */
    fun withdrawRequest(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _submitError.value = null

            runSuspendCatching {
                transferRequestRepository.clearRequest()
            }.onSuccess {
                _isSubmitting.value = false
                onSuccess()
            }.onFailure { error ->
                _isSubmitting.value = false
                _submitError.value =
                    error.message ?: "Unable to withdraw transfer request"
            }
        }
    }

    /**
     * Clears any previously surfaced submit/withdraw error.
     */
    fun clearSubmitError() {
        _submitError.value = null
    }
}
