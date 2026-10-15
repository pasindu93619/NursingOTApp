package com.pasindu.nursingotapp.transfer.data.model

import kotlinx.serialization.Serializable

/**
 * Immutable domain model representing a single mutual-transfer coordination message.
 *
 * Mapped strictly to the Firestore path:
 * /matches/{matchId}/messages/{messageId}
 */
@Serializable
data class TransferChatMessage(
    val messageId: String,
    val matchId: String,
    val senderUid: String,
    val senderHospitalId: String,
    val senderGrade: String,
    val text: String,
    val createdAtMillis: Long? = null,
    val hasPendingWrites: Boolean = false
)

/**
 * Domain-level error hierarchy isolating UI and consumers from raw Firebase exceptions.
 */
sealed class TransferChatError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object Unauthenticated : TransferChatError("User is not authenticated.")
    data class InvalidMessage(val reason: String) : TransferChatError(reason)
    data object PermissionDenied : TransferChatError("Access denied: You are not an authorized participant of this match or the chat window is closed.")
    data object DeadlineExceededOrClosed : TransferChatError("The communication window is closed or deadline has passed.")
    data class NetworkUnavailable(val details: String) : TransferChatError(details)
    data class Unknown(val details: String, override val cause: Throwable? = null) : TransferChatError(details, cause)
}
