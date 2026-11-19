package com.pasindu.nursingotapp.statgraph.engine

import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.TimeGranularity
import com.pasindu.nursingotapp.statgraph.model.TimePeriodParser
import java.util.Locale

enum class DetectionConfidence(val label: String) {
    HIGH("High match"),
    MEDIUM("Likely match"),
    LOW("Fallback guess")
}

data class RoleDetectionResult(
    val suggestedRole: FieldRole,
    val suggestedUnit: String?,
    val confidence: DetectionConfidence,
    val reason: String,
    val isEstimateBoundaryHint: Boolean = false
)

/**
 * Inspects field names, units, and sample cell strings to suggest a [FieldRole].
 *
 * Suggestions are advisory only and never override a user-confirmed role.
 */
object RoleAutoDetector {

    private val dayNameTokens = listOf("day", "date", "dob_day", "admission_date", "entry_date", "daily")
    private val weekNameTokens = listOf("week", "epi_week", "epiweek", "wk", "weekly")
    private val monthNameTokens = listOf("month", "yr_month", "period_month", "monthly", "ym")
    private val yearNameTokens = listOf("year", "yr", "annual", "yearly", "fy")

    private val estimateNameTokens = listOf(
        "lower limit", "upper limit", "lower_limit", "upper_limit",
        "ci lower", "ci upper", "ci_lower", "ci_upper",
        "lcl", "ucl", "lower bound", "upper bound", "confidence", "estimate"
    )
    private val durationNameTokens = listOf(
        "duration", "los", "length of stay", "stay", "follow-up", "followup",
        "time to", "survival", "wait time", "turnaround"
    )
    private val rateNameTokens = listOf(
        "percent", "percentage", "pct", "%", "rate", "ratio", "proportion",
        "incidence", "prevalence", "per 1000", "per 1,000", "per_1000",
        "mean", "average", "avg", "median", "score", "index", "bmi", "occupancy"
    )
    private val groupNameTokens = listOf(
        "group", "cohort", "arm", "stratum", "series", "age group", "age_group",
        "shift", "severity", "tier"
    )
    private val categoryNameTokens = listOf(
        "gender", "sex", "ward", "unit", "department", "dept", "district",
        "province", "hospital", "clinic", "category", "type", "diagnosis",
        "outcome", "status", "grade"
    )
    private val labelNameTokens = listOf(
        "label", "note", "notes", "comment", "remarks", "annotation", "tag"
    )
    private val totalMeasureTokens = listOf(
        "total", "all ", "population", "denominator", "admissions", "capacity", "beds"
    )
    private val countMeasureTokens = listOf(
        "patient", "patients", "case", "cases", "count", "number", "num",
        "death", "deaths", "visit", "visits", "event", "events", "staff",
        "nurses", "dengue", "infection", "infections", "incidents"
    )

