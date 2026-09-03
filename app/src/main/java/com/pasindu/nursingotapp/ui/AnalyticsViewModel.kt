package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.domain.usecase.ObserveAllDailyEntriesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimPeriodsForAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AnalyticsDataState(
    val dailyEntries: List<DailyEntryEntity> = emptyList(),
    val claimPeriods: List<ClaimPeriodEntity> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    observeAllDailyEntriesUseCase: ObserveAllDailyEntriesUseCase,
    observeClaimPeriodsForAnalyticsUseCase: ObserveClaimPeriodsForAnalyticsUseCase
) : ViewModel() {
    val data: StateFlow<AnalyticsDataState> = combine(
        observeAllDailyEntriesUseCase(),
        observeClaimPeriodsForAnalyticsUseCase()
    ) { entries, periods ->
        AnalyticsDataState(entries, periods)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AnalyticsDataState()
    )
}
