package com.pasindu.nursingotapp.transfer.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.WorkerDirectMatch
import com.pasindu.nursingotapp.transfer.data.model.WorkerThreeWayMatch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
    val matchType: String = "DIRECT_2_WAY",
    val directMatch: WorkerDirectMatch? = null,
    val threeWayMatch: WorkerThreeWayMatch? = null,
    val status: String,
    val createdAt: String,
    val expiresAt: String,
    val firstResponseAt: String? = null,
    val chatDeadline: String? = null,
    val acceptedByA: Boolean = false,
    val acceptedByB: Boolean = false,
    val acceptedByC: Boolean = false,
    val rejectedByA: Boolean = false,
    val rejectedByB: Boolean = false,
    val rejectedByC: Boolean = false,
    val confirmedByA: Boolean = false,
    val confirmedByB: Boolean = false,
    val confirmedByC: Boolean = false
) {
    /**
     * Backward-compatibility secondary constructor for 2-way call sites passing match directly.
     */
    constructor(
        matchId: String,
        match: WorkerDirectMatch,
        status: String,
        createdAt: String,
        expiresAt: String,
        acceptedByA: Boolean = false,
        acceptedByB: Boolean = false,
        rejectedByA: Boolean = false,
        rejectedByB: Boolean = false,
        firstResponseAt: String? = null,
        chatDeadline: String? = null,
        confirmedByA: Boolean = false,
        confirmedByB: Boolean = false
    ) : this(
        matchId = matchId,
        matchType = "DIRECT_2_WAY",
        directMatch = match,
        threeWayMatch = null,
        status = status,
        createdAt = createdAt,
        expiresAt = expiresAt,
        firstResponseAt = firstResponseAt,
        chatDeadline = chatDeadline,
        acceptedByA = acceptedByA,
        acceptedByB = acceptedByB,
        acceptedByC = false,
        rejectedByA = rejectedByA,
        rejectedByB = rejectedByB,
        rejectedByC = false,
        confirmedByA = confirmedByA,
        confirmedByB = confirmedByB,
        confirmedByC = false
    )

    // Backward-compatibility accessor for 2-way call sites
    val match: WorkerDirectMatch
        get() = directMatch ?: error("Expected direct 2-way match but was $matchType")
}

interface TransferRemoteDataSource {
    suspend fun fetchTransferRequest(userId: String): Result<RemoteTransferRequestState?>
    suspend fun fetchMatchDoc(matchId: String): Result<RemoteMatchState?>
    fun observeMatchDoc(matchId: String): kotlinx.coroutines.flow.Flow<Result<RemoteMatchState?>>
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

        val updatedAt = readUpdatedAtMillis(snapshot)

