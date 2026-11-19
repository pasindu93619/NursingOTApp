package com.pasindu.nursingotapp.statgraph.model

/**
 * Semantic chart axis / interpretation hint attached to every [FieldRole].
 * Used by both validation rules and the chart toolkit analyzer.
 */
enum class ChartAxisHint {
    DOMAIN_TIME,
    DOMAIN_CATEGORY,
    MEASURE_PRIMARY,
    MEASURE_COMPONENT,
    MEASURE_RATE,
    SERIES_GROUP,
    CONFIDENCE_INTERVAL,
    SURVIVAL_DURATION,
    ANNOTATION_LABEL
}

/**
 * Granularity options for [FieldRole.Time].
 */
enum class TimeGranularity(
    val displayName: String,
    val formatHint: String
) {
    DAY("Day", "yyyy-MM-dd"),
    WEEK("Week", "yyyy-Www"),
    MONTH("Month", "yyyy-MM"),
    YEAR("Year", "yyyy")
}

/**
 * Type-safe statistical role system for dataset fields.
 *
 * Every role defines validation semantics (e.g., whole-number vs decimal, negativity
 * allowance) and chart interpretation metadata.
 */
sealed class FieldRole(
    val displayName: String,
    val shortLabel: String,
    val plainLabel: String,
    val plainHint: String,
    val example: String,
    val isNumeric: Boolean,
    val allowsNegative: Boolean,
    val requiresWholeNumber: Boolean,
    val chartAxisHint: ChartAxisHint,
    val supportsAggregation: Boolean
) {
    /**
     * Temporal axis role with explicit granularity.
     */
    sealed class Time(
        val granularity: TimeGranularity
    ) : FieldRole(
        displayName = "Time (${granularity.displayName})",
        shortLabel = granularity.displayName,
        plainLabel = "Date or month",
        plainHint = "When each measurement was recorded (${granularity.displayName.lowercase()})",
        example = "e.g., Jan 2026, Week 1, 2026-03-01",
        isNumeric = false,
        allowsNegative = false,
        requiresWholeNumber = false,
        chartAxisHint = ChartAxisHint.DOMAIN_TIME,
        supportsAggregation = false
    ) {
        data object Day : Time(TimeGranularity.DAY)
        data object Week : Time(TimeGranularity.WEEK)
        data object Month : Time(TimeGranularity.MONTH)
        data object Year : Time(TimeGranularity.YEAR)

        companion object {
            fun fromGranularity(granularity: TimeGranularity): Time = when (granularity) {
                TimeGranularity.DAY -> Day
                TimeGranularity.WEEK -> Week
                TimeGranularity.MONTH -> Month
                TimeGranularity.YEAR -> Year
            }
        }
    }

    /**
     * Discrete categorical variable (e.g., Ward, Gender, District).
     */
    data object Category : FieldRole(
        displayName = "Category",
        shortLabel = "Category",
        plainLabel = "Name or group",
        plainHint = "A ward, department, or category name",
        example = "e.g., Medical, Surgical, ICU",
        isNumeric = false,
        allowsNegative = false,
        requiresWholeNumber = false,
        chartAxisHint = ChartAxisHint.DOMAIN_CATEGORY,
        supportsAggregation = false
    )

    /**
     * Primary aggregate count or total measure (whole numbers, non-negative).
     */
    data object Measure : FieldRole(
        displayName = "Count / total number",
        shortLabel = "Count",
        plainLabel = "A count (e.g. total patients)",
        plainHint = "A whole-number count or total",
        example = "e.g., Total patients = 120",
        isNumeric = true,
        allowsNegative = false,
        requiresWholeNumber = true,
        chartAxisHint = ChartAxisHint.MEASURE_PRIMARY,
        supportsAggregation = true
    )

    /**
     * Subset count belonging to an associated total [Measure] field.
     */
    data class PartOfMeasure(
        val parentFieldId: String? = null
    ) : FieldRole(
        displayName = "Part of a total",
        shortLabel = "Part",
        plainLabel = "Part of a total",
        plainHint = "A smaller count inside a larger total",
        example = "e.g., Dengue patients out of all patients",
        isNumeric = true,
        allowsNegative = false,
        requiresWholeNumber = true,
        chartAxisHint = ChartAxisHint.MEASURE_COMPONENT,
        supportsAggregation = true
    )

    /**
     * Continuous measurement or calculated/observed rate (decimals allowed).
     */
    data class RateContinuous(
        val allowNegativeValues: Boolean = false,
        val scaleHint: String? = null
    ) : FieldRole(
        displayName = "Percentage or average",
        shortLabel = "Rate / Avg",
        plainLabel = "Percentage or average",
        plainHint = "A percentage, rate, or decimal measurement",
        example = "e.g., 7.5%, 37.8 °C, 4.2 days",
        isNumeric = true,
        allowsNegative = allowNegativeValues,
        requiresWholeNumber = false,
        chartAxisHint = ChartAxisHint.MEASURE_RATE,
        supportsAggregation = true
    )

    /**
     * Stratification or series-splitting dimension (e.g., Age Group, Shift Type).
     */
    data object Group : FieldRole(
        displayName = "Group / Series",
        shortLabel = "Group",
        plainLabel = "Name or group (split series)",
        plainHint = "Splits the chart into separate lines or bars",
        example = "e.g., Day shift vs Night shift",
        isNumeric = false,
        allowsNegative = false,
        requiresWholeNumber = false,
        chartAxisHint = ChartAxisHint.SERIES_GROUP,
        supportsAggregation = false
    )

    /**
     * Point estimate paired with lower and upper confidence/uncertainty bounds.
     */
    data class EstimateWithLimits(
        val lowerLabel: String = "Lower Limit",
        val upperLabel: String = "Upper Limit"
    ) : FieldRole(
        displayName = "Value with range ±",
        shortLabel = "Value ± range",
        plainLabel = "Value with range ±",
        plainHint = "An estimate paired with lower and upper limits",
        example = "e.g., 12.5% (9.1 to 16.4)",
        isNumeric = true,
        allowsNegative = true,
        requiresWholeNumber = false,
        chartAxisHint = ChartAxisHint.CONFIDENCE_INTERVAL,
        supportsAggregation = false
    )

    /**
     * Follow-up or stay duration paired with an outcome/censoring event flag.
     */
    data class DurationWithEvent(
        val eventLabel: String = "Event Occurred"
    ) : FieldRole(
        displayName = "Time until event",
        shortLabel = "Duration",
        plainLabel = "Time until event",
        plainHint = "Follow-up duration paired with an outcome flag",
        example = "e.g., 14 days, discharged = yes",
        isNumeric = true,
        allowsNegative = false,
        requiresWholeNumber = false,
        chartAxisHint = ChartAxisHint.SURVIVAL_DURATION,
        supportsAggregation = false
    )

    /**
     * Aggregate-safe descriptive label or annotation (never for patient identifiers).
     */
    data object Label : FieldRole(
        displayName = "Note",
        shortLabel = "Note",
        plainLabel = "Note",
        plainHint = "Short text note (never patient names or IDs)",
        example = "e.g., Holiday week, Ward renovation",
        isNumeric = false,
        allowsNegative = false,
        requiresWholeNumber = false,
        chartAxisHint = ChartAxisHint.ANNOTATION_LABEL,
        supportsAggregation = false
    )

    companion object {
        /**
         * Returns the canonical list of selectable role presets for UI pickers.
         */
        fun selectablePresets(defaultParentFieldId: String? = null): List<FieldRole> = listOf(
            Time.Day,
            Time.Week,
            Time.Month,
            Time.Year,
            Category,
            Measure,
            PartOfMeasure(parentFieldId = defaultParentFieldId),
            RateContinuous(),
            Group,
            EstimateWithLimits(),
            DurationWithEvent(),
            Label
        )
    }
}
