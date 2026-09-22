package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import kotlinx.coroutines.flow.first

data class FinanceDeductionState(
    val apit: Double,
    val wop: Double,
    val loan: Double,
    val other: Double
) {
    val total: Double
        get() = apit + wop + loan + other

    val hasEnteredDeduction: Boolean
        get() = listOf(apit, wop, loan, other).any { it > 0.0 }
}

class ObserveFinanceDeductionsUseCase(
    private val dao: ProfileCompensationDao
) {
    operator fun invoke() = dao.observe()
}

class SaveFinanceDeductionsUseCase(
    private val dao: ProfileCompensationDao
) {
    suspend operator fun invoke(
        apit: Double,
        wop: Double,
        loan: Double,
        other: Double
    ) {
        val current = dao.observe().first()
        dao.upsert(
            ProfileCompensationEntity(
                id = 1,
                riskAllowance = current?.riskAllowance ?: 0.0,
                claAllowance = current?.claAllowance ?: 0.0,
                additionalAllowancesTotal = current?.additionalAllowancesTotal ?: 0.0,
                totalDeductions = (apit + wop + loan + other).coerceAtLeast(0.0),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