        RemoteTransferRequestState(
            status = snapshot.getString("status").orEmpty(),
            locked = snapshot.getBoolean("locked") ?: false,
            currentMatchId = snapshot.getString("currentMatchId"),
            updatedAt = updatedAt
        )
    }

    /**
     * Transfer-request updatedAt has existed in numeric and ISO-8601 string
     * form while the server-side matching bridge was being hardened.
     *
     * Read both representations so an older document cannot break the
     * synchronization loop. New Worker writes use numeric epoch millis.
     */
    private fun readUpdatedAtMillis(snapshot: DocumentSnapshot): Long? {
        return when (val value = snapshot.get("updatedAt")) {
            is Number -> value.toLong()
            is Timestamp -> value.toDate().time
            is String -> value.toLongOrNull()
                ?: runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            else -> null
        }
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
        fun bool(name: String): Boolean = snapshot.getBoolean(name) ?: false

        val nurseAUid = string("nurseAUid")
        val nurseBUid = string("nurseBUid")
        val nurseCUid = string("nurseCUid")

        val acceptedByA = bool("acceptedByA")
        val acceptedByB = bool("acceptedByB")
        val acceptedByC = bool("acceptedByC")
        val rejectedByA = bool("rejectedByA")
        val rejectedByB = bool("rejectedByB")
        val rejectedByC = bool("rejectedByC")

        val is3Way = nurseCUid.isNotBlank() ||
            snapshot.contains("acceptedByC") ||
            snapshot.contains("rejectedByC")

        val firstResponseAt = snapshot.getString("firstResponseAt")
        val chatDeadline = snapshot.getString("chatDeadline")
        val confirmedByA = bool("confirmedByA")
        val confirmedByB = bool("confirmedByB")
        val confirmedByC = bool("confirmedByC")

        if (is3Way) {
            val nurseACurrentHospitalId = string("nurseACurrentHospitalId")
            val nurseBCurrentHospitalId = string("nurseBCurrentHospitalId")
            val nurseCCurrentHospitalId = string("nurseCCurrentHospitalId")
            val nurseADestinationHospitalId = string("nurseADestinationHospitalId")
            val nurseBDestinationHospitalId = string("nurseBDestinationHospitalId")
            val nurseCDestinationHospitalId = string("nurseCDestinationHospitalId")
            val nurseAGrade = string("nurseAGrade")
            val nurseBGrade = string("nurseBGrade")
            val nurseCGrade = string("nurseCGrade")
            val priorityReason = string("priorityReason")

            require(
                nurseAUid.isNotBlank() &&
                nurseBUid.isNotBlank() &&
                nurseCUid.isNotBlank() &&
                nurseACurrentHospitalId.isNotBlank() &&
                nurseBCurrentHospitalId.isNotBlank() &&
                nurseCCurrentHospitalId.isNotBlank() &&
                nurseADestinationHospitalId.isNotBlank() &&
                nurseBDestinationHospitalId.isNotBlank() &&
                nurseCDestinationHospitalId.isNotBlank() &&
                nurseAGrade.isNotBlank() &&
                nurseBGrade.isNotBlank() &&
                nurseCGrade.isNotBlank()
            ) {
                "Malformed remote 3-way match document"
            }

            val threeWayMatch = WorkerThreeWayMatch(
                nurseAUid = nurseAUid,
                nurseBUid = nurseBUid,
                nurseCUid = nurseCUid,
                nurseACurrentHospitalId = nurseACurrentHospitalId,
                nurseBCurrentHospitalId = nurseBCurrentHospitalId,
                nurseCCurrentHospitalId = nurseCCurrentHospitalId,
                nurseADestinationHospitalId = nurseADestinationHospitalId,
                nurseBDestinationHospitalId = nurseBDestinationHospitalId,
                nurseCDestinationHospitalId = nurseCDestinationHospitalId,
                nurseAGrade = nurseAGrade,
                nurseBGrade = nurseBGrade,
                nurseCGrade = nurseCGrade,
                isAllSameGrade = bool("isAllSameGrade"),
                nurseAPreferenceRank = int("nurseAPreferenceRank"),
                nurseBPreferenceRank = int("nurseBPreferenceRank"),
                nurseCPreferenceRank = int("nurseCPreferenceRank"),
                combinedPreferenceRank = int("combinedPreferenceRank"),
                priorityReason = priorityReason
            )

            RemoteMatchState(
                matchId = matchId,
                matchType = "THREE_WAY",
                threeWayMatch = threeWayMatch,
                status = string("status"),
                createdAt = string("createdAt"),
                expiresAt = string("expiresAt"),
                firstResponseAt = firstResponseAt,
                chatDeadline = chatDeadline,
                acceptedByA = acceptedByA,
                acceptedByB = acceptedByB,
                acceptedByC = acceptedByC,
                rejectedByA = rejectedByA,
                rejectedByB = rejectedByB,
                rejectedByC = rejectedByC,
                confirmedByA = confirmedByA,
                confirmedByB = confirmedByB,
                confirmedByC = confirmedByC
            )
        } else {
            val directMatch = WorkerDirectMatch(
                nurseAUid = nurseAUid,
                nurseBUid = nurseBUid,
                nurseACurrentHospitalId = string("nurseACurrentHospitalId"),
                nurseBCurrentHospitalId = string("nurseBCurrentHospitalId"),
                nurseADestinationHospitalId = string("nurseADestinationHospitalId"),
                nurseBDestinationHospitalId = string("nurseBDestinationHospitalId"),
                nurseAGrade = string("nurseAGrade"),
                nurseBGrade = string("nurseBGrade"),
                isSameGrade = bool("isSameGrade"),
                nurseAPreferenceRank = int("nurseAPreferenceRank"),
                nurseBPreferenceRank = int("nurseBPreferenceRank"),
                combinedPreferenceRank = int("combinedPreferenceRank"),
                priorityReason = string("priorityReason")
            )

            require(directMatch.nurseAUid.isNotBlank() && directMatch.nurseBUid.isNotBlank()) {
                "Malformed remote 2-way match document"
            }

            RemoteMatchState(
                matchId = matchId,
                matchType = "DIRECT_2_WAY",
                directMatch = directMatch,
                status = string("status"),
                createdAt = string("createdAt"),
                expiresAt = string("expiresAt"),
                firstResponseAt = firstResponseAt,
                chatDeadline = chatDeadline,
                acceptedByA = acceptedByA,
                acceptedByB = acceptedByB,
                rejectedByA = rejectedByA,
                rejectedByB = rejectedByB,
                confirmedByA = confirmedByA,
                confirmedByB = confirmedByB
            )
        }
    }

    override fun observeMatchDoc(matchId: String): Flow<Result<RemoteMatchState?>> = callbackFlow {
        if (matchId.isBlank()) {
            trySend(Result.failure(IllegalArgumentException("matchId must not be blank")))
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("matches")
            .document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(Result.success(null))
                    return@addSnapshotListener
                }

                val parsed = runCatching { parseMatchSnapshot(matchId, snapshot) }
                trySend(parsed)
            }

        awaitClose {
            registration.remove()
        }
    }

    private fun parseMatchSnapshot(matchId: String, snapshot: DocumentSnapshot): RemoteMatchState {
        fun string(name: String): String = snapshot.getString(name).orEmpty()
        fun int(name: String): Int = (snapshot.getLong(name) ?: 0L).toInt()
        fun bool(name: String): Boolean = snapshot.getBoolean(name) ?: false

        val nurseAUid = string("nurseAUid")
        val nurseBUid = string("nurseBUid")
        val nurseCUid = string("nurseCUid")

        val acceptedByA = bool("acceptedByA")
        val acceptedByB = bool("acceptedByB")
        val acceptedByC = bool("acceptedByC")
        val rejectedByA = bool("rejectedByA")
        val rejectedByB = bool("rejectedByB")
        val rejectedByC = bool("rejectedByC")

        val is3Way = nurseCUid.isNotBlank() ||
            snapshot.contains("acceptedByC") ||
            snapshot.contains("rejectedByC")

        val firstResponseAt = snapshot.getString("firstResponseAt")
        val chatDeadline = snapshot.getString("chatDeadline")
        val confirmedByA = bool("confirmedByA")
        val confirmedByB = bool("confirmedByB")
        val confirmedByC = bool("confirmedByC")

        return if (is3Way) {
            val threeWayMatch = WorkerThreeWayMatch(
                nurseAUid = nurseAUid,
                nurseBUid = nurseBUid,
                nurseCUid = nurseCUid,
                nurseACurrentHospitalId = string("nurseACurrentHospitalId"),
                nurseBCurrentHospitalId = string("nurseBCurrentHospitalId"),
                nurseCCurrentHospitalId = string("nurseCCurrentHospitalId"),
                nurseADestinationHospitalId = string("nurseADestinationHospitalId"),
                nurseBDestinationHospitalId = string("nurseBDestinationHospitalId"),
                nurseCDestinationHospitalId = string("nurseCDestinationHospitalId"),
                nurseAGrade = string("nurseAGrade"),
                nurseBGrade = string("nurseBGrade"),
                nurseCGrade = string("nurseCGrade"),
                isAllSameGrade = bool("isAllSameGrade"),
                nurseAPreferenceRank = int("nurseAPreferenceRank"),
                nurseBPreferenceRank = int("nurseBPreferenceRank"),
                nurseCPreferenceRank = int("nurseCPreferenceRank"),
                combinedPreferenceRank = int("combinedPreferenceRank"),
                priorityReason = string("priorityReason")
            )

            RemoteMatchState(
                matchId = matchId,
                matchType = "THREE_WAY",
                threeWayMatch = threeWayMatch,
                status = string("status"),
                createdAt = string("createdAt"),
                expiresAt = string("expiresAt"),
                firstResponseAt = firstResponseAt,
                chatDeadline = chatDeadline,
                acceptedByA = acceptedByA,
                acceptedByB = acceptedByB,
                acceptedByC = acceptedByC,
                rejectedByA = rejectedByA,
                rejectedByB = rejectedByB,
                rejectedByC = rejectedByC,
                confirmedByA = confirmedByA,
                confirmedByB = confirmedByB,
                confirmedByC = confirmedByC
            )
        } else {
            val directMatch = WorkerDirectMatch(
                nurseAUid = nurseAUid,
                nurseBUid = nurseBUid,
                nurseACurrentHospitalId = string("nurseACurrentHospitalId"),
                nurseBCurrentHospitalId = string("nurseBCurrentHospitalId"),
                nurseADestinationHospitalId = string("nurseADestinationHospitalId"),
                nurseBDestinationHospitalId = string("nurseBDestinationHospitalId"),
                nurseAGrade = string("nurseAGrade"),
                nurseBGrade = string("nurseBGrade"),
                isSameGrade = bool("isSameGrade"),
                nurseAPreferenceRank = int("nurseAPreferenceRank"),
                nurseBPreferenceRank = int("nurseBPreferenceRank"),
                combinedPreferenceRank = int("combinedPreferenceRank"),
                priorityReason = string("priorityReason")
            )

            RemoteMatchState(
                matchId = matchId,
                matchType = "DIRECT_2_WAY",
                directMatch = directMatch,
                status = string("status"),
                createdAt = string("createdAt"),
                expiresAt = string("expiresAt"),
                firstResponseAt = firstResponseAt,
                chatDeadline = chatDeadline,
                acceptedByA = acceptedByA,
                acceptedByB = acceptedByB,
                rejectedByA = rejectedByA,
                rejectedByB = rejectedByB,
                confirmedByA = confirmedByA,
                confirmedByB = confirmedByB
            )
        }
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
