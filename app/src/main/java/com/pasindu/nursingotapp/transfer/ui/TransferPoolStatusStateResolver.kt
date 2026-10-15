package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.Slate

/**
 * Resolved header status model for the Transfer Pool Status screen.
 */
data class HeaderStatusInfo(
    val icon: ImageVector,
    val contentDescription: String,
    val tintColor: Color,
    val backgroundColor: Color
)

/**
 * Resolved journey stepper model for the Transfer Pool Status screen.
 */
data class TransferJourneyState(
    val stageLabel: String,
    val badgeBgColor: Color,
    val badgeTextColor: Color,
    val step2Completed: Boolean,
    val step2Active: Boolean,
    val step2Sublabel: String,
    val step3Completed: Boolean,
    val step3Active: Boolean,
    val step3Sublabel: String,
    val step4Completed: Boolean,
    val step4Active: Boolean,
    val step4Sublabel: String,
    val step5Completed: Boolean,
    val step5Active: Boolean,
    val step5Sublabel: String
)

/**
 * Pure state resolution logic for Mutual Transfer Pool Status.
 */
object TransferPoolStatusStateResolver {

    /**
     * Determines whether an active match is genuinely in progress.
     *
     * True only while:
     * - The request is in [TransferRequestStatus.MATCHED], AND
     * - The match status is not in a terminal state (CANCELLED or EXPIRED).
     *
     * If the match is CANCELLED or EXPIRED, or if the request is PENDING, COMPLETED, or WITHDRAWN,
     * this returns false.
     */
    fun isMatchActive(request: TransferRequest?): Boolean {
        if (request == null) return false
        if (request.requestStatus != TransferRequestStatus.MATCHED) return false
        val matchStatus = request.matchStatus?.trim()?.uppercase()
        return matchStatus != "CANCELLED" && matchStatus != "EXPIRED"
    }

    /**
     * Resolves the header status icon, color, and accessibility description
     * strictly derived from the actual request and match states.
     */
    fun resolveHeaderStatus(
        request: TransferRequest,
        mintSoft: Color,
        blueSoft: Color,
        roseSoft: Color,
        mutedSurface: Color
    ): HeaderStatusInfo {
        val serverStatus = request.matchStatus?.trim()?.uppercase()

        return when {
            request.requestStatus == TransferRequestStatus.COMPLETED -> {
                HeaderStatusInfo(
                    icon = Icons.Default.VerifiedUser,
                    contentDescription = "Transfer completed",
                    tintColor = Emerald,
                    backgroundColor = mintSoft
                )
            }
            request.requestStatus == TransferRequestStatus.WITHDRAWN -> {
                HeaderStatusInfo(
                    icon = Icons.Default.Close,
                    contentDescription = "Request withdrawn",
                    tintColor = Slate,
                    backgroundColor = mutedSurface
                )
            }
            request.requestStatus == TransferRequestStatus.MATCHED -> {
                when (serverStatus) {
                    "CHAT_OPEN" -> {
                        HeaderStatusInfo(
                            icon = Icons.Default.SwapHoriz,
                            contentDescription = "Team chat open",
                            tintColor = MedicalBlue,
                            backgroundColor = blueSoft
                        )
                    }
                    "CONFIRMED" -> {
                        HeaderStatusInfo(
                            icon = Icons.Default.CheckCircle,
                            contentDescription = "Transfer agreed and confirmed",
                            tintColor = Emerald,
                            backgroundColor = mintSoft
                        )
                    }
                    "CANCELLED" -> {
                        HeaderStatusInfo(
                            icon = Icons.Default.Close,
                            contentDescription = "Transfer match cancelled",
                            tintColor = CriticalRed,
                            backgroundColor = roseSoft
                        )
                    }
                    "EXPIRED" -> {
                        HeaderStatusInfo(
                            icon = Icons.Default.Info,
                            contentDescription = "Transfer match expired",
                            tintColor = Slate,
                            backgroundColor = mutedSurface
                        )
                    }
                    "ACCEPTED" -> {
                        HeaderStatusInfo(
                            icon = Icons.Default.HourglassEmpty,
                            contentDescription = "Match accepted, awaiting partner response",
                            tintColor = Emerald,
                            backgroundColor = mintSoft
                        )
                    }
                    else -> {
                        HeaderStatusInfo(
                            icon = Icons.Default.HourglassEmpty,
                            contentDescription = "Match awaiting your response",
                            tintColor = Emerald,
                            backgroundColor = mintSoft
                        )
                    }
                }
            }
            else -> {
                // requestStatus == PENDING or returned to searching
                HeaderStatusInfo(
                    icon = Icons.Default.Search,
                    contentDescription = "In matching pool, searching for partner",
                    tintColor = Emerald,
                    backgroundColor = mintSoft
                )
            }
        }
    }

