package com.pasindu.nursingotapp.statgraph.ui.workspace

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pasindu.nursingotapp.statgraph.data.repository.SavedStatDatasetSummary
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.R
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartPaletteTokens
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.rememberStatGraphChartStyle
import com.pasindu.nursingotapp.statgraph.data.CsvColumnOverride
import com.pasindu.nursingotapp.statgraph.data.StatGraphPreferences
import com.pasindu.nursingotapp.statgraph.data.StatGraphSampleData
import com.pasindu.nursingotapp.statgraph.data.StatGraphStarterTemplate
import com.pasindu.nursingotapp.statgraph.export.StatGraphExportBottomSheet
import com.pasindu.nursingotapp.statgraph.export.StatGraphExportPayload
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.orchestration.ChartRenderState
import com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryController
import com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryTab
import com.pasindu.nursingotapp.statgraph.ui.StatGraphOrchestratedView
import com.pasindu.nursingotapp.statgraph.ui.components.HowToReadChartCard
import com.pasindu.nursingotapp.statgraph.ui.components.NextStepGuidanceStrip
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphCsvImportSection
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphFirstRunIntroDialog
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphGuidedStartView
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphDerivedFieldCard
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphDerivedFieldDialog
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphFieldEditorDialog
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphInsightsSection
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphMappingControlsCard
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphPrivacyBanner
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphRecommendationSection
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphRowCard
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphSchemaFieldCard
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphSectionHeader
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphValidationBanner
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphWorkspaceHeader
import com.pasindu.nursingotapp.statgraph.ui.state.StatGraphWorkspaceController
import com.pasindu.nursingotapp.statgraph.ui.state.StatGraphWorkspaceSamples
import com.pasindu.nursingotapp.statgraph.ui.state.StatGraphWorkspaceUiState
import com.pasindu.nursingotapp.statgraph.ui.state.WorkspaceSamplePreset
import com.pasindu.nursingotapp.statgraph.ui.state.WorkspaceSection
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphWorkspaceTokens
import com.pasindu.nursingotapp.ui.theme.NursingOTAppTheme

/**
 * Optional persistence controls supplied when [StatGraphStudioWorkspaceContent] is bound
 * to [StatGraphStudioViewModel] and Room storage. Standalone callers keep this null.
 */
data class StatGraphPersistenceUiControls(
    val savedDatasets: List<SavedStatDatasetSummary>,
    val isInitializing: Boolean,
    val isDemoMode: Boolean,
    val activeDatasetId: String?,
    val saveStatus: StatGraphSaveStatus,
    val errorMessage: String?,
    val onSelectSavedDataset: (String) -> Unit,
    val onRequestDeleteDataset: (String) -> Unit,
    val onCreateNewDataset: () -> Unit,
    val onSaveDemoCopy: () -> Unit,
    val onDismissError: () -> Unit,
    val onLoadStarterTemplate: ((StatGraphStarterTemplate) -> Unit)? = null,
)

/**
 * Navigation-bound persistent route for Stat Graph Studio powered by [StatGraphStudioViewModel].
 */
