package com.pasindu.nursingotapp.transfer.ui

import android.content.Context
import android.content.ContextWrapper
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.TransferActiveCacheDao
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity
import com.pasindu.nursingotapp.transfer.data.HospitalReferenceRepository
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferInstructionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeDao : TransferActiveCacheDao {
        var entity: TransferActiveCacheEntity? = null
        override suspend fun upsert(cache: TransferActiveCacheEntity) { entity = cache }
        override fun observe(): Flow<TransferActiveCacheEntity?> = flowOf(entity)
        override suspend fun getOnce(): TransferActiveCacheEntity? = entity
        override suspend fun clear() { entity = null }
    }

    private class FakeProfileDao : ProfileDao {
        override suspend fun getProfileOnce(): ProfileEntity? = ProfileEntity(
            id = 1,
            fullName = "Nurse Silva",
            serviceNo = "SN-54321",
            unit = "ICU",
            paySheetNo = "PS-500",
            grade = "Grade I",
            basicSalary = 98000.0,
            otRate = 420.0,
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
        override suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult = WorkerSyncResult.NoMatch("none")
        override suspend fun respondToMatch(firebaseIdToken: String, payload: DecisionRequest): DecisionResponse = error("not implemented")
        override suspend fun withdrawRequest(firebaseIdToken: String): WorkerWithdrawResponse = WorkerWithdrawResponse(true)
        override suspend fun getMatchHistory(firebaseIdToken: String): List<TransferMatchHistoryItem> = emptyList()
        override suspend fun getCompletionStatus(firebaseIdToken: String, matchId: String): TransferCompletionStatus = error("not implemented")
        override suspend fun reportCompletion(firebaseIdToken: String, matchId: String): TransferCompletionStatus = error("not implemented")
    }

    private lateinit var repository: TransferRequestRepository
    private lateinit var hospitalRepo: HospitalReferenceRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val dummyContext: Context = ContextWrapper(null)
        hospitalRepo = HospitalReferenceRepository(dummyContext)
        repository = TransferRequestRepository(
            dao = FakeDao(),
            profileDao = FakeProfileDao(),
            tokenProvider = FakeTokenProvider(),
            remoteDataSource = FakeRemoteDataSource(),
            workerApiClient = FakeWorkerApiClient()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `E10 - checklist toggling accurately updates checked items StateFlow`() = runTest {
        val vm = TransferInstructionsViewModel(repository, hospitalRepo)
        assertTrue("Initially checked items must be empty", vm.checkedItems.value.isEmpty())

        vm.toggleCheckItem("doc_joint_application")
        assertEquals(true, vm.checkedItems.value["doc_joint_application"])

        vm.toggleCheckItem("doc_joint_application")
        assertEquals(false, vm.checkedItems.value["doc_joint_application"])

        vm.setItemChecked("doc_written_consent", true)
        assertEquals(true, vm.checkedItems.value["doc_written_consent"])
    }

    @Test
    fun `E10 - multiple checklist items persist their distinct states`() = runTest {
        val vm = TransferInstructionsViewModel(repository, hospitalRepo)
        vm.setItemChecked("doc_1", true)
        vm.setItemChecked("doc_2", false)
        vm.setItemChecked("doc_3", true)

        val items = vm.checkedItems.value
        assertEquals(true, items["doc_1"])
        assertEquals(false, items["doc_2"])
        assertEquals(true, items["doc_3"])
    }
}
