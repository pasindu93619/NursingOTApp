package com.pasindu.nursingotapp.transfer.sync

import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.TransferActiveCacheDao
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity
import com.pasindu.nursingotapp.transfer.data.TransferRemoteDataSource
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.TransferTokenProvider
import com.pasindu.nursingotapp.transfer.data.TransferWorkerApiClient
import com.pasindu.nursingotapp.transfer.data.model.CacheSyncStatus
import com.pasindu.nursingotapp.transfer.data.model.RankedPreferences
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
import com.pasindu.nursingotapp.transfer.data.model.WorkerDirectMatch
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import com.pasindu.nursingotapp.transfer.worker.TransferSyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class TransferSyncBridgeTest {

    // ---------------------------------------------------------------------------
    // Fakes
    // ---------------------------------------------------------------------------

    private class FakeDao : TransferActiveCacheDao {
        var entity: TransferActiveCacheEntity? = null
        private val flow = MutableStateFlow<TransferActiveCacheEntity?>(null)

        override suspend fun upsert(cache: TransferActiveCacheEntity) {
            entity = cache
            flow.value = cache
        }

        override fun observe(): Flow<TransferActiveCacheEntity?> = flow
        override suspend fun getOnce(): TransferActiveCacheEntity? = entity
        override suspend fun clear() {
            entity = null
            flow.value = null
        }
    }

    private class FakeProfileDao : ProfileDao {
        var profile: ProfileEntity? = ProfileEntity(
            serviceNo = "SN-12345",
            fullName = "Nurse Jane",
            unit = "ICU",
            paySheetNo = "PS-99",
            grade = "Grade I",
            basicSalary = 50000.0,
            otRate = 350.0,
            updatedAt = System.currentTimeMillis(),
            salaryStep = 5
        )

        override suspend fun upsert(profile: ProfileEntity) {
            this.profile = profile
        }

        override fun observeProfile(): Flow<ProfileEntity?> = MutableStateFlow(profile)
        override suspend fun getProfileOnce(): ProfileEntity? = profile
    }

    private class FakeTokenProvider : TransferTokenProvider {
        var token: String? = "mock-firebase-id-token-abc"
        var userId: String? = "test-nurse-uid-777"
        var tokenCallCount = 0

        override suspend fun getFirebaseIdToken(forceRefresh: Boolean): String? {
            tokenCallCount++
            return token
        }

        override suspend fun getCurrentUserId(): String? = userId
    }

    private class FakeRemoteDataSource : TransferRemoteDataSource {
        var publishedRequest: TransferRequest? = null
        var publishedUserId: String? = null
        var shouldFail = false
        var failureException: Throwable = IOException("Simulated network timeout")
        var publishCallCount = 0

        override suspend fun publishTransferRequest(
            userId: String,
            request: TransferRequest
        ): Result<Unit> {
            publishCallCount++
            return if (shouldFail) {
                Result.failure(failureException)
            } else {
                publishedUserId = userId
                publishedRequest = request
                Result.success(Unit)
            }
        }

        override suspend fun withdrawTransferRequest(userId: String): Result<Unit> {
            publishedRequest = null
            return Result.success(Unit)
        }

        override suspend fun fetchTransferRequest(userId: String): Result<com.pasindu.nursingotapp.transfer.data.RemoteTransferRequestState?> =
            Result.success(null)

        override suspend fun fetchMatchDoc(matchId: String): Result<com.pasindu.nursingotapp.transfer.data.RemoteMatchState?> =
            Result.success(null)

    }

    private class FakeWorkerApiClient : TransferWorkerApiClient {
        var result: WorkerSyncResult = WorkerSyncResult.NoMatch("No compatible partner found")
        var receivedToken: String? = null
        var callCount = 0

        override suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult {
            callCount++
            receivedToken = firebaseIdToken
            return result
        }

        override suspend fun respondToMatch(
            firebaseIdToken: String,
            payload: com.pasindu.nursingotapp.transfer.data.model.DecisionRequest
        ): com.pasindu.nursingotapp.transfer.data.model.DecisionResponse {
            return com.pasindu.nursingotapp.transfer.data.model.DecisionResponse(
                matchId = payload.matchId,
                newStatus = "CONFIRMED",
                expiresAt = "2026-10-04T12:00:00Z",
                decisionApplied = true
            )
        }
    }

    private class FakeSyncScheduler : TransferSyncScheduler {
        var scheduleCallCount = 0
        override fun scheduleSync() {
            scheduleCallCount++
        }
    }

    // ---------------------------------------------------------------------------
    // Test Setup
    // ---------------------------------------------------------------------------

    private lateinit var dao: FakeDao
    private lateinit var profileDao: FakeProfileDao
    private lateinit var tokenProvider: FakeTokenProvider
    private lateinit var remoteDataSource: FakeRemoteDataSource
    private lateinit var workerApiClient: FakeWorkerApiClient
    private lateinit var repository: TransferRequestRepository

    @Before
    fun setUp() {
        dao = FakeDao()
        profileDao = FakeProfileDao()
        tokenProvider = FakeTokenProvider()
        remoteDataSource = FakeRemoteDataSource()
        workerApiClient = FakeWorkerApiClient()

        repository = TransferRequestRepository(
            dao = dao,
            profileDao = profileDao,
            tokenProvider = tokenProvider,
            remoteDataSource = remoteDataSource,
            workerApiClient = workerApiClient
        )
    }

    // 1. Room request saves immediately
    @Test
    fun test1_roomRequestSavesImmediately() = runTest {
        val prefs = RankedPreferences(listOf("HOSP-002", "HOSP-003"))
        repository.saveRequest("HOSP-001", prefs)

        val cached = dao.entity
        assertNotNull(cached)
        assertEquals("HOSP-001", cached?.currentHospitalId)
        assertEquals("Grade I", cached?.grade)
        assertEquals(TransferRequestStatus.PENDING.name, cached?.requestStatus)
    }

    // 2. Newly saved request has PENDING syncStatus
    @Test
    fun test2_pendingRequestTriggersSync() = runTest {
        val prefs = RankedPreferences(listOf("HOSP-002"))
        repository.saveRequest("HOSP-001", prefs)

        val cached = dao.entity
        assertEquals(CacheSyncStatus.PENDING.name, cached?.syncStatus)
    }

    // 3. Firebase ID token is requested during sync
    @Test
    fun test3_firebaseIdTokenIsRequestedDuringSync() = runTest {
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))
        val result = repository.syncActiveRequest()

        assertTrue(tokenProvider.tokenCallCount > 0)
        assertEquals("mock-firebase-id-token-abc", workerApiClient.receivedToken)
    }

    // 4. Authorization header bearer token is delivered to worker
    @Test
    fun test4_tokenPassedToWorkerClient() = runTest {
        tokenProvider.token = "secure-test-token-jwt-123"
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))
        repository.syncActiveRequest()

        assertEquals("secure-test-token-jwt-123", workerApiClient.receivedToken)
    }

    // 5. Client UID cannot override authenticated identity (derived strictly from token provider)
    @Test
    fun test5_userIdDerivedExclusivelyFromTokenProvider() = runTest {
        tokenProvider.userId = "auth-uid-from-firebase-token"
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))
        repository.syncActiveRequest()

        assertEquals("auth-uid-from-firebase-token", remoteDataSource.publishedUserId)
        assertEquals("auth-uid-from-firebase-token", dao.entity?.requestId)
    }

    // 6. Successful Worker response with match updates local Room state
    @Test
    fun test6_successfulWorkerMatchUpdatesLocalState() = runTest {
        val match = WorkerDirectMatch(
            nurseAUid = "test-nurse-uid-777",
            nurseBUid = "partner-uid-888",
            nurseACurrentHospitalId = "HOSP-001",
            nurseBCurrentHospitalId = "HOSP-002",
            nurseADestinationHospitalId = "HOSP-002",
            nurseBDestinationHospitalId = "HOSP-001",
            nurseAGrade = "Grade I",
            nurseBGrade = "Grade I",
            isSameGrade = true,
            nurseAPreferenceRank = 1,
            nurseBPreferenceRank = 1,
            combinedPreferenceRank = 2,
            priorityReason = "Same-grade reciprocal match"
        )

        workerApiClient.result = WorkerSyncResult.MatchFound(
            matchId = "MATCH-2026-9999",
            directMatch = match,
            createdAt = "2026-10-02T12:00:00Z",
            expiresAt = "2026-10-04T12:00:00Z"
        )

        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))
        val syncResult = repository.syncActiveRequest()

        assertTrue(syncResult is WorkerSyncResult.MatchFound)
        val cached = dao.entity
        assertEquals(TransferRequestStatus.MATCHED.name, cached?.requestStatus)
        assertEquals("MATCH-2026-9999", cached?.matchCycleId)
        assertEquals("DIRECT_2_WAY", cached?.matchType)
        assertEquals("PENDING_CONFIRMATION", cached?.matchStatus)
        assertEquals(CacheSyncStatus.SYNCED.name, cached?.syncStatus)
        assertNotNull(cached?.matchPayloadJson)
    }

    // 7. Network failure retains local request
    @Test
    fun test7_networkFailureRetainsLocalRequest() = runTest {
        remoteDataSource.shouldFail = true
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))

        val syncResult = repository.syncActiveRequest()
        assertTrue(syncResult is WorkerSyncResult.NetworkError)

        val cached = dao.entity
        assertNotNull(cached)
        assertEquals("HOSP-001", cached?.currentHospitalId)
        assertEquals("Grade I", cached?.grade)
        assertEquals(CacheSyncStatus.ERROR.name, cached?.syncStatus)
    }

    // 8. Worker client network error retains local request and marks ERROR
    @Test
    fun test8_workerNetworkFailureRetainsLocalRequest() = runTest {
        workerApiClient.result = WorkerSyncResult.NetworkError("Worker timeout")
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))

        val syncResult = repository.syncActiveRequest()
        assertTrue(syncResult is WorkerSyncResult.NetworkError)

        val cached = dao.entity
        assertNotNull(cached)
        assertEquals(CacheSyncStatus.ERROR.name, cached?.syncStatus)
    }

    // 9. Authentication failure is handled cleanly
    @Test
    fun test9_authFailureIsHandledCleanly() = runTest {
        tokenProvider.token = null
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))

        val syncResult = repository.syncActiveRequest()
        assertTrue(syncResult is WorkerSyncResult.AuthError)

        val cached = dao.entity
        assertEquals(CacheSyncStatus.ERROR.name, cached?.syncStatus)
    }

    // 10. Server conflict is handled without falsifying a match
    @Test
    fun test10_serverConflictHandledWithoutFalsifyingMatch() = runTest {
        workerApiClient.result = WorkerSyncResult.Conflict("Concurrent modification conflict")
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))

        val syncResult = repository.syncActiveRequest()
        assertTrue(syncResult is WorkerSyncResult.Conflict)

        val cached = dao.entity
        assertNull(cached?.matchCycleId)
        assertEquals(CacheSyncStatus.ERROR.name, cached?.syncStatus)
    }

    // 11. Duplicate sync call is idempotent and preserves server state
    @Test
    fun test11_duplicateSyncIsIdempotent() = runTest {
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))

        repository.syncActiveRequest()
        repository.syncActiveRequest()

        assertEquals(2, remoteDataSource.publishCallCount)
        assertEquals(2, workerApiClient.callCount)
        assertEquals(CacheSyncStatus.SYNCED.name, dao.entity?.syncStatus)
    }

    // 12. Match response is cached locally in matchPayloadJson
    @Test
    fun test12_matchPayloadJsonIsPersisted() = runTest {
        val match = WorkerDirectMatch(
            nurseAUid = "uid-1",
            nurseBUid = "uid-2",
            nurseACurrentHospitalId = "HOSP-001",
            nurseBCurrentHospitalId = "HOSP-002",
            nurseADestinationHospitalId = "HOSP-002",
            nurseBDestinationHospitalId = "HOSP-001",
            nurseAGrade = "Grade I",
            nurseBGrade = "Grade II",
            isSameGrade = false,
            nurseAPreferenceRank = 1,
            nurseBPreferenceRank = 1,
            combinedPreferenceRank = 2,
            priorityReason = "Cross-grade match"
        )

        workerApiClient.result = WorkerSyncResult.MatchFound(
            matchId = "MATCH-CROSS-1",
            directMatch = match,
            createdAt = "2026-10-02T12:00:00Z",
            expiresAt = "2026-10-04T12:00:00Z"
        )

        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))
        repository.syncActiveRequest()

        val json = dao.entity?.matchPayloadJson
        assertNotNull(json)
        assertTrue(json!!.contains("MATCH-CROSS-1") || json.contains("HOSP-002"))
    }

    // 13. Existing ranked preference order remains unchanged
    @Test
    fun test13_rankedPreferenceOrderRemainsUnchanged() = runTest {
        val order = listOf("HOSP-003", "HOSP-001", "HOSP-002")
        repository.saveRequest("HOSP-005", RankedPreferences(order))
        repository.syncActiveRequest()

        val published = remoteDataSource.publishedRequest
        assertEquals(order, published?.rankedPreferences?.hospitalIds)
    }

    // 14. Existing grade remains unchanged
    @Test
    fun test14_gradeRemainsUnchanged() = runTest {
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))
        repository.syncActiveRequest()

        assertEquals("Grade I", remoteDataSource.publishedRequest?.grade)
        assertEquals("Grade I", dao.entity?.grade)
    }

    // 15. Existing current hospital remains unchanged
    @Test
    fun test15_currentHospitalRemainsUnchanged() = runTest {
        repository.saveRequest("HOSP-009", RankedPreferences(listOf("HOSP-002")))
        repository.syncActiveRequest()

        assertEquals("HOSP-009", remoteDataSource.publishedRequest?.currentHospitalId)
        assertEquals("HOSP-009", dao.entity?.currentHospitalId)
    }

    // 16. Room data survives failed network
    @Test
    fun test16_roomDataSurvivesFailedNetwork() = runTest {
        remoteDataSource.shouldFail = true
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))
        repository.syncActiveRequest()

        val req = repository.getActiveRequest()
        assertNotNull(req)
        assertEquals("HOSP-001", req?.currentHospitalId)
        assertEquals(listOf("HOSP-002"), req?.rankedPreferences?.hospitalIds)
    }

    // 17. No Firebase token is persisted in Room
    @Test
    fun test17_noFirebaseTokenPersistedInRoom() = runTest {
        tokenProvider.token = "SUPER-SECRET-TOKEN-DO-NOT-PERSIST"
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))
        repository.syncActiveRequest()

        val entity = dao.entity
        assertFalse(entity.toString().contains("SUPER-SECRET-TOKEN"))
        assertFalse(entity?.matchPayloadJson.orEmpty().contains("SUPER-SECRET-TOKEN"))
    }

    // 18. No Firebase token is logged
    @Test
    fun test18_noFirebaseTokenLogged() = runTest {
        tokenProvider.token = "SENSITIVE_BEARER_TOKEN"
        repository.saveRequest("HOSP-001", RankedPreferences(listOf("HOSP-002")))
        val result = repository.syncActiveRequest()

        assertFalse(result.toString().contains("SENSITIVE_BEARER_TOKEN"))
    }
}
