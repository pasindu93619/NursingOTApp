package com.pasindu.nursingotapp.statgraph.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.R
import com.pasindu.nursingotapp.statgraph.chart.toolkit.BarDensityPolicy
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartBarColorMode
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartLabelDisplayMode
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.orchestration.ChartMappingSelection
import com.pasindu.nursingotapp.statgraph.orchestration.ChartRenderState
import com.pasindu.nursingotapp.statgraph.ui.state.FieldMappingCapability
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphWorkspaceTokens

/**
 * Interactive axis, series, derived rate, and confidence-interval mapping controls.
 * Only renders controls relevant to the selected chart type and explains invalid mappings clearly.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatGraphMappingControlsCard(
    dataset: StatDataset,
    selection: ChartMappingSelection,
    capability: FieldMappingCapability,
    renderState: ChartRenderState,
    hasCustomMapping: Boolean,
    onSelectXField: (String?) -> Unit,
    onToggleYField: (String) -> Unit,
    onToggleDerivedField: (String) -> Unit,
    onSelectGroupField: (String?) -> Unit,
    onResetMappings: () -> Unit,
    selectedPalette: StatGraphChartPalette? = null,
    isResearchMode: Boolean = false,
    onSelectPalette: ((StatGraphChartPalette?) -> Unit)? = null,
    onSelectLabelDisplayMode: ((ChartLabelDisplayMode) -> Unit)? = null,
    onSelectBarColorMode: ((ChartBarColorMode) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (!capability.supportsXAxis && !capability.supportsYMeasures && !capability.supportsGroupSeries) {
        return
    }

    val xCandidates = if (capability.chartKind == ChartKind.SCATTER) {
        dataset.fields.filter { it.role.isNumeric }
    } else {
        dataset.fields.filter { it.role is FieldRole.Time || it.role is FieldRole.Category }
    }
    val numericFields = dataset.fields.filter { it.role.isNumeric }
    val groupCandidates = dataset.fields.filter {
        it.id != selection.xFieldId && (it.role is FieldRole.Group || it.role is FieldRole.Category)
    }
    val estimateFields = dataset.fields.filter { it.role is FieldRole.EstimateWithLimits }

    val activeConfig = (renderState as? ChartRenderState.Ready)?.configuration
    var expanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = StatGraphDesignTokens.Shapes.extraLarge,
        color = StatGraphWorkspaceTokens.Colors.surface,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                onClick = { expanded = !expanded },
                shape = StatGraphDesignTokens.Shapes.medium,
                color = StatGraphWorkspaceTokens.Colors.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.statgraph_customise_chart_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatGraphWorkspaceTokens.Colors.ink,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                text = if (hasCustomMapping) {
                                    stringResource(R.string.statgraph_customise_chart_custom)
                                } else {
                                    stringResource(R.string.statgraph_customise_chart_auto)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = StatGraphWorkspaceTokens.Colors.slate,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasCustomMapping) {
                            TextButton(
                                onClick = onResetMappings,
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Auto Reset")
                            }
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = StatGraphWorkspaceTokens.Colors.slate,
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded || renderState is ChartRenderState.InvalidDataset) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            if (renderState is ChartRenderState.InvalidDataset) {
                Surface(
                    shape = StatGraphDesignTokens.Shapes.medium,
                    color = StatGraphWorkspaceTokens.Colors.softRose,
                    border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = StatGraphWorkspaceTokens.Colors.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = renderState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphWorkspaceTokens.Colors.onSoftRose,
                        )
                    }
                }
            }

            var activeHelpTopic by remember { mutableStateOf<ContextualHelpTopic?>(null) }
            activeHelpTopic?.let { topic ->
                StatGraphContextualHelpDialog(
                    topic = topic,
                    dataset = dataset,
                    onDismiss = { activeHelpTopic = null },
                )
            }

            if (capability.supportsXAxis && xCandidates.isNotEmpty()) {
                MappingGroupBlock(
                    label = capability.xAxisControlLabel,
                    onHelpClick = { activeHelpTopic = ContextualHelpTopic.DATE_TIME_COLUMNS },
                ) {
                    FilterChip(
                        selected = selection.xFieldId == null,
                        onClick = { onSelectXField(null) },
                        label = {
                            val autoName = activeConfig?.xAxisField?.name
                            Text(if (autoName != null) "Auto ($autoName)" else "Auto")
                        },
                        colors = workspaceFilterChipColors(),
                    )
                    xCandidates.forEach { field ->
                        FilterChip(
                            selected = selection.xFieldId == field.id,
                            onClick = {
                                val next = if (selection.xFieldId == field.id) null else field.id
                                onSelectXField(next)
                            },
                            label = { Text(field.name) },
                            colors = workspaceFilterChipColors(),
                        )
                    }
                }
            }

            if (capability.supportsYMeasures && (numericFields.isNotEmpty() || dataset.derivedFields.isNotEmpty())) {
                MappingGroupBlock(
                    label = buildString {
                        append(capability.yAxisControlLabel)
                        if (capability.supportsMultipleY) append(" (multi-select supported)")
                    },
                ) {
                    numericFields.forEach { field ->
                        val isSelected = field.id in selection.yFieldIds
                        val isAutoPlotted = selection.yFieldIds.isEmpty() &&
                            selection.derivedFieldIds.isEmpty() &&
                            activeConfig?.yAxisFields?.any { it.id == field.id } == true
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleYField(field.id) },
                            label = {
                                val unitSuffix = if (field.unit.isNotBlank()) " (${field.unit})" else ""
                                val autoTag = if (isAutoPlotted) " • Auto" else ""
                                Text("${field.name}$unitSuffix$autoTag")
                            },
                            colors = workspaceFilterChipColors(),
                        )
                    }
                    if (capability.supportsDerivedRates) {
                        dataset.derivedFields.forEach { derived ->
                            val isSelected = derived.id in selection.derivedFieldIds
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleDerivedField(derived.id) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Functions,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    val unitSuffix = if (derived.unit.isNotBlank()) " (${derived.unit})" else ""
                                    Text("${derived.name}$unitSuffix")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatGraphWorkspaceTokens.Colors.accentOrangeSoft,
                                    selectedLabelColor = StatGraphWorkspaceTokens.Colors.onAccentOrangeSoft,
                                    selectedLeadingIconColor = StatGraphWorkspaceTokens.Colors.accentOrange,
                                ),
                            )
                        }
                    }
                }
            }

            if (capability.supportsGroupSeries && groupCandidates.isNotEmpty()) {
                MappingGroupBlock(
                    label = "Group / Series Split",
                    onHelpClick = { activeHelpTopic = ContextualHelpTopic.GROUP_CATEGORY_FIELDS },
                ) {
                    FilterChip(
                        selected = selection.groupFieldId == null,
                        onClick = { onSelectGroupField(null) },
                        label = { Text("Auto / None") },
                        colors = workspaceFilterChipColors(),
                    )
                    groupCandidates.forEach { grp ->
                        FilterChip(
                            selected = selection.groupFieldId == grp.id,
                            onClick = {
                                val next = if (selection.groupFieldId == grp.id) null else grp.id
                                onSelectGroupField(next)
                            },
                            label = { Text(grp.name) },
                            colors = workspaceFilterChipColors(),
                        )
                    }
                }
            }

            if (capability.supportsEstimateField && estimateFields.isNotEmpty()) {
                HorizontalDivider(color = StatGraphWorkspaceTokens.Colors.borderSubtle)
                val estNames = estimateFields.joinToString(", ") { it.name }
                Text(
                    text = "Confidence intervals available ($estNames): lower and upper uncertainty bounds are preserved in tooltips when mapped as Y.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphWorkspaceTokens.Colors.slate,
                )
            }

            if (onSelectLabelDisplayMode != null && capability.supportsXAxis) {
                HorizontalDivider(color = StatGraphWorkspaceTokens.Colors.borderSubtle)
                MappingGroupBlock(label = "Category Label Layout") {
                    ChartLabelDisplayMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selection.labelDisplayMode == mode,
                            onClick = { onSelectLabelDisplayMode(mode) },
                            label = { Text(mode.displayName) },
                            colors = workspaceFilterChipColors(),
                        )
                    }
                }
            }

            if (onSelectBarColorMode != null &&
                capability.chartKind != null &&
                BarDensityPolicy.isBarChartKind(capability.chartKind)
            ) {
                MappingGroupBlock(label = "Bar Color Coding") {
                    ChartBarColorMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selection.barColorMode == mode,
                            onClick = { onSelectBarColorMode(mode) },
                            label = { Text(mode.displayName) },
                            colors = workspaceFilterChipColors(),
                        )
                    }
                }
            }

            if (onSelectPalette != null) {
                HorizontalDivider(color = StatGraphWorkspaceTokens.Colors.borderSubtle)
                val autoDefault = StatGraphChartPalette.resolve(selected = null, isResearchMode = isResearchMode)
                MappingGroupBlock(label = stringResource(R.string.statgraph_palette_label)) {
                    FilterChip(
                        selected = selectedPalette == null,
                        onClick = { onSelectPalette(null) },
                        label = {
                            Text(
                                stringResource(
                                    R.string.statgraph_palette_auto_format,
                                    autoDefault.displayName,
                                ),
                            )
                        },
                        colors = workspaceFilterChipColors(),
                    )
                    StatGraphChartPalette.entries.forEach { palette ->
                        FilterChip(
                            selected = selectedPalette == palette,
                            onClick = { onSelectPalette(palette) },
                            label = { Text(palette.displayName) },
                            colors = workspaceFilterChipColors(),
                        )
                    }
                }
            }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MappingGroupBlock(
    label: String,
    onHelpClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = StatGraphWorkspaceTokens.Colors.ink,
            )
            if (onHelpClick != null) {
                ContextualHelpButton(onClick = onHelpClick)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun workspaceFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = StatGraphWorkspaceTokens.Colors.brandIndigoSoft,
    selectedLabelColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
    selectedLeadingIconColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
)
