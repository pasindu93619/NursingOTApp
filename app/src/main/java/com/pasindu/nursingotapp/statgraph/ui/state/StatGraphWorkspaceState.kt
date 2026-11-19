package com.pasindu.nursingotapp.statgraph.ui.state

import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartBarColorMode
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartLabelDisplayMode
import com.pasindu.nursingotapp.statgraph.data.StatGraphSampleData
import com.pasindu.nursingotapp.statgraph.engine.RoleAutoDetector
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartCatalogue
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport
import com.pasindu.nursingotapp.statgraph.engine.recommendation.RecommendationEngine
import com.pasindu.nursingotapp.statgraph.engine.recommendation.RecommendationSuiteResult
import com.pasindu.nursingotapp.statgraph.model.DatasetMetadata
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.orchestration.ChartMappingSelection
import com.pasindu.nursingotapp.statgraph.orchestration.ChartPainterResolver
import com.pasindu.nursingotapp.statgraph.orchestration.ChartRenderState
import com.pasindu.nursingotapp.statgraph.orchestration.ChartToolkitSupport
import com.pasindu.nursingotapp.statgraph.orchestration.PainterSupportResult
import com.pasindu.nursingotapp.statgraph.orchestration.StatGraphOrchestrator
import com.pasindu.nursingotapp.statgraph.stats.DerivedCalculationType
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import com.pasindu.nursingotapp.statgraph.stats.FieldDescriptiveSummary
import com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryController
import com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryTab
import com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryUiState
import java.time.YearMonth

/**
 * Primary navigation steps across the Stat Graph Studio Workspace workflow:
 * Enter/Import Data & Confirm Roles -> Explore Recommendations & Chart -> Inspect Stats & Audit.
 */
enum class WorkspaceSection(
    val stepNumber: Int,
    val label: String,
    val shortLabel: String,
) {
    DATA(1, "1. Add Data", "Data"),
    EXPLORE(2, "2. Your Chart", "Chart"),
    INSIGHTS(3, "3. Summary & Checks", "Summary"),
}

/**
 * Built-in standalone aggregate demonstrations and templates for immediate exploration.
 */
enum class WorkspaceSamplePreset(
    val title: String,
    val subtitle: String,
) {
    DENGUE_FOUR_MONTH_DEMO(
        title = "Dengue Surveillance Demo",
        subtitle = "Jan–Apr monthly dengue share (5%, 7.5%, 5%, 10%)",
    ),
    DENGUE_FIVE_MONTH_WARD(
        title = "Ward Occupancy & Dengue",
        subtitle = "5-month aggregate ward counts, bed occupancy & 3-mo avg",
    ),
    WARNINGS_AND_CI(
        title = "Warnings & 95% CI Demo",
        subtitle = "Small counts (<5), missing gaps, and confidence limits",
    ),
    BLANK(
        title = "Blank Dataset",
        subtitle = "Start from scratch with custom schema or CSV",
    ),
}

/**
 * Describes which axis/series mapping controls apply to the currently selected chart specification.
 */
data class FieldMappingCapability(
    val chartSpecId: String?,
    val chartKind: ChartKind?,
    val supportsXAxis: Boolean,
    val supportsYMeasures: Boolean,
    val supportsMultipleY: Boolean,
    val supportsGroupSeries: Boolean,
    val supportsDerivedRates: Boolean,
    val supportsEstimateField: Boolean,
    val xAxisControlLabel: String,
    val yAxisControlLabel: String,
)

/**
 * Sample presets specific to Phase 5 demonstration requirements.
 */
object StatGraphWorkspaceSamples {

