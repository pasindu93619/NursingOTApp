package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.domain.usecase.CreateClaimPeriodUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteAllClaimPeriodsUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteClaimPeriodUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimPeriodsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClaimPeriodViewModel @Inject constructor(
    observeClaimPeriods: ObserveClaimPeriodsUseCase,
    private val createClaimPeriodUseCase: CreateClaimPeriodUseCase,
    private val deleteClaimPeriodUseCase: DeleteClaimPeriodUseCase,
    private val deleteAllClaimPeriodsUseCase: DeleteAllClaimPeriodsUseCase
) : ViewModel() {

    val claimPeriods = observeClaimPeriods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createClaimPeriod(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
        wardType: String,
        onCreated: (Long) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching { createClaimPeriodUseCase(startDate, endDate, wardType) }
                .onSuccess(onCreated)
                .onFailure(onError)
        }
    }

    fun deleteClaimPeriod(
        period: ClaimPeriodEntity,
        onDeleted: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching { deleteClaimPeriodUseCase(period) }
                .onSuccess { onDeleted() }
                .onFailure(onError)
        }
    }

    fun deleteAll(
        onDeleted: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching { deleteAllClaimPeriodsUseCase() }
                .onSuccess { onDeleted() }
                .onFailure(onError)
        }
    }
}
