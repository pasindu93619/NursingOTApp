package com.pasindu.nursingotapp.transfer.data

import com.google.firebase.firestore.FirebaseFirestore
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.WorkerDirectMatch
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
data class RemoteTransferRequestState(
    val status: String,
    val locked: Boolean,
    val currentMatchId: String?,
    val updatedAt: Long?
)

data class RemoteMatchState(
    val matchId: String,
    val match: WorkerDirectMatch,
    val status: String,
    val createdAt: String,
    val expiresAt: String
)

interface TransferRemoteDataSource {
    suspend fun fetchTransferRequest(userId: String): Result<RemoteTransferRequestState?>
    suspend fun fetchMatchDoc(matchId: String): Result<RemoteMatchState?>
    suspend fun publishTransferRequest(userId: String, request: TransferRequest): Result<Unit>
    suspend fun withdrawTransferRequest(userId: String): Result<Unit>
}

@Singleton
class FirestoreTransferRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : TransferRemoteDataSource {

    override suspend fun fetchTransferRequest(userId: String): Result<RemoteTransferRequestState?> = runCatching {
        require(userId.isNotBlank()) { "userId must not be blank" }

        val snapshot = firestore.collection("transferRequests")
            .document(userId)
            .get()
            .await()

        if (!snapshot.exists()) return@runCatching null

        RemoteTransferRequestState(
            status = snapshot.getString("status").orEmpty(),
            locked = snapshot.getBoolean("locked") ?: false,
            currentMatchId = snapshot.getString("currentMatchId"),
            updatedAt = snapshot.getLong("updatedAt")
        )
    }

    override suspend fun fetchMatchDoc(matchId: String): Result<RemoteMatchState?> = runCatching {
        require(matchId.isNotBlank()) { "matchId must not be blank" }

        val snapshot = firestore.collection("matches")
            .document(matchId)
            .get()
            .await()

        if (!snapshot.exists()) return@runCatching null

        fun string(name: String): String = snapshot.getString(name).orEmpty()
        fun int(name: String): Int = (snapshot.getLong(name) ?: 0L).toInt()

        val match = WorkerDirectMatch(
            nurseAUid = string("nurseAUid"),
            nurseBUid = string("nurseBUid"),
            nurseACurrentHospitalId = string("nurseACurrentHospitalId"),
            nurseBCurrentHospitalId = string("nurseBCurrentHospitalId"),
            nurseADestinationHospitalId = string("nurseADestinationHospitalId"),
            nurseBDestinationHospitalId = string("nurseBDestinationHospitalId"),
            nurseAGrade = string("nurseAGrade"),
            nurseBGrade = string("nurseBGrade"),
            isSameGrade = snapshot.getBoolean("isSameGrade") ?: false,
            nurseAPreferenceRank = int("nurseAPreferenceRank"),
            nurseBPreferenceRank = int("nurseBPreferenceRank"),
            combinedPreferenceRank = int("combinedPreferenceRank"),
            priorityReason = string("priorityReason")
        )

        require(match.nurseAUid.isNotBlank() && match.nurseBUid.isNotBlank()) {
            "Malformed remote match document"
        }

        RemoteMatchState(
            matchId = matchId,
            match = match,
            status = string("status"),
            createdAt = string("createdAt"),
            expiresAt = string("expiresAt")
        )
    }

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
