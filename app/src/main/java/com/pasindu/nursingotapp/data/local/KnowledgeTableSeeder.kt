package com.pasindu.nursingotapp.data.local

import android.content.Context
import androidx.room.withTransaction
import com.pasindu.nursingotapp.data.local.dao.KnowledgeArticleDao
import com.pasindu.nursingotapp.data.local.dao.KnowledgeBookmarkDao
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeBookmarkEntity
import com.pasindu.nursingotapp.data.local.seed.KnowledgeHubSeedData
import com.pasindu.nursingotapp.ui.KnowledgeHubBookmarkStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Background, transaction-safe, idempotent seeder for Knowledge Hub Room v17.
 *
 * Concurrency & Lifecycle Architecture:
 * - In-flight seeding operations are tracked per AppDatabase instance in [activeSeedJobs].
 * - If a seeding operation is currently running for an AppDatabase, concurrent callers share and
 *   await that exact same in-flight Deferred.
 * - When the in-flight job completes (success, failure, or cancellation), its entry is cleaned up
 *   from [activeSeedJobs] in a finally block.
 * - Cleaning up entries upon completion ensures:
 *     1. Zero permanent memory leaks or strong references to AppDatabase instances.
 *     2. Failed jobs are never permanently cached; subsequent invocations can retry seeding safely.
 *     3. Cancelled jobs do not leave stale, un-awaitable entries.
 *     4. Independent database instances (e.g. in concurrent tests) seed without mutual contention.
 * - [awaitSeeding] directly awaits the in-flight job and rethrows any failure or cancellation,
 *   never silently concealing exceptions.
 *
 * Data Integrity Architecture:
 * - Sources, categories, tags, and cross-references are inserted with IGNORE semantics.
 * - Articles are updated via native in-place upsert (@Upsert), NEVER SQLite REPLACE.
 * - Child tables (bookmarks, notes, reading history, cache) are NEVER cascade-deleted.
 * - Legacy SharedPreferences bookmarks are migrated into Room with IGNORE semantics to preserve
 *   original timestamps, while safely retaining the legacy store for existing UI compatibility.
 */
object KnowledgeTableSeeder {

    const val CURRENT_SEED_VERSION = 1
    const val PREFS_NAME = "knowledge_hub_prefs"
    const val KEY_SEED_VERSION = "knowledge_seed_version"

    // Guard map tracking in-flight seeding jobs per AppDatabase instance.
    private val activeSeedJobs = ConcurrentHashMap<AppDatabase, CompletableDeferred<Unit>>()

    /**
     * Starts seeding asynchronously in the background on Dispatchers.IO.
     * Guaranteed non-blocking for application startup, Hilt graph creation, and UI render.
     *
     * Thread-safe and re-entrant:
     * Concurrent calls for the same [db] instance share the single in-flight Deferred.
     */
    fun startSeeding(
        db: AppDatabase,
        context: Context? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ): Deferred<Unit> {
        val (jobToLaunch, jobToReturn) = synchronized(activeSeedJobs) {
            val existing = activeSeedJobs[db]
            if (existing != null && existing.isActive) {
                null to existing
            } else {
                val deferred = CompletableDeferred<Unit>()
                activeSeedJobs[db] = deferred
                deferred to deferred
            }
        }

        if (jobToLaunch != null) {
            val coroutineJob = scope.launch {
                try {
                    seedIfNeeded(db, context)
                    synchronized(activeSeedJobs) {
                        if (activeSeedJobs[db] === jobToLaunch) {
                            activeSeedJobs.remove(db)
                        }
                    }
                    jobToLaunch.complete(Unit)
                } catch (ce: CancellationException) {
                    synchronized(activeSeedJobs) {
                        if (activeSeedJobs[db] === jobToLaunch) {
                            activeSeedJobs.remove(db)
                        }
                    }
                    jobToLaunch.cancel(ce)
                    throw ce
                } catch (e: Exception) {
                    try {
                        android.util.Log.e("KnowledgeTableSeeder", "Knowledge Hub seeding failed for $db", e)
                    } catch (_: Throwable) {
                        System.err.println("KnowledgeTableSeeder: Seeding failed for $db: ${e.message}")
                    }
                    synchronized(activeSeedJobs) {
                        if (activeSeedJobs[db] === jobToLaunch) {
                            activeSeedJobs.remove(db)
                        }
                    }
                    jobToLaunch.completeExceptionally(e)
                }
            }

            // Guarantee completion and map cleanup even if the coroutine is cancelled before starting.
            coroutineJob.invokeOnCompletion { cause ->
                synchronized(activeSeedJobs) {
                    if (activeSeedJobs[db] === jobToLaunch) {
                        activeSeedJobs.remove(db)
                    }
                }
                if (cause is CancellationException) {
                    jobToLaunch.cancel(cause)
                } else if (cause != null) {
                    jobToLaunch.completeExceptionally(cause)
                } else {
                    jobToLaunch.complete(Unit)
                }
            }
        }

        return jobToReturn
    }

    /**
     * Suspends until any in-flight seeding on the given database instance completes.
     * Propagates exceptions directly; never silently swallows errors or cancellation.
     */
    suspend fun awaitSeeding(db: AppDatabase) {
        val inFlight = synchronized(activeSeedJobs) { activeSeedJobs[db] }
        inFlight?.await()
    }

