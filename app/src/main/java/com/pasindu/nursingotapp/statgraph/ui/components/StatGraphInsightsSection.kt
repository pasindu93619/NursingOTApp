package com.pasindu.nursingotapp.statgraph.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.R
import com.pasindu.nursingotapp.statgraph.engine.ValidationIssue
import com.pasindu.nursingotapp.statgraph.engine.ValidationSeverity
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnostic
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DiagnosticCode
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DiagnosticSeverity
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.stats.DatasetSummaryMetrics
import com.pasindu.nursingotapp.statgraph.stats.DerivedCellResult
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldCalculator
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import com.pasindu.nursingotapp.statgraph.stats.DerivedValueStatus
import com.pasindu.nursingotapp.statgraph.stats.FieldDescriptiveSummary
import com.pasindu.nursingotapp.statgraph.stats.StatisticsEngine
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphWorkspaceTokens

/**
 * Displays grounded overview metrics, per-field descriptive statistics, derived calculations,
 * and plain-language diagnostic & privacy alerts (preserving missing values and small-count safety).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatGraphInsightsSection(
    dataset: StatDataset,
    summaryMetrics: DatasetSummaryMetrics,
    diagnosticsReport: DatasetDiagnosticsReport,
    derivedResults: Map<String, List<DerivedCellResult>>,
    narrativeSummary: String,
    compactMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val primaryNumericField = remember(dataset.fields, diagnosticsReport.fieldSummaries) {
        dataset.fields.firstOrNull { field ->
            field.role.isNumeric && (diagnosticsReport.fieldSummaries[field.id]?.validCount ?: 0) > 0
        }
    }
    val primarySummary = primaryNumericField?.let { diagnosticsReport.fieldSummaries[it.id] }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Overview KPIs Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = StatGraphDesignTokens.Shapes.extraLarge,
            color = StatGraphWorkspaceTokens.Colors.surface,
            border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Analytics,
                        contentDescription = null,
                        tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Statistical Overview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatGraphWorkspaceTokens.Colors.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                }

                val columns = if (compactMode) 2 else 3
                val kpiItems = buildList {
                    add(OverviewKpiItem("Observations", "${summaryMetrics.rowCount}", "${summaryMetrics.fieldCount} variable(s)"))
                    add(
                        OverviewKpiItem(
                            "Completeness",
                            "${summaryMetrics.completenessPercent}%",
                            if (summaryMetrics.missingCellCount > 0) "${summaryMetrics.missingCellCount} missing kept" else "No missing cells",
                        ),
                    )
                    add(
                        OverviewKpiItem(
                            "Diagnostics",
                            "${diagnosticsReport.warningDiagnostics.size}W · ${diagnosticsReport.blockingDiagnostics.size}B",
                            if (summaryMetrics.smallCountCellCount > 0) "${summaryMetrics.smallCountCellCount} small-count alert(s)" else "Privacy check clear",
                        ),
                    )
                    if (primaryNumericField != null && primarySummary != null) {
                        val unit = if (primaryNumericField.unit.isNotBlank()) " ${primaryNumericField.unit}" else ""
                        add(
                            OverviewKpiItem(
                                "Mean (${primaryNumericField.name})",
                                StatisticsEngine.formatNumber(primarySummary.mean) + unit,
                                "Median ${StatisticsEngine.formatNumber(primarySummary.median)}$unit",
                            ),
                        )
                        add(
                            OverviewKpiItem(
                                "Range (${primaryNumericField.name})",
                                "${StatisticsEngine.formatNumber(primarySummary.min)} – ${StatisticsEngine.formatNumber(primarySummary.max)}",
                                "n=${primarySummary.validCount} valid",
                            ),
                        )
                    }
                }

                kpiItems.chunked(columns).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowItems.forEach { kpi ->
                            OverviewKpiTile(item = kpi, modifier = Modifier.weight(1f))
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (narrativeSummary.isNotBlank()) {
                    Surface(
                        shape = StatGraphDesignTokens.Shapes.medium,
                        color = StatGraphWorkspaceTokens.Colors.brandIndigoSoft,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = narrativeSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
        }

        // 2. Derived Percentages / Calculations Breakdown
        if (dataset.derivedFields.isNotEmpty()) {
            DerivedCalculationsCard(
                dataset = dataset,
                derivedFields = dataset.derivedFields,
                derivedResults = derivedResults,
            )
        }

        // 3. Per-Variable Descriptive Statistics
        if (dataset.fields.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = StatGraphDesignTokens.Shapes.extraLarge,
                color = StatGraphWorkspaceTokens.Colors.surface,
                border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Variable Summaries",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatGraphWorkspaceTokens.Colors.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    dataset.fields.forEachIndexed { idx, field ->
                        val summary = diagnosticsReport.fieldSummaries[field.id]
                        if (summary != null) {
                            FieldDescriptiveSummaryRow(field = field, summary = summary)
                            if (idx < dataset.fields.lastIndex) {
                                HorizontalDivider(color = StatGraphWorkspaceTokens.Colors.borderSubtle)
                            }
                        }
                    }
                }
            }
        }

        // 4. Plain-Language Diagnostics & Privacy Audit
        DiagnosticsAndPrivacyCard(
            diagnosticsReport = diagnosticsReport,
            smallCountCellCount = summaryMetrics.smallCountCellCount,
        )
    }
}

private data class OverviewKpiItem(
    val label: String,
    val value: String,
    val caption: String,
)

@Composable
private fun OverviewKpiTile(
    item: OverviewKpiItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = StatGraphDesignTokens.Shapes.large,
        color = StatGraphWorkspaceTokens.Colors.surfaceMuted,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )
            Text(
                text = item.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = StatGraphWorkspaceTokens.Colors.ink,
            )
            Text(
                text = item.caption,
                style = MaterialTheme.typography.labelSmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DerivedCalculationsCard(
    dataset: StatDataset,
    derivedFields: List<DerivedFieldDefinition>,
    derivedResults: Map<String, List<DerivedCellResult>>,
) {
    val rowById = remember(dataset.rows) { dataset.rows.associateBy { it.id } }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StatGraphDesignTokens.Shapes.extraLarge,
        color = StatGraphWorkspaceTokens.Colors.surface,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Functions,
                    contentDescription = null,
                    tint = StatGraphWorkspaceTokens.Colors.accentOrange,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.statgraph_heading_calculated_values),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatGraphWorkspaceTokens.Colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
            }

            derivedFields.forEach { def ->
                val cells = derivedResults[def.id].orEmpty()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${def.name} (${def.formulaSummary})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                    )
                    if (cells.isEmpty()) {
                        Text(
                            text = "No observations available yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphWorkspaceTokens.Colors.slate,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            cells.forEach { cell ->
                                val rowLabel = rowById[cell.rowId]?.periodOrCategoryLabel?.ifBlank { cell.rowId } ?: cell.rowId
                                val chipBg = if (cell.status == DerivedValueStatus.VALID) {
                                    StatGraphWorkspaceTokens.Colors.brandIndigoSoft
                                } else {
                                    StatGraphWorkspaceTokens.Colors.softAmber
                                }
                                val chipFg = if (cell.status == DerivedValueStatus.VALID) {
                                    StatGraphWorkspaceTokens.Colors.brandIndigo
                                } else {
                                    StatGraphWorkspaceTokens.Colors.onSoftAmber
                                }
                                Surface(
                                    shape = StatGraphDesignTokens.Shapes.medium,
                                    color = chipBg,
                                ) {
                                    Text(
                                        text = "$rowLabel: ${cell.formattedValue}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = chipFg,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldDescriptiveSummaryRow(
    field: StatField,
    summary: FieldDescriptiveSummary,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = buildString {
                    append(field.name)
                    if (field.unit.isNotBlank()) append(" (${field.unit})")
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = StatGraphWorkspaceTokens.Colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = field.role.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )
        }

        if (field.role.isNumeric) {
            val parts = buildList {
                add("Valid: ${summary.validCount}/${summary.totalRows}")
                if (summary.missingCount > 0) add("Missing: ${summary.missingCount}")
                summary.mean?.let { add("Mean: ${StatisticsEngine.formatNumber(it)}") }
                summary.median?.let { add("Median: ${StatisticsEngine.formatNumber(it)}") }
                if (summary.min != null && summary.max != null) {
                    add("Min–Max: ${StatisticsEngine.formatNumber(summary.min)}–${StatisticsEngine.formatNumber(summary.max)}")
                }
                summary.sampleStdDev?.let { add("SD: ${StatisticsEngine.formatNumber(it)}") }
                summary.quartiles?.let { add("IQR: ${StatisticsEngine.formatNumber(it.iqr)}") }
            }
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )
        } else {
            val missingTxt = if (summary.missingCount > 0) " · Missing: ${summary.missingCount}" else ""
            Text(
                text = "Present: ${summary.validCount}/${summary.totalRows} · Distinct values: ${summary.distinctCount}$missingTxt",
                style = MaterialTheme.typography.bodySmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )
        }
    }
}

@Composable
private fun DiagnosticsAndPrivacyCard(
    diagnosticsReport: DatasetDiagnosticsReport,
    smallCountCellCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StatGraphDesignTokens.Shapes.extraLarge,
        color = StatGraphWorkspaceTokens.Colors.surface,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            var showMissingHelp by remember { mutableStateOf(false) }
            if (showMissingHelp) {
                StatGraphContextualHelpDialog(
                    topic = ContextualHelpTopic.MISSING_VALUES,
                    dataset = null,
                    onDismiss = { showMissingHelp = false },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.HealthAndSafety,
                        contentDescription = null,
                        tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.statgraph_heading_data_checks_privacy),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatGraphWorkspaceTokens.Colors.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                ContextualHelpButton(onClick = { showMissingHelp = true })
            }

            if (smallCountCellCount > 0) {
                Surface(
                    shape = StatGraphDesignTokens.Shapes.large,
                    color = StatGraphWorkspaceTokens.Colors.softRose,
                    border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PrivacyTip,
                            contentDescription = "Small count privacy notice",
                            tint = StatGraphWorkspaceTokens.Colors.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Privacy Safeguard: $smallCountCellCount cell(s) have counts between 1 and 4 (<5). Avoid sharing small-cell tables externally where individuals could be inferred; never include patient identifiers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphWorkspaceTokens.Colors.onSoftRose,
                        )
                    }
                }
            }

            if (diagnosticsReport.diagnostics.isEmpty() && diagnosticsReport.validationReport.isClean) {
                Surface(
                    shape = StatGraphDesignTokens.Shapes.medium,
                    color = StatGraphWorkspaceTokens.Colors.softMint,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "All statistical integrity, denominator, time-sequence, and aggregate privacy checks passed with no warnings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphWorkspaceTokens.Colors.onSoftMint,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            } else {
                diagnosticsReport.diagnostics.forEach { diag ->
                    DiagnosticItemCard(diag = diag)
                }
                // Include any validation issues without a matching diagnostic summary
                val extraValidation = diagnosticsReport.validationReport.issues.filter { issue ->
                    diagnosticsReport.diagnostics.none { d -> d.message == issue.message }
                }.take(5)
                extraValidation.forEach { issue ->
                    ValidationAuditRow(issue = issue)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItemCard(diag: DatasetDiagnostic) {
    val palette = StatGraphWorkspaceTokens.severityPalette(diag.severity)
    val icon = when (diag.severity) {
        DiagnosticSeverity.BLOCKING -> Icons.Rounded.ErrorOutline
        DiagnosticSeverity.WARNING -> Icons.Rounded.WarningAmber
        DiagnosticSeverity.INFO -> Icons.Rounded.Info
    }

    Surface(
        shape = StatGraphDesignTokens.Shapes.medium,
        color = palette.container,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.content,
                modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = humanReadableDiagnosticTitle(diag.code, diag.severity),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.content,
                )
                Text(
                    text = diag.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphWorkspaceTokens.Colors.ink,
                )
                if (diag.suggestedCorrection.isNotBlank()) {
                    Text(
                        text = "Guidance: ${diag.suggestedCorrection}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatGraphWorkspaceTokens.Colors.slate,
                    )
                }
            }
        }
    }
}

@Composable
private fun ValidationAuditRow(issue: ValidationIssue) {
    val isError = issue.severity == ValidationSeverity.ERROR
    val palette = StatGraphWorkspaceTokens.severityPalette(
        if (isError) DiagnosticSeverity.BLOCKING else DiagnosticSeverity.WARNING,
    )
    Surface(
        shape = StatGraphDesignTokens.Shapes.medium,
        color = palette.container,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "${if (isError) "Validation Error" else "Validation Notice"}: ${issue.message}",
            style = MaterialTheme.typography.bodySmall,
            color = palette.content,
            modifier = Modifier.padding(10.dp),
        )
    }
}

/**
 * Maps internal [DiagnosticCode] values to clear, user-facing clinical headings
 * without exposing raw enum names.
 */
