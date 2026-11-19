package com.pasindu.nursingotapp.statgraph.ui

import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartReadinessSuggestion
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartToolkitAnalyzer
import com.pasindu.nursingotapp.statgraph.data.CsvColumnOverride
import com.pasindu.nursingotapp.statgraph.data.CsvImportEngine
import com.pasindu.nursingotapp.statgraph.data.CsvImportPreview
import com.pasindu.nursingotapp.statgraph.data.StatGraphSampleData
import com.pasindu.nursingotapp.statgraph.engine.ValidationEngine
import com.pasindu.nursingotapp.statgraph.engine.ValidationReport
import com.pasindu.nursingotapp.statgraph.model.DatasetMetadata
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimeGranularity
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.stats.DatasetSummaryCalculator
import com.pasindu.nursingotapp.statgraph.stats.DatasetSummaryMetrics
import com.pasindu.nursingotapp.statgraph.stats.DerivedCellResult
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldCalculator
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.IsoFields
import java.util.UUID

enum class StatGraphEntryTab(val label: String) {
    ROWS("Rows & Entry"),
    SETUP("Schema & Derived"),
    CSV("CSV Import")
}

data class StatGraphEntryUiState(
    val dataset: StatDataset,
    val activeTab: StatGraphEntryTab = StatGraphEntryTab.ROWS,
    val validationReport: ValidationReport = ValidationEngine.validate(dataset),
    val derivedResults: Map<String, List<DerivedCellResult>> = DerivedFieldCalculator.calculateAll(dataset),
    val summaryMetrics: DatasetSummaryMetrics = DatasetSummaryCalculator.summarize(dataset),
    val chartReadiness: List<ChartReadinessSuggestion> = ChartToolkitAnalyzer.analyzeReadiness(dataset),
    val editingField: StatField? = null,
    val isFieldDialogOpen: Boolean = false,
    val isDerivedDialogOpen: Boolean = false,
    val csvRawText: String = "",
    val csvHasHeaderOverride: Boolean? = null,
    val csvColumnOverrides: Map<Int, CsvColumnOverride> = emptyMap(),
    val csvPreview: CsvImportPreview? = null,
    val csvFileName: String? = null
) {
    companion object {
        fun fromDataset(
            dataset: StatDataset,
            activeTab: StatGraphEntryTab = StatGraphEntryTab.ROWS
        ): StatGraphEntryUiState = StatGraphEntryUiState(
            dataset = dataset,
            activeTab = activeTab
        )
    }
}

/**
 * Pure state-transition helpers for [StatGraphEntryUiState].
 */
object StatGraphEntryController {

    fun recalculate(state: StatGraphEntryUiState, updatedDataset: StatDataset): StatGraphEntryUiState {
        val stampedDataset = updatedDataset.copy(
            metadata = updatedDataset.metadata.copy(updatedAtEpochMillis = System.currentTimeMillis())
        )
        return state.copy(
            dataset = stampedDataset,
            validationReport = ValidationEngine.validate(stampedDataset),
            derivedResults = DerivedFieldCalculator.calculateAll(stampedDataset),
            summaryMetrics = DatasetSummaryCalculator.summarize(stampedDataset),
            chartReadiness = ChartToolkitAnalyzer.analyzeReadiness(stampedDataset)
        )
    }

    fun updateDatasetMeta(
        state: StatGraphEntryUiState,
        name: String,
        description: String
    ): StatGraphEntryUiState = recalculate(
        state,
        state.dataset.copy(name = name, description = description)
    )

    fun saveField(state: StatGraphEntryUiState, field: StatField): StatGraphEntryUiState {
        val existingIndex = state.dataset.fields.indexOfFirst { it.id == field.id }
        val updatedFields = if (existingIndex >= 0) {
            state.dataset.fields.toMutableList().apply { set(existingIndex, field) }
        } else {
            state.dataset.fields + field
        }
        return recalculate(
            state.copy(isFieldDialogOpen = false, editingField = null),
            state.dataset.copy(fields = updatedFields)
        )
    }

    fun confirmSuggestedRole(state: StatGraphEntryUiState, fieldId: String): StatGraphEntryUiState {
        val updatedFields = state.dataset.fields.map { field ->
            if (field.id == fieldId) {
                val targetRole = field.suggestedRole ?: field.role
                field.copy(role = targetRole, isRoleConfirmed = true)
            } else {
                field
            }
        }
        return recalculate(state, state.dataset.copy(fields = updatedFields))
    }

    fun deleteField(state: StatGraphEntryUiState, fieldId: String): StatGraphEntryUiState {
        val updatedFields = state.dataset.fields.filterNot { it.id == fieldId }
        val updatedRows = state.dataset.rows.map { row ->
            row.copy(values = row.values - fieldId)
        }
        return recalculate(
            state,
            state.dataset.copy(fields = updatedFields, rows = updatedRows)
        )
    }

    fun addBlankRow(state: StatGraphEntryUiState): StatGraphEntryUiState {
        val newRow = StatRow(
            id = UUID.randomUUID().toString(),
            periodOrCategoryLabel = "Row ${state.dataset.rows.size + 1}",
            values = emptyMap()
        )
        return recalculate(
            state,
            state.dataset.copy(rows = state.dataset.rows + newRow)
        )
    }