    /**
     * Seeds or updates the Knowledge Hub tables if rows are missing or seed version is newer.
     * Executes in a single atomic Room transaction.
     */
    suspend fun seedIfNeeded(db: AppDatabase, context: Context?) {
        val articleDao = db.knowledgeArticleDao()
        val bookmarkDao = db.knowledgeBookmarkDao()

        val storedVersion = if (context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SEED_VERSION, 0)
        } else 0

        val existingArticles = articleDao.getAllArticles()
        val needsSeed = existingArticles.isEmpty() || storedVersion < CURRENT_SEED_VERSION

        if (!needsSeed) {
            return
        }

        val legacyBookmarkIds = if (context != null) {
            KnowledgeHubBookmarkStore(context).getBookmarkedIds()
        } else {
            emptySet()
        }

        seedIfNeededInternal(
            articleDao = articleDao,
            bookmarkDao = bookmarkDao,
            legacyBookmarkIds = legacyBookmarkIds,
            runInTransaction = { block -> db.withTransaction { block() } }
        )

        if (context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SEED_VERSION, CURRENT_SEED_VERSION)
                .apply()
        }
    }

    /**
     * Internal implementation allowing transaction-aware execution across both
     * Android Room runtime and pure JVM test environments.
     */
    internal suspend fun seedIfNeededInternal(
        articleDao: KnowledgeArticleDao,
        bookmarkDao: KnowledgeBookmarkDao,
        legacyBookmarkIds: Set<String> = emptySet(),
        runInTransaction: suspend (suspend () -> Unit) -> Unit
    ): SeedResult {
        var reconciliationResult: BookmarkReconciliationResult? = null

        runInTransaction {
            // Step 1: Insert parent sources
            articleDao.insertSources(KnowledgeHubSeedData.sources)

            // Step 2: Insert categories
            articleDao.insertCategories(KnowledgeHubSeedData.categories)

            // Step 3: Insert tags
            articleDao.insertTags(KnowledgeHubSeedData.tags)

            // Step 4: Upsert articles (CRITICAL: In-place update, never DELETE or REPLACE)
            articleDao.upsertArticles(KnowledgeHubSeedData.articles)

            // Step 5: Insert article-tag cross-references
            articleDao.insertArticleTagCrossRefs(KnowledgeHubSeedData.articleTagCrossRefs)

            // Step 6: Reconcile bookmarks strictly after article seeding succeeds
            if (legacyBookmarkIds.isNotEmpty()) {
                reconciliationResult = reconcileBookmarksInternal(
                    articleDao = articleDao,
                    bookmarkDao = bookmarkDao,
                    legacyBookmarkIds = legacyBookmarkIds
                )
            }
        }

        return SeedResult(
            sourcesCount = KnowledgeHubSeedData.sources.size,
            categoriesCount = KnowledgeHubSeedData.categories.size,
            tagsCount = KnowledgeHubSeedData.tags.size,
            articlesCount = KnowledgeHubSeedData.articles.size,
            crossRefsCount = KnowledgeHubSeedData.articleTagCrossRefs.size,
            reconciliationResult = reconciliationResult
        )
    }

    /**
     * Reconciles legacy SharedPreferences bookmarks with Room knowledge_bookmarks.
     *
     * Rules:
     * - Only IDs that exist in knowledge_articles are inserted into Room.
     * - Uses IGNORE so pre-existing bookmark timestamps are never modified.
     * - Unmatched IDs are returned to remain safely in SharedPreferences.
     * - Legacy SharedPreferences bookmarks are retained intact because existing UI
     *   (KnowledgeHubViewModel) currently relies on KnowledgeHubBookmarkStore until
     *   full ViewModel Room integration in a subsequent phase.
     */
    internal suspend fun reconcileBookmarksInternal(
        articleDao: KnowledgeArticleDao,
        bookmarkDao: KnowledgeBookmarkDao,
        legacyBookmarkIds: Set<String>
    ): BookmarkReconciliationResult {
        if (legacyBookmarkIds.isEmpty()) {
            return BookmarkReconciliationResult(matchedCount = 0, unmatchedIds = emptySet(), legacyStoreRetained = true)
        }

        val existingArticleIds = articleDao.getExistingArticleIds(legacyBookmarkIds.toList()).toSet()
        val matchingIds = legacyBookmarkIds.filter { it in existingArticleIds }
        val unmatchedIds = legacyBookmarkIds.filter { it !in existingArticleIds }.toSet()

        if (matchingIds.isNotEmpty()) {
            val entities = matchingIds.map { id ->
                KnowledgeBookmarkEntity(
                    articleId = id,
                    bookmarkedAt = System.currentTimeMillis()
                )
            }
            bookmarkDao.insertBookmarksOrIgnore(entities)
        }

        return BookmarkReconciliationResult(
            matchedCount = matchingIds.size,
            unmatchedIds = unmatchedIds,
            legacyStoreRetained = true
        )
    }

    internal fun clearJobForTest(db: AppDatabase) {
        synchronized(activeSeedJobs) {
            activeSeedJobs.remove(db)
        }
    }

    internal fun getActiveJobsCountForTest(): Int {
        return synchronized(activeSeedJobs) { activeSeedJobs.size }
    }
}

data class SeedResult(
    val sourcesCount: Int,
    val categoriesCount: Int,
    val tagsCount: Int,
    val articlesCount: Int,
    val crossRefsCount: Int,
    val reconciliationResult: BookmarkReconciliationResult?
)

data class BookmarkReconciliationResult(
    val matchedCount: Int,
    val unmatchedIds: Set<String>,
    val legacyStoreRetained: Boolean = true
)
