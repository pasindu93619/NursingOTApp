package com.pasindu.nursingotapp.statgraph.engine

import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.model.TimePeriodParser
import com.pasindu.nursingotapp.statgraph.model.asDoubleOrNull
import com.pasindu.nursingotapp.statgraph.model.isMissingValue
import java.util.Locale

enum class ValidationSeverity {
    ERROR,
    WARNING
}

data class ValidationIssue(
    val id: String,
    val severity: ValidationSeverity,
    val ruleCode: String,
    val message: String,
    val rowId: String? = null,
    val rowIndex: Int? = null,
    val fieldId: String? = null,
    val fieldName: String? = null
)

data class ValidationReport(
    val issues: List<ValidationIssue> = emptyList()
) {
    val errors: List<ValidationIssue>
        get() = issues.filter { it.severity == ValidationSeverity.ERROR }

    val warnings: List<ValidationIssue>
        get() = issues.filter { it.severity == ValidationSeverity.WARNING }

    val errorCount: Int get() = errors.size
    val warningCount: Int get() = warnings.size
    val hasBlockingErrors: Boolean get() = errors.isNotEmpty()
    val isClean: Boolean get() = issues.isEmpty()

    fun issuesForRow(rowId: String): List<ValidationIssue> =
        issues.filter { it.rowId == rowId }

    fun issuesForCell(rowId: String, fieldId: String): List<ValidationIssue> =
        issues.filter { it.rowId == rowId && it.fieldId == fieldId }
}

/**
 * Stateless validation engine for [StatDataset].
 *
 * Enforces clinical/statistical integrity rules while preserving missing data
 * as missing (never coercing empty cells to zero) and separating blocking
 * [ValidationSeverity.ERROR] issues from advisory [ValidationSeverity.WARNING]s.
 */
object ValidationEngine {

    private const val SMALL_COUNT_THRESHOLD = 5.0

    private val piiFieldNameTokens = listOf(
        "patient name", "full name", "first name", "last name", "surname",
        "nic", "national id", "passport", "bht", "hospital no", "hospital number",
        "mrn", "reg no", "registration number", "phone", "mobile", "address", "dob"
    )
    private val slNicOldRegex = Regex("""\b\d{9}[VvXx]\b""")
    private val slNicNewRegex = Regex("""\b(19|20)\d{10}\b""")
    private val phoneRegex = Regex("""\b07\d{8}\b""")

    fun validate(dataset: StatDataset): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        val fieldById = dataset.fields.associateBy { it.id }

        // Field-level checks (privacy field names & min/max configuration consistency)
        dataset.fields.forEach { field ->
            validateFieldDefinition(field, issues)
        }

        // Row-by-row cell checks
        dataset.rows.forEachIndexed { zeroBasedIndex, row ->
            val rowNumber = zeroBasedIndex + 1
            validateRow(
                row = row,
                rowNumber = rowNumber,
                fields = dataset.fields,
                fieldById = fieldById,
                issues = issues
            )
        }

        // Cross-row temporal checks (duplicates and period gaps)
        dataset.fields
            .filter { it.role is FieldRole.Time }
            .forEach { timeField ->
                validateTimeSeriesField(timeField, dataset.rows, issues)
            }

        // Cross-field paired Lower Limit / Upper Limit estimate checks (for split CSV columns)
        validatePairedEstimateColumns(dataset.fields, dataset.rows, issues)