fun humanReadableDiagnosticTitle(
    code: DiagnosticCode,
    severity: DiagnosticSeverity,
): String {
    val base = when (code) {
        DiagnosticCode.MISSING_VALUES -> "Missing Values Preserved"
        DiagnosticCode.EXCESSIVE_MISSINGNESS -> "High Missingness Proportion"
        DiagnosticCode.DUPLICATE_PERIODS -> "Duplicate Time Periods"
        DiagnosticCode.IRREGULAR_TIME_INTERVALS -> "Irregular Time Intervals"
        DiagnosticCode.INSUFFICIENT_OBSERVATIONS -> "Sample Size Notice"
        DiagnosticCode.CONSTANT_VALUES -> "Constant Series Detected"
        DiagnosticCode.EXTREME_OUTLIERS -> "Potential Extreme Outliers"
        DiagnosticCode.INAPPROPRIATE_NEGATIVE_VALUES -> "Negative Value Check"
        DiagnosticCode.SKEWED_DISTRIBUTION -> "Skewed Distribution"
        DiagnosticCode.TOO_MANY_CATEGORIES -> "High Category Count"
        DiagnosticCode.SMALL_COUNTS_BELOW_5 -> "Small-Cell Privacy Notice (<5)"
        DiagnosticCode.INCOMPATIBLE_FIELD_COMBINATIONS -> "Incompatible Field Combination"
        DiagnosticCode.INVALID_NUMERATOR_DENOMINATOR -> "Numerator / Denominator Check"
        DiagnosticCode.MISSING_UNCERTAINTY_INFO -> "Uncertainty Bounds Availability"
        DiagnosticCode.INAPPROPRIATE_GROUPING -> "Grouping Cardinality Check"
    }
    return when (severity) {
        DiagnosticSeverity.BLOCKING -> "Blocking: $base"
        DiagnosticSeverity.WARNING -> "Warning: $base"
        DiagnosticSeverity.INFO -> base
    }
}
