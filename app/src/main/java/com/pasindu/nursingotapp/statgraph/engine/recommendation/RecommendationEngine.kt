package com.pasindu.nursingotapp.statgraph.engine.recommendation

import com.pasindu.nursingotapp.statgraph.chart.toolkit.BarDensityPolicy
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartCatalogue
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartFamily
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartSpecification
import com.pasindu.nursingotapp.statgraph.engine.catalogue.RoleRequirement
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DataDiagnosticsEngine
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnostic
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DiagnosticCode
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DiagnosticSeverity
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.asDoubleOrNull
import com.pasindu.nursingotapp.statgraph.model.isMissingValue
import com.pasindu.nursingotapp.statgraph.stats.DerivedCalculationType
import com.pasindu.nursingotapp.statgraph.stats.StatisticsEngine
import kotlin.math.max
import kotlin.math.min

/**
 * Pure-Kotlin Intelligent Chart Recommendation Engine.
 *
 * Evaluates hard role and data constraints first (distinguishing [ChartVerdict.IMPOSSIBLE]
 * from [ChartVerdict.NOT_SUITABLE]), scores eligible candidates transparently using
 * [ScoringWeights], attaches statistical/clinical warnings from [DataDiagnosticsEngine],
 * and generates grounded plain-language explanations via [DatasetNarrativeExplainer].
 */
object RecommendationEngine {

    fun recommend(
        dataset: StatDataset,
        userIntentQuery: String? = null,
        weights: ScoringWeights = ScoringWeights()
    ): RecommendationSuiteResult {
        val normalizedWeights = weights.normalized()
        val diagnosticsReport = DataDiagnosticsEngine.analyze(dataset)
        val taskClassification = AnalyticalTaskClassifier.classify(dataset, userIntentQuery)
        val profile = buildDatasetProfile(dataset, diagnosticsReport)
        val derivedEvidence = DatasetNarrativeExplainer.buildDerivedEvidence(dataset)

        val preliminary = ChartCatalogue.allSpecifications.map { spec ->
            evaluateSpecification(
                dataset = dataset,
                spec = spec,
                profile = profile,
                taskClassification = taskClassification,
                diagnosticsReport = diagnosticsReport,
                weights = normalizedWeights
            )
        }

        val recommended = preliminary
            .filter { it.verdict == ChartVerdict.RECOMMENDED }
            .sortedByDescending { it.scoreBreakdown.finalScore }
        val valid = preliminary
            .filter { it.verdict == ChartVerdict.VALID }
            .sortedByDescending { it.scoreBreakdown.finalScore }
        val warning = preliminary
            .filter { it.verdict == ChartVerdict.VALID_WITH_WARNING }
            .sortedByDescending { it.scoreBreakdown.finalScore }

        val topAlternatives = (recommended + valid + warning)
            .take(3)
            .map { rec ->
                ChartAlternative(
                    specification = rec.specification,
                    verdict = rec.verdict,
                    score = rec.scoreBreakdown.finalScore,
                    reason = rec.summaryReason
                )
            }

        val notSuitable = preliminary
            .filter { it.verdict == ChartVerdict.NOT_SUITABLE }
            .sortedByDescending { it.scoreBreakdown.finalScore }
            .map { it.copy(suggestedAlternatives = topAlternatives.filter { alt -> alt.specification.id != it.specification.id }) }

        val impossible = preliminary
            .filter { it.verdict == ChartVerdict.IMPOSSIBLE }
            .map { it.copy(suggestedAlternatives = topAlternatives.filter { alt -> alt.specification.id != it.specification.id }) }

        val narrative = DatasetNarrativeExplainer.buildNarrative(
            dataset = dataset,
            taskClassification = taskClassification,
            diagnosticsReport = diagnosticsReport,
            topRecommendations = recommended.take(3),
            derivedEvidence = derivedEvidence
        )
        val simpleNarrative = DatasetNarrativeExplainer.buildSimpleNarrative(
            dataset = dataset,
            diagnosticsReport = diagnosticsReport,
            topRecommendations = recommended.take(3),
        )

        return RecommendationSuiteResult(
            datasetId = dataset.id,
            datasetName = dataset.name,
            taskClassification = taskClassification,
            diagnosticsReport = diagnosticsReport,
            recommendedCharts = recommended,
            validCharts = valid,
            warningCharts = warning,
            notSuitableCharts = notSuitable,
            impossibleCharts = impossible,
            derivedEvidenceSummary = derivedEvidence,
            plainLanguageNarrative = narrative,
            simpleNarrative = simpleNarrative,
        )
    }

