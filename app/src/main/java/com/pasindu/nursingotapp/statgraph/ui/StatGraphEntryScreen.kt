package com.pasindu.nursingotapp.statgraph.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.data.CsvColumnOverride
import com.pasindu.nursingotapp.statgraph.data.StatGraphSampleData
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.ui.components.ChartReadinessStrip
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphCsvImportSection
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphDerivedFieldCard
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphDerivedFieldDialog
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphFieldEditorDialog
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphHeroCard
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphPrivacyBanner
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphRowCard
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphSchemaFieldCard
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphSectionHeader
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphValidationBanner
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import com.pasindu.nursingotapp.ui.theme.NursingOTAppTheme

/**
 * Standalone entry screen for Stat Graph Studio (Phase 1).
 * Intentionally decoupled from AppNavigation, Room, and existing ViewModels.
 */
@Composable
fun StatGraphEntryScreen(
    initialDataset: StatDataset = StatGraphSampleData.emptyDataset(),
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var uiState by remember(initialDataset.id) {
        mutableStateOf(StatGraphEntryUiState.fromDataset(initialDataset))
    }

    StatGraphEntryContent(
        state = uiState,
        onStateChange = { uiState = it },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatGraphEntryContent(
    state: StatGraphEntryUiState,
    onStateChange: (StatGraphEntryUiState) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
        containerColor = StatGraphDesignTokens.Colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Stat Graph Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = StatGraphDesignTokens.Colors.ink
                        )
                        Text(
                            text = "Aggregate Data Entry & Validation",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatGraphDesignTokens.Colors.slate
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StatGraphDesignTokens.Colors.background
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StatGraphDesignTokens.Colors.background)
        ) {
            val isTabletLayout = maxWidth >= 720.dp

            if (isTabletLayout) {
                TabletTwoPaneLayout(
                    state = state,
                    categoryOptionsByField = categoryOptionsByField,
                    onStateChange = onStateChange
                )
            } else {
                PhoneSinglePaneLayout(
                    state = state,
                    categoryOptionsByField = categoryOptionsByField,
                    onStateChange = onStateChange
                )
            }
        }
    }

    if (state.isFieldDialogOpen) {
        StatGraphFieldEditorDialog(
            initialField = state.editingField,
            existingFields = state.dataset.fields,
            onSave = { field ->
                onStateChange(StatGraphEntryController.saveField(state, field))
            },
            onDismiss = {
                onStateChange(state.copy(isFieldDialogOpen = false, editingField = null))
            }
        )
    }

    if (state.isDerivedDialogOpen) {
        StatGraphDerivedFieldDialog(
            fields = state.dataset.fields,
            onSave = { def ->
                onStateChange(StatGraphEntryController.addDerivedField(state, def))
            },
            onDismiss = {
                onStateChange(state.copy(isDerivedDialogOpen = false))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneSinglePaneLayout(
    state: StatGraphEntryUiState,
    categoryOptionsByField: Map<String, List<String>>,
    onStateChange: (StatGraphEntryUiState) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StatGraphHeroCard(
                datasetName = state.dataset.name,
                datasetDescription = state.dataset.description,
                metrics = state.summaryMetrics,
                validationReport = state.validationReport
            )
        }

        item {
            StatGraphPrivacyBanner(smallCountCellCount = state.summaryMetrics.smallCountCellCount)
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                StatGraphEntryTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = state.activeTab == tab,
                        onClick = { onStateChange(state.copy(activeTab = tab)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = StatGraphEntryTab.entries.size
                        ),
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        }

        when (state.activeTab) {
            StatGraphEntryTab.ROWS -> {
                item {
                    StatGraphValidationBanner(report = state.validationReport)
                }
                item {
                    ChartReadinessStrip(suggestions = state.chartReadiness)
                }
                item {
                    RowControlsBar(
                        hasTimeField = state.dataset.fields.any { it.role is FieldRole.Time },
                        hasFields = state.dataset.fields.isNotEmpty(),
                        onAddRow = { onStateChange(StatGraphEntryController.addBlankRow(state)) },
                        onAddNextPeriod = { onStateChange(StatGraphEntryController.addNextPeriodRow(state)) },
                        onOpenAddField = {
                            onStateChange(state.copy(isFieldDialogOpen = true, editingField = null))
                        },
                        onLoadSample = {
                            onStateChange(
                                StatGraphEntryUiState.fromDataset(
                                    StatGraphSampleData.dengueMonthlyPopulatedDataset(),
                                    activeTab = StatGraphEntryTab.ROWS
                                )
                            )
                        },
                        onOpenCsv = { onStateChange(state.copy(activeTab = StatGraphEntryTab.CSV)) }
                    )
                }
                if (state.dataset.fields.isEmpty() || state.dataset.rows.isEmpty()) {
                    item {
                        EmptyRowsPastelCard(
                            hasFields = state.dataset.fields.isNotEmpty(),
                            onAddField = {
                                onStateChange(state.copy(isFieldDialogOpen = true, editingField = null))
                            },
                            onAddRow = { onStateChange(StatGraphEntryController.addBlankRow(state)) },
                            onLoadSample = {
                                onStateChange(
                                    StatGraphEntryUiState.fromDataset(
                                        StatGraphSampleData.dengueMonthlyPopulatedDataset(),
                                        activeTab = StatGraphEntryTab.ROWS
                                    )
                                )
                            },
                            onOpenCsv = { onStateChange(state.copy(activeTab = StatGraphEntryTab.CSV)) }
                        )
                    }
                } else {
                    itemsIndexed(
                        items = state.dataset.rows,
                        key = { _, row -> row.id }
                    ) { index, row ->
                        StatGraphRowCard(
                            rowNumber = index + 1,
                            row = row,
                            fields = state.dataset.fields,
                            derivedDefinitions = state.dataset.derivedFields,
                            derivedResultsByField = state.derivedResults,
                            rowIssues = state.validationReport.issuesForRow(row.id),
                            categoryOptionsByField = categoryOptionsByField,
                            onValueChange = { fieldId, updated ->
                                onStateChange(
                                    StatGraphEntryController.updateCellValue(state, row.id, fieldId, updated)
                                )
                            },
                            onDuplicate = {
                                onStateChange(StatGraphEntryController.duplicateRow(state, row.id))
                            },
                            onDelete = {
                                onStateChange(StatGraphEntryController.deleteRow(state, row.id))
                            }
                        )
                    }
                }
            }

            StatGraphEntryTab.SETUP -> {
                item {
                    DatasetMetadataCard(
                        name = state.dataset.name,
                        description = state.dataset.description,
                        onUpdate = { name, desc ->
                            onStateChange(StatGraphEntryController.updateDatasetMeta(state, name, desc))
                        }
                    )
                }
                item {
                    SchemaAndDerivedSection(
                        state = state,
                        onStateChange = onStateChange
                    )
                }
            }

            StatGraphEntryTab.CSV -> {
                item {
                    CsvSectionWrapper(state = state, onStateChange = onStateChange)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletTwoPaneLayout(
    state: StatGraphEntryUiState,
    categoryOptionsByField: Map<String, List<String>>,
    onStateChange: (StatGraphEntryUiState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Left Pane: Hero, Privacy, Dataset Metadata, Schema & Derived
        LazyColumn(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StatGraphHeroCard(
                    datasetName = state.dataset.name,
                    datasetDescription = state.dataset.description,
                    metrics = state.summaryMetrics,
                    validationReport = state.validationReport
                )
            }
            item {
                StatGraphPrivacyBanner(smallCountCellCount = state.summaryMetrics.smallCountCellCount)
            }
            item {
                DatasetMetadataCard(
                    name = state.dataset.name,
                    description = state.dataset.description,
                    onUpdate = { name, desc ->
                        onStateChange(StatGraphEntryController.updateDatasetMeta(state, name, desc))
                    }
                )
            }
            item {
                SchemaAndDerivedSection(state = state, onStateChange = onStateChange)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // Right Pane: Switch between Row Entry and CSV Import
        LazyColumn(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                val tabletTabs = listOf(StatGraphEntryTab.ROWS, StatGraphEntryTab.CSV)
                val selected = if (state.activeTab == StatGraphEntryTab.CSV) {
                    StatGraphEntryTab.CSV
                } else {
                    StatGraphEntryTab.ROWS
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tabletTabs.forEachIndexed { idx, tab ->
                        SegmentedButton(
                            selected = selected == tab,
                            onClick = { onStateChange(state.copy(activeTab = tab)) },
                            shape = SegmentedButtonDefaults.itemShape(idx, tabletTabs.size),
                            label = { Text(tab.label, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            if (state.activeTab == StatGraphEntryTab.CSV) {
                item {
                    CsvSectionWrapper(state = state, onStateChange = onStateChange)
                }
            } else {
                item {
                    StatGraphValidationBanner(report = state.validationReport)
                }
                item {
                    ChartReadinessStrip(suggestions = state.chartReadiness)
                }
                item {
                    RowControlsBar(
                        hasTimeField = state.dataset.fields.any { it.role is FieldRole.Time },
                        hasFields = state.dataset.fields.isNotEmpty(),
                        onAddRow = { onStateChange(StatGraphEntryController.addBlankRow(state)) },
                        onAddNextPeriod = { onStateChange(StatGraphEntryController.addNextPeriodRow(state)) },
                        onOpenAddField = {
                            onStateChange(state.copy(isFieldDialogOpen = true, editingField = null))
                        },
                        onLoadSample = {
                            onStateChange(
                                StatGraphEntryUiState.fromDataset(
                                    StatGraphSampleData.dengueMonthlyPopulatedDataset(),
                                    activeTab = StatGraphEntryTab.ROWS
                                )
                            )
                        },
                        onOpenCsv = { onStateChange(state.copy(activeTab = StatGraphEntryTab.CSV)) }
                    )
                }
                if (state.dataset.fields.isEmpty() || state.dataset.rows.isEmpty()) {
                    item {
                        EmptyRowsPastelCard(
                            hasFields = state.dataset.fields.isNotEmpty(),
                            onAddField = {
                                onStateChange(state.copy(isFieldDialogOpen = true, editingField = null))
                            },
                            onAddRow = { onStateChange(StatGraphEntryController.addBlankRow(state)) },
                            onLoadSample = {
                                onStateChange(
                                    StatGraphEntryUiState.fromDataset(
                                        StatGraphSampleData.dengueMonthlyPopulatedDataset(),
                                        activeTab = StatGraphEntryTab.ROWS
                                    )
                                )
                            },
                            onOpenCsv = { onStateChange(state.copy(activeTab = StatGraphEntryTab.CSV)) }
                        )
                    }
                } else {
                    itemsIndexed(
                        items = state.dataset.rows,
                        key = { _, row -> row.id }
                    ) { index, row ->
                        StatGraphRowCard(
                            rowNumber = index + 1,
                            row = row,
                            fields = state.dataset.fields,
                            derivedDefinitions = state.dataset.derivedFields,
                            derivedResultsByField = state.derivedResults,
                            rowIssues = state.validationReport.issuesForRow(row.id),
                            categoryOptionsByField = categoryOptionsByField,
                            onValueChange = { fieldId, updated ->
                                onStateChange(
                                    StatGraphEntryController.updateCellValue(state, row.id, fieldId, updated)
                                )
                            },
                            onDuplicate = {
                                onStateChange(StatGraphEntryController.duplicateRow(state, row.id))
                            },
                            onDelete = {
                                onStateChange(StatGraphEntryController.deleteRow(state, row.id))
                            }
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DatasetMetadataCard(
    name: String,
    description: String,
    onUpdate: (String, String) -> Unit
) {
    Surface(
        color = StatGraphDesignTokens.Colors.surface,
        shape = StatGraphDesignTokens.Shapes.extraLarge,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatGraphSectionHeader(
                eyebrow = "DATASET METADATA",
                title = "Identity & Scope",
                icon = Icons.Default.Storage
            )
            OutlinedTextField(
                value = name,
                onValueChange = { onUpdate(it, description) },
                label = { Text("Dataset Name") },
                placeholder = { Text("e.g., Monthly Ward Dengue Surveillance") },
                singleLine = true,
                shape = StatGraphDesignTokens.Shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { onUpdate(name, it) },
                label = { Text("Aggregate Scope / Description") },
                placeholder = { Text("Describe ward, period, and aggregate unit (no PII)") },
                minLines = 2,
                maxLines = 4,
                shape = StatGraphDesignTokens.Shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SchemaAndDerivedSection(
    state: StatGraphEntryUiState,
    onStateChange: (StatGraphEntryUiState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatGraphSectionHeader(
            eyebrow = "FIELD ROLES",
            title = "Schema (${state.dataset.fields.size})",
            icon = Icons.Default.Tune,
            trailingAction = {
                FilledTonalButton(
                    onClick = {
                        onStateChange(state.copy(isFieldDialogOpen = true, editingField = null))
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Field")
                }
            }
        )

        if (state.dataset.fields.isEmpty()) {
            Surface(
                color = StatGraphDesignTokens.Colors.softSlate,
                shape = StatGraphDesignTokens.Shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No fields defined yet. Tap 'Add Field' to configure Time, Measure, Category, or Estimate columns.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphDesignTokens.Colors.slate,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            val fieldMap = state.dataset.fields.associateBy { it.id }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.dataset.fields.forEach { field ->
                    val parentName = (field.role as? FieldRole.PartOfMeasure)
                        ?.parentFieldId?.let { fieldMap[it]?.name }
                    StatGraphSchemaFieldCard(
                        field = field,
                        parentFieldName = parentName,
                        onConfirmRole = {
                            onStateChange(StatGraphEntryController.confirmSuggestedRole(state, field.id))
                        },
                        onEdit = {
                            onStateChange(state.copy(isFieldDialogOpen = true, editingField = field))
                        },
                        onDelete = {
                            onStateChange(StatGraphEntryController.deleteField(state, field.id))
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        StatGraphSectionHeader(
            eyebrow = "NON-DESTRUCTIVE CALCULATIONS",
            title = "Derived Fields (${state.dataset.derivedFields.size})",
            icon = Icons.Default.Functions,
            trailingAction = {
                OutlinedButton(
                    enabled = state.dataset.fields.any { it.role.isNumeric },
                    onClick = { onStateChange(state.copy(isDerivedDialogOpen = true)) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Derived")
                }
            }
        )

        if (state.dataset.derivedFields.isEmpty()) {
            Surface(
                color = StatGraphDesignTokens.Colors.softPurple.copy(alpha = 0.55f),
                shape = StatGraphDesignTokens.Shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add Rate (%), Per 1,000, Moving Average, or Cumulative Total calculations without altering original observations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphDesignTokens.Colors.slate,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.dataset.derivedFields.forEach { def ->
                    StatGraphDerivedFieldCard(
                        definition = def,
                        onDelete = {
                            onStateChange(StatGraphEntryController.removeDerivedField(state, def.id))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowControlsBar(
    hasTimeField: Boolean,
    hasFields: Boolean,
    onAddRow: () -> Unit,
    onAddNextPeriod: () -> Unit,
    onOpenAddField: () -> Unit,
    onLoadSample: () -> Unit,
    onOpenCsv: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatGraphSectionHeader(
            eyebrow = "OBSERVATIONS",
            title = "Aggregate Data Rows",
            icon = Icons.Default.TableRows
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasFields) {
                Button(
                    onClick = onAddRow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatGraphDesignTokens.Colors.primary
                    ),
                    shape = StatGraphDesignTokens.Shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Row")
                }

                if (hasTimeField) {
                    FilledTonalButton(
                        onClick = onAddNextPeriod,
                        shape = StatGraphDesignTokens.Shapes.medium
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Next Period")
                    }
                }
            } else {
                Button(
                    onClick = onOpenAddField,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatGraphDesignTokens.Colors.primary
                    ),
                    shape = StatGraphDesignTokens.Shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add First Field")
                }
            }

            OutlinedButton(
                onClick = onLoadSample,
                shape = StatGraphDesignTokens.Shapes.medium
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Load Sample")
            }

            OutlinedButton(
                onClick = onOpenCsv,
                shape = StatGraphDesignTokens.Shapes.medium
            ) {
                Text("Import CSV")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyRowsPastelCard(
    hasFields: Boolean,
    onAddField: () -> Unit,
    onAddRow: () -> Unit,
    onLoadSample: () -> Unit,
    onOpenCsv: () -> Unit
) {
    Surface(
        color = StatGraphDesignTokens.Colors.softBlue,
        shape = StatGraphDesignTokens.Shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (hasFields) "Ready for observations" else "Start building your aggregate dataset",
                style = MaterialTheme.typography.titleMedium,
                color = StatGraphDesignTokens.Colors.ink,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (hasFields) {
                    "Add a blank row or use Next Period to start recording aggregate counts. Missing fields stay empty rather than defaulting to zero."
                } else {
                    "Define columns (Month, Ward, Cases, Total Patients), paste/import a CSV file, or load the Dengue Monthly sample dataset."
                },
                style = MaterialTheme.typography.bodySmall,
                color = StatGraphDesignTokens.Colors.slate
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasFields) {
                    Button(onClick = onAddRow) { Text("Add First Row") }
                } else {
                    Button(onClick = onAddField) { Text("Create Field") }
                }
                FilledTonalButton(onClick = onLoadSample) { Text("Load Sample Dataset") }
                OutlinedButton(onClick = onOpenCsv) { Text("Paste / Import CSV") }
            }
        }
    }
}

@Composable
private fun CsvSectionWrapper(
    state: StatGraphEntryUiState,
    onStateChange: (StatGraphEntryUiState) -> Unit
) {
    StatGraphCsvImportSection(
        rawCsvText = state.csvRawText,
        fileName = state.csvFileName,
        hasHeaderOverride = state.csvHasHeaderOverride,
        columnOverrides = state.csvColumnOverrides,
        preview = state.csvPreview,
        onCsvTextChange = { text, name ->
            onStateChange(
                StatGraphEntryController.updateCsvInput(
                    state = state,
                    rawText = text,
                    fileName = name
                )
            )
        },
        onLoadSampleCsv = {
            onStateChange(StatGraphEntryController.loadSampleCsv(state))
        },
        onHeaderToggle = { hasHeader ->
            onStateChange(
                StatGraphEntryController.updateCsvInput(
                    state = state,
                    rawText = state.csvRawText,
                    hasHeaderOverride = hasHeader
                )
            )
        },
        onColumnRoleOverride = { colIdx, role, confirmed ->
            val currentOverride = state.csvColumnOverrides[colIdx] ?: CsvColumnOverride()
            val updatedOverrides = state.csvColumnOverrides + (
                colIdx to currentOverride.copy(role = role, isRoleConfirmed = confirmed)
            )
            onStateChange(
                StatGraphEntryController.updateCsvInput(
                    state = state,
                    rawText = state.csvRawText,
                    overrides = updatedOverrides
                )
            )
        },
        onClearCsv = {
            onStateChange(
                state.copy(
                    csvRawText = "",
                    csvFileName = null,
                    csvPreview = null,
                    csvColumnOverrides = emptyMap(),
                    csvHasHeaderOverride = null
                )
            )
        },
        onAcceptImport = {
            onStateChange(StatGraphEntryController.acceptCsvPreview(state))
        }
    )
}

@Preview(name = "1. Empty State (Phone)", showBackground = true, widthDp = 390, heightDp = 820)
@Composable
fun StatGraphEntryScreenEmptyPreview() {
    NursingOTAppTheme {
        StatGraphEntryScreen(initialDataset = StatGraphSampleData.emptyDataset())
    }
}

@Preview(name = "2. Populated State (Phone)", showBackground = true, widthDp = 390, heightDp = 860)
@Composable
fun StatGraphEntryScreenPopulatedPreview() {
    NursingOTAppTheme {
        StatGraphEntryScreen(initialDataset = StatGraphSampleData.dengueMonthlyPopulatedDataset())
    }
}

@Preview(name = "3. Validation Warnings & Errors", showBackground = true, widthDp = 390, heightDp = 880)
@Composable
fun StatGraphEntryScreenValidationWarningPreview() {
    NursingOTAppTheme {
        StatGraphEntryScreen(initialDataset = StatGraphSampleData.validationWarningDataset())
    }
}

@Preview(name = "4. Responsive Tablet Two-Pane", showBackground = true, widthDp = 960, heightDp = 680)
@Composable
fun StatGraphEntryScreenTabletPreview() {
    NursingOTAppTheme {
        StatGraphEntryScreen(initialDataset = StatGraphSampleData.dengueMonthlyPopulatedDataset())
    }
}
