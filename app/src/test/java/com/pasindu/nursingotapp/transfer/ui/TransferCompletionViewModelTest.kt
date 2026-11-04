package com.pasindu.nursingotapp.transfer.ui

import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.TransferActiveCacheDao
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity
import com.pasindu.nursingotapp.transfer.data.RemoteMatchState
import com.pasindu.nursingotapp.transfer.data.RemoteTransferRequestState
import com.pasindu.nursingotapp.transfer.data.TransferRemoteDataSource
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.TransferTokenProvider
import com.pasindu.nursingotapp.transfer.data.TransferWorkerApiClient
import com.pasindu.nursingotapp.transfer.data.model.DecisionRequest
import com.pasindu.nursingotapp.transfer.data.model.DecisionResponse
import com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus
import com.pasindu.nursingotapp.transfer.data.model.TransferMatchHistoryItem
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import com.pasindu.nursingotapp.transfer.data.model.WorkerWithdrawResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferCompletionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val activeViewModels = mutableListOf<TransferCompletionViewModel>()

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
        override suspend fun getProfileOnce(): ProfileEntity? = ProfileEntity(
            id = 1,
            fullName = "Nurse A",
            serviceNo = "SN-1",
            unit = "ICU",
            paySheetNo = "PS-1",
            grade = "Grade I",
            basicSalary = 50000.0,
            otRate = 350.0,
            updatedAt = 1000L
        )
        override fun observeProfile(): Flow<ProfileEntity?> = flowOf(null)
        override suspend fun upsert(profile: ProfileEntity) = Unit
    }

    private class FakeTokenProvider : TransferTokenProvider {
        override suspend fun getFirebaseIdToken(forceRefresh: Boolean): String = "mock-token"
        override suspend fun getCurrentUserId(): String = "user-123"
    }

    private class FakeRemoteDataSource : TransferRemoteDataSource {
        override suspend fun fetchTransferRequest(userId: String): Result<RemoteTransferRequestState?> = Result.success(null)
        override suspend fun fetchMatchDoc(matchId: String): Result<RemoteMatchState?> = Result.success(null)
        override fun observeMatchDoc(matchId: String): Flow<Result<RemoteMatchState?>> = flowOf(Result.success(null))
        override suspend fun publishTransferRequest(userId: String, request: TransferRequest): Result<Unit> = Result.success(Unit)
        override suspend fun withdrawTransferRequest(userId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeWorkerApiClient : TransferWorkerApiClient {
        var completionStatus: TransferCompletionStatus = TransferCompletionStatus(
            matchId = "MATCH-100",
            status = "CONFIRMED",
            callerReportedComplete = false,
            completedParticipants = 0,
            totalParticipants = 2
        )
        var completionError: Throwable? = null

        override suspend fun getCompletionStatus(firebaseIdToken: String, matchId: String): TransferCompletionStatus {
            completionError?.let { throw it }
            return completionStatus
        }

        override suspend fun reportCompletion(firebaseIdToken: String, matchId: String): TransferCompletionStatus {
            completionError?.let { throw it }
            return completionStatus
        }

        override suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult = WorkerSyncResult.NoMatch("none")
        override suspend fun respondToMatch(firebaseIdToken: String, payload: DecisionRequest): DecisionResponse = error("not implemented")
        override suspend fun withdrawRequest(firebaseIdToken: String): WorkerWithdrawResponse = WorkerWithdrawResponse(true)
        override suspend fun getMatchHistory(firebaseIdToken: String): List<TransferMatchHistoryItem> = emptyList()
    }

    private lateinit var fakeWorkerApiClient: FakeWorkerApiClient
    private lateinit var repository: TransferRequestRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeWorkerApiClient = FakeWorkerApiClient()
        repository = TransferRequestRepository(
            dao = FakeDao(),
            profileDao = FakeProfileDao(),
            tokenProvider = FakeTokenProvider(),
            remoteDataSource = FakeRemoteDataSource(),
            workerApiClient = fakeWorkerApiClient
        )
    }

    @After
    fun tearDown() {
        activeViewModels.forEach { it.stopAutoRefresh() }
        activeViewModels.clear()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TransferCompletionViewModel =
        TransferCompletionViewModel(repository).also { activeViewModels += it }

    @Test
    fun `MAL-07 - startAutoRefresh stops on terminal status COMPLETED`() = runTest {
        val vm = createViewModel()
        fakeWorkerApiClient.completionStatus = TransferCompletionStatus(
            matchId = "MATCH-100",
            status = "COMPLETED",
            callerReportedComplete = true,
            completedParticipants = 2,
            totalParticipants = 2
        )

        vm.startAutoRefresh("MATCH-100")
        vm.load("MATCH-100")
        testDispatcher.scheduler.runCurrent()

        assertFalse("Auto refresh must stop on COMPLETED status", vm.isAutoRefreshActive)
        assertEquals("COMPLETED", vm.uiState.value.status?.status)
    }

    @Test
    fun `MAL-07 - startAutoRefresh stops on terminal status CANCELLED`() = runTest {
        val vm = createViewModel()
        fakeWorkerApiClient.completionStatus = TransferCompletionStatus(
            matchId = "MATCH-100",
            status = "CANCELLED",
            callerReportedComplete = false,
            completedParticipants = 0,
            totalParticipants = 2
        )

        vm.startAutoRefresh("MATCH-100")
        vm.load("MATCH-100")
        testDispatcher.scheduler.runCurrent()

        assertFalse("Auto refresh must stop on CANCELLED status", vm.isAutoRefreshActive)
        assertEquals("CANCELLED", vm.uiState.value.status?.status)
    }

    @Test
    fun `MAL-07 - startAutoRefresh stops on terminal status EXPIRED`() = runTest {
        val vm = createViewModel()
        fakeWorkerApiClient.completionStatus = TransferCompletionStatus(
            matchId = "MATCH-100",
            status = "EXPIRED",
            callerReportedComplete = false,
            completedParticipants = 0,
            totalParticipants = 2
        )

        vm.startAutoRefresh("MATCH-100")
        vm.load("MATCH-100")
        testDispatcher.scheduler.runCurrent()

        assertFalse("Auto refresh must stop on EXPIRED status", vm.isAutoRefreshActive)
        assertEquals("EXPIRED", vm.uiState.value.status?.status)
    }

    @Test
    fun `MAL-07 - startAutoRefresh stops on non-recoverable HTTP 404 error`() = runTest {
        val vm = createViewModel()
        fakeWorkerApiClient.completionError = RuntimeException("HTTP 404 NotFound: match document not found")

        vm.startAutoRefresh("MATCH-100")
        vm.load("MATCH-100")
        testDispatcher.scheduler.runCurrent()

        assertFalse("Auto refresh must stop on HTTP 404", vm.isAutoRefreshActive)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `MAL-07 - startAutoRefresh stops on non-recoverable HTTP 401 error`() = runTest {
        val vm = createViewModel()
        fakeWorkerApiClient.completionError = RuntimeException("HTTP 401 Unauthorized")

        vm.startAutoRefresh("MATCH-100")
        vm.load("MATCH-100")
        testDispatcher.scheduler.runCurrent()

        assertFalse("Auto refresh must stop on HTTP 401", vm.isAutoRefreshActive)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `MAL-07 - stopAutoRefresh cancels active polling job`() = runTest {
        val vm = createViewModel()
        fakeWorkerApiClient.completionStatus = TransferCompletionStatus(
            matchId = "MATCH-100",
            status = "AWAITING_COMPLETION",
            callerReportedComplete = false,
            completedParticipants = 0,
            totalParticipants = 2
        )

        vm.startAutoRefresh("MATCH-100")
        assertTrue("Auto refresh should be active", vm.isAutoRefreshActive)

        vm.stopAutoRefresh()
        assertFalse("Auto refresh must be stopped after stopAutoRefresh()", vm.isAutoRefreshActive)
    }

    @Test
    fun `MAL-07 - startAutoRefresh loops periodically for non-terminal status`() = runTest {
        val vm = createViewModel()
        fakeWorkerApiClient.completionStatus = TransferCompletionStatus(
            matchId = "MATCH-100",
            status = "AWAITING_COMPLETION",
            callerReportedComplete = false,
            completedParticipants = 0,
            totalParticipants = 2
        )

        vm.startAutoRefresh("MATCH-100")
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.isAutoRefreshActive)

        // Advance past delay
        testDispatcher.scheduler.advanceTimeBy(20_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.isAutoRefreshActive)

        // Transition to terminal
        fakeWorkerApiClient.completionStatus = TransferCompletionStatus(
            matchId = "MATCH-100",
            status = "COMPLETED",
            callerReportedComplete = true,
            completedParticipants = 2,
            totalParticipants = 2
        )
        testDispatcher.scheduler.advanceTimeBy(20_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse("Auto refresh must stop when transition to COMPLETED is detected", vm.isAutoRefreshActive)
    }
}