    /**
     * Simulated four-month Dengue Surveillance demonstration dataset:
     * - January: 1000 total patients, 50 dengue cases -> 5%
     * - February: 1200 total patients, 90 dengue cases -> 7.5%
     * - March: 900 total patients, 45 dengue cases -> 5%
     * - April: 1100 total patients, 110 dengue cases -> 10%
     */
    fun dengueFourMonthDemoDataset(confirmRolesImmediately: Boolean = false): StatDataset {
        val monthSuggestion = RoleAutoDetector.suggestRole(
            fieldName = "Month",
            unit = "",
            sampleValues = listOf("2026-01", "2026-02", "2026-03", "2026-04"),
        )
        val totalSuggestion = RoleAutoDetector.suggestRole(
            fieldName = "Total patients",
            unit = "patients",
            sampleValues = listOf("1000", "1200", "900", "1100"),
        )
        val totalField = StatField(
            id = "f_total_patients",
            name = "Total patients",
            unit = "patients",
            role = FieldRole.Measure,
            suggestedRole = totalSuggestion.suggestedRole,
            isRoleConfirmed = confirmRolesImmediately,
            minValue = 0.0,
            isRequired = true,
        )
        val dengueSuggestion = RoleAutoDetector.suggestRole(
            fieldName = "Dengue cases",
            unit = "patients",
            sampleValues = listOf("50", "90", "45", "110"),
            existingFields = listOf(totalField),
        )
        val monthField = StatField(
            id = "f_month",
            name = "Month",
            role = FieldRole.Time.Month,
            suggestedRole = monthSuggestion.suggestedRole,
            isRoleConfirmed = confirmRolesImmediately,
            isRequired = true,
        )
        val dengueField = StatField(
            id = "f_dengue_cases",
            name = "Dengue cases",
            unit = "patients",
            role = FieldRole.PartOfMeasure(parentFieldId = totalField.id),
            suggestedRole = dengueSuggestion.suggestedRole,
            isRoleConfirmed = confirmRolesImmediately,
            minValue = 0.0,
        )

        fun demoRow(id: String, year: Int, month: Int, total: Long, dengue: Long): StatRow {
            val tp = TimePeriod.Month(YearMonth.of(year, month))
            return StatRow(
                id = id,
                periodOrCategoryLabel = tp.formatDisplay(),
                values = mapOf(
                    monthField.id to StatValue.TimePeriodValue(tp, tp.formatIso()),
                    totalField.id to StatValue.WholeNumber(total),
                    dengueField.id to StatValue.WholeNumber(dengue),
                ),
            )
        }

        val rows = listOf(
            demoRow("demo_jan", 2026, 1, total = 1000L, dengue = 50L),
            demoRow("demo_feb", 2026, 2, total = 1200L, dengue = 90L),
            demoRow("demo_mar", 2026, 3, total = 900L, dengue = 45L),
            demoRow("demo_apr", 2026, 4, total = 1100L, dengue = 110L),
        )

        val derived = listOf(
            DerivedFieldDefinition(
                id = "d_dengue_pct",
                name = "Dengue Rate",
                unit = "%",
                calculation = DerivedCalculationType.RatePercentage(
                    numeratorFieldId = dengueField.id,
                    denominatorFieldId = totalField.id,
                ),
                formulaSummary = "(Dengue cases / Total patients) × 100",
            ),
        )

        return StatDataset(
            id = "ds_dengue_demo_4mo",
            name = "Dengue Surveillance Demonstration",
            description = "Simulated monthly aggregate ward totals & dengue proportion (no patient identifiers).",
            fields = listOf(monthField, totalField, dengueField),
            rows = rows,
            derivedFields = derived,
            metadata = DatasetMetadata(sourceLabel = "Simulated Demo Dataset"),
        )
    }

    fun datasetForPreset(preset: WorkspaceSamplePreset): StatDataset = when (preset) {
        WorkspaceSamplePreset.DENGUE_FOUR_MONTH_DEMO -> dengueFourMonthDemoDataset()
        WorkspaceSamplePreset.DENGUE_FIVE_MONTH_WARD -> StatGraphSampleData.dengueMonthlyPopulatedDataset()
        WorkspaceSamplePreset.WARNINGS_AND_CI -> StatGraphSampleData.validationWarningDataset()
        WorkspaceSamplePreset.BLANK -> StatGraphSampleData.emptyDataset()
    }
}

/**
 * Cohesive UI state for Phase 5 Stat Graph Studio Workspace.
 *
 * Wraps Phase 1 [StatGraphEntryUiState] cleanly alongside cached Phase 2 recommendation/diagnostics
 * outputs and Phase 4 [ChartRenderState], keeping pure computation separated from rendering.
 */
