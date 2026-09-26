package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline cache for the nurse's own active Mutual Transfer request/match.
 *
 * This table is intentionally scoped to the signed-in user's transfer state.
 * It is not a source of truth for matching, deadlines, or eligibility;
 * Firestore/Cloud Functions remain authoritative for those values.
 */
@Entity(tableName = "transfer_active_cache")
data class TransferActiveCacheEntity(
    @PrimaryKey val id: Int = 1,
    val requestId: String? = null,
    val requestStatus: String? = null,
    val currentHospitalId: String? = null,
    val preferenceHospitalIdsJson: String? = null,
    val matchCycleId: String? = null,
    val matchType: String? = null,
    val matchStatus: String? = null,
    val matchPayloadJson: String? = null,
    val syncStatus: String = "SYNCED",
    val updatedAt: Long
)
