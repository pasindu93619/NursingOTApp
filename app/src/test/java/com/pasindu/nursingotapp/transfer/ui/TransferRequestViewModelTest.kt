package com.pasindu.nursingotapp.transfer.ui

import android.content.Context
import android.content.ContextWrapper
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.TransferActiveCacheDao
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity
import com.pasindu.nursingotapp.transfer.data.HospitalReferenceRepository
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.CacheSyncStatus
import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import com.pasindu.nursingotapp.transfer.data.model.RankedPreferences
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Lifecycle and integration unit tests for [TransferRequestViewModel] and [TransferRequestRepository].
 *
 * Verifies all Phase 1.5.3B requirements:
 * 1. Newly submitted request appears as active/searching.
 * 2. Current hospital is preserved.
 * 3. Ranked preferences remain in exact order.
 * 4. Edit updates the persisted request.
 * 5. Withdraw/cancel removes or transitions the active request correctly according to the existing model.
 * 6. Reloading/observing the ViewModel restores the persisted request.
 * 7. Error states do not falsely report successful persistence.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransferRequestViewModelTest {

    private class FakeTransferActiveCacheDao : TransferActiveCacheDao {
        private val _flow = MutableStateFlow<TransferActiveCacheEntity?>(null)
        var lastUpserted: TransferActiveCacheEntity? = null
        var clearCallCount = 0
        var upsertShouldThrow: Throwable? = null

        override suspend fun upsert(cache: TransferActiveCacheEntity) {
            upsertShouldThrow?.let { throw it }
            lastUpserted = cache
            _flow.value = cache
        }

        override fun observe(): Flow<TransferActiveCacheEntity?> = _flow

        override suspend fun getOnce(): TransferActiveCacheEntity? = _flow.value

        override suspend fun clear() {
            clearCallCount++
            _flow.value = null
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

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakeTransferActiveCacheDao
    private lateinit var fakeProfileDao: FakeProfileDao
    private lateinit var repository: TransferRequestRepository
    private lateinit var dummyHospitalRepo: HospitalReferenceRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeTransferActiveCacheDao()
        fakeProfileDao = FakeProfileDao(
            ProfileEntity(
                id = 1,
                fullName = "Nurse Silva",
                serviceNo = "SN-54321",
                unit = "Surgical ICU",
                paySheetNo = "PS-500",
                grade = "Grade II",
                basicSalary = 98000.0,
                otRate = 420.0,
                updatedAt = 1000L
            )
        )
        repository = TransferRequestRepository(fakeDao, fakeProfileDao)

        // Provide a dummy ContextWrapper that satisfies the constructor without triggering asset loading
        val dummyContext: Context = ContextWrapper(null)
        dummyHospitalRepo = HospitalReferenceRepository(dummyContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TransferRequestViewModel =
        TransferRequestViewModel(
            hospitalReferenceRepository = dummyHospitalRepo,
            transferRequestRepository = repository
        )

    @Test
    fun `1 - newly submitted request appears as active searching`() = runTest {
        val viewModel = createViewModel()

        var successCallbackCalled = false
        viewModel.submitRequest(
            currentHospitalId = "MOH2026-0001",
            preferenceHospitalIds = listOf("MOH2026-0100"),
            onSuccess = { successCallbackCalled = true }
        )

        assertTrue("onSuccess callback must be called on successful save", successCallbackCalled)
        val active = viewModel.activeRequest.value
        assertNotNull("Active request must be populated", active)
        assertEquals(TransferRequestStatus.PENDING, active!!.requestStatus)
        assertEquals(CacheSyncStatus.PENDING, active.syncStatus)
    }

    @Test
    fun `2 - current hospital is preserved accurately in active request`() = runTest {
        val viewModel = createViewModel()

        viewModel.submitRequest(
            currentHospitalId = "MOH2026-0042",
            preferenceHospitalIds = listOf("MOH2026-0100", "MOH2026-0200")
        )

        val active = viewModel.activeRequest.value
        assertNotNull(active)
        assertEquals("MOH2026-0042", active!!.currentHospitalId)
        assertEquals("MOH2026-0042", fakeDao.lastUpserted!!.currentHospitalId)
    }

    @Test
    fun `3 - ranked preferences remain in exact order`() = runTest {
        val viewModel = createViewModel()
        val expectedRankedIds = listOf("MOH2026-0003", "MOH2026-0001", "MOH2026-0002")

        viewModel.submitRequest(
            currentHospitalId = "MOH2026-0999",
            preferenceHospitalIds = expectedRankedIds
        )

        val active = viewModel.activeRequest.value
        assertNotNull(active)
        val actualIds = active!!.rankedPreferences.hospitalIds
        assertEquals(3, actualIds.size)
        assertEquals("MOH2026-0003", actualIds[0])
        assertEquals("MOH2026-0001", actualIds[1])
        assertEquals("MOH2026-0002", actualIds[2])
        assertEquals(expectedRankedIds, actualIds)
    }

    @Test
    fun `4 - edit updates the persisted request and preserves ordering`() = runTest {
        val viewModel = createViewModel()

        // 1. Initial submission
        viewModel.submitRequest(
            currentHospitalId = "MOH2026-0001",
            preferenceHospitalIds = listOf("MOH2026-0100")
        )
        assertEquals(listOf("MOH2026-0100"), viewModel.activeRequest.value!!.rankedPreferences.hospitalIds)

        // 2. Edit submission with re-ordered and updated destinations
        val updatedIds = listOf("MOH2026-0300", "MOH2026-0200", "MOH2026-0100")
        var editSuccess = false
        viewModel.submitRequest(
            currentHospitalId = "MOH2026-0002",
            preferenceHospitalIds = updatedIds,
            onSuccess = { editSuccess = true }
        )

        assertTrue(editSuccess)
        val updatedActive = viewModel.activeRequest.value
        assertNotNull(updatedActive)
        assertEquals("MOH2026-0002", updatedActive!!.currentHospitalId)
        assertEquals(updatedIds, updatedActive.rankedPreferences.hospitalIds)
    }

    @Test
    fun `5 - withdraw or cancel removes the active request from state and dao`() = runTest {
        val viewModel = createViewModel()

        // Seed request
        viewModel.submitRequest(
            currentHospitalId = "MOH2026-0001",
            preferenceHospitalIds = listOf("MOH2026-0100")
        )
        assertNotNull(viewModel.activeRequest.value)

        // Withdraw
        var withdrawSuccess = false
        viewModel.withdrawRequest(onSuccess = { withdrawSuccess = true })

        assertTrue(withdrawSuccess)
        assertEquals(1, fakeDao.clearCallCount)
        assertNull("activeRequest must emit null after withdrawal", viewModel.activeRequest.value)
    }

    @Test
    fun `6 - reloading or re-observing ViewModel restores the persisted request`() = runTest {
        // 1. First ViewModel creates and persists a request
        val firstViewModel = createViewModel()
        firstViewModel.submitRequest(
            currentHospitalId = "MOH2026-0777",
            preferenceHospitalIds = listOf("MOH2026-0888", "MOH2026-0999")
        )

        // 2. Simulate opening the screen anew: create second ViewModel with the same repository
        val secondViewModel = createViewModel()

        val restoredActive = secondViewModel.activeRequest.value
        assertNotNull("Re-instantiated ViewModel must immediately observe persisted request", restoredActive)
        assertEquals("MOH2026-0777", restoredActive!!.currentHospitalId)
        assertEquals(listOf("MOH2026-0888", "MOH2026-0999"), restoredActive.rankedPreferences.hospitalIds)
    }

    @Test
    fun `7 - error states do not falsely report successful persistence`() = runTest {
        val viewModel = createViewModel()
        fakeDao.upsertShouldThrow = RuntimeException("Simulated SQLite disk error")

        var successCallbackInvoked = false
        viewModel.submitRequest(
            currentHospitalId = "MOH2026-0001",
            preferenceHospitalIds = listOf("MOH2026-0100"),
            onSuccess = { successCallbackInvoked = true }
        )

        assertFalse("onSuccess must NEVER be invoked when persistence fails", successCallbackInvoked)
        assertNotNull("submitError must be populated with error message", viewModel.submitError.value)
        assertEquals("Simulated SQLite disk error", viewModel.submitError.value)
        assertNull("activeRequest must remain null after failed persistence", viewModel.activeRequest.value)
    }

    @Test
    fun `7b - validation failure on empty preferences surfaces error without calling onSuccess`() = runTest {
        val viewModel = createViewModel()

        var successCallbackInvoked = false
        viewModel.submitRequest(
            currentHospitalId = "MOH2026-0001",
            preferenceHospitalIds = emptyList(),
            onSuccess = { successCallbackInvoked = true }
        )

        assertFalse(successCallbackInvoked)
        assertNotNull(viewModel.submitError.value)
        assertTrue(viewModel.submitError.value!!.contains("RankedPreferences requires at least 1 hospital ID"))
    }

    @Test
    fun `7c - missing profile grade surfaces error without calling onSuccess`() = runTest {
        val viewModel = createViewModel()
        fakeProfileDao.currentProfile = null

        var successCallbackInvoked = false
        viewModel.submitRequest(
            currentHospitalId = "MOH2026-0001",
            preferenceHospitalIds = listOf("MOH2026-0100"),
            onSuccess = { successCallbackInvoked = true }
        )

        assertFalse("onSuccess must NOT be called when profile grade is missing", successCallbackInvoked)
        assertNotNull(viewModel.submitError.value)
        assertTrue(viewModel.submitError.value!!.contains("A valid nursing grade is required in your profile"))
    }
}
