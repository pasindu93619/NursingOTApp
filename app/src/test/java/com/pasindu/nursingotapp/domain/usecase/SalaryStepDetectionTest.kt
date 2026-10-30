package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.SalaryTable2026_2027Seed
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SalaryStepDetectionTest {

    private class FullSeedFakeSalaryDao(
        private val rows: List<SalaryStep2027Entity>
    ) : SalaryStep2027Dao {
        override fun observeAll(): Flow<List<SalaryStep2027Entity>> = flowOf(rows)

        override fun observeForGrade(grade: String): Flow<List<SalaryStep2027Entity>> =
            flowOf(rows.filter { isGradeMatch(it.grade, grade) })

        override suspend fun find(grade: String, salaryStep: Int): SalaryStep2027Entity? =
            rows.firstOrNull { isGradeMatch(it.grade, grade) && it.salaryStep == salaryStep }

        override suspend fun findByCurrentBasic(grade: String, currentBasicSalary: Double): SalaryStep2027Entity? =
            rows.firstOrNull {
                isGradeMatch(it.grade, grade) && kotlin.math.abs(it.currentBasicSalary2026 - currentBasicSalary) < 0.01
            }

        override suspend fun findByBasicSalary(currentBasicSalary: Double): List<SalaryStep2027Entity> =
            rows.filter {
                kotlin.math.abs(it.currentBasicSalary2026 - currentBasicSalary) < 0.01
            }

        override suspend fun count(): Int = rows.size
        override suspend fun clearAll() = Unit
        override suspend fun insertAll(rows: List<SalaryStep2027Entity>) = Unit

        private fun isGradeMatch(actualGrade: String, queryGrade: String): Boolean {
            if (actualGrade.equals(queryGrade, ignoreCase = true)) return true
            val normActual = normalize(actualGrade)
            val normQuery = normalize(queryGrade)
            return normActual == normQuery
        }

        private fun normalize(g: String): String = when (g.trim().uppercase()) {
            "GRADE III", "GRADE 3", "3", "III" -> "III"
            "GRADE II", "GRADE 2", "2", "II" -> "II"
            "GRADE I", "GRADE 1", "1", "I" -> "I"
            else -> g.trim().uppercase()
        }
    }

    private val seedDao = FullSeedFakeSalaryDao(SalaryTable2026_2027Seed.rows)
    private val useCase = MatchSalaryStepUseCase(seedDao)

    @Test
    fun referenceCase_gradeIII_step3_resolvesCorrectlyWithGradeIIIString() = runTest {
        val result = useCase("Grade III", 52809.0)
        assertNotNull("Should detect salary step for Grade III and Rs. 52,809", result)
        assertEquals("III", result!!.grade)
        assertEquals(3, result.salaryStep)
        assertEquals(52809.0, result.currentBasicSalary2026, 0.001)
        assertEquals(56520.0, result.basicSalary2027, 0.001)

        val dayRate = result.basicSalary2027 / 30.0
        assertEquals(1884.0, dayRate, 0.001)
    }

    @Test
    fun referenceCase_resolvesCorrectlyWithRomanNumeralIII() = runTest {
        val result = useCase("III", 52809.0)
        assertNotNull(result)
        assertEquals(3, result!!.salaryStep)
        assertEquals(56520.0, result.basicSalary2027, 0.001)
    }

    @Test
    fun referenceCase_resolvesCorrectlyWhenGradeIsBlankOrNull() = runTest {
        val resultWithBlank = useCase("", 52809.0)
        assertNotNull("Should automatically infer Grade III Step 3 from unique 2026 salary", resultWithBlank)
        assertEquals("III", resultWithBlank!!.grade)
        assertEquals(3, resultWithBlank.salaryStep)
        assertEquals(56520.0, resultWithBlank.basicSalary2027, 0.001)

        val resultWithNull = useCase(null, 52809.0)
        assertNotNull(resultWithNull)
        assertEquals(3, resultWithNull!!.salaryStep)
        assertEquals(56520.0, resultWithNull.basicSalary2027, 0.001)
    }

    @Test
    fun otherGrades_resolveCorrectlyBothWithAndWithoutGrade() = runTest {
        // Grade II Step 12: 59,219 -> 64,110
        val grade2WithGrade = useCase("Grade II", 59219.0)
        assertNotNull(grade2WithGrade)
        assertEquals("II", grade2WithGrade!!.grade)
        assertEquals(12, grade2WithGrade.salaryStep)
        assertEquals(64110.0, grade2WithGrade.basicSalary2027, 0.001)

        val grade2WithoutGrade = useCase("", 59219.0)
        assertNotNull(grade2WithoutGrade)
        assertEquals(12, grade2WithoutGrade!!.salaryStep)
        assertEquals(64110.0, grade2WithoutGrade.basicSalary2027, 0.001)

        // Grade I Step 23: 70,378 -> 77,330
        val grade1WithGrade = useCase("Grade I", 70378.0)
        assertNotNull(grade1WithGrade)
        assertEquals("I", grade1WithGrade!!.grade)
        assertEquals(23, grade1WithGrade.salaryStep)
        assertEquals(77330.0, grade1WithGrade.basicSalary2027, 0.001)

        val grade1WithoutGrade = useCase("", 70378.0)
        assertNotNull(grade1WithoutGrade)
        assertEquals(23, grade1WithoutGrade!!.salaryStep)
        assertEquals(77330.0, grade1WithoutGrade.basicSalary2027, 0.001)
    }

    @Test
    fun unknownSalary_returnsNull() = runTest {
        val unknownWithGrade = useCase("Grade III", 99999.0)
        assertNull(unknownWithGrade)

        val unknownWithoutGrade = useCase("", 99999.0)
        assertNull(unknownWithoutGrade)
    }

    @Test
    fun zeroOrNegativeSalary_returnsNull() = runTest {
        assertNull(useCase("Grade III", 0.0))
        assertNull(useCase("Grade III", -50000.0))
        assertNull(useCase("", 0.0))
    }

    @Test
    fun mismatchedGrade_returnsNull() = runTest {
        // 52,809 is Grade III, not Grade I
        val mismatched = useCase("Grade I", 52809.0)
        assertNull("Salary 52,809 does not exist in Grade I", mismatched)
    }

    @Test
    fun ambiguousSalaryWithoutGrade_returnsNullSafely() = runTest {
        // If a synthetic table had duplicates across grades
        val ambiguousRows = listOf(
            SalaryStep2027Entity(1L, "III", 1, 50000.0, 55000.0, "2027-01-01", "test"),
            SalaryStep2027Entity(2L, "II", 1, 50000.0, 56000.0, "2027-01-01", "test")
        )
        val ambiguousDao = FullSeedFakeSalaryDao(ambiguousRows)
        val ambiguousUseCase = MatchSalaryStepUseCase(ambiguousDao)

        assertNull("Ambiguous salary across multiple grades without grade specification must return null", ambiguousUseCase("", 50000.0))

        // But with grade specified, it resolves cleanly
        val resolved = ambiguousUseCase("Grade III", 50000.0)
        assertNotNull(resolved)
        assertEquals("III", resolved!!.grade)
        assertEquals(55000.0, resolved.basicSalary2027, 0.001)
    }

    @Test
    fun gradeIII_configuredOtRateRemains283() {
        val rate = NursingOtRatePolicy.rateForGrade("Grade III")
        assertEquals(283.0, rate ?: 0.0, 0.001)

        val rateRoman = NursingOtRatePolicy.rateForGrade("III")
        assertEquals(283.0, rateRoman ?: 0.0, 0.001)
    }
}
