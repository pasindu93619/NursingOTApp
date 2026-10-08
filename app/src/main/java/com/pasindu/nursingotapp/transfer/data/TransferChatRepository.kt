package com.pasindu.nursingotapp.transfer.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.pasindu.nursingotapp.transfer.data.model.TransferChatError
import com.pasindu.nursingotapp.transfer.data.model.TransferChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface TransferChatRepository {
    /**
     * Realtime observation of messages for a match, ordered chronologically.
     */
    fun observeMessages(matchId: String): Flow<Result<List<TransferChatMessage>>>

    /**
     * Validates and sends a new mutual-transfer coordination message.
     */
    suspend fun sendMessage(
        matchId: String,
        text: String,
        senderHospitalId: String,
        senderGrade: String
    ): Result<TransferChatMessage>
}

@Singleton
class FirestoreTransferChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val tokenProvider: TransferTokenProvider
) : TransferChatRepository {

    override fun observeMessages(matchId: String): Flow<Result<List<TransferChatMessage>>> = callbackFlow {
        if (matchId.isBlank()) {
            trySend(Result.failure(TransferChatError.InvalidMessage("Match ID must not be blank.")))
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("matches")
            .document(matchId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(mapFirestoreException(error)))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val messageId = doc.getString("messageId") ?: doc.id
                        val docMatchId = doc.getString("matchId").orEmpty()
                        val senderUid = doc.getString("senderUid").orEmpty()
                        val senderHospitalId = doc.getString("senderHospitalId").orEmpty()
                        val senderGrade = doc.getString("senderGrade").orEmpty()
                        val text = doc.getString("text").orEmpty()
                        val timestamp = doc.getTimestamp("createdAt")
                        val hasPendingWrites = doc.metadata.hasPendingWrites()

                        val createdAtMillis = timestamp?.toDate()?.time

                        TransferChatMessage(
                            messageId = messageId,
                            matchId = if (docMatchId.isNotBlank()) docMatchId else matchId,
                            senderUid = senderUid,
                            senderHospitalId = senderHospitalId,
                            senderGrade = senderGrade,
                            text = text,
                            createdAtMillis = createdAtMillis,
                            hasPendingWrites = hasPendingWrites
                        )
                    }
                    trySend(Result.success(messages))
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun sendMessage(
        matchId: String,
        text: String,
        senderHospitalId: String,
        senderGrade: String
    ): Result<TransferChatMessage> {
        val trimmed = text.trim()
        if (matchId.isBlank()) {
            return Result.failure(TransferChatError.InvalidMessage("Match ID cannot be blank."))
        }
        if (trimmed.isEmpty()) {
            return Result.failure(TransferChatError.InvalidMessage("Message cannot be empty."))
        }
        if (trimmed.length > 500) {
            return Result.failure(TransferChatError.InvalidMessage("Message exceeds 500 characters."))
        }
        if (senderHospitalId.isBlank()) {
            return Result.failure(TransferChatError.InvalidMessage("Sender hospital ID is required."))
        }
        if (senderGrade.isBlank()) {
            return Result.failure(TransferChatError.InvalidMessage("Sender grade is required."))
        }

        val currentUserId = tokenProvider.getCurrentUserId()
            ?: return Result.failure(TransferChatError.Unauthenticated)

        val messageId = UUID.randomUUID().toString()

        val payload = mapOf(
            "messageId" to messageId,
            "matchId" to matchId,
            "senderUid" to currentUserId,
            "senderHospitalId" to senderHospitalId.trim(),
            "senderGrade" to senderGrade.trim(),
            "text" to trimmed,
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            firestore.collection("matches")
                .document(matchId)
                .collection("messages")
                .document(messageId)
                .set(payload)
                .await()

            Result.success(
                TransferChatMessage(
                    messageId = messageId,
                    matchId = matchId,
                    senderUid = currentUserId,
                    senderHospitalId = senderHospitalId.trim(),
                    senderGrade = senderGrade.trim(),
                    text = trimmed,
                    createdAtMillis = System.currentTimeMillis(),
                    hasPendingWrites = true
                )
            )
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    private fun mapFirestoreException(e: Exception): TransferChatError {
        if (e is FirebaseFirestoreException) {
            return when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    TransferChatError.PermissionDenied
                }
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                    TransferChatError.Unauthenticated
                }
                FirebaseFirestoreException.Code.UNAVAILABLE -> {
                    TransferChatError.NetworkUnavailable(e.message ?: "Firestore service unavailable.")
                }
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> {
                    TransferChatError.DeadlineExceededOrClosed
                }
                else -> {
                    TransferChatError.Unknown(e.message ?: "Firestore error (${e.code})", e)
                }
            }
        }
        return TransferChatError.Unknown(e.message ?: "Unexpected error", e)
    }
}
