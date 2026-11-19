package com.pasindu.nursingotapp.statgraph.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.R
import com.pasindu.nursingotapp.statgraph.engine.recommendation.ChartRecommendation
import com.pasindu.nursingotapp.statgraph.engine.recommendation.ChartVerdict
import com.pasindu.nursingotapp.statgraph.engine.recommendation.RecommendationSuiteResult
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.orchestration.ChartAvailabilityResolver
import com.pasindu.nursingotapp.statgraph.orchestration.ChartAvailabilityState
import com.pasindu.nursingotapp.statgraph.orchestration.ChartToolkitSupport
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphWorkspaceTokens

/**
 * Pure presentation helpers for Step D recommendation hierarchy:
 * one clear winner, up to 3 drawable alternatives, remaining catalogue entries,
 * and a data-grounded "Because..." explanation.
 */
object RecommendationPresentation {

    fun selectWinner(
        suite: RecommendationSuiteResult,
        selectedSpecId: String?,
        activeSpecId: String?,
    ): ChartRecommendation? {
        val lookupId = selectedSpecId ?: activeSpecId
        val pool = suite.recommendedCharts + suite.validCharts + suite.warningCharts
        return suite.allEvaluatedCharts.firstOrNull {
            it.specification.id == lookupId && ChartToolkitSupport.isSupported(it.specification.id)
        }
            ?: pool.firstOrNull { ChartToolkitSupport.isSupported(it.specification.id) }
            ?: suite.allEvaluatedCharts.firstOrNull { it.specification.id == lookupId }
    }

    fun selectAlternatives(
        suite: RecommendationSuiteResult,
        winnerSpecId: String?,
        limit: Int = 3,
    ): List<ChartRecommendation> {
        val pool = suite.recommendedCharts + suite.validCharts + suite.warningCharts
        return pool
            .asSequence()
            .filter { it.specification.id != winnerSpecId }
            .filter { ChartToolkitSupport.isSupported(it.specification.id) }
            .distinctBy { it.specification.id }
            .take(limit)
            .toList()
    }

    fun selectMoreCharts(
        suite: RecommendationSuiteResult,
        winnerSpecId: String?,
        alternativeIds: Set<String>,
    ): List<ChartRecommendation> {
        val excluded = buildSet {
            if (winnerSpecId != null) add(winnerSpecId)
            addAll(alternativeIds)
        }
        return suite.allEvaluatedCharts.filterNot { it.specification.id in excluded }
    }

    fun buildBecauseExplanation(
        recommendation: ChartRecommendation,
        dataset: StatDataset?,
        isResearchMode: Boolean,
    ): String {
        if (isResearchMode) {
            return recommendation.summaryReason
        }
        if (dataset == null || dataset.fields.isEmpty()) {
            return recommendation.summaryReason
        }
        val rowCount = dataset.rows.size
        val timeField = dataset.fields.firstOrNull { it.role is FieldRole.Time }
        val categoryField = dataset.fields.firstOrNull { it.role is FieldRole.Category }
        val measureNames = dataset.fields
            .filter { it.role.isNumeric }
            .take(2)
            .map { field ->
                if (field.unit.isNotBlank()) "${field.name} (${field.unit})" else field.name
            }

        val domainPart = when {
            timeField != null && categoryField != null ->
                "your $rowCount rows track ${timeField.name} and ${categoryField.name}"
            timeField != null ->
                "your $rowCount rows are ordered by ${timeField.name}"
            categoryField != null ->
                "your $rowCount rows compare ${categoryField.name}"
            else ->
                "your dataset has $rowCount row(s)"
        }
        val measurePart = if (measureNames.isNotEmpty()) {
            " for ${measureNames.joinToString(" and ")}"
        } else {
            ""
        }
        return "$domainPart$measurePart. ${recommendation.summaryReason}"
    }
}

