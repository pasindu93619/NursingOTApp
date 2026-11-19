package com.pasindu.nursingotapp.statgraph.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartReadinessSuggestion
import com.pasindu.nursingotapp.statgraph.engine.ValidationIssue
import com.pasindu.nursingotapp.statgraph.engine.ValidationReport
import com.pasindu.nursingotapp.statgraph.engine.ValidationSeverity
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.stats.DatasetSummaryMetrics
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens

@Composable
fun StatGraphHeroCard(
    datasetName: String,
    datasetDescription: String,
    metrics: DatasetSummaryMetrics,
    validationReport: ValidationReport,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = StatGraphDesignTokens.Shapes.hero,
        color = Color.Transparent,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(StatGraphDesignTokens.Colors.heroGradient, StatGraphDesignTokens.Shapes.hero)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = StatGraphDesignTokens.Colors.heroTextPrimary.copy(alpha = 0.08f),
                    radius = size.height * 0.65f,
                    center = Offset(size.width * 0.92f, size.height * 0.25f)
                )
                drawCircle(
                    color = StatGraphDesignTokens.Colors.heroTextPrimary.copy(alpha = 0.05f),
                    radius = size.height * 0.45f,
                    center = Offset(size.width * 0.08f, size.height * 0.85f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STAT GRAPH STUDIO · PHASE 1",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatGraphDesignTokens.Colors.heroTextMuted,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp
                    )
                    Surface(
                        color = StatGraphDesignTokens.Colors.glassTileBackground,
                        shape = StatGraphDesignTokens.Shapes.pill
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = StatGraphDesignTokens.Colors.heroTextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "AGGREGATE ONLY",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatGraphDesignTokens.Colors.heroTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = datasetName.ifBlank { "Untitled Clinical Dataset" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = StatGraphDesignTokens.Colors.heroTextPrimary,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = datasetDescription.ifBlank {
                        "Configure statistical field roles, enter aggregate observations, and validate missing values safely."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphDesignTokens.Colors.heroTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatGraphGlassTile(
                        label = "FIELDS",
                        value = "${metrics.fieldCount}",
                        supporting = if (metrics.derivedCount > 0) "+${metrics.derivedCount} derived" else "Schema",
                        modifier = Modifier.weight(1f)
                    )
                    StatGraphGlassTile(
                        label = "ROWS",
                        value = "${metrics.rowCount}",
                        supporting = "${metrics.completenessPercent}% filled",
                        modifier = Modifier.weight(1f)
                    )
                    StatGraphGlassTile(
                        label = "AUDIT",
                        value = "${validationReport.errorCount}E · ${validationReport.warningCount}W",
                        supporting = if (metrics.missingCellCount > 0) "${metrics.missingCellCount} missing" else "No gaps",
                        modifier = Modifier.weight(1.15f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatGraphGlassTile(
    label: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(
            width = 1.dp,
            color = StatGraphDesignTokens.Colors.glassTileBorder,
            shape = StatGraphDesignTokens.Shapes.medium
        ),
        shape = StatGraphDesignTokens.Shapes.medium,
        color = StatGraphDesignTokens.Colors.glassTileBackground
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = StatGraphDesignTokens.Colors.heroTextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = StatGraphDesignTokens.Colors.heroTextPrimary,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelSmall,
                color = StatGraphDesignTokens.Colors.heroTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatGraphPrivacyBanner(
    smallCountCellCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StatGraphDesignTokens.Colors.softAmber,
        shape = StatGraphDesignTokens.Shapes.large
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = StatGraphDesignTokens.Colors.glassPillBackground,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = "Aggregate privacy notice",
                    tint = StatGraphDesignTokens.Colors.warning,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Aggregate Statistical Data Only",
                    style = MaterialTheme.typography.titleSmall,
                    color = StatGraphDesignTokens.Colors.onSoftAmber,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Never enter patient names, NICs, BHT/hospital numbers, or identifying details. Empty cells stay missing (never converted to 0).",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphDesignTokens.Colors.slate
                )
                if (smallCountCellCount > 0) {
                    Surface(
                        color = StatGraphDesignTokens.Colors.softRose,
                        shape = StatGraphDesignTokens.Shapes.pill
                    ) {
                        Text(
                            text = "Small-count alert: $smallCountCellCount cell(s) have count < 5",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatGraphDesignTokens.Colors.onSoftRose,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatGraphValidationBanner(
    report: ValidationReport,
    modifier: Modifier = Modifier
) {
    if (report.isClean) return

    var expanded by remember { mutableStateOf(true) }
    val hasErrors = report.hasBlockingErrors
    val containerColor = if (hasErrors) {
        StatGraphDesignTokens.Colors.softRose
    } else {
        StatGraphDesignTokens.Colors.softAmber
    }
    val accentColor = if (hasErrors) {
        StatGraphDesignTokens.Colors.error
    } else {
        StatGraphDesignTokens.Colors.warning
    }
    val onContainerColor = if (hasErrors) {
        StatGraphDesignTokens.Colors.onSoftRose
    } else {
        StatGraphDesignTokens.Colors.onSoftAmber
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = StatGraphDesignTokens.Shapes.large
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasErrors) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildString {
                            if (report.errorCount > 0) append("${report.errorCount} Blocking Error(s)")
                            if (report.errorCount > 0 && report.warningCount > 0) append(" · ")
                            if (report.warningCount > 0) append("${report.warningCount} Warning(s)")
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = onContainerColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (hasErrors) {
                            "Resolve blocking errors before finalizing dataset."
                        } else {
                            "Warnings are non-blocking — review periods and small counts."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphDesignTokens.Colors.slate
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show (${report.issues.size})")
                }
            }

            if (expanded) {
                val visibleIssues = report.issues.take(8)
                visibleIssues.forEach { issue ->
                    ValidationIssueRow(issue = issue)
                }
                if (report.issues.size > visibleIssues.size) {
                    Text(
                        text = "+${report.issues.size - visibleIssues.size} additional issue(s) shown inline on rows",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatGraphDesignTokens.Colors.slate
                    )
                }
            }
        }
    }
}

@Composable
private fun ValidationIssueRow(issue: ValidationIssue) {
    val isError = issue.severity == ValidationSeverity.ERROR
    val badgeColor = if (isError) StatGraphDesignTokens.Colors.error else StatGraphDesignTokens.Colors.warning
    val badgeLabel = if (isError) "ERROR" else "WARN"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = StatGraphDesignTokens.Colors.surface.copy(alpha = 0.72f),
                shape = StatGraphDesignTokens.Shapes.small
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = badgeColor.copy(alpha = 0.14f),
            shape = StatGraphDesignTokens.Shapes.pill
        ) {
            Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Text(
            text = issue.message,
            style = MaterialTheme.typography.bodySmall,
            color = StatGraphDesignTokens.Colors.ink,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartReadinessStrip(
    suggestions: List<ChartReadinessSuggestion>,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StatGraphDesignTokens.Colors.softBlue,
        shape = StatGraphDesignTokens.Shapes.large
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = StatGraphDesignTokens.Colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Chart Toolkit Readiness",
                    style = MaterialTheme.typography.labelLarge,
                    color = StatGraphDesignTokens.Colors.ink,
                    fontWeight = FontWeight.Bold
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.forEach { item ->
                    Surface(
                        color = if (item.isReady) {
                            StatGraphDesignTokens.Colors.softMint
                        } else {
                            StatGraphDesignTokens.Colors.surface
                        },
                        shape = StatGraphDesignTokens.Shapes.pill
                    ) {
                        Text(
                            text = if (item.isReady) "✓ ${item.chartType.displayName}" else item.chartType.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isReady) {
                                StatGraphDesignTokens.Colors.onSoftMint
                            } else {
                                StatGraphDesignTokens.Colors.slate
                            },
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatGraphSchemaFieldCard(
    field: StatField,
    parentFieldName: String?,
    onConfirmRole: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pastelColor = when (field.role) {
        is FieldRole.Time -> StatGraphDesignTokens.Colors.softBlue
        is FieldRole.Measure, is FieldRole.PartOfMeasure -> StatGraphDesignTokens.Colors.softMint
        is FieldRole.RateContinuous, is FieldRole.EstimateWithLimits -> StatGraphDesignTokens.Colors.softPurple
        is FieldRole.DurationWithEvent -> StatGraphDesignTokens.Colors.softAmber
        FieldRole.Category, FieldRole.Group, FieldRole.Label -> StatGraphDesignTokens.Colors.softSlate
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StatGraphDesignTokens.Colors.surface,
        shape = StatGraphDesignTokens.Shapes.large,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = field.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = StatGraphDesignTokens.Colors.ink,
                            fontWeight = FontWeight.Bold
                        )
                        if (field.isRequired) {
                            Surface(
                                color = StatGraphDesignTokens.Colors.softRose,
                                shape = StatGraphDesignTokens.Shapes.pill
                            ) {
                                Text(
                                    text = "REQUIRED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatGraphDesignTokens.Colors.onSoftRose,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    val metaParts = buildList {
                        if (field.unit.isNotBlank()) add("Unit: ${field.unit}")
                        if (field.minValue != null) add("Min: ${field.minValue}")
                        if (field.maxValue != null) add("Max: ${field.maxValue}")
                        if (parentFieldName != null) add("Part of: $parentFieldName")
                    }
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphDesignTokens.Colors.slate
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit field ${field.name}",
                        tint = StatGraphDesignTokens.Colors.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete field ${field.name}",
                        tint = StatGraphDesignTokens.Colors.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = pastelColor,
                    shape = StatGraphDesignTokens.Shapes.pill
                ) {
                    Text(
                        text = field.role.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = StatGraphDesignTokens.Colors.ink,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                if (!field.isRoleConfirmed && field.suggestedRole != null) {
                    Surface(
                        onClick = onConfirmRole,
                        color = StatGraphDesignTokens.Colors.softPurple,
                        shape = StatGraphDesignTokens.Shapes.pill
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StatGraphDesignTokens.Colors.onSoftPurple,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Suggested · Tap to Confirm",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatGraphDesignTokens.Colors.onSoftPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatGraphDesignTokens.Colors.success,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Role confirmed",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatGraphDesignTokens.Colors.success,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatGraphDerivedFieldCard(
    definition: DerivedFieldDefinition,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StatGraphDesignTokens.Colors.softPurple,
        shape = StatGraphDesignTokens.Shapes.large
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = StatGraphDesignTokens.Colors.surface,
                shape = StatGraphDesignTokens.Shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Functions,
                    contentDescription = null,
                    tint = StatGraphDesignTokens.Colors.purpleAccent,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = definition.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = StatGraphDesignTokens.Colors.onSoftPurple,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildString {
                        append(definition.calculation.label)
                        if (definition.unit.isNotBlank()) append(" · ${definition.unit}")
                        if (definition.formulaSummary.isNotBlank()) append("\n${definition.formulaSummary}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphDesignTokens.Colors.slate
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove derived field",
                    tint = StatGraphDesignTokens.Colors.error
                )
            }
        }
    }
}

@Composable
fun StatGraphSectionHeader(
    eyebrow: String,
    title: String,
    icon: ImageVector? = null,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StatGraphDesignTokens.Colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = StatGraphDesignTokens.Colors.slate,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = StatGraphDesignTokens.Colors.ink,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        trailingAction?.invoke()
    }
}
