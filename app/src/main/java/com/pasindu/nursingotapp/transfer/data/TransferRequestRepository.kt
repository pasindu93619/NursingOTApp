package com.pasindu.nursingotapp.transfer.data

import kotlinx.coroutines.CancellationException
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
import com.pasindu.nursingotapp.transfer.data.model.WorkerWithdrawResponse
import com.pasindu.nursingotapp.transfer.data.model.TransferMatchHistoryItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
private inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

@Singleton
class TransferRequestRepository @Inject constructor(
    private val dao: TransferActiveCacheDao,
    private val profileDao: ProfileDao,
    private val tokenProvider: TransferTokenProvider,
    private val remoteDataSource: TransferRemoteDataSource,
    private val workerApiClient: TransferWorkerApiClient,
    private val logger: TransferSyncLogger = NoOpTransferSyncLogger
) {
    // Secondary constructor for local DAO / offline unit testing
    constructor(
        dao: TransferActiveCacheDao,
        profileDao: ProfileDao
    ) : this(
        dao = dao,
        profileDao = profileDao,
        tokenProvider = StubTransferTokenProvider,
        remoteDataSource = StubTransferRemoteDataSource,
        workerApiClient = StubTransferWorkerApiClient,
        logger = NoOpTransferSyncLogger
    )

    companion object {
        private const val TAG = "TransferSync"
    }

    private data class CompletedSyncRecord(
        val entityId: Int,
        val updatedAt: Long,
        val currentHospitalId: String?,
        val preferenceHospitalIdsJson: String?,
        val grade: String?,
        val result: WorkerSyncResult
    )

    private val syncMutex = Mutex()
    private var syncFlightId: Long = 0L
    private var lastCompletedSync: CompletedSyncRecord? = null

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
    ) = syncMutex.withLock {
        lastCompletedSync = null

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

        require(currentHospitalId !in rankedPreferences.hospitalIds) {
            "Your current hospital cannot also be a preferred destination. Choose a different hospital."
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
        val callFlightId = syncFlightId
        return syncMutex.withLock {
            val cachedEntity = dao.getOnce()
                ?: return@withLock WorkerSyncResult.NoMatch("No active local transfer request")

            // Re-check state after acquiring lock: if a concurrent in-flight sync just finished
            // for this exact request with SYNCED status, safely reuse its verified result.
            if (syncFlightId > callFlightId && cachedEntity.syncStatus == CacheSyncStatus.SYNCED.name) {
                val record = lastCompletedSync
                if (record != null &&
                    record.entityId == cachedEntity.id &&
                    record.updatedAt == cachedEntity.updatedAt &&
                    record.currentHospitalId == cachedEntity.currentHospitalId &&
                    record.preferenceHospitalIdsJson == cachedEntity.preferenceHospitalIdsJson &&
                    record.grade == cachedEntity.grade
                ) {
                    when (record.result) {
                        is WorkerSyncResult.MatchFound -> {
                            if (cachedEntity.requestStatus == TransferRequestStatus.MATCHED.name &&
                                cachedEntity.matchCycleId == record.result.matchId
                            ) {
                                return@withLock record.result
                            }
                        }
                        is WorkerSyncResult.NoMatch,
                        is WorkerSyncResult.ReturnedToPool -> {
                            if (cachedEntity.requestStatus == TransferRequestStatus.PENDING.name &&
                                cachedEntity.matchCycleId == null
                            ) {
                                return@withLock record.result
                            }
                        }
                        is WorkerSyncResult.RequestCompleted -> {
                            if (cachedEntity.requestStatus == TransferRequestStatus.COMPLETED.name) {
                                return@withLock record.result
                            }
                        }
                        is WorkerSyncResult.RequestWithdrawn -> {
                            if (cachedEntity.requestStatus == TransferRequestStatus.WITHDRAWN.name) {
                                return@withLock record.result
                            }
                        }
                        else -> {
                            // Transient errors (NetworkError, AuthError, Conflict) are never reused
                        }
                    }
                }
            }

            val result = syncActiveRequestLocked()
            if (result is WorkerSyncResult.MatchFound ||
                result is WorkerSyncResult.NoMatch ||
                result is WorkerSyncResult.ReturnedToPool ||
                result is WorkerSyncResult.RequestCompleted ||
                result is WorkerSyncResult.RequestWithdrawn
            ) {
                val latest = dao.getOnce()
                if (latest != null) {
                    lastCompletedSync = CompletedSyncRecord(
                        entityId = latest.id,
                        updatedAt = latest.updatedAt,
                        currentHospitalId = latest.currentHospitalId,
                        preferenceHospitalIdsJson = latest.preferenceHospitalIdsJson,
                        grade = latest.grade,
                        result = result
                    )
                }
            } else {
                lastCompletedSync = null
            }
            syncFlightId++
            result
        }
    }

    private suspend fun syncActiveRequestLocked(): WorkerSyncResult {
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

        if (domain.requestStatus == TransferRequestStatus.COMPLETED) {
            return WorkerSyncResult.RequestCompleted("Request is completed")
        }
        if (domain.requestStatus == TransferRequestStatus.WITHDRAWN) {
            return WorkerSyncResult.RequestWithdrawn("Request is withdrawn")
        }

        if (tokenProvider is StubTransferTokenProvider || workerApiClient is StubTransferWorkerApiClient) {
            return WorkerSyncResult.NoMatch("Offline stub client")
        }

        val userId = tokenProvider.getCurrentUserId()
            ?: run {
                logger.e(TAG, "Authentication failure: Unable to get user ID")
                dao.upsert(entity.copy(syncStatus = CacheSyncStatus.ERROR.name))
                return WorkerSyncResult.AuthError("Authentication failure: Unable to get user ID")
            }

        logger.d(TAG, "syncActiveRequest: authenticated user present")

        // Read server-owned state before publishing local SEARCHING data.
        val remoteState = remoteDataSource.fetchTransferRequest(userId).getOrElse { error ->
            logger.e(
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

        logger.d(
            TAG,
            "Firestore fetchTransferRequest OK: exists=${remoteState != null}, " +
                "status=${remoteState?.status}, locked=${remoteState?.locked}, " +
                "matchId=${remoteState?.currentMatchId != null}"
        )

        if (remoteState != null && remoteState.status.equals(TransferRequestStatus.COMPLETED.name, ignoreCase = true)) {
            val completedCache = entity.copy(
                currentHospitalId = remoteState.currentHospitalId ?: entity.currentHospitalId,
                requestStatus = TransferRequestStatus.COMPLETED.name,
                matchStatus = "COMPLETED",
                syncStatus = CacheSyncStatus.SYNCED.name,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsert(completedCache)
            return WorkerSyncResult.RequestCompleted("All participants have self-reported transfer completion")
        }

        if (
            remoteState?.status.equals(TransferRequestStatus.MATCHED.name, ignoreCase = true) &&
            remoteState?.locked == true &&
            !remoteState.currentMatchId.isNullOrBlank()
        ) {
            val matchId = remoteState.currentMatchId!!
            val remoteMatch = remoteDataSource.fetchMatchDoc(matchId).getOrElse { error ->
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
                acceptedByA = remoteMatch.acceptedByA,
                acceptedByB = remoteMatch.acceptedByB,
                acceptedByC = remoteMatch.acceptedByC,
                rejectedByA = remoteMatch.rejectedByA,
                rejectedByB = remoteMatch.rejectedByB,
                rejectedByC = remoteMatch.rejectedByC,
                confirmedByA = remoteMatch.confirmedByA,
                confirmedByB = remoteMatch.confirmedByB,
                confirmedByC = remoteMatch.confirmedByC
            )
        }

        /*
         * Server is not currently MATCHED and locked.
         *
         * If the local cache is still MATCHED, the match ended on the server (e.g. partner declined
         * or match expired while this nurse was offline or viewing the proposal).
         * Reconcile local row back to PENDING (searching) with matchStatus CANCELLED or EXPIRED,
         * clear match identifiers, preserve updatedAt, and return NoMatch without calling findAndLock in this pass.
         */
        val isLocallyMatched = entity.requestStatus.equals(TransferRequestStatus.MATCHED.name, ignoreCase = true)
        if (isLocallyMatched) {
            val oldMatchCycleId = entity.matchCycleId
            val finalMatchStatus = if (!oldMatchCycleId.isNullOrBlank()) {
                val matchDocResult = remoteDataSource.fetchMatchDoc(oldMatchCycleId)
                val oldMatchDoc = matchDocResult.getOrNull()
                val status = oldMatchDoc?.status?.trim()?.uppercase()
                when {
                    status == "EXPIRED" -> "EXPIRED"
                    status == "CANCELLED" -> "CANCELLED"
                    status != null -> status
                    entity.matchStatus == "EXPIRED" -> "EXPIRED"
                    matchDocResult.isFailure -> entity.matchStatus ?: "CANCELLED"
                    else -> "CANCELLED"
                }
            } else {
                entity.matchStatus ?: "CANCELLED"
            }

            val resetEntity = entity.copy(
                requestId = userId,
                requestStatus = TransferRequestStatus.PENDING.name,
                matchCycleId = null,
                matchType = null,
                matchStatus = finalMatchStatus,
                matchPayloadJson = null,
                syncStatus = CacheSyncStatus.SYNCED.name
                // updatedAt is intentionally UNCHANGED so it does not trigger a republish
            )
            dao.upsert(resetEntity)
            logger.d(TAG, "Reconciled local MATCHED state to PENDING with matchStatus=$finalMatchStatus")
            return WorkerSyncResult.ReturnedToPool("Match has ended ($finalMatchStatus); request returned to pool")
        }

        /*
         * Server is not currently matched and local is not matched.
         *
         * Do not rewrite an already-SYNCED SEARCHING document on every
         * polling cycle. Every Firestore write changes updateTime and can
         * invalidate the Worker atomic precondition while the other nurse
         * is being matched. Publish only when the local request is pending
         * or changed, or when the server has no request yet.
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
            logger.d(TAG, "Publishing transfer request to server")
            val publishResult = remoteDataSource.publishTransferRequest(userId, domain)
            if (publishResult.isFailure) {
                val err = publishResult.exceptionOrNull()
                logger.e(
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
            logger.d(TAG, "Firestore publishTransferRequest OK")
        } else {
            logger.d(
                TAG,
                "Remote request already SEARCHING and local cache is SYNCED; " +
                    "skipping redundant Firestore write before matching"
            )
        }

        val token = tokenProvider.getFirebaseIdToken()
            ?: run {
                logger.e(TAG, "Firebase ID token acquisition FAILED")
                dao.upsert(entity.copy(syncStatus = CacheSyncStatus.ERROR.name))
                return WorkerSyncResult.AuthError("Authentication failure: Unable to get ID token")
            }

        logger.d(TAG, "Firebase ID token acquired successfully")
        logger.d(TAG, "Calling Cloudflare matching Worker")
        val workerResult = workerApiClient.findAndLockMatch(token)
        logger.d(TAG, "Cloudflare matching Worker returned: ${workerResult::class.simpleName}")

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
            is WorkerSyncResult.NoMatch,
            is WorkerSyncResult.RequestCompleted,
            is WorkerSyncResult.RequestWithdrawn,
            is WorkerSyncResult.ReturnedToPool -> {
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
     * Loads the authenticated nurse's own cancelled/expired matches from the Worker.
     * Match history is remote-only and does not alter the Room active-request cache.
     */

    suspend fun getCompletionStatus(matchId: String): Result<com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus> = runSuspendCatching {
        require(matchId.isNotBlank()) { "matchId must not be blank" }
        val token = tokenProvider.getFirebaseIdToken()
            ?: throw IllegalStateException("Authentication provider unavailable")
        workerApiClient.getCompletionStatus(token, matchId)
    }

    suspend fun reportCompletion(matchId: String): Result<com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus> = runSuspendCatching {
        require(matchId.isNotBlank()) { "matchId must not be blank" }
        val token = tokenProvider.getFirebaseIdToken()
            ?: throw IllegalStateException("Authentication provider unavailable")
        val result = workerApiClient.reportCompletion(token, matchId)
        if (result.status == "COMPLETED") {
            val cached = dao.getOnce()
            if (cached != null && cached.matchCycleId == matchId) {
                dao.upsert(cached.copy(
                    currentHospitalId = result.callerDestinationHospitalId ?: cached.currentHospitalId,
                    requestStatus = TransferRequestStatus.COMPLETED.name,
                    matchStatus = "COMPLETED",
                    syncStatus = CacheSyncStatus.SYNCED.name,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        } else {
            val cached = dao.getOnce()
            if (cached != null && cached.matchCycleId == matchId) {
                dao.upsert(cached.copy(
                    requestStatus = TransferRequestStatus.MATCHED.name,
                    matchStatus = "AWAITING_COMPLETION",
                    syncStatus = CacheSyncStatus.SYNCED.name,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
        result
    }

    suspend fun getMatchHistory(): Result<List<TransferMatchHistoryItem>> = runSuspendCatching {
        val token = tokenProvider.getFirebaseIdToken() ?: error("Authentication failure: Unable to get ID token")
        workerApiClient.getMatchHistory(token)
    }

    /**
     * Clears the active request row from Room and authoritatively withdraws from the backend.
     *
     * After this call, [observeActiveRequest] will emit `null`.
     *
     * @throws Exception if backend withdrawal or Room delete fails.
     */
    suspend fun clearRequest() = syncMutex.withLock {
        lastCompletedSync = null
        if (tokenProvider !is StubTransferTokenProvider && workerApiClient !is StubTransferWorkerApiClient) {
            val token = tokenProvider.getFirebaseIdToken()
                ?: throw IllegalStateException("Authentication failure: Unable to get ID token")
            workerApiClient.withdrawRequest(token)
        }
        dao.clear()
    }

    /**
     * Submits an ACCEPT or REJECT decision for the current active match to the Cloudflare Worker.
     * Updates the local Room cache with the authoritative status upon success.
     */
    suspend fun respondToMatch(matchId: String, decision: Decision): Result<DecisionResponse> {
        val token = tokenProvider.getFirebaseIdToken()
            ?: return Result.failure(IllegalStateException("Authentication failure: Unable to get ID token"))

        val response = try {
            workerApiClient.respondToMatch(token, DecisionRequest(matchId, decision))
        } catch (e: CancellationException) {
            throw e
        } catch (originalError: Throwable) {
            // Reconcile with authoritative match state if the worker call failed
            if (decision == Decision.LEAVE || decision == Decision.REJECT) {
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

        lastCompletedSync = null
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
            matchStatus = this.matchStatus,
            matchCycleId = this.matchCycleId
        )
    }
}

// ---------------------------------------------------------------------------
// Offline / Test Stubs for secondary constructors
// ---------------------------------------------------------------------------

private object StubTransferTokenProvider : TransferTokenProvider {
    override suspend fun getFirebaseIdToken(forceRefresh: Boolean): String? = "stub-id-token"
    override suspend fun getCurrentUserId(): String? = "stub-user-id"
}

private object StubTransferRemoteDataSource : TransferRemoteDataSource {
    override suspend fun fetchTransferRequest(userId: String): Result<RemoteTransferRequestState?> =
        Result.success(null)

    override suspend fun fetchMatchDoc(matchId: String): Result<RemoteMatchState?> =
        Result.success(null)

    override fun observeMatchDoc(matchId: String): Flow<Result<RemoteMatchState?>> =
        kotlinx.coroutines.flow.flowOf(Result.success(null))

    override suspend fun publishTransferRequest(
        userId: String,
        request: TransferRequest
    ): Result<Unit> = Result.success(Unit)

    override suspend fun withdrawTransferRequest(userId: String): Result<Unit> =
        Result.success(Unit)
}

private object StubTransferWorkerApiClient : TransferWorkerApiClient {
    override suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult =
        WorkerSyncResult.NoMatch("Stub client")

    override suspend fun respondToMatch(
        firebaseIdToken: String,
        payload: DecisionRequest
    ): DecisionResponse = error("Stub client does not support respondToMatch")

    override suspend fun withdrawRequest(firebaseIdToken: String): WorkerWithdrawResponse =
        WorkerWithdrawResponse(withdrawn = true)

    override suspend fun getMatchHistory(firebaseIdToken: String): List<TransferMatchHistoryItem> =
        emptyList()

    override suspend fun getCompletionStatus(
        firebaseIdToken: String,
        matchId: String
    ): com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus =
        error("Stub client does not support getCompletionStatus")

    override suspend fun reportCompletion(
        firebaseIdToken: String,
        matchId: String
    ): com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus =
        error("Stub client does not support reportCompletion")
}
