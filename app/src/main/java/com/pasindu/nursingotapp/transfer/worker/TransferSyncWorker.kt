package com.pasindu.nursingotapp.transfer.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Offline-first WorkManager worker for Mutual Transfer request synchronization.
 *
 * Requirements satisfied:
 * - Runs only when network connectivity is available (NetworkType.CONNECTED).
 * - Uses exponential backoff (15s base) for transient network failures.
 * - Idempotent execution (ExistingWorkPolicy.KEEP prevents clobbering in-flight work).
 * - Caps maximum retry attempts (MAX_ATTEMPTS = 3).
 * - Safe against process termination or app being force-closed.
 * - Signals authentication errors via WorkManager output data and persistent flag.
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
        if (runAttemptCount >= MAX_ATTEMPTS) {
            Log.w(
                "TransferSyncWorker",
                "Max retry attempts ($MAX_ATTEMPTS) reached before execution. Failing work."
            )
            return Result.failure()
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            TransferSyncWorkerEntryPoint::class.java
        )
        val repository = entryPoint.repository()

        val syncResult = try {
            repository.syncActiveRequest()
        } catch (e: CancellationException) {
            throw e
        } catch (error: Throwable) {
            Log.e(
                "TransferSyncWorker",
                "syncActiveRequest threw: ${error.javaClass.simpleName}: ${error.message}",
                error
            )
            return if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                Log.w(
                    "TransferSyncWorker",
                    "Max retry attempts ($MAX_ATTEMPTS) reached after exception. Failing work."
                )
                Result.failure()
            } else {
                Result.retry()
            }
        }

        Log.d(
            "TransferSyncWorker",
            "syncActiveRequest result: ${syncResult::class.simpleName}"
        )

        return when (syncResult) {
            is WorkerSyncResult.MatchFound,
            is WorkerSyncResult.NoMatch,
            is WorkerSyncResult.RequestCompleted,
            is WorkerSyncResult.RequestWithdrawn,
            is WorkerSyncResult.ReturnedToPool -> {
                clearSyncAuthError(applicationContext)
                Result.success()
            }
            is WorkerSyncResult.NetworkError -> {
                if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                    Log.w(
                        "TransferSyncWorker",
                        "Max retry attempts ($MAX_ATTEMPTS) reached for NetworkError. Failing work."
                    )
                    Result.failure()
                } else {
                    // Retry with exponential backoff on transient network failures
                    Result.retry()
                }
            }
            is WorkerSyncResult.Conflict -> {
                // Server conflict; do not loop indefinitely
                Result.failure()
            }
            is WorkerSyncResult.AuthError -> {
                // Authentication issue; record flag and fail with output data so UI can prompt
                Log.e("TransferSyncWorker", "Authentication error during background sync.")
                recordSyncAuthError(applicationContext)
                Result.failure(workDataOf(KEY_AUTH_ERROR to true))
            }
        }
    }

    companion object {
        const val WORK_NAME = "transfer_active_request_sync"
        const val MAX_ATTEMPTS = 3
        const val KEY_AUTH_ERROR = "auth_error"
        const val PREFS_NAME = "transfer_sync_prefs"
        const val KEY_SYNC_AUTH_ERROR = "key_sync_auth_error"

        fun recordSyncAuthError(context: Context) {
            try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_SYNC_AUTH_ERROR, true)
                    .apply()
            } catch (ignored: Exception) {}
        }

        fun clearSyncAuthError(context: Context) {
            try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_SYNC_AUTH_ERROR, false)
                    .apply()
            } catch (ignored: Exception) {}
        }

        fun hasSyncAuthError(context: Context): Boolean {
            return try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(KEY_SYNC_AUTH_ERROR, false)
            } catch (ignored: Exception) {
                false
            }
        }

        fun enqueue(
            context: Context,
            policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP
        ) {
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
                    policy,
                    workRequest
                )
        }
    }
}
