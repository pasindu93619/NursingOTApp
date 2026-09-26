package com.pasindu.nursingotapp.domain.usecase

/**
 * Fixed Nursing Service overtime rates used by NursingOTApp.
 *
 * Persisted profiles may contain legacy grade labels such as "III".
 * The canonical labels remain unchanged so salary-table matching and
 * stored profile data are not rewritten just to fix presentation lookup.
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

    /**
     * Converts both current canonical labels and known legacy labels to the
     * canonical Nursing Service grade used by the policy table.
     */
    fun canonicalGrade(value: String): String? = when (value.trim().uppercase()) {
        "III", "GRADE III", "GRADE 3" -> GRADE_III
        "II", "GRADE II", "GRADE 2" -> GRADE_II
        "I", "GRADE I", "GRADE 1" -> GRADE_I
        "SUPRA", "SUPRA GRADE" -> SUPRA_GRADE
        "SPECIAL", "SPECIAL GRADE" -> SPECIAL_GRADE
        else -> null
    }

    fun rateForGrade(grade: String): Double? = when (canonicalGrade(grade)) {
        GRADE_III -> RATE_GRADE_III
        GRADE_II -> RATE_GRADE_II
        GRADE_I -> RATE_GRADE_I
        SUPRA_GRADE -> RATE_SUPRA_GRADE
        SPECIAL_GRADE -> RATE_SPECIAL_GRADE
        else -> null
    }
}
