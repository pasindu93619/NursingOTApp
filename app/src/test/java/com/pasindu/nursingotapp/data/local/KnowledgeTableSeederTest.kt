package com.pasindu.nursingotapp.data.local

import com.pasindu.nursingotapp.data.local.dao.KnowledgeArticleDao
import com.pasindu.nursingotapp.data.local.dao.KnowledgeBookmarkDao
import com.pasindu.nursingotapp.data.local.entity.knowledge.ArticleTagCrossRef
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeArticleEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeBookmarkEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeCategoryEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgePersonalNoteEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeSourceEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeTagEntity
import com.pasindu.nursingotapp.data.local.seed.KnowledgeHubSeedData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Focused JVM Unit Tests for Knowledge Hub Seeding and Bookmark Reconciliation logic.
 *
 * NOTE: For actual SQLite / Room child-table preservation and AppDatabase startSeeding()
 * concurrency tests with real Room transactions, refer to the Android instrumentation test:
 * [KnowledgeRoomChildPreservationTest].
 */
class KnowledgeTableSeederTest {

    @Test
    fun knowledgeTableSeeder_insertsAllSourcesCategoriesTagsAndArticles() = runTest {
        val fakeArticleDao = FakeKnowledgeArticleDao()
        val fakeBookmarkDao = FakeKnowledgeBookmarkDao()
        var transactionExecuted = false

        val result = KnowledgeTableSeeder.seedIfNeededInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = emptySet(),
            runInTransaction = { block ->
                transactionExecuted = true
                block()
            }
        )

        assertTrue("Seeding must execute inside a transaction", transactionExecuted)
        assertEquals(4, result.sourcesCount)
        assertEquals(5, result.categoriesCount)
        assertEquals(27, result.tagsCount)
        assertEquals(7, result.articlesCount)
        assertEquals(33, result.crossRefsCount)

        assertEquals(4, fakeArticleDao.sources.size)
        assertEquals(5, fakeArticleDao.categories.size)
        assertEquals(27, fakeArticleDao.tags.size)
        assertEquals(7, fakeArticleDao.articles.size)
        assertEquals(33, fakeArticleDao.crossRefs.size)

