package com.pasindu.nursingotapp.statgraph.data

import com.pasindu.nursingotapp.statgraph.engine.RoleAutoDetector
import com.pasindu.nursingotapp.statgraph.engine.RoleDetectionResult
import com.pasindu.nursingotapp.statgraph.engine.ValidationEngine
import com.pasindu.nursingotapp.statgraph.engine.ValidationReport
import com.pasindu.nursingotapp.statgraph.model.DatasetMetadata
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimePeriodParser
import java.util.Locale
import java.util.UUID

data class CsvColumnOverride(
    val name: String? = null,
    val role: FieldRole? = null,
    val unit: String? = null,
    val isRoleConfirmed: Boolean = false
)

data class CsvColumnPreview(
    val columnIndex: Int,
    val headerName: String,
    val sampleValues: List<String>,
    val detection: RoleDetectionResult,
    val selectedRole: FieldRole,
    val unit: String,
    val isRoleConfirmed: Boolean,
    val missingCount: Int
)

data class CsvImportPreview(
    val rawRowCount: Int,
    val detectedHasHeader: Boolean,
    val effectiveHasHeader: Boolean,
    val columns: List<CsvColumnPreview>,
    val candidateDataset: StatDataset,
    val validationReport: ValidationReport,
    val parseWarnings: List<String>
) {
    val canAcceptCleanly: Boolean
        get() = candidateDataset.fields.isNotEmpty() &&
            candidateDataset.rows.isNotEmpty() &&
            !validationReport.hasBlockingErrors
}

/**
 * Pure-Kotlin RFC-4180 CSV parser and pre-import validator for Stat Graph Studio.
 *
 * Guarantees:
 * - Empty cells remain missing (`null`) and are never coerced to zero.
 * - Headers are detected heuristically and can be toggled by the user.
 * - Field roles are suggested via [RoleAutoDetector] and validated before acceptance.
 */
object CsvImportEngine {

    private val missingTokens = setOf("", "na", "n/a", "null", "missing", "—")

