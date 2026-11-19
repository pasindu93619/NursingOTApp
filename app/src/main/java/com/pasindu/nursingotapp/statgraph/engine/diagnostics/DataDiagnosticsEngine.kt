package com.pasindu.nursingotapp.statgraph.engine.diagnostics

import com.pasindu.nursingotapp.statgraph.engine.ValidationEngine
import com.pasindu.nursingotapp.statgraph.engine.ValidationReport
import com.pasindu.nursingotapp.statgraph.engine.ValidationSeverity
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.model.asDoubleOrNull
import com.pasindu.nursingotapp.statgraph.model.isMissingValue
import com.pasindu.nursingotapp.statgraph.stats.FieldDescriptiveSummary
import com.pasindu.nursingotapp.statgraph.stats.StatisticsEngine
import kotlin.math.abs

enum class DiagnosticSeverity {
    BLOCKING,
    WARNING,
    INFO
}

enum class DiagnosticCode {
    MISSING_VALUES,
    EXCESSIVE_MISSINGNESS,
    DUPLICATE_PERIODS,
    IRREGULAR_TIME_INTERVALS,
    INSUFFICIENT_OBSERVATIONS,
    CONSTANT_VALUES,
    EXTREME_OUTLIERS,
    INAPPROPRIATE_NEGATIVE_VALUES,
    SKEWED_DISTRIBUTION,
    TOO_MANY_CATEGORIES,
    SMALL_COUNTS_BELOW_5,
    INCOMPATIBLE_FIELD_COMBINATIONS,
    INVALID_NUMERATOR_DENOMINATOR,
    MISSING_UNCERTAINTY_INFO,
    INAPPROPRIATE_GROUPING
}

data class DatasetDiagnostic(
    val id: String,
    val code: DiagnosticCode,
    val severity: DiagnosticSeverity,
    val affectedFieldIds: List<String> = emptyList(),
    val affectedRowIds: List<String> = emptyList(),
    val message: String,
    val suggestedCorrection: String,
    val evidence: Map<String, String> = emptyMap()
)

data class DatasetDiagnosticsReport(
    val diagnostics: List<DatasetDiagnostic>,
    val validationReport: ValidationReport,
    val fieldSummaries: Map<String, FieldDescriptiveSummary>
) {
    val blockingDiagnostics: List<DatasetDiagnostic>
        get() = diagnostics.filter { it.severity == DiagnosticSeverity.BLOCKING }

    val warningDiagnostics: List<DatasetDiagnostic>
        get() = diagnostics.filter { it.severity == DiagnosticSeverity.WARNING }

    val infoDiagnostics: List<DatasetDiagnostic>
        get() = diagnostics.filter { it.severity == DiagnosticSeverity.INFO }

    val hasBlockingIssues: Boolean
        get() = blockingDiagnostics.isNotEmpty() || validationReport.hasBlockingErrors

    fun diagnosticsForField(fieldId: String): List<DatasetDiagnostic> =
        diagnostics.filter { fieldId in it.affectedFieldIds }
}

/**
 * Pure-Kotlin automatic dataset diagnostics engine.
 *
 * Reuses [ValidationEngine] and [StatisticsEngine] without mutating user data.
 */
object DataDiagnosticsEngine {

    private const val EXCESSIVE_MISSING_RATIO = 0.30
    private const val CATEGORY_WARNING_LIMIT = 12
    private const val PIE_CATEGORY_INFO_LIMIT = 6

