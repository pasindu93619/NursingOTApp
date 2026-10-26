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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeDao : TransferActiveCacheDao {
        override suspend fun upsert(cache: TransferActiveCacheEntity) = Unit
        override fun observe(): Flow<TransferActiveCacheEntity?> = flowOf(null)
        override suspend fun getOnce(): TransferActiveCacheEntity? = null
        override suspend fun clear() = Unit
    }

    private class FakeProfileDao : ProfileDao {
        override suspend fun getProfileOnce(): ProfileEntity? = null
        override fun observeProfile(): Flow<ProfileEntity?> = flowOf(null)
        override suspend fun upsert(profile: ProfileEntity) = Unit
    }

    private class FakeTokenProvider : TransferTokenProvider {
        override suspend fun getFirebaseIdToken(forceRefresh: Boolean): String = "test-token"
        override suspend fun getCurrentUserId(): String = "test-user"
    }

    private class FakeRemoteDataSource : TransferRemoteDataSource {
        override suspend fun fetchTransferRequest(userId: String): Result<RemoteTransferRequestState?> = Result.success(null)
        override suspend fun fetchMatchDoc(matchId: String): Result<RemoteMatchState?> = Result.success(null)
        override fun observeMatchDoc(matchId: String): Flow<Result<RemoteMatchState?>> = flowOf(Result.success(null))
        override suspend fun publishTransferRequest(userId: String, request: TransferRequest): Result<Unit> = Result.success(Unit)
        override suspend fun withdrawTransferRequest(userId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeWorkerApiClient(
        var historyResult: List<TransferMatchHistoryItem> = emptyList(),
        var throwError: Boolean = false
    ) : TransferWorkerApiClient {
        override suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult = WorkerSyncResult.NoMatch("none")
        override suspend fun respondToMatch(firebaseIdToken: String, payload: DecisionRequest): DecisionResponse = error("not implemented")
        override suspend fun withdrawRequest(firebaseIdToken: String): WorkerWithdrawResponse = WorkerWithdrawResponse(true)
        override suspend fun getMatchHistory(firebaseIdToken: String): List<TransferMatchHistoryItem> {
            if (throwError) throw RuntimeException("Network unreachable")
            return historyResult
        }
        override suspend fun getCompletionStatus(firebaseIdToken: String, matchId: String): TransferCompletionStatus = error("not implemented")
        override suspend fun reportCompletion(firebaseIdToken: String, matchId: String): TransferCompletionStatus = error("not implemented")
    }

    private lateinit var workerApiClient: FakeWorkerApiClient
    private lateinit var repository: TransferRequestRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workerApiClient = FakeWorkerApiClient()
        repository = TransferRequestRepository(
            dao = FakeDao(),
            profileDao = FakeProfileDao(),
            tokenProvider = FakeTokenProvider(),
            remoteDataSource = FakeRemoteDataSource(),
            workerApiClient = workerApiClient
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `PROB-07 - refresh populates items on successful fetch`() = runTest {
        val sampleItem = TransferMatchHistoryItem(
            matchId = "match_123",
            matchType = "DIRECT_2_WAY",
            status = "COMPLETED",
            createdAt = "2026-10-20T10:00:00Z",
            endedAt = "2026-10-21T10:00:00Z",
            reason = "Completed by all nurses"
        )
        workerApiClient.historyResult = listOf(sampleItem)

        val vm = TransferHistoryViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.items.size)
        assertEquals("match_123", state.items[0].matchId)
    }

    @Test
    fun `PROB-07 - refresh captures error message on failure`() = runTest {
        workerApiClient.throwError = true

        val vm = TransferHistoryViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network unreachable", state.error)
        assertEquals(0, state.items.size)
    }
}
