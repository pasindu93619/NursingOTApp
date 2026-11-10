package com.pasindu.nursingotapp.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeArticleEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeBookmarkEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeCategoryEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeOfflineCacheEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgePersonalNoteEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeReadingHistoryEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeSourceEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android Instrumentation Test validating actual SQLite / Room database behavior for:
 * 1. Room @Upsert non-destructive update semantics preserving all child tables
 *    (bookmarks, personal notes, reading history, offline cache) under active Foreign Keys.
 * 2. Real AppDatabase concurrency sharing on startSeeding().
 * 3. Independent database instance isolation.
 * 4. Failure recovery and retryability.
 * 5. Cancellation recovery without permanent blocking.
 */
@RunWith(AndroidJUnit4::class)
class KnowledgeRoomChildPreservationTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        KnowledgeTableSeeder.clearJobForTest(db)
        db.close()
    }

    @Test
    fun upsertArticle_preservesAllChildRecordsAndOriginalTimestamps() = runBlocking {
        val articleDao = db.knowledgeArticleDao()
        val bookmarkDao = db.knowledgeBookmarkDao()
        val noteDao = db.knowledgeNoteDao()
        val readingDao = db.knowledgeReadingDao()
        val cacheDao = db.knowledgeOfflineCacheDao()

        // 1. Insert parent source and category
        articleDao.insertSource(
            KnowledgeSourceEntity(
                id = "SRC-WHO-SEARO",
                name = "WHO SEARO",
                shortCode = "WHO",
                trustTier = "TIER_1_OFFICIAL",
                websiteUrl = "https://www.who.int/southeastasia"
            )
        )
        articleDao.insertCategory(
            KnowledgeCategoryEntity(
                id = "Infection Prevention",
                displayName = "Infection Prevention",
                displayOrder = 1
            )
        )

        // 2. Insert original article
        val articleId = "WHO-DENGUE-2024"
        val originalArticle = KnowledgeArticleEntity(
            id = articleId,
            title = "Original Dengue Guideline 2024",
            sourceInstitution = "SRC-WHO-SEARO",
            category = "Infection Prevention",
            clinicalSummary = "Original summary text",
            recommendationsJson = "[\"Original recommendation\"]",
            evidenceGrade = "GRADE_A",
            year = 2024,
            lastReviewed = "2024-01-01",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )
        articleDao.upsertArticle(originalArticle)

        // 3. Insert real child bookmark
        val originalBookmarkTime = 1700000010000L
        bookmarkDao.insertBookmark(
            KnowledgeBookmarkEntity(
                articleId = articleId,
                bookmarkedAt = originalBookmarkTime
            )
        )

        // 4. Insert real child personal note
        val noteId = noteDao.insertNote(
            KnowledgePersonalNoteEntity(
                id = 0L,
                articleId = articleId,
                originalArticleId = articleId,
                noteContent = "Clinical observation: strict hourly urine output monitoring required.",
                createdAt = 1700000020000L,
                updatedAt = 1700000020000L
            )
        )

        // 5. Insert real child reading history (linked to CPD if desired)
        val history = KnowledgeReadingHistoryEntity(
            articleId = articleId,
            lastReadAt = 1700000030000L,
            readingProgressPercent = 100,
            isCompleted = true,
            completedAt = 1700000030000L,
            linkedCpdLogId = null
        )
        readingDao.upsertReadingHistory(history)

        // 6. Insert real child offline cache record
        val cacheRecord = KnowledgeOfflineCacheEntity(
            articleId = articleId,
            cachedAt = 1700000040000L,
            fileSizeBytes = 2048L,
            localRelativePath = "knowledge_cache/who_dengue_2024.pdf",
            sha256Hash = "sha256:dengue_guideline_content",
            isFullTextCached = 1
        )
        cacheDao.upsertCacheEntry(cacheRecord)

        // Verify baseline exists
        assertNotNull(articleDao.getArticleById(articleId))
        assertEquals(originalBookmarkTime, bookmarkDao.getBookmarkByArticleId(articleId)?.bookmarkedAt)
        val initialNotes = noteDao.getNotesForArticle(articleId)
        assertEquals(1, initialNotes.size)
        assertEquals(articleId, initialNotes[0].articleId)
        assertNotNull(readingDao.getReadingHistory(articleId))
        assertNotNull(cacheDao.getByArticleId(articleId))

        // 7. Execute real DAO @Upsert with updated article metadata
        val updatedArticle = originalArticle.copy(
            title = "Comprehensive Guidelines for Prevention and Control of Dengue - Updated Edition",
            clinicalSummary = "Updated clinical fluid resuscitation and hematocrit titration guidance.",
            recommendationsJson = "[\"Updated step-down fluid titration.\"]",
            lastReviewed = "2026-08-01",
            updatedAt = 1700000099000L
        )
        articleDao.upsertArticle(updatedArticle)

        // 8. Query all child tables again and verify 100% preservation
        val reFetchedArticle = articleDao.getArticleById(articleId)
        assertNotNull("Article must still exist", reFetchedArticle)
        assertEquals("Comprehensive Guidelines for Prevention and Control of Dengue - Updated Edition", reFetchedArticle?.title)
        assertEquals("Updated clinical fluid resuscitation and hematocrit titration guidance.", reFetchedArticle?.clinicalSummary)
        assertEquals(1700000099000L, reFetchedArticle?.updatedAt)

        // Verify Bookmark was NOT cascade-deleted and timestamp is UNCHANGED
        val reFetchedBookmark = bookmarkDao.getBookmarkByArticleId(articleId)
        assertNotNull("Bookmark must NOT be cascade deleted by article @Upsert", reFetchedBookmark)
        assertEquals("Bookmark timestamp must be exactly preserved", originalBookmarkTime, reFetchedBookmark?.bookmarkedAt)

        // Verify Personal Note was NOT cascade-set-to-null
        val reFetchedNotes = noteDao.getNotesForArticle(articleId)
        assertEquals("Personal notes must not be lost", 1, reFetchedNotes.size)
        assertEquals("Personal note articleId must remain intact", articleId, reFetchedNotes[0].articleId)
        assertEquals("Clinical observation: strict hourly urine output monitoring required.", reFetchedNotes[0].noteContent)

        // Verify Reading History was NOT cascade-deleted
        val reFetchedHistory = readingDao.getReadingHistory(articleId)
        assertNotNull("Reading history must NOT be cascade deleted", reFetchedHistory)
        assertEquals(100, reFetchedHistory?.readingProgressPercent)
        assertTrue(reFetchedHistory?.isCompleted == true)

        // Verify Offline Cache was NOT cascade-deleted
        val reFetchedCache = cacheDao.getByArticleId(articleId)
        assertNotNull("Offline cache must NOT be cascade deleted", reFetchedCache)
        assertEquals("knowledge_cache/who_dengue_2024.pdf", reFetchedCache?.localRelativePath)
        assertEquals(2048L, reFetchedCache?.fileSizeBytes)
    }

    @Test
    fun startSeeding_concurrentCallsShareInFlightOperationAndSeedOnce() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO)

        // Two concurrent calls for the same database instance
        val deferred1 = KnowledgeTableSeeder.startSeeding(db, context = null, scope = scope)
        val deferred2 = KnowledgeTableSeeder.startSeeding(db, context = null, scope = scope)

        // Both calls must return the exact same in-flight Deferred instance
        assertSame("Concurrent startSeeding calls for the same DB must share the same Deferred", deferred1, deferred2)

        // Await completion
        deferred1.await()
        deferred2.await()

        // Verify database was seeded
        val articles = db.knowledgeArticleDao().getAllArticles()
        assertEquals(7, articles.size)

        // Verify that after completion, the job is cleaned up from the map to avoid leaking memory
        assertEquals(0, KnowledgeTableSeeder.getActiveJobsCountForTest())
    }

    @Test
    fun startSeeding_independentDatabasesSeedIndependently() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db2 = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val scope = CoroutineScope(Dispatchers.IO)
            val deferred1 = KnowledgeTableSeeder.startSeeding(db, context = null, scope = scope)
            val deferred2 = KnowledgeTableSeeder.startSeeding(db2, context = null, scope = scope)

            // Distinct databases must have distinct Deferred jobs
            assertFalse("Independent databases must not share the same Deferred", deferred1 === deferred2)

            deferred1.await()
            deferred2.await()

            assertEquals(7, db.knowledgeArticleDao().getAllArticles().size)
            assertEquals(7, db2.knowledgeArticleDao().getAllArticles().size)
        } finally {
            KnowledgeTableSeeder.clearJobForTest(db2)
            db2.close()
        }
    }

    @Test
    fun startSeeding_failedAttemptCanBeRetriedSuccessfully() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val failingDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Close the database immediately so queries fail
        failingDb.close()

        val scope = CoroutineScope(Dispatchers.IO)
        val failedDeferred = KnowledgeTableSeeder.startSeeding(failingDb, context = null, scope = scope)

        try {
            failedDeferred.await()
            fail("Closed database should fail seeding")
        } catch (_: Exception) {
            // Expected failure
        }

        // Verify active jobs cleaned up after failure
        assertEquals(0, KnowledgeTableSeeder.getActiveJobsCountForTest())

        // Create a healthy database and verify retry succeeds
        val healthyDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val retryDeferred = KnowledgeTableSeeder.startSeeding(healthyDb, context = null, scope = scope)
            retryDeferred.await()
            assertEquals(7, healthyDb.knowledgeArticleDao().getAllArticles().size)
        } finally {
            KnowledgeTableSeeder.clearJobForTest(healthyDb)
            healthyDb.close()
        }
    }

    @Test
    fun startSeeding_cancellationDoesNotBlockFutureSeeding() = runBlocking {
        val cancellableScope = CoroutineScope(Dispatchers.IO)
        val deferred = KnowledgeTableSeeder.startSeeding(db, context = null, scope = cancellableScope)

        // Cancel scope immediately
        cancellableScope.cancel()

        try {
            deferred.await()
        } catch (_: Exception) {
            // Cancellation or completion is fine
        }

        // Ensure clean state
        assertEquals(0, KnowledgeTableSeeder.getActiveJobsCountForTest())

        // Launch fresh seeding on a new scope
        val freshScope = CoroutineScope(Dispatchers.IO)
        val freshDeferred = KnowledgeTableSeeder.startSeeding(db, context = null, scope = freshScope)
        freshDeferred.await()

        assertEquals(7, db.knowledgeArticleDao().getAllArticles().size)
    }
}
