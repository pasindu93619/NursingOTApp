package com.pasindu.nursingotapp.data.local

import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import com.pasindu.nursingotapp.domain.usecase.MatchSalaryStepUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SalaryTableSeederTest {

    @Before
    @After
    fun cleanup() {
        SalaryTableSeeder.setSeedingDeferredForTest(null)
    }

    @Test
    fun emptyTable_isFilledWithCompleteSalaryDataset() = runTest {
        val fakeDao = TransactionAwareFakeSalaryStepDao(emptyList())
        var transactionCount = 0

        val repaired = SalaryTableSeeder.seedIfNeededInternal(
            dao = fakeDao,
            runInTransaction = { block ->
                transactionCount++
                fakeDao.runInTransaction(block)
            }
        )

        assertTrue("Empty table should trigger seeding repair", repaired)
        assertEquals(1, transactionCount)
        assertEquals(SalaryTable2026_2027Seed.rows.size, fakeDao.currentRows.size)

        // Verify Grade III Step 3
        val grade3Step3 = fakeDao.findByCurrentBasic("III", 52809.0)
        assertNotNull(grade3Step3)
        assertEquals(56520.0, grade3Step3!!.basicSalary2027, 0.001)

        // Verify Grade II Step 12
        val grade2Step12 = fakeDao.findByCurrentBasic("II", 59219.0)
        assertNotNull(grade2Step12)
        assertEquals(64110.0, grade2Step12!!.basicSalary2027, 0.001)

        // Verify Grade I Step 23
        val grade1Step23 = fakeDao.findByCurrentBasic("I", 70378.0)
        assertNotNull(grade1Step23)
        assertEquals(77330.0, grade1Step23!!.basicSalary2027, 0.001)
    }

    @Test
    fun runningSeedMoreThanOnce_isIdempotentAndDoesNotAlterTable() = runTest {
        val fakeDao = TransactionAwareFakeSalaryStepDao(SalaryTable2026_2027Seed.rows)
        var transactionCount = 0

        val repaired = SalaryTableSeeder.seedIfNeededInternal(
            dao = fakeDao,
            runInTransaction = { block ->
                transactionCount++
                fakeDao.runInTransaction(block)
            }
        )

        assertFalse("Correct table should not be modified", repaired)
        assertEquals(0, transactionCount)
        assertEquals(SalaryTable2026_2027Seed.rows.size, fakeDao.currentRows.size)
    }

    @Test
    fun staleOrIncompleteTable_isRepairedInOneStep() = runTest {
        val staleRow = SalaryStep2027Entity(
            id = 1L,
            grade = "III",
            salaryStep = 1,
            currentBasicSalary2026 = 50000.0, // Stale/corrupt value
            basicSalary2027 = 53000.0,
            effectiveFrom = "2027-01-01",
            sourceLabel = "stale"
        )
        val fakeDao = TransactionAwareFakeSalaryStepDao(listOf(staleRow))
        var transactionCount = 0

        val repaired = SalaryTableSeeder.seedIfNeededInternal(
            dao = fakeDao,
            runInTransaction = { block ->
                transactionCount++
                fakeDao.runInTransaction(block)
            }
        )

        assertTrue("Stale table should be repaired", repaired)
        assertEquals(1, transactionCount)
        assertEquals(SalaryTable2026_2027Seed.rows.size, fakeDao.currentRows.size)
        // Ensure stale row is gone and correct Step 1 is in place
        val correctStep1 = fakeDao.findByCurrentBasic("III", 51457.0)
        assertNotNull(correctStep1)
        assertEquals(54920.0, correctStep1!!.basicSalary2027, 0.001)
    }

    @Test
    fun failureDuringSeeding_rollsBackOperationAtomically() = runTest {
        val initialRow = SalaryStep2027Entity(
            id = 99L,
            grade = "III",
            salaryStep = 1,
            currentBasicSalary2026 = 51457.0,
            basicSalary2027 = 54920.0,
            effectiveFrom = "2027-01-01",
            sourceLabel = "existing"
        )
        val fakeDao = TransactionAwareFakeSalaryStepDao(listOf(initialRow))
        fakeDao.failOnInsert = true // Simulate disk failure or DB constraint crash mid-insert

        var errorThrown = false
        try {
            SalaryTableSeeder.seedIfNeededInternal(
                dao = fakeDao,
                runInTransaction = { block -> fakeDao.runInTransaction(block) }
            )
        } catch (e: IllegalStateException) {
            errorThrown = true
        }

        assertTrue("Transaction should have failed with exception", errorThrown)
        // Verify rollback: initial state must be preserved, table must NOT be left empty!
        assertEquals("Table must retain initial rows after transaction rollback", 1, fakeDao.currentRows.size)
        assertEquals(initialRow, fakeDao.currentRows[0])
    }

    @Test
    fun matchSalaryStep_awaitsInFlightStartupSeeding() = runTest {
        val deferred = CompletableDeferred<Unit>()
        SalaryTableSeeder.setSeedingDeferredForTest(deferred)

        val fakeDao = TransactionAwareFakeSalaryStepDao(SalaryTable2026_2027Seed.rows)
        val useCase = MatchSalaryStepUseCase(fakeDao)

        // Launch lookup in coroutine
        val lookupJob = async {
            useCase("III", 52809.0)
        }

        // Deferred is not yet completed, lookup should not be finished
        assertFalse(lookupJob.isCompleted)

        // Complete the seeding
        deferred.complete(Unit)

        val result = lookupJob.await()
        assertNotNull(result)
        assertEquals(56520.0, result!!.basicSalary2027, 0.001)
    }

    private class TransactionAwareFakeSalaryStepDao(
        initialRows: List<SalaryStep2027Entity>
    ) : SalaryStep2027Dao {
        val currentRows = initialRows.toMutableList()
        private val _flow = MutableStateFlow(currentRows.toList())
        var failOnInsert = false

        suspend fun runInTransaction(block: suspend () -> Unit) {
            val snapshot = currentRows.toList()
            try {
                block()
            } catch (t: Throwable) {
                currentRows.clear()
                currentRows.addAll(snapshot)
                _flow.value = currentRows.toList()
                throw t
            }
        }

        override fun observeAll(): Flow<List<SalaryStep2027Entity>> = _flow.asStateFlow()

        override fun observeForGrade(grade: String): Flow<List<SalaryStep2027Entity>> = _flow.asStateFlow()

        override suspend fun find(grade: String, salaryStep: Int): SalaryStep2027Entity? =
            currentRows.firstOrNull { it.grade == grade && it.salaryStep == salaryStep }

        override suspend fun findByCurrentBasic(grade: String, currentBasicSalary: Double): SalaryStep2027Entity? {
            val normGrade = when (grade.trim().uppercase()) {
                "GRADE III", "GRADE 3", "3", "III" -> "III"
                "GRADE II", "GRADE 2", "2", "II" -> "II"
                "GRADE I", "GRADE 1", "1", "I" -> "I"
                else -> grade.trim()
            }
            return currentRows.firstOrNull {
                (it.grade == grade || it.grade == normGrade) && kotlin.math.abs(it.currentBasicSalary2026 - currentBasicSalary) < 0.01
            }
        }

        override suspend fun findByBasicSalary(currentBasicSalary: Double): List<SalaryStep2027Entity> =
            currentRows.filter {
                kotlin.math.abs(it.currentBasicSalary2026 - currentBasicSalary) < 0.01
            }

        override suspend fun count(): Int = currentRows.size

        override suspend fun clearAll() {
            currentRows.clear()
            _flow.value = currentRows.toList()
        }

        override suspend fun insertAll(rows: List<SalaryStep2027Entity>) {
            if (failOnInsert) {
                throw IllegalStateException("Simulated disk error during insertAll")
            }
            currentRows.addAll(rows)
            _flow.value = currentRows.toList()
        }
    }
}
