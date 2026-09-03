package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import kotlinx.coroutines.flow.Flow

class ObserveAllDailyEntriesUseCase(
    private val dao: DailyEntryDao
) {
    operator fun invoke(): Flow<List<com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity>> =
        dao.observeAllEntries()
}

class ObserveClaimPeriodsForAnalyticsUseCase(
    private val dao: ClaimPeriodDao
) {
    operator fun invoke(): Flow<List<com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity>> =
        dao.observeClaimPeriods()
}
