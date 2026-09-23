package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FinancePolicyUseCasesTest {

    @Test
    fun ensureManualPayRateRecord_createsDefaultOnlyWhenMissing() = runBlocking {
        val dao = FakePayRateDao(null)
        EnsureManualPayRateRecordUseCase(dao)()
        assertNotNull(dao.value.value)
        assertEquals("MANUAL", dao.value.value?.rateSource)
        assertEquals(0.0, dao.value.value?.otRate ?: -1.0, 0.0)
    }

    @Test
    fun applyFinancePolicyRates_gradeIIIStep3Resolves2027BasicAndUses884DayRate() = runBlocking {
        val payDao = FakePayRateDao(
            PayRateSettingsEntity(
                id = 1,
                otRate = 999.0,
                phRate = 0.0,
                doRate = 0.0,
                rateSource = "MANUAL",
                basisSalary2027 = null,
                updatedAt = 1L
            )
        )
        val salary = SalaryStep2027Entity(
            grade = "Grade III",
            salaryStep = 3,
            currentBasicSalary2026 = 52809.0,
            basicSalary2027 = 56520.0
        )
        val salaryDao = FakeSalaryDao(listOf(salary))

        val matched = MatchSalaryStepUseCase(salaryDao)(testProfile())
        assertNotNull(matched)
        ApplyFinancePolicyRatesUseCase(
            MatchSalaryStepUseCase(salaryDao),
            ApplyMatched2027DayRateUseCase(payDao)
        )(testProfile())

        val saved = payDao.value.value
        assertEquals(3, matched?.salaryStep)
        assertEquals(52809.0, matched?.currentBasicSalary2026 ?: 0.0, 0.001)
        assertEquals(56520.0, saved?.basisSalary2027 ?: 0.0, 0.001)
        assertEquals(1884.0, saved?.phRate ?: 0.0, 0.001)
        assertEquals(1884.0, saved?.doRate ?: 0.0, 0.001)
        assertEquals("2027_BASIC_SALARY_DIV_30", saved?.rateSource)
        assertEquals(999.0, saved?.otRate ?: 0.0, 0.001)
    }

    private fun testProfile() = ProfileEntity(
        id = 1,
        fullName = "Test Nurse",
        serviceNo = "N1",
        unit = "Ward",
        paySheetNo = "P1",
        grade = "Grade III",
        basicSalary = 52809.0,
        otRate = 283.0,
        updatedAt = 1L,
        salaryStep = 3
    )

    private class FakePayRateDao(initial: PayRateSettingsEntity?) : PayRateSettingsDao {
        val value = MutableStateFlow(initial)
        override fun observe(): Flow<PayRateSettingsEntity?> = value
        override suspend fun upsert(settings: PayRateSettingsEntity) {
            value.value = settings
        }
    }

    private class FakeSalaryDao(private val rows: List<SalaryStep2027Entity>) : SalaryStep2027Dao {
        override fun observeAll(): Flow<List<SalaryStep2027Entity>> = flowOf(rows)
        override fun observeForGrade(grade: String): Flow<List<SalaryStep2027Entity>> =
            flowOf(rows.filter { it.grade == grade })

        override suspend fun find(grade: String, salaryStep: Int): SalaryStep2027Entity? =
            rows.firstOrNull { it.grade == grade && it.salaryStep == salaryStep }

        override suspend fun findByCurrentBasic(grade: String, currentBasicSalary: Double): SalaryStep2027Entity? =
            rows.firstOrNull { it.grade == grade && kotlin.math.abs(it.currentBasicSalary2026 - currentBasicSalary) < 0.01 }

        override suspend fun count(): Int = rows.size
        override suspend fun clearAll() = Unit
        override suspend fun insertAll(rows: List<SalaryStep2027Entity>) = Unit
    }
}
