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

/**
 * Resolve the current 2026 salary row from the exact nurse profile inputs,
 * then apply the matched 2027 basic salary as the authoritative PH/DO basis.
 *
 * The two operations remain deliberately separate so the salary-table lookup
 * and the day-rate application are individually testable and reusable by
 * Profile, Home, PDF export, and Finance.
 */
class ApplyFinancePolicyRatesUseCase(
    private val matchSalaryStepUseCase: MatchSalaryStepUseCase,
    private val applyMatched2027DayRateUseCase: ApplyMatched2027DayRateUseCase
) {
    suspend operator fun invoke(profile: ProfileEntity) =
        matchSalaryStepUseCase(
            grade = profile.grade,
            currentBasicSalary = profile.basicSalary
        )?.let { matched ->
            applyMatched2027DayRateUseCase(matched.basicSalary2027)
            matched
        }
}

/**
 * Backwards-compatible policy synchronizer retained for other existing
 * callers. Advanced Finance uses [ApplyFinancePolicyRatesUseCase] so its
 * source path is explicit and identical to the corrected profile flow.
 */
class SynchronizePolicyRatesUseCase(
    private val payRateSettingsDao: PayRateSettingsDao,
    private val salaryStep2027Dao: SalaryStep2027Dao
) {
    suspend operator fun invoke(profile: ProfileEntity) {
        val salaryStep = salaryStep2027Dao.findByCurrentBasic(
            grade = profile.grade,
            currentBasicSalary = profile.basicSalary
        ) ?: return

        val otRate =
            NursingOtRatePolicy.rateForGrade(profile.grade)
                ?: profile.otRate.coerceAtLeast(0.0)
        val dayRate = (salaryStep.basicSalary2027 / 30.0).coerceAtLeast(0.0)

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
                riskAllowance = 6850.0,
                claAllowance = 17800.0,
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
