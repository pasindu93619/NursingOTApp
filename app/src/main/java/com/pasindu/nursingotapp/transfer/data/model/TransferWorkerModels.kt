package com.pasindu.nursingotapp.transfer.data.model

import kotlinx.serialization.Serializable

/**
 * Direct 2-way match structure returned by Cloudflare Worker.
 */
@Serializable
data class WorkerDirectMatch(
    val nurseAUid: String,
    val nurseBUid: String,
    val nurseACurrentHospitalId: String,
    val nurseBCurrentHospitalId: String,
    val nurseADestinationHospitalId: String,
    val nurseBDestinationHospitalId: String,
    val nurseAGrade: String,
    val nurseBGrade: String,
    val isSameGrade: Boolean,
    val nurseAPreferenceRank: Int,
    val nurseBPreferenceRank: Int,
    val combinedPreferenceRank: Int,
    val priorityReason: String
)

/**
 * Response payload from POST /api/matching/find-and-lock.
 */
@Serializable
data class WorkerMatchResponse(
    val matched: Boolean = false,
    val matchId: String? = null,
    val match: WorkerDirectMatch? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val message: String? = null,
    val error: String? = null
)

/**
 * High-level result of a synchronization attempt with the Cloudflare Worker.
 */
sealed interface WorkerSyncResult {
    data class MatchFound(
        val matchId: String,
        val match: WorkerDirectMatch,
        val createdAt: String,
        val expiresAt: String
    ) : WorkerSyncResult

    data class NoMatch(val message: String) : WorkerSyncResult
    data class Conflict(val message: String) : WorkerSyncResult
    data class AuthError(val message: String) : WorkerSyncResult
    data class NetworkError(val message: String, val cause: Throwable? = null) : WorkerSyncResult
}
    /**
     * Decision request payload for accept/reject.
     */
    @Serializable
    data class DecisionRequest(
        val matchId: String,
        val decision: Decision
    )

    /**
     * Decision response payload after server processes the decision.
     */
    @Serializable
    data class DecisionResponse(
        val matchId: String,
        val newStatus: String,
        val expiresAt: String,
        val decisionApplied: Boolean
    )

    /**
     * Enum of possible decisions.
     */
    @Serializable
    enum class Decision { ACCEPT, REJECT }

