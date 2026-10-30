package com.pasindu.nursingotapp.data.local

import androidx.room.withTransaction
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ensures the exact supplied salary lookup table is available.
 * Repairs incomplete or stale lookup data atomically within a database transaction,
 * preventing race conditions and partial reads during application initialization.
 */
object SalaryTableSeeder {

    @Volatile
    private var seedDeferred: CompletableDeferred<Unit>? = null

    /**
     * Atomically seeds or repairs the 2026/2027 salary table if rows are missing or stale.
     * Wrapped in a Room database transaction so clearAll and insertAll execute as a single atomic unit.
     */
    suspend fun seedIfNeeded(db: AppDatabase) {
        try {
            seedIfNeededInternal(
                dao = db.salaryStep2027Dao(),
                runInTransaction = { block -> db.withTransaction { block() } }
            )
        } finally {
            synchronized(this) {
                seedDeferred?.complete(Unit)
            }
        }
    }

    /**
     * Internal implementation allowing transaction-aware execution across both
     * Android Room runtime and unit-test environments.
     */
    internal suspend fun seedIfNeededInternal(
        dao: SalaryStep2027Dao,
        runInTransaction: suspend (suspend () -> Unit) -> Unit
    ): Boolean {
        val expected = SalaryTable2026_2027Seed.rows
        val actual = dao.observeAll().first()

        val needsRepair =
            actual.size != expected.size ||
                expected.any { expectedRow ->
                    actual.none { stored ->
                        stored.grade == expectedRow.grade &&
                            stored.salaryStep == expectedRow.salaryStep &&
                            stored.currentBasicSalary2026 == expectedRow.currentBasicSalary2026 &&
                            stored.basicSalary2027 == expectedRow.basicSalary2027
                    }
                }

        if (needsRepair) {
            runInTransaction {
                dao.clearAll()
                dao.insertAll(expected)
            }
            return true
        }
        return false
    }

    /**
     * Starts seeding asynchronously in the given coroutine scope (defaults to Dispatchers.IO).
     * Returns a [Deferred] representing seeding completion.
     */
    fun startSeeding(
        db: AppDatabase,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ): Deferred<Unit> {
        synchronized(this) {
            val existing = seedDeferred
            if (existing != null && !existing.isCancelled) {
                return existing
            }
            val deferred = CompletableDeferred<Unit>()
            seedDeferred = deferred
            scope.launch {
                try {
                    seedIfNeeded(db)
                    deferred.complete(Unit)
                } catch (t: Throwable) {
                    deferred.completeExceptionally(t)
                }
            }
            return deferred
        }
    }

    /**
     * Suspends until any in-flight startup seeding has completed.
     */
    suspend fun awaitSeeding() {
        runCatching { seedDeferred?.await() }
    }

    internal fun setSeedingDeferredForTest(deferred: CompletableDeferred<Unit>?) {
        synchronized(this) {
            seedDeferred = deferred
        }
    }
}
