package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinancePolicyUseCasesTest {

    @Test
    fun ensureManualPayRateRecord_createsDefaultOnlyWhenMissing() = runBlocking {
        val dao = FakePayRateDao(null)
        EnsureManualPayRateRecordUseCase(dao)()
        assertEquals("MANUAL", dao.value.value?.rateSource)

        val existing = PayRateSettingsEntity(id = 1, otRate = 283.0, phRate = 100.0, doRate = 100.0, rateSource = "CUSTOM")
        val existingDao = FakePayRateDao(existing)
        EnsureManualPayRateRecordUseCase(existingDao)()
        assertEquals(existing, existingDao.value.value)
    }

    @Test
    fun synchronizePolicyRates_matchesCurrentBasicAndApplies2027DayRate() = runBlocking {
        val payDao = FakePayRateDao(PayRateSettingsEntity(id = 1, otRate = 283.0))
        val salary = SalaryStep2027Entity(
            grade = "III",
            salaryStep = 1,
            currentBasicSalary2026 = 50290.0,
            basicSalary2027 = 55600.0
        )
        val salaryDao = FakeSalaryDao(listOf(salary))
        val profile = ProfileEntity(
            id = 1,
            fullName = "Test Nurse",
            serviceNo = "N1",
            unit = "Ward",
            paySheetNo = "P1",
            grade = "Grade III",
            basicSalary = 50290.0,
            otRate = 283.0
        )

        SynchronizePolicyRatesUseCase(payDao, salaryDao)(profile)
        val saved = payDao.value.value
        assertEquals(283.0, saved?.otRate ?: 0.0, 0.001)
        assertEquals(55600.0 / 30.0, saved?.phRate ?: 0.0, 0.001)
        assertEquals(saved?.phRate, saved?.doRate)
        assertEquals(55600.0, saved?.basisSalary2027 ?: 0.0, 0.001)
        assertEquals("2027_BASIC_SALARY_DIV_30", saved?.rateSource)
    }

    @Test
    fun synchronizePolicyRates_doesNothingWhenSalaryStepDoesNotMatch() = runBlocking {
        val existing = PayRateSettingsEntity(id = 1, otRate = 283.0, phRate = 100.0, doRate = 100.0, rateSource = "CUSTOM")
        val payDao = FakePayRateDao(existing)
        val salaryDao = FakeSalaryDao(emptyList())
        val profile = ProfileEntity(
            id = 1,
            fullName = "Test Nurse",
            serviceNo = "N1",
            unit = "Ward",
            paySheetNo = "P1",
            grade = "III",
            basicSalary = 50290.0,
            otRate = 283.0
        )

        SynchronizePolicyRatesUseCase(payDao, salaryDao)(profile)
        assertEquals(existing, payDao.value.value)
    }

    private class FakePayRateDao(initial: PayRateSettingsEntity?) : PayRateSettingsDao {
        val value = MutableStateFlow(initial)
        override fun observe(): Flow<PayRateSettingsEntity?> = value
        override suspend fun upsert(settings: PayRateSettingsEntity) { value.value = settings }
    }

    private class FakeSalaryDao(private val rows: List<SalaryStep2027Entity>) : SalaryStep2027Dao {
        override fun observeForGrade(grade: String): Flow<List<SalaryStep2027Entity>> =
            flowOf(rows.filter { it.grade == grade })
        override suspend fun find(grade: String, salaryStep: Int): SalaryStep2027Entity? = rows.firstOrNull { it.grade == grade && it.salaryStep == salaryStep }
        override suspend fun findByCurrentBasic(grade: String, currentBasicSalary: Double): SalaryStep2027Entity? = rows.firstOrNull { it.grade == grade && kotlin.math.abs(it.currentBasicSalary2026 - currentBasicSalary) < 0.01 }
        override suspend fun count(): Int = rows.size
        override suspend fun clearAll() = Unit
        override suspend fun insertAll(rows: List<SalaryStep2027Entity>) = Unit
    }
}
