package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.HospitalReferenceRepository
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import com.pasindu.nursingotapp.transfer.data.model.RankedPreferences
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads the canonical 2026 hospital reference data for the Mutual Transfer UI
 * and wires the nurse's active transfer request into Room via
 * [TransferRequestRepository].
 *
 * Phase 1.5.3A additions:
 * - Observes the active request from Room and exposes it as [activeRequest].
 * - Provides [submitRequest] to save/update a request in Room.
 * - Provides [withdrawRequest] to clear the active request from Room.
 * - Exposes [submitError] for error messaging.
 *
 * The hospital reference loading, search, and ranking behaviour is unchanged.
 */
@HiltViewModel
class TransferRequestViewModel @Inject constructor(
    private val hospitalReferenceRepository: HospitalReferenceRepository,
    private val transferRequestRepository: TransferRequestRepository
) : ViewModel() {

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
    // Active transfer request (Phase 1.5.3A)
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

    init {
        loadHospitals()
        observeActiveRequest()
    }

    // ---------------------------------------------------------------------------
    // Hospital directory (existing, unchanged)
    // ---------------------------------------------------------------------------

    private fun loadHospitals() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null

            runCatching {
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
    // Active request observation (Phase 1.5.3A)
    // ---------------------------------------------------------------------------

    private fun observeActiveRequest() {
        viewModelScope.launch {
            transferRequestRepository.observeActiveRequest().collect { request ->
                _activeRequest.value = request
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Submit / save request (Phase 1.5.3A)
    // ---------------------------------------------------------------------------

    /**
     * Saves or updates the nurse's active transfer request in Room.
     *
     * @param currentHospitalId Canonical hospitalId of the nurse's current posting.
     * @param preferenceHospitalIds Ranked list of preferred hospital IDs.
     *   Order is preserved exactly: index 0 = 1st preference.
     *
     * Validation failures and Room errors are surfaced via [submitError].
     */
    fun submitRequest(
        currentHospitalId: String,
        preferenceHospitalIds: List<String>
    ) {
        if (_isSubmitting.value) return

        viewModelScope.launch {
            _isSubmitting.value = true
            _submitError.value = null

            runCatching {
                val prefs = RankedPreferences(preferenceHospitalIds)
                transferRequestRepository.saveRequest(currentHospitalId, prefs)
            }.onFailure { error ->
                _submitError.value =
                    error.message ?: "Unable to save transfer request"
            }

            _isSubmitting.value = false
        }
    }

    // ---------------------------------------------------------------------------
    // Withdraw / clear request (Phase 1.5.3A)
    // ---------------------------------------------------------------------------

    /**
     * Clears the active transfer request from Room.
     *
     * After this call [activeRequest] will emit `null`.
     * Errors are surfaced via [submitError].
     */
    fun withdrawRequest() {
        viewModelScope.launch {
            _submitError.value = null

            runCatching {
                transferRequestRepository.clearRequest()
            }.onFailure { error ->
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
