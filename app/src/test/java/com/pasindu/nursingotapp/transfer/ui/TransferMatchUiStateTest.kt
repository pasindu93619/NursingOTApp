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
}