    fun evaluateRequestedChart(
        dataset: StatDataset,
        chartQuery: String,
        userIntentQuery: String? = null,
        weights: ScoringWeights = ScoringWeights()
    ): ChartVerdictReport {
        val suite = recommend(dataset, userIntentQuery, weights)
        val matchResult = ChartAliasMatcher.match(
            rawQuery = chartQuery,
            fallbackTask = suite.taskClassification.primaryTask
        )

        val topAlts = (suite.recommendedCharts + suite.validCharts + suite.warningCharts)
            .take(3)
            .map { rec ->
                ChartAlternative(
                    specification = rec.specification,
                    verdict = rec.verdict,
                    score = rec.scoreBreakdown.finalScore,
                    reason = rec.summaryReason
                )
            }

        return when (matchResult) {
            is ChartMatchResult.Unknown -> {
                ChartVerdictReport(
                    query = chartQuery,
                    matchResult = matchResult,
                    verdict = ChartVerdict.IMPOSSIBLE,
                    evaluatedChart = null,
                    scoreBreakdown = null,
                    explanation = "Could not recognize chart request '$chartQuery'. ${matchResult.reason} " +
                        "Based on your dataset's primary task (${suite.taskClassification.primaryTask.displayName}), " +
                        "consider the recommended alternatives instead.",
                    warnings = emptyList(),
                    missingRequirements = listOf("A recognized chart type in the catalogue (query '$chartQuery' is unknown)"),
                    alternatives = topAlts,
                    relevantDiagnostics = suite.diagnosticsReport.diagnostics,
                    numericalEvidence = suite.derivedEvidenceSummary
                )
            }
            is ChartMatchResult.ExactOrAliasMatch -> {
                val evaluated = suite.allEvaluatedCharts.first { it.specification.id == matchResult.specification.id }
                val filteredAlts = topAlts.filter { it.specification.id != evaluated.specification.id }
                val aliasNote = if (matchResult.isFuzzyCorrection) {
                    "Interpreted '$chartQuery' as '${evaluated.specification.canonicalName}' (matched via '${matchResult.matchedTerm}'). "
                } else {
                    ""
                }
                ChartVerdictReport(
                    query = chartQuery,
                    matchResult = matchResult,
                    verdict = evaluated.verdict,
                    evaluatedChart = evaluated.specification,
                    scoreBreakdown = evaluated.scoreBreakdown,
                    explanation = "$aliasNote${evaluated.summaryReason}",
                    warnings = evaluated.warnings,
                    missingRequirements = evaluated.missingRequirements,
                    alternatives = filteredAlts,
                    relevantDiagnostics = filterRelevantDiagnostics(evaluated.specification, suite.diagnosticsReport),
                    numericalEvidence = suite.derivedEvidenceSummary
                )
            }
        }
    }

