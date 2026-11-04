package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.SalaryTable2026_2027Seed
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import com.pasindu.nursingotapp.logic.CalculationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PayRateResolverTest {

    private fun seedRows(): List<SalaryStep2027Entity> =
        SalaryTable2026_2027Seed.rows

    @Test
    fun gradeIiiStep3_gives56520DividedBy30Equals1884ForPhAndDo() = runTest {
        val salaryDao = FakeSalaryStep2027Dao(seedRows())
        val payDao = FakePayRateSettingsDao(
            PayRateSettingsEntity(
                id = 1,
                otRate = 425.0,
                phRate = 0.0,
                doRate = 0.0,
                rateSource = "MANUAL",
                basisSalary2027 = null
            )
        )
        val resolver = PayRateResolver(
            matchSalaryStep = MatchSalaryStepUseCase(salaryDao),
            applyDayRate = ApplyMatched2027DayRateUseCase(payDao)
        )

        // Grade III Step 3: 2026 basic is 52809.0, 2027 basic is 56520.0
        val profile = ProfileEntity(
            id = 1,
            fullName = "Grade III Nurse",
            serviceNo = "N123",
            unit = "ICU",
            paySheetNo = "P456",
            grade = "Grade III",
            basicSalary = 52809.0,
            otRate = 425.0,
            updatedAt = 0L
        )

        val rates = resolver.resolve(profile)
        assertNotNull(rates)
        assertEquals(56520.0, rates!!.basis2027, 0.001)
        assertEquals(1884.0, rates.phRate, 0.001)
        assertEquals(1884.0, rates.doRate, 0.001)

        // Persisted settings are updated with the 2027 day rate
        assertEquals(1884.0, payDao.saved?.phRate ?: 0.0, 0.001)
        assertEquals(1884.0, payDao.saved?.doRate ?: 0.0, 0.001)
        assertEquals(56520.0, payDao.saved?.basisSalary2027 ?: 0.0, 0.001)
        assertEquals("2027_BASIC_SALARY_DIV_30", payDao.saved?.rateSource)
    }

    @Test
    fun missingSalaryRow_returnsNull() = runTest {
        val salaryDao = FakeSalaryStep2027Dao(seedRows())
        val payDao = FakePayRateSettingsDao(null)
        val resolver = PayRateResolver(
            matchSalaryStep = MatchSalaryStepUseCase(salaryDao),
            applyDayRate = ApplyMatched2027DayRateUseCase(payDao)
        )

        val profile = ProfileEntity(
            id = 1,
            fullName = "Unmatched Nurse",
            serviceNo = "N999",
            unit = "Ward",
            paySheetNo = "P999",
            grade = "Grade III",
            basicSalary = 99999.0, // Non-existent salary in table
            otRate = 400.0,
            updatedAt = 0L
        )

        val rates = resolver.resolve(profile)
        assertNull(rates)
        // Ensure no settings were saved
        assertNull(payDao.saved)
    }

    @Test
    fun userConfiguredOtRate_isPreservedWhenDayRatesAreUpdated() = runTest {
        val salaryDao = FakeSalaryStep2027Dao(seedRows())
        val existingOtRate = 485.50
        val payDao = FakePayRateSettingsDao(
            PayRateSettingsEntity(
                id = 1,
                otRate = existingOtRate,
                phRate = 1200.0,
                doRate = 1200.0,
                rateSource = "MANUAL",
                basisSalary2027 = null
            )
        )
        val resolver = PayRateResolver(
            matchSalaryStep = MatchSalaryStepUseCase(salaryDao),
            applyDayRate = ApplyMatched2027DayRateUseCase(payDao)
        )

        val profile = ProfileEntity(
            id = 1,
            fullName = "Nurse with custom OT",
            serviceNo = "N100",
            unit = "ER",
            paySheetNo = "P100",
            grade = "Grade II",
            basicSalary = 59219.0, // Grade II Step 12: 2027 basic = 64110.0
            otRate = existingOtRate,
            updatedAt = 0L
        )

        val rates = resolver.resolve(profile)
        assertNotNull(rates)
        val expectedDayRate = 64110.0 / 30.0 // 2137.0
        assertEquals(expectedDayRate, rates!!.phRate, 0.001)
        assertEquals(expectedDayRate, rates.doRate, 0.001)

        // OT rate remains untouched
        assertEquals(existingOtRate, payDao.saved?.otRate ?: 0.0, 0.001)
    }

    @Test
    fun pdfFallbackTo2026Basic_isNeverUsed() = runTest {
        val salaryDao = FakeSalaryStep2027Dao(seedRows())
        val payDao = FakePayRateSettingsDao(null)
        val resolver = PayRateResolver(
            matchSalaryStep = MatchSalaryStepUseCase(salaryDao),
            applyDayRate = ApplyMatched2027DayRateUseCase(payDao)
        )

        // Unmatched profile
        val unmatchedProfile = ProfileEntity(
            id = 1,
            fullName = "Unmatched Nurse",
            serviceNo = "N999",
            unit = "Ward",
            paySheetNo = "P999",
            grade = "Grade III",
            basicSalary = 52800.0, // Off by 9 rupees from 52809.0
            otRate = 400.0,
            updatedAt = 0L
        )

        val rates = resolver.resolve(unmatchedProfile)
        // With old logic, it would have used 52800.0 / 30.0 = 1760.0.
        // With PayRateResolver, it returns null so the caller aborts PDF generation.
        assertNull(rates)

        // For matched profile: Grade III Step 3 (52809.0)
        val matchedProfile = unmatchedProfile.copy(basicSalary = 52809.0)
        val matchedRates = resolver.resolve(matchedProfile)
        assertNotNull(matchedRates)
        // Verify it is NOT 52809.0 / 30.0 (~1760.30)
        val incorrect2026DayRate = 52809.0 / 30.0
        assertTrue(kotlin.math.abs(matchedRates!!.phRate - incorrect2026DayRate) > 100.0)
        // It MUST be 56520.0 / 30.0 = 1884.0
        assertEquals(1884.0, matchedRates.phRate, 0.001)
    }

    @Test
    fun calculationEngine_exposesRatesMissingWhenRatesAreNullOrZero() {
        val profile = ProfileEntity(
            id = 1,
            fullName = "Nurse",
            serviceNo = "N1",
            unit = "Unit",
            paySheetNo = "P1",
            grade = "Grade III",
            basicSalary = 52809.0,
            otRate = 350.0,
            updatedAt = 0L
        )

        // 1. PayRates = null -> ratesMissing = true
        val (_, summaryNull) = CalculationEngine.processClaimData(
            profileEntity = profile,
            entries = emptyList(),
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30),
            payRates = null
        )
        assertTrue(summaryNull.ratesMissing)

        // 2. PayRates with phRate = null or doRate = null -> ratesMissing = true
        val (_, summaryPartial) = CalculationEngine.processClaimData(
            profileEntity = profile,
            entries = emptyList(),
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30),
            payRates = CalculationEngine.PayRates(otRate = 350.0, phRate = 1884.0, doRate = null)
        )
        assertTrue(summaryPartial.ratesMissing)

        // 3. PayRates with valid phRate and doRate -> ratesMissing = false
        val (_, summaryValid) = CalculationEngine.processClaimData(
            profileEntity = profile,
            entries = emptyList(),
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30),
            payRates = CalculationEngine.PayRates(otRate = 350.0, phRate = 1884.0, doRate = 1884.0)
        )
        assertFalse(summaryValid.ratesMissing)
    }

    private class FakeSalaryStep2027Dao(
        private val rows: List<SalaryStep2027Entity>
    ) : SalaryStep2027Dao {
        override fun observeAll(): Flow<List<SalaryStep2027Entity>> = flowOf(rows)
        override fun observeForGrade(grade: String): Flow<List<SalaryStep2027Entity>> = flowOf(rows)
        override suspend fun find(grade: String, salaryStep: Int): SalaryStep2027Entity? = null
        override suspend fun findByCurrentBasic(grade: String, currentBasicSalary: Double): SalaryStep2027Entity? {
            val normalized = grade.removePrefix("Grade ").trim()
            return rows.firstOrNull {
                it.grade.removePrefix("Grade ").trim().equals(normalized, ignoreCase = true) &&
                        kotlin.math.abs(it.currentBasicSalary2026 - currentBasicSalary) < 0.01
            }
        }
        override suspend fun findByBasicSalary(currentBasicSalary: Double): List<SalaryStep2027Entity> =
            rows.filter { kotlin.math.abs(it.currentBasicSalary2026 - currentBasicSalary) < 0.01 }
        override suspend fun count(): Int = rows.size
        override suspend fun clearAll() = Unit
        override suspend fun insertAll(rows: List<SalaryStep2027Entity>) = Unit
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
}
