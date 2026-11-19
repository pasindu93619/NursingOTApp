package com.pasindu.nursingotapp.statgraph.engine.recommendation

import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartCatalogue
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartSpecification
import java.util.Locale
import kotlin.math.min

sealed class ChartMatchResult {
    data class ExactOrAliasMatch(
        val specification: ChartSpecification,
        val matchedTerm: String,
        val isFuzzyCorrection: Boolean = false
    ) : ChartMatchResult()

    data class Unknown(
        val rawInput: String,
        val inferredTask: AnalyticalTask,
        val reason: String
    ) : ChartMatchResult()
}

/**
 * Pure-Kotlin chart name and alias matcher.
 *
 * Supports case-insensitive normalization, common synonyms, and minor typo tolerance,
 * while explicitly returning [ChartMatchResult.Unknown] for unrecognized chart names.
 */
object ChartAliasMatcher {

    private val extraSynonymMap: Map<String, String> = mapOf(
        "trend graph" to "line_chart",
        "trend line" to "line_chart",
        "line" to "line_chart",
        "bar graph" to "bar_chart",
        "bar" to "bar_chart",
        "circle chart" to "pie_chart",
        "pie" to "pie_chart",
        "donut" to "donut_chart",
        "frequency distribution" to "histogram",
        "correlation graph" to "scatter_plot",
        "scatter" to "scatter_plot",
        "percentage comparison" to "stacked_bar_100_chart",
        "proportion chart" to "stacked_bar_100_chart",
        "survival curve" to "kaplan_meier_curve",
        "kaplan meier curve" to "kaplan_meier_curve",
        "kaplan meier survival curve" to "kaplan_meier_curve",
        "error bar chart" to "error_bar_chart",
        "box and whisker plot" to "box_plot",
        "grouped bar" to "grouped_bar_chart",
        "stacked bar" to "stacked_bar_chart",
        "horizontal bar" to "horizontal_bar_chart",
        "slope" to "slope_chart",
        "bump" to "bump_chart",
        "sparkline" to "sparkline_small_multiples",
        "range band" to "range_band_chart",
        "connected scatter" to "connected_scatter_plot",
        "cycle plot" to "cycle_plot",
        "calendar heatmap" to "calendar_heatmap",
        "bullet" to "bullet_chart",
        "dumbbell" to "dumbbell_chart",
        "radar" to "radar_chart",
        "diverging bar" to "diverging_bar_chart",
        "likert" to "likert_stacked_diverging_chart",
        "likert chart" to "likert_stacked_diverging_chart",
        "waffle" to "waffle_chart",
        "pareto" to "pareto_chart",
        "ecdf" to "ecdf_chart",
        "qq plot" to "qq_plot",
        "q-q plot" to "qq_plot",
        "stem and leaf" to "stem_and_leaf_plot",
        "hexbin" to "hexbin_plot",
        "splom" to "scatterplot_matrix",
        "mosaic" to "mosaic_plot",
        "marimekko" to "mosaic_plot",
        "sunburst" to "sunburst_chart",
    )

    fun match(
        rawQuery: String,
        fallbackTask: AnalyticalTask = AnalyticalTask.COMPARE_CATEGORIES
    ): ChartMatchResult {
        val normalized = normalize(rawQuery)
        if (normalized.isEmpty()) {
            return ChartMatchResult.Unknown(
                rawInput = rawQuery,
                inferredTask = fallbackTask,
                reason = "Chart query was empty."
            )
        }

        // 1. Direct ID match
        ChartCatalogue.findById(normalized.replace(' ', '_'))?.let { spec ->
            return ChartMatchResult.ExactOrAliasMatch(spec, rawQuery.trim(), isFuzzyCorrection = false)
        }

        // 2. Canonical name & explicit aliases match
        for (spec in ChartCatalogue.allSpecifications) {
            if (normalize(spec.canonicalName) == normalized) {
                return ChartMatchResult.ExactOrAliasMatch(spec, spec.canonicalName, isFuzzyCorrection = false)
            }
            val aliasHit = spec.aliases.firstOrNull { normalize(it) == normalized }
            if (aliasHit != null) {
                return ChartMatchResult.ExactOrAliasMatch(spec, aliasHit, isFuzzyCorrection = false)
            }
        }

        // 3. Extra synonym dictionary
        extraSynonymMap[normalized]?.let { targetId ->
            ChartCatalogue.findById(targetId)?.let { spec ->
                return ChartMatchResult.ExactOrAliasMatch(spec, normalized, isFuzzyCorrection = false)
            }
        }

        // 4. Minor spelling tolerance (Levenshtein <= 2 on multi-character normalized terms)
        if (normalized.length >= 5) {
            var bestSpec: ChartSpecification? = null
            var bestTerm = ""
            var bestDist = Int.MAX_VALUE

            for (spec in ChartCatalogue.allSpecifications) {
                val terms = listOf(spec.canonicalName) + spec.aliases
                for (term in terms) {
                    val normTerm = normalize(term)
                    if (normTerm.length < 5) continue
                    val dist = levenshtein(normalized, normTerm)
                    val threshold = if (normTerm.length >= 8) 2 else 1
                    if (dist in 1..threshold && dist < bestDist) {
                        bestDist = dist
                        bestSpec = spec
                        bestTerm = term
                    }
                }
            }

            if (bestSpec != null) {
                return ChartMatchResult.ExactOrAliasMatch(
                    specification = bestSpec,
                    matchedTerm = bestTerm,
                    isFuzzyCorrection = true
                )
            }
        }

        return ChartMatchResult.Unknown(
            rawInput = rawQuery,
            inferredTask = inferTaskFromUnknownQuery(normalized, fallbackTask),
            reason = "'${rawQuery.trim()}' is not a recognized chart type in the Stat Graph Studio catalogue."
        )
    }

    private fun inferTaskFromUnknownQuery(
        normalized: String,
        fallback: AnalyticalTask
    ): AnalyticalTask = when {
        normalized.contains("trend") || normalized.contains("time") -> AnalyticalTask.SHOW_CHANGE_OVER_TIME
        normalized.contains("distribut") || normalized.contains("freq") -> AnalyticalTask.SHOW_DISTRIBUTION
        normalized.contains("correl") || normalized.contains("relation") -> AnalyticalTask.SHOW_CORRELATION
        normalized.contains("percent") || normalized.contains("proport") -> AnalyticalTask.COMPARE_PROPORTIONS
        normalized.contains("composit") || normalized.contains("share") -> AnalyticalTask.SHOW_COMPOSITION
        normalized.contains("surviv") -> AnalyticalTask.SHOW_SURVIVAL_OVER_TIME
        normalized.contains("uncertain") || normalized.contains("error") -> AnalyticalTask.SHOW_UNCERTAINTY
        normalized.contains("rank") -> AnalyticalTask.SHOW_RANKING
        else -> fallback
    }

    internal fun normalize(input: String): String = input
        .lowercase(Locale.ENGLISH)
        .replace(Regex("""[-–—_/]+"""), " ")
        .replace(Regex("""[^a-z0-9%\s]"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)

        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            System.arraycopy(curr, 0, prev, 0, b.length + 1)
        }
        return prev[b.length]
    }
}
