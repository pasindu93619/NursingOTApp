package com.pasindu.nursingotapp.transfer.data

import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.TransferActiveCacheDao
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity
import com.pasindu.nursingotapp.transfer.data.model.CacheSyncStatus
import com.pasindu.nursingotapp.transfer.data.model.RankedPreferences
import com.pasindu.nursingotapp.transfer.data.model.Decision
import com.pasindu.nursingotapp.transfer.data.model.DecisionRequest
import com.pasindu.nursingotapp.transfer.data.model.DecisionResponse
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
import com.pasindu.nursingotapp.transfer.data.model.WorkerDirectMatch
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TransferRequestRepository].
 *
 * Uses a simple in-memory fake DAO — no Mockito, no Room instrumentation required.
 * Tests run on the JVM (testDebugUnitTest).
 */
class TransferRequestRepositoryTest {

    // ---------------------------------------------------------------------------
    // Fake DAO — in-memory implementation for unit testing
    // ---------------------------------------------------------------------------

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

        fun copyForGrade(newGrade: String): FakeProfileDao =
            FakeProfileDao(currentProfile?.copy(grade = newGrade))
    }

    // ---------------------------------------------------------------------------
    // Subject under test
    // ---------------------------------------------------------------------------

    private lateinit var fakeDao: FakeTransferActiveCacheDao
    private lateinit var fakeProfileDao: FakeProfileDao
    private lateinit var repository: TransferRequestRepository

    private class FakeTokenProvider : TransferTokenProvider {
        override suspend fun getFirebaseIdToken(forceRefresh: Boolean): String = "test-token"
        override suspend fun getCurrentUserId(): String = "nurse-a"
    }

    private class FakeWorkerApiClient : TransferWorkerApiClient {
        var findAndLockCalls = 0

        override suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult {
            findAndLockCalls++
            return WorkerSyncResult.NoMatch("worker should not be called when remote match exists")
        }

        override suspend fun respondToMatch(
            firebaseIdToken: String,
            payload: DecisionRequest
        ): DecisionResponse {
            error("Not used by this test")
        }
    }

    private class FakeRemoteDataSource(
        private val remoteRequest: RemoteTransferRequestState?,
        private val remoteMatch: RemoteMatchState?
    ) : TransferRemoteDataSource {
        var publishCalls = 0
        var lastPublishedRequest: com.pasindu.nursingotapp.transfer.data.model.TransferRequest? = null

        override suspend fun fetchTransferRequest(userId: String): Result<RemoteTransferRequestState?> =
            Result.success(remoteRequest)

        override suspend fun fetchMatchDoc(matchId: String): Result<RemoteMatchState?> =
            Result.success(remoteMatch)

        override suspend fun publishTransferRequest(
            userId: String,
            request: com.pasindu.nursingotapp.transfer.data.model.TransferRequest
        ): Result<Unit> {
            publishCalls++
            lastPublishedRequest = request
            return Result.success(Unit)
        }

        override suspend fun withdrawTransferRequest(userId: String): Result<Unit> =
            Result.success(Unit)
    }


    @Before
    fun setUp() {
        fakeDao = FakeTransferActiveCacheDao()
        fakeProfileDao = FakeProfileDao(
            ProfileEntity(
                id = 1,
                fullName = "Nurse Perera",
                serviceNo = "SN-9876",
                unit = "Ward 4",
                paySheetNo = "PS-100",
                grade = "Grade I",
                basicSalary = 105000.0,
                otRate = 450.0,
                updatedAt = 1000L
            )
        )
        repository = TransferRequestRepository(fakeDao, fakeProfileDao)
    }

    // ---------------------------------------------------------------------------
    // saveRequest — happy path
    // ---------------------------------------------------------------------------

    @Test
    fun `saveRequest stores entity with PENDING sync status`() = runTest {
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )

        val stored = fakeDao.lastUpserted
        assertNotNull(stored)
        assertEquals(CacheSyncStatus.PENDING.name, stored!!.syncStatus)
    }

    @Test
    fun `saveRequest stores PENDING request status`() = runTest {
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )

        assertEquals(TransferRequestStatus.PENDING.name, fakeDao.lastUpserted!!.requestStatus)
    }

    @Test
    fun `saveRequest stores current hospital ID`() = runTest {
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )

        assertEquals("MOH2026-0001", fakeDao.lastUpserted!!.currentHospitalId)
    }

    @Test
    fun `saveRequest stores non-zero updatedAt timestamp`() = runTest {
        val before = System.currentTimeMillis()
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )
        val after = System.currentTimeMillis()

        val updatedAt = fakeDao.lastUpserted!!.updatedAt
        assertTrue(
            "updatedAt $updatedAt must be between $before and $after",
            updatedAt in before..after
        )
    }

    // ---------------------------------------------------------------------------
    // Ranked preference order preservation
    // ---------------------------------------------------------------------------

    @Test
    fun `saveRequest preserves ranked preference order — 3 preferences`() = runTest {
        val ids = listOf("MOH2026-0001", "MOH2026-0002", "MOH2026-0003")
        repository.saveRequest(
            currentHospitalId = "MOH2026-0999",
            rankedPreferences = RankedPreferences(ids)
        )

        val json = fakeDao.lastUpserted!!.preferenceHospitalIdsJson
        val decoded = repository.deserializePreferences(json)
        assertNotNull(decoded)
        assertEquals(ids, decoded!!.hospitalIds)
    }

    @Test
    fun `saveRequest preserves ranked preference order — 1 preference`() = runTest {
        val ids = listOf("MOH2026-0555")
        repository.saveRequest(
            currentHospitalId = "MOH2026-0999",
            rankedPreferences = RankedPreferences(ids)
        )

        val json = fakeDao.lastUpserted!!.preferenceHospitalIdsJson
        val decoded = repository.deserializePreferences(json)
        assertNotNull(decoded)
        assertEquals(ids, decoded!!.hospitalIds)
    }

    @Test
    fun `serializePreferences round-trips preserving exact order`() {
        val original = RankedPreferences(listOf("MOH2026-A", "MOH2026-B", "MOH2026-C"))
        val json = repository.serializePreferences(original)
        val decoded = repository.deserializePreferences(json)

        assertNotNull(decoded)
        assertEquals(original.hospitalIds, decoded!!.hospitalIds)
        // Verify positional equality
        assertEquals("MOH2026-A", decoded.hospitalIds[0])
        assertEquals("MOH2026-B", decoded.hospitalIds[1])
        assertEquals("MOH2026-C", decoded.hospitalIds[2])
    }

    // ---------------------------------------------------------------------------
    // updateRequest
    // ---------------------------------------------------------------------------

    @Test
    fun `updateRequest preserves existing requestId`() = runTest {
        // Pre-seed a row with a known requestId
        fakeDao.upsert(
            TransferActiveCacheEntity(
                id = 1,
                requestId = "req-existing-123",
                requestStatus = TransferRequestStatus.PENDING.name,
                currentHospitalId = "MOH2026-0001",
                preferenceHospitalIdsJson = """["MOH2026-0100"]""",
                matchCycleId = null,
                matchType = null,
                matchStatus = null,
                matchPayloadJson = null,
                syncStatus = CacheSyncStatus.SYNCED.name,
                updatedAt = 1000L
            )
        )

        repository.updateRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0200", "MOH2026-0300"))
        )

        val stored = fakeDao.lastUpserted
        assertEquals("req-existing-123", stored!!.requestId)
    }

    @Test
    fun `updateRequest updates preferences and resets sync status to PENDING`() = runTest {
        fakeDao.upsert(
            TransferActiveCacheEntity(
                id = 1,
                requestId = null,
                requestStatus = TransferRequestStatus.PENDING.name,
                currentHospitalId = "MOH2026-0001",
                preferenceHospitalIdsJson = """["MOH2026-0100"]""",
                matchCycleId = null,
                matchType = null,
                matchStatus = null,
                matchPayloadJson = null,
                syncStatus = CacheSyncStatus.SYNCED.name,
                updatedAt = 1000L
            )
        )

        repository.updateRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0200", "MOH2026-0300"))
        )

        val decoded = repository.deserializePreferences(fakeDao.lastUpserted!!.preferenceHospitalIdsJson)
        assertEquals(listOf("MOH2026-0200", "MOH2026-0300"), decoded!!.hospitalIds)
        assertEquals(CacheSyncStatus.PENDING.name, fakeDao.lastUpserted!!.syncStatus)
    }

    // ---------------------------------------------------------------------------
    // observeActiveRequest / getActiveRequest
    // ---------------------------------------------------------------------------

    @Test
    fun `observeActiveRequest emits null when cache is empty`() = runTest {
        val result = repository.observeActiveRequest().first()
        assertNull(result)
    }

    @Test
    fun `observeActiveRequest emits domain object after saveRequest`() = runTest {
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100", "MOH2026-0200"))
        )

        val result = repository.observeActiveRequest().first()
        assertNotNull(result)
        assertEquals("MOH2026-0001", result!!.currentHospitalId)
        assertEquals(TransferRequestStatus.PENDING, result.requestStatus)
        assertEquals(listOf("MOH2026-0100", "MOH2026-0200"), result.rankedPreferences.hospitalIds)
        assertEquals(CacheSyncStatus.PENDING, result.syncStatus)
    }

    @Test
    fun `getActiveRequest returns null when cache is empty`() = runTest {
        val result = repository.getActiveRequest()
        assertNull(result)
    }

    @Test
    fun `getActiveRequest returns domain object after saveRequest`() = runTest {
        repository.saveRequest(
            currentHospitalId = "MOH2026-0010",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0500"))
        )

        val result = repository.getActiveRequest()
        assertNotNull(result)
        assertEquals("MOH2026-0010", result!!.currentHospitalId)
    }

    @Test
    fun `syncActiveRequest repairs stale cached grade from current profile before publishing`() = runTest {
        fakeDao.upsert(
            TransferActiveCacheEntity(
                id = 1,
                requestId = null,
                requestStatus = TransferRequestStatus.PENDING.name,
                currentHospitalId = "MOH2026-0010",
                preferenceHospitalIdsJson = """["MOH2026-0332"]""",
                grade = "",
                matchCycleId = null,
                matchType = null,
                matchStatus = null,
                matchPayloadJson = null,
                syncStatus = CacheSyncStatus.SYNCED.name,
                updatedAt = 1000L
            )
        )

        val remote = FakeRemoteDataSource(
            remoteRequest = null,
            remoteMatch = null
        )
        val worker = FakeWorkerApiClient()

        repository = TransferRequestRepository(
            dao = fakeDao,
            profileDao = fakeProfileDao.copyForGrade("Grade III"),
            tokenProvider = FakeTokenProvider(),
            remoteDataSource = remote,
            workerApiClient = worker
        )

        val result = repository.syncActiveRequest()

        assertTrue(result is WorkerSyncResult.NoMatch)
        assertEquals("Grade III", fakeDao.getOnce()!!.grade)
        assertEquals("Grade III", remote.lastPublishedRequest!!.grade)
    }

    @Test
    fun `syncActiveRequest skips redundant publish when remote is SEARCHING and local cache is SYNCED`() = runTest {
        fakeDao.upsert(
            TransferActiveCacheEntity(
                id = 1,
                requestId = "nurse-a",
                requestStatus = TransferRequestStatus.PENDING.name,
                currentHospitalId = "MOH2026-0010",
                preferenceHospitalIdsJson = """["MOH2026-0332"]""",
                grade = "Grade III",
                matchCycleId = null,
                matchType = null,
                matchStatus = null,
                matchPayloadJson = null,
                syncStatus = CacheSyncStatus.SYNCED.name,
                updatedAt = 1000L
            )
        )

        val remote = FakeRemoteDataSource(
            remoteRequest = RemoteTransferRequestState(
                status = "SEARCHING",
                locked = false,
                currentMatchId = null,
                updatedAt = 2000L
            ),
            remoteMatch = null
        )
        val worker = FakeWorkerApiClient()

        repository = TransferRequestRepository(
            dao = fakeDao,
            profileDao = fakeProfileDao.copyForGrade("Grade III"),
            tokenProvider = FakeTokenProvider(),
            remoteDataSource = remote,
            workerApiClient = worker
        )

        val result = repository.syncActiveRequest()

        assertTrue(result is WorkerSyncResult.NoMatch)
        assertEquals(0, remote.publishCalls)
        assertEquals(1, worker.findAndLockCalls)
        assertEquals(CacheSyncStatus.SYNCED.name, fakeDao.getOnce()!!.syncStatus)
    }

    @Test
    fun `syncActiveRequest does not republish SEARCHING request after transient sync error`() = runTest {
        fakeDao.upsert(
            TransferActiveCacheEntity(
                id = 1,
                requestId = "nurse-a",
                requestStatus = TransferRequestStatus.PENDING.name,
                currentHospitalId = "MOH2026-0010",
                preferenceHospitalIdsJson = """["MOH2026-0332"]""",
                grade = "Grade III",
                matchCycleId = null,
                matchType = null,
                matchStatus = null,
                matchPayloadJson = null,
                syncStatus = CacheSyncStatus.ERROR.name,
                updatedAt = 1000L
            )
        )

        val remote = FakeRemoteDataSource(
            remoteRequest = RemoteTransferRequestState(
                status = "SEARCHING",
                locked = false,
                currentMatchId = null,
                updatedAt = 2000L
            ),
            remoteMatch = null
        )
        val worker = FakeWorkerApiClient()

        repository = TransferRequestRepository(
            dao = fakeDao,
            profileDao = fakeProfileDao.copyForGrade("Grade III"),
            tokenProvider = FakeTokenProvider(),
            remoteDataSource = remote,
            workerApiClient = worker
        )

        val result = repository.syncActiveRequest()

        assertTrue(result is WorkerSyncResult.NoMatch)
        assertEquals(0, remote.publishCalls)
        assertEquals(1, worker.findAndLockCalls)
        assertEquals(CacheSyncStatus.SYNCED.name, fakeDao.getOnce()!!.syncStatus)
    }

    @Test
    fun `syncActiveRequest ingests an already matched remote request without republishing`() = runTest {
        repository.saveRequest(
            currentHospitalId = "MOH2026-0010",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0332"))
        )

        val remoteMatch = RemoteMatchState(
            matchId = "match-123",
            match = WorkerDirectMatch(
                nurseAUid = "nurse-a",
                nurseBUid = "nurse-b",
                nurseACurrentHospitalId = "MOH2026-0010",
                nurseBCurrentHospitalId = "MOH2026-0332",
                nurseADestinationHospitalId = "MOH2026-0332",
                nurseBDestinationHospitalId = "MOH2026-0010",
                nurseAGrade = "Grade I",
                nurseBGrade = "Grade I",
                isSameGrade = true,
                nurseAPreferenceRank = 1,
                nurseBPreferenceRank = 1,
                combinedPreferenceRank = 2,
                priorityReason = "SAME_GRADE_PRIORITY"
            ),
            status = "PENDING_CONFIRMATION",
            createdAt = "2026-10-07T10:00:00Z",
            expiresAt = "2026-10-09T10:00:00Z"
        )

        val remote = FakeRemoteDataSource(
            remoteRequest = RemoteTransferRequestState(
                status = "MATCHED",
                locked = true,
                currentMatchId = "match-123",
                updatedAt = System.currentTimeMillis()
            ),
            remoteMatch = remoteMatch
        )
        val worker = FakeWorkerApiClient()

        repository = TransferRequestRepository(
            dao = fakeDao,
            profileDao = fakeProfileDao,
            tokenProvider = FakeTokenProvider(),
            remoteDataSource = remote,
            workerApiClient = worker
        )

        val result = repository.syncActiveRequest()

        assertTrue(result is WorkerSyncResult.MatchFound)
        assertEquals(0, remote.publishCalls)
        assertEquals(0, worker.findAndLockCalls)

        val stored = fakeDao.getOnce()
        assertNotNull(stored)
        assertEquals(TransferRequestStatus.MATCHED.name, stored!!.requestStatus)
        assertEquals("match-123", stored.matchCycleId)
        assertEquals("PENDING_CONFIRMATION", stored.matchStatus)
        assertEquals(CacheSyncStatus.SYNCED.name, stored.syncStatus)
    }

    // ---------------------------------------------------------------------------
    // clearRequest / withdraw
    // ---------------------------------------------------------------------------

    @Test
    fun `clearRequest delegates to dao clear`() = runTest {
        repository.clearRequest()
        assertEquals(1, fakeDao.clearCallCount)
    }

    @Test
    fun `observeActiveRequest emits null after clearRequest`() = runTest {
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )
        repository.clearRequest()

        val result = repository.observeActiveRequest().first()
        assertNull(result)
    }

    // ---------------------------------------------------------------------------
    // Failure propagation
    // ---------------------------------------------------------------------------

    @Test(expected = RuntimeException::class)
    fun `saveRequest propagates Room write failure`() = runTest {
        fakeDao.upsertShouldThrow = RuntimeException("Simulated Room write failure")
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveRequest throws for blank currentHospitalId`() = runTest {
        repository.saveRequest(
            currentHospitalId = "   ",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )
    }

    // ---------------------------------------------------------------------------
    // RankedPreferences validation
    // ---------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `RankedPreferences rejects empty list`() {
        RankedPreferences(emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `RankedPreferences rejects more than 3 IDs`() {
        RankedPreferences(listOf("A", "B", "C", "D"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `RankedPreferences rejects blank hospital ID`() {
        RankedPreferences(listOf("MOH2026-0001", "  "))
    }

    @Test
    fun `RankedPreferences rankOf returns correct 1-based index`() {
        val prefs = RankedPreferences(listOf("MOH2026-A", "MOH2026-B", "MOH2026-C"))
        assertEquals(1, prefs.rankOf("MOH2026-A"))
        assertEquals(2, prefs.rankOf("MOH2026-B"))
        assertEquals(3, prefs.rankOf("MOH2026-C"))
        assertNull(prefs.rankOf("MOH2026-X"))
    }

    // ---------------------------------------------------------------------------
    // deserializePreferences edge cases
    // ---------------------------------------------------------------------------

    @Test
    fun `deserializePreferences returns null for null input`() {
        assertNull(repository.deserializePreferences(null))
    }

    @Test
    fun `deserializePreferences returns null for blank input`() {
        assertNull(repository.deserializePreferences("   "))
    }

    @Test
    fun `deserializePreferences returns null for empty JSON array`() {
        assertNull(repository.deserializePreferences("[]"))
    }

    @Test
    fun `deserializePreferences returns null for malformed JSON`() {
        assertNull(repository.deserializePreferences("{not_an_array}"))
    }

    @Test
    fun `deserializePreferences decodes single-element array`() {
        val result = repository.deserializePreferences("""["MOH2026-0001"]""")
        assertNotNull(result)
        assertEquals(listOf("MOH2026-0001"), result!!.hospitalIds)
    }

    // ---------------------------------------------------------------------------
    // Grade Bridge tests (Phase 1.5.3C-1)
    // ---------------------------------------------------------------------------

    @Test
    fun `saveRequest reads grade from ProfileEntity and stores it in entity`() = runTest {
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )

        val stored = fakeDao.lastUpserted
        assertNotNull(stored)
        assertEquals("Grade I", stored!!.grade)
    }

    @Test
    fun `observeActiveRequest emits TransferRequest containing profile grade`() = runTest {
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )

        val request = repository.observeActiveRequest().first()
        assertNotNull(request)
        assertEquals("Grade I", request!!.grade)
    }

    @Test
    fun `getActiveRequest returns domain object containing profile grade`() = runTest {
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )

        val request = repository.getActiveRequest()
        assertNotNull(request)
        assertEquals("Grade I", request!!.grade)
    }

    @Test
    fun `updateRequest preserves or updates grade from profile`() = runTest {
        // Initial save with Grade I
        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )
        assertEquals("Grade I", fakeDao.lastUpserted!!.grade)

        // Nurse gets promoted to Special Grade
        fakeProfileDao.currentProfile = fakeProfileDao.currentProfile!!.copy(grade = "Special Grade")

        // Update request preferences
        repository.updateRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0200"))
        )

        assertEquals("Special Grade", fakeDao.lastUpserted!!.grade)
        val request = repository.getActiveRequest()
        assertNotNull(request)
        assertEquals("Special Grade", request!!.grade)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveRequest throws when profile is missing`() = runTest {
        fakeProfileDao.currentProfile = null

        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveRequest throws when profile grade is blank`() = runTest {
        fakeProfileDao.currentProfile = fakeProfileDao.currentProfile!!.copy(grade = "   ")

        repository.saveRequest(
            currentHospitalId = "MOH2026-0001",
            rankedPreferences = RankedPreferences(listOf("MOH2026-0100"))
        )
    }

    @Test
    fun `observeActiveRequest emits null if cached entity has blank grade`() = runTest {
        fakeDao.upsert(
            TransferActiveCacheEntity(
                id = 1,
                requestId = "req-legacy",
                requestStatus = TransferRequestStatus.PENDING.name,
                currentHospitalId = "MOH2026-0001",
                preferenceHospitalIdsJson = """["MOH2026-0100"]""",
                grade = null, // Old cached row without grade
                syncStatus = CacheSyncStatus.SYNCED.name,
                updatedAt = 1000L
            )
        )

        val request = repository.observeActiveRequest().first()
        assertNull("Request with missing grade must not be emitted as active", request)
    }
}
