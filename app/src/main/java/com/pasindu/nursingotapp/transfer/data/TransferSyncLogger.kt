package com.pasindu.nursingotapp.transfer.data

import android.util.Log
import javax.inject.Inject

/**
 * Logging abstraction for the Mutual Transfer data layer so that
 * JVM unit tests do not invoke unmocked `android.util.Log` methods.
 */
interface TransferSyncLogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

object NoOpTransferSyncLogger : TransferSyncLogger {
    override fun d(tag: String, message: String) = Unit
    override fun e(tag: String, message: String, throwable: Throwable?) = Unit
}

class AndroidTransferSyncLogger @Inject constructor() : TransferSyncLogger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
