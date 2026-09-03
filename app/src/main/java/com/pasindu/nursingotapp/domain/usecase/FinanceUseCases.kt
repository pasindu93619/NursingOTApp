package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.logic.CalculationEngine
import kotlinx.coroutines.flow.first
import kotlin.math.abs

class ObserveFinanceProfileUseCase(private val dao: ProfileDao) {
    operator fun invoke() = dao.observeProfile()
}

class ObserveFinanceClaimPeriodUseCase(private val dao: ClaimPeriodDao) {
    operator fun invoke() = dao.observeClaimPeriods()
}

class ObserveFinancePayRatesUseCase(private val dao: PayRateSettingsDao) {
    operator fun invoke() = dao.observe()
}

class ObserveFinanceCompensationUseCase(private val dao: ProfileCompensationDao) {
    operator fun invoke() = dao.observe()
}

class ObserveFinanceEntriesUseCase(private val dao: DailyEntryDao) {
    operator fun invoke(claimPeriodId: Long) = dao.observeEntriesForPeriod(claimPeriodId)
}

class EnsureManualPayRateRecordUseCase(private val dao: PayRateSettingsDao) {
    suspend operator fun invoke() {
        if (dao.observe().first() == null) {
            dao.upsert(PayRateSettingsEntity(id = 1, rateSource = "MANUAL"))
        }
    }
}

class SynchronizePolicyRatesUseCase(
    private val payRateSettingsDao: PayRateSettingsDao,
    private val salaryStep2027Dao: SalaryStep2027Dao
) {
    suspend operator fun invoke(profile: ProfileEntity) {
        val current = payRateSettingsDao.observe().first()
        val grade = normalizeGrade(profile.grade)
        val matched = salaryStep2027Dao.observeForGrade(grade).first()
            .firstOrNull { abs(it.currentBasicSalary2026 - profile.basicSalary) < 0.01 }
            ?: return

        val dayRate = matched.basicSalary2027 / 30.0
        val existingOtRate = current?.otRate ?: 0.0
        val alreadyCorrect = current?.basisSalary2027 == matched.basicSalary2027 &&
            abs(current.phRate - dayRate) < 0.01 &&
            abs(current.doRate - dayRate) < 0.01 &&
            current.rateSource == "2027_BASIC_SALARY_DIV_30"

        if (!alreadyCorrect) {
            payRateSettingsDao.upsert(
                PayRateSettingsEntity(
                    id = 1,
                    otRate = existingOtRate.coerceAtLeast(0.0),
                    phRate = dayRate,
                    doRate = dayRate,
                    rateSource = "2027_BASIC_SALARY_DIV_30",
                    basisSalary2027 = matched.basicSalary2027,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}

class CalculateFinanceClaimUseCase(private val entriesDao: DailyEntryDao) {
    suspend operator fun invoke(
        profile: ProfileEntity,
        claimPeriodId: Long,
        payRates: CalculationEngine.PayRates?
    ) = entriesDao.observeEntriesForPeriod(claimPeriodId).first().let { entries ->
        CalculationEngine.processClaimData(
            profileEntity = profile,
            entries = entries,
            claimStart = requireNotNull(entriesDao.getClaimPeriodStartFallback(claimPeriodId))
                { "Claim period start date unavailable." },
            claimEnd = requireNotNull(entriesDao.getClaimPeriodEndFallback(claimPeriodId))
                { "Claim period end date unavailable." },
            payRates = payRates
        )
    }
}

class SaveFinanceCompensationUseCase(private val dao: ProfileCompensationDao) {
    suspend operator fun invoke(
        riskAllowance: Double,
        claAllowance: Double,
        additionalAllowancesTotal: Double,
        totalDeductions: Double
    ) {
        dao.upsert(
            ProfileCompensationEntity(
                id = 1,
                riskAllowance = riskAllowance.coerceAtLeast(0.0),
                claAllowance = claAllowance.coerceAtLeast(0.0),
                additionalAllowancesTotal = additionalAllowancesTotal.coerceAtLeast(0.0),
                totalDeductions = totalDeductions.coerceAtLeast(0.0),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

class SaveFinanceRatesUseCase(private val dao: PayRateSettingsDao) {
    suspend operator fun invoke(
        otRate: Double,
        phRate: Double,
        doRate: Double,
        basisSalary2027: Double?,
        source: String
    ) {
        dao.upsert(
            PayRateSettingsEntity(
                id = 1,
                otRate = otRate.coerceAtLeast(0.0),
                phRate = phRate.coerceAtLeast(0.0),
                doRate = doRate.coerceAtLeast(0.0),
                rateSource = source,
                basisSalary2027 = basisSalary2027,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

private fun normalizeGrade(value: String): String {
    val cleaned = value.trim().uppercase().replace("GRADE", "").trim()
    return when (cleaned) {
        "1", "I" -> "I"
        "2", "II" -> "II"
        "3", "III" -> "III"
        else -> value.trim().uppercase()
    }
}

private suspend fun DailyEntryDao.getClaimPeriodStartFallback(id: Long) =
    error("Not implemented: Finance claim calculation should receive claim period from its dedicated use case.")

private suspend fun DailyEntryDao.getClaimPeriodEndFallback(id: Long) =
    error("Not implemented: Finance claim calculation should receive claim period from its dedicated use case.")
