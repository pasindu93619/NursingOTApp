package com.pasindu.nursingotapp.statgraph.engine.recommendation

import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.stats.DerivedCalculationType
import java.util.Locale

data class TaskClassificationResult(
    val primaryTask: AnalyticalTask,
    val secondaryTasks: List<AnalyticalTask>,
    val source: TaskClassificationSource,
    val rationale: String
)

enum class TaskClassificationSource {
    USER_INTENT_TEXT,
    DATASET_SCHEMA_AND_DERIVED
}

/**
 * Classifies analytical intent from user text and/or dataset schema, keeping
 * analytical task distinct from any requested chart form.
 */
object AnalyticalTaskClassifier {

    private val keywordRules: List<Pair<AnalyticalTask, List<String>>> = listOf(
        AnalyticalTask.SHOW_SURVIVAL_OVER_TIME to listOf("survival", "kaplan", "censor", "time to event", "time-to-event"),
        AnalyticalTask.SHOW_UNCERTAINTY to listOf("uncertainty", "confidence interval", "error bar", "forest", "margin of error", "range", "limits"),
        AnalyticalTask.SHOW_CORRELATION to listOf("correlation", "correlate", "pearson", "r value", "linear association"),
        AnalyticalTask.SHOW_RELATIONSHIP to listOf("relationship", "association", "scatter", "versus", "vs"),
        AnalyticalTask.COMPARE_PROPORTIONS to listOf("compare proportion", "percentage comparison", "proportion", "share over", "share across", "percent share"),
        AnalyticalTask.SHOW_RATES to listOf("rate", "per 1000", "per 1,000", "incidence", "prevalence", "percentage"),
        AnalyticalTask.SHOW_CUMULATIVE_TOTALS to listOf("cumulative", "running total", "waterfall", "increment"),
        AnalyticalTask.SHOW_COMPOSITION to listOf("composition", "part to whole", "part-to-whole", "breakdown", "share of total", "pie", "donut", "treemap"),
        AnalyticalTask.SHOW_DISTRIBUTION to listOf("distribution", "frequency", "spread", "histogram", "boxplot", "box plot", "skew", "outlier", "quartile", "violin", "density"),
        AnalyticalTask.SHOW_CHANGE_OVER_TIME to listOf("over time", "trend", "monthly", "weekly", "daily", "yearly", "annual", "trajectory", "time series", "change"),
        AnalyticalTask.SHOW_RANKING to listOf("rank", "ranking", "top", "highest", "lowest", "sorted"),
        AnalyticalTask.COMPARE_GROUPS to listOf("compare group", "cohort", "side by side", "across group", "grouped", "pyramid", "stratified"),
        AnalyticalTask.COMPARE_CATEGORIES to listOf("compare", "comparison", "categories", "category", "ward", "difference")
    )

    fun classify(
        dataset: StatDataset,
        userIntentQuery: String? = null
    ): TaskClassificationResult {
        val cleaned = userIntentQuery?.trim()?.lowercase(Locale.ENGLISH).orEmpty()
        if (cleaned.isNotEmpty()) {
            val matched = keywordRules
                .filter { (_, tokens) -> tokens.any { token -> cleaned.contains(token) } }
                .map { it.first }
                .distinct()

            if (matched.isNotEmpty()) {
                return TaskClassificationResult(
                    primaryTask = matched.first(),
                    secondaryTasks = matched.drop(1),
                    source = TaskClassificationSource.USER_INTENT_TEXT,
                    rationale = "Matched user analytical intent '${matched.first().displayName}' from query."
                )
            }
        }

        return inferFromDataset(dataset)
    }

    fun inferFromDataset(dataset: StatDataset): TaskClassificationResult {
        val roles = dataset.fields.map { it.role }
        val hasTime = roles.any { it is FieldRole.Time }
        val hasCategory = roles.any { it is FieldRole.Category }
        val hasGroup = roles.any { it is FieldRole.Group }
        val hasPart = roles.any { it is FieldRole.PartOfMeasure }
        val hasEstimate = roles.any { it is FieldRole.EstimateWithLimits }
        val hasDuration = roles.any { it is FieldRole.DurationWithEvent }
        val numericCount = roles.count { it is FieldRole.Measure || it is FieldRole.PartOfMeasure || it is FieldRole.RateContinuous }
        val hasRateField = roles.any { it is FieldRole.RateContinuous }
        val hasDerivedRate = dataset.derivedFields.any {
            it.calculation is DerivedCalculationType.RatePercentage ||
                it.calculation is DerivedCalculationType.PerThousand
        }
        val hasDerivedCumulative = dataset.derivedFields.any {
            it.calculation is DerivedCalculationType.CumulativeTotal
        }

        val candidates = linkedSetOf<AnalyticalTask>()

        if (hasDuration) candidates.add(AnalyticalTask.SHOW_SURVIVAL_OVER_TIME)
        if (hasEstimate) candidates.add(AnalyticalTask.SHOW_UNCERTAINTY)
        if (hasTime && numericCount >= 1) candidates.add(AnalyticalTask.SHOW_CHANGE_OVER_TIME)
        if (hasTime && (numericCount >= 2 || hasGroup)) candidates.add(AnalyticalTask.COMPARE_GROUPS)
        if ((hasPart || hasDerivedRate || hasRateField) && hasTime) {
            candidates.add(AnalyticalTask.SHOW_RATES)
            candidates.add(AnalyticalTask.COMPARE_PROPORTIONS)
        }
        if (hasDerivedCumulative) candidates.add(AnalyticalTask.SHOW_CUMULATIVE_TOTALS)
        if (hasCategory && numericCount >= 1) {
            candidates.add(AnalyticalTask.COMPARE_CATEGORIES)
            if (hasPart) candidates.add(AnalyticalTask.SHOW_COMPOSITION)
        }
        if (!hasTime && !hasCategory && numericCount >= 2) {
            candidates.add(AnalyticalTask.SHOW_RELATIONSHIP)
            candidates.add(AnalyticalTask.SHOW_CORRELATION)
        }
        if (numericCount >= 1) {
            candidates.add(AnalyticalTask.SHOW_DISTRIBUTION)
        }
        if (candidates.isEmpty()) {
            candidates.add(AnalyticalTask.COMPARE_CATEGORIES)
        }

        val list = candidates.toList()
        return TaskClassificationResult(
            primaryTask = list.first(),
            secondaryTasks = list.drop(1),
            source = TaskClassificationSource.DATASET_SCHEMA_AND_DERIVED,
            rationale = "Inferred '${list.first().displayName}' from dataset roles (${dataset.fields.joinToString { it.role.shortLabel }})."
        )
    }
}
