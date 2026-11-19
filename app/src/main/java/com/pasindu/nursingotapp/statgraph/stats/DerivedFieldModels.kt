package com.pasindu.nursingotapp.statgraph.stats

import java.util.UUID

/**
 * Supported reusable derived statistical calculations.
 * Derived fields never overwrite original row values.
 */
sealed class DerivedCalculationType {
    abstract val label: String
    abstract val defaultUnit: String

    /**
     * Percentage: (numerator / denominator) * 100.
     * Supports either another dataset field or a fixed constant denominator.
     */
    data class RatePercentage(
        val numeratorFieldId: String,
        val denominatorFieldId: String? = null,
        val constantDenominator: Double? = null
    ) : DerivedCalculationType() {
        override val label: String = "Rate (%)"
        override val defaultUnit: String = "%"
    }

    /**
     * Incidence/proportion per 1,000: (numerator / denominator) * 1000.
     */
    data class PerThousand(
        val numeratorFieldId: String,
        val denominatorFieldId: String? = null,
        val constantDenominator: Double? = null
    ) : DerivedCalculationType() {
        override val label: String = "Per 1,000"
        override val defaultUnit: String = "per 1,000"
    }

    /**
     * Trailing moving average over a configurable window size.
     */
    data class MovingAverage(
        val sourceFieldId: String,
        val windowSize: Int = 3,
        val requireFullWindow: Boolean = false
    ) : DerivedCalculationType() {
        override val label: String = "Moving Avg ($windowSize)"
        override val defaultUnit: String = ""
    }

    /**
     * Running cumulative total across ordered rows, optionally reset per group.
     */
    data class CumulativeTotal(
        val sourceFieldId: String,
        val groupByFieldId: String? = null
    ) : DerivedCalculationType() {
        override val label: String = "Cumulative Total"
        override val defaultUnit: String = ""
    }
}

/**
 * Definition of a non-destructive derived column computed from existing fields.
 */
data class DerivedFieldDefinition(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val unit: String,
    val calculation: DerivedCalculationType,
    val formulaSummary: String = "",
    val decimalPlaces: Int = 2
)

/**
 * Explicit outcome state for a derived cell calculation.
 */
enum class DerivedValueStatus {
    VALID,
    MISSING_INPUT,
    DIVISION_BY_ZERO,
    INSUFFICIENT_WINDOW
}

/**
 * Computed derived value for a single row, preserving calculation metadata and safe formatting.
 */
data class DerivedCellResult(
    val rowId: String,
    val derivedFieldId: String,
    val numericValue: Double?,
    val formattedValue: String,
    val unit: String,
    val status: DerivedValueStatus,
    val calculationMetadata: String
)
