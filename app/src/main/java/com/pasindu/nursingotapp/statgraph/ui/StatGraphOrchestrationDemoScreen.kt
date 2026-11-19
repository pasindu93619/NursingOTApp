package com.pasindu.nursingotapp.statgraph.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartOrientationAdvisor
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartReadabilityAnalyzer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartHost
import com.pasindu.nursingotapp.statgraph.chart.toolkit.rememberStatGraphChartStyle
import com.pasindu.nursingotapp.statgraph.data.StatGraphSampleData
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnostic
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DiagnosticSeverity
import com.pasindu.nursingotapp.statgraph.model.DatasetMetadata
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.orchestration.ChartMappingSelection
import com.pasindu.nursingotapp.statgraph.orchestration.ChartRenderState
import com.pasindu.nursingotapp.statgraph.orchestration.StatGraphOrchestrator
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import java.time.YearMonth

/**
 * Reusable Compose bridge connecting [StatGraphOrchestrator] to [StatGraphChartHost],
 * analytical warnings, clinical notes, and explicit unsupported/error/empty states.
 *
 * Expensive analysis, diagnostics, and data mapping run inside `remember(dataset, selection, isLoading)`
 * so recompositions remain lightweight.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatGraphOrchestratedView(
    dataset: StatDataset,
    selection: ChartMappingSelection = ChartMappingSelection(),
    isLoading: Boolean = false,
    onSelectAlternativeChart: ((specId: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    style: ChartStyle = rememberStatGraphChartStyle(),
) {
    val renderState = remember(dataset, selection, isLoading) {
        StatGraphOrchestrator.orchestrate(
            dataset = dataset,
            selection = selection,
            isLoading = isLoading,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        when (renderState) {
            is ChartRenderState.Loading -> {
                StatGraphChartHost(
                    data = ChartRenderData(
                        title = dataset.name.ifBlank { "Chart" },
                        chartKind = ChartKind.LINE,
                        domainLabels = emptyList(),
                        series = emptyList(),
                        isLoading = true,
                    ),
                )
            }

            is ChartRenderState.EmptyDataset -> {
                StatGraphChartHost(
                    data = ChartRenderData(
                        title = renderState.datasetName,
                        chartKind = ChartKind.BAR,
                        domainLabels = emptyList(),
                        series = emptyList(),
                        emptyMessage = renderState.message,
                    ),
                )
            }

            is ChartRenderState.InvalidDataset -> {
                StatGraphChartHost(
                    data = ChartRenderData(
                        title = renderState.datasetName,
                        chartKind = ChartKind.BAR,
                        domainLabels = emptyList(),
                        series = emptyList(),
                        errorMessage = renderState.message,
                    ),
                )
            }

            is ChartRenderState.RenderingError -> {
                StatGraphChartHost(
                    data = ChartRenderData(
                        title = renderState.datasetName,
                        chartKind = ChartKind.BAR,
                        domainLabels = emptyList(),
                        series = emptyList(),
                        errorMessage = renderState.message,
                    ),
                )
            }

            is ChartRenderState.NoSuitableRecommendation -> {
                OrchestrationNoticeCard(
                    title = "No Suitable Chart Matched",
                    message = renderState.message,
                    isError = false,
                )
            }

            is ChartRenderState.UnsupportedChartRenderer -> {
                OrchestrationNoticeCard(
                    title = "${renderState.requestedSpec.canonicalName} — Renderer Unavailable",
                    message = renderState.explanation,
                    isError = false,
                )
                if (renderState.supportedAlternatives.isNotEmpty() && onSelectAlternativeChart != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Supported alternatives recommended for this dataset:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        renderState.supportedAlternatives.forEach { alt ->
                            AssistChip(
                                onClick = { onSelectAlternativeChart(alt.specification.id) },
                                label = { Text("${alt.specification.canonicalName} (${alt.score.toInt()}%)") },
                            )
                        }
                    }
                }
            }

            is ChartRenderState.Ready -> {
                val config = renderState.configuration
                val recommendation = renderState.suite.allEvaluatedCharts.firstOrNull {
                    it.specification.id == config.specification.id
                }
                val orientationAdvice = remember(renderState.renderData) {
                    val metrics = ChartReadabilityAnalyzer.analyze(renderState.renderData)
                    ChartOrientationAdvisor.advise(
                        metrics = metrics,
                        userExplicitlySelectedVertical = renderState.renderData.userExplicitlySelectedChart,
                    )
                }

                // Recommendation badge + metadata bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = StatGraphDesignTokens.Shapes.medium,
                    color = StatGraphDesignTokens.Colors.softBlue,
                    border = BorderStroke(1.dp, StatGraphDesignTokens.Colors.borderSubtle),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${config.specification.canonicalName} (${recommendation?.verdict?.displayName ?: "Ready"})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "X: ${config.xAxisLabel} • Y: ${config.yAxisLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (orientationAdvice.recommendHorizontalBar &&
                            renderState.renderData.chartKind == ChartKind.BAR &&
                            onSelectAlternativeChart != null
                        ) {
                            AssistChip(
                                onClick = { onSelectAlternativeChart("horizontal_bar_chart") },
                                label = {
                                    Text(
                                        text = "Switch to Horizontal Bar (Full unrotated labels)",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                StatGraphChartHost(data = renderState.renderData, style = style)

                if (config.clinicalNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NotesBanner(notes = config.clinicalNotes)
                }

                val visibleWarnings = config.diagnostics.filter {
                    it.severity == DiagnosticSeverity.WARNING || it.severity == DiagnosticSeverity.BLOCKING
                }
                if (visibleWarnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    WarningsPanel(diagnostics = visibleWarnings)
                }

                if (renderState.suite.derivedEvidenceSummary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DerivedSummaryPanel(summaries = renderState.suite.derivedEvidenceSummary)
                }
            }
        }
    }
}

@Composable
private fun OrchestrationNoticeCard(
    title: String,
    message: String,
    isError: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StatGraphDesignTokens.Shapes.large,
        color = if (isError) StatGraphDesignTokens.Colors.softRose else StatGraphDesignTokens.Colors.softAmber,
        border = BorderStroke(1.dp, StatGraphDesignTokens.Colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else StatGraphDesignTokens.Colors.warning,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NotesBanner(notes: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StatGraphDesignTokens.Shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, StatGraphDesignTokens.Colors.borderSubtle),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Clinical & Methodological Notes",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            notes.forEach { note ->
                Text(
                    text = "• $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WarningsPanel(diagnostics: List<DatasetDiagnostic>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StatGraphDesignTokens.Shapes.medium,
        color = StatGraphDesignTokens.Colors.softAmber,
        border = BorderStroke(1.dp, StatGraphDesignTokens.Colors.warning.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = StatGraphDesignTokens.Colors.warning,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Analytical & Privacy Warnings (${diagnostics.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            diagnostics.forEach { diag ->
                Text(
                    text = "• ${diag.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DerivedSummaryPanel(summaries: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StatGraphDesignTokens.Shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, StatGraphDesignTokens.Colors.borderSubtle),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Computed Derived Evidence",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = StatGraphDesignTokens.Colors.borderSubtle)
            Spacer(modifier = Modifier.height(6.dp))
            summaries.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Standalone Dengue Surveillance Orchestration Demonstration.
 * Not wired into app navigation or home screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatGraphOrchestrationDemoScreen(
    modifier: Modifier = Modifier,
) {
    var selectedScenario by remember { mutableIntStateOf(0) }
    var requestedOverrideSpecId by remember { mutableStateOf<String?>(null) }

    val baseDengueDataset = remember { StatGraphSampleData.dengueMonthlyPopulatedDataset() }
    val groupedWardDengueDataset = remember { buildGroupedDengueWarningDemoDataset() }

    val (activeDataset, baseSelection) = remember(selectedScenario, baseDengueDataset, groupedWardDengueDataset) {
        when (selectedScenario) {
            0 -> baseDengueDataset to ChartMappingSelection(
                customTitle = "Dengue Admissions Trend (Auto-Recommended)",
            )
            1 -> baseDengueDataset to ChartMappingSelection(
                chartSpecId = "line_chart",
                xFieldId = "f_month",
                derivedFieldIds = listOf("d_pct"),
                customTitle = "Monthly Dengue Share (% of Total Admissions)",
            )
            else -> groupedWardDengueDataset to ChartMappingSelection(
                chartSpecId = "grouped_bar_chart",
                xFieldId = "f_month",
                yFieldIds = listOf("f_dengue"),
                groupFieldId = "f_ward",
                customTitle = "Dengue Patients by Ward (Missing & <5 Preserved)",
            )
        }
    }

    val effectiveSelection = remember(baseSelection, requestedOverrideSpecId) {
        if (requestedOverrideSpecId != null) {
            baseSelection.copy(chartSpecId = requestedOverrideSpecId)
        } else {
            baseSelection
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Stat Graph Studio — Orchestration Demo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Dataset → Statistics & Derived Rates → Diagnostics → Recommendation → Chart Host",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = selectedScenario == 0 && requestedOverrideSpecId == null,
                onClick = {
                    selectedScenario = 0
                    requestedOverrideSpecId = null
                },
                label = { Text("Auto Trend") },
            )
            FilterChip(
                selected = selectedScenario == 1 && requestedOverrideSpecId == null,
                onClick = {
                    selectedScenario = 1
                    requestedOverrideSpecId = null
                },
                label = { Text("Dengue Share (%)") },
            )
            FilterChip(
                selected = selectedScenario == 2 && requestedOverrideSpecId == null,
                onClick = {
                    selectedScenario = 2
                    requestedOverrideSpecId = null
                },
                label = { Text("Grouped Wards") },
            )
            FilterChip(
                selected = requestedOverrideSpecId == "pie_chart",
                onClick = {
                    requestedOverrideSpecId = if (requestedOverrideSpecId == "pie_chart") null else "pie_chart"
                },
                label = { Text("Try Pie (Unsupported)") },
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        StatGraphOrchestratedView(
            dataset = activeDataset,
            selection = effectiveSelection,
            onSelectAlternativeChart = { altId -> requestedOverrideSpecId = altId },
        )
    }
}

private fun buildGroupedDengueWarningDemoDataset(): StatDataset {
    val monthField = StatField(id = "f_month", name = "Month", role = FieldRole.Time.Month, isRequired = true)
    val wardField = StatField(id = "f_ward", name = "Ward", role = FieldRole.Group, isRequired = true)
    val dengueField = StatField(id = "f_dengue", name = "Dengue Patients", unit = "patients", role = FieldRole.Measure, minValue = 0.0)
    val totalField = StatField(id = "f_total", name = "Total Patients", unit = "patients", role = FieldRole.Measure, minValue = 0.0)

    fun row(id: String, m: Int, ward: String, dengue: StatValue, total: Long): StatRow {
        val period = TimePeriod.Month(YearMonth.of(2026, m))
        return StatRow(
            id = id,
            periodOrCategoryLabel = period.formatDisplay(),
            values = mapOf(
                "f_month" to StatValue.TimePeriodValue(period, period.formatIso()),
                "f_ward" to StatValue.TextValue(ward),
                "f_dengue" to dengue,
                "f_total" to StatValue.WholeNumber(total),
            ),
        )
    }

    return StatDataset(
        id = "ds_dengue_ward_demo",
        name = "Ward Dengue Surveillance (Grouped)",
        description = "Grouped ward comparison demonstrating missing values preserved without zero coercion and <5 small-count privacy alerts.",
        fields = listOf(monthField, wardField, dengueField, totalField),
        rows = listOf(
            row("m1", 1, "Medical", StatValue.WholeNumber(18), 120),
            row("p1", 1, "Pediatric", StatValue.WholeNumber(7), 64),
            row("m2", 2, "Medical", StatValue.WholeNumber(24), 132),
            row("p2", 2, "Pediatric", StatValue.Missing, 68),
            row("m3", 3, "Medical", StatValue.WholeNumber(31), 145),
            row("p3", 3, "Pediatric", StatValue.WholeNumber(11), 71),
            row("m4", 4, "Medical", StatValue.WholeNumber(27), 138),
            row("p4", 4, "Pediatric", StatValue.WholeNumber(4), 60),
        ),
        metadata = DatasetMetadata(sourceLabel = "Grouped Ward Demo"),
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 760)
@Composable
private fun StatGraphOrchestrationDemoPreview() {
    MaterialTheme {
        StatGraphOrchestrationDemoScreen()
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun StatGraphGroupedWarningPreview() {
    val grouped = remember { buildGroupedDengueWarningDemoDataset() }
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StatGraphOrchestratedView(
                dataset = grouped,
                selection = ChartMappingSelection(
                    chartSpecId = "grouped_bar_chart",
                    xFieldId = "f_month",
                    yFieldIds = listOf("f_dengue"),
                    groupFieldId = "f_ward",
                ),
            )
        }
    }
}