    private fun evaluateSpecification(
        dataset: StatDataset,
        spec: ChartSpecification,
        profile: DatasetFeatureProfile,
        taskClassification: TaskClassificationResult,
        diagnosticsReport: DatasetDiagnosticsReport,
        weights: ScoringWeights
    ): ChartRecommendation {
        val missingRequirements = mutableListOf<String>()
        val notSuitableReasons = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (profile.rowCount == 0) {
            missingRequirements.add("At least 1 observation row (dataset currently has 0 rows)")
        }

        // 1. Evaluate hard role requirements before ranking
        for (req in spec.requiredRoles) {
            when (req) {
                RoleRequirement.TIME_DOMAIN -> {
                    if (!profile.hasTimeDomain) {
                        missingRequirements.add(req.humanReadableRequirement)
                    }
                }
                RoleRequirement.CATEGORY_DOMAIN -> {
                    if (!profile.hasCategoryDomain) {
                        if (spec.disallowsSequentialTimeComparison && profile.hasTimeDomain) {
                            notSuitableReasons.add(
                                "${spec.canonicalName} is not suitable for sequential time periods (${profile.timeFieldNames.joinToString()}); " +
                                    "comparing slices across time hides trends and multi-period counts do not represent one static population whole."
                            )
                        } else {
                            missingRequirements.add(req.humanReadableRequirement)
                        }
                    }
                }
                RoleRequirement.CATEGORY_OR_TIME_DOMAIN -> {
                    if (!profile.hasCategoryDomain && !profile.hasTimeDomain) {
                        missingRequirements.add(req.humanReadableRequirement)
                    }
                }
                RoleRequirement.NUMERIC_MEASURE_OR_RATE -> {
                    if (profile.validNumericFields.isEmpty()) {
                        missingRequirements.add(req.humanReadableRequirement)
                    }
                }
                RoleRequirement.TWO_NUMERIC_FIELDS -> {
                    if (profile.validNumericFields.size < 2) {
                        missingRequirements.add(req.humanReadableRequirement)
                    }
                }
                RoleRequirement.THREE_NUMERIC_FIELDS -> {
                    if (profile.validNumericFields.size < 3) {
                        missingRequirements.add(req.humanReadableRequirement)
                    }
                }
                RoleRequirement.MULTIPLE_NUMERIC_OR_GROUP -> {
                    val hasMultiNumeric = profile.validNumericFields.size >= 2
                    val hasGroupWithNumeric = profile.hasGroupField && profile.validNumericFields.isNotEmpty()
                    if (!hasMultiNumeric && !hasGroupWithNumeric) {
                        missingRequirements.add(req.humanReadableRequirement)
                    }
                }
                RoleRequirement.PART_TO_WHOLE_STRUCTURE -> {
                    if (!profile.hasPartToWholeStructure) {
                        missingRequirements.add(req.humanReadableRequirement)
                    } else if (profile.hasInvalidPartRatio) {
                        missingRequirements.add("Valid Part-of-Measure values (part must not exceed total and denominator must exceed 0)")
                    }
                }
                RoleRequirement.ESTIMATE_WITH_LIMITS -> {
                    if (!profile.hasValidEstimateWithLimits) {
                        missingRequirements.add(req.humanReadableRequirement)
                    }
                }
                RoleRequirement.DURATION_WITH_EVENT -> {
                    if (!profile.hasValidDurationWithEvent) {
                        missingRequirements.add(req.humanReadableRequirement)
                    }
                }
            }
        }

        // 2. Hard numeric polarity check
        if (spec.requiresNonNegativeValues && profile.hasNegativeNumericValues) {
            missingRequirements.add(
                "${spec.canonicalName} requires non-negative values, but the dataset contains negative numbers."
            )
        }

        // 3. Observation minimum checks
        val effectiveObservations = max(profile.maxValidNumericCount, profile.rowCount)
        if (profile.rowCount > 0 && effectiveObservations < 2 && spec.minObservations >= 2) {
            missingRequirements.add("At least 2 valid observations (dataset has $effectiveObservations)")
        } else if (missingRequirements.isEmpty() && effectiveObservations < spec.minObservations) {
            if (spec.family == ChartFamily.DISTRIBUTION ||
                spec.family == ChartFamily.RELATIONSHIP ||
                spec.id == "control_chart" ||
                spec.minObservations - effectiveObservations >= 2
            ) {
                notSuitableReasons.add(
                    "${spec.canonicalName} requires at least ${spec.minObservations} observations for reliable statistical interpretation; " +
                        "this dataset provides only $effectiveObservations."
                )
            } else {
                warnings.add(
                    "Sparse series: ${spec.canonicalName} works best with >= ${spec.minObservations} observations (dataset has $effectiveObservations)."
                )
            }
        }

        // 4. Sequential time prohibition on pie/donut/etc.
        if (spec.disallowsSequentialTimeComparison && profile.hasTimeDomain && notSuitableReasons.isEmpty()) {
            notSuitableReasons.add(
                "${spec.canonicalName} is not suitable for sequential time series; angle/area slices obstruct trend comparison."
            )
        }

        // 5. Part-to-whole integrity for Pie/Donut/Treemap even when Category domain exists
        if (spec.requiresPartToWhole && profile.hasCategoryDomain && !profile.hasPartToWholeStructure && !profile.hasAllNonNegativeCounts) {
            notSuitableReasons.add(
                "${spec.canonicalName} requires non-negative counts forming a meaningful part-to-whole total."
            )
        }

        // 6. Category count limit
        val maxCat = spec.maxRecommendedCategories
        if (maxCat != null && profile.domainDistinctCount > maxCat) {
            if (spec.id == "pie_chart" || spec.id == "donut_chart") {
                notSuitableReasons.add(
                    "${spec.canonicalName} becomes illegible with ${profile.domainDistinctCount} slices (recommended <= $maxCat)."
                )
            } else {
                warnings.add(
                    "High cardinality: ${profile.domainDistinctCount} domain items exceeds comfortable phone limit ($maxCat) for ${spec.canonicalName}."
                )
            }
        }

        // 7. Special clinical safeguards: PartOfMeasure stacked double-counting hazard
        if (spec.id == "stacked_bar_chart" && profile.hasLinkedPartAndParentMeasure) {
            warnings.add(
                "Dataset includes both a Part-of-Measure and its Total parent field; stacking them directly would double-count the subset. " +
                    "Plot Part vs Remainder (Total - Part), or use a Grouped bar chart / 100% stacked bar chart."
            )
        }

        // 8. Statistical diagnostics warnings attached appropriately
        val diagnostics = diagnosticsReport.diagnostics
        for (diag in diagnostics) {
            when (diag.code) {
                DiagnosticCode.EXCESSIVE_MISSINGNESS -> {
                    warnings.add(diag.message)
                }
                DiagnosticCode.MISSING_VALUES -> {
                    if (spec.family == ChartFamily.TRENDS) {
                        warnings.add("Missing values present: keep gaps visible rather than interpolating or converting missing entries to 0.")
                    }
                }
                DiagnosticCode.IRREGULAR_TIME_INTERVALS -> {
                    if (spec.family == ChartFamily.TRENDS || spec.id == "control_chart") {
                        warnings.add(diag.message)
                    }
                }
                DiagnosticCode.DUPLICATE_PERIODS -> {
                    if (spec.family == ChartFamily.TRENDS || spec.id == "line_chart") {
                        warnings.add(diag.message)
                    }
                }
                DiagnosticCode.SMALL_COUNTS_BELOW_5 -> {
                    if (spec.requiresPartToWhole || spec.id == "line_chart" || spec.id == "multi_series_line_chart" ||
                        taskClassification.primaryTask == AnalyticalTask.SHOW_RATES ||
                        taskClassification.primaryTask == AnalyticalTask.COMPARE_PROPORTIONS
                    ) {
                        warnings.add(diag.message)
                    }
                }
                DiagnosticCode.EXTREME_OUTLIERS -> {
                    if (spec.family == ChartFamily.DISTRIBUTION || spec.family == ChartFamily.RELATIONSHIP) {
                        warnings.add(diag.message)
                    }
                }
                DiagnosticCode.SKEWED_DISTRIBUTION -> {
                    if (spec.id == "box_plot" || spec.id == "histogram" || spec.id == "density_plot") {
                        warnings.add("Skewed distribution noted; consider median/IQR summaries alongside raw counts.")
                    }
                }
                DiagnosticCode.CONSTANT_VALUES -> {
                    warnings.add(diag.message)
                }
                DiagnosticCode.MISSING_UNCERTAINTY_INFO -> {
                    if (spec.id == "error_bar_chart" || spec.id == "forest_plot" || spec.id == "range_plot") {
                        warnings.add(diag.message)
                    }
                }
                DiagnosticCode.INVALID_NUMERATOR_DENOMINATOR -> {
                    if (spec.requiresPartToWhole || spec.id == "grouped_bar_chart" || spec.id == "multi_series_line_chart") {
                        warnings.add(diag.message)
                    }
                }
                else -> Unit
            }
        }

        // Scale disparity note when comparing parent Total (e.g. 1200) and small Part (e.g. 45) on shared axis
        if (profile.scaleRatioBetweenNumericFields >= 8.0 &&
            spec.id in setOf(
                "multi_series_line_chart",
                "grouped_bar_chart",
                "slope_chart",
                "dumbbell_chart",
                "bullet_chart",
            )
        ) {
            warnings.add(
                "Large scale difference (${StatisticsEngine.formatNumber(profile.scaleRatioBetweenNumericFields, 1)}x) " +
                    "between measures can visually flatten the smaller series; pair count comparison with a derived percentage rate."
            )
        }

        val totalGroupedBars = profile.domainDistinctCount * profile.validNumericFields.size.coerceAtLeast(1)
        if (spec.id == "grouped_bar_chart" && totalGroupedBars > BarDensityPolicy.MAX_PHONE_GROUPED_BARS) {
            if (profile.hasTimeDomain) {
                val hasWeekField = dataset.fields.any { it.role is FieldRole.Time.Week }
                val periodText = if (hasWeekField) {
                    "${profile.domainDistinctCount} weeks"
                } else {
                    "${profile.domainDistinctCount} time periods ($totalGroupedBars bars)"
                }
                warnings.add("$periodText is a lot of bars; a line is easier to read")
            } else {
                warnings.add("$totalGroupedBars bars is a lot of bars for a phone screen; consider grouping into smaller blocks.")
            }
        }

        val distinctWarnings = warnings.distinct()

        // If IMPOSSIBLE -> short-circuit zero score
        if (missingRequirements.isNotEmpty()) {
            val zeroBreakdown = ScoreBreakdown(
                taskFitScore = 0.0,
                perceptualAccuracyScore = spec.perceptualAccuracyRating.toDouble(),
                dataFitScore = 0.0,
                phoneReadabilityScore = spec.phoneReadability.score.toDouble(),
                misleadingDesignPenalty = 0.0,
                finalScore = 0.0,
                componentNotes = listOf("Failed hard constraints: ${missingRequirements.joinToString("; ")}")
            )
            return ChartRecommendation(
                specification = spec,
                verdict = ChartVerdict.IMPOSSIBLE,
                scoreBreakdown = zeroBreakdown,
                summaryReason = "${spec.canonicalName} cannot be plotted with the current dataset: ${missingRequirements.joinToString("; ")}.",
                warnings = distinctWarnings,
                missingRequirements = missingRequirements.distinct()
            )
        }

        // Compute transparent weighted score
        val scoreBreakdown = computeScore(
            spec = spec,
            profile = profile,
            taskClassification = taskClassification,
            notSuitableReasons = notSuitableReasons,
            warnings = distinctWarnings,
            weights = weights
        )

        // Determine final verdict tier
        val seriousWarnings = distinctWarnings.filter { warning ->
            !warning.startsWith("Large scale difference") &&
                !warning.startsWith("Skewed distribution noted")
        }

        val verdict = when {
            notSuitableReasons.isNotEmpty() -> ChartVerdict.NOT_SUITABLE
            scoreBreakdown.finalScore < 45.0 -> ChartVerdict.NOT_SUITABLE
            seriousWarnings.isNotEmpty() -> ChartVerdict.VALID_WITH_WARNING
            isRecommendedCandidate(spec, taskClassification, scoreBreakdown.finalScore) -> ChartVerdict.RECOMMENDED
            else -> ChartVerdict.VALID
        }

        val reason = buildSummaryReason(spec, verdict, taskClassification, notSuitableReasons, distinctWarnings, scoreBreakdown)

        return ChartRecommendation(
            specification = spec,
            verdict = verdict,
            scoreBreakdown = scoreBreakdown,
            summaryReason = reason,
            warnings = distinctWarnings,
            missingRequirements = emptyList()
        )
    }

