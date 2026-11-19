package com.pasindu.nursingotapp.statgraph.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.statgraph.engine.ValidationReport
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport
import com.pasindu.nursingotapp.statgraph.stats.DatasetSummaryMetrics
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphWorkspaceTokens

/**
 * Hero workspace header establishing Stat Graph Studio identity, dataset readiness status,
 * key aggregate counts, and primary dataset actions without overcrowding mobile viewports.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatGraphWorkspaceHeader(
    datasetName: String,
    datasetDescription: String,
    sourceLabel: String,
    summaryMetrics: DatasetSummaryMetrics,
    validationReport: ValidationReport,
    diagnosticsReport: DatasetDiagnosticsReport,
    unconfirmedRoleCount: Int,
    compactMode: Boolean,
    onCreateBlankDataset: () -> Unit,
    onImportCsv: () -> Unit,
    onLoadDengueDemo: () -> Unit,
    modifier: Modifier = Modifier,
    datasetCount: Int = 1,
    eligibleChartCount: Int = 31,
) {
    val statusLabel = when {
        summaryMetrics.fieldCount == 0 || summaryMetrics.rowCount == 0 -> "Empty"
        diagnosticsReport.hasBlockingIssues || validationReport.hasBlockingErrors -> "Needs Fix"
        unconfirmedRoleCount > 0 -> "Confirm Roles"
        diagnosticsReport.warningDiagnostics.isNotEmpty() -> "Ready · Alerts"
        else -> "Ready"
    }

    val statusBadgeBg = when {
        diagnosticsReport.hasBlockingIssues || validationReport.hasBlockingErrors ->
            StatGraphWorkspaceTokens.Colors.error
        unconfirmedRoleCount > 0 || diagnosticsReport.warningDiagnostics.isNotEmpty() ->
            StatGraphWorkspaceTokens.Colors.warning
        else -> StatGraphWorkspaceTokens.Colors.success
    }

    val heroShape = RoundedCornerShape(28.dp)
    val tileShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = heroShape,
        color = Color.Transparent,
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = StatGraphWorkspaceTokens.Colors.StatGraphHeroGradient,
                    shape = heroShape,
                )
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "DATA & RESEARCH STUDIO",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = datasetName.ifBlank { "Untitled Clinical Dataset" },
                            color = Color.White,
                            fontSize = if (compactMode) 22.sp else 25.sp,
                            lineHeight = if (compactMode) 27.sp else 30.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = datasetDescription.ifBlank {
                                "Aggregate surveillance & statistical exploration workspace (no patient identifiers)."
                            },
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            maxLines = if (compactMode) 2 else 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QueryStats,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                        Surface(
                            color = statusBadgeBg,
                            shape = StatGraphDesignTokens.Shapes.pill,
                            modifier = Modifier.semantics {
                                contentDescription = "Dataset status: $statusLabel"
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WorkspaceGlassTile(
                        label = "DATASETS",
                        value = "$datasetCount",
                        supporting = sourceLabel.ifBlank { "Active" },
                        shape = tileShape,
                        modifier = Modifier.weight(1f),
                    )
                    WorkspaceGlassTile(
                        label = "ROWS",
                        value = "${summaryMetrics.rowCount}",
                        supporting = "${summaryMetrics.completenessPercent}% complete",
                        shape = tileShape,
                        modifier = Modifier.weight(1f),
                    )
                    WorkspaceGlassTile(
                        label = "CHARTS",
                        value = "$eligibleChartCount",
                        supporting = "${summaryMetrics.fieldCount} vars",
                        shape = tileShape,
                        modifier = Modifier.weight(1f),
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onLoadDengueDemo,
                        shape = StatGraphDesignTokens.Shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = StatGraphWorkspaceTokens.Colors.ink,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Dengue Demo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    OutlinedButton(
                        onClick = onImportCsv,
                        shape = StatGraphDesignTokens.Shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StatGraphWorkspaceTokens.Colors.glassTileBackground,
                            contentColor = Color.White,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = StatGraphWorkspaceTokens.Colors.glassTileBorder,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Import CSV",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    OutlinedButton(
                        onClick = onCreateBlankDataset,
                        shape = StatGraphDesignTokens.Shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StatGraphWorkspaceTokens.Colors.glassTileBackground,
                            contentColor = Color.White,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = StatGraphWorkspaceTokens.Colors.glassTileBorder,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "New Dataset",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceGlassTile(
    label: String,
    value: String,
    supporting: String,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .border(
                width = 1.dp,
                color = StatGraphWorkspaceTokens.Colors.glassTileBorder,
                shape = shape,
            ),
        shape = shape,
        color = StatGraphWorkspaceTokens.Colors.glassTileBackground,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = supporting,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
