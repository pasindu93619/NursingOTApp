package com.pasindu.nursingotapp.transfer.data.model

/**
 * Lifecycle status of the nurse's own active Mutual Transfer request.
 *
 * These values are persisted as strings in [TransferActiveCacheEntity.requestStatus].
 * Do not rename without a migration.
 */
enum class TransferRequestStatus {
    /** Built locally; not yet submitted to a match cycle. */
    PENDING,

    /** Matched to a partner; awaiting confirmation. */
    MATCHED,

    /** Transfer finalised and completed. */
    COMPLETED,

    /** Nurse withdrew the request. */
    WITHDRAWN
}

/**
 * Synchronisation state of the local Room cache row relative to Firestore.
 *
 * These values are persisted as strings in [TransferActiveCacheEntity.syncStatus].
 * Do not rename without a migration.
 */
enum class CacheSyncStatus {
    /** Local write not yet confirmed by Firestore. */
    PENDING,

    /** Cache matches the last confirmed Firestore read. */
    SYNCED,

    /** Last sync attempt failed; retry required. */
    ERROR
}

/**
 * Ordered ranked preferences for a Mutual Transfer request.
 *
 * Invariants:
 * - List size: 1–3 inclusive.
 * - Each ID must be a non-blank canonical [HospitalReference.hospitalId].
 * - Order is significant: index 0 = 1st preference, index 1 = 2nd, index 2 = 3rd.
 *
 * Serialisation preserves order via a JSON array; do NOT use a Set.
 */
data class RankedPreferences(val hospitalIds: List<String>) {
    init {
        require(hospitalIds.isNotEmpty()) {
            "RankedPreferences requires at least 1 hospital ID"
        }
        require(hospitalIds.size <= 3) {
            "RankedPreferences allows at most 3 hospital IDs, got ${hospitalIds.size}"
        }
        require(hospitalIds.all { it.isNotBlank() }) {
            "Every hospital ID in RankedPreferences must be non-blank"
        }
    }

    /** 1-based index of [hospitalId], or null if not in this preference list. */
    fun rankOf(hospitalId: String): Int? {
        val idx = hospitalIds.indexOf(hospitalId)
        return if (idx >= 0) idx + 1 else null
    }
}

/**
 * Immutable in-memory snapshot of the nurse's active Mutual Transfer request.
 *
 * This is the domain object returned by [TransferRequestRepository].
 * It is never persisted directly; it is mapped to/from [TransferActiveCacheEntity].
 *
 * Phase 1.5.3A scope: local persistence only.
 * Firestore sync, match-cycle fields, and deadline fields are wired in later steps.
 */
data class TransferRequest(
    /** Firestore document ID when synced; null for a locally created but unsynced request. */
    val requestId: String?,

    /** Status of this request in the transfer lifecycle. */
    val requestStatus: TransferRequestStatus,

    /** Canonical [HospitalReference.hospitalId] of the nurse's current posting. */
    val currentHospitalId: String,

    /** Ranked list of preferred transfer destination hospitals. */
    val rankedPreferences: RankedPreferences,

    /** Room cache synchronisation state. */
    val syncStatus: CacheSyncStatus,

    /**
     * Wall-clock epoch milliseconds of the last local write.
     * Updated on every [TransferRequestRepository.saveRequest] call.
     */
    val updatedAt: Long
)