    private fun isRecommendedCandidate(
        spec: ChartSpecification,
        taskClassification: TaskClassificationResult,
        finalScore: Double
    ): Boolean {
        val matchesPrimary = spec.primaryTask == taskClassification.primaryTask ||
            taskClassification.primaryTask in spec.supportedTasks
        val matchesTopSecondary = spec.primaryTask in taskClassification.secondaryTasks.take(2)
        return finalScore >= 75.0 && (matchesPrimary || matchesTopSecondary)
    }

    private fun computeScore(
        spec: ChartSpecification,
        profile: DatasetFeatureProfile,
        taskClassification: TaskClassificationResult,
        notSuitableReasons: List<String>,
        warnings: List<String>,
        weights: ScoringWeights
    ): ScoreBreakdown {
        val notes = mutableListOf<String>()

        // 1. Task fit (0..100)
        val taskFitScore = when {
            spec.primaryTask == taskClassification.primaryTask -> {
                notes.add("Direct match for primary task '${taskClassification.primaryTask.displayName}'")
                100.0
            }
            taskClassification.primaryTask in spec.supportedTasks -> {
                notes.add("Supports primary task '${taskClassification.primaryTask.displayName}'")
                88.0
            }
            spec.primaryTask in taskClassification.secondaryTasks -> {
                notes.add("Matches secondary dataset task '${spec.primaryTask.displayName}'")
                82.0
            }
            taskClassification.secondaryTasks.any { it in spec.supportedTasks } -> {
                notes.add("Partially supports secondary analytical tasks")
                70.0
            }
            else -> {
                notes.add("Weak alignment with inferred task '${taskClassification.primaryTask.displayName}'")
                35.0
            }
        }

        // 2. Perceptual accuracy (0..100 from catalogue)
        val perceptualScore = spec.perceptualAccuracyRating.toDouble().coerceIn(0.0, 100.0)

        // 3. Data fit (0..100)
        var dataFit = 75.0
        if (profile.hasTimeDomain && spec.family == ChartFamily.TRENDS) dataFit += 15.0
        if (profile.hasCategoryDomain && spec.family == ChartFamily.COMPARISON) dataFit += 15.0
        if (profile.hasPartToWholeStructure && spec.id == "stacked_bar_100_chart") dataFit += 15.0
        if (profile.hasPartToWholeStructure && profile.hasTimeDomain &&
            (spec.id == "line_chart" || spec.id == "multi_series_line_chart" || spec.id == "grouped_bar_chart")
        ) {
            dataFit += 12.0
        }
        if (profile.hasValidDurationWithEvent && spec.id == "kaplan_meier_curve") dataFit = 98.0
        if (profile.hasValidEstimateWithLimits && (spec.id == "forest_plot" || spec.id == "error_bar_chart")) dataFit = 96.0
        if (profile.maxValidNumericCount >= spec.minObservations * 2) dataFit += 5.0
        if (spec.id == "slope_chart" && profile.hasTimeDomain && profile.domainDistinctCount > 2 && !profile.hasGroupField) {
            dataFit -= 15.0
        }
        dataFit -= (warnings.size * 7.0)
        dataFit = dataFit.coerceIn(10.0, 100.0)

        // 4. Phone readability (0..100)
        var phoneScore = spec.phoneReadability.score.toDouble()
        if (profile.domainDistinctCount > (spec.maxRecommendedCategories ?: Int.MAX_VALUE)) {
            phoneScore -= 18.0
        }
        val totalGroupedBars = profile.domainDistinctCount * profile.validNumericFields.size.coerceAtLeast(1)
        if (totalGroupedBars > BarDensityPolicy.MAX_PHONE_GROUPED_BARS) {
            if (spec.id == "grouped_bar_chart") {
                phoneScore -= 24.0
                notes.add("Dense grouped bar penalty: $totalGroupedBars bars exceeds phone threshold (${BarDensityPolicy.MAX_PHONE_GROUPED_BARS}).")
            } else if (profile.hasTimeDomain &&
                (spec.id == "line_chart" || spec.id == "multi_series_line_chart") &&
                com.pasindu.nursingotapp.statgraph.orchestration.ChartToolkitSupport.isSupported(spec.id)
            ) {
                phoneScore += 8.0
                notes.add("Line/area chart favoured over dense grouped bars for ${profile.domainDistinctCount} ordered time periods.")
            }
        }
        phoneScore = phoneScore.coerceIn(10.0, 100.0)

        // 5. Misleading design penalty (0..maxMisleadingPenalty)
        var penalty = 0.0
        if (notSuitableReasons.isNotEmpty()) penalty += 25.0
        if (spec.id == "stacked_bar_chart" && profile.hasLinkedPartAndParentMeasure) penalty += 15.0
        if (warnings.any { it.contains("small counts", ignoreCase = true) }) penalty += 6.0
        if (warnings.any { it.contains("Irregular", ignoreCase = true) }) penalty += 8.0
        if (warnings.any { it.contains("missing", ignoreCase = true) }) penalty += 6.0
        penalty = min(penalty, weights.maxMisleadingPenalty)

        val weightedRaw =
            (taskFitScore * weights.taskFitWeight) +
                (perceptualScore * weights.perceptualAccuracyWeight) +
                (dataFit * weights.dataFitWeight) +
                (phoneScore * weights.phoneReadabilityWeight) -
                penalty

        val finalScore = weightedRaw.coerceIn(0.0, 100.0)

        return ScoreBreakdown(
            taskFitScore = taskFitScore,
            perceptualAccuracyScore = perceptualScore,
            dataFitScore = dataFit,
            phoneReadabilityScore = phoneScore,
            misleadingDesignPenalty = penalty,
            finalScore = finalScore,
            componentNotes = notes
        )
    }

