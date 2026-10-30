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
import com.pasindu.nursingotapp.transfer.data.model.WorkerThreeWayMatch
import com.pasindu.nursingotapp.transfer.data.model.TransferWorkerApiException
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import com.pasindu.nursingotapp.transfer.data.model.WorkerWithdrawResponse
import com.pasindu.nursingotapp.transfer.ui.MatchDecisionStatus
import com.pasindu.nursingotapp.transfer.ui.POLL_WARNING_MESSAGE
import com.pasindu.nursingotapp.transfer.ui.TransferMatchUiState
import com.pasindu.nursingotapp.transfer.ui.TransferMatchViewModel
import com.pasindu.nursingotapp.transfer.ui.calculatePollBackoff
import com.pasindu.nursingotapp.transfer.ui.createSampleMatchUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferMatchDecisionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val activeViewModels = mutableListOf<TransferMatchViewModel>()

    private fun stopAllPolling() = activeViewModels.forEach { it.onScreenExit() }

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            stopAllPolling()
        }
    }
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
        var refreshCount = 0
        var refreshedToken: String? = null
        override suspend fun getFirebaseIdToken(forceRefresh: Boolean): String? {
            if (forceRefresh) {
                refreshCount++
                if (refreshedToken != null) {
                    idToken = refreshedToken
                }
            }
            return idToken
        }
        override suspend fun getCurrentUserId(): String? = userId
    }

    private class FakeRemoteDataSource : TransferRemoteDataSource {
        var fetchRequestError: Throwable? = null
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
            fetchRequestError?.let { Result.failure(it) } ?: Result.success(remoteRequest)

        override suspend fun fetchMatchDoc(matchId: String): Result<RemoteMatchState?> =
            Result.success(remoteMatch)

        var observeMatchDocCallCount = 0
        var lastObservedMatchId: String? = null
        val matchDocFlow = MutableSharedFlow<Result<RemoteMatchState?>>(replay = 1).apply {
            tryEmit(Result.success(remoteMatch))
        }

        fun emitMatchDoc(match: RemoteMatchState?) {
            remoteMatch = match
            matchDocFlow.tryEmit(Result.success(match))
        }

        fun emitMatchDocError(error: Throwable) {
            matchDocFlow.tryEmit(Result.failure(error))
        }

        override fun observeMatchDoc(matchId: String): kotlinx.coroutines.flow.Flow<Result<RemoteMatchState?>> {
            observeMatchDocCallCount++
            lastObservedMatchId = matchId
            return matchDocFlow
        }

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
        var syncResult: WorkerSyncResult = WorkerSyncResult.NoMatch("No match")
        var respondCallCount: Int = 0

        override suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult {
            return syncResult
        }

        override suspend fun respondToMatch(
            firebaseIdToken: String,
            payload: DecisionRequest
        ): DecisionResponse {
            respondCallCount++
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

        override suspend fun getMatchHistory(firebaseIdToken: String): List<com.pasindu.nursingotapp.transfer.data.model.TransferMatchHistoryItem> {
            return emptyList()
        }

        override suspend fun getCompletionStatus(
            firebaseIdToken: String,
            matchId: String
        ): com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus {
            return com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus(
                matchId = matchId,
                status = "CONFIRMED"
            )
        }

        override suspend fun reportCompletion(
            firebaseIdToken: String,
            matchId: String
        ): com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus {
            return com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus(
                matchId = matchId,
                status = "CONFIRMED",
                callerReportedComplete = true,
                completedParticipants = 1,
                totalParticipants = 2
            )
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
        activeViewModels.clear()
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
        activeViewModels += viewModel
        testDispatcher.scheduler.runCurrent()
        viewModel.onScreenExit()

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
        if (::viewModel.isInitialized) {
            viewModel.onScreenExit()
        }
        stopAllPolling()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TransferMatchViewModel =
        TransferMatchViewModel(repository, hospitalRepo).also {
            activeViewModels += it
            if (::viewModel.isInitialized) viewModel.onScreenExit()
            viewModel = it
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
    fun apiClient_respondToMatch_capturesTokenAndPayload() = runVmTest {
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
    fun repository_respondToMatch_accept_updatesCacheToConfirmed() = runVmTest {
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
    fun repository_respondToMatch_reject_updatesCacheToCancelled() = runVmTest {
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
    fun repository_respondToMatch_networkFailure_returnsFailure() = runVmTest {
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
    fun viewModel_acceptMatch_setsLoadingThenAccepted() = runVmTest {
        runCurrent()
        workerApiClient.respondResult = Result.success(
            DecisionResponse(
                matchId = "MATCH-001",
                newStatus = "PENDING_CONFIRMATION",
                expiresAt = "2026-10-04T12:00:00Z",
                decisionApplied = true
            )
        )

        viewModel.acceptMatch("MATCH-001")
        assertTrue("isSubmitting should be true while in flight", viewModel.isSubmitting.value)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is TransferMatchUiState.AlreadyAccepted)
        assertEquals(MatchDecisionStatus.ACCEPTED, (state as TransferMatchUiState.AlreadyAccepted).match.myStatus)
        assertFalse(viewModel.isSubmitting.value)
        viewModel.onScreenExit()
    }

    // 8. ViewModel REJECT
    @Test
    fun viewModel_rejectMatch_setsLoadingThenRejected() = runVmTest {
        runCurrent()
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
        assertTrue("isSubmitting should be true while in flight", viewModel.isSubmitting.value)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is TransferMatchUiState.AlreadyRejected)
        assertEquals(MatchDecisionStatus.REJECTED, (state as TransferMatchUiState.AlreadyRejected).match.myStatus)
        assertTrue(completed)
        assertFalse(viewModel.isSubmitting.value)
    }

    // 9. Duplicate action and error protection
    @Test
    fun viewModel_duplicateActionProtection_ignoresSecondConcurrentCall() = runVmTest {
        runCurrent()
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

        testScheduler.runCurrent()
        assertEquals(1, workerApiClient.respondCallCount)
        assertTrue("Expected initial AlreadyAccepted state, but actual UI state was: ${viewModel.uiState.value}", viewModel.uiState.value is TransferMatchUiState.AlreadyAccepted)
        viewModel.onScreenExit()
    }

    @Test
    fun viewModel_failure_setsErrorState() = runVmTest {
        runCurrent()
        workerApiClient.respondResult = Result.failure(Exception("Worker timeout 504"))

        viewModel.acceptMatch("MATCH-001")
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is TransferMatchUiState.Error)
        assertEquals("The matching server timed out. Please check your connection and try again.", (state as TransferMatchUiState.Error).message)
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
    fun viewModel_acceptMatch_advancingToChatOpen_setsChatOpenState() = runVmTest {
        runCurrent()
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
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue("Expected ChatOpen state", state is TransferMatchUiState.ChatOpen)
        val match = (state as TransferMatchUiState.ChatOpen).match
        assertEquals("CHAT_OPEN", match.serverStatus)
        assertEquals(MatchDecisionStatus.ACCEPTED, match.myStatus)
        assertFalse(viewModel.isSubmitting.value)
    }

    // 14. ViewModel: CONFIRM leading to CONFIRMED sets Confirmed state
    @Test
    fun viewModel_confirmMatch_advancingToConfirmed_setsConfirmedState() = runVmTest {
        runCurrent()
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
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue("Expected Confirmed state", state is TransferMatchUiState.Confirmed)
        val match = (state as TransferMatchUiState.Confirmed).match
        assertEquals("CONFIRMED", match.serverStatus)
        assertTrue(match.myConfirmed)
        assertFalse(viewModel.isSubmitting.value)
    }

    // 15. Repository: Room persistence preserves matchStatus during workflow advancement
    @Test
    fun repository_respondToMatch_persistsChatOpenAndConfirmedStatusToRoom() = runVmTest {
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

    // 16. B3: toUiModel accurately maps acceptedBy / rejectedBy flags to myStatus and partnerStatus
    @Test
    fun toUiModel_mapsAcceptedAndRejectedFlagsToMatchDecisionStatus() = runVmTest {
        val matchFound = WorkerSyncResult.MatchFound(
            matchId = "MATCH-B3",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "nurse-a",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION",
            acceptedByA = true,
            acceptedByB = false,
            rejectedByA = false,
            rejectedByB = false
        )

        // Exercise the WorkerSyncResult -> UI mapping path. A server-side MATCHED
        // request takes the repository's authoritative Firestore fast path instead.
        remoteDataSource.remoteRequest = null
        val fakeDoc = RemoteMatchState(
            matchId = matchFound.matchId,
            matchType = matchFound.matchType,
            directMatch = matchFound.directMatch,
            createdAt = matchFound.createdAt,
            expiresAt = matchFound.expiresAt,
            status = matchFound.status,
            acceptedByA = matchFound.acceptedByA,
            acceptedByB = matchFound.acceptedByB
        )
        remoteDataSource.emitMatchDoc(fakeDoc)
        dao.entity = dao.entity!!.copy(
            requestStatus = TransferRequestStatus.PENDING.name,
            matchCycleId = null,
            syncStatus = CacheSyncStatus.PENDING.name
        )
        workerApiClient.syncResult = matchFound
        tokenProvider.userId = "nurse-a"

        val viewModel = createViewModel()
        try {
            testScheduler.runCurrent()

            val state = viewModel.uiState.value
            assertTrue("Expected AlreadyAccepted, but actual UI state was: $state", state is TransferMatchUiState.AlreadyAccepted)
            val match = (state as TransferMatchUiState.AlreadyAccepted).match
            assertEquals(MatchDecisionStatus.ACCEPTED, match.myStatus)
            assertEquals(MatchDecisionStatus.PENDING, match.partnerStatus)
        } finally {
            viewModel.onScreenExit()
        }
    }

    // 17. B4: Match polling transitions to error/pool when NoMatch is received
    @Test
    fun matchPolling_transitionsToPoolMessageWhenNoMatchReceived() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-B4",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "nurse-a",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION",
            acceptedByA = true
        )

        // Start from the pool so the initial match comes from the fake Worker.
        // On the next poll, the now-MATCHED local row plus no server request should
        // reconcile to NoMatch and move the ViewModel out of its waiting state.
        remoteDataSource.remoteRequest = null
        remoteDataSource.remoteMatch = null
        dao.entity = dao.entity!!.copy(
            requestStatus = TransferRequestStatus.PENDING.name,
            matchCycleId = null,
            syncStatus = CacheSyncStatus.PENDING.name
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "nurse-a"

        val viewModel = createViewModel()
        try {
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value is TransferMatchUiState.AlreadyAccepted)

            // Simulate the server-side match document being cancelled, then let the
            // real-time snapshot listener deliver the updated match doc.
            remoteDataSource.emitMatchDoc(
                RemoteMatchState(
                    matchId = "MATCH-B4",
                    matchType = "DIRECT_2_WAY",
                    directMatch = initialMatch.directMatch,
                    status = "CANCELLED",
                    createdAt = "2026-10-15T10:00:00Z",
                    expiresAt = "2026-10-15T12:00:00Z",
                    acceptedByA = true
                )
            )
            testScheduler.runCurrent()

            val finalState = viewModel.uiState.value
            assertTrue("Listener on a cancelled match transitions to AlreadyRejected", finalState is TransferMatchUiState.AlreadyRejected)
        } finally {
            viewModel.onScreenExit()
        }
    }
    // ---------------------------------------------------------------------------
    // MAL-02: Match Screen Polling Lifecycle Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `MAL-02 - match polling starts when match is loaded in waiting state`() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-MAL02",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION"
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertTrue("ViewModel should be in MatchFound state", vm.uiState.value is TransferMatchUiState.MatchFound)
        assertTrue("Polling must be active while in MatchFound waiting state", vm.isPollingActive)
        vm.onScreenExit()
    }

    @Test
    fun `MAL-02 - onScreenExit stops active match polling`() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-MAL02-EXIT",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION"
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()
        assertTrue("Polling should be active initially", vm.isPollingActive)

        // Screen exits / leaves foreground
        vm.onScreenExit()
        assertFalse("Polling must be cancelled immediately when screen exits", vm.isPollingActive)
    }

    @Test
    fun `MAL-02 - onScreenResume restarts polling without duplicate jobs`() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-MAL02-RESUME",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION"
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()
        vm.onScreenExit()
        assertFalse(vm.isPollingActive)

        // Returning to foreground resumes polling
        vm.onScreenResume()
        assertTrue("Polling must resume when returning to foreground in waiting state", vm.isPollingActive)

        // Duplicate resume calls are idempotent
        vm.onScreenResume()
        assertTrue("Duplicate onScreenResume must be idempotent and remain active", vm.isPollingActive)
        vm.onScreenExit()
    }

    @Test
    fun `MAL-02 - onScreenResume does not start polling in terminal or chat open states`() = runVmTest {
        val chatOpenMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-MAL02-CHAT",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "CHAT_OPEN",
            acceptedByA = true,
            acceptedByB = true
        )
        remoteDataSource.remoteRequest = null
        remoteDataSource.remoteMatch = null
        dao.entity = dao.entity!!.copy(
            requestStatus = TransferRequestStatus.PENDING.name,
            matchCycleId = null,
            syncStatus = CacheSyncStatus.PENDING.name
        )
        workerApiClient.syncResult = chatOpenMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()
        assertTrue("State should be ChatOpen", vm.uiState.value is TransferMatchUiState.ChatOpen)
        assertFalse("Polling must not run for CHAT_OPEN state", vm.isPollingActive)

        // Calling onScreenResume must NOT start polling in non-waiting state
        vm.onScreenResume()
        assertFalse("onScreenResume must not start polling when not in waiting state", vm.isPollingActive)
        vm.onScreenExit()
    }

    // ---------------------------------------------------------------------------
    // Step 3.2: Robust Polling Error Handling & Backoff Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `Step 3_2 - consecutive poll failures - 1 and 2 failures do not show warning, 3 failures shows warning and degraded`() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-MAL05",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION"
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertTrue("Initial state must be MatchFound", vm.uiState.value is TransferMatchUiState.MatchFound)
        assertFalse("Initially should not be degraded", vm.isOfflineOrDegraded.value)
        assertNull("Initially poll warning must be null", vm.pollWarning.value)

        // Listener disconnected, fallback polling starts
        remoteDataSource.emitMatchDocError(java.io.IOException("Listener down"))
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.isFallbackPollingActive)

        // First network error during fallback polling (delay 30s)
        remoteDataSource.fetchRequestError = java.io.IOException("Network timeout")
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse("Single failure should not mark as degraded", vm.isOfflineOrDegraded.value)
        assertNull("Single failure should not set poll warning", vm.pollWarning.value)

        // Second network error during fallback polling (delay 30s)
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse("Two failures should not mark as degraded", vm.isOfflineOrDegraded.value)
        assertNull("Two failures should not set poll warning", vm.pollWarning.value)

        // Third network error during fallback polling (delay 30s)
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue("Three consecutive failures must mark as degraded", vm.isOfflineOrDegraded.value)
        assertEquals("Three consecutive failures must set warning", POLL_WARNING_MESSAGE, vm.pollWarning.value)

        // Next poll succeeds (delay 30s): failure counter and warning reset
        remoteDataSource.fetchRequestError = null
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse("Successful poll resets degraded state", vm.isOfflineOrDegraded.value)
        assertNull("Successful poll clears poll warning", vm.pollWarning.value)
        vm.onScreenExit()
    }

    @Test
    fun `Step 3_2 - retryPolling resets failure counter, warning, and degraded flag`() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-MAL05-RETRY",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION"
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        // Trigger listener error to engage fallback polling
        remoteDataSource.emitMatchDocError(java.io.IOException("Offline"))
        testDispatcher.scheduler.runCurrent()

        // 3 failures during fallback polling (30s each)
        remoteDataSource.fetchRequestError = java.io.IOException("Offline")
        testDispatcher.scheduler.advanceTimeBy(30_000L) // Failure 1
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(30_000L) // Failure 2
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(30_000L) // Failure 3
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.isOfflineOrDegraded.value)
        assertEquals(POLL_WARNING_MESSAGE, vm.pollWarning.value)

        // User taps retry: network restored
        remoteDataSource.fetchRequestError = null
        vm.retryPolling()
        testDispatcher.scheduler.runCurrent()
        assertFalse("retryPolling must clear degraded state", vm.isOfflineOrDegraded.value)
        assertNull("retryPolling must clear poll warning", vm.pollWarning.value)
        vm.onScreenExit()
    }

    @Test
    fun `Step 3_2 - calculatePollBackoff schedule follows 3s, 6s, 12s, and capped at 30s`() {
        assertEquals("0 failures: base delay 3s", 3_000L, calculatePollBackoff(0))
        assertEquals("1 failure: 3s delay", 3_000L, calculatePollBackoff(1))
        assertEquals("2 failures: 6s delay", 6_000L, calculatePollBackoff(2))
        assertEquals("3 failures: 12s delay", 12_000L, calculatePollBackoff(3))
        assertEquals("4 failures: capped at 30s", 30_000L, calculatePollBackoff(4))
        assertEquals("5 failures: capped at 30s", 30_000L, calculatePollBackoff(5))
        assertEquals("10 failures: capped at 30s", 30_000L, calculatePollBackoff(10))
    }

    @Test
    fun `Step 3_2 - CancellationException does not increment failure count or set warning`() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-CANCEL-TEST",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION"
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.isPollingActive)
        assertFalse(vm.isOfflineOrDegraded.value)
        assertNull(vm.pollWarning.value)

        // Screen exits -> cancels polling job cleanly
        vm.onScreenExit()
        testDispatcher.scheduler.runCurrent()

        assertFalse(vm.isPollingActive)
        assertFalse("Cancellation must not set degraded flag", vm.isOfflineOrDegraded.value)
        assertNull("Cancellation must not set poll warning", vm.pollWarning.value)
    }

    @Test
    fun `Step 3_2 - AuthError triggers token refresh once before counting as failure`() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-AUTH-REFRESH",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION"
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertEquals("Initially token refresh count is 0", 0, tokenProvider.refreshCount)

        // Trigger listener error to engage fallback polling
        remoteDataSource.emitMatchDocError(java.io.IOException("Auth needed"))
        testDispatcher.scheduler.runCurrent()

        // Make fallback syncActiveRequest return AuthError by clearing user ID from token provider
        tokenProvider.userId = null
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()

        assertEquals("First AuthError forces token refresh once", 1, tokenProvider.refreshCount)
        assertNull("First AuthError should not count towards consecutive failures", vm.pollWarning.value)

        vm.onScreenExit()
    }

    @Test
    fun `Step 3_2 - Polling continues after recoverable failures until terminal state`() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-RECOVERABLE",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION"
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.isPollingActive)

        // Trigger listener error to engage fallback polling
        remoteDataSource.emitMatchDocError(java.io.IOException("Socket timeout"))
        testDispatcher.scheduler.runCurrent()

        // Inject network error into fallback polling
        remoteDataSource.fetchRequestError = java.io.IOException("Socket timeout")
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()

        // Polling remains active despite network failure
        assertTrue("Polling must remain active after recoverable failure", vm.isPollingActive)

        // Restore network and listener delivers a terminal state (CONFIRMED)
        remoteDataSource.fetchRequestError = null
        remoteDataSource.emitMatchDoc(remoteDataSource.remoteMatch?.copy(status = "CONFIRMED"))
        testDispatcher.scheduler.runCurrent()

        assertTrue("Terminal CONFIRMED state reached", vm.uiState.value is TransferMatchUiState.Confirmed)
        assertFalse("Polling terminates once terminal state is reached", vm.isPollingActive)
        vm.onScreenExit()
    }

    @Test
    fun `Step 3_2 - worker error code EXPIRED maps to Expired UI state and stops polling`() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-EXPIRED-TEST",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION"
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.isPollingActive)

        // Server responds with EXPIRED error
        workerApiClient.respondResult = Result.failure(
            TransferWorkerApiException(
                errorCode = "EXPIRED",
                statusCode = 410,
                message = "The 48-hour decision window has expired"
            )
        )

        vm.acceptMatch("MATCH-EXPIRED-TEST")
        testDispatcher.scheduler.runCurrent()

        assertTrue("UI state must transition to Expired", vm.uiState.value is TransferMatchUiState.Expired)
        assertFalse("Polling must terminate when match expires", vm.isPollingActive)
        vm.onScreenExit()
    }

    // ---------------------------------------------------------------------------
    // Step 3 Domain Outcome & Validation Tests
    // ---------------------------------------------------------------------------

    @Test
    fun syncActiveRequest_returningRequestCompleted_transitionsToCompletedError() = runVmTest {
        remoteDataSource.remoteRequest = null
        remoteDataSource.remoteMatch = null
        dao.entity = dao.entity!!.copy(
            requestStatus = TransferRequestStatus.COMPLETED.name,
            matchCycleId = null,
            syncStatus = CacheSyncStatus.SYNCED.name
        )
        tokenProvider.userId = "nurse-a"

        val vm = createViewModel()
        try {
            testScheduler.runCurrent()
            val state = vm.uiState.value
            assertTrue("Expected Error state on completed request", state is TransferMatchUiState.Error)
            assertEquals("This mutual transfer has already been completed.", (state as TransferMatchUiState.Error).message)
        } finally {
            vm.onScreenExit()
        }
    }

    @Test
    fun syncActiveRequest_returningRequestWithdrawn_transitionsToWithdrawnError() = runVmTest {
        remoteDataSource.remoteRequest = null
        remoteDataSource.remoteMatch = null
        dao.entity = dao.entity!!.copy(
            requestStatus = TransferRequestStatus.WITHDRAWN.name,
            matchCycleId = null,
            syncStatus = CacheSyncStatus.SYNCED.name
        )
        tokenProvider.userId = "nurse-a"

        val vm = createViewModel()
        try {
            testScheduler.runCurrent()
            val state = vm.uiState.value
            assertTrue("Expected Error state on withdrawn request", state is TransferMatchUiState.Error)
            assertEquals("Your mutual transfer request has been withdrawn.", (state as TransferMatchUiState.Error).message)
        } finally {
            vm.onScreenExit()
        }
    }

    @Test
    fun syncActiveRequest_returningReturnedToPool_transitionsToReturnedToPoolError() = runVmTest {
        remoteDataSource.remoteRequest = RemoteTransferRequestState(
            status = "SEARCHING",
            locked = false,
            currentMatchId = null,
            updatedAt = 5000L,
            currentHospitalId = "HOSP-001"
        )
        remoteDataSource.remoteMatch = null
        dao.entity = dao.entity!!.copy(
            requestStatus = TransferRequestStatus.MATCHED.name,
            matchCycleId = "MATCH-001",
            matchStatus = "CANCELLED",
            syncStatus = CacheSyncStatus.SYNCED.name
        )
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        try {
            testScheduler.runCurrent()
            val state = vm.uiState.value
            assertTrue("Expected Error state on returned to pool", state is TransferMatchUiState.Error)
            assertEquals("This match has ended. Your request has been returned to the matching pool.", (state as TransferMatchUiState.Error).message)
        } finally {
            vm.onScreenExit()
        }
    }

    @Test
    fun nonParticipantUid_in2WayMatch_rejectedWithExplicitError() = runVmTest {
        val directMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-2WAY-NONPART",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "nurse-a",
                nurseBUid = "nurse-b",
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
                priorityReason = "Direct 2-way"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z"
        )

        try {
            val method = TransferMatchViewModel::class.java.getDeclaredMethod(
                "toUiModel",
                WorkerSyncResult.MatchFound::class.java,
                String::class.java,
                List::class.java
            )
            method.isAccessible = true
            method.invoke(viewModel, directMatch, "nurse-intruder", emptyList<HospitalReference>())
            org.junit.Assert.fail("Expected IllegalArgumentException for non-participant UID")
        } catch (e: java.lang.reflect.InvocationTargetException) {
            assertTrue(e.targetException is IllegalArgumentException)
            assertTrue(e.targetException.message?.contains("is not a participant in 2-way match") == true)
        }
    }

    @Test
    fun nonParticipantUid_in3WayMatch_rejectedWithExplicitError() = runVmTest {
        val threeWayMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-3WAY-NONPART",
            matchType = "THREE_WAY",
            threeWayMatch = WorkerThreeWayMatch(
                nurseAUid = "nurse-a",
                nurseBUid = "nurse-b",
                nurseCUid = "nurse-c",
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
                priorityReason = "3-way circular"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z"
        )

        try {
            val method = TransferMatchViewModel::class.java.getDeclaredMethod(
                "toUiModel",
                WorkerSyncResult.MatchFound::class.java,
                String::class.java,
                List::class.java
            )
            method.isAccessible = true
            method.invoke(viewModel, threeWayMatch, "nurse-intruder", emptyList<HospitalReference>())
            org.junit.Assert.fail("Expected IllegalArgumentException for non-participant UID in 3-way match")
        } catch (e: java.lang.reflect.InvocationTargetException) {
            assertTrue(e.targetException is IllegalArgumentException)
            assertTrue(e.targetException.message?.contains("is not a participant in 3-way match") == true)
        }
    }
    @Test
    fun `MAL-02 - match polling stops immediately when server status transitions to EXPIRED`() = runVmTest {
        tokenProvider.userId = "user-123"
        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()
        assertTrue("Initially should be MatchFound", vm.uiState.value is TransferMatchUiState.MatchFound)
        assertTrue("Polling should be active in waiting state", vm.isPollingActive)

        // Snapshot listener delivers EXPIRED
        remoteDataSource.emitMatchDoc(remoteDataSource.remoteMatch!!.copy(status = "EXPIRED"))
        testDispatcher.scheduler.runCurrent()

        assertTrue("UI state must be Expired", vm.uiState.value is TransferMatchUiState.Expired)
        assertFalse("Polling must terminate when match expires", vm.isPollingActive)
        vm.onScreenExit()
    }

    @Test
    fun `MAL-02 - brand new match ID received during polling maps consistently and stops if terminal`() = runVmTest {
        tokenProvider.userId = "user-123"
        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value is TransferMatchUiState.MatchFound)
        assertTrue(vm.isPollingActive)

        // A brand new match ID arrives and is observed
        remoteDataSource.remoteRequest = remoteDataSource.remoteRequest!!.copy(currentMatchId = "MATCH-NEW-EXPIRED")
        val expiredMatch = remoteDataSource.remoteMatch!!.copy(
            matchId = "MATCH-NEW-EXPIRED",
            status = "EXPIRED"
        )
        remoteDataSource.emitMatchDoc(expiredMatch)
        testDispatcher.scheduler.runCurrent()

        assertTrue("UI state must map to Expired for new match ID", vm.uiState.value is TransferMatchUiState.Expired)
        assertEquals("MATCH-NEW-EXPIRED", (vm.uiState.value as TransferMatchUiState.Expired).match.matchId)
        assertFalse("Polling must terminate and not loop endlessly on new expired match", vm.isPollingActive)
        vm.onScreenExit()
    }

    // ---------------------------------------------------------------------------
    // Step 3.3: Firestore Snapshot Listener & Fallback Polling Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `Step 3_3 - listener receives match update and updates UI state`() = runVmTest {
        tokenProvider.userId = "user-123"
        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.isListenerActive)
        assertFalse(vm.isFallbackPollingActive)
        assertEquals("MATCH-001", remoteDataSource.lastObservedMatchId)

        // Snapshot listener delivers partner accepted update
        val updated = remoteDataSource.remoteMatch!!.copy(
            acceptedByA = false,
            acceptedByB = true
        )
        remoteDataSource.emitMatchDoc(updated)
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.value is TransferMatchUiState.MatchFound)
        val state = vm.uiState.value as TransferMatchUiState.MatchFound
        assertEquals(MatchDecisionStatus.ACCEPTED, state.match.partnerStatus)
        assertEquals(MatchDecisionStatus.PENDING, state.match.myStatus)
        assertTrue(vm.isListenerActive)
        assertFalse(vm.isFallbackPollingActive)
        vm.onScreenExit()
    }

    @Test
    fun `Step 3_3 - listener-driven terminal states stop monitoring immediately`() = runVmTest {
        tokenProvider.userId = "user-123"
        val terminalStatuses = listOf("CHAT_OPEN", "CONFIRMED", "CANCELLED", "EXPIRED")

        for (terminal in terminalStatuses) {
            val baseMatch = remoteDataSource.remoteMatch!!.copy(status = "PENDING_CONFIRMATION")
            remoteDataSource.emitMatchDoc(baseMatch)
            val vm = createViewModel()
            testDispatcher.scheduler.runCurrent()
            assertTrue("Listener must be active initially for $terminal", vm.isListenerActive)

            remoteDataSource.emitMatchDoc(baseMatch.copy(status = terminal))
            testDispatcher.scheduler.runCurrent()

            assertFalse("Listener must terminate on $terminal", vm.isListenerActive)
            assertFalse("Fallback must not run on $terminal", vm.isFallbackPollingActive)
            assertFalse("isPollingActive must be false on $terminal", vm.isPollingActive)

            val state = vm.uiState.value
            when (terminal) {
                "CHAT_OPEN" -> assertTrue(state is TransferMatchUiState.ChatOpen)
                "CONFIRMED" -> assertTrue(state is TransferMatchUiState.Confirmed)
                "CANCELLED" -> assertTrue(state is TransferMatchUiState.AlreadyRejected)
                "EXPIRED" -> assertTrue(state is TransferMatchUiState.Expired)
            }
            vm.onScreenExit()
        }
    }

    @Test
    fun `Step 3_3 - healthy listener does not trigger fallback polling`() = runVmTest {
        tokenProvider.userId = "user-123"
        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.isListenerActive)
        assertFalse(vm.isFallbackPollingActive)

        // Advance virtual time by 90 seconds (longer than fallback polling intervals)
        testDispatcher.scheduler.advanceTimeBy(90_000L)
        testDispatcher.scheduler.runCurrent()

        // Fallback polling should never have started because listener is healthy
        assertTrue("Listener remains active", vm.isListenerActive)
        assertFalse("Fallback polling must NOT be active while listener is healthy", vm.isFallbackPollingActive)
        assertNull(vm.pollWarning.value)
        assertFalse(vm.isOfflineOrDegraded.value)
        vm.onScreenExit()
    }

    @Test
    fun `Step 3_3 - listener error activates 30-second fallback poll and sets warning at 3 failures`() = runVmTest {
        tokenProvider.userId = "user-123"
        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.isListenerActive)
        assertFalse(vm.isFallbackPollingActive)

        // Simulate listener error
        remoteDataSource.emitMatchDocError(java.io.IOException("Firestore listener connection lost"))
        testDispatcher.scheduler.runCurrent()

        // Fallback polling starts immediately
        assertTrue("Fallback polling should be active", vm.isFallbackPollingActive)

        // Let fallback poll fail 1st time (delay 30s)
        remoteDataSource.fetchRequestError = java.io.IOException("Sync network down")
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertNull("Single fallback failure should not set warning", vm.pollWarning.value)

        // Let fallback poll fail 2nd time (delay 30s)
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertNull("Two fallback failures should not set warning", vm.pollWarning.value)

        // Let fallback poll fail 3rd time (delay 30s)
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(POLL_WARNING_MESSAGE, vm.pollWarning.value)
        assertTrue(vm.isOfflineOrDegraded.value)

        vm.onScreenExit()
    }

    @Test
    fun `Step 3_3 - listener recovery stops fallback polling and clears warning`() = runVmTest {
        tokenProvider.userId = "user-123"
        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        // Listener fails and triggers fallback
        remoteDataSource.emitMatchDocError(java.io.IOException("Firestore listener error"))
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.isFallbackPollingActive)

        // Fail 3 times to get warning
        remoteDataSource.fetchRequestError = java.io.IOException("Network down")
        testDispatcher.scheduler.advanceTimeBy(90_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(POLL_WARNING_MESSAGE, vm.pollWarning.value)
        assertTrue(vm.isOfflineOrDegraded.value)

        // Listener recovers and delivers healthy snapshot
        remoteDataSource.emitMatchDoc(remoteDataSource.remoteMatch)
        testDispatcher.scheduler.runCurrent()

        assertFalse("Fallback polling must stop upon listener recovery", vm.isFallbackPollingActive)
        assertNull("Poll warning must be cleared", vm.pollWarning.value)
        assertFalse("Degraded state must be cleared", vm.isOfflineOrDegraded.value)

        vm.onScreenExit()
    }

    @Test
    fun `Step 3_3 - changing match ID cancels previous listener and observes new match`() = runVmTest {
        tokenProvider.userId = "user-123"
        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertEquals("MATCH-001", remoteDataSource.lastObservedMatchId)
        val initialCallCount = remoteDataSource.observeMatchDocCallCount

        // A new match proposal arrives from server with a different matchId
        val newMatch = remoteDataSource.remoteMatch!!.copy(matchId = "MATCH-999")
        remoteDataSource.remoteRequest = remoteDataSource.remoteRequest!!.copy(currentMatchId = "MATCH-999")
        remoteDataSource.remoteMatch = newMatch
        workerApiClient.syncResult = WorkerSyncResult.MatchFound(
            matchId = "MATCH-999",
            matchType = "DIRECT_2_WAY",
            directMatch = newMatch.directMatch,
            createdAt = newMatch.createdAt,
            expiresAt = newMatch.expiresAt,
            status = "PENDING_CONFIRMATION"
        )

        vm.loadCurrentMatch()
        testDispatcher.scheduler.runCurrent()

        assertEquals("MATCH-999", remoteDataSource.lastObservedMatchId)
        assertEquals(initialCallCount + 1, remoteDataSource.observeMatchDocCallCount)
        assertTrue(vm.isListenerActive)

        vm.onScreenExit()
    }

    @Test
    fun `Step 3_3 - onScreenExit and onCleared remove listener and stop fallback polling`() = runVmTest {
        tokenProvider.userId = "user-123"
        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.isListenerActive)

        vm.onScreenExit()
        assertFalse(vm.isListenerActive)
        assertFalse(vm.isFallbackPollingActive)
        assertFalse(vm.isPollingActive)

        // Resume
        vm.onScreenResume()
        assertTrue(vm.isListenerActive)

        // Trigger fallback then exit
        remoteDataSource.emitMatchDocError(java.io.IOException("Error"))
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.isFallbackPollingActive)

        vm.onScreenExit()
        assertFalse(vm.isListenerActive)
        assertFalse(vm.isFallbackPollingActive)
        assertFalse(vm.isPollingActive)
    }

    // ---------------------------------------------------------------------------
    // Step 3.4: Token Refresh on HTTP 401 & Single Retry Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `Step 3_4 - HTTP 401 on respondToMatch triggers forceRefresh and succeeds on retry`() = runVmTest {
        tokenProvider.idToken = "expired-token"
        tokenProvider.refreshedToken = "fresh-valid-token"

        var callCount = 0
        workerApiClient.respondResult = null
        val originalRespond = workerApiClient

        // First call fails with 401, second call succeeds
        var capturedTokens = mutableListOf<String>()
        val customApiClient = object : TransferWorkerApiClient by workerApiClient {
            override suspend fun respondToMatch(firebaseIdToken: String, payload: DecisionRequest): DecisionResponse {
                capturedTokens.add(firebaseIdToken)
                callCount++
                if (callCount == 1) {
                    throw TransferWorkerApiException(
                        errorCode = "UNAUTHENTICATED",
                        statusCode = 401,
                        message = "ID token expired"
                    )
                }
                return DecisionResponse(
                    matchId = payload.matchId,
                    newStatus = "CONFIRMED",
                    expiresAt = "2026-10-15T12:00:00Z",
                    decisionApplied = true
                )
            }
        }

        val testRepo = TransferRequestRepository(
            dao = dao,
            profileDao = profileDao,
            tokenProvider = tokenProvider,
            remoteDataSource = remoteDataSource,
            workerApiClient = customApiClient
        )

        val result = testRepo.respondToMatch("MATCH-001", Decision.ACCEPT)
        assertTrue("Decision should succeed after retry", result.isSuccess)
        assertEquals("Should have called worker twice (1 failure + 1 retry)", 2, callCount)
        assertEquals("Token provider forceRefresh count should be 1", 1, tokenProvider.refreshCount)
        assertEquals("First attempt used expired token", "expired-token", capturedTokens[0])
        assertEquals("Second attempt used refreshed token", "fresh-valid-token", capturedTokens[1])
    }

    @Test
    fun `Step 3_4 - repeated HTTP 401 terminates and does not enter infinite retry loop`() = runVmTest {
        tokenProvider.idToken = "expired-token"
        tokenProvider.refreshedToken = "still-invalid-token"

        var callCount = 0
        val customApiClient = object : TransferWorkerApiClient by workerApiClient {
            override suspend fun respondToMatch(firebaseIdToken: String, payload: DecisionRequest): DecisionResponse {
                callCount++
                throw TransferWorkerApiException(
                    errorCode = "UNAUTHENTICATED",
                    statusCode = 401,
                    message = "Persistent 401"
                )
            }
        }

        val testRepo = TransferRequestRepository(
            dao = dao,
            profileDao = profileDao,
            tokenProvider = tokenProvider,
            remoteDataSource = remoteDataSource,
            workerApiClient = customApiClient
        )

        val result = testRepo.respondToMatch("MATCH-001", Decision.ACCEPT)
        assertTrue("Persistent 401 should result in failure", result.isFailure)
        assertEquals("Should only retry once, terminating after 2 calls total", 2, callCount)
        assertEquals("Token provider forceRefresh count must be exactly 1", 1, tokenProvider.refreshCount)
    }

    @Test
    fun `Step 3_4 - non-401 error does not trigger token refresh or retry`() = runVmTest {
        tokenProvider.idToken = "valid-token"

        var callCount = 0
        val customApiClient = object : TransferWorkerApiClient by workerApiClient {
            override suspend fun respondToMatch(firebaseIdToken: String, payload: DecisionRequest): DecisionResponse {
                callCount++
                throw TransferWorkerApiException(
                    errorCode = "CONFLICT",
                    statusCode = 409,
                    message = "Conflict occurred"
                )
            }
        }

        val testRepo = TransferRequestRepository(
            dao = dao,
            profileDao = profileDao,
            tokenProvider = tokenProvider,
            remoteDataSource = remoteDataSource,
            workerApiClient = customApiClient
        )

        val result = testRepo.respondToMatch("MATCH-001", Decision.ACCEPT)
        assertTrue(result.isFailure)
        assertEquals("Non-401 error must not be retried", 1, callCount)
        assertEquals("forceRefresh must not be called on non-401 error", 0, tokenProvider.refreshCount)
    }

    @Test
    fun `Step 3_4 - token refresh failure surfaces correctly without retry`() = runVmTest {
        tokenProvider.idToken = "expired-token"
        // Configure token provider to fail when forceRefresh is requested
        val failingTokenProvider = object : TransferTokenProvider by tokenProvider {
            override suspend fun getFirebaseIdToken(forceRefresh: Boolean): String? {
                if (forceRefresh) return null
                return "expired-token"
            }
        }

        var callCount = 0
        val customApiClient = object : TransferWorkerApiClient by workerApiClient {
            override suspend fun respondToMatch(firebaseIdToken: String, payload: DecisionRequest): DecisionResponse {
                callCount++
                throw TransferWorkerApiException(
                    errorCode = "UNAUTHENTICATED",
                    statusCode = 401,
                    message = "Expired token"
                )
            }
        }

        val testRepo = TransferRequestRepository(
            dao = dao,
            profileDao = profileDao,
            tokenProvider = failingTokenProvider,
            remoteDataSource = remoteDataSource,
            workerApiClient = customApiClient
        )

        val result = testRepo.respondToMatch("MATCH-001", Decision.ACCEPT)
        assertTrue(result.isFailure)
        assertEquals("Should not retry if token refresh fails", 1, callCount)
        assertTrue("Error message mentions authentication failure", result.exceptionOrNull()?.message?.contains("Authentication failure") == true)
    }

    @Test
    fun `Step 3_4 - findAndLockMatch retries on AuthError with fresh token`() = runVmTest {
        tokenProvider.idToken = "stale-token"
        tokenProvider.refreshedToken = "fresh-token"

        var callCount = 0
        val capturedTokens = mutableListOf<String>()
        val customApiClient = object : TransferWorkerApiClient by workerApiClient {
            override suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult {
                callCount++
                capturedTokens.add(firebaseIdToken)
                return if (callCount == 1) {
                    WorkerSyncResult.AuthError("Token expired")
                } else {
                    WorkerSyncResult.NoMatch("No matches available")
                }
            }
        }

        // Set local entity to non-MATCHED SEARCHING state
        dao.entity = dao.entity!!.copy(
            requestStatus = TransferRequestStatus.PENDING.name,
            matchCycleId = null,
            syncStatus = CacheSyncStatus.PENDING.name
        )
        remoteDataSource.remoteRequest = null

        val testRepo = TransferRequestRepository(
            dao = dao,
            profileDao = profileDao,
            tokenProvider = tokenProvider,
            remoteDataSource = remoteDataSource,
            workerApiClient = customApiClient
        )

        val syncResult = testRepo.syncActiveRequest()
        assertTrue("Expected NoMatch after successful token refresh retry", syncResult is WorkerSyncResult.NoMatch)
        assertEquals("Should have called findAndLockMatch twice", 2, callCount)
        assertEquals("First call used stale token", "stale-token", capturedTokens[0])
        assertEquals("Second call used fresh token", "fresh-token", capturedTokens[1])
        assertEquals("forceRefresh count is 1", 1, tokenProvider.refreshCount)
    }

    // ---------------------------------------------------------------------------
    // Step 3.6: ViewModel Tests with Virtual Time (runTest + TestScheduler)
    // ---------------------------------------------------------------------------

    @Test
    fun `Step 3_6 - virtual time - new match ID already in CHAT_OPEN maps directly to ChatOpen without polling`() = runVmTest {
        tokenProvider.userId = "user-123"
        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertTrue("Initial state must be MatchFound", vm.uiState.value is TransferMatchUiState.MatchFound)
        assertTrue("Polling active for initial waiting match", vm.isPollingActive)

        // A brand new match ID arrives that is ALREADY in CHAT_OPEN state (Step 3.1)
        val newMatchId = "MATCH-NEW-CHAT-OPEN"
        remoteDataSource.remoteRequest = remoteDataSource.remoteRequest!!.copy(currentMatchId = newMatchId)
        val chatOpenMatch = remoteDataSource.remoteMatch!!.copy(
            matchId = newMatchId,
            status = "CHAT_OPEN",
            acceptedByA = true,
            acceptedByB = true
        )
        remoteDataSource.emitMatchDoc(chatOpenMatch)
        testDispatcher.scheduler.runCurrent()

        assertTrue(
            "New match arriving in CHAT_OPEN must immediately transition to ChatOpen UI state",
            vm.uiState.value is TransferMatchUiState.ChatOpen
        )
        assertEquals(newMatchId, (vm.uiState.value as TransferMatchUiState.ChatOpen).match.matchId)
        assertFalse(
            "Polling must stop immediately when new match is CHAT_OPEN",
            vm.isPollingActive
        )
        vm.onScreenExit()
    }

    @Test
    fun `Step 3_6 - virtual time - three consecutive failures show warning and recovery resets counter`() = runVmTest {
        val initialMatch = WorkerSyncResult.MatchFound(
            matchId = "MATCH-VIRTUAL-TIME",
            matchType = "DIRECT_2_WAY",
            directMatch = WorkerDirectMatch(
                nurseAUid = "user-123",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "HOSP-A",
                nurseBCurrentHospitalId = "HOSP-B",
                nurseADestinationHospitalId = "HOSP-B",
                nurseBDestinationHospitalId = "HOSP-A",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "Mutual 1st preference"
            ),
            createdAt = "2026-10-15T10:00:00Z",
            expiresAt = "2026-10-15T12:00:00Z",
            status = "PENDING_CONFIRMATION"
        )
        workerApiClient.syncResult = initialMatch
        tokenProvider.userId = "user-123"

        val vm = createViewModel()
        testDispatcher.scheduler.runCurrent()

        // 1. Snapshot listener disconnects, triggering fallback polling
        remoteDataSource.emitMatchDocError(java.io.IOException("Listener connection lost"))
        testDispatcher.scheduler.runCurrent()
        assertTrue("Fallback polling active when listener reports error", vm.isFallbackPollingActive)

        // 2. Simulate 3 consecutive poll failures using virtual time (no real delay)
        remoteDataSource.fetchRequestError = java.io.IOException("Network unreachable")

        // Failure 1 (virtual time +30s)
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse("1st failure must not display warning", vm.isOfflineOrDegraded.value)
        assertNull(vm.pollWarning.value)

        // Failure 2 (virtual time +30s)
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse("2nd failure must not display warning", vm.isOfflineOrDegraded.value)
        assertNull(vm.pollWarning.value)

        // Failure 3 (virtual time +30s)
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue("3rd consecutive failure must show degraded state", vm.isOfflineOrDegraded.value)
        assertEquals("3rd consecutive failure must show warning banner", POLL_WARNING_MESSAGE, vm.pollWarning.value)

        // 3. Recovery: next poll succeeds using virtual time
        remoteDataSource.fetchRequestError = null
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse("Recovery must clear degraded state", vm.isOfflineOrDegraded.value)
        assertNull("Recovery must clear poll warning banner", vm.pollWarning.value)

        // 4. Verify counter was reset: a single subsequent failure does not show warning
        remoteDataSource.fetchRequestError = java.io.IOException("Transient glitch")
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse("Single failure after recovery must not immediately re-show warning", vm.isOfflineOrDegraded.value)
        assertNull(vm.pollWarning.value)

        vm.onScreenExit()
    }
}
