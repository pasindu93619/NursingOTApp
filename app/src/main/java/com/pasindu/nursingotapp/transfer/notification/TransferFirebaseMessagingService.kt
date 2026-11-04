package com.pasindu.nursingotapp.transfer.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TransferFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token received from Firebase Messaging infrastructure.
        // Direct persistence and upload to Firestore are deferred to Phase 5, Step 5.3B.
        // Token is intentionally not logged to avoid credential/token exposure in system logs.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
            val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]
            val payload = TransferNotificationHelper.parsePayload(
                data = remoteMessage.data,
                fallbackTitle = title,
                fallbackBody = body
            )
            TransferNotificationHelper.showNotification(applicationContext, payload)
        } catch (e: Exception) {
            android.util.Log.w("TransferFCMService", "Error processing incoming FCM message: ${e.javaClass.simpleName}")
        }
    }
}
