package com.pasindu.nursingotapp.domain.export

import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.domain.usecase.PayRateResolver
import com.pasindu.nursingotapp.ui.model.DailyEntryUiModel
import java.time.LocalDate

/**
 * Pre-export readiness validation model for OT Claim PDF generation.
 */
data class ExportReadinessState(
    val isProfileComplete: Boolean,
    val missingProfileFields: List<String>,
    val isSalaryScaleMatched: Boolean,
    val resolvedOtRate: Double,
    val totalPeriodDays: Int,
    val completedDaysCount: Int,
    val missingDaysCount: Int,
    val isCalendarComplete: Boolean,
    val hasUnsavedStagedEdits: Boolean,
    val unsavedEditsCount: Int,
    val canExportImmediately: Boolean,
    val requiresIncompleteConfirmation: Boolean,
    val blockerReason: String?
)

/**
 * Validates prerequisite readiness before compiling the official Ministry OT claim PDF.
 * Prevents rejected submissions, missing salary rates, and discrepancies caused by unsaved edits.
 */
object ClaimExportValidator {

    fun validate(
        profile: ProfileEntity?,
        resolvedRates: PayRateResolver.Rates?,
        allDates: List<LocalDate>,
        savedEntries: List<DailyEntryUiModel>,
        unsavedStagedEditsCount: Int,
        configuredOtRate: Double = 0.0
    ): ExportReadinessState {
        val missingFields = mutableListOf<String>()

        if (profile == null) {
            missingFields.add("Profile")
        } else {
            if (profile.fullName.isBlank()) missingFields.add("Full Name")
            if (profile.serviceNo.isBlank()) missingFields.add("Service Number")
            if (profile.grade.isBlank()) missingFields.add("Grade")
            if (profile.unit.isBlank()) missingFields.add("Unit / Ward")
            if (profile.paySheetNo.isBlank()) missingFields.add("Pay Sheet Number")
            if (profile.basicSalary <= 0.0) missingFields.add("Basic Salary")
        }

        val isProfileComplete = missingFields.isEmpty() && profile != null
        val isSalaryScaleMatched = resolvedRates != null

        val effectiveOtRate = if (configuredOtRate > 0.0) {
            configuredOtRate
        } else {
            profile?.otRate?.coerceAtLeast(0.0) ?: 0.0
        }

        val totalPeriodDays = allDates.size
        val savedDatesSet = savedEntries.filter { entry ->
            entry.normalTimeIn.isNotBlank() || entry.isLeave || entry.isDO || entry.isPH || entry.otHours > 0f
        }.map { it.date }.toSet()

        val completedDaysCount = allDates.count { it in savedDatesSet }
        val missingDaysCount = (totalPeriodDays - completedDaysCount).coerceAtLeast(0)
        val isCalendarComplete = totalPeriodDays > 0 && completedDaysCount >= totalPeriodDays

        val hasUnsavedStagedEdits = unsavedStagedEditsCount > 0

        val blockerReason = when {
            !isProfileComplete -> "Profile incomplete. Missing: ${missingFields.joinToString(", ")}."
            !isSalaryScaleMatched -> "Basic salary is not matched to the 2027 salary scale. Please check Grade and Salary in Profile."
            hasUnsavedStagedEdits -> "You have $unsavedStagedEditsCount unsaved calendar edit(s). Save changes before exporting."
            else -> null
        }

        val canExportImmediately = isProfileComplete && isSalaryScaleMatched && !hasUnsavedStagedEdits && isCalendarComplete
        val requiresIncompleteConfirmation = isProfileComplete && isSalaryScaleMatched && !hasUnsavedStagedEdits && !isCalendarComplete

        return ExportReadinessState(
            isProfileComplete = isProfileComplete,
            missingProfileFields = missingFields,
            isSalaryScaleMatched = isSalaryScaleMatched,
            resolvedOtRate = effectiveOtRate,
            totalPeriodDays = totalPeriodDays,
            completedDaysCount = completedDaysCount,
            missingDaysCount = missingDaysCount,
            isCalendarComplete = isCalendarComplete,
            hasUnsavedStagedEdits = hasUnsavedStagedEdits,
            unsavedEditsCount = unsavedStagedEditsCount,
            canExportImmediately = canExportImmediately,
            requiresIncompleteConfirmation = requiresIncompleteConfirmation,
            blockerReason = blockerReason
        )
    }
}