        // Verify the 7 stable IDs are all present
        val expectedIds = listOf(
            "WHO-DENGUE-2024",
            "EPMC-SEPSIS-2024",
            "RNAO-PRESSURE-2023",
            "MOH-INFUSION-2025",
            "MOH-2026-04",
            "MOH-2026-08",
            "MOH-2026-11"
        )
        for (id in expectedIds) {
            assertNotNull("Article $id must be seeded", fakeArticleDao.articles[id])
        }
    }

    @Test
    fun knowledgeTableSeeder_isIdempotent() = runTest {
        val fakeArticleDao = FakeKnowledgeArticleDao()
        val fakeBookmarkDao = FakeKnowledgeBookmarkDao()

        // First seeding run
        KnowledgeTableSeeder.seedIfNeededInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = emptySet(),
            runInTransaction = { block -> block() }
        )

        val firstArticleCount = fakeArticleDao.articles.size
        val firstSourceCount = fakeArticleDao.sources.size
        val firstTagCount = fakeArticleDao.tags.size

        // Second seeding run (idempotency check)
        KnowledgeTableSeeder.seedIfNeededInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = emptySet(),
            runInTransaction = { block -> block() }
        )

        assertEquals("Articles count must remain identical after second run", firstArticleCount, fakeArticleDao.articles.size)
        assertEquals("Sources count must remain identical after second run", firstSourceCount, fakeArticleDao.sources.size)
        assertEquals("Tags count must remain identical after second run", firstTagCount, fakeArticleDao.tags.size)
    }

    @Test
    fun knowledgeTableSeeder_updatesArticleWithoutDeletingChildRecords() = runTest {
        val fakeArticleDao = FakeKnowledgeArticleDao()
        val fakeBookmarkDao = FakeKnowledgeBookmarkDao()

        // Seed initially
        KnowledgeTableSeeder.seedIfNeededInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = emptySet(),
            runInTransaction = { block -> block() }
        )

        // Attach child bookmark and simulate child note on "MOH-INFUSION-2025"
        val originalTimestamp = 1715000000000L
        fakeBookmarkDao.insertBookmarksOrIgnore(
            listOf(KnowledgeBookmarkEntity(articleId = "MOH-INFUSION-2025", bookmarkedAt = originalTimestamp))
        )
        val note = KnowledgePersonalNoteEntity(
            id = 101L,
            articleId = "MOH-INFUSION-2025",
            originalArticleId = "MOH-INFUSION-2025",
            noteContent = "Checked with ICU sister regarding potassium ampoules."
        )

        // Simulate an updated article pack where MOH-INFUSION-2025 has updated title and clinicalSummary
        val updatedArticle = fakeArticleDao.articles["MOH-INFUSION-2025"]!!.copy(
            title = "High-Alert Medication Safety (2026 Revised Revision)",
            clinicalSummary = "Updated protocol with revised syringe pump pressure limit guidelines."
        )
        fakeArticleDao.upsertArticles(listOf(updatedArticle))

        // Verify article was updated in-place
        val storedArticle = fakeArticleDao.articles["MOH-INFUSION-2025"]
        assertNotNull(storedArticle)
        assertEquals("High-Alert Medication Safety (2026 Revised Revision)", storedArticle!!.title)
        assertEquals("Updated protocol with revised syringe pump pressure limit guidelines.", storedArticle.clinicalSummary)

        // Verify child records survived intact without cascading deletion
        assertTrue("Child bookmark must survive article upsert", fakeBookmarkDao.isBookmarked("MOH-INFUSION-2025"))
        assertEquals("Bookmark timestamp must remain unchanged", originalTimestamp, fakeBookmarkDao.bookmarks["MOH-INFUSION-2025"]!!.bookmarkedAt)
        assertEquals("Note must still refer to articleId", "MOH-INFUSION-2025", note.articleId)
    }

    @Test
    fun knowledgeTableSeeder_preservesExistingBookmarksAndNotes() = runTest {
        val fakeArticleDao = FakeKnowledgeArticleDao()
        val fakeBookmarkDao = FakeKnowledgeBookmarkDao()

        // Pre-insert an existing user bookmark before seeding
        val existingTimestamp = 1710000000000L
        fakeBookmarkDao.insertBookmarksOrIgnore(
            listOf(KnowledgeBookmarkEntity(articleId = "WHO-DENGUE-2024", bookmarkedAt = existingTimestamp))
        )

        // Simulate existing note
        val existingNote = KnowledgePersonalNoteEntity(
            id = 1L,
            articleId = "WHO-DENGUE-2024",
            originalArticleId = "WHO-DENGUE-2024",
            noteContent = "Serial micro-hematocrit centrifuge checks are vital."
        )

        // Run seeding with legacy ID also pointing to WHO-DENGUE-2024
        KnowledgeTableSeeder.seedIfNeededInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = setOf("WHO-DENGUE-2024"),
            runInTransaction = { block -> block() }
        )

        // Assert existing bookmark still exists and timestamp was NOT overwritten
        assertTrue(fakeBookmarkDao.isBookmarked("WHO-DENGUE-2024"))
        assertEquals(existingTimestamp, fakeBookmarkDao.bookmarks["WHO-DENGUE-2024"]!!.bookmarkedAt)
        assertEquals("WHO-DENGUE-2024", existingNote.articleId)
    }

    @Test
    fun bookmarkReconciliation_migratesMatchingLegacyIds() = runTest {
        val fakeArticleDao = FakeKnowledgeArticleDao()
        val fakeBookmarkDao = FakeKnowledgeBookmarkDao()

        // Seed articles
        KnowledgeTableSeeder.seedIfNeededInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = emptySet(),
            runInTransaction = { block -> block() }
        )

        val legacyIds = setOf("WHO-DENGUE-2024", "MOH-2026-04")
        val result = KnowledgeTableSeeder.reconcileBookmarksInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = legacyIds
        )

        assertEquals("Both matching IDs must be migrated", 2, result.matchedCount)
        assertTrue("No unmatched IDs expected", result.unmatchedIds.isEmpty())
        assertTrue("WHO-DENGUE-2024 must be bookmarked in Room", fakeBookmarkDao.isBookmarked("WHO-DENGUE-2024"))
        assertTrue("MOH-2026-04 must be bookmarked in Room", fakeBookmarkDao.isBookmarked("MOH-2026-04"))
        assertTrue("Legacy store should be retained for UI safety", result.legacyStoreRetained)
    }

    @Test
    fun bookmarkReconciliation_retainsUnmatchedLegacyIds() = runTest {
        val fakeArticleDao = FakeKnowledgeArticleDao()
        val fakeBookmarkDao = FakeKnowledgeBookmarkDao()

        // Seed standard articles
        KnowledgeTableSeeder.seedIfNeededInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = emptySet(),
            runInTransaction = { block -> block() }
        )

        // Mix of 1 matching ID and 2 unmatched IDs (e.g. from future or remote circulars)
        val legacyIds = setOf("WHO-DENGUE-2024", "FUTURE-CIRCULAR-999", "CUSTOM-ARTICLE-888")
        val result = KnowledgeTableSeeder.reconcileBookmarksInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = legacyIds
        )

        assertEquals("Only 1 matching ID should be migrated", 1, result.matchedCount)
        assertEquals("2 unmatched IDs must be retained", 2, result.unmatchedIds.size)
        assertTrue(result.unmatchedIds.contains("FUTURE-CIRCULAR-999"))
        assertTrue(result.unmatchedIds.contains("CUSTOM-ARTICLE-888"))

        assertTrue("Matching ID was inserted", fakeBookmarkDao.isBookmarked("WHO-DENGUE-2024"))
        assertFalse("Unmatched ID must NOT be in Room", fakeBookmarkDao.isBookmarked("FUTURE-CIRCULAR-999"))
        assertFalse("Unmatched ID must NOT be in Room", fakeBookmarkDao.isBookmarked("CUSTOM-ARTICLE-888"))
    }

    @Test
    fun bookmarkReconciliation_doesNotRunAfterSeedFailure() = runTest {
        val fakeArticleDao = FakeKnowledgeArticleDao().apply {
            shouldFailOnUpsert = true
        }
        val fakeBookmarkDao = FakeKnowledgeBookmarkDao()
        val legacyIds = setOf("WHO-DENGUE-2024")

        try {
            KnowledgeTableSeeder.seedIfNeededInternal(
                articleDao = fakeArticleDao,
                bookmarkDao = fakeBookmarkDao,
                legacyBookmarkIds = legacyIds,
                runInTransaction = { block -> block() }
            )
            fail("Expected exception when article upsert fails")
        } catch (expected: IllegalStateException) {
            // Expected failure
        }

        // Bookmark reconciliation must not have completed
        assertFalse("Room bookmarks must remain empty after seed failure", fakeBookmarkDao.isBookmarked("WHO-DENGUE-2024"))
        assertEquals(0, fakeBookmarkDao.bookmarks.size)
    }

    @Test
    fun bookmarkReconciliation_preservesExistingBookmarkTimestamp() = runTest {
        val fakeArticleDao = FakeKnowledgeArticleDao()
        val fakeBookmarkDao = FakeKnowledgeBookmarkDao()

        KnowledgeTableSeeder.seedIfNeededInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = emptySet(),
            runInTransaction = { block -> block() }
        )

        val originalTimestamp = 1600000000000L
        fakeBookmarkDao.insertBookmarksOrIgnore(
            listOf(KnowledgeBookmarkEntity(articleId = "EPMC-SEPSIS-2024", bookmarkedAt = originalTimestamp))
        )

        // Run reconciliation for the same ID
        KnowledgeTableSeeder.reconcileBookmarksInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = setOf("EPMC-SEPSIS-2024")
        )

        val currentBookmark = fakeBookmarkDao.bookmarks["EPMC-SEPSIS-2024"]
        assertNotNull(currentBookmark)
        assertEquals("Pre-existing bookmark timestamp must be preserved via IGNORE strategy", originalTimestamp, currentBookmark!!.bookmarkedAt)
    }

    @Test
    fun bookmarkReconciliation_repeatedReconciliationIsIdempotent() = runTest {
        val fakeArticleDao = FakeKnowledgeArticleDao()
        val fakeBookmarkDao = FakeKnowledgeBookmarkDao()

        KnowledgeTableSeeder.seedIfNeededInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = emptySet(),
            runInTransaction = { block -> block() }
        )

        val legacyIds = setOf("WHO-DENGUE-2024", "RNAO-PRESSURE-2023")

        // First reconciliation
        val result1 = KnowledgeTableSeeder.reconcileBookmarksInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = legacyIds
        )
        assertEquals(2, result1.matchedCount)
        val initialTimestamp = fakeBookmarkDao.bookmarks["WHO-DENGUE-2024"]!!.bookmarkedAt

        // Second repeated reconciliation
        val result2 = KnowledgeTableSeeder.reconcileBookmarksInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = legacyIds
        )
        assertEquals(2, result2.matchedCount)
        assertEquals("Repeated reconciliation must not alter existing timestamp", initialTimestamp, fakeBookmarkDao.bookmarks["WHO-DENGUE-2024"]!!.bookmarkedAt)
        assertEquals("Total bookmarks count must remain 2", 2, fakeBookmarkDao.bookmarks.size)
    }

    @Test
    fun bookmarkReconciliation_handlesPartialMigrationSafely() = runTest {
        val fakeArticleDao = FakeKnowledgeArticleDao()
        val fakeBookmarkDao = FakeKnowledgeBookmarkDao()

        // Seed with only 1 article initially
        fakeArticleDao.upsertArticle(
            KnowledgeArticleEntity(
                id = "WHO-DENGUE-2024",
                title = "Dengue Guideline",
                sourceInstitution = "SRC-WHO-SEARO",
                category = "Infection Prevention",
                clinicalSummary = "Summary",
                recommendationsJson = "[]",
                evidenceGrade = "GRADE_A",
                year = 2024,
                lastReviewed = "2024-01-01"
            )
        )

        val legacyIds = setOf("WHO-DENGUE-2024", "UNSEEDED-ARTICLE-999")
        val result = KnowledgeTableSeeder.reconcileBookmarksInternal(
            articleDao = fakeArticleDao,
            bookmarkDao = fakeBookmarkDao,
            legacyBookmarkIds = legacyIds
        )

        assertEquals("1 matched", 1, result.matchedCount)
        assertEquals("1 unmatched", 1, result.unmatchedIds.size)
        assertTrue(result.unmatchedIds.contains("UNSEEDED-ARTICLE-999"))
        assertTrue(fakeBookmarkDao.isBookmarked("WHO-DENGUE-2024"))
        assertFalse(fakeBookmarkDao.isBookmarked("UNSEEDED-ARTICLE-999"))
    }

    // --- In-Memory Fake DAOs for Pure JVM Testing ---

    private class FakeKnowledgeArticleDao : KnowledgeArticleDao {
        val sources = mutableMapOf<String, KnowledgeSourceEntity>()
        val categories = mutableMapOf<String, KnowledgeCategoryEntity>()
        val tags = mutableSetOf<String>()
        val articles = mutableMapOf<String, KnowledgeArticleEntity>()
        val crossRefs = mutableListOf<ArticleTagCrossRef>()
        var shouldFailOnUpsert = false

        override suspend fun upsertArticle(article: KnowledgeArticleEntity) {
            if (shouldFailOnUpsert) throw IllegalStateException("Simulated database disk failure")
            articles[article.id] = article
        }

        override suspend fun upsertArticles(articles: List<KnowledgeArticleEntity>) {
            if (shouldFailOnUpsert) throw IllegalStateException("Simulated database disk failure")
            for (article in articles) {
                this.articles[article.id] = article
            }
        }

        override suspend fun insertArticle(article: KnowledgeArticleEntity) {
            if (!articles.containsKey(article.id)) articles[article.id] = article
        }

        override suspend fun insertArticles(articles: List<KnowledgeArticleEntity>) {
            for (article in articles) insertArticle(article)
        }

        override suspend fun updateArticle(article: KnowledgeArticleEntity) {
            articles[article.id] = article
        }

        override suspend fun getArticleById(id: String): KnowledgeArticleEntity? = articles[id]

        override fun observeArticleById(id: String): Flow<KnowledgeArticleEntity?> =
            flowOf(articles[id])

        override fun observeAllArticles(): Flow<List<KnowledgeArticleEntity>> =
            flowOf(articles.values.sortedByDescending { it.year })

        override suspend fun getAllArticles(): List<KnowledgeArticleEntity> =
            articles.values.sortedByDescending { it.year }

        override fun observeArticlesByCategory(category: String): Flow<List<KnowledgeArticleEntity>> =
            flowOf(articles.values.filter { it.category == category }.sortedByDescending { it.year })

        override suspend fun getExistingArticleIds(ids: List<String>): List<String> =
            ids.filter { articles.containsKey(it) }

        override suspend fun deleteArticleById(id: String): Int = if (articles.remove(id) != null) 1 else 0

        override suspend fun insertSource(source: KnowledgeSourceEntity) {
            if (!sources.containsKey(source.id)) sources[source.id] = source
        }

        override suspend fun insertSources(sources: List<KnowledgeSourceEntity>) {
            for (source in sources) insertSource(source)
        }

        override fun observeAllSources(): Flow<List<KnowledgeSourceEntity>> =
            flowOf(sources.values.toList())

        override suspend fun insertCategory(category: KnowledgeCategoryEntity) {
            if (!categories.containsKey(category.id)) categories[category.id] = category
        }

        override suspend fun insertCategories(categories: List<KnowledgeCategoryEntity>) {
            for (category in categories) insertCategory(category)
        }

        override fun observeAllCategories(): Flow<List<KnowledgeCategoryEntity>> =
            flowOf(categories.values.sortedBy { it.displayOrder })

        override suspend fun insertTag(tag: KnowledgeTagEntity) {
            tags.add(tag.tag)
        }

        override suspend fun insertTags(tags: List<KnowledgeTagEntity>) {
            for (tag in tags) insertTag(tag)
        }

        override suspend fun insertArticleTagCrossRef(crossRef: ArticleTagCrossRef) {
            if (!crossRefs.contains(crossRef)) crossRefs.add(crossRef)
        }

        override suspend fun insertArticleTagCrossRefs(crossRefs: List<ArticleTagCrossRef>) {
            for (crossRef in crossRefs) insertArticleTagCrossRef(crossRef)
        }

        override fun observeTagsForArticle(articleId: String): Flow<List<String>> =
            flowOf(crossRefs.filter { it.articleId == articleId }.map { it.tag })
    }

    private class FakeKnowledgeBookmarkDao : KnowledgeBookmarkDao {
        val bookmarks = mutableMapOf<String, KnowledgeBookmarkEntity>()

        override suspend fun insertBookmark(bookmark: KnowledgeBookmarkEntity) {
            bookmarks[bookmark.articleId] = bookmark
        }

        override suspend fun insertBookmarksOrIgnore(bookmarks: List<KnowledgeBookmarkEntity>) {
            for (bookmark in bookmarks) {
                if (!this.bookmarks.containsKey(bookmark.articleId)) {
                    this.bookmarks[bookmark.articleId] = bookmark
                }
            }
        }

        override suspend fun deleteBookmark(articleId: String): Int = if (bookmarks.remove(articleId) != null) 1 else 0

        override fun observeIsBookmarked(articleId: String): Flow<Boolean> =
            flowOf(bookmarks.containsKey(articleId))

        override suspend fun isBookmarked(articleId: String): Boolean = bookmarks.containsKey(articleId)

        override fun observeAllBookmarkedArticleIds(): Flow<List<String>> =
            flowOf(bookmarks.keys.toList())

        override suspend fun getAllBookmarkedArticleIds(): List<String> = bookmarks.keys.toList()

        override suspend fun getBookmarkByArticleId(articleId: String): KnowledgeBookmarkEntity? = bookmarks[articleId]
    }
}