data class StatGraphWorkspaceUiState(
    val entryState: StatGraphEntryUiState,
    val activeSection: WorkspaceSection,
    val mappingSelection: ChartMappingSelection,
    val isLoading: Boolean,
    val activePreset: WorkspaceSamplePreset?,
    val recommendationSuite: RecommendationSuiteResult,
    val diagnosticsReport: DatasetDiagnosticsReport,
    val renderState: ChartRenderState,
    val showAllCatalogueCharts: Boolean = false,
    val isResearchMode: Boolean = false,
    val showGuidedStart: Boolean = false,
    val selectedPalette: com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette? = null,
) {
    val dataset: StatDataset
        get() = entryState.dataset

    val fieldSummaries: List<FieldDescriptiveSummary>
        get() = dataset.fields.mapNotNull { field ->
            diagnosticsReport.fieldSummaries[field.id]
        }

    val unconfirmedRoleCount: Int
        get() = dataset.fields.count { !it.isRoleConfirmed }

    val hasCustomMapping: Boolean
        get() = mappingSelection.chartSpecId != null ||
            mappingSelection.xFieldId != null ||
            mappingSelection.yFieldIds.isNotEmpty() ||
            mappingSelection.groupFieldId != null ||
            mappingSelection.derivedFieldIds.isNotEmpty() ||
            !mappingSelection.userIntentQuery.isNullOrBlank() ||
            mappingSelection.labelDisplayMode != ChartLabelDisplayMode.AUTO ||
            mappingSelection.barColorMode != ChartBarColorMode.AUTO

    val mappingCapability: FieldMappingCapability
        get() = StatGraphWorkspaceController.resolveMappingCapability(
            dataset = dataset,
            selection = mappingSelection,
            renderState = renderState,
        )
}

/**
 * Pure-Kotlin controller managing workspace state transitions and orchestrating Phase 1–4 engines.
 */
object StatGraphWorkspaceController {

    fun createInitial(
        dataset: StatDataset = StatGraphWorkspaceSamples.dengueFourMonthDemoDataset(),
        activeSection: WorkspaceSection? = null,
        mappingSelection: ChartMappingSelection = ChartMappingSelection(),
        activePreset: WorkspaceSamplePreset? = if (dataset.id == "ds_dengue_demo_4mo") {
            WorkspaceSamplePreset.DENGUE_FOUR_MONTH_DEMO
        } else {
            null
        },
        showAllCatalogueCharts: Boolean = false,
    ): StatGraphWorkspaceUiState {
        val defaultSection = activeSection ?: if (dataset.fields.isEmpty() || dataset.rows.isEmpty()) {
            WorkspaceSection.DATA
        } else {
            WorkspaceSection.EXPLORE
        }
        val initialEntry = StatGraphEntryUiState.fromDataset(
            dataset = dataset,
            activeTab = com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryTab.ROWS,
        )
        return buildWorkspaceState(
            entryState = initialEntry,
            activeSection = defaultSection,
            mappingSelection = mappingSelection,
            isLoading = false,
            activePreset = activePreset,
            showAllCatalogueCharts = showAllCatalogueCharts,
        )
    }

    fun fromPreset(preset: WorkspaceSamplePreset): StatGraphWorkspaceUiState {
        val ds = StatGraphWorkspaceSamples.datasetForPreset(preset)
        return createInitial(
            dataset = ds,
            activeSection = if (preset == WorkspaceSamplePreset.BLANK) {
                WorkspaceSection.DATA
            } else {
                WorkspaceSection.EXPLORE
            },
            mappingSelection = ChartMappingSelection(),
            activePreset = preset,
        )
    }

    fun setSection(
        state: StatGraphWorkspaceUiState,
        section: WorkspaceSection,
    ): StatGraphWorkspaceUiState = state.copy(activeSection = section)

    fun setDataTab(
        state: StatGraphWorkspaceUiState,
        tab: com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryTab,
    ): StatGraphWorkspaceUiState = state.copy(
        activeSection = WorkspaceSection.DATA,
        entryState = state.entryState.copy(activeTab = tab),
    )

    fun openCsvImport(state: StatGraphWorkspaceUiState): StatGraphWorkspaceUiState =
        setDataTab(state, com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryTab.CSV)

    fun updateEntryState(
        state: StatGraphWorkspaceUiState,
        updatedEntryState: StatGraphEntryUiState,
    ): StatGraphWorkspaceUiState {
        if (updatedEntryState.dataset == state.entryState.dataset) {
            return state.copy(entryState = updatedEntryState)
        }
        val cleanedSelection = sanitizeSelectionForDataset(
            dataset = updatedEntryState.dataset,
            selection = state.mappingSelection,
        )
        return buildWorkspaceState(
            entryState = updatedEntryState,
            activeSection = state.activeSection,
            mappingSelection = cleanedSelection,
            isLoading = state.isLoading,
            activePreset = state.activePreset,
            showAllCatalogueCharts = state.showAllCatalogueCharts,
        ).copy(
            isResearchMode = state.isResearchMode,
            showGuidedStart = state.showGuidedStart,
            selectedPalette = state.selectedPalette,
        )
    }

