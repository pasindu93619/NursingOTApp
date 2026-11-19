package com.pasindu.nursingotapp.statgraph.engine.recommendation

/**
 * Analytical tasks supported by Stat Graph Studio's recommendation engine.
 * Kept distinct from requested chart names so task fit and chart form can be
 * evaluated independently.
 */
enum class AnalyticalTask(
    val displayName: String,
    val description: String
) {
    COMPARE_CATEGORIES("Compare categories", "Compare aggregate values across discrete categories."),
    SHOW_CHANGE_OVER_TIME("Show change over time", "Track trends or trajectories across ordered time periods."),
    SHOW_COMPOSITION("Show composition", "Show how component parts contribute to a whole total."),
    SHOW_DISTRIBUTION("Show distribution", "Examine spread, central tendency, skewness, and outliers."),
    SHOW_RELATIONSHIP("Show relationship", "Explore paired association between two continuous/count variables."),
    SHOW_CORRELATION("Show correlation", "Quantify and inspect linear association across numeric variables."),
    SHOW_RANKING("Show ranking", "Order categories from highest to lowest magnitude."),
    SHOW_UNCERTAINTY("Show uncertainty", "Display point estimates alongside lower and upper bounds."),
    COMPARE_PROPORTIONS("Compare proportions", "Compare percentage shares or normalized rates across periods or groups."),
    SHOW_CUMULATIVE_TOTALS("Show cumulative totals", "Track running cumulative totals or sequential increments."),
    SHOW_RATES("Show rates", "Analyze calculated rates, incidences per 1,000, or percentages."),
    COMPARE_GROUPS("Compare groups", "Compare metrics stratified across multiple cohorts or groups."),
    SHOW_SURVIVAL_OVER_TIME("Show survival over time", "Analyze time-to-event outcomes with censoring flags.")
}
