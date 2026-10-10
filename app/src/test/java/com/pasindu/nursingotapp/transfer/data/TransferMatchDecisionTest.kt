package com.pasindu.nursingotapp.transfer.data

import android.content.Context
import android.content.ContextWrapper
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.TransferActiveCacheDao
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity
import com.pasindu.nursingotapp.transfer.data.model.CacheSyncStatus
import com.pasindu.nursingotapp.transfer.data.model.Decision
import com.pasindu.nursingotapp.transfer.data.model.DecisionRequest
import com.pasindu.nursingotapp.transfer.data.model.DecisionResponse
import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
import com.pasindu.nursingotapp.transfer.data.model.WorkerDirectMatch
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import com.pasindu.nursingotapp.transfer.data.model.WorkerWithdrawResponse
import com.pasindu.nursingotapp.transfer.ui.MatchDecisionStatus
import com.pasindu.nursingotapp.transfer.ui.TransferMatchUiState
import com.pasindu.nursingotapp.transfer.ui.TransferMatchViewModel
import com.pasindu.nursingotapp.transfer.ui.createSampleMatchUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferMatchDecisionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

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

    private class FakeProfileDao(var currentProfile: ProfileEntity? = null) : ProfileDao {
        private val _flow = MutableStateFlow<ProfileEntity?>(currentProfile)

        override suspend fun upsert(profile: ProfileEntity) {
            currentProfile = profile
            _flow.value = profile
        }

        override fun observeProfile(): Flow<ProfileEntity?> = _flow

        override suspend fun getProfileOnce(): ProfileEntity? = currentProfile
    }

    private class FakeTokenProvider(
        var idToken: String? = "test-firebase-id-token",
        var userId: String? = "user-123"
    ) : TransferTokenProvider {
        override suspend fun getFirebaseIdToken(forceRefresh: Boolean): String? = idToken
        override suspend fun getCurrentUserId(): String? = userId
    }

    private class FakeRemoteDataSource : TransferRemoteDataSource {
        var remoteRequest: RemoteTransferRequestState? = RemoteTransferRequestState(
            status = "MATCHED",
            locked = true,
            currentMatchId = "MATCH-001",
            updatedAt = 1000L
        )

        var remoteMatch: RemoteMatchState? = RemoteMatchState(
            matchId = "MATCH-001",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "user-456",
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
                priorityReason = "Mutual preference match"
            ),
            status = "PENDING_CONFIRMATION",
            createdAt = "2026-10-04T12:00:00Z",
            expiresAt = "2026-10-06T12:00:00Z"
        )

        override suspend fun fetchTransferRequest(userId: String): Result<RemoteTransferRequestState?> =
            Result.success(remoteRequest)

        override suspend fun fetchMatchDoc(matchId: String): Result<RemoteMatchState?> =
            Result.success(remoteMatch)

        override suspend fun publishTransferRequest(
            userId: String,
            request: TransferRequest
        ): Result<Unit> = Result.success(Unit)

        override suspend fun withdrawTransferRequest(userId: String): Result<Unit> =
            Result.success(Unit)
    }

    private class FakeWorkerApiClient : TransferWorkerApiClient {
        var respondResult: Result<DecisionResponse>? = null
        var lastCapturedRequest: DecisionRequest? = null
        var lastCapturedToken: String? = null

        override suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult {
            return WorkerSyncResult.NoMatch("No match")
        }

        override suspend fun respondToMatch(
            firebaseIdToken: String,
            payload: DecisionRequest
        ): DecisionResponse {
            lastCapturedToken = firebaseIdToken
            lastCapturedRequest = payload
            val res = respondResult ?: Result.success(
                DecisionResponse(
                    matchId = payload.matchId,
                    newStatus = if (payload.decision == Decision.ACCEPT) "CONFIRMED" else "CANCELLED",
                    expiresAt = "2026-10-04T12:00:00Z",
                    decisionApplied = true
                )
            )
            return res.getOrThrow()
        }

        override suspend fun withdrawRequest(firebaseIdToken: String): WorkerWithdrawResponse {
            return WorkerWithdrawResponse(withdrawn = true)
        }
    }

    private lateinit var dao: FakeDao
    private lateinit var profileDao: FakeProfileDao
    private lateinit var tokenProvider: FakeTokenProvider
    private lateinit var remoteDataSource: FakeRemoteDataSource
    private lateinit var workerApiClient: FakeWorkerApiClient
    private lateinit var hospitalRepo: HospitalReferenceRepository
    private lateinit var repository: TransferRequestRepository
    private lateinit var viewModel: TransferMatchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeDao()
        profileDao = FakeProfileDao(
            ProfileEntity(
                id = 1,
                fullName = "Nurse Jane",
                serviceNo = "SN-12345",
                unit = "ICU",
                paySheetNo = "PS-99",
                grade = "Grade I",
                basicSalary = 50000.0,
                otRate = 250.0,
                updatedAt = System.currentTimeMillis()
            )
        )
        tokenProvider = FakeTokenProvider()
        remoteDataSource = FakeRemoteDataSource()
        workerApiClient = FakeWorkerApiClient()

        val dummyContext: Context = ContextWrapper(null)
        hospitalRepo = HospitalReferenceRepository(dummyContext)
        val cachedField = HospitalReferenceRepository::class.java.getDeclaredField("cachedHospitals")
        cachedField.isAccessible = true
        cachedField.set(
            hospitalRepo,
            listOf(
                HospitalReference(
                    hospitalId = "HOSP-001",
                    province = "Western",
                    rdhsDivision = "Colombo",
                    category = "National",
                    categoryFullName = "National Hospital",
                    name = "General Hospital 1",
                    administeringAuthority = "Line Ministry"
                ),
                HospitalReference(
                    hospitalId = "HOSP-002",
                    province = "Southern",
                    rdhsDivision = "Galle",
                    category = "Teaching",
                    categoryFullName = "Teaching Hospital",
                    name = "General Hospital 2",
                    administeringAuthority = "Line Ministry"
                )
            )
        )

        // Seed an active matched entity in Room cache
        dao.entity = TransferActiveCacheEntity(
            id = 1,
            requestId = "user-123",
            requestStatus = TransferRequestStatus.MATCHED.name,
            currentHospitalId = "HOSP-001",
            preferenceHospitalIdsJson = """["HOSP-002"]""",
            grade = "Grade I",
            matchCycleId = "MATCH-001",
            matchType = "DIRECT_2_WAY",
            matchStatus = "PENDING_CONFIRMATION",
            matchPayloadJson = "{}",
            syncStatus = CacheSyncStatus.SYNCED.name,
            updatedAt = 1000L
        )

        repository = TransferRequestRepository(
            dao = dao,
            profileDao = profileDao,
            tokenProvider = tokenProvider,
            remoteDataSource = remoteDataSource,
            workerApiClient = workerApiClient
        )

        viewModel = TransferMatchViewModel(repository, hospitalRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        // Ensure ViewModel has loaded match state for decision tests
        val uiStateField = TransferMatchViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = uiStateField.get(viewModel) as MutableStateFlow<TransferMatchUiState>
        stateFlow.value = TransferMatchUiState.MatchFound(
            createSampleMatchUiModel().copy(matchId = "MATCH-001")
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. Serialization
    @Test
    fun decisionRequest_serialization_producesExpectedJson() {
        val req = DecisionRequest("match-123", Decision.ACCEPT)
        val jsonStr = json.encodeToString(req)
        assertTrue(jsonStr.contains(""""matchId":"match-123""""))
        assertTrue(jsonStr.contains(""""decision":"ACCEPT""""))
    }

    // 2. Deserialization
    @Test
    fun decisionResponse_deserialization_parsesExpectedFields() {
        val rawJson = """
            {
                "matchId": "match-123",
                "newStatus": "CONFIRMED",
                "expiresAt": "2026-10-04T12:00:00Z",
                "decisionApplied": true
            }
        """.trimIndent()
        val parsed = json.decodeFromString<DecisionResponse>(rawJson)
        assertEquals("match-123", parsed.matchId)
        assertEquals("CONFIRMED", parsed.newStatus)
        assertEquals("2026-10-04T12:00:00Z", parsed.expiresAt)
        assertTrue(parsed.decisionApplied)
    }

    // 3. API Client interaction
    @Test
    fun apiClient_respondToMatch_capturesTokenAndPayload() = runTest {
        val resp = workerApiClient.respondToMatch(
            "token-xyz",
            DecisionRequest("match-abc", Decision.REJECT)
        )
        assertEquals("token-xyz", workerApiClient.lastCapturedToken)
        assertEquals("match-abc", workerApiClient.lastCapturedRequest?.matchId)
        assertEquals(Decision.REJECT, workerApiClient.lastCapturedRequest?.decision)
        assertEquals("CANCELLED", resp.newStatus)
    }

    // 4. Repository ACCEPT updates cache
    @Test
    fun repository_respondToMatch_accept_updatesCacheToConfirmed() = runTest {
        workerApiClient.respondResult = Result.success(
            DecisionResponse(
                matchId = "MATCH-001",
                newStatus = "CONFIRMED",
                expiresAt = "2026-10-04T12:00:00Z",
                decisionApplied = true
            )
        )

        val result = repository.respondToMatch("MATCH-001", Decision.ACCEPT)
        assertTrue(result.isSuccess)
        val updated = dao.getOnce()
        assertNotNull(updated)
        assertEquals("CONFIRMED", updated?.matchStatus)
        assertEquals(TransferRequestStatus.MATCHED.name, updated?.requestStatus)
        assertEquals(CacheSyncStatus.SYNCED.name, updated?.syncStatus)
    }

    // 5. Repository REJECT updates cache
    @Test
    fun repository_respondToMatch_reject_updatesCacheToCancelled() = runTest {
        workerApiClient.respondResult = Result.success(
            DecisionResponse(
                matchId = "MATCH-001",
                newStatus = "CANCELLED",
                expiresAt = "2026-10-04T12:00:00Z",
                decisionApplied = true
            )
        )

        val result = repository.respondToMatch("MATCH-001", Decision.REJECT)
        assertTrue(result.isSuccess)
        val updated = dao.getOnce()
        assertNotNull(updated)
        assertEquals("CANCELLED", updated?.matchStatus)
        assertEquals(TransferRequestStatus.PENDING.name, updated?.requestStatus)
        assertEquals(CacheSyncStatus.SYNCED.name, updated?.syncStatus)
    }

    // 6. Repository error handling
    @Test
    fun repository_respondToMatch_networkFailure_returnsFailure() = runTest {
        workerApiClient.respondResult = Result.failure(Exception("Network unreachable"))

        val result = repository.respondToMatch("MATCH-001", Decision.ACCEPT)
        assertTrue(result.isFailure)
        assertEquals("Network unreachable", result.exceptionOrNull()?.message)
        // Entity status should not have changed to CONFIRMED
        val current = dao.getOnce()
        assertEquals("PENDING_CONFIRMATION", current?.matchStatus)
    }

    // 7. ViewModel ACCEPT
    @Test
    fun viewModel_acceptMatch_setsLoadingThenAccepted() = runTest {
        advanceUntilIdle()
        workerApiClient.respondResult = Result.success(
            DecisionResponse(
                matchId = "MATCH-001",
                newStatus = "PENDING_CONFIRMATION",
                expiresAt = "2026-10-04T12:00:00Z",
                decisionApplied = true
            )
        )

        viewModel.acceptMatch("MATCH-001")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TransferMatchUiState.AlreadyAccepted)
        assertEquals(MatchDecisionStatus.ACCEPTED, (state as TransferMatchUiState.AlreadyAccepted).match.myStatus)
        assertFalse(viewModel.isSubmitting.value)
    }

    // 8. ViewModel REJECT
    @Test
    fun viewModel_rejectMatch_setsLoadingThenRejected() = runTest {
        advanceUntilIdle()
        workerApiClient.respondResult = Result.success(
            DecisionResponse(
                matchId = "MATCH-001",
                newStatus = "CANCELLED",
                expiresAt = "2026-10-04T12:00:00Z",
                decisionApplied = true
            )
        )

        var completed = false
        viewModel.rejectMatch("MATCH-001") {
            completed = true
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TransferMatchUiState.AlreadyRejected)
        assertEquals(MatchDecisionStatus.REJECTED, (state as TransferMatchUiState.AlreadyRejected).match.myStatus)
        assertTrue(completed)
        assertFalse(viewModel.isSubmitting.value)
    }

    // 9. Duplicate action and error protection
    @Test
    fun viewModel_duplicateActionProtection_ignoresSecondConcurrentCall() = runTest {
        advanceUntilIdle()
        workerApiClient.respondResult = Result.success(
            DecisionResponse(
                matchId = "MATCH-001",
                newStatus = "CONFIRMED",
                expiresAt = "2026-10-04T12:00:00Z",
                decisionApplied = true
            )
        )

        viewModel.acceptMatch("MATCH-001")
        // Calling again while submitting
        viewModel.acceptMatch("MATCH-001")

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is TransferMatchUiState.AlreadyAccepted)
    }

    @Test
    fun viewModel_failure_setsErrorState() = runTest {
        advanceUntilIdle()
        workerApiClient.respondResult = Result.failure(Exception("Worker timeout 504"))

        viewModel.acceptMatch("MATCH-001")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TransferMatchUiState.Error)
        assertEquals("Worker timeout 504", (state as TransferMatchUiState.Error).message)
        assertFalse(viewModel.isSubmitting.value)
    }

    // 10. 3-Way Match Models & Serialization
    @Test
    fun workerThreeWayMatch_serializationAndDeserialization_preservesAllFields() {
        val threeWayMatch = com.pasindu.nursingotapp.transfer.data.model.WorkerThreeWayMatch(
            nurseAUid = "user-A",
            nurseBUid = "user-B",
            nurseCUid = "user-C",
            nurseACurrentHospitalId = "HOSP-001",
            nurseBCurrentHospitalId = "HOSP-002",
            nurseCCurrentHospitalId = "HOSP-003",
            nurseADestinationHospitalId = "HOSP-002",
            nurseBDestinationHospitalId = "HOSP-003",
            nurseCDestinationHospitalId = "HOSP-001",
            nurseAGrade = "Grade I",
            nurseBGrade = "Grade I",
            nurseCGrade = "Grade I",
            isAllSameGrade = true,
            nurseAPreferenceRank = 1,
            nurseBPreferenceRank = 1,
            nurseCPreferenceRank = 1,
            combinedPreferenceRank = 3,
            priorityReason = "3-way circular match (A -> B -> C -> A)"
        )

        val jsonStr = json.encodeToString(threeWayMatch)
        assertTrue(jsonStr.contains(""""nurseAUid":"user-A""""))
        assertTrue(jsonStr.contains(""""nurseCUid":"user-C""""))
        assertTrue(jsonStr.contains(""""isAllSameGrade":true"""))

        val decoded = json.decodeFromString<com.pasindu.nursingotapp.transfer.data.model.WorkerThreeWayMatch>(jsonStr)
        assertEquals("user-A", decoded.nurseAUid)
        assertEquals("user-B", decoded.nurseBUid)
        assertEquals("user-C", decoded.nurseCUid)
        assertEquals("HOSP-001", decoded.nurseACurrentHospitalId)
        assertEquals("HOSP-002", decoded.nurseBCurrentHospitalId)
        assertEquals("HOSP-003", decoded.nurseCCurrentHospitalId)
        assertTrue(decoded.isAllSameGrade)
        assertEquals(3, decoded.combinedPreferenceRank)
    }

    @Test
    fun workerMatchResponse_threeWayMatch_deserializesCorrectly() {
        val rawJson = """
            {
                "matched": true,
                "matchId": "MATCH-3WAY-999",
                "matchType": "THREE_WAY",
                "threeWayMatch": {
                    "nurseAUid": "user-A",
                    "nurseBUid": "user-B",
                    "nurseCUid": "user-C",
                    "nurseACurrentHospitalId": "HOSP-001",
                    "nurseBCurrentHospitalId": "HOSP-002",
                    "nurseCCurrentHospitalId": "HOSP-003",
                    "nurseADestinationHospitalId": "HOSP-002",
                    "nurseBDestinationHospitalId": "HOSP-003",
                    "nurseCDestinationHospitalId": "HOSP-001",
                    "nurseAGrade": "Grade I",
                    "nurseBGrade": "Grade I",
                    "nurseCGrade": "Grade I",
                    "isAllSameGrade": true,
                    "nurseAPreferenceRank": 1,
                    "nurseBPreferenceRank": 1,
                    "nurseCPreferenceRank": 1,
                    "combinedPreferenceRank": 3,
                    "priorityReason": "3-way circular cycle"
                },
                "createdAt": "2026-10-08T12:00:00Z",
                "expiresAt": "2026-10-10T12:00:00Z"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<com.pasindu.nursingotapp.transfer.data.model.WorkerMatchResponse>(rawJson)
        assertTrue(parsed.matched)
        assertEquals("MATCH-3WAY-999", parsed.matchId)
        assertEquals("THREE_WAY", parsed.matchType)
        assertNotNull(parsed.threeWayMatch)
        assertEquals("user-C", parsed.threeWayMatch?.nurseCUid)
        assertEquals(3, parsed.threeWayMatch?.combinedPreferenceRank)
    }

    // 11. Phase 2 Workflow: DecisionResponse parsing with sliding window & chat deadline
    @Test
    fun decisionResponse_withWorkflowFields_deserializesAccurately() {
        val rawJson = """
            {
                "matchId": "match-flow-1",
                "newStatus": "CHAT_OPEN",
                "expiresAt": "2026-10-11T12:00:00Z",
                "chatDeadline": "2026-10-11T12:00:00Z",
                "firstResponseAt": "2026-10-08T12:00:00Z",
                "decisionApplied": true
            }
        """.trimIndent()
        val parsed = json.decodeFromString<DecisionResponse>(rawJson)
        assertEquals("match-flow-1", parsed.matchId)
        assertEquals("CHAT_OPEN", parsed.newStatus)
        assertEquals("2026-10-11T12:00:00Z", parsed.expiresAt)
        assertEquals("2026-10-11T12:00:00Z", parsed.chatDeadline)
        assertEquals("2026-10-08T12:00:00Z", parsed.firstResponseAt)
        assertTrue(parsed.decisionApplied)
    }

    // 12. DecisionRequest with CONFIRM decision
    @Test
    fun decisionRequest_withConfirmDecision_serializesAccurately() {
        val req = DecisionRequest("match-conf-1", Decision.CONFIRM)
        val jsonStr = json.encodeToString(req)
        assertTrue(jsonStr.contains(""""decision":"CONFIRM""""))
    }

    // 13. ViewModel: ACCEPT leading to CHAT_OPEN sets ChatOpen state
    @Test
    fun viewModel_acceptMatch_advancingToChatOpen_setsChatOpenState() = runTest {
        advanceUntilIdle()
        workerApiClient.respondResult = Result.success(
            DecisionResponse(
                matchId = "MATCH-001",
                newStatus = "CHAT_OPEN",
                expiresAt = "2026-10-11T12:00:00Z",
                chatDeadline = "2026-10-11T12:00:00Z",
                firstResponseAt = "2026-10-08T12:00:00Z",
                decisionApplied = true
            )
        )

        viewModel.acceptMatch("MATCH-001")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected ChatOpen state", state is TransferMatchUiState.ChatOpen)
        val match = (state as TransferMatchUiState.ChatOpen).match
        assertEquals("CHAT_OPEN", match.serverStatus)
        assertEquals(MatchDecisionStatus.ACCEPTED, match.myStatus)
        assertFalse(viewModel.isSubmitting.value)
    }

    // 14. ViewModel: CONFIRM leading to CONFIRMED sets Confirmed state
    @Test
    fun viewModel_confirmMatch_advancingToConfirmed_setsConfirmedState() = runTest {
        advanceUntilIdle()
        workerApiClient.respondResult = Result.success(
            DecisionResponse(
                matchId = "MATCH-001",
                newStatus = "CONFIRMED",
                expiresAt = "2026-10-11T12:00:00Z",
                chatDeadline = "2026-10-11T12:00:00Z",
                decisionApplied = true
            )
        )

        viewModel.confirmMatch("MATCH-001")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Confirmed state", state is TransferMatchUiState.Confirmed)
        val match = (state as TransferMatchUiState.Confirmed).match
        assertEquals("CONFIRMED", match.serverStatus)
        assertTrue(match.myConfirmed)
        assertFalse(viewModel.isSubmitting.value)
    }

    // 15. Repository: Room persistence preserves matchStatus during workflow advancement
    @Test
    fun repository_respondToMatch_persistsChatOpenAndConfirmedStatusToRoom() = runTest {
        workerApiClient.respondResult = Result.success(
            DecisionResponse(
                matchId = "MATCH-001",
                newStatus = "CHAT_OPEN",
                expiresAt = "2026-10-11T12:00:00Z",
                chatDeadline = "2026-10-11T12:00:00Z",
                decisionApplied = true
            )
        )

        val res = repository.respondToMatch("MATCH-001", Decision.ACCEPT)
        assertTrue(res.isSuccess)
        val cached = dao.getOnce()
        assertEquals("CHAT_OPEN", cached?.matchStatus)
        assertEquals(TransferRequestStatus.MATCHED.name, cached?.requestStatus)

        // Now CONFIRM
        workerApiClient.respondResult = Result.success(
            DecisionResponse(
                matchId = "MATCH-001",
                newStatus = "CONFIRMED",
                expiresAt = "2026-10-11T12:00:00Z",
                decisionApplied = true
            )
        )
        val confirmRes = repository.respondToMatch("MATCH-001", Decision.CONFIRM)
        assertTrue(confirmRes.isSuccess)
        val confirmedCache = dao.getOnce()
        assertEquals("CONFIRMED", confirmedCache?.matchStatus)
        assertEquals(TransferRequestStatus.MATCHED.name, confirmedCache?.requestStatus)
    }
}
