package com.pasindu.nursingotapp.statgraph.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.R
import com.pasindu.nursingotapp.statgraph.data.StarterGoal
import com.pasindu.nursingotapp.statgraph.data.StatGraphStarterTemplate
import com.pasindu.nursingotapp.statgraph.data.StatGraphStarterTemplateId
import com.pasindu.nursingotapp.statgraph.data.StatGraphStarterTemplates
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphWorkspaceTokens

/**
 * Compact one-line guidance strip shown at the top of each workspace stage.
 */
@Composable
fun NextStepGuidanceStrip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = StatGraphDesignTokens.Shapes.medium,
        color = StatGraphWorkspaceTokens.Colors.brandIndigoSoft.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = StatGraphWorkspaceTokens.Colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Chart-reading guidance card shown below supported charts.
 */
@Composable
fun HowToReadChartCard(
    chartName: String,
    howToRead: String,
    plainWhy: String,
    modifier: Modifier = Modifier,
) {
    if (howToRead.isBlank()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MenuBook,
                    contentDescription = null,
                    tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.statgraph_how_to_read_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatGraphWorkspaceTokens.Colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(
                text = chartName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = StatGraphWorkspaceTokens.Colors.brandIndigo,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.statgraph_how_to_read_shows),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatGraphWorkspaceTokens.Colors.slate,
                )
                Text(
                    text = howToRead,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphWorkspaceTokens.Colors.ink,
                )
            }
            if (plainWhy.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.statgraph_how_to_read_look_for),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatGraphWorkspaceTokens.Colors.slate,
                    )
                    Text(
                        text = plainWhy,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphWorkspaceTokens.Colors.ink,
                    )
                }
            }
        }
    }
}

/**
 * Guided Start view headed "What do you want to show?" with 4 goal cards, Blank, and 6 templates.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatGraphGuidedStartView(
    canDismissToWorkspace: Boolean,
    onSelectTemplate: (StatGraphStarterTemplate) -> Unit,
    onDismissToWorkspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "guided_start_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.statgraph_guided_start_heading),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = StatGraphWorkspaceTokens.Colors.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.statgraph_guided_start_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphWorkspaceTokens.Colors.slate,
                    )
                }
                if (canDismissToWorkspace) {
                    IconButton(onClick = onDismissToWorkspace) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.statgraph_help_close),
                            tint = StatGraphWorkspaceTokens.Colors.slate,
                        )
                    }
                }
            }
        }

        items(
            items = StarterGoal.entries,
            key = { "goal_${it.name}" },
        ) { goal ->
            val targetTemplate = remember(goal) {
                StatGraphStarterTemplates.templateById(goal.defaultTemplateId)
            }
            GoalSelectionCard(
                goal = goal,
                icon = goalIcon(goal),
                onClick = { onSelectTemplate(targetTemplate) },
            )
        }

        item(key = "guided_start_blank") {
            val blankTemplate = remember {
                StatGraphStarterTemplates.templateById(StatGraphStarterTemplateId.BLANK)
            }
            Surface(
                onClick = { onSelectTemplate(blankTemplate) },
                shape = StatGraphDesignTokens.Shapes.large,
                color = StatGraphWorkspaceTokens.Colors.surface,
                border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddCircleOutline,
                        contentDescription = null,
                        tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.statgraph_guided_start_blank_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatGraphWorkspaceTokens.Colors.ink,
                        )
                        Text(
                            text = stringResource(R.string.statgraph_guided_start_blank_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphWorkspaceTokens.Colors.slate,
                        )
                    }
                }
            }
        }

        item(key = "guided_start_templates_header") {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.statgraph_guided_start_templates_heading),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = StatGraphWorkspaceTokens.Colors.ink,
                modifier = Modifier.semantics { heading() },
            )
        }

        items(
            items = StatGraphStarterTemplates.allTemplates,
            key = { "tpl_${it.id.name}" },
        ) { template ->
            StarterTemplateCard(
                template = template,
                onClick = { onSelectTemplate(template) },
            )
        }

        item(key = "guided_start_bottom_spacer") {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GoalSelectionCard(
    goal: StarterGoal,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = StatGraphDesignTokens.Shapes.extraLarge,
        color = StatGraphWorkspaceTokens.Colors.surface,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StatGraphWorkspaceTokens.Colors.brandIndigoSoft,
                modifier = Modifier.size(42.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatGraphWorkspaceTokens.Colors.ink,
                )
                Text(
                    text = goal.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphWorkspaceTokens.Colors.slate,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StarterTemplateCard(
    template: StatGraphStarterTemplate,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = StatGraphDesignTokens.Shapes.large,
        color = StatGraphWorkspaceTokens.Colors.surfaceMuted,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = StatGraphWorkspaceTokens.Colors.ink,
                    modifier = Modifier.weight(1f),
                )
                if (template.isFictionalSample) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = StatGraphDesignTokens.Shapes.pill,
                        color = StatGraphWorkspaceTokens.Colors.softAmber,
                    ) {
                        Text(
                            text = stringResource(R.string.statgraph_label_fictional_sample),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = StatGraphWorkspaceTokens.Colors.onSoftAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Text(
                text = template.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )
            if (template.samplePreviewLines.isNotEmpty()) {
                Surface(
                    shape = StatGraphDesignTokens.Shapes.medium,
                    color = StatGraphWorkspaceTokens.Colors.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        template.samplePreviewLines.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.labelSmall,
                                color = StatGraphWorkspaceTokens.Colors.slate,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun goalIcon(goal: StarterGoal): ImageVector = when (goal) {
    StarterGoal.CHANGE_OVER_TIME -> Icons.AutoMirrored.Rounded.ShowChart
    StarterGoal.COMPARE_GROUPS -> Icons.Rounded.BarChart
    StarterGoal.SHARE_OF_TOTAL -> Icons.Rounded.PieChart
    StarterGoal.RESEARCH_RANGES -> Icons.Rounded.Science
}

/**
 * First-run 3-card introduction dialog (shown once on first use, skippable).
 */
