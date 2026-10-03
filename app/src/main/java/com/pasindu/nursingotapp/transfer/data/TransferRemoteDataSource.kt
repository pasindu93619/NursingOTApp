package com.pasindu.nursingotapp.transfer.data

import com.google.firebase.firestore.FirebaseFirestore
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source interacting directly with Firestore for transferRequests publication.
 *
 * Enforces the Phase 1.5.3C-2 Firestore security contract:
 * - Client can only write its own document under /transferRequests/{userId}.
 * - Sets status = 'SEARCHING', locked = false, currentMatchId = null.
 * - Does not perform matching or locking (reserved strictly for Cloudflare Worker).
 */
interface TransferRemoteDataSource {
    suspend fun publishTransferRequest(userId: String, request: TransferRequest): Result<Unit>
    suspend fun withdrawTransferRequest(userId: String): Result<Unit>
}

@Singleton
class FirestoreTransferRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : TransferRemoteDataSource {

    override suspend fun publishTransferRequest(
        userId: String,
        request: TransferRequest
    ): Result<Unit> = runCatching {
        require(userId.isNotBlank()) { "userId must not be blank" }

        val data = mapOf(
            "firebaseUid" to userId,
            "status" to "SEARCHING",
            "locked" to false,
            "currentMatchId" to null,
            "currentHospitalId" to request.currentHospitalId,
            "preferenceHospitalIds" to request.rankedPreferences.hospitalIds,
            "grade" to request.grade,
            "updatedAt" to request.updatedAt
        )

        firestore.collection("transferRequests")
            .document(userId)
            .set(data)
            .await()
    }

    override suspend fun withdrawTransferRequest(userId: String): Result<Unit> = runCatching {
        require(userId.isNotBlank()) { "userId must not be blank" }

        firestore.collection("transferRequests")
            .document(userId)
            .update(
                mapOf(
                    "status" to "WITHDRAWN",
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }
}