    fun buildPreview(
        rawText: String,
        datasetName: String = "Imported Clinical Dataset",
        datasetDescription: String = "",
        hasHeaderOverride: Boolean? = null,
        columnOverrides: Map<Int, CsvColumnOverride> = emptyMap()
    ): CsvImportPreview {
        val records = parseCsvRecords(rawText)
        if (records.isEmpty()) {
            val emptyDs = StatDataset(
                name = datasetName.ifBlank { "Imported Dataset" },
                description = datasetDescription,
                metadata = DatasetMetadata(sourceLabel = "CSV Import")
            )
            return CsvImportPreview(
                rawRowCount = 0,
                detectedHasHeader = false,
                effectiveHasHeader = false,
                columns = emptyList(),
                candidateDataset = emptyDs,
                validationReport = ValidationReport(),
                parseWarnings = emptyList()
            )
        }

        val maxColumns = records.maxOf { it.size }.coerceAtLeast(1)
        val normalizedRecords = records.map { row ->
            if (row.size < maxColumns) row + List(maxColumns - row.size) { "" } else row
        }

        val detectedHasHeader = detectHeaderRow(normalizedRecords)
        val effectiveHasHeader = hasHeaderOverride ?: detectedHasHeader

        val headerNames = if (effectiveHasHeader) {
            normalizedRecords.first().mapIndexed { idx, rawHeader ->
                rawHeader.trim().ifBlank { "Column ${idx + 1}" }
            }
        } else {
            (0 until maxColumns).map { idx -> "Column ${idx + 1}" }
        }

        val dataRows = if (effectiveHasHeader) normalizedRecords.drop(1) else normalizedRecords
        val parseWarnings = mutableListOf<String>()

        // Pass 1: Detect suggested roles for each column
        val preliminaryFields = mutableListOf<StatField>()
        val columnPreviews = mutableListOf<CsvColumnPreview>()

        for (colIdx in 0 until maxColumns) {
            val override = columnOverrides[colIdx]
            val colName = (override?.name ?: headerNames[colIdx]).trim().ifBlank { "Column ${colIdx + 1}" }
            val columnCells = dataRows.map { it[colIdx] }
            val sampleNonMissing = columnCells
                .map { it.trim() }
                .filterNot { it.lowercase(Locale.ENGLISH) in missingTokens }
                .take(8)
            val missingCount = columnCells.count { it.trim().lowercase(Locale.ENGLISH) in missingTokens }

            val detection = RoleAutoDetector.suggestRole(
                fieldName = colName,
                unit = override?.unit.orEmpty(),
                sampleValues = sampleNonMissing,
                existingFields = preliminaryFields
            )

            val selectedRole = override?.role ?: detection.suggestedRole
            val resolvedUnit = override?.unit ?: detection.suggestedUnit.orEmpty()
            val confirmed = override?.isRoleConfirmed ?: false

            val fieldId = "csv_col_$colIdx"
            val statField = StatField(
                id = fieldId,
                name = colName,
                unit = resolvedUnit,
                role = selectedRole,
                suggestedRole = detection.suggestedRole,
                isRoleConfirmed = confirmed,
                allowedCategories = if (selectedRole is FieldRole.Category || selectedRole is FieldRole.Group) {
                    sampleNonMissing.distinct().take(12)
                } else {
                    emptyList()
                }
            )
            preliminaryFields.add(statField)

            columnPreviews.add(
                CsvColumnPreview(
                    columnIndex = colIdx,
                    headerName = colName,
                    sampleValues = sampleNonMissing.take(4),
                    detection = detection,
                    selectedRole = selectedRole,
                    unit = resolvedUnit,
                    isRoleConfirmed = confirmed,
                    missingCount = missingCount
                )
            )
        }

        // Pass 2: Parse rows preserving missing values as null (never 0)
        val statRows = dataRows.mapIndexed { rowIdx, rawCells ->
            val values = mutableMapOf<String, StatValue?>()
            var periodLabel = ""

            preliminaryFields.forEachIndexed { colIdx, field ->
                val rawCell = rawCells[colIdx].trim()
                if (rawCell.lowercase(Locale.ENGLISH) in missingTokens) {
                    values[field.id] = null
                } else {
                    val parsedValue = parseCellForRole(
                        rawCell = rawCell,
                        role = field.role,
                        rowNumber = rowIdx + 1,
                        fieldName = field.name,
                        parseWarnings = parseWarnings
                    )
                    values[field.id] = parsedValue
                    if (periodLabel.isBlank() &&
                        (field.role is FieldRole.Time || field.role is FieldRole.Category)
                    ) {
                        periodLabel = rawCell
                    }
                }
            }

            StatRow(
                id = UUID.randomUUID().toString(),
                periodOrCategoryLabel = periodLabel,
                values = values
            )
        }

        val candidateDataset = StatDataset(
            name = datasetName.ifBlank { "Imported CSV Dataset" },
            description = datasetDescription.ifBlank {
                "Imported from CSV (${statRows.size} rows, ${preliminaryFields.size} fields)"
            },
            fields = preliminaryFields,
            rows = statRows,
            metadata = DatasetMetadata(sourceLabel = "CSV Import")
        )

        val validationReport = ValidationEngine.validate(candidateDataset)

        return CsvImportPreview(
            rawRowCount = dataRows.size,
            detectedHasHeader = detectedHasHeader,
            effectiveHasHeader = effectiveHasHeader,
            columns = columnPreviews,
            candidateDataset = candidateDataset,
            validationReport = validationReport,
            parseWarnings = parseWarnings
        )
    }

