package com.pasindu.nursingotapp.transfer.data

import com.pasindu.nursingotapp.data.local.dao.TransferActiveCacheDao
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity
import com.pasindu.nursingotapp.transfer.data.model.CacheSyncStatus
import com.pasindu.nursingotapp.transfer.data.model.RankedPreferences
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local Room-backed repository for the nurse's own active Mutual Transfer request.
 *
 * Phase 1.5.3A scope: ALL persistence is Room-only. Firestore synchronisation
 * is wired in a later step. The [TransferActiveCacheDao] singleton-row pattern
 * (primary key = 1) ensures there is at most one active request per device.
 *
 * Ranked preference order is guaranteed: the hospital IDs are stored as a
 * JSON ARRAY and deserialised back in the same order. A Set is never used.
 *
 * Room failure propagation: if [dao.upsert] throws (e.g. disk full, DB
 * corruption), the exception propagates out of [saveRequest] to the caller.
 * The caller (ViewModel) is responsible for catching and surfacing it to the UI.
 */
class TransferRequestRepository @Inject constructor(
    private val dao: TransferActiveCacheDao
) {

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
    ) {
        require(currentHospitalId.isNotBlank()) {
            "currentHospitalId must not be blank"
        }

        val preferenceJson = serializePreferences(rankedPreferences)
        val nowMs = System.currentTimeMillis()

        // Preserve the existing requestId if there is already a cached row.
        val existingRequestId = dao.getOnce()?.requestId

        val entity = TransferActiveCacheEntity(
            id = 1,
            requestId = existingRequestId,
            requestStatus = TransferRequestStatus.PENDING.name,
            currentHospitalId = currentHospitalId,
            preferenceHospitalIdsJson = preferenceJson,
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
     * Clears the active request row from Room.
     *
     * After this call, [observeActiveRequest] will emit `null`.
     *
     * @throws Exception if the Room delete fails.
     */
    suspend fun clearRequest() {
        dao.clear()
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

        if (statusRaw.isNullOrEmpty() || hospitalId.isNullOrEmpty()) return null

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
            syncStatus = cacheSyncStatus,
            updatedAt = this.updatedAt
        )
    }
}
