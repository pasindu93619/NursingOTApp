package com.pasindu.nursingotapp.statgraph.engine.catalogue

import com.pasindu.nursingotapp.statgraph.engine.recommendation.AnalyticalTask

enum class ChartFamily(val displayName: String) {
    COMPARISON("Comparison"),
    TRENDS("Trends"),
    DISTRIBUTION("Distribution"),
    RELATIONSHIP("Relationship"),
    COMPOSITION("Composition"),
    STATISTICAL_SPECIALISED("Statistical & Specialised")
}

enum class PhoneReadabilityTier(
    val displayName: String,
    val defaultScore: Int
) {
    EXCELLENT("Excellent on phone", 92),
    GOOD("Good on phone", 78),
    MODERATE("Moderate (may need scroll/landscape)", 58),
    CHALLENGING("Compact phone layout constrained", 38)
}

/**
 * High-level field role requirements evaluated before ranking candidate charts.
 */
enum class RoleRequirement(val humanReadableRequirement: String) {
    TIME_DOMAIN("A Time field (Day, Week, Month, or Year)"),
    CATEGORY_OR_TIME_DOMAIN("A Category or Time domain field"),
    CATEGORY_DOMAIN("A discrete Category field"),
    NUMERIC_MEASURE_OR_RATE("At least one numeric Measure or Rate field"),
    TWO_NUMERIC_FIELDS("At least two numeric Measure or Rate fields"),
    THREE_NUMERIC_FIELDS("At least three numeric fields (X, Y, and bubble size)"),
    MULTIPLE_NUMERIC_OR_GROUP("Either multiple numeric measures or a Group/Series field"),
    PART_TO_WHOLE_STRUCTURE("A Part-of-Measure linked to a Total Measure (or compositional categories)"),
    ESTIMATE_WITH_LIMITS("An Estimate with Lower/Upper Limits field"),
    DURATION_WITH_EVENT("A Duration + Event/Censoring Flag field")
}

enum class OptionalRoleSupport {
    GROUP_SERIES,
    CATEGORY,
    DERIVED_RATE,
    ESTIMATE_LIMITS,
    LABEL
}

data class PhoneReadabilityProfile(
    val tier: PhoneReadabilityTier,
    val score: Int = tier.defaultScore,
    val notes: String
)

/**
 * Structured specification for a statistical chart type.
 *
 * Phase 2 describes chart requirements, perceptual accuracy, phone readability,
 * and misleading-design risks without claiming Phase 3 rendering support yet.
 */
data class ChartSpecification(
    val id: String,
    val canonicalName: String,
    val aliases: List<String>,
    val family: ChartFamily,
    val primaryTask: AnalyticalTask,
    val supportedTasks: Set<AnalyticalTask>,
    val requiredRoles: Set<RoleRequirement>,
    val optionalRoles: Set<OptionalRoleSupport> = emptySet(),
    val minObservations: Int,
    val maxRecommendedCategories: Int? = null,
    val supportedCharacteristics: List<String>,
    val knownLimitations: List<String>,
    val perceptualAccuracyRating: Int,
    val phoneReadability: PhoneReadabilityProfile,
    val misleadingDesignRisks: List<String>,
    val requiresNonNegativeValues: Boolean = false,
    val requiresPartToWhole: Boolean = false,
    val disallowsSequentialTimeComparison: Boolean = false,
    val renderingSupportedInToolkitYet: Boolean = false,
    val howToRead: String = "",
    val plainWhy: String = ""
)
