package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.first
import kotlin.math.abs

class ObserveProfileUseCase(private val profileDao: ProfileDao) {
    operator fun invoke() = profileDao.observeProfile()
}

class ObserveProfileCompensationUseCase(private val dao: ProfileCompensationDao) {
    operator fun invoke() = dao.observe()
}

class ObserveOtRateUseCase(private val dao: PayRateSettingsDao) {
    operator fun invoke() = dao.observe()
}

class SaveProfileUseCase(private val profileDao: ProfileDao) {
    suspend operator fun invoke(profile: ProfileEntity) = profileDao.upsert(profile)
}

class SaveProfileCompensationUseCase(private val dao: ProfileCompensationDao) {
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

class SaveOtRateUseCase(private val dao: PayRateSettingsDao) {
    suspend operator fun invoke(value: Double) {
        val current = dao.observe().first()
        dao.upsert(
            PayRateSettingsEntity(
                id = 1,
                otRate = value.coerceAtLeast(0.0),
                phRate = current?.phRate ?: 0.0,
                doRate = current?.doRate ?: 0.0,
                rateSource = "MANUAL",
                basisSalary2027 = current?.basisSalary2027,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

class MatchSalaryStepUseCase(private val salaryDao: SalaryStep2027Dao) {
    suspend operator fun invoke(grade: String, currentBasicSalary: Double) =
        if (grade.isBlank() || currentBasicSalary <= 0.0) {
            null
        } else {
            salaryDao.observeForGrade(grade.trim()).first()
                .minByOrNull { row -> abs(row.currentBasicSalary2026 - currentBasicSalary) }
                ?.takeIf { row -> abs(row.currentBasicSalary2026 - currentBasicSalary) < 0.01 }
        }
}

class ApplyMatched2027DayRateUseCase(private val dao: PayRateSettingsDao) {
    suspend operator fun invoke(basisSalary2027: Double) {
        if (basisSalary2027 <= 0.0) return
        val current = dao.observe().first()
        val dayRate = basisSalary2027 / 30.0
        dao.upsert(
            PayRateSettingsEntity(
                id = 1,
                otRate = current?.otRate ?: 0.0,
                phRate = dayRate,
                doRate = dayRate,
                rateSource = "2027_BASIC_SALARY_DIV_30",
                basisSalary2027 = basisSalary2027,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