@Composable
fun StatGraphFirstRunIntroDialog(
    onSkip: () -> Unit,
    onComplete: () -> Unit,
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val cards = listOf(
        stringResource(R.string.statgraph_intro_card1_title) to stringResource(R.string.statgraph_intro_card1_body),
        stringResource(R.string.statgraph_intro_card2_title) to stringResource(R.string.statgraph_intro_card2_body),
        stringResource(R.string.statgraph_intro_card3_title) to stringResource(R.string.statgraph_intro_card3_body),
    )
    val (title, body) = cards[stepIndex]

    AlertDialog(
        onDismissRequest = onSkip,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.statgraph_intro_step_of, stepIndex + 1),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (stepIndex < cards.lastIndex) {
                Button(
                    onClick = { stepIndex += 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        contentColor = StatGraphWorkspaceTokens.Colors.onBrandIndigo,
                    ),
                ) {
                    Text(stringResource(R.string.statgraph_intro_continue))
                }
            } else {
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        contentColor = StatGraphWorkspaceTokens.Colors.onBrandIndigo,
                    ),
                ) {
                    Text(stringResource(R.string.statgraph_intro_done))
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onSkip) {
                Text(stringResource(R.string.statgraph_intro_skip))
            }
        },
    )
}

/**
 * Contextual help topics for non-obvious statistical / data concepts.
 */