    fun addNextPeriodRow(state: StatGraphEntryUiState): StatGraphEntryUiState {
        val timeField = state.dataset.fields.firstOrNull { it.role is FieldRole.Time }
        if (timeField == null) return addBlankRow(state)

        val timeRole = timeField.role as FieldRole.Time
        val existingPeriods = state.dataset.rows.mapNotNull { row ->
            (row.values[timeField.id] as? StatValue.TimePeriodValue)?.period
        }

        val nextPeriod = if (existingPeriods.isNotEmpty()) {
            existingPeriods.maxBy { it.sortKey() }.nextPeriod()
        } else {
            defaultInitialPeriod(timeRole.granularity)
        }

        val newRow = StatRow(
            id = UUID.randomUUID().toString(),
            periodOrCategoryLabel = nextPeriod.formatDisplay(),
            values = mapOf(
                timeField.id to StatValue.TimePeriodValue(
                    period = nextPeriod,
                    rawText = nextPeriod.formatIso()
                )
            )
        )
        return recalculate(
            state,
            state.dataset.copy(rows = state.dataset.rows + newRow)
        )
    }

    fun duplicateRow(state: StatGraphEntryUiState, rowId: String): StatGraphEntryUiState {
        val index = state.dataset.rows.indexOfFirst { it.id == rowId }
        if (index < 0) return state
        val source = state.dataset.rows[index]
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            values = source.values.toMap()
        )
        val updatedRows = state.dataset.rows.toMutableList().apply {
            add(index + 1, copy)
        }
        return recalculate(state, state.dataset.copy(rows = updatedRows))
    }

    fun deleteRow(state: StatGraphEntryUiState, rowId: String): StatGraphEntryUiState {
        val updatedRows = state.dataset.rows.filterNot { it.id == rowId }
        return recalculate(state, state.dataset.copy(rows = updatedRows))
    }

    fun updateCellValue(
        state: StatGraphEntryUiState,
        rowId: String,
        fieldId: String,
        value: StatValue?
    ): StatGraphEntryUiState {
        val updatedRows = state.dataset.rows.map { row ->
            if (row.id != rowId) {
                row
            } else {
                val updatedMap = row.values.toMutableMap()
                if (value == null || value is StatValue.Missing) {
                    updatedMap.remove(fieldId)
                } else {
                    updatedMap[fieldId] = value
                }
                row.copy(values = updatedMap)
            }
        }
        return recalculate(state, state.dataset.copy(rows = updatedRows))
    }

    fun addDerivedField(
        state: StatGraphEntryUiState,
        definition: DerivedFieldDefinition
    ): StatGraphEntryUiState = recalculate(
        state.copy(isDerivedDialogOpen = false),
        state.dataset.copy(derivedFields = state.dataset.derivedFields + definition)
    )

    fun removeDerivedField(state: StatGraphEntryUiState, derivedId: String): StatGraphEntryUiState =
        recalculate(
            state,
            state.dataset.copy(derivedFields = state.dataset.derivedFields.filterNot { it.id == derivedId })
        )

    fun updateCsvInput(
        state: StatGraphEntryUiState,
        rawText: String,
        fileName: String? = state.csvFileName,
        hasHeaderOverride: Boolean? = state.csvHasHeaderOverride,
        overrides: Map<Int, CsvColumnOverride> = state.csvColumnOverrides
    ): StatGraphEntryUiState {
        val preview = if (rawText.isBlank()) {
            null
        } else {
            CsvImportEngine.buildPreview(
                rawText = rawText,
                datasetName = state.dataset.name.ifBlank { "Imported CSV Dataset" },
                datasetDescription = state.dataset.description,
                hasHeaderOverride = hasHeaderOverride,
                columnOverrides = overrides
            )
        }
        return state.copy(
            csvRawText = rawText,
            csvFileName = fileName,
            csvHasHeaderOverride = hasHeaderOverride,
            csvColumnOverrides = overrides,
            csvPreview = preview
        )
    }

    fun loadSampleCsv(state: StatGraphEntryUiState): StatGraphEntryUiState =
        updateCsvInput(
            state = state,
            rawText = StatGraphSampleData.sampleCsvText,
            fileName = "sample_surveillance.csv",
            hasHeaderOverride = null,
            overrides = emptyMap()
        )

    fun acceptCsvPreview(state: StatGraphEntryUiState): StatGraphEntryUiState {
        val preview = state.csvPreview ?: return state
        val imported = preview.candidateDataset.copy(
            id = state.dataset.id,
            name = state.dataset.name.ifBlank { preview.candidateDataset.name },
            description = state.dataset.description.ifBlank { preview.candidateDataset.description },
            fields = preview.candidateDataset.fields.map { it.copy(isRoleConfirmed = true) },
            metadata = DatasetMetadata(sourceLabel = state.csvFileName?.let { "CSV: $it" } ?: "CSV Import")
        )
        return recalculate(
            state.copy(
                activeTab = StatGraphEntryTab.ROWS,
                csvPreview = null,
                csvRawText = "",
                csvFileName = null,
                csvColumnOverrides = emptyMap(),
                csvHasHeaderOverride = null
            ),
            imported
        )
    }

    private fun defaultInitialPeriod(granularity: TimeGranularity): TimePeriod {
        val today = LocalDate.now()
        return when (granularity) {
            TimeGranularity.DAY -> TimePeriod.Day(today)
            TimeGranularity.WEEK -> TimePeriod.Week(
                year = today.get(IsoFields.WEEK_BASED_YEAR),
                weekOfYear = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            )
            TimeGranularity.MONTH -> TimePeriod.Month(YearMonth.from(today))
            TimeGranularity.YEAR -> TimePeriod.Year(today.year)
        }
    }
}