/**
 * Step D Intelligent Chart Recommendation UI:
 * - One clear winner ("Best for your data: [Chart name]") with data-grounded "Because..." explanation
 * - Up to 3 drawable suitable alternatives
 * - Expandable "More charts" section for remaining catalogue items
 * - "Why not the others?" ModalBottomSheet explaining unsuitable/impossible charts
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StatGraphRecommendationSection(
    suite: RecommendationSuiteResult,
    selectedSpecId: String?,
    activeSpecId: String?,
    showAllCatalogueCharts: Boolean,
    onSelectChartSpec: (String?) -> Unit,
    onToggleShowAllCatalogueCharts: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dataset: StatDataset? = null,
    isResearchMode: Boolean = false,
    onToggleResearchMode: ((Boolean) -> Unit)? = null,
    onUseLineChart: (() -> Unit)? = null,
    onGroupIntoFourWeekBlocks: (() -> Unit)? = null,
) {
    val winner = remember(suite, selectedSpecId, activeSpecId) {
        RecommendationPresentation.selectWinner(suite, selectedSpecId, activeSpecId)
    }
    val alternatives = remember(suite, winner?.specification?.id) {
        RecommendationPresentation.selectAlternatives(suite, winner?.specification?.id, limit = 3)
    }
    val moreCharts = remember(suite, winner?.specification?.id, alternatives) {
        RecommendationPresentation.selectMoreCharts(
            suite = suite,
            winnerSpecId = winner?.specification?.id,
            alternativeIds = alternatives.map { it.specification.id }.toSet(),
        )
    }

    var showMoreCharts by remember { mutableStateOf(showAllCatalogueCharts) }
    var showWhyNotSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. One Clear Winner Card
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.statgraph_heading_best_chart),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatGraphWorkspaceTokens.Colors.ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .testTag("statgraph_best_chart_heading")
                                .semantics { heading() },
                        )
                    }
                    if (isResearchMode) {
                        Surface(
                            shape = StatGraphDesignTokens.Shapes.pill,
                            color = StatGraphWorkspaceTokens.Colors.brandIndigoSoft,
                        ) {
                            Text(
                                text = suite.taskClassification.primaryTask.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }

                if (onToggleResearchMode != null) {
                    StatGraphModeSegmentedSelector(
                        isResearchMode = isResearchMode,
                        onSelectMode = onToggleResearchMode,
                    )
                }

                if (winner != null) {
                    val becauseText = remember(winner, dataset, isResearchMode) {
                        RecommendationPresentation.buildBecauseExplanation(
                            recommendation = winner,
                            dataset = dataset,
                            isResearchMode = isResearchMode,
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.statgraph_winner_heading,
                            winner.specification.canonicalName,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = StatGraphWorkspaceTokens.Colors.ink,
                    )
                    Surface(
                        shape = StatGraphDesignTokens.Shapes.medium,
                        color = StatGraphWorkspaceTokens.Colors.brandIndigoSoft.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.statgraph_winner_because, becauseText),
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphWorkspaceTokens.Colors.ink,
                            modifier = Modifier.padding(10.dp),
                        )
                    }

                    val densityWarning = remember(winner, suite) {
                        winner.warnings.firstOrNull { it.contains("lot of bars", ignoreCase = true) }
                            ?: suite.allEvaluatedCharts
                                .firstOrNull { it.specification.id == "grouped_bar_chart" }
                                ?.warnings
                                ?.firstOrNull { it.contains("lot of bars", ignoreCase = true) }
                    }
                    if (densityWarning != null && (onUseLineChart != null || onGroupIntoFourWeekBlocks != null)) {
                        Surface(
                            shape = StatGraphDesignTokens.Shapes.medium,
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = densityWarning,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = StatGraphWorkspaceTokens.Colors.ink,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    if (onUseLineChart != null &&
                                        winner.specification.id != "line_chart" &&
                                        winner.specification.id != "multi_series_line_chart"
                                    ) {
                                        OutlinedButton(
                                            onClick = onUseLineChart,
                                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        ) {
                                            Text("Use line chart")
                                        }
                                    }
                                    if (onGroupIntoFourWeekBlocks != null) {
                                        OutlinedButton(
                                            onClick = onGroupIntoFourWeekBlocks,
                                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        ) {
                                            Text("Group into 4-week blocks")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isResearchMode) {
                        HorizontalDivider(color = StatGraphWorkspaceTokens.Colors.borderSubtle)
                        ActiveRecommendationDetailCard(
                            recommendation = winner,
                            isToolkitSupported = ChartToolkitSupport.isSupported(winner.specification.id),
                            isResearchMode = true,
                        )
                    }
                } else {
                    Text(
                        text = suite.taskClassification.rationale,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphWorkspaceTokens.Colors.slate,
                    )
                }
            }
        }

        // 2. Up to Three Drawable Alternatives
        if (alternatives.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = StatGraphDesignTokens.Shapes.extraLarge,
                color = StatGraphWorkspaceTokens.Colors.surface,
                border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.statgraph_alternatives_heading),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatGraphWorkspaceTokens.Colors.ink,
                            modifier = Modifier
                                .testTag("statgraph_other_options_heading")
                                .semantics { heading() },
                        )
                        if (selectedSpecId != null) {
                            TextButton(
                                onClick = { onSelectChartSpec(null) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text("Auto Recommend")
                            }
                        }
                    }

                    alternatives.forEach { rec ->
                        val isSelected = selectedSpecId == rec.specification.id
                        val palette = StatGraphWorkspaceTokens.verdictPalette(rec.verdict)
                        Surface(
                            onClick = { onSelectChartSpec(rec.specification.id) },
                            shape = StatGraphDesignTokens.Shapes.large,
                            color = if (isSelected) {
                                palette.container.copy(alpha = 0.45f)
                            } else {
                                StatGraphWorkspaceTokens.Colors.surfaceMuted
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) palette.content else StatGraphWorkspaceTokens.Colors.borderSubtle,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(verdictDotColor(rec.verdict)),
                                        )
                                        Text(
                                            text = rec.specification.canonicalName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = StatGraphWorkspaceTokens.Colors.ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    VerdictPill(
                                        verdict = rec.verdict,
                                        isResearchMode = isResearchMode,
                                        missingRequirementHint = rec.missingRequirements.firstOrNull(),
                                    )
                                }
                                Text(
                                    text = rec.summaryReason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatGraphWorkspaceTokens.Colors.slate,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Compact "More charts" Expandable Section + 4. "Why not the others?" Bottom Sheet Trigger
        val unsuitableCount = suite.notSuitableCharts.size + suite.impossibleCharts.size
        if (moreCharts.isNotEmpty() || unsuitableCount > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = StatGraphDesignTokens.Shapes.extraLarge,
                color = StatGraphWorkspaceTokens.Colors.surface,
                border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (moreCharts.isNotEmpty()) {
                        Surface(
                            onClick = {
                                showMoreCharts = !showMoreCharts
                                onToggleShowAllCatalogueCharts(showMoreCharts)
                            },
                            shape = StatGraphDesignTokens.Shapes.medium,
                            color = StatGraphWorkspaceTokens.Colors.surfaceMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.statgraph_more_charts_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatGraphWorkspaceTokens.Colors.ink,
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.statgraph_more_charts_subtitle,
                                            moreCharts.size,
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatGraphWorkspaceTokens.Colors.slate,
                                    )
                                }
                                Icon(
                                    imageVector = if (showMoreCharts) {
                                        Icons.Rounded.ExpandLess
                                    } else {
                                        Icons.Rounded.ExpandMore
                                    },
                                    contentDescription = null,
                                    tint = StatGraphWorkspaceTokens.Colors.slate,
                                )
                            }
                        }

                        AnimatedVisibility(visible = showMoreCharts) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                moreCharts.forEach { rec ->
                                    val isSupported = ChartToolkitSupport.isSupported(rec.specification.id)
                                    val isSelectable = isSupported &&
                                        rec.verdict != ChartVerdict.NOT_SUITABLE &&
                                        rec.verdict != ChartVerdict.IMPOSSIBLE
                                    VerdictEvaluationRow(
                                        recommendation = rec,
                                        isSelected = selectedSpecId == rec.specification.id,
                                        enabled = isSelectable,
                                        onSelect = {
                                            if (isSelectable) {
                                                onSelectChartSpec(rec.specification.id)
                                            }
                                        },
                                        isResearchMode = isResearchMode,
                                    )
                                }
                            }
                        }
                    }

                    if (unsuitableCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${suite.notSuitableCharts.size} not suitable · ${suite.impossibleCharts.size} impossible",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatGraphWorkspaceTokens.Colors.slate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                            )
                            TextButton(
                                onClick = { showWhyNotSheet = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("statgraph_why_not_button"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.statgraph_action_why_not_others),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                                        maxLines = 1,
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWhyNotSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val excludedCharts = suite.notSuitableCharts + suite.impossibleCharts
        ModalBottomSheet(
            onDismissRequest = { showWhyNotSheet = false },
            sheetState = sheetState,
            containerColor = StatGraphWorkspaceTokens.Colors.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("statgraph_why_not_sheet")
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.statgraph_why_not_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = StatGraphWorkspaceTokens.Colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.statgraph_why_not_sheet_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphWorkspaceTokens.Colors.slate,
                )
                HorizontalDivider(color = StatGraphWorkspaceTokens.Colors.borderSubtle)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("statgraph_why_not_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(
                        items = excludedCharts,
                        key = { it.specification.id },
                    ) { rec ->
                        WhyNotChartRow(
                            recommendation = rec,
                            isResearchMode = isResearchMode,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatGraphModeSegmentedSelector(
    isResearchMode: Boolean,
    onSelectMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("statgraph_mode_selector"),
        shape = StatGraphDesignTokens.Shapes.large,
        color = StatGraphWorkspaceTokens.Colors.surfaceMuted,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val options = listOf(
                Triple(
                    false,
                    stringResource(R.string.statgraph_mode_simple),
                    Icons.Rounded.Lightbulb,
                ),
                Triple(
                    true,
                    stringResource(R.string.statgraph_mode_research),
                    Icons.Rounded.Science,
                ),
            )
            options.forEach { (research, label, icon) ->
                val isSelected = isResearchMode == research
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        StatGraphWorkspaceTokens.Colors.brandIndigo
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "modeContainerColor",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        Color.White
                    } else {
                        StatGraphWorkspaceTokens.Colors.slate
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "modeContentColor",
                )
                val optionTag = if (research) {
                    "statgraph_mode_option_research"
                } else {
                    "statgraph_mode_option_simple"
                }

                Surface(
                    onClick = { onSelectMode(research) },
                    shape = StatGraphDesignTokens.Shapes.medium,
                    color = containerColor,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag(optionTag)
                        .semantics { selected = isSelected },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartCardTitleAndVerdictHeader(
    title: String,
    verdict: ChartVerdict,
    isResearchMode: Boolean,
    missingRequirementHint: String? = null,
    titleModifier: Modifier = Modifier,
    verdictModifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = StatGraphWorkspaceTokens.Colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = titleModifier.padding(end = 8.dp),
        )
        VerdictPill(
            verdict = verdict,
            isResearchMode = isResearchMode,
            missingRequirementHint = missingRequirementHint,
            modifier = verdictModifier,
        )
    }
}

@Composable
private fun WhyNotChartRow(
    recommendation: ChartRecommendation,
    isResearchMode: Boolean,
) {
    val specId = recommendation.specification.id
    val isSupported = ChartToolkitSupport.isSupported(specId)
    Surface(
        shape = StatGraphDesignTokens.Shapes.large,
        color = StatGraphWorkspaceTokens.Colors.surfaceMuted,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("statgraph_why_not_card_$specId"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChartCardTitleAndVerdictHeader(
                title = recommendation.specification.canonicalName,
                verdict = recommendation.verdict,
                isResearchMode = isResearchMode,
                missingRequirementHint = recommendation.missingRequirements.firstOrNull(),
                titleModifier = Modifier.testTag("statgraph_why_not_title_$specId"),
                verdictModifier = Modifier.testTag("statgraph_why_not_verdict_$specId"),
            )

            Text(
                text = recommendation.summaryReason,
                style = MaterialTheme.typography.bodySmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )

            if (recommendation.missingRequirements.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.statgraph_why_not_what_to_add,
                        recommendation.missingRequirements.joinToString("; "),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                )
            }

            if (!isSupported) {
                Text(
                    text = stringResource(R.string.statgraph_label_coming_soon),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = StatGraphWorkspaceTokens.Colors.onSoftAmber,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveRecommendationDetailCard(
    recommendation: ChartRecommendation,
    isToolkitSupported: Boolean,
    isResearchMode: Boolean = false,
) {
    val palette = StatGraphWorkspaceTokens.verdictPalette(recommendation.verdict)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VerdictPill(
                verdict = recommendation.verdict,
                isResearchMode = isResearchMode,
                missingRequirementHint = recommendation.missingRequirements.firstOrNull(),
            )

            if (isResearchMode) {
                Surface(
                    shape = StatGraphDesignTokens.Shapes.pill,
                    color = StatGraphWorkspaceTokens.Colors.softSlate,
                ) {
                    Text(
                        text = "Fit ${recommendation.scoreBreakdown.finalScore.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatGraphWorkspaceTokens.Colors.ink,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Surface(
                shape = StatGraphDesignTokens.Shapes.pill,
                color = if (isToolkitSupported) {
                    StatGraphWorkspaceTokens.Colors.softMint
                } else {
                    StatGraphWorkspaceTokens.Colors.softAmber
                },
            ) {
                Text(
                    text = if (isToolkitSupported) {
                        stringResource(R.string.statgraph_label_chart_ready)
                    } else {
                        stringResource(R.string.statgraph_label_coming_soon)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isToolkitSupported) {
                        StatGraphWorkspaceTokens.Colors.onSoftMint
                    } else {
                        StatGraphWorkspaceTokens.Colors.onSoftAmber
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        if (recommendation.scoreBreakdown.componentNotes.isNotEmpty()) {
            DetailBulletGroup(
                title = "Why this fits",
                items = recommendation.scoreBreakdown.componentNotes,
            )
        }

        if (recommendation.warnings.isNotEmpty()) {
            DetailBulletGroup(
                title = "Statistical cautions",
                items = recommendation.warnings,
            )
        }

        if (!isToolkitSupported) {
            Surface(
                shape = StatGraphDesignTokens.Shapes.medium,
                color = palette.container.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Chart renderer not yet implemented in toolkit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphWorkspaceTokens.Colors.slate,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun VerdictEvaluationRow(
    recommendation: ChartRecommendation,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    isResearchMode: Boolean = false,
) {
    val palette = StatGraphWorkspaceTokens.verdictPalette(recommendation.verdict)
    val isSupported = ChartToolkitSupport.isSupported(recommendation.specification.id)
    val border = if (isSelected) {
        BorderStroke(1.5.dp, palette.content)
    } else {
        BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle)
    }

    Surface(
        onClick = onSelect,
        enabled = enabled,
        shape = StatGraphDesignTokens.Shapes.large,
        color = if (isSelected) palette.container.copy(alpha = 0.45f) else StatGraphWorkspaceTokens.Colors.surfaceMuted,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            ChartCardTitleAndVerdictHeader(
                title = recommendation.specification.canonicalName,
                verdict = recommendation.verdict,
                isResearchMode = isResearchMode,
                missingRequirementHint = recommendation.missingRequirements.firstOrNull(),
            )

            Text(
                text = if (recommendation.missingRequirements.isNotEmpty()) {
                    "Missing: ${recommendation.missingRequirements.joinToString("; ")}"
                } else {
                    recommendation.summaryReason
                },
                style = MaterialTheme.typography.bodySmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )

            val availability = ChartAvailabilityResolver.resolve(
                specId = recommendation.specification.id,
                verdict = recommendation.verdict,
            )
            when (availability) {
                ChartAvailabilityState.COMING_SOON -> {
                    Surface(
                        shape = StatGraphDesignTokens.Shapes.pill,
                        color = StatGraphWorkspaceTokens.Colors.softAmber,
                    ) {
                        Text(
                            text = stringResource(R.string.statgraph_label_coming_soon),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = StatGraphWorkspaceTokens.Colors.onSoftAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                ChartAvailabilityState.NEEDS_FIELDS -> {
                    Surface(
                        shape = StatGraphDesignTokens.Shapes.pill,
                        color = StatGraphWorkspaceTokens.Colors.brandIndigoSoft,
                    ) {
                        Text(
                            text = stringResource(R.string.statgraph_label_needs_fields),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                ChartAvailabilityState.READY,
                ChartAvailabilityState.NOT_SUITABLE -> Unit
            }
        }
    }
}

private fun verdictDotColor(verdict: ChartVerdict): Color = when (verdict) {
    ChartVerdict.RECOMMENDED -> StatGraphWorkspaceTokens.Colors.success
    ChartVerdict.VALID -> StatGraphWorkspaceTokens.Colors.brandIndigo
    ChartVerdict.VALID_WITH_WARNING -> StatGraphWorkspaceTokens.Colors.warning
    ChartVerdict.NOT_SUITABLE, ChartVerdict.IMPOSSIBLE -> StatGraphWorkspaceTokens.Colors.error
}

@Composable
fun VerdictPill(
    verdict: ChartVerdict,
    modifier: Modifier = Modifier,
    isResearchMode: Boolean = false,
    missingRequirementHint: String? = null,
) {
    val palette = StatGraphWorkspaceTokens.verdictPalette(verdict)
    val icon = when (verdict) {
        ChartVerdict.RECOMMENDED -> Icons.Rounded.Verified
        ChartVerdict.VALID -> Icons.Rounded.CheckCircle
        ChartVerdict.VALID_WITH_WARNING -> Icons.Rounded.WarningAmber
        ChartVerdict.NOT_SUITABLE -> Icons.Rounded.VisibilityOff
        ChartVerdict.IMPOSSIBLE -> Icons.Rounded.Block
    }
    val labelText = if (isResearchMode) {
        verdict.displayName
    } else {
        when (verdict) {
            ChartVerdict.RECOMMENDED -> stringResource(R.string.statgraph_verdict_great_match)
            ChartVerdict.VALID -> stringResource(R.string.statgraph_verdict_works_well)
            ChartVerdict.VALID_WITH_WARNING -> stringResource(R.string.statgraph_verdict_be_careful)
            ChartVerdict.NOT_SUITABLE -> stringResource(R.string.statgraph_verdict_not_good_fit)
            ChartVerdict.IMPOSSIBLE -> if (!missingRequirementHint.isNullOrBlank()) {
                stringResource(R.string.statgraph_verdict_cant_be_drawn, missingRequirementHint)
            } else {
                stringResource(R.string.statgraph_verdict_cant_be_drawn_generic)
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = StatGraphDesignTokens.Shapes.pill,
        color = palette.container,
        border = BorderStroke(1.dp, palette.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.content,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = palette.content,
            )
        }
    }
}

@Composable
private fun DetailBulletGroup(
    title: String,
    items: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = StatGraphWorkspaceTokens.Colors.ink,
            )
        }
        items.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodySmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )
        }
    }
}