    fun confirmAllSuggestedRoles(state: StatGraphWorkspaceUiState): StatGraphWorkspaceUiState {
        val updatedFields = state.dataset.fields.map { field ->
            val targetRole = when {
                field.role is FieldRole.PartOfMeasure && field.suggestedRole is FieldRole.Measure -> field.role
                else -> field.suggestedRole ?: field.role
            }
            field.copy(role = targetRole, isRoleConfirmed = true)
        }
        val updatedEntry = com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryController.recalculate(
            state = state.entryState,
            updatedDataset = state.dataset.copy(fields = updatedFields),
        )
        return updateEntryState(state, updatedEntry)
    }

    fun groupIntoFourWeekBlocks(state: StatGraphWorkspaceUiState): StatGraphWorkspaceUiState {
        val groupedDataset = com.pasindu.nursingotapp.statgraph.chart.toolkit.BarDensityPolicy
            .groupIntoFourWeekBlocks(state.dataset)
        val updatedEntry = com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryController.recalculate(
            state = state.entryState,
            updatedDataset = groupedDataset,
        )
        return updateEntryState(state, updatedEntry)
    }

    fun selectChartSpec(
        state: StatGraphWorkspaceUiState,
        specId: String?,
    ): StatGraphWorkspaceUiState {
        val nextKind = specId?.let { ChartToolkitSupport.chartKindFor(it) }
        val adjusted = sanitizeSelectionForChartChange(
            dataset = state.dataset,
            selection = state.mappingSelection.copy(chartSpecId = specId),
            targetKind = nextKind,
        )
        return buildWorkspaceState(
            entryState = state.entryState,
            activeSection = state.activeSection,
            mappingSelection = adjusted,
            isLoading = state.isLoading,
            activePreset = state.activePreset,
            showAllCatalogueCharts = state.showAllCatalogueCharts,
        ).copy(
            isResearchMode = state.isResearchMode,
            showGuidedStart = state.showGuidedStart,
            selectedPalette = state.selectedPalette,
        )
    }

    fun setXField(
        state: StatGraphWorkspaceUiState,
        fieldId: String?,
    ): StatGraphWorkspaceUiState = refreshWithSelection(
        state = state,
        selection = state.mappingSelection.copy(xFieldId = fieldId),
    )

    fun toggleYField(
        state: StatGraphWorkspaceUiState,
        fieldId: String,
    ): StatGraphWorkspaceUiState {
        val cap = state.mappingCapability
        val current = state.mappingSelection.yFieldIds
        val updatedY = if (cap.supportsMultipleY) {
            if (fieldId in current) current - fieldId else current + fieldId
        } else {
            if (current == listOf(fieldId)) emptyList() else listOf(fieldId)
        }
        val updatedDerived = if (!cap.supportsMultipleY && updatedY.isNotEmpty()) {
            emptyList()
        } else {
            state.mappingSelection.derivedFieldIds
        }
        return refreshWithSelection(
            state = state,
            selection = state.mappingSelection.copy(
                yFieldIds = updatedY,
                derivedFieldIds = updatedDerived,
            ),
        )
    }

    fun toggleDerivedField(
        state: StatGraphWorkspaceUiState,
        derivedId: String,
    ): StatGraphWorkspaceUiState {
        val cap = state.mappingCapability
        val current = state.mappingSelection.derivedFieldIds
        val updatedDerived = if (cap.supportsMultipleY) {
            if (derivedId in current) current - derivedId else current + derivedId
        } else {
            if (current == listOf(derivedId)) emptyList() else listOf(derivedId)
        }
        val updatedY = if (!cap.supportsMultipleY && updatedDerived.isNotEmpty()) {
            emptyList()
        } else {
            state.mappingSelection.yFieldIds
        }
        return refreshWithSelection(
            state = state,
            selection = state.mappingSelection.copy(
                yFieldIds = updatedY,
                derivedFieldIds = updatedDerived,
            ),
        )
    }