    fun suggestRole(
        fieldName: String,
        unit: String = "",
        sampleValues: List<String> = emptyList(),
        existingFields: List<StatField> = emptyList()
    ): RoleDetectionResult {
        val normalizedName = fieldName.trim().lowercase(Locale.ENGLISH)
        val normalizedUnit = unit.trim().lowercase(Locale.ENGLISH)
        val nonBlankSamples = sampleValues.map { it.trim() }.filter { it.isNotEmpty() }

        // 1. Check period formats in samples first if unambiguous
        val sampleGranularity = TimePeriodParser.detectGranularity(nonBlankSamples)
        if (sampleGranularity != null && (normalizedName.isEmpty() || matchesAny(normalizedName, dayNameTokens + weekNameTokens + monthNameTokens + yearNameTokens + listOf("period", "time")))) {
            return RoleDetectionResult(
                suggestedRole = FieldRole.Time.fromGranularity(sampleGranularity),
                suggestedUnit = null,
                confidence = DetectionConfidence.HIGH,
                reason = "Detected ${sampleGranularity.displayName.lowercase(Locale.ENGLISH)} period format in sample values"
            )
        }

        // 2. Time keywords by field name
        when {
            matchesAny(normalizedName, monthNameTokens) -> return RoleDetectionResult(
                suggestedRole = FieldRole.Time.Month,
                suggestedUnit = null,
                confidence = DetectionConfidence.HIGH,
                reason = "Field name matches monthly time period"
            )
            matchesAny(normalizedName, weekNameTokens) -> return RoleDetectionResult(
                suggestedRole = FieldRole.Time.Week,
                suggestedUnit = null,
                confidence = DetectionConfidence.HIGH,
                reason = "Field name matches weekly time period"
            )
            matchesAny(normalizedName, dayNameTokens) -> return RoleDetectionResult(
                suggestedRole = FieldRole.Time.Day,
                suggestedUnit = null,
                confidence = DetectionConfidence.HIGH,
                reason = "Field name matches daily date period"
            )
            matchesAny(normalizedName, yearNameTokens) -> return RoleDetectionResult(
                suggestedRole = FieldRole.Time.Year,
                suggestedUnit = null,
                confidence = DetectionConfidence.HIGH,
                reason = "Field name matches annual time period"
            )
        }

        // 3. Estimate boundaries / confidence intervals
        if (matchesAny(normalizedName, estimateNameTokens)) {
            return RoleDetectionResult(
                suggestedRole = FieldRole.EstimateWithLimits(),
                suggestedUnit = unit.ifBlank { null },
                confidence = DetectionConfidence.HIGH,
                reason = "Name suggests an estimate or lower/upper confidence boundary",
                isEstimateBoundaryHint = true
            )
        }

        // 4. Duration + event flag
        if (matchesAny(normalizedName, durationNameTokens)) {
            val inferredUnit = unit.ifBlank {
                when {
                    normalizedName.contains("hour") -> "hours"
                    normalizedName.contains("day") || normalizedName.contains("los") -> "days"
                    normalizedName.contains("month") -> "months"
                    else -> "days"
                }
            }
            return RoleDetectionResult(
                suggestedRole = FieldRole.DurationWithEvent(),
                suggestedUnit = inferredUnit,
                confidence = DetectionConfidence.HIGH,
                reason = "Field name matches duration / time-to-event"
            )
        }

        // 5. Rate / percentage / continuous
        if (matchesAny(normalizedName, rateNameTokens) ||
            normalizedUnit == "%" ||
            normalizedUnit.contains("percent") ||
            normalizedUnit.contains("per 1000") ||
            normalizedUnit.contains("per 1,000")
        ) {
            val inferredUnit = unit.ifBlank {
                when {
                    normalizedName.contains("1000") || normalizedName.contains("1,000") -> "per 1,000"
                    normalizedName.contains("percent") || normalizedName.contains("pct") || normalizedName.contains("%") -> "%"
                    else -> "%"
                }
            }
            return RoleDetectionResult(
                suggestedRole = FieldRole.RateContinuous(scaleHint = inferredUnit),
                suggestedUnit = inferredUnit,
                confidence = DetectionConfidence.HIGH,
                reason = "Name or unit indicates a rate or continuous metric"
            )
        }

        // 6. Group / Category / Label keywords
        if (matchesAny(normalizedName, groupNameTokens)) {
            return RoleDetectionResult(
                suggestedRole = FieldRole.Group,
                suggestedUnit = null,
                confidence = DetectionConfidence.HIGH,
                reason = "Field name matches grouping / stratification dimension"
            )
        }
        if (matchesAny(normalizedName, categoryNameTokens)) {
            return RoleDetectionResult(
                suggestedRole = FieldRole.Category,
                suggestedUnit = null,
                confidence = DetectionConfidence.HIGH,
                reason = "Field name matches categorical dimension"
            )
        }
        if (matchesAny(normalizedName, labelNameTokens)) {
            return RoleDetectionResult(
                suggestedRole = FieldRole.Label,
                suggestedUnit = null,
                confidence = DetectionConfidence.HIGH,
                reason = "Field name matches descriptive annotation label"
            )
        }

        // 7. Measure vs Part-of-Measure
        val looksLikeTotal = matchesAny(normalizedName, totalMeasureTokens)
        val looksLikeCount = matchesAny(normalizedName, countMeasureTokens)
        if (looksLikeTotal) {
            return RoleDetectionResult(
                suggestedRole = FieldRole.Measure,
                suggestedUnit = unit.ifBlank { "patients" },
                confidence = DetectionConfidence.HIGH,
                reason = "Field name indicates an aggregate total / denominator count"
            )
        }
        if (looksLikeCount) {
            val candidateParent = existingFields.firstOrNull {
                it.role is FieldRole.Measure &&
                    it.name.lowercase(Locale.ENGLISH).contains("total")
            }
            return if (candidateParent != null && !normalizedName.contains("total")) {
                RoleDetectionResult(
                    suggestedRole = FieldRole.Measure,
                    suggestedUnit = unit.ifBlank { "patients" },
                    confidence = DetectionConfidence.HIGH,
                    reason = "Count field detected (can also be linked as Part of '${candidateParent.name}')"
                )
            } else {
                RoleDetectionResult(
                    suggestedRole = FieldRole.Measure,
                    suggestedUnit = unit.ifBlank { "count" },
                    confidence = DetectionConfidence.HIGH,
                    reason = "Field name indicates a whole-number count measure"
                )
            }
        }

        // 8. Sample-value fallback analysis
        if (nonBlankSamples.isNotEmpty()) {
            val numericSamples = nonBlankSamples.mapNotNull { it.replace(",", "").toDoubleOrNull() }
            if (numericSamples.size == nonBlankSamples.size) {
                val hasDecimals = numericSamples.any { it % 1.0 != 0.0 }
                val hasNegatives = numericSamples.any { it < 0.0 }
                return if (hasDecimals || hasNegatives) {
                    RoleDetectionResult(
                        suggestedRole = FieldRole.RateContinuous(allowNegativeValues = hasNegatives),
                        suggestedUnit = unit.ifBlank { null },
                        confidence = DetectionConfidence.MEDIUM,
                        reason = "Sample values contain decimal or signed numbers"
                    )
                } else {
                    RoleDetectionResult(
                        suggestedRole = FieldRole.Measure,
                        suggestedUnit = unit.ifBlank { "count" },
                        confidence = DetectionConfidence.MEDIUM,
                        reason = "Sample values are non-negative whole numbers"
                    )
                }
            }

            if (sampleGranularity != null) {
                return RoleDetectionResult(
                    suggestedRole = FieldRole.Time.fromGranularity(sampleGranularity),
                    suggestedUnit = null,
                    confidence = DetectionConfidence.MEDIUM,
                    reason = "Sample values match ${sampleGranularity.displayName} periods"
                )
            }

            val distinctCount = nonBlankSamples.distinct().size
            return if (distinctCount <= (nonBlankSamples.size * 0.7).coerceAtLeast(3.0)) {
                RoleDetectionResult(
                    suggestedRole = FieldRole.Category,
                    suggestedUnit = null,
                    confidence = DetectionConfidence.MEDIUM,
                    reason = "Sample values are repeated text categories"
                )
            } else {
                RoleDetectionResult(
                    suggestedRole = FieldRole.Label,
                    suggestedUnit = null,
                    confidence = DetectionConfidence.LOW,
                    reason = "Free-text values without numeric or period pattern"
                )
            }
        }

        // 9. Safe default when only a blank name is available
        return RoleDetectionResult(
            suggestedRole = FieldRole.Measure,
            suggestedUnit = unit.ifBlank { null },
            confidence = DetectionConfidence.LOW,
            reason = "Default aggregate count role — confirm or change as needed"
        )
    }

    /**
     * Suggests a role only if the user has not yet confirmed the field's role.
     */
    fun applySuggestionIfUnconfirmed(
        field: StatField,
        sampleValues: List<String> = emptyList(),
        existingFields: List<StatField> = emptyList()
    ): StatField {
        if (field.isRoleConfirmed) return field
        val detection = suggestRole(field.name, field.unit, sampleValues, existingFields)
        return field.copy(
            role = detection.suggestedRole,
            suggestedRole = detection.suggestedRole,
            unit = field.unit.ifBlank { detection.suggestedUnit.orEmpty() }
        )
    }

    private fun matchesAny(text: String, tokens: List<String>): Boolean =
        tokens.any { token -> text.contains(token) }
}
