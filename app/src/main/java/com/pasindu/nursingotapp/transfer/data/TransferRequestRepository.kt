package com.pasindu.nursingotapp.transfer.data

import android.util.Log

import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.TransferActiveCacheDao
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity
import com.pasindu.nursingotapp.transfer.data.model.CacheSyncStatus
import com.pasindu.nursingotapp.transfer.data.model.Decision
import com.pasindu.nursingotapp.transfer.data.model.DecisionRequest
import com.pasindu.nursingotapp.transfer.data.model.DecisionResponse
import com.pasindu.nursingotapp.transfer.data.model.RankedPreferences
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local Room-backed repository for the nurse's own active Mutual Transfer request
 * with Cloudflare Worker and Firestore synchronization bridge.
 *
 * Ranked preference order is guaranteed: the hospital IDs are stored as a
 * JSON ARRAY and deserialised back in the same order. A Set is never used.
 *
 * Offline-first writes via Room + WorkManager:
 * Every write persists immediately to Room with syncStatus = PENDING.
 * Remote synchronization publishes the request to Firestore and triggers
 * Cloudflare Worker matching without blocking the UI.
 */
class TransferRequestRepository @Inject constructor(
    private val dao: TransferActiveCacheDao,
    private val profileDao: ProfileDao,
    private val tokenProvider: TransferTokenProvider? = null,
    private val remoteDataSource: TransferRemoteDataSource? = null,
    private val workerApiClient: TransferWorkerApiClient? = null
) {

    companion object {
        private const val TAG = "TransferSync"
    }

    // ---------------------------------------------------------------------------
    // JSON configuration — plain array, no surrounding whitespace.
    // ---------------------------------------------------------------------------

    private val json = Json { encodeDefaults = true }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Observes the nurse's active transfer request as a [Flow].
     *
     * Emits `null` when:
     * - The cache row has never been written.
     * - The cached entity has a blank [TransferActiveCacheEntity.currentHospitalId]
     *   or a blank/null [TransferActiveCacheEntity.requestStatus].
     * - The preference list cannot be deserialised.
     *
     * Emits a [TransferRequest] on every subsequent Room write.
     */
    fun observeActiveRequest(): Flow<TransferRequest?> =
        dao.observe().map { entity -> entity?.toDomain() }

    /**
     * Returns the current active request snapshot, or `null` if none exists.
     *
     * This is a one-shot suspend read. Prefer [observeActiveRequest] for
     * reactive UI state.
     */
    suspend fun getActiveRequest(): TransferRequest? =
        dao.getOnce()?.toDomain()

    /**
     * Returns the authenticated Firebase UID used to orient a server-created
     * mutual-transfer match to the signed-in nurse.
     */
    suspend fun getCurrentUserId(): String? = tokenProvider?.getCurrentUserId()

    /**
     * Persists a new or updated transfer request to Room.
     *
     * Sets [CacheSyncStatus.PENDING] on every write so that a future Firestore
     * sync step can detect local changes.
     *
     * @param currentHospitalId Canonical [HospitalReference.hospitalId] of the
     *   nurse's current posting. Must not be blank.
     * @param rankedPreferences Ordered preferred transfer destinations. The list
     *   order is preserved exactly — index 0 = 1st preference.
     *
     * @throws IllegalArgumentException if [currentHospitalId] is blank.
     * @throws IllegalArgumentException if [RankedPreferences] validation fails
     *   (caller should validate before calling).
     * @throws Exception if the Room write fails (disk, DB error, etc.).
     */
    suspend fun saveRequest(
        currentHospitalId: String,
        rankedPreferences: RankedPreferences
    ) {
        require(currentHospitalId.isNotBlank()) {
            "currentHospitalId must not be blank"
        }

        val profile = profileDao.getProfileOnce()
        val profileGrade = profile?.grade?.trim().orEmpty()
        require(profileGrade.isNotBlank()) {
            "A valid nursing grade is required in your profile before submitting a transfer request"
        }

        // Never replace an active matched cache row with a fresh PENDING request.
        // That would erase matchCycleId/matchStatus/matchPayloadJson before the
        // server's lock rejection arrives, making the UI falsely look unmatched.
        val existing = dao.getOnce()
        val existingMatchStatus = existing?.matchStatus?.trim()?.uppercase()
        val hasActiveLocalMatch =
            existing?.requestStatus.equals(TransferRequestStatus.MATCHED.name, ignoreCase = true) &&
                existingMatchStatus != "CANCELLED" &&
                existingMatchStatus != "EXPIRED"
        check(!hasActiveLocalMatch) {
            "Your request is locked while a match is active. Cancel or finish the match before editing preferences."
        }

        val preferenceJson = serializePreferences(rankedPreferences)
        val nowMs = System.currentTimeMillis()

        // Preserve the existing requestId if there is already a cached row.
        val existingRequestId = existing?.requestId

        val entity = TransferActiveCacheEntity(
            id = 1,
            requestId = existingRequestId,
            requestStatus = TransferRequestStatus.PENDING.name,
            currentHospitalId = currentHospitalId,
            preferenceHospitalIdsJson = preferenceJson,
            grade = profileGrade,
            matchCycleId = null,
            matchType = null,
            matchStatus = null,
            matchPayloadJson = null,
            syncStatus = CacheSyncStatus.PENDING.name,
            updatedAt = nowMs
        )

        // This upserts (INSERT OR REPLACE) into Room. Any Room exception propagates.
        dao.upsert(entity)
    }

    /**
     * Updates an existing active request with a new set of ranked preferences
     * and/or current hospital, preserving the existing [requestId] and request status.
     *
     * If no active request exists in Room, this behaves identically to [saveRequest].
     *
     * @throws IllegalArgumentException if [currentHospitalId] is blank.
     * @throws Exception if the Room write fails.
     */
    suspend fun updateRequest(
        currentHospitalId: String,
        rankedPreferences: RankedPreferences
    ) {
        // Delegates to saveRequest — the upsert already reads existing requestId.
        saveRequest(currentHospitalId, rankedPreferences)
    }

    /**
     * Synchronizes the active Room transfer request with Firestore and Cloudflare Worker.
     *
     * Invariants:
     * 1. Reads local request from Room. If none, returns NoMatch.
     * 2. Publishes/updates /transferRequests/{userId} on Firestore.
     * 3. Calls Worker POST /api/matching/find-and-lock using Bearer token.
     * 4. On match: caches match details in [TransferActiveCacheEntity] and sets SYNCED.
     * 5. On no match: sets SYNCED (request remains active in pool).
     * 6. On failure: does NOT delete Room request; retains local data and marks ERROR.
     */
    suspend fun syncActiveRequest(): WorkerSyncResult {
        val cachedEntity = dao.getOnce()
            ?: return WorkerSyncResult.NoMatch("No active local transfer request")

        /*
         * The ProfileEntity is the authoritative local source for the nurse's
         * selected nursing grade. Older Room transfer-cache rows can pre-date
         * the grade bridge and therefore contain a blank/stale grade.
         *
         * Reconcile the cached grade before converting to the domain model so
         * the request published to Firestore always reflects the current
         * profile selection (for example, "Grade III").
         */
        val profileGrade = profileDao.getProfileOnce()?.grade?.trim().orEmpty()
        val entity = if (
            profileGrade.isNotBlank() &&
            !profileGrade.equals(cachedEntity.grade?.trim(), ignoreCase = false)
        ) {
            cachedEntity.copy(
                grade = profileGrade,
                syncStatus = CacheSyncStatus.PENDING.name,
                updatedAt = System.currentTimeMillis()
            ).also { dao.upsert(it) }
        } else {
            cachedEntity
        }

        val domain = entity.toDomain()
            ?: return WorkerSyncResult.NoMatch("Invalid local transfer request")

        if (domain.requestStatus == TransferRequestStatus.COMPLETED ||
            domain.requestStatus == TransferRequestStatus.WITHDRAWN
        ) {
            return WorkerSyncResult.NoMatch("Request is not active")
        }

        val tp = tokenProvider ?: return WorkerSyncResult.AuthError("Token provider not configured")
        val rds = remoteDataSource ?: return WorkerSyncResult.NetworkError("Remote data source not configured")
        val api = workerApiClient ?: return WorkerSyncResult.NetworkError("Worker API client not configured")

        val userId = tp.getCurrentUserId()
            ?: run {
                Log.e(TAG, "Authentication failure: Unable to get user ID")
                dao.upsert(entity.copy(syncStatus = CacheSyncStatus.ERROR.name))
                return WorkerSyncResult.AuthError("Authentication failure: Unable to get user ID")
            }

        Log.d(TAG, "syncActiveRequest: authenticated uid=$userId")

        // Read server-owned state before publishing local SEARCHING data.
        val remoteState = rds.fetchTransferRequest(userId).getOrElse { error ->
            Log.e(
                TAG,
                "Firestore fetchTransferRequest FAILED: ${error.javaClass.simpleName}: ${error.message}",
                error
            )
            dao.upsert(entity.copy(syncStatus = CacheSyncStatus.ERROR.name))
            return WorkerSyncResult.NetworkError(
                "Failed to read transfer request state from server: ${error.message}",
                error
            )
        }

        Log.d(
            TAG,
            "Firestore fetchTransferRequest OK: exists=${remoteState != null}, " +
                "status=${remoteState?.status}, locked=${remoteState?.locked}, " +
                "matchId=${remoteState?.currentMatchId != null}"
        )

        if (
            remoteState?.status.equals(TransferRequestStatus.MATCHED.name, ignoreCase = true) &&
            remoteState?.locked == true &&
            !remoteState.currentMatchId.isNullOrBlank()
        ) {
            val matchId = remoteState.currentMatchId!!
            val remoteMatch = rds.fetchMatchDoc(matchId).getOrElse { error ->
                dao.upsert(entity.copy(syncStatus = CacheSyncStatus.ERROR.name))
                return WorkerSyncResult.NetworkError(
                    "Match was found on server but could not be read: ${error.message}",
                    error
                )
            }

            if (remoteMatch == null) {
                dao.upsert(entity.copy(syncStatus = CacheSyncStatus.ERROR.name))
                return WorkerSyncResult.NetworkError(
                    "Server request is MATCHED but match document $matchId is missing"
                )
            }

            val matchJson = if (remoteMatch.matchType == "THREE_WAY" && remoteMatch.threeWayMatch != null) {
                json.encodeToString(remoteMatch.threeWayMatch)
            } else {
                json.encodeToString(remoteMatch.match)
            }
            val updated = entity.copy(
                requestId = userId,
                requestStatus = TransferRequestStatus.MATCHED.name,
                matchCycleId = matchId,
                matchType = remoteMatch.matchType,
                matchStatus = remoteMatch.status.ifBlank { "PENDING_CONFIRMATION" },
                matchPayloadJson = matchJson,
                syncStatus = CacheSyncStatus.SYNCED.name,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsert(updated)

            return WorkerSyncResult.MatchFound(
                matchId = matchId,
                matchType = remoteMatch.matchType,
                directMatch = remoteMatch.directMatch,
                threeWayMatch = remoteMatch.threeWayMatch,
                createdAt = remoteMatch.createdAt,
                expiresAt = remoteMatch.expiresAt,
                status = remoteMatch.status.ifBlank { "PENDING_CONFIRMATION" },
                firstResponseAt = remoteMatch.firstResponseAt,
                chatDeadline = remoteMatch.chatDeadline,
                confirmedByA = remoteMatch.confirmedByA,
                confirmedByB = remoteMatch.confirmedByB,
                confirmedByC = remoteMatch.confirmedByC
            )
        }

        /*
         * Server is not currently matched.
         *
         * Do not rewrite an already-SYNCED SEARCHING document on every
         * polling cycle. Every Firestore write changes updateTime and can
         * invalidate the Worker atomic precondition while the other nurse
         * is being matched. Publish only when the local request is pending
         * or changed, or when the server has no request yet.
         */
        /*
         * A transient Worker conflict must not force the Android client to
         * rewrite an already-searching server request. Rewriting that
         * document changes Firestore updateTime and can race the Worker
         * precondition. Publish only for a missing/non-searching server
         * request, an explicitly pending local change, or a local edit that
         * is newer than the server's known updatedAt.
         */
        val localSyncStatus = entity.syncStatus.trim().uppercase()
        val localHasPendingChanges = localSyncStatus == CacheSyncStatus.PENDING.name
        val localChangedSinceRemote = remoteState?.updatedAt?.let { remoteUpdatedAt ->
            entity.updatedAt > remoteUpdatedAt
        } == true

        val shouldPublishRemote = remoteState == null ||
            !remoteState.status.equals("SEARCHING", ignoreCase = true) ||
            localHasPendingChanges ||
            localChangedSinceRemote

        if (shouldPublishRemote) {
            Log.d(
                TAG,
                "Publishing transfer request: current=${domain.currentHospitalId}, " +
                    "preferences=${domain.rankedPreferences.hospitalIds.size}, grade=${domain.grade}"
            )
            val publishResult = rds.publishTransferRequest(userId, domain)
            if (publishResult.isFailure) {
                val err = publishResult.exceptionOrNull()
                Log.e(
                    TAG,
                    "Firestore publishTransferRequest FAILED: " +
                        "${err?.javaClass?.simpleName}: ${err?.message}",
                    err
                )
                dao.upsert(entity.copy(syncStatus = CacheSyncStatus.ERROR.name))
                return WorkerSyncResult.NetworkError(
                    "Failed to publish request to server: ${err?.message}",
                    err
                )
            }
            Log.d(TAG, "Firestore publishTransferRequest OK")
        } else {
            Log.d(
                TAG,
                "Remote request already SEARCHING and local cache is SYNCED; " +
                    "skipping redundant Firestore write before matching"
            )
        }

        val token = tp.getFirebaseIdToken()
            ?: run {
                Log.e(TAG, "Firebase ID token acquisition FAILED")
                dao.upsert(entity.copy(syncStatus = CacheSyncStatus.ERROR.name))
                return WorkerSyncResult.AuthError("Authentication failure: Unable to get ID token")
            }

        Log.d(TAG, "Firebase ID token acquired successfully")
        Log.d(TAG, "Calling Cloudflare matching Worker")
        val workerResult = api.findAndLockMatch(token)
        Log.d(TAG, "Cloudflare matching Worker returned: ${workerResult::class.simpleName}")

        when (workerResult) {
            is WorkerSyncResult.MatchFound -> {
                val matchJson = if (workerResult.matchType == "THREE_WAY" && workerResult.threeWayMatch != null) {
                    json.encodeToString(workerResult.threeWayMatch)
                } else {
                    json.encodeToString(workerResult.match)
                }
                val updated = entity.copy(
                    requestId = userId,
                    requestStatus = TransferRequestStatus.MATCHED.name,
                    matchCycleId = workerResult.matchId,
                    matchType = workerResult.matchType,
                    matchStatus = workerResult.status.ifBlank { "PENDING_CONFIRMATION" },
                    matchPayloadJson = matchJson,
                    syncStatus = CacheSyncStatus.SYNCED.name,
                    updatedAt = System.currentTimeMillis()
                )
                dao.upsert(updated)
            }
            is WorkerSyncResult.NoMatch -> {
                dao.upsert(
                    entity.copy(
                        requestId = userId,
                        syncStatus = CacheSyncStatus.SYNCED.name
                    )
                )
            }
            is WorkerSyncResult.Conflict,
            is WorkerSyncResult.AuthError,
            is WorkerSyncResult.NetworkError -> {
                dao.upsert(entity.copy(syncStatus = CacheSyncStatus.ERROR.name))
            }
        }

        return workerResult
    }

    /**
     * Clears the active request row from Room and authoritatively withdraws from the backend.
     *
     * After this call, [observeActiveRequest] will emit `null`.
     *
     * @throws Exception if backend withdrawal or Room delete fails.
     */
    suspend fun clearRequest() {
        val tp = tokenProvider
        val api = workerApiClient
        val remote = remoteDataSource

        if (api != null && tp != null) {
            val token = tp.getFirebaseIdToken()
                ?: throw IllegalStateException("Authentication failure: Unable to get ID token")
            api.withdrawRequest(token)
        } else if (remote != null && tp != null) {
            val userId = tp.getCurrentUserId()
            if (userId != null) {
                remote.withdrawTransferRequest(userId)
            }
        }
        dao.clear()
    }

    /**
     * Submits an ACCEPT or REJECT decision for the current active match to the Cloudflare Worker.
     * Updates the local Room cache with the authoritative status upon success.
     */
    suspend fun respondToMatch(matchId: String, decision: Decision): Result<DecisionResponse> {
        val tp = tokenProvider ?: return Result.failure(IllegalStateException("Token provider not configured"))
        val api = workerApiClient ?: return Result.failure(IllegalStateException("Worker API client not configured"))

        val token = tp.getFirebaseIdToken()
            ?: return Result.failure(IllegalStateException("Authentication failure: Unable to get ID token"))

        val response = runCatching {
            api.respondToMatch(token, DecisionRequest(matchId, decision))
        }.getOrElse { originalError ->
            // Reconcile with authoritative match state if the worker call failed
            if (remoteDataSource != null && (decision == Decision.LEAVE || decision == Decision.REJECT)) {
                val remoteMatch = remoteDataSource.fetchMatchDoc(matchId).getOrNull()
                if (remoteMatch != null && remoteMatch.status == "CANCELLED") {
                    DecisionResponse(
                        matchId = matchId,
                        newStatus = "CANCELLED",
                        expiresAt = remoteMatch.expiresAt,
                        chatDeadline = remoteMatch.chatDeadline,
                        firstResponseAt = remoteMatch.firstResponseAt,
                        decisionApplied = true
                    )
                } else {
                    return Result.failure(originalError)
                }
            } else {
                return Result.failure(originalError)
            }
        }

        val entity = dao.getOnce()
        if (entity != null) {
            // Guard: Stale actions must not corrupt a newer or different match
            val isTargetMatch = entity.matchCycleId == null || entity.matchCycleId == matchId
            if (isTargetMatch) {
                val updated = when (response.newStatus) {
                    "CANCELLED" -> entity.copy(
                        matchStatus = "CANCELLED",
                        requestStatus = TransferRequestStatus.PENDING.name,
                        matchCycleId = null,
                        matchType = null,
                        matchPayloadJson = null,
                        syncStatus = CacheSyncStatus.SYNCED.name,
                        updatedAt = System.currentTimeMillis()
                    )
                    "CONFIRMED" -> entity.copy(
                        matchStatus = "CONFIRMED",
                        requestStatus = TransferRequestStatus.MATCHED.name,
                        syncStatus = CacheSyncStatus.SYNCED.name,
                        updatedAt = System.currentTimeMillis()
                    )
                    else -> entity.copy(
                        matchStatus = response.newStatus,
                        syncStatus = CacheSyncStatus.SYNCED.name,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                dao.upsert(updated)
            }
        }

        return Result.success(response)
    }

    // ---------------------------------------------------------------------------
    // Serialisation helpers
    // ---------------------------------------------------------------------------

    /**
     * Serialises ranked preference hospital IDs to a JSON array string.
     *
     * Example: `["MOH2026-0001","MOH2026-0002","MOH2026-0003"]`
     *
     * Array order is preserved exactly. A Set is never used.
     */
    internal fun serializePreferences(preferences: RankedPreferences): String =
        json.encodeToString(preferences.hospitalIds)

    /**
     * Deserialises a JSON array string back to a [RankedPreferences].
     *
     * Returns `null` if the JSON is blank, malformed, or produces an empty list.
     * The decoded list order matches the encoded order exactly.
     */
    internal fun deserializePreferences(jsonString: String?): RankedPreferences? {
        if (jsonString.isNullOrBlank()) return null
        return runCatching {
            val ids: List<String> = json.decodeFromString(jsonString)
            if (ids.isEmpty()) return null
            RankedPreferences(ids)
        }.getOrNull()
    }

    // ---------------------------------------------------------------------------
    // Entity ↔ domain mapping
    // ---------------------------------------------------------------------------

    private fun TransferActiveCacheEntity.toDomain(): TransferRequest? {
        val statusRaw = requestStatus?.trim()?.uppercase()
        val hospitalId = currentHospitalId?.trim()
        val gradeClean = grade?.trim()

        if (statusRaw.isNullOrEmpty() || hospitalId.isNullOrEmpty() || gradeClean.isNullOrEmpty()) return null

        val requestStatus = runCatching {
            TransferRequestStatus.valueOf(statusRaw)
        }.getOrNull() ?: return null

        val prefs = deserializePreferences(preferenceHospitalIdsJson) ?: return null

        val cacheSyncStatus = runCatching {
            CacheSyncStatus.valueOf(syncStatus.trim().uppercase())
        }.getOrElse { CacheSyncStatus.SYNCED }

        return TransferRequest(
            requestId = this.requestId,
            requestStatus = requestStatus,
            currentHospitalId = hospitalId,
            rankedPreferences = prefs,
            grade = gradeClean,
            syncStatus = cacheSyncStatus,
            updatedAt = this.updatedAt,
            matchStatus = this.matchStatus
        )
    }
}
