package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.domain.usecase.CreateClaimPeriodUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteAllClaimPeriodsUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteClaimPeriodUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimPeriodsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ClaimPeriodUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ClaimPeriodViewModel @Inject constructor(
    observeClaimPeriods: ObserveClaimPeriodsUseCase,
    private val createClaimPeriodUseCase: CreateClaimPeriodUseCase,
    private val deleteClaimPeriodUseCase: DeleteClaimPeriodUseCase,
    private val deleteAllClaimPeriodsUseCase: DeleteAllClaimPeriodsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClaimPeriodUiState())
    val uiState: StateFlow<ClaimPeriodUiState> = _uiState.asStateFlow()

    val claimPeriods: StateFlow<List<ClaimPeriodEntity>> = observeClaimPeriods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createClaimPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
        wardType: String,
        onCreated: (Long) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        executeOperation(onError) {
            createClaimPeriodUseCase(startDate, endDate, wardType)
        }?.let(onCreated)
    }

    fun deleteClaimPeriod(
        period: ClaimPeriodEntity,
        onDeleted: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        executeOperation(onError) {
            deleteClaimPeriodUseCase(period)
        }
        onDeleted()
    }

    fun deleteAll(
        onDeleted: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        executeOperation(onError) {
            deleteAllClaimPeriodsUseCase()
        }
        onDeleted()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun <T> executeOperation(
        onError: (Throwable) -> Unit,
        block: suspend () -> T
    ): T? {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { block() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() }
                            ?: "Unable to complete the claim-period operation."
                    )
                    onError(error)
                }
        }
        return null
    }
}
