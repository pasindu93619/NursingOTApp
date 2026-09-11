package com.pasindu.nursingotapp.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class NursingAiContextTest {
    @Test
    fun financeContextContainsAuthoritativeFinancialFormulas() {
        val context = nursingAiContextForRoute("advanced_finance_hub")
        requireNotNull(context)
        val promptContext = context.asPromptContext()

        assertTrue(promptContext.contains("OT amount = total OT hours × configured OT rate."))
        assertTrue(promptContext.contains("Gross = basic salary + risk allowance + CLA allowance + additional allowances + OT + PH + DO."))
        assertTrue(promptContext.contains("Estimated net = gross earnings − listed paysheet deductions."))
    }

    @Test
    fun unknownRouteDoesNotExposeAContext() {
        assertTrue(nursingAiContextForRoute("unknown_route") == null)
    }

    @Test
    fun clinicalContextContainsSafetyBoundary() {
        val context = nursingAiContextForRoute("clinical_calculators")
        requireNotNull(context)
        assertTrue(context.safetyRules.any { it.contains("clinical judgement", ignoreCase = true) })
    }
}
