package com.pasindu.nursingotapp.transfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.HospitalReferenceRepository
import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads the canonical 2026 hospital reference data for the Mutual Transfer UI.
 *
 * The repository owns dataset integrity validation and in-memory caching.
 * This ViewModel only exposes the loaded reference data to Compose.
 */
@HiltViewModel
class TransferRequestViewModel @Inject constructor(
    private val hospitalReferenceRepository: HospitalReferenceRepository
) : ViewModel() {

    private val _hospitalOptions = MutableStateFlow<List<HospitalReference>>(emptyList())
    val hospitalOptions: StateFlow<List<HospitalReference>> = _hospitalOptions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    init {
        loadHospitals()
    }

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
}