    /**
     * Parses RFC-4180 CSV text into rows of cell strings.
     */
    fun parseCsvRecords(rawText: String): List<List<String>> {
        val cleaned = rawText.removePrefix("\uFEFF")
        if (cleaned.isBlank()) return emptyList()

        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < cleaned.length) {
            val ch = cleaned[i]
            when {
                inQuotes -> {
                    if (ch == '"') {
                        if (i + 1 < cleaned.length && cleaned[i + 1] == '"') {
                            currentCell.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        currentCell.append(ch)
                    }
                }
                ch == '"' -> {
                    inQuotes = true
                }
                ch == ',' -> {
                    currentRow.add(currentCell.toString())
                    currentCell.setLength(0)
                }
                ch == '\r' -> {
                    if (i + 1 < cleaned.length && cleaned[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentCell.toString())
                    currentCell.setLength(0)
                    if (currentRow.any { it.isNotBlank() }) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                ch == '\n' -> {
                    currentRow.add(currentCell.toString())
                    currentCell.setLength(0)
                    if (currentRow.any { it.isNotBlank() }) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                else -> {
                    currentCell.append(ch)
                }
            }
            i++
        }

        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentCell.toString())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }

    fun detectHeaderRow(records: List<List<String>>): Boolean {
        if (records.size < 2) return false
        val firstRow = records.first()
        val dataRows = records.drop(1)

        val firstRowHasNumbers = firstRow.any { cell ->
            val trimmed = cell.trim().replace(",", "")
            trimmed.toDoubleOrNull() != null || TimePeriodParser.parse(trimmed) != null
        }
        if (!firstRowHasNumbers) return true

        val secondRowNumericCount = dataRows.first().count { cell ->
            val trimmed = cell.trim().replace(",", "")
            trimmed.toDoubleOrNull() != null || TimePeriodParser.parse(trimmed) != null
        }
        val firstRowNumericCount = firstRow.count { cell ->
            val trimmed = cell.trim().replace(",", "")
            trimmed.toDoubleOrNull() != null || TimePeriodParser.parse(trimmed) != null
        }
        return firstRowNumericCount < secondRowNumericCount
    }

    private fun parseCellForRole(
        rawCell: String,
        role: FieldRole,
        rowNumber: Int,
        fieldName: String,
        parseWarnings: MutableList<String>
    ): StatValue {
        val numericCleaned = rawCell.replace(",", "").removeSuffix("%").trim()
        return when (role) {
            is FieldRole.Time -> {
                val period = TimePeriodParser.parse(rawCell, role.granularity)
                if (period == null) {
                    parseWarnings.add(
                        "Row $rowNumber · '$fieldName': '$rawCell' could not be parsed as ${role.granularity.displayName}."
                    )
                }
                StatValue.TimePeriodValue(period = period, rawText = rawCell)
            }
            is FieldRole.Measure,
            is FieldRole.PartOfMeasure -> {
                val asLong = numericCleaned.toLongOrNull()
                if (asLong != null) {
                    StatValue.WholeNumber(asLong)
                } else {
                    val asDouble = numericCleaned.toDoubleOrNull()
                    if (asDouble != null && asDouble.isFinite()) {
                        StatValue.DecimalNumber(asDouble)
                    } else {
                        parseWarnings.add(
                            "Row $rowNumber · '$fieldName': non-numeric value '$rawCell' preserved as text for review."
                        )
                        StatValue.TextValue(rawCell)
                    }
                }
            }
            is FieldRole.RateContinuous -> {
                val asDouble = numericCleaned.toDoubleOrNull()
                if (asDouble != null && asDouble.isFinite()) {
                    StatValue.DecimalNumber(asDouble)
                } else {
                    parseWarnings.add(
                        "Row $rowNumber · '$fieldName': non-numeric rate '$rawCell' preserved for review."
                    )
                    StatValue.TextValue(rawCell)
                }
            }
            is FieldRole.EstimateWithLimits -> {
                val asDouble = numericCleaned.toDoubleOrNull()
                if (asDouble != null && asDouble.isFinite()) {
                    StatValue.EstimateValue(estimate = asDouble)
                } else {
                    parseWarnings.add(
                        "Row $rowNumber · '$fieldName': '$rawCell' is not a valid numeric estimate."
                    )
                    StatValue.TextValue(rawCell)
                }
            }
            is FieldRole.DurationWithEvent -> {
                val parts = rawCell.split("|", ";", "/").map { it.trim() }
                val dur = parts.firstOrNull()?.toDoubleOrNull()
                val evt = parts.getOrNull(1)?.lowercase(Locale.ENGLISH)?.let {
                    when (it) {
                        "1", "true", "yes", "event", "y" -> true
                        "0", "false", "no", "censored", "n" -> false
                        else -> null
                    }
                }
                if (dur != null && dur.isFinite()) {
                    StatValue.DurationEventValue(duration = dur, eventOccurred = evt)
                } else {
                    parseWarnings.add(
                        "Row $rowNumber · '$fieldName': '$rawCell' could not be parsed as duration."
                    )
                    StatValue.TextValue(rawCell)
                }
            }
            FieldRole.Category,
            FieldRole.Group,
            FieldRole.Label -> StatValue.TextValue(rawCell)
        }
    }
}