        return ValidationReport(issues = issues)
    }

    private fun validateFieldDefinition(
        field: StatField,
        issues: MutableList<ValidationIssue>
    ) {
        val lowerName = field.name.trim().lowercase(Locale.ENGLISH)
        if (piiFieldNameTokens.any { lowerName.contains(it) }) {
            issues.add(
                ValidationIssue(
                    id = "pii_field_${field.id}",
                    severity = ValidationSeverity.WARNING,
                    ruleCode = "PRIVACY_PII_FIELD",
                    message = "Column '${field.name}' looks like it may contain personal patient details. Please use ward totals or anonymous group names only.",
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
        }

        if (field.minValue != null && field.maxValue != null && field.minValue > field.maxValue) {
            issues.add(
                ValidationIssue(
                    id = "field_bounds_${field.id}",
                    severity = ValidationSeverity.ERROR,
                    ruleCode = "INVALID_FIELD_BOUNDS",
                    message = "In column '${field.name}', the minimum allowed value (${formatNum(field.minValue)}) is higher than the maximum (${formatNum(field.maxValue)}). Please swap or fix these limits.",
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
        }

        if (field.role is FieldRole.PartOfMeasure && field.role.parentFieldId == null) {
            issues.add(
                ValidationIssue(
                    id = "part_unlinked_${field.id}",
                    severity = ValidationSeverity.WARNING,
                    ruleCode = "PART_WITHOUT_TOTAL",
                    message = "Column '${field.name}' is marked as part of a total, but isn't linked to a total column yet. Open the column settings to pick its total column.",
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
        }
    }

    private fun validateRow(
        row: StatRow,
        rowNumber: Int,
        fields: List<StatField>,
        fieldById: Map<String, StatField>,
        issues: MutableList<ValidationIssue>
    ) {
        val rowContext = rowContextLabel(row, fields, rowNumber)
        fields.forEach { field ->
            val value = row.values[field.id]

            // Missing check: missing values are NEVER treated as zero.
            if (value.isMissingValue()) {
                if (field.isRequired) {
                    issues.add(
                        ValidationIssue(
                            id = "req_${row.id}_${field.id}",
                            severity = ValidationSeverity.ERROR,
                            ruleCode = "REQUIRED_MISSING",
                            message = "'${field.name}' is empty $rowContext. Please enter a value.",
                            rowId = row.id,
                            rowIndex = rowNumber,
                            fieldId = field.id,
                            fieldName = field.name
                        )
                    )
                }
                return@forEach
            }

            // PII content check on text/label/category values
            if (value is StatValue.TextValue) {
                val text = value.value.trim()
                if (slNicOldRegex.containsMatchIn(text) || slNicNewRegex.containsMatchIn(text) || phoneRegex.containsMatchIn(text)) {
                    issues.add(
                        ValidationIssue(
                            id = "pii_cell_${row.id}_${field.id}",
                            severity = ValidationSeverity.WARNING,
                            ruleCode = "PRIVACY_PII_VALUE",
                            message = "'${field.name}' $rowContext looks like an ID or phone number. Please remove personal details before sharing.",
                            rowId = row.id,
                            rowIndex = rowNumber,
                            fieldId = field.id,
                            fieldName = field.name
                        )
                    )
                }
            }

            // Role-specific checks
            when (val role = field.role) {
                is FieldRole.Measure -> {
                    validateNumericCountCell(value, field, row, rowNumber, rowContext, issues)
                }
                is FieldRole.PartOfMeasure -> {
                    validateNumericCountCell(value, field, row, rowNumber, rowContext, issues)
                    validatePartAgainstParent(value, role, field, row, rowNumber, rowContext, fieldById, issues)
                }
                is FieldRole.RateContinuous -> {
                    val numeric = value.asDoubleOrNull()
                    if (numeric == null) {
                        issues.add(
                            ValidationIssue(
                                id = "bad_num_${row.id}_${field.id}",
                                severity = ValidationSeverity.ERROR,
                                ruleCode = "INVALID_NUMERIC_VALUE",
                                message = "'${field.name}' $rowContext has '${(value as? StatValue.TextValue)?.value ?: value}', which is not a number. Please enter digits only.",
                                rowId = row.id,
                                rowIndex = rowNumber,
                                fieldId = field.id,
                                fieldName = field.name
                            )
                        )
                    } else {
                        if (!role.allowsNegative && numeric < 0.0) {
                            issues.add(
                                ValidationIssue(
                                    id = "neg_${row.id}_${field.id}",
                                    severity = ValidationSeverity.ERROR,
                                    ruleCode = "NEGATIVE_VALUE",
                                    message = "'${field.name}' (${formatNum(numeric)}) $rowContext cannot be negative. Please check this number.",
                                    rowId = row.id,
                                    rowIndex = rowNumber,
                                    fieldId = field.id,
                                    fieldName = field.name
                                )
                            )
                        }
                        validateMinMax(numeric, field, row, rowNumber, rowContext, issues)
                    }
                }
                is FieldRole.EstimateWithLimits -> {
                    validateEstimateCell(value, field, row, rowNumber, rowContext, issues)
                }
                is FieldRole.DurationWithEvent -> {
                    if (value is StatValue.DurationEventValue) {
                        if (value.duration != null) {
                            if (value.duration < 0.0) {
                                issues.add(
                                    ValidationIssue(
                                        id = "neg_dur_${row.id}_${field.id}",
                                        severity = ValidationSeverity.ERROR,
                                        ruleCode = "NEGATIVE_DURATION",
                                        message = "'${field.name}' (${formatNum(value.duration)}) $rowContext cannot be negative. Please enter 0 or more.",
                                        rowId = row.id,
                                        rowIndex = rowNumber,
                                        fieldId = field.id,
                                        fieldName = field.name
                                    )
                                )
                            }
                            validateMinMax(value.duration, field, row, rowNumber, rowContext, issues)
                        }
                    } else {
                        issues.add(
                            ValidationIssue(
                                id = "bad_dur_${row.id}_${field.id}",
                                severity = ValidationSeverity.ERROR,
                                ruleCode = "INVALID_NUMERIC_VALUE",
                                message = "'${field.name}' $rowContext needs a number for time until event.",
                                rowId = row.id,
                                rowIndex = rowNumber,
                                fieldId = field.id,
                                fieldName = field.name
                            )
                        )
                    }
                }
                is FieldRole.Time -> {
                    if (value is StatValue.TimePeriodValue && value.period == null && value.rawText.isNotBlank()) {
                        issues.add(
                            ValidationIssue(
                                id = "bad_time_${row.id}_${field.id}",
                                severity = ValidationSeverity.ERROR,
                                ruleCode = "INVALID_TIME_FORMAT",
                                message = "'${field.name}' $rowContext has '${value.rawText}', which doesn't look like a ${role.granularity.displayName.lowercase()} (expected ${role.granularity.formatHint}).",
                                rowId = row.id,
                                rowIndex = rowNumber,
                                fieldId = field.id,
                                fieldName = field.name
                            )
                        )
                    }
                }
                FieldRole.Category,
                FieldRole.Group,
                FieldRole.Label -> Unit
            }
        }
    }

    private fun validateNumericCountCell(
        value: StatValue?,
        field: StatField,
        row: StatRow,
        rowNumber: Int,
        rowContext: String,
        issues: MutableList<ValidationIssue>
    ) {
        val numeric = value.asDoubleOrNull()
        if (numeric == null) {
            issues.add(
                ValidationIssue(
                    id = "bad_count_${row.id}_${field.id}",
                    severity = ValidationSeverity.ERROR,
                    ruleCode = "INVALID_NUMERIC_VALUE",
                    message = "'${field.name}' $rowContext has '${(value as? StatValue.TextValue)?.value ?: value}', which is not a number. Please enter digits only.",
                    rowId = row.id,
                    rowIndex = rowNumber,
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
            return
        }

        if (numeric < 0.0) {
            issues.add(
                ValidationIssue(
                    id = "neg_count_${row.id}_${field.id}",
                    severity = ValidationSeverity.ERROR,
                    ruleCode = "NEGATIVE_COUNT",
                    message = "'${field.name}' (${formatNum(numeric)}) $rowContext cannot be negative. Please enter 0 or more.",
                    rowId = row.id,
                    rowIndex = rowNumber,
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
        }

        if (field.role.requiresWholeNumber && numeric % 1.0 != 0.0) {
            issues.add(
                ValidationIssue(
                    id = "frac_count_${row.id}_${field.id}",
                    severity = ValidationSeverity.ERROR,
                    ruleCode = "WHOLE_NUMBER_REQUIRED",
                    message = "'${field.name}' (${formatNum(numeric)}) $rowContext should be a whole number (no decimals).",
                    rowId = row.id,
                    rowIndex = rowNumber,
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
        }

        validateMinMax(numeric, field, row, rowNumber, rowContext, issues)

        if (numeric > 0.0 && numeric < SMALL_COUNT_THRESHOLD) {
            issues.add(
                ValidationIssue(
                    id = "small_count_${row.id}_${field.id}",
                    severity = ValidationSeverity.WARNING,
                    ruleCode = "PRIVACY_SMALL_COUNT",
                    message = "'${field.name}' (${formatNum(numeric)}) $rowContext is a very small count (under 5). Consider grouping rows together before sharing so patients can't be identified.",
                    rowId = row.id,
                    rowIndex = rowNumber,
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
        }
    }

    private fun validatePartAgainstParent(
        value: StatValue?,
        role: FieldRole.PartOfMeasure,
        field: StatField,
        row: StatRow,
        rowNumber: Int,
        rowContext: String,
        fieldById: Map<String, StatField>,
        issues: MutableList<ValidationIssue>
    ) {
        val parentField = role.parentFieldId?.let { fieldById[it] } ?: return
        val partNumeric = value.asDoubleOrNull() ?: return
        val totalNumeric = row.values[parentField.id].asDoubleOrNull() ?: return

        if (partNumeric > totalNumeric) {
            issues.add(
                ValidationIssue(
                    id = "part_gt_total_${row.id}_${field.id}",
                    severity = ValidationSeverity.ERROR,
                    ruleCode = "PART_EXCEEDS_TOTAL",
                    message = "'${field.name}' (${formatNum(partNumeric)}) is more than '${parentField.name}' (${formatNum(totalNumeric)}) $rowContext. Please check both numbers.",
                    rowId = row.id,
                    rowIndex = rowNumber,
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
        }
    }

    private fun validateEstimateCell(
        value: StatValue?,
        field: StatField,
        row: StatRow,
        rowNumber: Int,
        rowContext: String,
        issues: MutableList<ValidationIssue>
    ) {
        val est = value as? StatValue.EstimateValue ?: run {
            val fallbackNum = value.asDoubleOrNull()
            if (fallbackNum != null) {
                validateMinMax(fallbackNum, field, row, rowNumber, rowContext, issues)
            } else {
                issues.add(
                    ValidationIssue(
                        id = "bad_est_${row.id}_${field.id}",
                        severity = ValidationSeverity.ERROR,
                        ruleCode = "INVALID_NUMERIC_VALUE",
                        message = "'${field.name}' $rowContext needs a number.",
                        rowId = row.id,
                        rowIndex = rowNumber,
                        fieldId = field.id,
                        fieldName = field.name
                    )
                )
            }
            return
        }

        val lower = est.lowerLimit
        val upper = est.upperLimit
        val point = est.estimate

        if (lower != null && upper != null && lower > upper) {
            issues.add(
                ValidationIssue(
                    id = "est_bounds_${row.id}_${field.id}",
                    severity = ValidationSeverity.ERROR,
                    ruleCode = "LOWER_EXCEEDS_UPPER",
                    message = "'${field.name}' $rowContext has a lower limit (${formatNum(lower)}) higher than its upper limit (${formatNum(upper)}). Please check both numbers.",
                    rowId = row.id,
                    rowIndex = rowNumber,
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
        }

        if (point != null) {
            if ((lower != null && point < lower) || (upper != null && point > upper)) {
                issues.add(
                    ValidationIssue(
                        id = "est_point_${row.id}_${field.id}",
                        severity = ValidationSeverity.WARNING,
                        ruleCode = "ESTIMATE_OUTSIDE_LIMITS",
                        message = "'${field.name}' (${formatNum(point)}) $rowContext falls outside its range [${lower?.let(::formatNum) ?: "—"} to ${upper?.let(::formatNum) ?: "—"}]. Please check these numbers.",
                        rowId = row.id,
                        rowIndex = rowNumber,
                        fieldId = field.id,
                        fieldName = field.name
                    )
                )
            }
            validateMinMax(point, field, row, rowNumber, rowContext, issues)
        }
    }

    private fun validateMinMax(
        numeric: Double,
        field: StatField,
        row: StatRow,
        rowNumber: Int,
        rowContext: String,
        issues: MutableList<ValidationIssue>
    ) {
        if (field.minValue != null && numeric < field.minValue) {
            issues.add(
                ValidationIssue(
                    id = "min_${row.id}_${field.id}",
                    severity = ValidationSeverity.ERROR,
                    ruleCode = "BELOW_MIN",
                    message = "'${field.name}' (${formatNum(numeric)}) $rowContext is below the lowest allowed value (${formatNum(field.minValue)}).",
                    rowId = row.id,
                    rowIndex = rowNumber,
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
        }
        if (field.maxValue != null && numeric > field.maxValue) {
            issues.add(
                ValidationIssue(
                    id = "max_${row.id}_${field.id}",
                    severity = ValidationSeverity.ERROR,
                    ruleCode = "ABOVE_MAX",
                    message = "'${field.name}' (${formatNum(numeric)}) $rowContext is above the highest allowed value (${formatNum(field.maxValue)}).",
                    rowId = row.id,
                    rowIndex = rowNumber,
                    fieldId = field.id,
                    fieldName = field.name
                )
            )
        }
    }

    private fun validateTimeSeriesField(
        timeField: StatField,
        rows: List<StatRow>,
        issues: MutableList<ValidationIssue>
    ) {
        val seenKeys = mutableMapOf<Long, MutableList<Pair<Int, StatRow>>>()
        val periods = mutableListOf<TimePeriod>()

        rows.forEachIndexed { idx, row ->
            val rowNumber = idx + 1
            val cell = row.values[timeField.id]
            if (cell.isMissingValue()) {
                if (!timeField.isRequired) {
                    issues.add(
                        ValidationIssue(
                            id = "missing_time_row_${row.id}_${timeField.id}",
                            severity = ValidationSeverity.WARNING,
                            ruleCode = "MISSING_TIME_PERIOD",
                            message = "'${timeField.name}' is missing a date or month in row $rowNumber.",
                            rowId = row.id,
                            rowIndex = rowNumber,
                            fieldId = timeField.id,
                            fieldName = timeField.name
                        )
                    )
                }
                return@forEachIndexed
            }

            val period = (cell as? StatValue.TimePeriodValue)?.period ?: return@forEachIndexed
            periods.add(period)
            seenKeys.getOrPut(period.sortKey()) { mutableListOf() }.add(rowNumber to row)
        }

        // Duplicate period warnings
        seenKeys.values.filter { it.size > 1 }.forEach { occurrences ->
            val rowsLabel = occurrences.joinToString(", ") { "Row ${it.first}" }
            val periodLabel = (occurrences.first().second.values[timeField.id] as? StatValue.TimePeriodValue)
                ?.period?.formatDisplay() ?: "period"
            occurrences.forEach { (rowNumber, row) ->
                issues.add(
                    ValidationIssue(
                        id = "dup_time_${row.id}_${timeField.id}",
                        severity = ValidationSeverity.WARNING,
                        ruleCode = "DUPLICATE_TIME_PERIOD",
                        message = "'$periodLabel' appears more than once in '${timeField.name}' ($rowsLabel). That's fine if each row is for a different ward or group — otherwise please combine or remove duplicates.",
                        rowId = row.id,
                        rowIndex = rowNumber,
                        fieldId = timeField.id,
                        fieldName = timeField.name
                    )
                )
            }
        }

        // Missing gap period warnings
        val sortedDistinct = periods.distinctBy { it.sortKey() }.sortedBy { it.sortKey() }
        if (sortedDistinct.size >= 2) {
            val missingGaps = mutableListOf<TimePeriod>()
            for (i in 0 until sortedDistinct.lastIndex) {
                missingGaps.addAll(
                    TimePeriodParser.enumerateBetween(sortedDistinct[i], sortedDistinct[i + 1], maxSteps = 12)
                )
            }
            if (missingGaps.isNotEmpty()) {
                val previewGaps = missingGaps.take(4).joinToString(", ") { it.formatDisplay() }
                val suffix = if (missingGaps.size > 4) " (+${missingGaps.size - 4} more)" else ""
                issues.add(
                    ValidationIssue(
                        id = "gap_time_${timeField.id}",
                        severity = ValidationSeverity.WARNING,
                        ruleCode = "MISSING_PERIOD_GAP",
                        message = "There is a gap in '${timeField.name}' — missing $previewGaps$suffix. Add those dates if you have data for them.",
                        fieldId = timeField.id,
                        fieldName = timeField.name
                    )
                )
            }
        }
    }

    private fun validatePairedEstimateColumns(
        fields: List<StatField>,
        rows: List<StatRow>,
        issues: MutableList<ValidationIssue>
    ) {
        val lowerField = fields.firstOrNull { f ->
            val n = f.name.lowercase(Locale.ENGLISH)
            f.role.isNumeric && (n.contains("lower") || n.contains("lcl"))
        } ?: return
        val upperField = fields.firstOrNull { f ->
            val n = f.name.lowercase(Locale.ENGLISH)
            f.role.isNumeric && f.id != lowerField.id && (n.contains("upper") || n.contains("ucl"))
        } ?: return

        rows.forEachIndexed { idx, row ->
            val rowNumber = idx + 1
            val lo = row.values[lowerField.id].asDoubleOrNull() ?: return@forEachIndexed
            val hi = row.values[upperField.id].asDoubleOrNull() ?: return@forEachIndexed
            if (lo > hi) {
                val rowContext = rowContextLabel(row, fields, rowNumber)
                issues.add(
                    ValidationIssue(
                        id = "paired_est_${row.id}_${lowerField.id}",
                        severity = ValidationSeverity.ERROR,
                        ruleCode = "LOWER_EXCEEDS_UPPER",
                        message = "'${lowerField.name}' (${formatNum(lo)}) is higher than '${upperField.name}' (${formatNum(hi)}) $rowContext. Please check both numbers.",
                        rowId = row.id,
                        rowIndex = rowNumber,
                        fieldId = lowerField.id,
                        fieldName = lowerField.name
                    )
                )
            }
        }
    }

    private fun rowContextLabel(
        row: StatRow,
        fields: List<StatField>,
        rowNumber: Int
    ): String {
        val labelValue = fields
            .firstOrNull {
                it.role is FieldRole.Time ||
                    it.role is FieldRole.Category ||
                    it.role is FieldRole.Group ||
                    it.role is FieldRole.Label
            }
            ?.let { field ->
                when (val v = row.values[field.id]) {
                    is StatValue.TimePeriodValue -> v.period?.formatDisplay() ?: v.rawText.trim()
                    is StatValue.TextValue -> v.value.trim()
                    else -> null
                }
            }
            ?.takeIf { it.isNotBlank() }
        return if (labelValue != null) "in $labelValue (row $rowNumber)" else "in row $rowNumber"
    }

    private fun formatNum(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(Locale.US, "%.2f", value)
}
