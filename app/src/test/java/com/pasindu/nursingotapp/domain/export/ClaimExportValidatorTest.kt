package com.pasindu.nursingotapp.domain.export

import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.domain.usecase.PayRateResolver
import com.pasindu.nursingotapp.ui.model.DailyEntryUiModel
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaimExportValidatorTest {

    private val sampleProfile = ProfileEntity(
        id = 1,
        fullName = "Perera A.B.",
        serviceNo = "SN-12345",
        unit = "Ward 4A",
        paySheetNo = "PS-9988",
        grade = "Grade I",
        basicSalary = 55000.0,
        otRate = 450.0,
        updatedAt = 1000L
    )

    private val sampleRates = PayRateResolver.Rates(
        phRate = 1833.33,
        doRate = 1833.33,
        basis2027 = 55000.0
    )

    private val marchDates = (1..31).map { LocalDate.of(2026, 3, it) }

    private fun createCompleteSavedEntries(dates: List<LocalDate>): List<DailyEntryUiModel> {
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
    fun validCompleteStatePermitsImmediateExport() {
        val savedEntries = createCompleteSavedEntries(marchDates)
        val result = ClaimExportValidator.validate(
            profile = sampleProfile,
            resolvedRates = sampleRates,
            allDates = marchDates,
            savedEntries = savedEntries,
            unsavedStagedEditsCount = 0
        )

        assertTrue(result.isProfileComplete)
        assertTrue(result.isSalaryScaleMatched)
        assertTrue(result.isCalendarComplete)
        assertFalse(result.hasUnsavedStagedEdits)
        assertTrue(result.canExportImmediately)
        assertFalse(result.requiresIncompleteConfirmation)
        assertNull(result.blockerReason)
        assertEquals(31, result.completedDaysCount)
        assertEquals(0, result.missingDaysCount)
    }

    @Test
    fun incompleteProfileFlagsMissingFieldsAndBlocksExport() {
        val incompleteProfile = sampleProfile.copy(
            serviceNo = "",
            paySheetNo = "  ",
            basicSalary = 0.0
        )
        val savedEntries = createCompleteSavedEntries(marchDates)

        val result = ClaimExportValidator.validate(
            profile = incompleteProfile,
            resolvedRates = sampleRates,
            allDates = marchDates,
            savedEntries = savedEntries,
            unsavedStagedEditsCount = 0
        )

        assertFalse(result.isProfileComplete)
        assertFalse(result.canExportImmediately)
        assertTrue(result.missingProfileFields.contains("Service Number"))
        assertTrue(result.missingProfileFields.contains("Pay Sheet Number"))
        assertTrue(result.missingProfileFields.contains("Basic Salary"))
        assertNotNull(result.blockerReason)
        assertTrue(result.blockerReason!!.contains("Profile incomplete"))
    }

    @Test
    fun unmatchedSalaryStepBlocksExport() {
        val savedEntries = createCompleteSavedEntries(marchDates)

        val result = ClaimExportValidator.validate(
            profile = sampleProfile,
            resolvedRates = null, // PayRateResolver returned null
            allDates = marchDates,
            savedEntries = savedEntries,
            unsavedStagedEditsCount = 0
        )

        assertFalse(result.isSalaryScaleMatched)
        assertFalse(result.canExportImmediately)
        assertNotNull(result.blockerReason)
        assertTrue(result.blockerReason!!.contains("Basic salary is not matched to the 2027 salary scale"))
    }

    @Test
    fun unsavedStagedEditsBlockExportUntilSaved() {
        val savedEntries = createCompleteSavedEntries(marchDates)

        val result = ClaimExportValidator.validate(
            profile = sampleProfile,
            resolvedRates = sampleRates,
            allDates = marchDates,
            savedEntries = savedEntries,
            unsavedStagedEditsCount = 5
        )

        assertTrue(result.hasUnsavedStagedEdits)
        assertEquals(5, result.unsavedEditsCount)
        assertFalse(result.canExportImmediately)
        assertNotNull(result.blockerReason)
        assertTrue(result.blockerReason!!.contains("5 unsaved calendar edit(s)"))
    }

    @Test
    fun incompleteCalendarRequiresUserConfirmationBeforeExport() {
        // Only 20 days logged out of 31
        val partialEntries = createCompleteSavedEntries(marchDates.take(20))

        val result = ClaimExportValidator.validate(
            profile = sampleProfile,
            resolvedRates = sampleRates,
            allDates = marchDates,
            savedEntries = partialEntries,
            unsavedStagedEditsCount = 0
        )

        assertTrue(result.isProfileComplete)
        assertTrue(result.isSalaryScaleMatched)
        assertFalse(result.hasUnsavedStagedEdits)
        assertFalse(result.isCalendarComplete)
        assertEquals(20, result.completedDaysCount)
        assertEquals(11, result.missingDaysCount)

        assertFalse(result.canExportImmediately)
        assertTrue(result.requiresIncompleteConfirmation)
        assertNull(result.blockerReason) // Not a hard blocker, requires confirmation
    }

    @Test
    fun configuredOtRateOverrideIsRespected() {
        val savedEntries = createCompleteSavedEntries(marchDates)

        val result = ClaimExportValidator.validate(
            profile = sampleProfile.copy(otRate = 400.0),
            resolvedRates = sampleRates,
            allDates = marchDates,
            savedEntries = savedEntries,
            unsavedStagedEditsCount = 0,
            configuredOtRate = 525.0
        )

        assertEquals(525.0, result.resolvedOtRate, 0.01)
    }
}
