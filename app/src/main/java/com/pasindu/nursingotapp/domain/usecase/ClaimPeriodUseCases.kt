package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import kotlinx.coroutines.flow.first

class ObserveClaimPeriodsUseCase(private val dao: ClaimPeriodDao) {
    operator fun invoke() = dao.observeClaimPeriods()
}

class CreateClaimPeriodUseCase(private val dao: ClaimPeriodDao) {
    suspend operator fun invoke(startDate: java.time.LocalDate, endDate: java.time.LocalDate, wardType: String): Long {
        require(!endDate.isBefore(startDate)) { "Claim period end date cannot be before start date." }
        require(wardType.isNotBlank()) { "Ward type cannot be blank." }
        return dao.insertClaimPeriod(
            ClaimPeriodEntity(
                startDate = startDate,
                endDate = endDate,
                createdAt = System.currentTimeMillis(),
                wardType = wardType.trim()
            )
        )
    }
}

class DeleteClaimPeriodUseCase(
    private val claimPeriodDao: ClaimPeriodDao,
    private val dailyEntryDao: DailyEntryDao
) {
    suspend operator fun invoke(period: ClaimPeriodEntity) {
        // Preserve the existing explicit delete order: child daily entries first, then parent claim.
        dailyEntryDao.deleteEntriesForPeriod(period.id)
        claimPeriodDao.deleteClaimPeriod(period)
    }
}

class DeleteAllClaimPeriodsUseCase(
    private val claimPeriodDao: ClaimPeriodDao,
    private val dailyEntryDao: DailyEntryDao
) {
    suspend operator fun invoke() {
        // Preserve the existing explicit delete order for complete history removal.
        dailyEntryDao.deleteAllEntries()
        claimPeriodDao.deleteAllClaimPeriods()
    }
}
