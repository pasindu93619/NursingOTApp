package com.pasindu.nursingotapp.transfer.data

import com.pasindu.nursingotapp.data.local.dao.TransferActiveCacheDao
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity
import com.pasindu.nursingotapp.transfer.data.model.CacheSyncStatus
import com.pasindu.nursingotapp.transfer.data.model.RankedPreferences
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
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

    // ---------------------------------------------------------------------------
    // Subject under test
    // ---------------------------------------------------------------------------

    private lateinit var fakeDao: FakeTransferActiveCacheDao
    private lateinit var repository: TransferRequestRepository

    @Before
    fun setUp() {
        fakeDao = FakeTransferActiveCacheDao()
        repository = TransferRequestRepository(fakeDao)
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
}
