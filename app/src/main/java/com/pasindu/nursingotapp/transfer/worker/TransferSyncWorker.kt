package com.pasindu.nursingotapp.transfer.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Offline-first WorkManager worker for Mutual Transfer request synchronization.
 *
 * Requirements satisfied:
 * - Runs only when network connectivity is available (NetworkType.CONNECTED).
 * - Uses exponential backoff (15s base) for transient network failures.
 * - Idempotent execution (ExistingWorkPolicy.REPLACE prevents duplicate scheduled jobs).
 * - Safe against process termination or app being force-closed.
 * - Local Room requests are never deleted on synchronization failures.
 */
class TransferSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TransferSyncWorkerEntryPoint {
        fun repository(): TransferRequestRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            TransferSyncWorkerEntryPoint::class.java
        )
        val repository = entryPoint.repository()

        return when (val syncResult = repository.syncActiveRequest()) {
            is WorkerSyncResult.MatchFound,
            is WorkerSyncResult.NoMatch -> {
                Result.success()
            }
            is WorkerSyncResult.NetworkError -> {
                // Retry with exponential backoff on transient network failures
                Result.retry()
            }
            is WorkerSyncResult.Conflict -> {
                // Server conflict; do not loop indefinitely
                Result.failure()
            }
            is WorkerSyncResult.AuthError -> {
                // Authentication issue; user must be signed in
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "transfer_active_request_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<TransferSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        }
    }
}