    fun setGroupField(
        state: StatGraphWorkspaceUiState,
        fieldId: String?,
    ): StatGraphWorkspaceUiState = refreshWithSelection(
        state = state,
        selection = state.mappingSelection.copy(groupFieldId = fieldId),
    )

    fun setUserIntentQuery(
        state: StatGraphWorkspaceUiState,
        query: String?,
    ): StatGraphWorkspaceUiState = refreshWithSelection(
        state = state,
        selection = state.mappingSelection.copy(
            userIntentQuery = query?.takeIf { it.isNotBlank() },
        ),
    )

    fun resetMappings(state: StatGraphWorkspaceUiState): StatGraphWorkspaceUiState = refreshWithSelection(
        state = state,
        selection = ChartMappingSelection(),
    )

    fun setLabelDisplayMode(
        state: StatGraphWorkspaceUiState,
        mode: ChartLabelDisplayMode,
    ): StatGraphWorkspaceUiState = refreshWithSelection(
        state = state,
        selection = state.mappingSelection.copy(labelDisplayMode = mode),
    )

    fun setBarColorMode(
        state: StatGraphWorkspaceUiState,
        mode: ChartBarColorMode,
    ): StatGraphWorkspaceUiState = refreshWithSelection(
        state = state,
        selection = state.mappingSelection.copy(barColorMode = mode),
    )

    fun toggleShowAllCatalogueCharts(
        state: StatGraphWorkspaceUiState,
        showAll: Boolean,
    ): StatGraphWorkspaceUiState = state.copy(showAllCatalogueCharts = showAll)

    fun setResearchMode(
        state: StatGraphWorkspaceUiState,
        isResearchMode: Boolean,
    ): StatGraphWorkspaceUiState = state.copy(isResearchMode = isResearchMode)

    fun setShowGuidedStart(
        state: StatGraphWorkspaceUiState,
        show: Boolean,
    ): StatGraphWorkspaceUiState = state.copy(showGuidedStart = show)

    fun setPalette(
        state: StatGraphWorkspaceUiState,
        palette: com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette?,
    ): StatGraphWorkspaceUiState = state.copy(selectedPalette = palette)

    fun fromStarterTemplate(
        template: com.pasindu.nursingotapp.statgraph.data.StatGraphStarterTemplate,
        isResearchMode: Boolean = false,
        selectedPalette: com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette? = null,
    ): StatGraphWorkspaceUiState = createInitial(
        dataset = template.buildDataset(),
        activeSection = WorkspaceSection.DATA,
        activePreset = null,
    ).copy(
        isResearchMode = isResearchMode,
        showGuidedStart = false,
        selectedPalette = selectedPalette,
    )

    fun refreshAnalysis(state: StatGraphWorkspaceUiState): StatGraphWorkspaceUiState =
        buildWorkspaceState(
            entryState = com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryController.recalculate(
                state.entryState,
                state.dataset,
            ),
            activeSection = state.activeSection,
            mappingSelection = state.mappingSelection,
            isLoading = state.isLoading,
            activePreset = state.activePreset,
            showAllCatalogueCharts = state.showAllCatalogueCharts,
        ).copy(
            isResearchMode = state.isResearchMode,
            showGuidedStart = state.showGuidedStart,
            selectedPalette = state.selectedPalette,
        )

