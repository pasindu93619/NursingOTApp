package com.pasindu.nursingotapp.ui

import com.pasindu.nursingotapp.data.local.dao.KnowledgeHubDao
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity
import com.pasindu.nursingotapp.domain.model.EvidenceAccessClass
import com.pasindu.nursingotapp.domain.usecase.AddCpdLogUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveCpdLogsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgeHubSearchAndReaderTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakeKnowledgeHubDao
    private lateinit var fakeBookmarkStore: FakeBookmarkStore
    private lateinit var viewModel: KnowledgeHubViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeKnowledgeHubDao()
        fakeBookmarkStore = FakeBookmarkStore()
        viewModel = KnowledgeHubViewModel(
            observeCpdLogsUseCase = ObserveCpdLogsUseCase(fakeDao),
            addCpdLogUseCase = AddCpdLogUseCase(fakeDao),
            bookmarkStore = fakeBookmarkStore
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun curatedEvidenceArticles_areLoadedOnInitialization() {
        val articles = viewModel.evidenceArticles.value
        assertTrue("Evidence articles should not be empty", articles.isNotEmpty())
        assertEquals(7, articles.size)

        val dengue = articles.firstOrNull { it.id == "WHO-DENGUE-2024" }
        assertNotNull(dengue)
        assertEquals(EvidenceAccessClass.CLASS_A_FULL_TEXT, dengue?.accessClass)
        assertTrue(dengue!!.keyRecommendations.isNotEmpty())
        assertTrue(dengue.safetyWarnings.isNotEmpty())
        assertTrue(dengue.bedsideActions.isNotEmpty())
    }

    @Test
    fun searchFiltering_matchesTitleSummaryAndBedsideActions() {
        val articles = viewModel.evidenceArticles.value

        // Search by title term
        val dengueMatches = articles.filter { it.title.contains("Dengue", ignoreCase = true) }
        assertEquals(1, dengueMatches.size)
        assertEquals("WHO-DENGUE-2024", dengueMatches.first().id)

        // Search by bedside clinical action term
        val lactateMatches = articles.filter { article ->
            article.summary.contains("lactate", ignoreCase = true) ||
                    article.keyRecommendations.any { it.contains("lactate", ignoreCase = true) }
        }
        assertEquals(1, lactateMatches.size)
        assertEquals("EPMC-SEPSIS-2024", lactateMatches.first().id)

        // Search by medication keyword
        val potassiumMatches = articles.filter { article ->
            article.title.contains("Potassium", ignoreCase = true) ||
                    article.summary.contains("potassium", ignoreCase = true) ||
                    article.keyRecommendations.any { it.contains("Potassium", ignoreCase = true) }
        }
        assertEquals(1, potassiumMatches.size)
        assertEquals("MOH-INFUSION-2025", potassiumMatches.first().id)
    }

    @Test
    fun categoryFiltering_isolatesSpecificClinicalDomains() {
        val articles = viewModel.evidenceArticles.value

        val criticalCare = articles.filter { it.category.equals("Critical Care", ignoreCase = true) }
        assertEquals(1, criticalCare.size)
        assertEquals("EPMC-SEPSIS-2024", criticalCare.first().id)

        val circulars = articles.filter { it.category.equals("Ministry Circulars", ignoreCase = true) }
        assertEquals(3, circulars.size)

        val medicationSafety = articles.filter { it.category.equals("Medication Safety", ignoreCase = true) }
        assertEquals(1, medicationSafety.size)
        assertEquals("MOH-INFUSION-2025", medicationSafety.first().id)
    }

    @Test
    fun openAndCloseArticleReader_managesSelectedArticleState() {
        assertNull(viewModel.selectedArticleForReading.value)

        val article = viewModel.evidenceArticles.value.first()
        viewModel.openArticleReader(article)
        assertEquals(article, viewModel.selectedArticleForReading.value)

        viewModel.closeArticleReader()
        assertNull(viewModel.selectedArticleForReading.value)
    }

    @Test
    fun bookmarkToggle_updatesBookmarkStoreAndSavedArticlesFlow() = runTest {
        advanceUntilIdle()
        val targetId = "WHO-DENGUE-2024"
        assertFalse(targetId in viewModel.bookmarkedCircularIds.value)
        assertTrue(viewModel.savedArticles.value.none { it.id == targetId })

        // Toggle bookmark ON
        viewModel.toggleCircularBookmark(targetId)
        advanceUntilIdle()
        assertTrue(targetId in viewModel.bookmarkedCircularIds.value)
        assertTrue(fakeBookmarkStore.getBookmarkedIds().contains(targetId))
        assertTrue(viewModel.savedArticles.value.any { it.id == targetId })

        // Toggle bookmark OFF
        viewModel.toggleCircularBookmark(targetId)
        advanceUntilIdle()
        assertFalse(targetId in viewModel.bookmarkedCircularIds.value)
        assertFalse(fakeBookmarkStore.getBookmarkedIds().contains(targetId))
        assertTrue(viewModel.savedArticles.value.none { it.id == targetId })
    }

    @Test
    fun preSeededSavedArticles_reflectInitialBookmarkStoreState() = runTest {
        val preSeededStore = FakeBookmarkStore(setOf("RNAO-PRESSURE-2023", "MOH-2026-08"))
        val preSeededVm = KnowledgeHubViewModel(
            observeCpdLogsUseCase = ObserveCpdLogsUseCase(fakeDao),
            addCpdLogUseCase = AddCpdLogUseCase(fakeDao),
            bookmarkStore = preSeededStore
        )
        advanceUntilIdle()

        assertEquals(2, preSeededVm.savedArticles.value.size)
        val savedIds = preSeededVm.savedArticles.value.map { it.id }.toSet()
        assertTrue(savedIds.contains("RNAO-PRESSURE-2023"))
        assertTrue(savedIds.contains("MOH-2026-08"))
    }

    @Test
    fun simulatedMinistryCirculars_areExclusivelyAndAccuratelyClassified() {
        val articles = viewModel.evidenceArticles.value
        val simulatedArticles = articles.filter { it.isSimulationOrUnverified }

        // 1. Verify exactly the four specified Ministry articles are classified as simulated
        val expectedSimulatedIds = setOf(
            "MOH-INFUSION-2025",
            "MOH-2026-04",
            "MOH-2026-08",
            "MOH-2026-11"
        )
        assertEquals(expectedSimulatedIds, simulatedArticles.map { it.id }.toSet())
        assertEquals(4, simulatedArticles.size)

        // 2. Verify all simulated articles have UNVERIFIED grade and non-empty disclaimer
        simulatedArticles.forEach { article ->
            assertEquals("UNVERIFIED", article.evidenceGrade)
            assertNotNull("Disclaimer should not be null for ${article.id}", article.sourceUrlDisclaimer)
            assertTrue("Disclaimer should not be empty for ${article.id}", article.sourceUrlDisclaimer!!.isNotBlank())
            assertTrue(
                "License notice should indicate simulation for ${article.id}",
                article.licenseNotice?.contains("Simulation", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun verifiedArticles_retainGradeAAndAreNotClassifiedAsSimulation() {
        val articles = viewModel.evidenceArticles.value
        val verifiedIds = setOf("WHO-DENGUE-2024", "EPMC-SEPSIS-2024", "RNAO-PRESSURE-2023")
        val verifiedArticles = articles.filter { it.id in verifiedIds }

        assertEquals(3, verifiedArticles.size)
        verifiedArticles.forEach { article ->
            assertEquals("GRADE_A", article.evidenceGrade)
            assertNull("Verified article ${article.id} must not have a disclaimer", article.sourceUrlDisclaimer)
            assertFalse("Verified article ${article.id} must not be marked simulation", article.isSimulationOrUnverified)
        }
    }

    @Test
    fun articleItem_defaultMetadata_doesNotClassifyContentAsSimulationAutomatically() {
        // Constructing an article without explicit unverified/disclaimer metadata must NOT default to simulation
        val defaultArticle = com.pasindu.nursingotapp.domain.model.EvidenceArticleItem(
            id = "CUSTOM-MOH-2026",
            title = "Official Ministry Guideline",
            source = com.pasindu.nursingotapp.domain.model.EvidenceSourceInstitution.SRI_LANKA_MOH,
            documentType = com.pasindu.nursingotapp.domain.model.EvidenceDocumentType.GOVERNMENT_CIRCULAR,
            category = "Ministry Circulars",
            publicationDate = "2026-01-01",
            lastVerifiedDate = "2026-01-01",
            summary = "Verified official circular",
            fullTextOrAbstract = "Full text content",
            canonicalUrl = "https://www.health.gov.lk"
        )

        assertNull(defaultArticle.evidenceGrade)
        assertNull(defaultArticle.sourceUrlDisclaimer)
        assertFalse(
            "Ministry source without UNVERIFIED grade or disclaimer must not be classified as simulation",
            defaultArticle.isSimulationOrUnverified
        )
    }

    @Test
    fun existingArticleIdsAndTotalCount_remainStrictlyPreserved() {
        val articles = viewModel.evidenceArticles.value
        assertEquals(7, articles.size)
        val expectedIds = setOf(
            "WHO-DENGUE-2024",
            "EPMC-SEPSIS-2024",
            "RNAO-PRESSURE-2023",
            "MOH-INFUSION-2025",
            "MOH-2026-04",
            "MOH-2026-08",
            "MOH-2026-11"
        )
        assertEquals(expectedIds, articles.map { it.id }.toSet())
    }

    private class FakeKnowledgeHubDao : KnowledgeHubDao {
        override suspend fun insertCpdLog(log: CpdLogEntity) {}
        override fun getAllCpdLogs(): Flow<List<CpdLogEntity>> = flowOf(emptyList())
    }

    private class FakeBookmarkStore(initial: Set<String> = emptySet()) : BookmarkStore {
        private val ids = initial.toMutableSet()
        override fun getBookmarkedIds(): Set<String> = ids.toSet()
        override fun setBookmarked(id: String, bookmarked: Boolean) {
            if (bookmarked) {
                ids.add(id)
            } else {
                ids.remove(id)
            }
        }
    }
}
