package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import java.time.LocalDate

class ObserveClaimDailyEntriesUseCase(private val dao: DailyEntryDao) {
    operator fun invoke(claimPeriodId: Long) = dao.observeEntriesForPeriod(claimPeriodId)
}

class SaveDailyEntryUseCase(private val dao: DailyEntryDao) {
    suspend operator fun invoke(entry: DailyEntryEntity) {
        dao.insertEntry(entry)
    }
}

class GetDailyEntryForDateUseCase(private val dao: DailyEntryDao) {
    suspend operator fun invoke(claimPeriodId: Long, date: LocalDate): DailyEntryEntity? =
        dao.getEntryForDate(claimPeriodId, date)
}
