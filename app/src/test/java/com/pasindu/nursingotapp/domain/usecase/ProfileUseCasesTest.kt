package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileUseCasesTest {

    @Test
    fun matchSalaryStepFindsExactCurrentBasicSalary() = runTest {
        val dao = FakeSalaryStep2027Dao(
            listOf(
                row(id = 1, current2026 = 51457.0, basic2027 = 54920.0),
                row(id = 2, current2026 = 52809.0, basic2027 = 56520.0)
            )
        )

        val result = MatchSalaryStepUseCase(dao)("III", 52809.0)

        assertEquals(2L, result?.id)
        assertEquals(56520.0, result?.basicSalary2027 ?: 0.0, 0.0)
    }

    @Test
    fun matchSalaryStepRejectsNonExactSalary() = runTest {
        val dao = FakeSalaryStep2027Dao(
            listOf(row(id = 1, current2026 = 51457.0, basic2027 = 54920.0))
        )

        assertNull(MatchSalaryStepUseCase(dao)("III", 51457.01))
    }

    @Test
    fun saveOtRatePreservesExistingPhAndDoRates() = runTest {
        val dao = FakePayRateSettingsDao(
            PayRateSettingsEntity(
                id = 1,
                otRate = 250.0,
                phRate = 1800.0,
                doRate = 1800.0,
                rateSource = "2027_BASIC_SALARY_DIV_30",
                basisSalary2027 = 54000.0,
                updatedAt = 1L
            )
        )

        SaveOtRateUseCase(dao)(283.0)

        assertEquals(283.0, dao.saved?.otRate ?: 0.0, 0.0)
        assertEquals(1800.0, dao.saved?.phRate ?: 0.0, 0.0)
        assertEquals(1800.0, dao.saved?.doRate ?: 0.0, 0.0)
        assertEquals("MANUAL", dao.saved?.rateSource)
    }

    private fun row(id: Long, current2026: Double, basic2027: Double) =
        SalaryStep2027Entity(
            id = id,
            grade = "III",
            salaryStep = id.toInt(),
            currentBasicSalary2026 = current2026,
            basicSalary2027 = basic2027,
            effectiveFrom = "2027-01-01",
            sourceLabel = "unit-test"
        )

    private class FakeSalaryStep2027Dao(
        private val rows: List<SalaryStep2027Entity>
    ) : SalaryStep2027Dao {
        override fun observeForGrade(grade: String): Flow<List<SalaryStep2027Entity>> = flowOf(rows)
        override suspend fun insertAll(items: List<SalaryStep2027Entity>) = Unit
        override suspend fun count(): Int = rows.size
        override suspend fun deleteAll() = Unit
    }

    private class FakePayRateSettingsDao(
        private var current: PayRateSettingsEntity?
    ) : PayRateSettingsDao {
        var saved: PayRateSettingsEntity? = null
        override fun observe(): Flow<PayRateSettingsEntity?> = flowOf(current)
        override suspend fun upsert(settings: PayRateSettingsEntity) {
            saved = settings
            current = settings
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private class FakeProfileDao : ProfileDao {
        override suspend fun upsert(profile: ProfileEntity) = Unit
        override fun observeProfile(): Flow<ProfileEntity?> = flowOf(null)
        override suspend fun getProfileOnce(): ProfileEntity? = null
    }

    @Suppress("UNUSED_PARAMETER")
    private class FakeProfileCompensationDao : ProfileCompensationDao {
        override suspend fun upsert(compensation: ProfileCompensationEntity) = Unit
        override fun observe(): Flow<ProfileCompensationEntity?> = flowOf(null)
    }
}
