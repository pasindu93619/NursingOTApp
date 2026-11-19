package com.pasindu.nursingotapp.statgraph.model

import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import java.util.UUID

/**
 * Type-safe representation of a single cell value inside a [StatRow].
 *
 * Missing values are represented either as `null` in [StatRow.values] or [Missing],
 * and are NEVER implicitly coerced to `0` or `0.0`.
 */
sealed class StatValue {
    data object Missing : StatValue()

    data class WholeNumber(val value: Long) : StatValue()

    data class DecimalNumber(val value: Double) : StatValue()

    data class TextValue(val value: String) : StatValue()

    data class TimePeriodValue(
        val period: TimePeriod?,
        val rawText: String
    ) : StatValue()

    data class EstimateValue(
        val estimate: Double? = null,
        val lowerLimit: Double? = null,
        val upperLimit: Double? = null
    ) : StatValue()

    data class DurationEventValue(
        val duration: Double? = null,
        val eventOccurred: Boolean? = null
    ) : StatValue()
}

/**
 * Returns true if this [StatValue] represents an absent/empty cell.
 * Zero (`0` / `0.0`) is explicitly NOT considered missing.
 */
fun StatValue?.isMissingValue(): Boolean = when (this) {
    null, StatValue.Missing -> true
    is StatValue.TextValue -> value.isBlank()
    is StatValue.TimePeriodValue -> period == null && rawText.isBlank()
    is StatValue.EstimateValue -> estimate == null && lowerLimit == null && upperLimit == null
    is StatValue.DurationEventValue -> duration == null && eventOccurred == null
    is StatValue.WholeNumber -> false
    is StatValue.DecimalNumber -> !value.isFinite()
}

/**
 * Extracts a numeric representation if available, returning `null` when missing.
 * Never returns `0.0` for missing entries.
 */
fun StatValue?.asDoubleOrNull(): Double? = when (this) {
    is StatValue.WholeNumber -> value.toDouble()
    is StatValue.DecimalNumber -> value.takeIf { it.isFinite() }
    is StatValue.EstimateValue -> estimate?.takeIf { it.isFinite() }
    is StatValue.DurationEventValue -> duration?.takeIf { it.isFinite() }
    else -> null
}

/**
 * Formats a [StatValue] for display, rendering missing values as an em-dash (`—`).
 */
fun StatValue?.toDisplayString(): String = when (this) {
    null, StatValue.Missing -> "—"
    is StatValue.WholeNumber -> value.toString()
    is StatValue.DecimalNumber -> {
        if (!value.isFinite()) "—"
        else if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(java.util.Locale.US, "%.2f", value)
    }
    is StatValue.TextValue -> value.ifBlank { "—" }
    is StatValue.TimePeriodValue -> period?.formatDisplay() ?: rawText.ifBlank { "—" }
    is StatValue.EstimateValue -> {
        val est = estimate?.let { formatCompactNumber(it) } ?: "—"
        val lo = lowerLimit?.let { formatCompactNumber(it) } ?: "—"
        val hi = upperLimit?.let { formatCompactNumber(it) } ?: "—"
        if (estimate == null && lowerLimit == null && upperLimit == null) "—"
        else "$est [$lo, $hi]"
    }
    is StatValue.DurationEventValue -> {
        val dur = duration?.let { formatCompactNumber(it) } ?: "—"
        val evt = when (eventOccurred) {
            true -> "Event"
            false -> "Censored"
            null -> "—"
        }
        if (duration == null && eventOccurred == null) "—"
        else "$dur ($evt)"
    }
}

private fun formatCompactNumber(value: Double): String =
    if (!value.isFinite()) "—"
    else if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(java.util.Locale.US, "%.2f", value)

/**
 * Definition of a single statistical column/variable within a [StatDataset].
 */
data class StatField(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val unit: String = "",
    val role: FieldRole,
    val suggestedRole: FieldRole? = null,
    val isRoleConfirmed: Boolean = true,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val isRequired: Boolean = false,
    val allowedCategories: List<String> = emptyList()
)

/**
 * A single aggregate observation row within a [StatDataset].
 *
 * [values] maps [StatField.id] -> [StatValue?]. Omitted keys or `null` represent
 * missing data and are never converted to zero.
 */
data class StatRow(
    val id: String = UUID.randomUUID().toString(),
    val periodOrCategoryLabel: String = "",
    val values: Map<String, StatValue?> = emptyMap()
)

/**
 * Audit metadata for a [StatDataset].
 */
data class DatasetMetadata(
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val sourceLabel: String = "Manual Entry"
)

/**
 * Top-level aggregate statistical dataset model for Stat Graph Studio.
 * Intentionally excludes patient-identifying records.
 */
data class StatDataset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val fields: List<StatField> = emptyList(),
    val rows: List<StatRow> = emptyList(),
    val derivedFields: List<DerivedFieldDefinition> = emptyList(),
    val metadata: DatasetMetadata = DatasetMetadata()
)
