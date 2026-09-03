package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.KnowledgeHubDao
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeHubUseCasesTest {

    @Test
    fun addCpdLogTrimsInputAndPersistsValues() = runTest {
        val dao = FakeKnowledgeHubDao()

        AddCpdLogUseCase(dao)(
            title = "  IV Therapy Workshop  ",
            earnedPoints = 4,
            institution = "  Nursing Training School  ",
            notes = "  Cannulation refresher  "
        )

        val saved = dao.insertedLog
        assertEquals("IV Therapy Workshop", saved?.seminarTitle)
        assertEquals(4, saved?.earnedPoints)
        assertEquals("Nursing Training School", saved?.speakerOrInstitution)
        assertEquals("Cannulation refresher", saved?.notes)
        assertTrue((saved?.date ?: 0L) > 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addCpdLogRejectsBlankTitle() = runTest {
        AddCpdLogUseCase(FakeKnowledgeHubDao())(
            title = "   ",
            earnedPoints = 2,
            institution = "Unit",
            notes = "Notes"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun addCpdLogRejectsNegativePoints() = runTest {
        AddCpdLogUseCase(FakeKnowledgeHubDao())(
            title = "Workshop",
            earnedPoints = -1,
            institution = "Unit",
            notes = "Notes"
        )
    }

    @Test
    fun observeCpdLogsDelegatesToDao() = runTest {
        val expected = listOf(
            CpdLogEntity(
                id = 1,
                seminarTitle = "Workshop",
                date = 100L,
                earnedPoints = 2,
                speakerOrInstitution = "Institution",
                notes = "Notes"
            )
        )
        val dao = FakeKnowledgeHubDao(expected)

        val result = ObserveCpdLogsUseCase(dao)().first()

        assertEquals(expected, result)
    }

    private class FakeKnowledgeHubDao(
        private val logs: List<CpdLogEntity> = emptyList()
    ) : KnowledgeHubDao {
        var insertedLog: CpdLogEntity? = null

        override suspend fun insertCpdLog(log: CpdLogEntity) {
            insertedLog = log
        }

        override fun getAllCpdLogs(): Flow<List<CpdLogEntity>> = flowOf(logs)
    }
}