    fun resolveMappingCapability(
        dataset: StatDataset,
        selection: ChartMappingSelection,
        renderState: ChartRenderState,
    ): FieldMappingCapability {
        val resolvedSpecId = when (renderState) {
            is ChartRenderState.Ready -> renderState.configuration.specification.id
            is ChartRenderState.UnsupportedChartRenderer -> renderState.requestedSpec.id
            else -> selection.chartSpecId
        }
        val kind = when (renderState) {
            is ChartRenderState.Ready -> renderState.configuration.chartKind
            else -> resolvedSpecId?.let { ChartToolkitSupport.chartKindFor(it) }
        }
        val spec = resolvedSpecId?.let { id -> ChartCatalogue.findById(id) }
        val supportKnown = resolvedSpecId == null || resolvedSpecId in ChartToolkitSupport.supportedSpecIds

        if (!supportKnown) {
            return FieldMappingCapability(
                chartSpecId = resolvedSpecId,
                chartKind = null,
                supportsXAxis = false,
                supportsYMeasures = false,
                supportsMultipleY = false,
                supportsGroupSeries = false,
                supportsDerivedRates = false,
                supportsEstimateField = dataset.fields.any { it.role is FieldRole.EstimateWithLimits } ||
                    (spec?.id in setOf("error_bar_chart", "range_plot", "forest_plot")),
                xAxisControlLabel = "Domain / X-Axis",
                yAxisControlLabel = "Value / Y-Axis",
            )
        }

        val hasEstimate = dataset.fields.any { it.role is FieldRole.EstimateWithLimits }
        return when (kind) {
            ChartKind.SCATTER,
            ChartKind.CONNECTED_SCATTER,
            ChartKind.BUBBLE,
            ChartKind.HEXBIN -> FieldMappingCapability(
                chartSpecId = resolvedSpecId,
                chartKind = kind,
                supportsXAxis = true,
                supportsYMeasures = true,
                supportsMultipleY = false,
                supportsGroupSeries = true,
                supportsDerivedRates = dataset.derivedFields.isNotEmpty(),
                supportsEstimateField = hasEstimate,
                xAxisControlLabel = "X Variable (Numeric)",
                yAxisControlLabel = "Y Variable (Numeric)",
            )
            ChartKind.LINE,
            ChartKind.BAR,
            ChartKind.HORIZONTAL_BAR,
            ChartKind.LOLLIPOP,
            ChartKind.DOT_PLOT,
            ChartKind.AREA,
            ChartKind.STEP,
            ChartKind.RANGE_BAND,
            ChartKind.CYCLE_PLOT,
            ChartKind.CALENDAR_HEATMAP,
            ChartKind.DIVERGING_BAR,
            ChartKind.WAFFLE,
            ChartKind.PARETO,
            ChartKind.ECDF,
            ChartKind.QQ_PLOT,
            ChartKind.STEM_AND_LEAF,
            ChartKind.HISTOGRAM,
            ChartKind.BOX_PLOT,
            ChartKind.VIOLIN,
            ChartKind.DENSITY,
            ChartKind.TREEMAP,
            ChartKind.FUNNEL,
            ChartKind.STRIP_PLOT,
            ChartKind.ERROR_BAR,
            ChartKind.RANGE_PLOT,
            ChartKind.FOREST_PLOT,
            ChartKind.CONTROL_CHART,
            ChartKind.KAPLAN_MEIER,
            ChartKind.PIE,
            ChartKind.DONUT,
            ChartKind.WATERFALL,
            ChartKind.MOSAIC,
            ChartKind.SUNBURST -> FieldMappingCapability(
                chartSpecId = resolvedSpecId,
                chartKind = kind,
                supportsXAxis = true,
                supportsYMeasures = true,
                supportsMultipleY = false,
                supportsGroupSeries = false,
                supportsDerivedRates = dataset.derivedFields.isNotEmpty(),
                supportsEstimateField = hasEstimate,
                xAxisControlLabel = "X Domain (Time / Category)",
                yAxisControlLabel = "Y Measure / Rate",
            )
            ChartKind.MULTI_LINE,
            ChartKind.GROUPED_BAR,
            ChartKind.STACKED_BAR,
            ChartKind.PERCENT_STACKED_BAR,
            ChartKind.STACKED_AREA,
            ChartKind.SLOPE,
            ChartKind.BUMP,
            ChartKind.SPARKLINE_MULTIPLES,
            ChartKind.BULLET,
            ChartKind.DUMBBELL,
            ChartKind.RADAR,
            ChartKind.LIKERT_DIVERGING,
            ChartKind.CORRELATION_HEATMAP,
            ChartKind.POPULATION_PYRAMID,
            ChartKind.SCATTERPLOT_MATRIX -> FieldMappingCapability(
                chartSpecId = resolvedSpecId,
                chartKind = kind,
                supportsXAxis = true,
                supportsYMeasures = true,
                supportsMultipleY = true,
                supportsGroupSeries = true,
                supportsDerivedRates = dataset.derivedFields.isNotEmpty(),
                supportsEstimateField = hasEstimate,
                xAxisControlLabel = "X Domain (Time / Category)",
                yAxisControlLabel = "Y Measures / Series",
            )
            else -> FieldMappingCapability(
                chartSpecId = resolvedSpecId,
                chartKind = null,
                supportsXAxis = dataset.fields.isNotEmpty(),
                supportsYMeasures = dataset.fields.any { it.role.isNumeric },
                supportsMultipleY = true,
                supportsGroupSeries = dataset.fields.any { it.role is FieldRole.Group || it.role is FieldRole.Category },
                supportsDerivedRates = dataset.derivedFields.isNotEmpty(),
                supportsEstimateField = hasEstimate,
                xAxisControlLabel = "X Domain Axis",
                yAxisControlLabel = "Y Measure / Rate",
            )
        }
    }

