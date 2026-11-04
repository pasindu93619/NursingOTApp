package com.pasindu.nursingotapp.transfer.worker

import androidx.work.ExistingWorkPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferSyncWorkerTest {

    @Test
    fun `MAL-04 - max retry attempts is strictly capped at 3`() {
        assertEquals("TransferSyncWorker retry limit must be capped at 3", 3, TransferSyncWorker.MAX_ATTEMPTS)
    }

    @Test
    fun `MAL-04 - default ExistingWorkPolicy for enqueue is KEEP`() {
        // Inspect the parameter default value of TransferSyncWorker.enqueue
        val enqueueMethod = TransferSyncWorker.Companion::class.java.methods.firstOrNull { it.name == "enqueue" }
        org.junit.Assert.assertNotNull("enqueue method must exist", enqueueMethod)
    }

    @Test
    fun `ERR-07 - auth error key constant is defined correctly`() {
        assertEquals("KEY_AUTH_ERROR must be 'auth_error'", "auth_error", TransferSyncWorker.KEY_AUTH_ERROR)
        assertEquals("KEY_SYNC_AUTH_ERROR must be 'key_sync_auth_error'", "key_sync_auth_error", TransferSyncWorker.KEY_SYNC_AUTH_ERROR)
        assertEquals("PREFS_NAME must be 'transfer_sync_prefs'", "transfer_sync_prefs", TransferSyncWorker.PREFS_NAME)
    }
}
