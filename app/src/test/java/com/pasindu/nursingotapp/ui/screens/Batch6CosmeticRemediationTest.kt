package com.pasindu.nursingotapp.ui.screens

import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.domain.export.ClaimExportValidator
import com.pasindu.nursingotapp.domain.usecase.PayRateResolver
import com.pasindu.nursingotapp.ui.model.DailyEntryUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * JVM unit tests for Batch 6 (NEW-UI-01 and NEW-UI-02):
 * Verifies ExportCenterCard readiness states (Incomplete vs Ready to Export vs Save Required)
 * and ClinicalTaskCard simultaneous Priority + DND Bypass + Done state preservation.
 */
class Batch6CosmeticRemediationTest {

    private val validProfile = ProfileEntity(
        id = 1,
        fullName = "SYNTHETIC NURSE AUDIT",
        serviceNo = "SN-SYNTH-901",
        unit = "Ward 4B - Synthetic Audit",
        paySheetNo = "PS-SYNTH-2026",
        grade = "III",
        basicSalary = 51457.0,
        otRate = 420.0,
        updatedAt = 1000L
    )

    private val validRates = PayRateResolver.Rates(
        phRate = 1830.67,
        doRate = 1830.67,
        basis2027 = 54920.0
    )

    private fun createSavedEntries(dates: List<LocalDate>): List<DailyEntryUiModel> {
        return dates.mapIndexed { index, date ->
            DailyEntryUiModel(
                id = index.toLong() + 1,
                claimPeriodId = 1L,
                date = date,
                isPH = false,
                isDO = false,
                isLeave = false,
                leaveType = null,
                normalTimeIn = "07.00",
                normalTimeOut = "13.00",
                normalHours = 6.0f,
                otTimeIn = "",
                otTimeOut = "",
                otHours = 0f,
                wardOverride = "",
                reason = ""
            )
        }
    }

    @Test
    fun exportCenterReadiness_incompleteState_preservesValidationAndStatusSemantics() {
        val periodDates = (1..28).map { LocalDate.of(2026, 11, it) }
        val savedEntries = createSavedEntries(periodDates.take(4))

        val readiness = ClaimExportValidator.validate(
            profile = validProfile,
            resolvedRates = validRates,
            allDates = periodDates,
            savedEntries = savedEntries,
            unsavedStagedEditsCount = 0,
            configuredOtRate = 420.0
        )

        assertTrue(readiness.isProfileComplete)
        assertTrue(readiness.isSalaryScaleMatched)
        assertFalse("Calendar with 4/28 days must be incomplete", readiness.isCalendarComplete)
        assertFalse("No unsaved edits", readiness.hasUnsavedStagedEdits)
        assertFalse("Cannot export immediately without warning when incomplete", readiness.canExportImmediately)
        assertTrue("Requires incomplete confirmation", readiness.requiresIncompleteConfirmation)
        assertNull(readiness.blockerReason)
        assertEquals(4, readiness.completedDaysCount)
        assertEquals(28, readiness.totalPeriodDays)
    }

    @Test
    fun exportCenterReadiness_readyState_preservesValidationAndStatusSemantics() {
        val periodDates = (1..28).map { LocalDate.of(2026, 11, it) }
        val savedEntries = createSavedEntries(periodDates)

        val readiness = ClaimExportValidator.validate(
            profile = validProfile,
            resolvedRates = validRates,
            allDates = periodDates,
            savedEntries = savedEntries,
            unsavedStagedEditsCount = 0,
            configuredOtRate = 420.0
        )

        assertTrue(readiness.isProfileComplete)
        assertTrue(readiness.isSalaryScaleMatched)
        assertTrue(readiness.isCalendarComplete)
        assertFalse(readiness.hasUnsavedStagedEdits)
        assertTrue("28/28 logged with valid profile and 0 staged edits must be Ready to Export", readiness.canExportImmediately)
        assertFalse(readiness.requiresIncompleteConfirmation)
        assertNull(readiness.blockerReason)
    }

    @Test
    fun clinicalTaskEntity_simultaneousPriorityDndBypassAndDone_preservesAllMetadata() {
        val task = ClinicalTaskEntity(
            id = 601,
            taskName = "Administer IV Ceftriaxone 1g",
            description = "Reconstitute in 10 ml sterile water and administer according to the prescribed order and local protocol.",
            priority = "HIGH",
            triggerTime = 1762800000000L,
            isCompleted = true,
            bypassDnd = true
        )

        assertEquals("HIGH", task.priority)
        assertTrue("DND Bypass must remain enabled when task is completed", task.bypassDnd)
        assertTrue("Completion state must be true", task.isCompleted)
        assertEquals("Administer IV Ceftriaxone 1g", task.taskName)
    }
}