    private fun buildSummaryReason(
        spec: ChartSpecification,
        verdict: ChartVerdict,
        taskClassification: TaskClassificationResult,
        notSuitableReasons: List<String>,
        warnings: List<String>,
        scoreBreakdown: ScoreBreakdown
    ): String = when (verdict) {
        ChartVerdict.RECOMMENDED ->
            "${spec.canonicalName} is well-suited for ${taskClassification.primaryTask.displayName.lowercase()} " +
                "(overall score ${StatisticsEngine.formatNumber(scoreBreakdown.finalScore, 1)}/100; " +
                "perceptual accuracy ${spec.perceptualAccuracyRating}/100, phone readability '${spec.phoneReadability.tier.displayName}')."
        ChartVerdict.VALID ->
            "${spec.canonicalName} is structurally valid for this dataset " +
                "(score ${StatisticsEngine.formatNumber(scoreBreakdown.finalScore, 1)}/100) and supports ${spec.primaryTask.displayName.lowercase()}."
        ChartVerdict.VALID_WITH_WARNING ->
            "${spec.canonicalName} can plot this dataset, with statistical caution(s): ${warnings.joinToString(" ")}"
        ChartVerdict.NOT_SUITABLE ->
            notSuitableReasons.joinToString(" ").ifBlank {
                "${spec.canonicalName} scores poorly (${StatisticsEngine.formatNumber(scoreBreakdown.finalScore, 1)}/100) for ${taskClassification.primaryTask.displayName.lowercase()}."
            }
        ChartVerdict.IMPOSSIBLE ->
            "${spec.canonicalName} is impossible with this dataset."
    }

