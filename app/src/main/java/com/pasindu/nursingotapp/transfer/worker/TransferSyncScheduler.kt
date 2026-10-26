package com.pasindu.nursingotapp.transfer.worker

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface TransferSyncScheduler {
    fun scheduleSync()
}

object NoOpTransferSyncScheduler : TransferSyncScheduler {
    override fun scheduleSync() = Unit
}

@Singleton
class WorkManagerTransferSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : TransferSyncScheduler {
    override fun scheduleSync() {
        TransferSyncWorker.enqueue(context)
    }
}