@Composable
fun StatGraphStudioRoute(
    viewModel: StatGraphStudioViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val holder by viewModel.uiState.collectAsState()

    val controls = StatGraphPersistenceUiControls(
        savedDatasets = holder.savedDatasets,
        isInitializing = holder.isInitializing,
        isDemoMode = holder.isDemoMode,
        activeDatasetId = holder.activePersistedDatasetId,
        saveStatus = holder.saveStatus,
        errorMessage = holder.errorMessage,
        onSelectSavedDataset = viewModel::selectSavedDataset,
        onRequestDeleteDataset = viewModel::requestDeleteDataset,
        onCreateNewDataset = { viewModel.createBlankDataset() },
        onSaveDemoCopy = viewModel::saveDemoCopyToLibrary,
        onDismissError = viewModel::dismissError,
        onLoadStarterTemplate = viewModel::loadStarterTemplate,
    )

    StatGraphStudioWorkspaceContent(
        state = holder.workspace,
        onStateChange = viewModel::onWorkspaceStateChange,
        onNavigateBack = onNavigateBack,
        persistenceControls = controls,
        modifier = modifier,
    )

    val pendingDeleteId = holder.pendingDeleteDatasetId
    if (pendingDeleteId != null) {
        val targetName = holder.savedDatasets.firstOrNull { it.id == pendingDeleteId }?.name
            ?.ifBlank { "Untitled Dataset" }
            ?: "this saved dataset"
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("Delete saved dataset?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Remove \"$targetName\" from local Stat Graph Studio storage? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteDataset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatGraphWorkspaceTokens.Colors.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Complete standalone Phase 5 Stat Graph Studio Workspace Screen.
 *
 * Unifies Phase 1 data/CSV entry & validation, Phase 2 intelligent chart recommendations
 * and statistics/diagnostics, Phase 3 interactive canvas charts, and Phase 4 orchestration
 * in an adaptive light-mode clinical workspace aligned with NursingOTApp.
 */
@Composable
fun StatGraphStudioWorkspaceScreen(
    initialDataset: StatDataset = StatGraphWorkspaceSamples.dengueFourMonthDemoDataset(),
    initialSection: WorkspaceSection? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var workspaceState by remember(initialDataset.id, initialSection) {
        mutableStateOf(
            StatGraphWorkspaceController.createInitial(
                dataset = initialDataset,
                activeSection = initialSection,
            ),
        )
    }

    StatGraphStudioWorkspaceContent(
        state = workspaceState,
        onStateChange = { workspaceState = it },
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatGraphStudioWorkspaceContent(
    state: StatGraphWorkspaceUiState,
    onStateChange: (StatGraphWorkspaceUiState) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    persistenceControls: StatGraphPersistenceUiControls? = null,
    modifier: Modifier = Modifier,
) {
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var libraryDialogOpen by remember { mutableStateOf(false) }
    var quickGuideDialogOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember(context) { StatGraphPreferences(context) }
    var showIntroDialog by remember { mutableStateOf(!prefs.hasSeenIntro()) }
    LaunchedEffect(prefs) {
        val persistedPalette = StatGraphChartPalette.fromId(prefs.getSelectedPaletteId())
        if (persistedPalette != null && state.selectedPalette == null) {
            onStateChange(StatGraphWorkspaceController.setPalette(state, persistedPalette))
        }
    }
    val activePalette = StatGraphChartPalette.resolve(
        selected = state.selectedPalette,
        isResearchMode = state.isResearchMode,
    )
    val chartStyle = rememberStatGraphChartStyle(
        seriesColors = ChartPaletteTokens.colorsFor(activePalette),
        palette = activePalette,
    )

    val categoryOptionsByField = remember(state.dataset.fields, state.dataset.rows) {
        state.dataset.fields
            .filter { it.role is FieldRole.Category || it.role is FieldRole.Group }
            .associate { field ->
                val observed = state.dataset.rows.mapNotNull { row ->
                    (row.values[field.id] as? StatValue.TextValue)?.value?.trim()?.takeIf { it.isNotEmpty() }
                }
                field.id to (field.allowedCategories + observed).distinct()
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StatGraphWorkspaceTokens.Colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Stat Graph Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val subtitleText = when (val status = persistenceControls?.saveStatus) {
                            null -> "Clinical Statistical Exploration Workspace"
                            StatGraphSaveStatus.Initializing -> "Restoring saved workspace..."
                            StatGraphSaveStatus.DemoPreview -> "Demo showcase · Save copy or edit to store"
                            StatGraphSaveStatus.PendingChanges -> "Saving changes locally..."
                            StatGraphSaveStatus.Saving -> "Saving to device..."
                            is StatGraphSaveStatus.Saved -> "Saved locally on device"
                            is StatGraphSaveStatus.Error -> "Save error · ${status.message}"
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (persistenceControls?.saveStatus is StatGraphSaveStatus.Error) {
                                StatGraphWorkspaceTokens.Colors.error
                            } else {
                                StatGraphWorkspaceTokens.Colors.slate
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { overflowMenuExpanded = true },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More studio options",
                                tint = StatGraphWorkspaceTokens.Colors.ink,
                            )
                        }
                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = { overflowMenuExpanded = false },
                        ) {
                            if (persistenceControls != null) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                                    },
                                    text = {
                                        val countSuffix = if (persistenceControls.savedDatasets.isEmpty()) {
                                            ""
                                        } else {
                                            " (${persistenceControls.savedDatasets.size})"
                                        }
                                        Text("Library$countSuffix", fontWeight = FontWeight.SemiBold)
                                    },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        libraryDialogOpen = true
                                    },
                                )
                            }
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                },
                                text = {
                                    Text(
                                        stringResource(R.string.statgraph_menu_guided_start),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onStateChange(
                                        StatGraphWorkspaceController.setShowGuidedStart(state, true),
                                    )
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                },
                                text = {
                                    Text("New dataset", fontWeight = FontWeight.SemiBold)
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    if (persistenceControls != null) {
                                        persistenceControls.onCreateNewDataset()
                                    } else {
                                        onStateChange(
                                            StatGraphWorkspaceController.fromPreset(WorkspaceSamplePreset.BLANK),
                                        )
                                    }
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Rounded.Tune, contentDescription = null)
                                },
                                text = {
                                    Text(
                                        text = stringResource(
                                            if (state.isResearchMode) {
                                                R.string.statgraph_menu_switch_to_simple
                                            } else {
                                                R.string.statgraph_menu_switch_to_research
                                            },
                                        ),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onStateChange(
                                        StatGraphWorkspaceController.setResearchMode(
                                            state,
                                            !state.isResearchMode,
                                        ),
                                    )
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Rounded.HelpOutline, contentDescription = null)
                                },
                                text = {
                                    Text("Quick Guide", fontWeight = FontWeight.SemiBold)
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    quickGuideDialogOpen = true
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StatGraphWorkspaceTokens.Colors.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StatGraphWorkspaceTokens.Colors.background),
        ) {
            val persistenceError = persistenceControls?.errorMessage
            if (persistenceError != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = StatGraphDesignTokens.Shapes.medium,
                    color = StatGraphWorkspaceTokens.Colors.softRose,
                    border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.error.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = persistenceError,
                            style = MaterialTheme.typography.labelMedium,
                            color = StatGraphWorkspaceTokens.Colors.onSoftRose,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = persistenceControls.onDismissError) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss error",
                                tint = StatGraphWorkspaceTokens.Colors.onSoftRose,
                            )
                        }
                    }
                }
            }

            if (persistenceControls?.isInitializing == true) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        )
                        Text(
                            text = "Loading saved datasets...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatGraphWorkspaceTokens.Colors.slate,
                        )
                    }
                }
            } else if (state.showGuidedStart) {
                StatGraphGuidedStartView(
                    canDismissToWorkspace = true,
                    onSelectTemplate = { template ->
                        val handler = persistenceControls?.onLoadStarterTemplate
                        if (handler != null) {
                            handler(template)
                        } else {
                            onStateChange(
                                StatGraphWorkspaceController.fromStarterTemplate(
                                    template = template,
                                    isResearchMode = state.isResearchMode,
                                ),
                            )
                        }
                    },
                    onDismissToWorkspace = {
                        onStateChange(StatGraphWorkspaceController.setShowGuidedStart(state, false))
                    },
                )
            } else {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val isCompactPhone = maxWidth < 600.dp
                    val isExpandedTablet = maxWidth >= 720.dp
                    val savedDatasetCount = (persistenceControls?.savedDatasets?.size ?: 1).coerceAtLeast(1)

                    if (isExpandedTablet) {
                        TabletTwoPaneWorkspace(
                            state = state,
                            datasetCount = savedDatasetCount,
                            categoryOptionsByField = categoryOptionsByField,
                            onStateChange = onStateChange,
                        )
                    } else {
                        CompactOrMediumWorkspace(
                            state = state,
                            compactMode = isCompactPhone,
                            datasetCount = savedDatasetCount,
                            categoryOptionsByField = categoryOptionsByField,
                            onStateChange = onStateChange,
                        )
                    }
                }
            }
        }
    }

    if (showIntroDialog) {
        StatGraphFirstRunIntroDialog(
            onSkip = {
                prefs.setHasSeenIntro(true)
                showIntroDialog = false
            },
            onComplete = {
                prefs.setHasSeenIntro(true)
                showIntroDialog = false
            },
        )
    }

    if (libraryDialogOpen && persistenceControls != null) {
        AlertDialog(
            onDismissRequest = { libraryDialogOpen = false },
            title = { Text("Dataset Library", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (persistenceControls.isDemoMode) {
                        Button(
                            onClick = {
                                libraryDialogOpen = false
                                persistenceControls.onSaveDemoCopy()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save Demo Copy to Library", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (persistenceControls.savedDatasets.isEmpty()) {
                        Text(
                            text = "No saved datasets yet. Edit or import a CSV to save locally on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphWorkspaceTokens.Colors.slate,
                        )
                    } else {
                        persistenceControls.savedDatasets.forEach { summary ->
                            val isActive =
                                !persistenceControls.isDemoMode &&
                                    summary.id == persistenceControls.activeDatasetId
                            Surface(
                                onClick = {
                                    libraryDialogOpen = false
                                    persistenceControls.onSelectSavedDataset(summary.id)
                                },
                                shape = StatGraphDesignTokens.Shapes.medium,
                                color = if (isActive) {
                                    StatGraphWorkspaceTokens.Colors.brandIndigoSoft
                                } else {
                                    StatGraphWorkspaceTokens.Colors.surfaceMuted
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isActive) {
                                        StatGraphWorkspaceTokens.Colors.brandIndigo.copy(alpha = 0.35f)
                                    } else {
                                        StatGraphWorkspaceTokens.Colors.borderSubtle
                                    },
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = summary.name.ifBlank { "Untitled Dataset" },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            color = StatGraphWorkspaceTokens.Colors.ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = summary.sourceLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatGraphWorkspaceTokens.Colors.slate,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            libraryDialogOpen = false
                                            persistenceControls.onRequestDeleteDataset(summary.id)
                                        },
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete ${summary.name}",
                                            tint = StatGraphWorkspaceTokens.Colors.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { libraryDialogOpen = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("Close")
                }
            },
        )
    }

    if (quickGuideDialogOpen) {
        AlertDialog(
            onDismissRequest = { quickGuideDialogOpen = false },
            title = { Text("Stat Graph Studio Quick Guide", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Data — Enter rows, import a CSV, or verify column roles (Category, Time, Measure, Group).",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphWorkspaceTokens.Colors.ink,
                    )
                    Text(
                        text = "2. Chart — Explore recommended charts with suitability verdicts, tap any chart element to inspect exact values.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphWorkspaceTokens.Colors.ink,
                    )
                    Text(
                        text = "3. Stats — Review descriptive summaries, missing-value diagnostics, and small-sample privacy alerts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphWorkspaceTokens.Colors.ink,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { quickGuideDialogOpen = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("Got it")
                }
            },
        )
    }

    if (state.entryState.isFieldDialogOpen) {
        StatGraphFieldEditorDialog(
            initialField = state.entryState.editingField,
            existingFields = state.dataset.fields,
            onSave = { field ->
                onStateChange(
                    StatGraphWorkspaceController.updateEntryState(
                        state,
                        StatGraphEntryController.saveField(state.entryState, field),
                    ),
                )
            },
            onDismiss = {
                onStateChange(
                    StatGraphWorkspaceController.updateEntryState(
                        state,
                        state.entryState.copy(isFieldDialogOpen = false, editingField = null),
                    ),
                )
            },
        )
    }

    if (state.entryState.isDerivedDialogOpen) {
        StatGraphDerivedFieldDialog(
            fields = state.dataset.fields,
            onSave = { def ->
                onStateChange(
                    StatGraphWorkspaceController.updateEntryState(
                        state,
                        StatGraphEntryController.addDerivedField(state.entryState, def),
                    ),
                )
            },
            onDismiss = {
                onStateChange(
                    StatGraphWorkspaceController.updateEntryState(
                        state,
                        state.entryState.copy(isDerivedDialogOpen = false),
                    ),
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactOrMediumWorkspace(
    state: StatGraphWorkspaceUiState,
    compactMode: Boolean,
    datasetCount: Int,
    categoryOptionsByField: Map<String, List<String>>,
    onStateChange: (StatGraphWorkspaceUiState) -> Unit,
) {
    val horizontalPad = if (compactMode) 14.dp else 20.dp

    Column(modifier = Modifier.fillMaxSize()) {
        // Section Navigation Bar
        Surface(
            color = StatGraphWorkspaceTokens.Colors.background,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = 6.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                WorkspaceSection.entries.forEachIndexed { index, section ->
                    SegmentedButton(
                        selected = state.activeSection == section,
                        onClick = { onStateChange(StatGraphWorkspaceController.setSection(state, section)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = WorkspaceSection.entries.size,
                        ),
                        label = {
                            val tabLabel = when (section) {
                                WorkspaceSection.DATA -> stringResource(
                                    if (compactMode) R.string.statgraph_tab_data_short else R.string.statgraph_tab_data_full,
                                )
                                WorkspaceSection.EXPLORE -> stringResource(
                                    if (compactMode) R.string.statgraph_tab_chart_short else R.string.statgraph_tab_chart_full,
                                )
                                WorkspaceSection.INSIGHTS -> stringResource(
                                    if (compactMode) R.string.statgraph_tab_summary_short else R.string.statgraph_tab_summary_full,
                                )
                            }
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }

        Crossfade(
            targetState = state.activeSection,
            animationSpec = tween(durationMillis = 180),
            label = "workspaceSectionTransition",
        ) { section ->
            when (section) {
                WorkspaceSection.DATA -> DataWorkspaceColumn(
                    state = state,
                    compactMode = compactMode,
                    horizontalPad = horizontalPad,
                    datasetCount = datasetCount,
                    categoryOptionsByField = categoryOptionsByField,
                    showAnalyseCta = true,
                    onStateChange = onStateChange,
                )

                WorkspaceSection.EXPLORE -> ExploreWorkspaceColumn(
                    state = state,
                    compactMode = compactMode,
                    horizontalPad = horizontalPad,
                    onStateChange = onStateChange,
                )

                WorkspaceSection.INSIGHTS -> InsightsWorkspaceColumn(
                    state = state,
                    compactMode = compactMode,
                    horizontalPad = horizontalPad,
                    onStateChange = onStateChange,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletTwoPaneWorkspace(
    state: StatGraphWorkspaceUiState,
    datasetCount: Int,
    categoryOptionsByField: Map<String, List<String>>,
    onStateChange: (StatGraphWorkspaceUiState) -> Unit,
) {
    val leftModes = StatGraphEntryTab.entries
    val context = LocalContext.current
    val prefs = remember(context) { StatGraphPreferences(context) }
    val activePalette = StatGraphChartPalette.resolve(
        selected = state.selectedPalette,
        isResearchMode = state.isResearchMode,
    )
    val chartStyle = rememberStatGraphChartStyle(
        seriesColors = ChartPaletteTokens.colorsFor(activePalette),
        palette = activePalette,
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Left Pane: Workspace Header, Privacy, Data Entry / Schema / CSV
        LazyColumn(
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "tablet_header") {
                StatGraphWorkspaceHeader(
                    datasetName = state.dataset.name,
                    datasetDescription = state.dataset.description,
                    sourceLabel = state.dataset.metadata.sourceLabel,
                    summaryMetrics = state.entryState.summaryMetrics,
                    validationReport = state.entryState.validationReport,
                    diagnosticsReport = state.diagnosticsReport,
                    unconfirmedRoleCount = state.unconfirmedRoleCount,
                    compactMode = false,
                    datasetCount = datasetCount,
                    eligibleChartCount = state.recommendationSuite.recommendedCharts.size +
                        state.recommendationSuite.validCharts.size +
                        state.recommendationSuite.warningCharts.size,
                    onCreateBlankDataset = {
                        onStateChange(StatGraphWorkspaceController.fromPreset(WorkspaceSamplePreset.BLANK))
                    },
                    onImportCsv = {
                        onStateChange(StatGraphWorkspaceController.openCsvImport(state))
                    },
                    onLoadDengueDemo = {
                        onStateChange(
                            StatGraphWorkspaceController.fromPreset(WorkspaceSamplePreset.DENGUE_FOUR_MONTH_DEMO),
                        )
                    },
                )
            }

            item(key = "tablet_privacy") {
                StatGraphPrivacyBanner(smallCountCellCount = state.entryState.summaryMetrics.smallCountCellCount)
            }

            item(key = "tablet_data_tabs") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    leftModes.forEachIndexed { idx, tab ->
                        SegmentedButton(
                            selected = state.entryState.activeTab == tab,
                            onClick = {
                                onStateChange(
                                    StatGraphWorkspaceController.updateEntryState(
                                        state,
                                        state.entryState.copy(activeTab = tab),
                                    ),
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(idx, leftModes.size),
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }

            if (state.unconfirmedRoleCount > 0) {
                item(key = "tablet_confirm_roles") {
                    RoleConfirmationPromptBanner(
                        unconfirmedCount = state.unconfirmedRoleCount,
                        onConfirmAll = {
                            onStateChange(StatGraphWorkspaceController.confirmAllSuggestedRoles(state))
                        },
                    )
                }
            }

            when (state.entryState.activeTab) {
                StatGraphEntryTab.ROWS -> {
                    item(key = "tablet_validation_banner") {
                        StatGraphValidationBanner(report = state.entryState.validationReport)
                    }
                    if (state.dataset.fields.isNotEmpty()) {
                        item(key = "tablet_row_controls") {
                            WorkspaceRowControlsBar(
                                hasTimeField = state.dataset.fields.any { it.role is FieldRole.Time },
                                hasFields = true,
                                onAddRow = {
                                    onStateChange(
                                        StatGraphWorkspaceController.updateEntryState(
                                            state,
                                            StatGraphEntryController.addBlankRow(state.entryState),
                                        ),
                                    )
                                },
                                onAddNextPeriod = {
                                    onStateChange(
                                        StatGraphWorkspaceController.updateEntryState(
                                            state,
                                            StatGraphEntryController.addNextPeriodRow(state.entryState),
                                        ),
                                    )
                                },
                                onOpenAddField = {
                                    onStateChange(
                                        StatGraphWorkspaceController.updateEntryState(
                                            state,
                                            state.entryState.copy(isFieldDialogOpen = true, editingField = null),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                    if (state.dataset.fields.isEmpty() || state.dataset.rows.isEmpty()) {
                        item(key = "tablet_empty_rows") {
                            WorkspaceEmptyDataCard(
                                hasFields = state.dataset.fields.isNotEmpty(),
                                onAddField = {
                                    onStateChange(
                                        StatGraphWorkspaceController.updateEntryState(
                                            state,
                                            state.entryState.copy(isFieldDialogOpen = true, editingField = null),
                                        ),
                                    )
                                },
                                onAddRow = {
                                    onStateChange(
                                        StatGraphWorkspaceController.updateEntryState(
                                            state,
                                            StatGraphEntryController.addBlankRow(state.entryState),
                                        ),
                                    )
                                },
                                onLoadDemo = {
                                    onStateChange(
                                        StatGraphWorkspaceController.fromPreset(
                                            WorkspaceSamplePreset.DENGUE_FOUR_MONTH_DEMO,
                                        ),
                                    )
                                },
                                onOpenCsv = {
                                    onStateChange(StatGraphWorkspaceController.openCsvImport(state))
                                },
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = state.dataset.rows,
                            key = { _, row -> "tablet_${row.id}" },
                        ) { index, row ->
                            StatGraphRowCard(
                                rowNumber = index + 1,
                                row = row,
                                fields = state.dataset.fields,
                                derivedDefinitions = state.dataset.derivedFields,
                                derivedResultsByField = state.entryState.derivedResults,
                                rowIssues = state.entryState.validationReport.issuesForRow(row.id),
                                categoryOptionsByField = categoryOptionsByField,
                                onValueChange = { fieldId, updated ->
                                    onStateChange(
                                        StatGraphWorkspaceController.updateEntryState(
                                            state,
                                            StatGraphEntryController.updateCellValue(
                                                state.entryState,
                                                row.id,
                                                fieldId,
                                                updated,
                                            ),
                                        ),
                                    )
                                },
                                onDuplicate = {
                                    onStateChange(
                                        StatGraphWorkspaceController.updateEntryState(
                                            state,
                                            StatGraphEntryController.duplicateRow(state.entryState, row.id),
                                        ),
                                    )
                                },
                                onDelete = {
                                    onStateChange(
                                        StatGraphWorkspaceController.updateEntryState(
                                            state,
                                            StatGraphEntryController.deleteRow(state.entryState, row.id),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }

                StatGraphEntryTab.SETUP -> {
                    item(key = "tablet_metadata") {
                        WorkspaceMetadataCard(
                            name = state.dataset.name,
                            description = state.dataset.description,
                            onUpdate = { name, desc ->
                                onStateChange(
                                    StatGraphWorkspaceController.updateEntryState(
                                        state,
                                        StatGraphEntryController.updateDatasetMeta(state.entryState, name, desc),
                                    ),
                                )
                            },
                        )
                    }
                    item(key = "tablet_schema_derived") {
                        WorkspaceSchemaAndDerivedBlock(state = state, onStateChange = onStateChange)
                    }
                }

                StatGraphEntryTab.CSV -> {
                    item(key = "tablet_csv") {
                        WorkspaceCsvImportBlock(state = state, onStateChange = onStateChange)
                    }
                }
            }
        }

        // Right Pane: Interactive Chart Exploration, Mapping, Recommendation Intelligence & Statistics
        LazyColumn(
            modifier = Modifier
                .weight(0.56f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "tablet_chart_host") {
                val readyState = state.renderState as? ChartRenderState.Ready
                val activeSpecId = readyState?.configuration?.specification?.id
                var exportSheetOpen by remember { mutableStateOf(false) }
                StatGraphOrchestratedView(
                    dataset = state.dataset,
                    selection = state.mappingSelection,
                    isLoading = state.isLoading,
                    style = chartStyle,
                    onSelectAlternativeChart = { altSpecId ->
                        onStateChange(StatGraphWorkspaceController.selectChartSpec(state, altSpecId))
                    },
                )
                if (readyState != null && readyState.renderData.hasDrawablePoints) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { exportSheetOpen = true },
                        shape = StatGraphDesignTokens.Shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
                            contentColor = StatGraphWorkspaceTokens.Colors.onBrandIndigo,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Download", fontWeight = FontWeight.SemiBold)
                    }
                    if (exportSheetOpen) {
                        val isTimeOrdered = state.dataset.fields.any {
                            it.role is com.pasindu.nursingotapp.statgraph.model.FieldRole.Time
                        }
                        val lineTargetSpecId = if (readyState.renderData.series.size > 1) {
                            "multi_series_line_chart"
                        } else {
                            "line_chart"
                        }
                        StatGraphExportBottomSheet(
                            payload = StatGraphExportPayload(
                                renderData = readyState.renderData,
                                chartStyle = chartStyle,
                                howToRead = readyState.configuration.specification.howToRead,
                                plainWhy = readyState.configuration.specification.plainWhy,
                                sourceLabel = state.dataset.metadata.sourceLabel,
                                clinicalNotes = readyState.configuration.clinicalNotes,
                                exportDateText = java.time.LocalDate.now().toString(),
                                isOrderedTimeSeries = isTimeOrdered,
                            ),
                            onDismiss = { exportSheetOpen = false },
                            onSwitchToLineChart = if (isTimeOrdered) {
                                { onStateChange(StatGraphWorkspaceController.selectChartSpec(state, lineTargetSpecId)) }
                            } else {
                                null
                            },
                            onGroupIntoFourWeekBlocks = {
                                onStateChange(StatGraphWorkspaceController.groupIntoFourWeekBlocks(state))
                            },
                        )
                    }
                }
                val readySpec = readyState?.configuration?.specification
                if (readySpec != null && readySpec.howToRead.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    HowToReadChartCard(
                        chartName = readySpec.canonicalName,
                        howToRead = readySpec.howToRead,
                        plainWhy = readySpec.plainWhy,
                    )
                }
                if (activeSpecId == null && state.renderState is ChartRenderState.NoSuitableRecommendation) {
                    Spacer(Modifier.height(6.dp))
                }
            }

            item(key = "tablet_mapping_controls") {
                StatGraphMappingControlsCard(
                    dataset = state.dataset,
                    selection = state.mappingSelection,
                    capability = state.mappingCapability,
                    renderState = state.renderState,
                    hasCustomMapping = state.hasCustomMapping,
                    onSelectXField = { fieldId ->
                        onStateChange(StatGraphWorkspaceController.setXField(state, fieldId))
                    },
                    onToggleYField = { fieldId ->
                        onStateChange(StatGraphWorkspaceController.toggleYField(state, fieldId))
                    },
                    onToggleDerivedField = { derivedId ->
                        onStateChange(StatGraphWorkspaceController.toggleDerivedField(state, derivedId))
                    },
                    onSelectGroupField = { grpId ->
                        onStateChange(StatGraphWorkspaceController.setGroupField(state, grpId))
                    },
                    onResetMappings = {
                        onStateChange(StatGraphWorkspaceController.resetMappings(state))
                    },
                    selectedPalette = state.selectedPalette,
                    isResearchMode = state.isResearchMode,
                    onSelectPalette = { palette ->
                        prefs.setSelectedPaletteId(palette?.id)
                        onStateChange(StatGraphWorkspaceController.setPalette(state, palette))
                    },
                    onSelectLabelDisplayMode = { mode ->
                        onStateChange(StatGraphWorkspaceController.setLabelDisplayMode(state, mode))
                    },
                    onSelectBarColorMode = { mode ->
                        onStateChange(StatGraphWorkspaceController.setBarColorMode(state, mode))
                    },
                )
            }

            item(key = "tablet_recommendations") {
                val activeSpecId = (state.renderState as? ChartRenderState.Ready)?.configuration?.specification?.id
                    ?: (state.renderState as? ChartRenderState.UnsupportedChartRenderer)?.requestedSpec?.id
                val isTimeOrdered = state.dataset.fields.any {
                    it.role is com.pasindu.nursingotapp.statgraph.model.FieldRole.Time
                }
                val lineTargetSpecId = if (state.mappingSelection.yFieldIds.size > 1 ||
                    state.mappingSelection.groupFieldId != null
                ) {
                    "multi_series_line_chart"
                } else {
                    "line_chart"
                }
                StatGraphRecommendationSection(
                    suite = state.recommendationSuite,
                    selectedSpecId = state.mappingSelection.chartSpecId,
                    activeSpecId = activeSpecId,
                    showAllCatalogueCharts = state.showAllCatalogueCharts,
                    onSelectChartSpec = { specId ->
                        onStateChange(StatGraphWorkspaceController.selectChartSpec(state, specId))
                    },
                    onToggleShowAllCatalogueCharts = { show ->
                        onStateChange(StatGraphWorkspaceController.toggleShowAllCatalogueCharts(state, show))
                    },
                    dataset = state.dataset,
                    isResearchMode = state.isResearchMode,
                    onToggleResearchMode = { research ->
                        onStateChange(StatGraphWorkspaceController.setResearchMode(state, research))
                    },
                    onUseLineChart = if (isTimeOrdered) {
                        { onStateChange(StatGraphWorkspaceController.selectChartSpec(state, lineTargetSpecId)) }
                    } else {
                        null
                    },
                    onGroupIntoFourWeekBlocks = {
                        onStateChange(StatGraphWorkspaceController.groupIntoFourWeekBlocks(state))
                    },
                )
            }

            item(key = "tablet_insights") {
                StatGraphInsightsSection(
                    dataset = state.dataset,
                    summaryMetrics = state.entryState.summaryMetrics,
                    diagnosticsReport = state.diagnosticsReport,
                    derivedResults = state.entryState.derivedResults,
                    narrativeSummary = if (state.isResearchMode) {
                        state.recommendationSuite.plainLanguageNarrative
                    } else {
                        state.recommendationSuite.simpleNarrative
                    },
                    compactMode = false,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataWorkspaceColumn(
    state: StatGraphWorkspaceUiState,
    compactMode: Boolean,
    horizontalPad: androidx.compose.ui.unit.Dp,
    categoryOptionsByField: Map<String, List<String>>,
    showAnalyseCta: Boolean,
    onStateChange: (StatGraphWorkspaceUiState) -> Unit,
    datasetCount: Int = 1,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "phone_data_next_step") {
            NextStepGuidanceStrip(text = stringResource(R.string.statgraph_next_data))
        }
        item(key = "phone_header") {
            StatGraphWorkspaceHeader(
                datasetName = state.dataset.name,
                datasetDescription = state.dataset.description,
                sourceLabel = state.dataset.metadata.sourceLabel,
                summaryMetrics = state.entryState.summaryMetrics,
                validationReport = state.entryState.validationReport,
                diagnosticsReport = state.diagnosticsReport,
                unconfirmedRoleCount = state.unconfirmedRoleCount,
                compactMode = compactMode,
                datasetCount = datasetCount,
                eligibleChartCount = state.recommendationSuite.recommendedCharts.size +
                    state.recommendationSuite.validCharts.size +
                    state.recommendationSuite.warningCharts.size,
                onCreateBlankDataset = {
                    onStateChange(StatGraphWorkspaceController.fromPreset(WorkspaceSamplePreset.BLANK))
                },
                onImportCsv = {
                    onStateChange(StatGraphWorkspaceController.openCsvImport(state))
                },
                onLoadDengueDemo = {
                    onStateChange(
                        StatGraphWorkspaceController.fromPreset(WorkspaceSamplePreset.DENGUE_FOUR_MONTH_DEMO),
                    )
                },
            )
        }

        item(key = "phone_privacy") {
            StatGraphPrivacyBanner(smallCountCellCount = state.entryState.summaryMetrics.smallCountCellCount)
        }

        item(key = "phone_data_subtabs") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                StatGraphEntryTab.entries.forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = state.entryState.activeTab == tab,
                        onClick = {
                            onStateChange(
                                StatGraphWorkspaceController.updateEntryState(
                                    state,
                                    state.entryState.copy(activeTab = tab),
                                ),
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, StatGraphEntryTab.entries.size),
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }

        if (state.unconfirmedRoleCount > 0) {
            item(key = "phone_confirm_roles") {
                RoleConfirmationPromptBanner(
                    unconfirmedCount = state.unconfirmedRoleCount,
                    onConfirmAll = {
                        onStateChange(StatGraphWorkspaceController.confirmAllSuggestedRoles(state))
                    },
                )
            }
        }

        when (state.entryState.activeTab) {
            StatGraphEntryTab.ROWS -> {
                item(key = "phone_validation") {
                    StatGraphValidationBanner(report = state.entryState.validationReport)
                }
                if (state.dataset.fields.isNotEmpty()) {
                    item(key = "phone_row_controls") {
                        WorkspaceRowControlsBar(
                            hasTimeField = state.dataset.fields.any { it.role is FieldRole.Time },
                            hasFields = true,
                            onAddRow = {
                                onStateChange(
                                    StatGraphWorkspaceController.updateEntryState(
                                        state,
                                        StatGraphEntryController.addBlankRow(state.entryState),
                                    ),
                                )
                            },
                            onAddNextPeriod = {
                                onStateChange(
                                    StatGraphWorkspaceController.updateEntryState(
                                        state,
                                        StatGraphEntryController.addNextPeriodRow(state.entryState),
                                    ),
                                )
                            },
                            onOpenAddField = {
                                onStateChange(
                                    StatGraphWorkspaceController.updateEntryState(
                                        state,
                                        state.entryState.copy(isFieldDialogOpen = true, editingField = null),
                                    ),
                                )
                            },
                        )
                    }
                }
                if (state.dataset.fields.isEmpty() || state.dataset.rows.isEmpty()) {
                    item(key = "phone_empty_data") {
                        WorkspaceEmptyDataCard(
                            hasFields = state.dataset.fields.isNotEmpty(),
                            onAddField = {
                                onStateChange(
                                    StatGraphWorkspaceController.updateEntryState(
                                        state,
                                        state.entryState.copy(isFieldDialogOpen = true, editingField = null),
                                    ),
                                )
                            },
                            onAddRow = {
                                onStateChange(
                                    StatGraphWorkspaceController.updateEntryState(
                                        state,
                                        StatGraphEntryController.addBlankRow(state.entryState),
                                    ),
                                )
                            },
                            onLoadDemo = {
                                onStateChange(
                                    StatGraphWorkspaceController.fromPreset(
                                        WorkspaceSamplePreset.DENGUE_FOUR_MONTH_DEMO,
                                    ),
                                )
                            },
                            onOpenCsv = {
                                onStateChange(StatGraphWorkspaceController.openCsvImport(state))
                            },
                        )
                    }
                } else {
                    itemsIndexed(
                        items = state.dataset.rows,
                        key = { _, row -> row.id },
                    ) { index, row ->
                        StatGraphRowCard(
                            rowNumber = index + 1,
                            row = row,
                            fields = state.dataset.fields,
                            derivedDefinitions = state.dataset.derivedFields,
                            derivedResultsByField = state.entryState.derivedResults,
                            rowIssues = state.entryState.validationReport.issuesForRow(row.id),
                            categoryOptionsByField = categoryOptionsByField,
                            onValueChange = { fieldId, updated ->
                                onStateChange(
                                    StatGraphWorkspaceController.updateEntryState(
                                        state,
                                        StatGraphEntryController.updateCellValue(
                                            state.entryState,
                                            row.id,
                                            fieldId,
                                            updated,
                                        ),
                                    ),
                                )
                            },
                            onDuplicate = {
                                onStateChange(
                                    StatGraphWorkspaceController.updateEntryState(
                                        state,
                                        StatGraphEntryController.duplicateRow(state.entryState, row.id),
                                    ),
                                )
                            },
                            onDelete = {
                                onStateChange(
                                    StatGraphWorkspaceController.updateEntryState(
                                        state,
                                        StatGraphEntryController.deleteRow(state.entryState, row.id),
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            StatGraphEntryTab.SETUP -> {
                item(key = "phone_metadata") {
                    WorkspaceMetadataCard(
                        name = state.dataset.name,
                        description = state.dataset.description,
                        onUpdate = { name, desc ->
                            onStateChange(
                                StatGraphWorkspaceController.updateEntryState(
                                    state,
                                    StatGraphEntryController.updateDatasetMeta(state.entryState, name, desc),
                                ),
                            )
                        },
                    )
                }
                item(key = "phone_schema_derived") {
                    WorkspaceSchemaAndDerivedBlock(state = state, onStateChange = onStateChange)
                }
            }

            StatGraphEntryTab.CSV -> {
                item(key = "phone_csv_import") {
                    WorkspaceCsvImportBlock(state = state, onStateChange = onStateChange)
                }
            }
        }

        if (showAnalyseCta && state.dataset.rows.isNotEmpty() && state.dataset.fields.isNotEmpty()) {
            item(key = "phone_analyse_cta") {
                Button(
                    onClick = {
                        onStateChange(StatGraphWorkspaceController.setSection(state, WorkspaceSection.EXPLORE))
                    },
                    shape = StatGraphDesignTokens.Shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        contentColor = StatGraphWorkspaceTokens.Colors.onBrandIndigo,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.statgraph_cta_show_best_chart),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item(key = "phone_data_bottom_spacer") { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun ExploreWorkspaceColumn(
    state: StatGraphWorkspaceUiState,
    compactMode: Boolean,
    horizontalPad: androidx.compose.ui.unit.Dp,
    onStateChange: (StatGraphWorkspaceUiState) -> Unit,
) {
    val activeSpecId = (state.renderState as? ChartRenderState.Ready)?.configuration?.specification?.id
        ?: (state.renderState as? ChartRenderState.UnsupportedChartRenderer)?.requestedSpec?.id
    val context = LocalContext.current
    val prefs = remember(context) { StatGraphPreferences(context) }
    val activePalette = StatGraphChartPalette.resolve(
        selected = state.selectedPalette,
        isResearchMode = state.isResearchMode,
    )
    val chartStyle = rememberStatGraphChartStyle(
        seriesColors = ChartPaletteTokens.colorsFor(activePalette),
        palette = activePalette,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "explore_next_step") {
            NextStepGuidanceStrip(text = stringResource(R.string.statgraph_next_chart))
        }
        if (compactMode) {
            item(key = "explore_compact_banner") {
                Surface(
                    shape = StatGraphDesignTokens.Shapes.large,
                    color = StatGraphWorkspaceTokens.Colors.brandIndigoSoft,
                    border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.dataset.name.ifBlank { "Untitled Dataset" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${state.entryState.summaryMetrics.rowCount} rows · ${state.entryState.summaryMetrics.fieldCount} fields · Tap chart to inspect",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatGraphWorkspaceTokens.Colors.slate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        item(key = "explore_chart_host") {
            val readyState = state.renderState as? ChartRenderState.Ready
            var exportSheetOpen by remember { mutableStateOf(false) }
            StatGraphOrchestratedView(
                dataset = state.dataset,
                selection = state.mappingSelection,
                isLoading = state.isLoading,
                style = chartStyle,
                onSelectAlternativeChart = { altSpecId ->
                    onStateChange(StatGraphWorkspaceController.selectChartSpec(state, altSpecId))
                },
            )
            if (readyState != null && readyState.renderData.hasDrawablePoints) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { exportSheetOpen = true },
                    shape = StatGraphDesignTokens.Shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        contentColor = StatGraphWorkspaceTokens.Colors.onBrandIndigo,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download", fontWeight = FontWeight.SemiBold)
                }
                if (exportSheetOpen) {
                    val isTimeOrdered = state.dataset.fields.any {
                        it.role is com.pasindu.nursingotapp.statgraph.model.FieldRole.Time
                    }
                    val lineTargetSpecId = if (readyState.renderData.series.size > 1) {
                        "multi_series_line_chart"
                    } else {
                        "line_chart"
                    }
                    StatGraphExportBottomSheet(
                        payload = StatGraphExportPayload(
                            renderData = readyState.renderData,
                            chartStyle = chartStyle,
                            howToRead = readyState.configuration.specification.howToRead,
                            plainWhy = readyState.configuration.specification.plainWhy,
                            sourceLabel = state.dataset.metadata.sourceLabel,
                            clinicalNotes = readyState.configuration.clinicalNotes,
                            exportDateText = java.time.LocalDate.now().toString(),
                            isOrderedTimeSeries = isTimeOrdered,
                        ),
                        onDismiss = { exportSheetOpen = false },
                        onSwitchToLineChart = if (isTimeOrdered) {
                            { onStateChange(StatGraphWorkspaceController.selectChartSpec(state, lineTargetSpecId)) }
                        } else {
                            null
                        },
                        onGroupIntoFourWeekBlocks = {
                            onStateChange(StatGraphWorkspaceController.groupIntoFourWeekBlocks(state))
                        },
                    )
                }
            }
            val readySpec = readyState?.configuration?.specification
            if (readySpec != null && readySpec.howToRead.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                HowToReadChartCard(
                    chartName = readySpec.canonicalName,
                    howToRead = readySpec.howToRead,
                    plainWhy = readySpec.plainWhy,
                )
            }
        }

        item(key = "explore_mapping_controls") {
            StatGraphMappingControlsCard(
                dataset = state.dataset,
                selection = state.mappingSelection,
                capability = state.mappingCapability,
                renderState = state.renderState,
                hasCustomMapping = state.hasCustomMapping,
                onSelectXField = { fieldId ->
                    onStateChange(StatGraphWorkspaceController.setXField(state, fieldId))
                },
                onToggleYField = { fieldId ->
                    onStateChange(StatGraphWorkspaceController.toggleYField(state, fieldId))
                },
                onToggleDerivedField = { derivedId ->
                    onStateChange(StatGraphWorkspaceController.toggleDerivedField(state, derivedId))
                },
                onSelectGroupField = { grpId ->
                    onStateChange(StatGraphWorkspaceController.setGroupField(state, grpId))
                },
                onResetMappings = {
                    onStateChange(StatGraphWorkspaceController.resetMappings(state))
                },
                selectedPalette = state.selectedPalette,
                isResearchMode = state.isResearchMode,
                onSelectPalette = { palette ->
                    prefs.setSelectedPaletteId(palette?.id)
                    onStateChange(StatGraphWorkspaceController.setPalette(state, palette))
                },
                onSelectLabelDisplayMode = { mode ->
                    onStateChange(StatGraphWorkspaceController.setLabelDisplayMode(state, mode))
                },
                onSelectBarColorMode = { mode ->
                    onStateChange(StatGraphWorkspaceController.setBarColorMode(state, mode))
                },
            )
        }

        item(key = "explore_recommendations") {
            val isTimeOrdered = state.dataset.fields.any {
                it.role is com.pasindu.nursingotapp.statgraph.model.FieldRole.Time
            }
            val lineTargetSpecId = if (state.mappingSelection.yFieldIds.size > 1 ||
                state.mappingSelection.groupFieldId != null
            ) {
                "multi_series_line_chart"
            } else {
                "line_chart"
            }
            StatGraphRecommendationSection(
                suite = state.recommendationSuite,
                selectedSpecId = state.mappingSelection.chartSpecId,
                activeSpecId = activeSpecId,
                showAllCatalogueCharts = state.showAllCatalogueCharts,
                onSelectChartSpec = { specId ->
                    onStateChange(StatGraphWorkspaceController.selectChartSpec(state, specId))
                },
                onToggleShowAllCatalogueCharts = { show ->
                    onStateChange(StatGraphWorkspaceController.toggleShowAllCatalogueCharts(state, show))
                },
                dataset = state.dataset,
                isResearchMode = state.isResearchMode,
                onToggleResearchMode = { research ->
                    onStateChange(StatGraphWorkspaceController.setResearchMode(state, research))
                },
                onUseLineChart = if (isTimeOrdered) {
                    { onStateChange(StatGraphWorkspaceController.selectChartSpec(state, lineTargetSpecId)) }
                } else {
                    null
                },
                onGroupIntoFourWeekBlocks = {
                    onStateChange(StatGraphWorkspaceController.groupIntoFourWeekBlocks(state))
                },
            )
        }

        item(key = "explore_bottom_spacer") { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun InsightsWorkspaceColumn(
    state: StatGraphWorkspaceUiState,
    compactMode: Boolean,
    horizontalPad: androidx.compose.ui.unit.Dp,
    onStateChange: (StatGraphWorkspaceUiState) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "insights_next_step") {
            NextStepGuidanceStrip(text = stringResource(R.string.statgraph_next_summary))
        }
        if (state.unconfirmedRoleCount > 0) {
            item(key = "insights_confirm_roles") {
                RoleConfirmationPromptBanner(
                    unconfirmedCount = state.unconfirmedRoleCount,
                    onConfirmAll = {
                        onStateChange(StatGraphWorkspaceController.confirmAllSuggestedRoles(state))
                    },
                )
            }
        }

        item(key = "insights_content") {
            StatGraphInsightsSection(
                dataset = state.dataset,
                summaryMetrics = state.entryState.summaryMetrics,
                diagnosticsReport = state.diagnosticsReport,
                derivedResults = state.entryState.derivedResults,
                narrativeSummary = if (state.isResearchMode) {
                    state.recommendationSuite.plainLanguageNarrative
                } else {
                    state.recommendationSuite.simpleNarrative
                },
                compactMode = compactMode,
            )
        }

        item(key = "insights_bottom_spacer") { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun RoleConfirmationPromptBanner(
    unconfirmedCount: Int,
    onConfirmAll: () -> Unit,
) {
    Surface(
        shape = StatGraphDesignTokens.Shapes.large,
        color = StatGraphWorkspaceTokens.Colors.brandIndigoSoft,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.brandIndigo.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircleOutline,
                contentDescription = null,
                tint = StatGraphWorkspaceTokens.Colors.brandIndigo,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$unconfirmedCount suggested field role(s) pending confirmation",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                )
                Text(
                    text = "Confirm auto-detected Time, Measure, and Part-of-Measure roles to finalize chart mapping.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphWorkspaceTokens.Colors.slate,
                )
            }
            FilledTonalButton(
                onClick = onConfirmAll,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceRowControlsBar(
    hasTimeField: Boolean,
    hasFields: Boolean,
    onAddRow: () -> Unit,
    onAddNextPeriod: () -> Unit,
    onOpenAddField: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatGraphSectionHeader(
            eyebrow = "OBSERVATIONS",
            title = "Editable Aggregate Rows",
            icon = Icons.Rounded.TableChart,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hasFields) {
                Button(
                    onClick = onAddRow,
                    shape = StatGraphDesignTokens.Shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Row")
                }

                if (hasTimeField) {
                    FilledTonalButton(
                        onClick = onAddNextPeriod,
                        shape = StatGraphDesignTokens.Shapes.medium,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                    ) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Next Period")
                    }
                }
            }
            OutlinedButton(
                onClick = onOpenAddField,
                shape = StatGraphDesignTokens.Shapes.medium,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Field")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceEmptyDataCard(
    hasFields: Boolean,
    onAddField: () -> Unit,
    onAddRow: () -> Unit,
    onLoadDemo: () -> Unit,
    onOpenCsv: () -> Unit,
) {
    Surface(
        color = StatGraphWorkspaceTokens.Colors.brandIndigoSoft,
        shape = StatGraphDesignTokens.Shapes.extraLarge,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (hasFields) "Ready for aggregate observations" else "Build or import a clinical dataset",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = StatGraphWorkspaceTokens.Colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = if (hasFields) {
                    "Add an observation row or load the simulated Dengue Surveillance demo. Empty cells always remain Missing."
                } else {
                    "Define Time, Category, Measure, or Estimate fields, import a CSV file, or explore the Dengue Surveillance demo."
                },
                style = MaterialTheme.typography.bodySmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (hasFields) {
                    Button(
                        onClick = onAddRow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        ),
                    ) { Text("Add First Row") }
                } else {
                    Button(
                        onClick = onAddField,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        ),
                    ) { Text("Create Field") }
                }
                FilledTonalButton(onClick = onLoadDemo) { Text("Load Dengue Demo") }
                OutlinedButton(onClick = onOpenCsv) { Text("Import CSV") }
            }
        }
    }
}

@Composable
private fun WorkspaceMetadataCard(
    name: String,
    description: String,
    onUpdate: (String, String) -> Unit,
) {
    Surface(
        color = StatGraphWorkspaceTokens.Colors.surface,
        shape = StatGraphDesignTokens.Shapes.extraLarge,
        border = BorderStroke(1.dp, StatGraphWorkspaceTokens.Colors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatGraphSectionHeader(
                eyebrow = "DATASET METADATA",
                title = "Identity & Aggregate Scope",
                icon = Icons.Rounded.Storage,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { onUpdate(it, description) },
                label = { Text("Dataset Name") },
                singleLine = true,
                shape = StatGraphDesignTokens.Shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { onUpdate(name, it) },
                label = { Text("Description (no PII)") },
                minLines = 2,
                maxLines = 4,
                shape = StatGraphDesignTokens.Shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WorkspaceSchemaAndDerivedBlock(
    state: StatGraphWorkspaceUiState,
    onStateChange: (StatGraphWorkspaceUiState) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatGraphSectionHeader(
            eyebrow = "FIELD ROLES & UNITS",
            title = "Schema (${state.dataset.fields.size})",
            icon = Icons.Rounded.Tune,
            trailingAction = {
                FilledTonalButton(
                    onClick = {
                        onStateChange(
                            StatGraphWorkspaceController.updateEntryState(
                                state,
                                state.entryState.copy(isFieldDialogOpen = true, editingField = null),
                            ),
                        )
                    },
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Field")
                }
            },
        )

        if (state.dataset.fields.isEmpty()) {
            Surface(
                color = StatGraphWorkspaceTokens.Colors.softSlate,
                shape = StatGraphDesignTokens.Shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "No fields configured yet. Add a Time, Measure, Category, or Estimate field.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphWorkspaceTokens.Colors.slate,
                    modifier = Modifier.padding(14.dp),
                )
            }
        } else {
            val fieldMap = remember(state.dataset.fields) { state.dataset.fields.associateBy { it.id } }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.dataset.fields.forEach { field ->
                    val parentName = (field.role as? FieldRole.PartOfMeasure)
                        ?.parentFieldId?.let { fieldMap[it]?.name }
                    StatGraphSchemaFieldCard(
                        field = field,
                        parentFieldName = parentName,
                        onConfirmRole = {
                            onStateChange(
                                StatGraphWorkspaceController.updateEntryState(
                                    state,
                                    StatGraphEntryController.confirmSuggestedRole(state.entryState, field.id),
                                ),
                            )
                        },
                        onEdit = {
                            onStateChange(
                                StatGraphWorkspaceController.updateEntryState(
                                    state,
                                    state.entryState.copy(isFieldDialogOpen = true, editingField = field),
                                ),
                            )
                        },
                        onDelete = {
                            onStateChange(
                                StatGraphWorkspaceController.updateEntryState(
                                    state,
                                    StatGraphEntryController.deleteField(state.entryState, field.id),
                                ),
                            )
                        },
                    )
                }
            }
        }

        StatGraphSectionHeader(
            eyebrow = "NON-DESTRUCTIVE CALCULATIONS",
            title = "Derived Fields (${state.dataset.derivedFields.size})",
            icon = Icons.Rounded.Functions,
            trailingAction = {
                OutlinedButton(
                    enabled = state.dataset.fields.any { it.role.isNumeric },
                    onClick = {
                        onStateChange(
                            StatGraphWorkspaceController.updateEntryState(
                                state,
                                state.entryState.copy(isDerivedDialogOpen = true),
                            ),
                        )
                    },
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Derived")
                }
            },
        )

        if (state.dataset.derivedFields.isEmpty()) {
            Surface(
                color = StatGraphWorkspaceTokens.Colors.softPurple.copy(alpha = 0.55f),
                shape = StatGraphDesignTokens.Shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Compute percentage shares, per-1,000 rates, or moving averages without modifying source values.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphWorkspaceTokens.Colors.slate,
                    modifier = Modifier.padding(14.dp),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.dataset.derivedFields.forEach { def ->
                    StatGraphDerivedFieldCard(
                        definition = def,
                        onDelete = {
                            onStateChange(
                                StatGraphWorkspaceController.updateEntryState(
                                    state,
                                    StatGraphEntryController.removeDerivedField(state.entryState, def.id),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceCsvImportBlock(
    state: StatGraphWorkspaceUiState,
    onStateChange: (StatGraphWorkspaceUiState) -> Unit,
) {
    StatGraphCsvImportSection(
        rawCsvText = state.entryState.csvRawText,
        fileName = state.entryState.csvFileName,
        hasHeaderOverride = state.entryState.csvHasHeaderOverride,
        columnOverrides = state.entryState.csvColumnOverrides,
        preview = state.entryState.csvPreview,
        onCsvTextChange = { text, name ->
            onStateChange(
                StatGraphWorkspaceController.updateEntryState(
                    state,
                    StatGraphEntryController.updateCsvInput(state.entryState, text, name),
                ),
            )
        },
        onLoadSampleCsv = {
            onStateChange(
                StatGraphWorkspaceController.updateEntryState(
                    state,
                    StatGraphEntryController.loadSampleCsv(state.entryState),
                ),
            )
        },
        onHeaderToggle = { hasHeader ->
            onStateChange(
                StatGraphWorkspaceController.updateEntryState(
                    state,
                    StatGraphEntryController.updateCsvInput(
                        state = state.entryState,
                        rawText = state.entryState.csvRawText,
                        hasHeaderOverride = hasHeader,
                    ),
                ),
            )
        },
        onColumnRoleOverride = { colIdx, role, confirmed ->
            val currentOverride = state.entryState.csvColumnOverrides[colIdx] ?: CsvColumnOverride()
            val updatedOverrides = state.entryState.csvColumnOverrides + (
                colIdx to currentOverride.copy(role = role, isRoleConfirmed = confirmed)
            )
            onStateChange(
                StatGraphWorkspaceController.updateEntryState(
                    state,
                    StatGraphEntryController.updateCsvInput(
                        state = state.entryState,
                        rawText = state.entryState.csvRawText,
                        overrides = updatedOverrides,
                    ),
                ),
            )
        },
        onClearCsv = {
            onStateChange(
                StatGraphWorkspaceController.updateEntryState(
                    state,
                    state.entryState.copy(
                        csvRawText = "",
                        csvFileName = null,
                        csvPreview = null,
                        csvColumnOverrides = emptyMap(),
                        csvHasHeaderOverride = null,
                    ),
                ),
            )
        },
        onAcceptImport = {
            val accepted = StatGraphEntryController.acceptCsvPreview(state.entryState)
            val updated = StatGraphWorkspaceController.updateEntryState(state, accepted)
            onStateChange(StatGraphWorkspaceController.setSection(updated, WorkspaceSection.EXPLORE))
        },
    )
}

@Preview(name = "1. Empty Workspace (Phone)", showBackground = true, widthDp = 390, heightDp = 840)
@Composable
fun StatGraphWorkspaceEmptyPreview() {
    NursingOTAppTheme {
        StatGraphStudioWorkspaceScreen(
            initialDataset = StatGraphSampleData.emptyDataset(),
            initialSection = WorkspaceSection.DATA,
        )
    }
}

@Preview(name = "2. Dengue Surveillance Demo (Phone)", showBackground = true, widthDp = 390, heightDp = 860)
@Composable
fun StatGraphWorkspaceDengueDemoPreview() {
    NursingOTAppTheme {
        StatGraphStudioWorkspaceScreen(
            initialDataset = StatGraphWorkspaceSamples.dengueFourMonthDemoDataset(),
            initialSection = WorkspaceSection.DATA,
        )
    }
}

@Preview(name = "3. CSV Import Preview (Phone)", showBackground = true, widthDp = 390, heightDp = 860)
@Composable
fun StatGraphWorkspaceCsvImportPreview() {
    NursingOTAppTheme {
        val base = StatGraphWorkspaceController.fromPreset(WorkspaceSamplePreset.BLANK)
        val withSampleCsv = StatGraphWorkspaceController.updateEntryState(
            base,
            StatGraphEntryController.loadSampleCsv(base.entryState),
        )
        StatGraphStudioWorkspaceContent(
            state = StatGraphWorkspaceController.openCsvImport(withSampleCsv),
            onStateChange = {},
        )
    }
}

@Preview(name = "4. Recommendation & Chart Studio (Phone)", showBackground = true, widthDp = 390, heightDp = 880)
@Composable
fun StatGraphWorkspaceRecommendationChartPreview() {
    NursingOTAppTheme {
        StatGraphStudioWorkspaceScreen(
            initialDataset = StatGraphWorkspaceSamples.dengueFourMonthDemoDataset(confirmRolesImmediately = true),
            initialSection = WorkspaceSection.EXPLORE,
        )
    }
}

@Preview(name = "5. Validation Warnings & Stats Audit (Phone)", showBackground = true, widthDp = 390, heightDp = 880)
@Composable
fun StatGraphWorkspaceWarningsPreview() {
    NursingOTAppTheme {
        StatGraphStudioWorkspaceScreen(
            initialDataset = StatGraphSampleData.validationWarningDataset(),
            initialSection = WorkspaceSection.INSIGHTS,
        )
    }
}

@Preview(name = "6. Responsive Tablet Workspace (Two-Pane)", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
fun StatGraphWorkspaceTabletPreview() {
    NursingOTAppTheme {
        StatGraphStudioWorkspaceScreen(
            initialDataset = StatGraphWorkspaceSamples.dengueFourMonthDemoDataset(confirmRolesImmediately = true),
        )
    }
}