    private fun filterRelevantDiagnostics(
        spec: ChartSpecification,
        report: DatasetDiagnosticsReport
    ): List<DatasetDiagnostic> = report.diagnostics.filter { diag ->
        when (diag.code) {
            DiagnosticCode.IRREGULAR_TIME_INTERVALS, DiagnosticCode.DUPLICATE_PERIODS ->
                spec.family == ChartFamily.TRENDS || spec.disallowsSequentialTimeComparison
            DiagnosticCode.INVALID_NUMERATOR_DENOMINATOR ->
                spec.requiresPartToWhole || spec.id == "grouped_bar_chart" || spec.id == "multi_series_line_chart"
            DiagnosticCode.INAPPROPRIATE_NEGATIVE_VALUES -> spec.requiresNonNegativeValues
            else -> true
        }
    }

    private fun buildDatasetProfile(
        dataset: StatDataset,
        diagnosticsReport: DatasetDiagnosticsReport
    ): DatasetFeatureProfile {
        val timeFields = dataset.fields.filter { it.role is FieldRole.Time }
        val categoryFields = dataset.fields.filter { it.role is FieldRole.Category }
        val groupFields = dataset.fields.filter { it.role is FieldRole.Group }
        val numericFields = dataset.fields.filter { it.role.isNumeric }
        val partFields = dataset.fields.filter { it.role is FieldRole.PartOfMeasure }
        val estimateFields = dataset.fields.filter { it.role is FieldRole.EstimateWithLimits }
        val durationFields = dataset.fields.filter { it.role is FieldRole.DurationWithEvent }

        val validNumericFields = numericFields.filter { field ->
            (diagnosticsReport.fieldSummaries[field.id]?.validCount ?: 0) > 0
        }

        val validTimeCount = timeFields.maxOfOrNull { tf ->
            dataset.rows.count { row ->
                val v = row.values[tf.id]
                !v.isMissingValue() && (v !is StatValue.TimePeriodValue || v.period != null || v.rawText.isNotBlank())
            }
        } ?: 0

        val validCategoryCount = categoryFields.maxOfOrNull { cf ->
            dataset.rows.count { row -> !row.values[cf.id].isMissingValue() }
        } ?: 0

        val maxValidNumericCount = validNumericFields.maxOfOrNull {
            diagnosticsReport.fieldSummaries[it.id]?.validCount ?: 0
        } ?: 0

        val hasNegativeNumericValues = numericFields.any {
            (diagnosticsReport.fieldSummaries[it.id]?.negativeCount ?: 0) > 0
        }

        val hasAllNonNegativeCounts = validNumericFields.isNotEmpty() && !hasNegativeNumericValues

        val hasLinkedPart = partFields.any { pf ->
            val role = pf.role as FieldRole.PartOfMeasure
            role.parentFieldId != null && dataset.fields.any { it.id == role.parentFieldId }
        }

        val hasDerivedPartRate = dataset.derivedFields.any {
            it.calculation is DerivedCalculationType.RatePercentage ||
                it.calculation is DerivedCalculationType.PerThousand
        }

        val hasPartToWholeStructure = hasLinkedPart || hasDerivedPartRate ||
            (categoryFields.isNotEmpty() && partFields.isNotEmpty())

        val hasInvalidPartRatio = diagnosticsReport.diagnostics.any {
            it.code == DiagnosticCode.INVALID_NUMERATOR_DENOMINATOR &&
                it.severity == DiagnosticSeverity.BLOCKING
        }

        val hasValidEstimateWithLimits = estimateFields.any { ef ->
            dataset.rows.any { row ->
                val ev = row.values[ef.id] as? StatValue.EstimateValue
                ev != null && ev.estimate != null && ev.lowerLimit != null && ev.upperLimit != null
            }
        }

        val hasValidDurationWithEvent = durationFields.any { df ->
            dataset.rows.any { row ->
                val dv = row.values[df.id] as? StatValue.DurationEventValue
                dv != null && dv.duration != null && dv.duration >= 0.0 && dv.eventOccurred != null
            }
        }

        val domainDistinctCount = when {
            categoryFields.isNotEmpty() -> {
                categoryFields.maxOfOrNull { diagnosticsReport.fieldSummaries[it.id]?.distinctCount ?: 0 } ?: 0
            }
            timeFields.isNotEmpty() -> validTimeCount
            else -> dataset.rows.size
        }

        val maxPositiveMeans = validNumericFields
            .mapNotNull { diagnosticsReport.fieldSummaries[it.id]?.max?.takeIf { m -> m > 0.0 } }
        val scaleRatio = if (maxPositiveMeans.size >= 2) {
            maxPositiveMeans.max() / maxPositiveMeans.min()
        } else {
            1.0
        }

        return DatasetFeatureProfile(
            rowCount = dataset.rows.size,
            hasTimeDomain = timeFields.isNotEmpty() && validTimeCount > 0,
            timeFieldNames = timeFields.map { it.name },
            hasCategoryDomain = categoryFields.isNotEmpty() && validCategoryCount > 0,
            hasGroupField = groupFields.isNotEmpty(),
            validNumericFields = validNumericFields,
            maxValidNumericCount = maxValidNumericCount,
            hasNegativeNumericValues = hasNegativeNumericValues,
            hasAllNonNegativeCounts = hasAllNonNegativeCounts,
            hasLinkedPartAndParentMeasure = hasLinkedPart,
            hasPartToWholeStructure = hasPartToWholeStructure,
            hasInvalidPartRatio = hasInvalidPartRatio,
            hasValidEstimateWithLimits = hasValidEstimateWithLimits,
            hasValidDurationWithEvent = hasValidDurationWithEvent,
            domainDistinctCount = domainDistinctCount,
            scaleRatioBetweenNumericFields = scaleRatio
        )
    }

    private data class DatasetFeatureProfile(
        val rowCount: Int,
        val hasTimeDomain: Boolean,
        val timeFieldNames: List<String>,
        val hasCategoryDomain: Boolean,
        val hasGroupField: Boolean,
        val validNumericFields: List<StatField>,
        val maxValidNumericCount: Int,
        val hasNegativeNumericValues: Boolean,
        val hasAllNonNegativeCounts: Boolean,
        val hasLinkedPartAndParentMeasure: Boolean,
        val hasPartToWholeStructure: Boolean,
        val hasInvalidPartRatio: Boolean,
        val hasValidEstimateWithLimits: Boolean,
        val hasValidDurationWithEvent: Boolean,
        val domainDistinctCount: Int,
        val scaleRatioBetweenNumericFields: Double
    )
}