    /**
     * Resolves the 5-stage milestone journey labels, step completions, and active states.
     * Sequence: Request saved -> Searching -> Review & accept -> Discuss with team -> Confirm transfer
     */
    fun resolveJourneyState(
        request: TransferRequest,
        blueSoft: Color,
        primaryClinical: Color,
        mintSoft: Color,
        roseSoft: Color,
        mutedSurface: Color
    ): TransferJourneyState {
        val isMatched = request.requestStatus == TransferRequestStatus.MATCHED
        val serverStatus = request.matchStatus?.trim()?.uppercase()

        return when {
            request.requestStatus == TransferRequestStatus.COMPLETED -> {
                TransferJourneyState(
                    stageLabel = "Transfer completed",
                    badgeBgColor = mintSoft,
                    badgeTextColor = Emerald,
                    step2Completed = true,
                    step2Active = false,
                    step2Sublabel = "Done",
                    step3Completed = true,
                    step3Active = false,
                    step3Sublabel = "Done",
                    step4Completed = true,
                    step4Active = false,
                    step4Sublabel = "Done",
                    step5Completed = true,
                    step5Active = false,
                    step5Sublabel = "Completed"
                )
            }
            request.requestStatus == TransferRequestStatus.WITHDRAWN -> {
                TransferJourneyState(
                    stageLabel = "Request withdrawn",
                    badgeBgColor = mutedSurface,
                    badgeTextColor = Slate,
                    step2Completed = false,
                    step2Active = false,
                    step2Sublabel = "Withdrawn",
                    step3Completed = false,
                    step3Active = false,
                    step3Sublabel = "Closed",
                    step4Completed = false,
                    step4Active = false,
                    step4Sublabel = "Closed",
                    step5Completed = false,
                    step5Active = false,
                    step5Sublabel = "Closed"
                )
            }
            !isMatched -> {
                // PENDING (including stale CANCELLED / EXPIRED matchStatus from prior matches)
                TransferJourneyState(
                    stageLabel = "Stage 2 of 5 • In transfer pool",
                    badgeBgColor = blueSoft,
                    badgeTextColor = primaryClinical,
                    step2Completed = false,
                    step2Active = true,
                    step2Sublabel = "In pool",
                    step3Completed = false,
                    step3Active = false,
                    step3Sublabel = "Pending",
                    step4Completed = false,
                    step4Active = false,
                    step4Sublabel = "Pending",
                    step5Completed = false,
                    step5Active = false,
                    step5Sublabel = "Pending"
                )
            }
            else -> {
            when (serverStatus) {
                "CHAT_OPEN" -> {
                    TransferJourneyState(
                        stageLabel = "Stage 4 of 5 • Team chat",
                        badgeBgColor = blueSoft,
                        badgeTextColor = MedicalBlue,
                        step2Completed = true,
                        step2Active = false,
                        step2Sublabel = "Done",
                        step3Completed = true,
                        step3Active = false,
                        step3Sublabel = "Done",
                        step4Completed = false,
                        step4Active = true,
                        step4Sublabel = "In progress",
                        step5Completed = false,
                        step5Active = false,
                        step5Sublabel = "Pending"
                    )
                }
                "CONFIRMED" -> {
                    TransferJourneyState(
                        stageLabel = "Stage 5 of 5 • Confirmed",
                        badgeBgColor = mintSoft,
                        badgeTextColor = Emerald,
                        step2Completed = true,
                        step2Active = false,
                        step2Sublabel = "Done",
                        step3Completed = true,
                        step3Active = false,
                        step3Sublabel = "Done",
                        step4Completed = true,
                        step4Active = false,
                        step4Sublabel = "Done",
                        step5Completed = true,
                        step5Active = true,
                        step5Sublabel = "All confirmed"
                    )
                }
                "CANCELLED" -> {
                    TransferJourneyState(
                        stageLabel = "Transfer cancelled",
                        badgeBgColor = roseSoft,
                        badgeTextColor = CriticalRed,
                        step2Completed = true,
                        step2Active = false,
                        step2Sublabel = "Done",
                        step3Completed = false,
                        step3Active = false,
                        step3Sublabel = "Cancelled",
                        step4Completed = false,
                        step4Active = false,
                        step4Sublabel = "Closed",
                        step5Completed = false,
                        step5Active = false,
                        step5Sublabel = "Closed"
                    )
                }
                "EXPIRED" -> {
                    TransferJourneyState(
                        stageLabel = "Transfer expired",
                        badgeBgColor = mutedSurface,
                        badgeTextColor = Slate,
                        step2Completed = true,
                        step2Active = false,
                        step2Sublabel = "Done",
                        step3Completed = false,
                        step3Active = false,
                        step3Sublabel = "Expired",
                        step4Completed = false,
                        step4Active = false,
                        step4Sublabel = "Closed",
                        step5Completed = false,
                        step5Active = false,
                        step5Sublabel = "Closed"
                    )
                }
                "ACCEPTED" -> {
                    TransferJourneyState(
                        stageLabel = "Stage 3 of 5 • Review & accept",
                        badgeBgColor = mintSoft,
                        badgeTextColor = Emerald,
                        step2Completed = true,
                        step2Active = false,
                        step2Sublabel = "Done",
                        step3Completed = false,
                        step3Active = true,
                        step3Sublabel = "Waiting on partner",
                        step4Completed = false,
                        step4Active = false,
                        step4Sublabel = "Pending",
                        step5Completed = false,
                        step5Active = false,
                        step5Sublabel = "Pending"
                    )
                }
                else -> {
                    // PENDING_CONFIRMATION / proposed match
                    TransferJourneyState(
                        stageLabel = "Stage 3 of 5 • Review & accept",
                        badgeBgColor = mintSoft,
                        badgeTextColor = Emerald,
                        step2Completed = true,
                        step2Active = false,
                        step2Sublabel = "Done",
                        step3Completed = false,
                        step3Active = true,
                        step3Sublabel = "Action required",
                        step4Completed = false,
                        step4Active = false,
                        step4Sublabel = "Pending",
                        step5Completed = false,
                        step5Active = false,
                        step5Sublabel = "Pending"
                    )
                }
            }
        }
    }
}
}