    private fun refreshWithSelection(
        state: StatGraphWorkspaceUiState,
        selection: ChartMappingSelection,
    ): StatGraphWorkspaceUiState = buildWorkspaceState(
        entryState = state.entryState,
        activeSection = state.activeSection,
        mappingSelection = selection,
        isLoading = state.isLoading,
        activePreset = state.activePreset,
        showAllCatalogueCharts = state.showAllCatalogueCharts,
    ).copy(
        isResearchMode = state.isResearchMode,
        showGuidedStart = state.showGuidedStart,
        selectedPalette = state.selectedPalette,
    )

    private fun sanitizeSelectionForDataset(
        dataset: StatDataset,
        selection: ChartMappingSelection,
    ): ChartMappingSelection {
        val validFieldIds = dataset.fields.map { it.id }.toSet()
        val validDerivedIds = dataset.derivedFields.map { it.id }.toSet()
        return selection.copy(
            xFieldId = selection.xFieldId?.takeIf { it in validFieldIds || it in validDerivedIds },
            yFieldIds = selection.yFieldIds.filter { it in validFieldIds },
            groupFieldId = selection.groupFieldId?.takeIf { it in validFieldIds },
            derivedFieldIds = selection.derivedFieldIds.filter { it in validDerivedIds },
        )
    }

    private fun sanitizeSelectionForChartChange(
        dataset: StatDataset,
        selection: ChartMappingSelection,
        targetKind: ChartKind?,
    ): ChartMappingSelection {
        val fieldById = dataset.fields.associateBy { it.id }
        val derivedIds = dataset.derivedFields.map { it.id }.toSet()

        val validX = selection.xFieldId?.let { xId ->
            when (targetKind) {
                ChartKind.SCATTER -> xId.takeIf { fieldById[it]?.role?.isNumeric == true || it in derivedIds }
                null -> xId
                else -> xId.takeIf {
                    val role = fieldById[it]?.role
                    role is FieldRole.Time || role is FieldRole.Category
                }
            }
        }
        val validY = if (targetKind == ChartKind.SCATTER || targetKind == ChartKind.LINE || targetKind == ChartKind.BAR) {
            selection.yFieldIds.take(1)
        } else {
            selection.yFieldIds
        }
        return selection.copy(
            xFieldId = validX,
            yFieldIds = validY,
        )
    }

    private fun buildWorkspaceState(
        entryState: StatGraphEntryUiState,
        activeSection: WorkspaceSection,
        mappingSelection: ChartMappingSelection,
        isLoading: Boolean,
        activePreset: WorkspaceSamplePreset?,
        showAllCatalogueCharts: Boolean,
    ): StatGraphWorkspaceUiState {
        val renderState = StatGraphOrchestrator.orchestrate(
            dataset = entryState.dataset,
            selection = mappingSelection,
            isLoading = isLoading,
        )
        val suite = when (renderState) {
            is ChartRenderState.Ready -> renderState.suite
            is ChartRenderState.EmptyDataset -> renderState.suite
            is ChartRenderState.InvalidDataset -> renderState.suite
            is ChartRenderState.NoSuitableRecommendation -> renderState.suite
            is ChartRenderState.UnsupportedChartRenderer -> renderState.suite
            is ChartRenderState.RenderingError -> renderState.suite
            ChartRenderState.Loading -> null
        } ?: RecommendationEngine.recommend(
            dataset = entryState.dataset,
            userIntentQuery = mappingSelection.userIntentQuery,
        )

        return StatGraphWorkspaceUiState(
            entryState = entryState,
            activeSection = activeSection,
            mappingSelection = mappingSelection,
            isLoading = isLoading,
            activePreset = activePreset,
            recommendationSuite = suite,
            diagnosticsReport = suite.diagnosticsReport,
            renderState = renderState,
            showAllCatalogueCharts = showAllCatalogueCharts,
        )
    }
}
