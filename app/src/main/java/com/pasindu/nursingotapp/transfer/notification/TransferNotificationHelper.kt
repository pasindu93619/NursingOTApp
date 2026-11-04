package com.pasindu.nursingotapp.transfer.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pasindu.nursingotapp.MainActivity
import com.pasindu.nursingotapp.R

data class TransferNotificationPayload(
    val title: String,
    val body: String,
    val type: String,
    val matchId: String? = null
)

object TransferNotificationHelper {
    const val CHANNEL_ID = "nursing_transfer_alerts"
    const val CHANNEL_NAME = "Mutual Transfer Alerts"
    const val CHANNEL_DESCRIPTION = "Notifications for mutual transfer matches, responses, and coordination updates"
    const val NOTIFICATION_ID = 202610

    const val EXTRA_MATCH_ID = "EXTRA_MATCH_ID"
    const val EXTRA_NOTIFICATION_TYPE = "EXTRA_NOTIFICATION_TYPE"

    const val TYPE_MATCH_FOUND = "MATCH_FOUND"
    const val TYPE_PARTNER_ACCEPTED = "PARTNER_ACCEPTED"
    const val TYPE_CHAT_MESSAGE = "CHAT_MESSAGE"
    const val TYPE_MATCH_CONFIRMED = "MATCH_CONFIRMED"
    const val TYPE_MATCH_EXPIRED = "MATCH_EXPIRED"

    /**
     * Resolves an incoming notification type to an existing app navigation route.
     * Both transfer_chat and transfer_match destinations are bound to the signed-in nurse's
     * single active transfer request and load the authoritative match via repository sync.
     * Returns null if type is blank, unknown, or has no valid destination.
     */
    fun resolveNotificationDestination(type: String?): String? {
        return when (type?.trim()?.uppercase()) {
            TYPE_CHAT_MESSAGE -> "transfer_chat"
            TYPE_MATCH_FOUND,
            TYPE_PARTNER_ACCEPTED,
            TYPE_MATCH_CONFIRMED,
            TYPE_MATCH_EXPIRED -> "transfer_match"
            else -> null
        }
    }

    /**
     * Parses incoming FCM data map and fallback notification fields into a structured model.
     * Pure function for deterministic testing and zero crash risk.
     */
    fun parsePayload(
        data: Map<String, String>,
        fallbackTitle: String? = null,
        fallbackBody: String? = null
    ): TransferNotificationPayload {
        val type = data["type"]?.trim()?.uppercase() ?: "GENERAL"
        val matchId = data["matchId"]?.trim()?.takeIf { it.isNotEmpty() }

        val title = when {
            !fallbackTitle.isNullOrBlank() -> fallbackTitle.trim()
            data["title"]?.isNotBlank() == true -> data["title"]!!.trim()
            type == TYPE_MATCH_FOUND -> "Mutual Transfer Match Found"
            type == TYPE_PARTNER_ACCEPTED -> "Partner Accepted Transfer"
            type == TYPE_CHAT_MESSAGE -> "New Transfer Message"
            type == TYPE_MATCH_CONFIRMED -> "Mutual Transfer Confirmed"
            type == TYPE_MATCH_EXPIRED -> "Transfer Match Expired"
            else -> "Mutual Transfer Update"
        }

        val body = when {
            !fallbackBody.isNullOrBlank() -> fallbackBody.trim()
            data["body"]?.isNotBlank() == true -> data["body"]!!.trim()
            type == TYPE_MATCH_FOUND -> "A compatible transfer route has been found. Open the app to review and respond."
            type == TYPE_PARTNER_ACCEPTED -> "Your transfer partner accepted the match. Please confirm to proceed."
            type == TYPE_CHAT_MESSAGE -> "You have a new message from your transfer coordination team."
            type == TYPE_MATCH_CONFIRMED -> "All participants confirmed. Your transfer application is ready for submission."
            type == TYPE_MATCH_EXPIRED -> "A match window has expired. Your request has returned to the pool."
            else -> "Check the app for recent mutual transfer updates."
        }

        return TransferNotificationPayload(
            title = title,
            body = body,
            type = type,
            matchId = matchId
        )
    }

    /**
     * Ensures the notification channel exists on API 26+.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val existing = notificationManager?.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Shows a local notification if permissions and settings allow it.
     */
    fun showNotification(context: Context, payload: TransferNotificationPayload) {
        try {
            // Check Android 13+ runtime permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            // Check if user disabled notifications in system settings
            val managerCompat = NotificationManagerCompat.from(context)
            if (!managerCompat.areNotificationsEnabled()) return

            createNotificationChannel(context)

            // Prepare safe launch intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!payload.matchId.isNullOrBlank()) {
                    putExtra(EXTRA_MATCH_ID, payload.matchId)
                }
                putExtra(EXTRA_NOTIFICATION_TYPE, payload.type)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(payload.title)
                .setContentText(payload.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            managerCompat.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.w("TransferNotification", "Permission revoked concurrently while showing notification: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.w("TransferNotification", "Failed to display mutual transfer notification: ${e.javaClass.simpleName}")
        }
    }
}