enum class ContextualHelpTopic(
    val title: String,
    val explanation: String,
) {
    COUNT_VS_PART(
        title = "Count vs. part of a total",
        explanation = "A Count is the whole number (such as all ward admissions). A Part of a total is a subgroup inside that whole (such as dengue patients out of all admissions).",
    ),
    PERCENTAGE_VS_COUNT(
        title = "Percentage vs. count",
        explanation = "Counts show how many people or events happened. Percentages divide the part by the total so you can compare wards or months of different sizes fairly.",
    ),
    DATE_TIME_COLUMNS(
        title = "Date & time columns",
        explanation = "Use a Date or month column when each row belongs to a day, week, month, or year. This lets Stat Graph Studio put your points in chronological order.",
    ),
    DENOMINATOR_AND_RATE(
        title = "Denominator & rate",
        explanation = "The denominator is the total group at risk (such as total admissions). Dividing a part count by its denominator produces a rate or percentage.",
    ),
    MISSING_VALUES(
        title = "Missing values",
        explanation = "Blank cells are kept as missing rather than quietly turned into zero. Missing values are flagged in Data checks so you can tell 'not recorded' apart from '0'.",
    ),
    GROUP_CATEGORY_FIELDS(
        title = "Group & category fields",
        explanation = "Use Name or group (Category) for the main labels like Ward, and Group / series split when you want to break each bar or line into subgroups like Sex.",
    )
}

@Composable
fun ContextualHelpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(28.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
            contentDescription = stringResource(R.string.statgraph_help_cd),
            tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun StatGraphContextualHelpDialog(
    topic: ContextualHelpTopic,
    dataset: StatDataset?,
    onDismiss: () -> Unit,
) {
    val exampleText = remember(topic, dataset) {
        buildContextualExample(topic, dataset)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = topic.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (exampleText != null) {
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = stringResource(R.string.statgraph_help_example_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        )
                        Text(
                            text = exampleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.statgraph_help_close))
            }
        },
    )
}

private fun buildContextualExample(
    topic: ContextualHelpTopic,
    dataset: StatDataset?,
): String? {
    if (dataset == null || dataset.fields.isEmpty()) return null
    val measureField = dataset.fields.firstOrNull { it.role is FieldRole.Measure }
    val partField = dataset.fields.firstOrNull { it.role is FieldRole.PartOfMeasure }
    val rateField = dataset.fields.firstOrNull { it.role is FieldRole.RateContinuous }
    val timeField = dataset.fields.firstOrNull { it.role is FieldRole.Time }
    val categoryField = dataset.fields.firstOrNull { it.role is FieldRole.Category }
    val groupField = dataset.fields.firstOrNull { it.role is FieldRole.Group }

    return when (topic) {
        ContextualHelpTopic.COUNT_VS_PART -> when {
            partField != null && measureField != null ->
                "'${partField.name}' is part of '${measureField.name}'."
            measureField != null ->
                "'${measureField.name}' is currently set as a total count."
            else -> null
        }
        ContextualHelpTopic.PERCENTAGE_VS_COUNT -> when {
            rateField != null && measureField != null ->
                "'${rateField.name}' is a rate/percentage, while '${measureField.name}' is a count."
            partField != null && measureField != null ->
                "You can calculate a % from '${partField.name}' divided by '${measureField.name}'."
            else -> null
        }
        ContextualHelpTopic.DATE_TIME_COLUMNS -> timeField?.let {
            "'${it.name}' is your ${(it.role as FieldRole.Time).granularity.displayName.lowercase()} column."
        }
        ContextualHelpTopic.DENOMINATOR_AND_RATE -> when {
            partField != null && measureField != null ->
                "In this dataset, '${partField.name}' is the numerator and '${measureField.name}' is the denominator."
            measureField != null ->
                "'${measureField.name}' can act as the denominator when calculating a rate."
            else -> null
        }
        ContextualHelpTopic.MISSING_VALUES -> {
            val missingCount = dataset.rows.sumOf { row ->
                dataset.fields.count { f -> row.values[f.id] == null || row.values[f.id] is com.pasindu.nursingotapp.statgraph.model.StatValue.Missing }
            }
            "This table currently has $missingCount blank cell(s) across ${dataset.rows.size} row(s)."
        }
        ContextualHelpTopic.GROUP_CATEGORY_FIELDS -> when {
            categoryField != null && groupField != null ->
                "'${categoryField.name}' is the main label and '${groupField.name}' splits each group."
            categoryField != null ->
                "'${categoryField.name}' is your category label column."
            groupField != null ->
                "'${groupField.name}' is your group split column."
            else -> null
        }
    }
}
