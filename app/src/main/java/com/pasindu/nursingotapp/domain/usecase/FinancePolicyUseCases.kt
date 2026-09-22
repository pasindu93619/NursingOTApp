package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.first

class EnsureManualPayRateRecordUseCase(
    private val payRateSettingsDao: PayRateSettingsDao
) {
    suspend operator fun invoke() {
        if (payRateSettingsDao.observe().first() == null) {
            payRateSettingsDao.upsert(
                PayRateSettingsEntity(
                    id = 1,
                    otRate = 0.0,
                    phRate = 0.0,
                    doRate = 0.0,
                    rateSource = "MANUAL",
                    basisSalary2027 = null
                )
            )
        }
    }
}

class SynchronizePolicyRatesUseCase(
    private val payRateSettingsDao: PayRateSettingsDao,
    private val salaryStep2027Dao: SalaryStep2027Dao
) {
    suspend operator fun invoke(profile: ProfileEntity) {
        val salaryStep = salaryStep2027Dao.findByCurrentBasic(
            grade = profile.grade,
            currentBasicSalary = profile.basicSalary
        ) ?: return

        val current = payRateSettingsDao.observe().first()
        val otRate = (current?.otRate ?: profile.otRate).coerceAtLeast(0.0)
        val dayRate = salaryStep.basicSalary2027
            .div(30.0)
            .coerceAtLeast(0.0)

        payRateSettingsDao.upsert(
            PayRateSettingsEntity(
                id = 1,
                otRate = otRate,
                phRate = dayRate,
                doRate = dayRate,
                rateSource = "2027_BASIC_SALARY_DIV_30",
                basisSalary2027 = salaryStep.basicSalary2027
            )
        )
    }
}

/**
 * Re-applies the same matched 2027 salary-step policy used by the finance
 * synchronization path. This prevents individual screens from maintaining
 * a separate PH/DO calculation.
 */
class ApplyMatched2027DayRateUseCase(
    private val payRateSettingsDao: PayRateSettingsDao,
    private val salaryStep2027Dao: SalaryStep2027Dao
) {
    suspend operator fun invoke(profile: ProfileEntity): Double? {
        val salaryStep = salaryStep2027Dao.findByCurrentBasic(
            grade = profile.grade,
            currentBasicSalary = profile.basicSalary
        ) ?: return null

        val dayRate = salaryStep.basicSalary2027
            .div(30.0)
            .coerceAtLeast(0.0)

        val current = payRateSettingsDao.observe().first()
        payRateSettingsDao.upsert(
            PayRateSettingsEntity(
                id = 1,
                otRate = (current?.otRate ?: profile.otRate).coerceAtLeast(0.0),
                phRate = dayRate,
                doRate = dayRate,
                rateSource = "2027_BASIC_SALARY_DIV_30",
                basisSalary2027 = salaryStep.basicSalary2027
            )
        )
        return dayRate
    }
}

class SaveFinanceCompensationUseCase(
    private val profileCompensationDao: ProfileCompensationDao
) {
    suspend operator fun invoke(
        riskAllowance: Double,
        claAllowance: Double,
        additionalAllowancesTotal: Double,
        totalDeductions: Double
    ) {
        profileCompensationDao.upsert(
            ProfileCompensationEntity(
                id = 1,
                riskAllowance = riskAllowance.coerceAtLeast(0.0),
                claAllowance = claAllowance.coerceAtLeast(0.0),
                additionalAllowancesTotal = additionalAllowancesTotal.coerceAtLeast(0.0),
                totalDeductions = totalDeductions.coerceAtLeast(0.0)
            )
        )
    }
}

class SaveFinanceRatesUseCase(
    private val payRateSettingsDao: PayRateSettingsDao
) {
    suspend operator fun invoke(
        otRate: Double,
        phRate: Double,
        doRate: Double,
        basisSalary2027: Double?,
        rateSource: String
    ) {
        payRateSettingsDao.upsert(
            PayRateSettingsEntity(
                id = 1,
                otRate = otRate.coerceAtLeast(0.0),
                phRate = phRate.coerceAtLeast(0.0),
                doRate = doRate.coerceAtLeast(0.0),
                rateSource = rateSource.ifBlank { "MANUAL" },
                basisSalary2027 = basisSalary2027?.takeIf { it > 0.0 }
            )
        )
    }
}