    fun analyze(dataset: StatDataset): DatasetDiagnosticsReport {
        val validationReport = ValidationEngine.validate(dataset)
        val fieldSummaries = dataset.fields.associate { field ->
            field.id to StatisticsEngine.summarizeField(dataset, field)
        }
        val diagnostics = mutableListOf<DatasetDiagnostic>()

        // 1. Insufficient observations / empty schema
        checkObservationSufficiency(dataset, diagnostics)

        // 2. Incompatible field combinations
        checkFieldCompatibility(dataset, diagnostics)

        // 3. Map core ValidationEngine issues into structured diagnostics
        mapValidationIssues(validationReport, diagnostics)

        // 4. Per-field statistical & structural diagnostics
        dataset.fields.forEach { field ->
            val summary = fieldSummaries.getValue(field.id)
            checkMissingness(dataset, field, summary, diagnostics)
            checkNumericDistribution(dataset, field, summary, diagnostics)
            checkCategoriesAndGrouping(dataset, field, summary, diagnostics)
            checkNumeratorDenominator(dataset, field, diagnostics)
            checkUncertaintyCompleteness(dataset, field, diagnostics)
        }

        // 5. Irregular time intervals beyond missing consecutive gaps
        dataset.fields
            .filter { it.role is FieldRole.Time }
            .forEach { timeField ->
                checkTimeIntervals(dataset, timeField, diagnostics)
            }

        // 6. Missing uncertainty suggestion when rate/continuous data exists with no bounds field
        val hasRateOrMeasure = dataset.fields.any {
            it.role is FieldRole.RateContinuous || it.role is FieldRole.Measure
        }
        val hasEstimateField = dataset.fields.any { it.role is FieldRole.EstimateWithLimits }
        if (hasRateOrMeasure && !hasEstimateField && dataset.rows.isNotEmpty()) {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_no_uncertainty_field",
                    code = DiagnosticCode.MISSING_UNCERTAINTY_INFO,
                    severity = DiagnosticSeverity.INFO,
                    affectedFieldIds = dataset.fields.filter {
                        it.role is FieldRole.RateContinuous || it.role is FieldRole.Measure
                    }.map { it.id },
                    message = "No confidence/uncertainty limits field is present in this dataset.",
                    suggestedCorrection = "Add an Estimate with Limits field if you want error bars, range plots, or forest plots."
                )
            )
        }

        return DatasetDiagnosticsReport(
            diagnostics = diagnostics.distinctBy { it.id },
            validationReport = validationReport,
            fieldSummaries = fieldSummaries
        )
    }

    private fun checkObservationSufficiency(
        dataset: StatDataset,
        diagnostics: MutableList<DatasetDiagnostic>
    ) {
        if (dataset.fields.isEmpty()) {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_no_fields",
                    code = DiagnosticCode.INSUFFICIENT_OBSERVATIONS,
                    severity = DiagnosticSeverity.BLOCKING,
                    message = "No fields are defined in the dataset schema.",
                    suggestedCorrection = "Define at least one domain/measure field before charting."
                )
            )
            return
        }

        when {
            dataset.rows.isEmpty() -> diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_zero_rows",
                    code = DiagnosticCode.INSUFFICIENT_OBSERVATIONS,
                    severity = DiagnosticSeverity.BLOCKING,
                    message = "Dataset has 0 observations.",
                    suggestedCorrection = "Add at least 1 observation row (3+ recommended for trends or distributions)."
                )
            )
            dataset.rows.size < 3 -> diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_few_rows",
                    code = DiagnosticCode.INSUFFICIENT_OBSERVATIONS,
                    severity = DiagnosticSeverity.WARNING,
                    affectedRowIds = dataset.rows.map { it.id },
                    message = "Only ${dataset.rows.size} observation(s) present; trend, distribution, and correlation analyses are unreliable.",
                    suggestedCorrection = "Add at least ${3 - dataset.rows.size} more observation(s) for meaningful pattern detection.",
                    evidence = mapOf("rowCount" to dataset.rows.size.toString())
                )
            )
        }
    }

    private fun checkFieldCompatibility(
        dataset: StatDataset,
        diagnostics: MutableList<DatasetDiagnostic>
    ) {
        if (dataset.fields.isEmpty()) return

        val nonLabelFields = dataset.fields.filterNot { it.role is FieldRole.Label }
        if (nonLabelFields.isEmpty()) {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_label_only",
                    code = DiagnosticCode.INCOMPATIBLE_FIELD_COMBINATIONS,
                    severity = DiagnosticSeverity.BLOCKING,
                    affectedFieldIds = dataset.fields.map { it.id },
                    message = "Dataset contains only Label/Note fields with no numeric or categorical variables.",
                    suggestedCorrection = "Add at least one Measure, Rate, Time, or Category field."
                )
            )
        }

        val hasNumeric = dataset.fields.any { it.role.isNumeric }
        if (!hasNumeric && dataset.fields.isNotEmpty()) {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_no_numeric_fields",
                    code = DiagnosticCode.INCOMPATIBLE_FIELD_COMBINATIONS,
                    severity = DiagnosticSeverity.WARNING,
                    affectedFieldIds = dataset.fields.map { it.id },
                    message = "No numeric Measure, Rate, Estimate, or Duration fields exist.",
                    suggestedCorrection = "Add a Measure or Rate/Continuous field to plot quantitative comparisons or trends."
                )
            )
        }

        dataset.fields.filter { it.role is FieldRole.PartOfMeasure }.forEach { partField ->
            val role = partField.role as FieldRole.PartOfMeasure
            val parent = role.parentFieldId?.let { id -> dataset.fields.firstOrNull { it.id == id } }
            if (role.parentFieldId != null && (parent == null || parent.role !is FieldRole.Measure)) {
                diagnostics.add(
                    DatasetDiagnostic(
                        id = "diag_invalid_parent_${partField.id}",
                        code = DiagnosticCode.INCOMPATIBLE_FIELD_COMBINATIONS,
                        severity = DiagnosticSeverity.BLOCKING,
                        affectedFieldIds = listOf(partField.id),
                        message = "'${partField.name}' is marked Part of Measure, but its linked total field is missing or not a Measure.",
                        suggestedCorrection = "Link '${partField.name}' to a valid total Measure field or change its role to Measure."
                    )
                )
            }
        }
    }

    private fun mapValidationIssues(
        report: ValidationReport,
        diagnostics: MutableList<DatasetDiagnostic>
    ) {
        val grouped = report.issues.groupBy { it.ruleCode }

        grouped["PART_EXCEEDS_TOTAL"]?.let { list ->
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_part_exceeds_total",
                    code = DiagnosticCode.INVALID_NUMERATOR_DENOMINATOR,
                    severity = DiagnosticSeverity.BLOCKING,
                    affectedFieldIds = list.mapNotNull { it.fieldId }.distinct(),
                    affectedRowIds = list.mapNotNull { it.rowId }.distinct(),
                    message = "${list.size} row(s) have a part-of-measure count exceeding its linked total measure.",
                    suggestedCorrection = "Verify numerator and total denominator counts; a subset cannot exceed 100% of the total."
                )
            )
        }

        val negIssues = (grouped["NEGATIVE_COUNT"].orEmpty() +
            grouped["NEGATIVE_VALUE"].orEmpty() +
            grouped["NEGATIVE_DURATION"].orEmpty())
        if (negIssues.isNotEmpty()) {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_negative_values",
                    code = DiagnosticCode.INAPPROPRIATE_NEGATIVE_VALUES,
                    severity = DiagnosticSeverity.BLOCKING,
                    affectedFieldIds = negIssues.mapNotNull { it.fieldId }.distinct(),
                    affectedRowIds = negIssues.mapNotNull { it.rowId }.distinct(),
                    message = "${negIssues.size} cell(s) contain negative values in non-negative count/rate/duration fields.",
                    suggestedCorrection = "Correct negative entries or enable signed continuous values if negative changes are intentional."
                )
            )
        }

        grouped["DUPLICATE_TIME_PERIOD"]?.let { list ->
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_duplicate_periods",
                    code = DiagnosticCode.DUPLICATE_PERIODS,
                    severity = DiagnosticSeverity.WARNING,
                    affectedFieldIds = list.mapNotNull { it.fieldId }.distinct(),
                    affectedRowIds = list.mapNotNull { it.rowId }.distinct(),
                    message = "Duplicate time periods detected (${list.mapNotNull { it.rowIndex }.distinct().size} row occurrences).",
                    suggestedCorrection = "Aggregate rows per period or assign a Group/Category field to stratify repeated periods."
                )
            )
        }

        grouped["PRIVACY_SMALL_COUNT"]?.let { list ->
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_small_counts",
                    code = DiagnosticCode.SMALL_COUNTS_BELOW_5,
                    severity = DiagnosticSeverity.WARNING,
                    affectedFieldIds = list.mapNotNull { it.fieldId }.distinct(),
                    affectedRowIds = list.mapNotNull { it.rowId }.distinct(),
                    message = "${list.size} aggregate count cell(s) are below 5 (1–4), posing small-cell re-identification risk.",
                    suggestedCorrection = "Consider pooling periods/categories or applying small-cell suppression before sharing charts."
                )
            )
        }
    }

    private fun checkMissingness(
        dataset: StatDataset,
        field: StatField,
        summary: FieldDescriptiveSummary,
        diagnostics: MutableList<DatasetDiagnostic>
    ) {
        if (dataset.rows.isEmpty() || summary.missingCount == 0) return

        val missingRows = dataset.rows.filter { it.values[field.id].isMissingValue() }.map { it.id }
        val ratio = summary.missingCount.toDouble() / dataset.rows.size.toDouble()
        val pct = (ratio * 100.0).toInt()

        if (ratio >= EXCESSIVE_MISSING_RATIO && dataset.rows.size >= 2) {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_excess_missing_${field.id}",
                    code = DiagnosticCode.EXCESSIVE_MISSINGNESS,
                    severity = DiagnosticSeverity.WARNING,
                    affectedFieldIds = listOf(field.id),
                    affectedRowIds = missingRows,
                    message = "'${field.name}' is missing $pct% of values (${summary.missingCount}/${dataset.rows.size} rows).",
                    suggestedCorrection = "Verify missing records or limit visualizations to complete observations (never substitute 0).",
                    evidence = mapOf("missingPercent" to "$pct%", "missingCount" to "${summary.missingCount}")
                )
            )
        } else {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_missing_${field.id}",
                    code = DiagnosticCode.MISSING_VALUES,
                    severity = DiagnosticSeverity.INFO,
                    affectedFieldIds = listOf(field.id),
                    affectedRowIds = missingRows,
                    message = "'${field.name}' has ${summary.missingCount} missing value(s), preserved as gaps rather than zero.",
                    suggestedCorrection = "Charts should display gaps cleanly without interpolating missing counts as 0.",
                    evidence = mapOf("missingCount" to "${summary.missingCount}")
                )
            )
        }
    }

    private fun checkNumericDistribution(
        dataset: StatDataset,
        field: StatField,
        summary: FieldDescriptiveSummary,
        diagnostics: MutableList<DatasetDiagnostic>
    ) {
        if (!field.role.isNumeric || summary.validCount < 2) return

        // Constant values
        if (summary.range != null && summary.range == 0.0) {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_constant_${field.id}",
                    code = DiagnosticCode.CONSTANT_VALUES,
                    severity = DiagnosticSeverity.WARNING,
                    affectedFieldIds = listOf(field.id),
                    message = "'${field.name}' has constant value ${StatisticsEngine.formatNumber(summary.min)} across all ${summary.validCount} observations.",
                    suggestedCorrection = "Verify data entry; constant series cannot show trends, correlations, or spread.",
                    evidence = mapOf("constantValue" to StatisticsEngine.formatNumber(summary.min))
                )
            )
            return
        }

        // Extreme outliers (Tukey 3.0 * IQR fence)
        val quartiles = summary.quartiles
        if (summary.validCount >= 4 && quartiles != null && quartiles.iqr > 0.0) {
            val lowerFence = quartiles.q1 - (3.0 * quartiles.iqr)
            val upperFence = quartiles.q3 + (3.0 * quartiles.iqr)
            val outlierRows = dataset.rows.filter { row ->
                val v = row.values[field.id].asDoubleOrNull()
                v != null && (v < lowerFence || v > upperFence)
            }
            if (outlierRows.isNotEmpty()) {
                diagnostics.add(
                    DatasetDiagnostic(
                        id = "diag_outliers_${field.id}",
                        code = DiagnosticCode.EXTREME_OUTLIERS,
                        severity = DiagnosticSeverity.WARNING,
                        affectedFieldIds = listOf(field.id),
                        affectedRowIds = outlierRows.map { it.id },
                        message = "'${field.name}' contains ${outlierRows.size} extreme outlier(s) beyond 3×IQR (${StatisticsEngine.formatNumber(lowerFence)} to ${StatisticsEngine.formatNumber(upperFence)}).",
                        suggestedCorrection = "Check for unit/entry errors or prefer box/strip plots and median/IQR summaries.",
                        evidence = mapOf("outlierCount" to "${outlierRows.size}", "iqr" to StatisticsEngine.formatNumber(quartiles.iqr))
                    )
                )
            }
        }

        // Skewed distribution
        val skew = summary.skewness
        if (summary.validCount >= 4 && skew != null && abs(skew) >= 1.0) {
            val direction = if (skew > 0) "right-skewed" else "left-skewed"
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_skew_${field.id}",
                    code = DiagnosticCode.SKEWED_DISTRIBUTION,
                    severity = DiagnosticSeverity.INFO,
                    affectedFieldIds = listOf(field.id),
                    message = "'${field.name}' shows a $direction distribution (skewness = ${StatisticsEngine.formatNumber(skew)}).",
                    suggestedCorrection = "Prefer median/IQR and box, violin, or strip plots over mean-only summaries.",
                    evidence = mapOf("skewness" to StatisticsEngine.formatNumber(skew))
                )
            )
        }
    }

    private fun checkCategoriesAndGrouping(
        dataset: StatDataset,
        field: StatField,
        summary: FieldDescriptiveSummary,
        diagnostics: MutableList<DatasetDiagnostic>
    ) {
        if (field.role is FieldRole.Category) {
            if (summary.distinctCount > CATEGORY_WARNING_LIMIT) {
                diagnostics.add(
                    DatasetDiagnostic(
                        id = "diag_many_cats_${field.id}",
                        code = DiagnosticCode.TOO_MANY_CATEGORIES,
                        severity = DiagnosticSeverity.WARNING,
                        affectedFieldIds = listOf(field.id),
                        message = "'${field.name}' has ${summary.distinctCount} distinct categories, reducing readability on mobile charts.",
                        suggestedCorrection = "Use a horizontal bar, lollipop, or dot plot, or group minor categories into 'Other'.",
                        evidence = mapOf("distinctCount" to "${summary.distinctCount}")
                    )
                )
            } else if (summary.distinctCount > PIE_CATEGORY_INFO_LIMIT) {
                diagnostics.add(
                    DatasetDiagnostic(
                        id = "diag_pie_cats_${field.id}",
                        code = DiagnosticCode.TOO_MANY_CATEGORIES,
                        severity = DiagnosticSeverity.INFO,
                        affectedFieldIds = listOf(field.id),
                        message = "'${field.name}' has ${summary.distinctCount} categories (> $PIE_CATEGORY_INFO_LIMIT), which crowds pie/donut slices.",
                        suggestedCorrection = "Prefer a horizontal bar or 100% stacked bar over pie/donut charts.",
                        evidence = mapOf("distinctCount" to "${summary.distinctCount}")
                    )
                )
            }
        }

        if (field.role is FieldRole.Group) {
            val nonMissingGroupRows = dataset.rows.count { !it.values[field.id].isMissingValue() }
            if (nonMissingGroupRows >= 2 && summary.distinctCount <= 1) {
                diagnostics.add(
                    DatasetDiagnostic(
                        id = "diag_single_group_${field.id}",
                        code = DiagnosticCode.INAPPROPRIATE_GROUPING,
                        severity = DiagnosticSeverity.WARNING,
                        affectedFieldIds = listOf(field.id),
                        message = "Group field '${field.name}' has only ${summary.distinctCount} unique value; grouping adds no separation.",
                        suggestedCorrection = "Add another group level or change '${field.name}' role to Label."
                    )
                )
            } else if (nonMissingGroupRows >= 4 && summary.distinctCount >= nonMissingGroupRows) {
                diagnostics.add(
                    DatasetDiagnostic(
                        id = "diag_unique_group_${field.id}",
                        code = DiagnosticCode.INAPPROPRIATE_GROUPING,
                        severity = DiagnosticSeverity.WARNING,
                        affectedFieldIds = listOf(field.id),
                        message = "Group field '${field.name}' has a unique value on every row (${summary.distinctCount}/$nonMissingGroupRows), creating excessive fragmented series.",
                        suggestedCorrection = "Group by broader strata (e.g., Ward or Age Band) or switch role to Category/Label."
                    )
                )
            }
        }
    }

    private fun checkNumeratorDenominator(
        dataset: StatDataset,
        field: StatField,
        diagnostics: MutableList<DatasetDiagnostic>
    ) {
        val role = field.role
        if (role is FieldRole.PartOfMeasure && role.parentFieldId == null) {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_part_unlinked_${field.id}",
                    code = DiagnosticCode.INVALID_NUMERATOR_DENOMINATOR,
                    severity = DiagnosticSeverity.WARNING,
                    affectedFieldIds = listOf(field.id),
                    message = "'${field.name}' is a Part-of-Measure without a linked total denominator field.",
                    suggestedCorrection = "Link '${field.name}' to its total Measure field to enable share validation and proportion charts."
                )
            )
        }

        // Check if this field is used as denominator in any derived field and contains 0
        val usedAsDenominator = dataset.derivedFields.any { def ->
            when (val calc = def.calculation) {
                is com.pasindu.nursingotapp.statgraph.stats.DerivedCalculationType.RatePercentage ->
                    calc.denominatorFieldId == field.id
                is com.pasindu.nursingotapp.statgraph.stats.DerivedCalculationType.PerThousand ->
                    calc.denominatorFieldId == field.id
                else -> false
            }
        }
        if (usedAsDenominator) {
            val zeroRows = dataset.rows.filter { row ->
                row.values[field.id].asDoubleOrNull() == 0.0
            }
            if (zeroRows.isNotEmpty()) {
                diagnostics.add(
                    DatasetDiagnostic(
                        id = "diag_zero_denom_${field.id}",
                        code = DiagnosticCode.INVALID_NUMERATOR_DENOMINATOR,
                        severity = DiagnosticSeverity.WARNING,
                        affectedFieldIds = listOf(field.id),
                        affectedRowIds = zeroRows.map { it.id },
                        message = "Denominator '${field.name}' is zero in ${zeroRows.size} row(s), making derived rates undefined.",
                        suggestedCorrection = "Review zero-denominator rows; derived rates will remain Undefined (÷0)."
                    )
                )
            }
        }
    }

    private fun checkUncertaintyCompleteness(
        dataset: StatDataset,
        field: StatField,
        diagnostics: MutableList<DatasetDiagnostic>
    ) {
        if (field.role !is FieldRole.EstimateWithLimits || dataset.rows.isEmpty()) return
        val rowsMissingBounds = dataset.rows.filter { row ->
            val v = row.values[field.id] as? StatValue.EstimateValue
            v != null && (v.lowerLimit == null || v.upperLimit == null)
        }
        if (rowsMissingBounds.isNotEmpty()) {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_missing_bounds_${field.id}",
                    code = DiagnosticCode.MISSING_UNCERTAINTY_INFO,
                    severity = DiagnosticSeverity.WARNING,
                    affectedFieldIds = listOf(field.id),
                    affectedRowIds = rowsMissingBounds.map { it.id },
                    message = "${rowsMissingBounds.size} row(s) in '${field.name}' are missing lower or upper uncertainty limits.",
                    suggestedCorrection = "Provide both lower and upper confidence bounds to render error bars or forest plots."
                )
            )
        }
    }

    private fun checkTimeIntervals(
        dataset: StatDataset,
        timeField: StatField,
        diagnostics: MutableList<DatasetDiagnostic>
    ) {
        val sortedPeriods = dataset.rows.mapNotNull { row ->
            (row.values[timeField.id] as? StatValue.TimePeriodValue)?.period
        }.distinctBy { it.sortKey() }.sortedBy { it.sortKey() }

        if (sortedPeriods.size < 2) return

        var hasGaps = false
        for (i in 0 until sortedPeriods.lastIndex) {
            if (sortedPeriods[i].nextPeriod().sortKey() != sortedPeriods[i + 1].sortKey()) {
                hasGaps = true
                break
            }
        }

        if (hasGaps) {
            diagnostics.add(
                DatasetDiagnostic(
                    id = "diag_irregular_time_${timeField.id}",
                    code = DiagnosticCode.IRREGULAR_TIME_INTERVALS,
                    severity = DiagnosticSeverity.WARNING,
                    affectedFieldIds = listOf(timeField.id),
                    message = "'${timeField.name}' has irregular time spacing or skipped periods across ${sortedPeriods.size} distinct periods.",
                    suggestedCorrection = "Keep missing periods explicit or avoid connecting across large missing time gaps.",
                    evidence = mapOf("distinctPeriods" to "${sortedPeriods.size}")
                )
            )
        }
    }
}
