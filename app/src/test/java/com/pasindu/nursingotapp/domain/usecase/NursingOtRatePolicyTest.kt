package com.pasindu.nursingotapp.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NursingOtRatePolicyTest {

    @Test
    fun legacyGradeLabelsResolveToExistingNursingServiceRates() {
        assertEquals(283.0, NursingOtRatePolicy.rateForGrade("III"), 0.0)
        assertEquals(283.0, NursingOtRatePolicy.rateForGrade("Grade III"), 0.0)
        assertEquals(338.0, NursingOtRatePolicy.rateForGrade("grade 2"), 0.0)
        assertEquals(416.0, NursingOtRatePolicy.rateForGrade("I"), 0.0)
        assertEquals(483.0, NursingOtRatePolicy.rateForGrade("Supra"), 0.0)
        assertEquals(571.0, NursingOtRatePolicy.rateForGrade("special grade"), 0.0)
    }

    @Test
    fun unknownGradeDoesNotProduceAnOtRate() {
        assertNull(NursingOtRatePolicy.rateForGrade("Unknown Grade"))
    }
}
