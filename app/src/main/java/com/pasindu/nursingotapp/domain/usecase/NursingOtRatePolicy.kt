package com.pasindu.nursingotapp.domain.usecase

/**
 * Fixed Nursing Service overtime rates used by NursingOTApp.
 *
 * These are the rates supplied for the Nursing Service:
 * Grade III Rs. 283/hour
 * Grade II Rs. 338/hour
 * Grade I Rs. 416/hour
 * Supra Grade Rs. 483/hour
 * Special Grade Rs. 571/hour
 */
object NursingOtRatePolicy {
    const val GRADE_III = "Grade III"
    const val GRADE_II = "Grade II"
    const val GRADE_I = "Grade I"
    const val SUPRA_GRADE = "Supra Grade"
    const val SPECIAL_GRADE = "Special Grade"

    const val RATE_GRADE_III = 283.0
    const val RATE_GRADE_II = 338.0
    const val RATE_GRADE_I = 416.0
    const val RATE_SUPRA_GRADE = 483.0
    const val RATE_SPECIAL_GRADE = 571.0

    val grades: List<String> = listOf(
        GRADE_III,
        GRADE_II,
        GRADE_I,
        SUPRA_GRADE,
        SPECIAL_GRADE
    )

    fun rateForGrade(grade: String): Double? = when (grade.trim()) {
        GRADE_III -> RATE_GRADE_III
        GRADE_II -> RATE_GRADE_II
        GRADE_I -> RATE_GRADE_I
        SUPRA_GRADE -> RATE_SUPRA_GRADE
        SPECIAL_GRADE -> RATE_SPECIAL_GRADE
        else -> null
    }
}
