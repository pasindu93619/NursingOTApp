package com.pasindu.nursingotapp.transfer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferMatchUiStateTest {

    @Test
    fun sampleMatchUiModel_initializesWithExpectedDefaults() {
        val model = createSampleMatchUiModel()

        assertEquals("MATCH-2026-0042", model.matchId)
        assertEquals("MOH2026-0001", model.myHospitalId)
        assertEquals("MOH2026-0045", model.partnerHospitalId)
        assertEquals("Grade I", model.myGrade)
        assertEquals("Grade I", model.partnerGrade)
        assertTrue(model.isSameGrade)
        assertEquals("DIRECT_2_WAY", model.matchType)
        assertEquals(MatchDecisionStatus.PENDING, model.myStatus)
        assertEquals(MatchDecisionStatus.PENDING, model.partnerStatus)
        assertNotNull(model.expiresAtMs)
    }

    @Test
    fun crossGradeMatch_preservesEligibleFlagAndDifferentGrades() {
        val crossGradeModel = TransferMatchUiModel(
            matchId = "MATCH-2026-0099",
            myHospitalId = "HOSP-001",
            myHospitalName = "Colombo General",
            myHospitalLocation = "Western Province",
            myGrade = "Grade I",
            partnerHospitalId = "HOSP-002",
            partnerHospitalName = "Galle General",
            partnerHospitalLocation = "Southern Province",
            partnerGrade = "Grade II",
            isSameGrade = false,
            compatibilityReason = "Cross-grade mutual transfer",
            expiresAtMs = 1700000000000L
        )

        assertFalse(crossGradeModel.isSameGrade)
        assertEquals("Grade I", crossGradeModel.myGrade)
        assertEquals("Grade II", crossGradeModel.partnerGrade)
        assertEquals("DIRECT_2_WAY", crossGradeModel.matchType)
    }

    @Test
    fun uiState_sealedHierarchyVerification() {
        val match = createSampleMatchUiModel()

        val stateMatchFound: TransferMatchUiState = TransferMatchUiState.MatchFound(match)
        assertTrue(stateMatchFound is TransferMatchUiState.MatchFound)
        assertEquals(match, (stateMatchFound as TransferMatchUiState.MatchFound).match)

        val stateAccepted: TransferMatchUiState = TransferMatchUiState.AlreadyAccepted(match)
        assertTrue(stateAccepted is TransferMatchUiState.AlreadyAccepted)

        val stateRejected: TransferMatchUiState = TransferMatchUiState.AlreadyRejected(match)
        assertTrue(stateRejected is TransferMatchUiState.AlreadyRejected)

        val stateExpired: TransferMatchUiState = TransferMatchUiState.Expired(match)
        assertTrue(stateExpired is TransferMatchUiState.Expired)

        val stateLoading: TransferMatchUiState = TransferMatchUiState.Loading
        assertTrue(stateLoading is TransferMatchUiState.Loading)

        val stateError: TransferMatchUiState = TransferMatchUiState.Error("Network failure")
        assertTrue(stateError is TransferMatchUiState.Error)
        assertEquals("Network failure", (stateError as TransferMatchUiState.Error).message)
    }

    @Test
    fun sampleThreeWayMatchUiModel_initializesWithExpectedDefaults() {
        val model = createSampleThreeWayMatchUiModel()

        assertEquals("MATCH-3WAY-2026-0001", model.matchId)
        assertEquals("THREE_WAY", model.matchType)
        assertTrue(model.isSameGrade)
        assertNotNull(model.participant2)
        assertNotNull(model.participant3)
        assertEquals("NURSE 2", model.participant2?.roleLabel)
        assertEquals("NURSE 3", model.participant3?.roleLabel)
        assertEquals("MOH2026-0045", model.participant2?.hospitalId)
        assertEquals("MOH2026-0080", model.participant3?.hospitalId)
        assertEquals(MatchDecisionStatus.PENDING, model.myStatus)
        assertEquals(MatchDecisionStatus.PENDING, model.participant2?.status)
        assertEquals(MatchDecisionStatus.PENDING, model.participant3?.status)
    }
    @Test
    fun stateFor_centralizedMapping_mapsAllStatusesCorrectly() {
        val baseModel = createSampleMatchUiModel()

        // 1. CHAT_OPEN -> ChatOpen
        val chatOpenState = TransferMatchViewModel.stateFor(baseModel.copy(serverStatus = "CHAT_OPEN"))
        assertTrue(chatOpenState is TransferMatchUiState.ChatOpen)
        assertEquals("CHAT_OPEN", (chatOpenState as TransferMatchUiState.ChatOpen).match.serverStatus)

        // 2. CONFIRMED -> Confirmed
        val confirmedState = TransferMatchViewModel.stateFor(baseModel.copy(serverStatus = "CONFIRMED"))
        assertTrue(confirmedState is TransferMatchUiState.Confirmed)
        assertEquals("CONFIRMED", (confirmedState as TransferMatchUiState.Confirmed).match.serverStatus)

        // 3. CANCELLED -> AlreadyRejected
        val cancelledState = TransferMatchViewModel.stateFor(baseModel.copy(serverStatus = "CANCELLED"))
        assertTrue(cancelledState is TransferMatchUiState.AlreadyRejected)
        assertEquals("CANCELLED", (cancelledState as TransferMatchUiState.AlreadyRejected).match.serverStatus)

        // 4. EXPIRED -> Expired
        val expiredState = TransferMatchViewModel.stateFor(baseModel.copy(serverStatus = "EXPIRED"))
        assertTrue(expiredState is TransferMatchUiState.Expired)
        assertEquals("EXPIRED", (expiredState as TransferMatchUiState.Expired).match.serverStatus)

        // 5. Otherwise, myStatus == ACCEPTED -> AlreadyAccepted
        val acceptedState = TransferMatchViewModel.stateFor(
            baseModel.copy(serverStatus = "PENDING_CONFIRMATION", myStatus = MatchDecisionStatus.ACCEPTED)
        )
        assertTrue(acceptedState is TransferMatchUiState.AlreadyAccepted)

        // 6. Other statuses (e.g. PENDING_CONFIRMATION with myStatus PENDING) -> MatchFound
        val matchFoundState = TransferMatchViewModel.stateFor(
            baseModel.copy(serverStatus = "PENDING_CONFIRMATION", myStatus = MatchDecisionStatus.PENDING)
        )
        assertTrue(matchFoundState is TransferMatchUiState.MatchFound)

        // 7. Unknown status with myStatus ACCEPTED -> AlreadyAccepted
        val customAccepted = TransferMatchViewModel.stateFor(
            baseModel.copy(serverStatus = "UNKNOWN_WAITING", myStatus = MatchDecisionStatus.ACCEPTED)
        )
        assertTrue(customAccepted is TransferMatchUiState.AlreadyAccepted)

        // 8. Unknown status with myStatus PENDING -> MatchFound
        val customPending = TransferMatchViewModel.stateFor(
            baseModel.copy(serverStatus = "UNKNOWN_WAITING", myStatus = MatchDecisionStatus.PENDING)
        )
        assertTrue(customPending is TransferMatchUiState.MatchFound)
    }
}
