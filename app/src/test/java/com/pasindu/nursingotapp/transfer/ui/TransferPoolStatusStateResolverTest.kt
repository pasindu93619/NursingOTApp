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
import com.pasindu.nursingotapp.transfer.data.model.CacheSyncStatus
import com.pasindu.nursingotapp.transfer.data.model.RankedPreferences
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.Slate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferPoolStatusStateResolverTest {

    private val mintSoft = Color(0xFFEAFBF5)
    private val blueSoft = Color(0xFFEAF6FF)
    private val roseSoft = Color(0xFFFFF1F2)
    private val mutedSurface = Color(0xFFF1F5F9)
    private val primaryClinical = Color(0xFF0D9488)

    private fun createRequest(
        requestStatus: TransferRequestStatus,
        matchStatus: String? = null
    ): TransferRequest {
        return TransferRequest(
            requestId = "req-1",
            requestStatus = requestStatus,
            currentHospitalId = "HOSP-001",
            rankedPreferences = RankedPreferences(listOf("HOSP-002")),
            grade = "Grade I",
            syncStatus = CacheSyncStatus.SYNCED,
            updatedAt = 1000L,
            matchStatus = matchStatus
        )
    }

    // -------------------------------------------------------------------------
    // Issue 1 & 2: isMatchActive
    // -------------------------------------------------------------------------

    @Test
    fun isMatchActive_returnsFalse_forNullRequest() {
        assertFalse(TransferPoolStatusStateResolver.isMatchActive(null))
    }

    @Test
    fun isMatchActive_returnsFalse_whenRequestStatusIsPending() {
        val req = createRequest(TransferRequestStatus.PENDING, null)
        assertFalse(TransferPoolStatusStateResolver.isMatchActive(req))
    }

    @Test
    fun isMatchActive_returnsFalse_whenRequestReturnedToPendingWithStaleCancelledMatchStatus() {
        // Critical: Peer cancelled match, Room synced request back to PENDING while matchStatus says CANCELLED
        val req = createRequest(TransferRequestStatus.PENDING, "CANCELLED")
        assertFalse(TransferPoolStatusStateResolver.isMatchActive(req))
    }

    @Test
    fun isMatchActive_returnsTrue_whenMatchedInPendingConfirmation() {
        val req = createRequest(TransferRequestStatus.MATCHED, "PENDING_CONFIRMATION")
        assertTrue(TransferPoolStatusStateResolver.isMatchActive(req))
    }

    @Test
    fun isMatchActive_returnsTrue_whenMatchedInChatOpen() {
        val req = createRequest(TransferRequestStatus.MATCHED, "CHAT_OPEN")
        assertTrue(TransferPoolStatusStateResolver.isMatchActive(req))
    }

    @Test
    fun isMatchActive_returnsTrue_whenMatchedAndConfirmed() {
        val req = createRequest(TransferRequestStatus.MATCHED, "CONFIRMED")
        assertTrue(TransferPoolStatusStateResolver.isMatchActive(req))
    }

    @Test
    fun isMatchActive_returnsFalse_whenMatchedWithCancelledStatus() {
        val req = createRequest(TransferRequestStatus.MATCHED, "CANCELLED")
        assertFalse(TransferPoolStatusStateResolver.isMatchActive(req))
    }

    @Test
    fun isMatchActive_returnsFalse_whenMatchedWithExpiredStatus() {
        val req = createRequest(TransferRequestStatus.MATCHED, "EXPIRED")
        assertFalse(TransferPoolStatusStateResolver.isMatchActive(req))
    }

    @Test
    fun isMatchActive_returnsFalse_whenCompletedOrWithdrawn() {
        assertFalse(TransferPoolStatusStateResolver.isMatchActive(createRequest(TransferRequestStatus.COMPLETED)))
        assertFalse(TransferPoolStatusStateResolver.isMatchActive(createRequest(TransferRequestStatus.WITHDRAWN)))
    }

    // -------------------------------------------------------------------------
    // Issue 3: resolveHeaderStatus
    // -------------------------------------------------------------------------

    @Test
    fun resolveHeaderStatus_searchingInPool() {
        val req = createRequest(TransferRequestStatus.PENDING)
        val header = TransferPoolStatusStateResolver.resolveHeaderStatus(req, mintSoft, blueSoft, roseSoft, mutedSurface)
        assertEquals(Icons.Default.Search, header.icon)
        assertEquals("In matching pool, searching for partner", header.contentDescription)
        assertEquals(Emerald, header.tintColor)
    }

    @Test
    fun resolveHeaderStatus_requestPendingWithOldCancelledMatchStatus_showsSearchingInPool() {
        // A request returned to PENDING after peer cancellation must show searching, not cancelled!
        val req = createRequest(TransferRequestStatus.PENDING, "CANCELLED")
        val header = TransferPoolStatusStateResolver.resolveHeaderStatus(req, mintSoft, blueSoft, roseSoft, mutedSurface)
        assertEquals(Icons.Default.Search, header.icon)
        assertEquals("In matching pool, searching for partner", header.contentDescription)
        assertEquals(Emerald, header.tintColor)
    }

    @Test
    fun resolveHeaderStatus_chatOpen() {
        val req = createRequest(TransferRequestStatus.MATCHED, "CHAT_OPEN")
        val header = TransferPoolStatusStateResolver.resolveHeaderStatus(req, mintSoft, blueSoft, roseSoft, mutedSurface)
        assertEquals(Icons.Default.SwapHoriz, header.icon)
        assertEquals("Team chat open", header.contentDescription)
        assertEquals(MedicalBlue, header.tintColor)
    }

    @Test
    fun resolveHeaderStatus_confirmed() {
        val req = createRequest(TransferRequestStatus.MATCHED, "CONFIRMED")
        val header = TransferPoolStatusStateResolver.resolveHeaderStatus(req, mintSoft, blueSoft, roseSoft, mutedSurface)
        assertEquals(Icons.Default.CheckCircle, header.icon)
        assertEquals("Transfer agreed and confirmed", header.contentDescription)
        assertEquals(Emerald, header.tintColor)
    }

    @Test
    fun resolveHeaderStatus_cancelledMatch() {
        val req = createRequest(TransferRequestStatus.MATCHED, "CANCELLED")
        val header = TransferPoolStatusStateResolver.resolveHeaderStatus(req, mintSoft, blueSoft, roseSoft, mutedSurface)
        assertEquals(Icons.Default.Close, header.icon)
        assertEquals("Transfer match cancelled", header.contentDescription)
        assertEquals(CriticalRed, header.tintColor)
    }

    @Test
    fun resolveHeaderStatus_expiredMatch() {
        val req = createRequest(TransferRequestStatus.MATCHED, "EXPIRED")
        val header = TransferPoolStatusStateResolver.resolveHeaderStatus(req, mintSoft, blueSoft, roseSoft, mutedSurface)
        assertEquals(Icons.Default.Info, header.icon)
        assertEquals("Transfer match expired", header.contentDescription)
        assertEquals(Slate, header.tintColor)
    }

    @Test
    fun resolveHeaderStatus_awaitingResponse() {
        val req = createRequest(TransferRequestStatus.MATCHED, "PENDING_CONFIRMATION")
        val header = TransferPoolStatusStateResolver.resolveHeaderStatus(req, mintSoft, blueSoft, roseSoft, mutedSurface)
        assertEquals(Icons.Default.HourglassEmpty, header.icon)
        assertEquals("Match awaiting your response", header.contentDescription)
    }

    @Test
    fun resolveHeaderStatus_completed() {
        val req = createRequest(TransferRequestStatus.COMPLETED)
        val header = TransferPoolStatusStateResolver.resolveHeaderStatus(req, mintSoft, blueSoft, roseSoft, mutedSurface)
        assertEquals(Icons.Default.VerifiedUser, header.icon)
        assertEquals("Transfer completed", header.contentDescription)
    }

    // -------------------------------------------------------------------------
    // Issue 5: resolveJourneyState
    // -------------------------------------------------------------------------

    @Test
    fun resolveJourneyState_inPool() {
        val req = createRequest(TransferRequestStatus.PENDING)
        val journey = TransferPoolStatusStateResolver.resolveJourneyState(req, blueSoft, primaryClinical, mintSoft, roseSoft, mutedSurface)
        assertEquals("Stage 2 of 5 • In transfer pool", journey.stageLabel)
        assertFalse(journey.step2Completed)
        assertTrue(journey.step2Active)
        assertEquals("In pool", journey.step2Sublabel)
        assertFalse(journey.step3Active)
        assertEquals("Pending", journey.step3Sublabel)
    }

    @Test
    fun resolveJourneyState_matchFound_awaitingResponse() {
        val req = createRequest(TransferRequestStatus.MATCHED, "PENDING_CONFIRMATION")
        val journey = TransferPoolStatusStateResolver.resolveJourneyState(req, blueSoft, primaryClinical, mintSoft, roseSoft, mutedSurface)
        assertEquals("Stage 3 of 5 • Review & accept", journey.stageLabel)
        assertTrue(journey.step2Completed)
        assertFalse(journey.step2Active)
        assertTrue(journey.step3Active)
        assertEquals("Action required", journey.step3Sublabel)
        assertFalse(journey.step4Active)
        assertEquals("Pending", journey.step4Sublabel)
    }

    @Test
    fun resolveJourneyState_chatOpen() {
        val req = createRequest(TransferRequestStatus.MATCHED, "CHAT_OPEN")
        val journey = TransferPoolStatusStateResolver.resolveJourneyState(req, blueSoft, primaryClinical, mintSoft, roseSoft, mutedSurface)
        assertEquals("Stage 4 of 5 • Team chat", journey.stageLabel)
        assertTrue(journey.step2Completed)
        assertTrue(journey.step3Completed)
        assertTrue(journey.step4Active)
        assertEquals("In progress", journey.step4Sublabel)
        assertFalse(journey.step5Active)
    }

    @Test
    fun resolveJourneyState_confirmed() {
        val req = createRequest(TransferRequestStatus.MATCHED, "CONFIRMED")
        val journey = TransferPoolStatusStateResolver.resolveJourneyState(req, blueSoft, primaryClinical, mintSoft, roseSoft, mutedSurface)
        assertEquals("Stage 5 of 5 • Confirmed", journey.stageLabel)
        assertTrue(journey.step2Completed)
        assertTrue(journey.step3Completed)
        assertTrue(journey.step4Completed)
        assertTrue(journey.step5Completed)
        assertTrue(journey.step5Active)
        assertEquals("All confirmed", journey.step5Sublabel)
    }

    @Test
    fun resolveJourneyState_cancelled() {
        val req = createRequest(TransferRequestStatus.MATCHED, "CANCELLED")
        val journey = TransferPoolStatusStateResolver.resolveJourneyState(req, blueSoft, primaryClinical, mintSoft, roseSoft, mutedSurface)
        assertEquals("Transfer cancelled", journey.stageLabel)
        assertTrue(journey.step2Completed)
        assertFalse(journey.step3Completed)
        assertFalse(journey.step3Active)
        assertEquals("Cancelled", journey.step3Sublabel)
        assertEquals("Closed", journey.step4Sublabel)
        assertEquals("Closed", journey.step5Sublabel)
    }

    @Test
    fun resolveJourneyState_expired() {
        val req = createRequest(TransferRequestStatus.MATCHED, "EXPIRED")
        val journey = TransferPoolStatusStateResolver.resolveJourneyState(req, blueSoft, primaryClinical, mintSoft, roseSoft, mutedSurface)
        assertEquals("Transfer expired", journey.stageLabel)
        assertTrue(journey.step2Completed)
        assertFalse(journey.step3Completed)
        assertFalse(journey.step3Active)
        assertEquals("Expired", journey.step3Sublabel)
        assertEquals("Closed", journey.step4Sublabel)
        assertEquals("Closed", journey.step5Sublabel)
    }

    @Test
    fun resolveJourneyState_completed_showsFinalizedMilestones() {
        val req = createRequest(TransferRequestStatus.COMPLETED)
        val journey = TransferPoolStatusStateResolver.resolveJourneyState(req, blueSoft, primaryClinical, mintSoft, roseSoft, mutedSurface)
        assertEquals("Transfer completed", journey.stageLabel)
        assertEquals(mintSoft, journey.badgeBgColor)
        assertEquals(Emerald, journey.badgeTextColor)
        assertTrue(journey.step2Completed)
        assertFalse(journey.step2Active)
        assertEquals("Done", journey.step2Sublabel)
        assertTrue(journey.step3Completed)
        assertFalse(journey.step3Active)
        assertEquals("Done", journey.step3Sublabel)
        assertTrue(journey.step4Completed)
        assertFalse(journey.step4Active)
        assertEquals("Done", journey.step4Sublabel)
        assertTrue(journey.step5Completed)
        assertFalse(journey.step5Active)
        assertEquals("Completed", journey.step5Sublabel)
    }

    @Test
    fun resolveJourneyState_withdrawn_showsWithdrawnMilestones() {
        val req = createRequest(TransferRequestStatus.WITHDRAWN)
        val journey = TransferPoolStatusStateResolver.resolveJourneyState(req, blueSoft, primaryClinical, mintSoft, roseSoft, mutedSurface)
        assertEquals("Request withdrawn", journey.stageLabel)
        assertEquals(mutedSurface, journey.badgeBgColor)
        assertEquals(Slate, journey.badgeTextColor)
        assertFalse(journey.step2Completed)
        assertFalse(journey.step2Active)
        assertEquals("Withdrawn", journey.step2Sublabel)
        assertFalse(journey.step3Completed)
        assertFalse(journey.step3Active)
        assertEquals("Closed", journey.step3Sublabel)
        assertFalse(journey.step4Completed)
        assertFalse(journey.step4Active)
        assertEquals("Closed", journey.step4Sublabel)
        assertFalse(journey.step5Completed)
        assertFalse(journey.step5Active)
        assertEquals("Closed", journey.step5Sublabel)
    }

    @Test
    fun resolveJourneyState_pendingWithStaleCancelledMatchStatus_showsInPoolStage2() {
        // When peer cancels, nurse's request returns to PENDING while matchStatus remains CANCELLED in cache
        val req = createRequest(TransferRequestStatus.PENDING, "CANCELLED")
        val journey = TransferPoolStatusStateResolver.resolveJourneyState(req, blueSoft, primaryClinical, mintSoft, roseSoft, mutedSurface)
        assertEquals("Stage 2 of 5 • In transfer pool", journey.stageLabel)
        assertFalse(journey.step2Completed)
        assertTrue(journey.step2Active)
        assertEquals("In pool", journey.step2Sublabel)
        assertFalse(journey.step3Active)
        assertEquals("Pending", journey.step3Sublabel)
    }

    @Test
    fun resolveJourneyState_pendingWithStaleExpiredMatchStatus_showsInPoolStage2() {
        val req = createRequest(TransferRequestStatus.PENDING, "EXPIRED")
        val journey = TransferPoolStatusStateResolver.resolveJourneyState(req, blueSoft, primaryClinical, mintSoft, roseSoft, mutedSurface)
        assertEquals("Stage 2 of 5 • In transfer pool", journey.stageLabel)
        assertFalse(journey.step2Completed)
        assertTrue(journey.step2Active)
        assertEquals("In pool", journey.step2Sublabel)
        assertFalse(journey.step3Active)
        assertEquals("Pending", journey.step3Sublabel)
    }
}

