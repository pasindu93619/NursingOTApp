package com.pasindu.nursingotapp.statgraph.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.engine.ValidationIssue
import com.pasindu.nursingotapp.statgraph.engine.ValidationSeverity
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimeGranularity
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.model.TimePeriodParser
import com.pasindu.nursingotapp.statgraph.model.isMissingValue
import com.pasindu.nursingotapp.statgraph.stats.DerivedCellResult
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import com.pasindu.nursingotapp.statgraph.stats.DerivedValueStatus
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatGraphRowCard(
    rowNumber: Int,
    row: StatRow,
    fields: List<StatField>,
    derivedDefinitions: List<DerivedFieldDefinition>,
    derivedResultsByField: Map<String, List<DerivedCellResult>>,
    rowIssues: List<ValidationIssue>,
    categoryOptionsByField: Map<String, List<String>>,
    onValueChange: (fieldId: String, value: StatValue?) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasErrors = rowIssues.any { it.severity == ValidationSeverity.ERROR }
    val hasWarnings = rowIssues.any { it.severity == ValidationSeverity.WARNING }
    val borderColor = when {
        hasErrors -> StatGraphDesignTokens.Colors.error.copy(alpha = 0.55f)
        hasWarnings -> StatGraphDesignTokens.Colors.warning.copy(alpha = 0.65f)
        else -> StatGraphDesignTokens.Colors.borderSubtle
    }

    val primaryHeader = remember(row, fields) {
        val timeOrCatField = fields.firstOrNull { it.role is FieldRole.Time || it.role is FieldRole.Category }
        val cell = timeOrCatField?.let { row.values[it.id] }
        when (cell) {
            is StatValue.TimePeriodValue -> cell.period?.formatDisplay() ?: cell.rawText.ifBlank { "Row $rowNumber" }
            is StatValue.TextValue -> cell.value.ifBlank { "Row $rowNumber" }
            else -> row.periodOrCategoryLabel.ifBlank { "Row $rowNumber" }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StatGraphDesignTokens.Colors.surface,
        shape = StatGraphDesignTokens.Shapes.extraLarge,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = StatGraphDesignTokens.Colors.softBlue,
                    shape = StatGraphDesignTokens.Shapes.pill
                ) {
                    Text(
                        text = "#$rowNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatGraphDesignTokens.Colors.primary,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = primaryHeader,
                    style = MaterialTheme.typography.titleMedium,
                    color = StatGraphDesignTokens.Colors.ink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDuplicate) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate row $rowNumber",
                        tint = StatGraphDesignTokens.Colors.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete row $rowNumber",
                        tint = StatGraphDesignTokens.Colors.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Dynamic Role-Driven Field Editors
            fields.forEach { field ->
                val cellIssues = rowIssues.filter { it.fieldId == field.id }
                DynamicRoleFieldInput(
                    rowId = row.id,
                    field = field,
                    value = row.values[field.id],
                    cellIssues = cellIssues,
                    categoryOptions = categoryOptionsByField[field.id].orEmpty(),
                    onValueChange = { updated -> onValueChange(field.id, updated) }
                )
            }

            // Computed Derived Values Strip (Read-Only, Non-Destructive)
            if (derivedDefinitions.isNotEmpty()) {
                HorizontalDivider(color = StatGraphDesignTokens.Colors.borderSubtle)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    derivedDefinitions.forEach { def ->
                        val cellResult = derivedResultsByField[def.id]?.firstOrNull { it.rowId == row.id }
                        val chipBg = when (cellResult?.status) {
                            DerivedValueStatus.DIVISION_BY_ZERO -> StatGraphDesignTokens.Colors.softRose
                            DerivedValueStatus.MISSING_INPUT,
                            DerivedValueStatus.INSUFFICIENT_WINDOW -> StatGraphDesignTokens.Colors.softSlate
                            else -> StatGraphDesignTokens.Colors.softPurple
                        }
                        val chipFg = when (cellResult?.status) {
                            DerivedValueStatus.DIVISION_BY_ZERO -> StatGraphDesignTokens.Colors.onSoftRose
                            DerivedValueStatus.MISSING_INPUT,
                            DerivedValueStatus.INSUFFICIENT_WINDOW -> StatGraphDesignTokens.Colors.slate
                            else -> StatGraphDesignTokens.Colors.onSoftPurple
                        }
                        Surface(
                            color = chipBg,
                            shape = StatGraphDesignTokens.Shapes.medium
                        ) {
                            Text(
                                text = "${def.name}: ${cellResult?.formattedValue ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = chipFg,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Row-level Validation Notices
            if (rowIssues.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowIssues.forEach { issue ->
                        val isError = issue.severity == ValidationSeverity.ERROR
                        Surface(
                            color = if (isError) StatGraphDesignTokens.Colors.softRose else StatGraphDesignTokens.Colors.softAmber,
                            shape = StatGraphDesignTokens.Shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = if (isError) StatGraphDesignTokens.Colors.error else StatGraphDesignTokens.Colors.warning,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = issue.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isError) StatGraphDesignTokens.Colors.onSoftRose else StatGraphDesignTokens.Colors.onSoftAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicRoleFieldInput(
    rowId: String,
    field: StatField,
    value: StatValue?,
    cellIssues: List<ValidationIssue>,
    categoryOptions: List<String>,
    onValueChange: (StatValue?) -> Unit
) {
    val hasError = cellIssues.any { it.severity == ValidationSeverity.ERROR }
    val labelText = buildString {
        append(field.name)
        if (field.unit.isNotBlank()) append(" (${field.unit})")
        if (field.isRequired) append(" *")
    }

    when (val role = field.role) {
        is FieldRole.Time -> {
            TimeRoleInput(
                label = labelText,
                granularity = role.granularity,
                value = value as? StatValue.TimePeriodValue,
                isError = hasError,
                onValueChange = onValueChange
            )
        }
        is FieldRole.Measure,
        is FieldRole.PartOfMeasure -> {
            WholeNumberRoleInput(
                cellKey = "${rowId}_${field.id}",
                label = labelText,
                value = value,
                isError = hasError,
                onValueChange = onValueChange
            )
        }
        is FieldRole.RateContinuous -> {
            DecimalRoleInput(
                cellKey = "${rowId}_${field.id}",
                label = labelText,
                value = value,
                isError = hasError,
                onValueChange = onValueChange
            )
        }
        FieldRole.Category,
        FieldRole.Group -> {
            CategoryRoleInput(
                label = labelText,
                value = (value as? StatValue.TextValue)?.value.orEmpty(),
                options = categoryOptions,
                isError = hasError,
                onValueChange = { text ->
                    if (text.isBlank()) onValueChange(null)
                    else onValueChange(StatValue.TextValue(text))
                }
            )
        }
        is FieldRole.EstimateWithLimits -> {
            EstimateRoleInput(
                cellKey = "${rowId}_${field.id}",
                field = field,
                role = role,
                value = value as? StatValue.EstimateValue,
                isError = hasError,
                onValueChange = onValueChange
            )
        }
        is FieldRole.DurationWithEvent -> {
            DurationEventRoleInput(
                cellKey = "${rowId}_${field.id}",
                label = labelText,
                eventLabel = role.eventLabel,
                value = value as? StatValue.DurationEventValue,
                isError = hasError,
                onValueChange = onValueChange
            )
        }
        FieldRole.Label -> {
            val text = (value as? StatValue.TextValue)?.value.orEmpty()
            OutlinedTextField(
                value = text,
                onValueChange = { updated ->
                    onValueChange(if (updated.isBlank()) null else StatValue.TextValue(updated))
                },
                label = { Text(labelText) },
                placeholder = { Text("Aggregate note (no patient IDs) · empty = missing") },
                isError = hasError,
                singleLine = true,
                shape = StatGraphDesignTokens.Shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WholeNumberRoleInput(
    cellKey: String,
    label: String,
    value: StatValue?,
    isError: Boolean,
    onValueChange: (StatValue?) -> Unit
) {
    val externalText = when (value) {
        is StatValue.WholeNumber -> value.value.toString()
        is StatValue.DecimalNumber -> value.value.toString()
        is StatValue.TextValue -> value.value
        else -> ""
    }
    var buffer by remember(cellKey) { mutableStateOf(externalText) }
    LaunchedEffect(externalText) {
        if (externalText.isEmpty() && buffer.isNotEmpty()) {
            buffer = ""
        } else if (buffer.trim().toDoubleOrNull() != externalText.toDoubleOrNull()) {
            buffer = externalText
        }
    }

    OutlinedTextField(
        value = buffer,
        onValueChange = { raw ->
            val trimmed = raw.trim()
            buffer = raw
            if (trimmed.isEmpty()) {
                onValueChange(null)
            } else {
                val parsedLong = trimmed.toLongOrNull()
                if (parsedLong != null) {
                    onValueChange(StatValue.WholeNumber(parsedLong))
                } else {
                    val parsedDouble = trimmed.toDoubleOrNull()
                    if (parsedDouble != null && parsedDouble.isFinite()) {
                        onValueChange(StatValue.DecimalNumber(parsedDouble))
                    } else {
                        onValueChange(StatValue.TextValue(trimmed))
                    }
                }
            }
        },
        label = { Text(label) },
        placeholder = { Text("Missing (leave empty)") },
        supportingText = if (value.isMissingValue()) {
            { Text("Empty = missing (not treated as 0)") }
        } else null,
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = StatGraphDesignTokens.Shapes.medium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DecimalRoleInput(
    cellKey: String,
    label: String,
    value: StatValue?,
    isError: Boolean,
    onValueChange: (StatValue?) -> Unit
) {
    val externalText = when (value) {
        is StatValue.DecimalNumber -> value.value.toString()
        is StatValue.WholeNumber -> value.value.toString()
        is StatValue.TextValue -> value.value
        else -> ""
    }
    var buffer by remember(cellKey) { mutableStateOf(externalText) }
    LaunchedEffect(externalText) {
        if (externalText.isEmpty() && buffer.isNotEmpty()) {
            buffer = ""
        } else if (buffer.trim().toDoubleOrNull() != externalText.toDoubleOrNull()) {
            buffer = externalText
        }
    }

    OutlinedTextField(
        value = buffer,
        onValueChange = { raw ->
            buffer = raw
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                onValueChange(null)
            } else {
                val parsed = trimmed.toDoubleOrNull()
                if (parsed != null && parsed.isFinite()) {
                    onValueChange(StatValue.DecimalNumber(parsed))
                } else {
                    onValueChange(StatValue.TextValue(trimmed))
                }
            }
        },
        label = { Text(label) },
        placeholder = { Text("Missing (leave empty)") },
        supportingText = if (value.isMissingValue()) {
            { Text("Empty = missing (not treated as 0)") }
        } else null,
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = StatGraphDesignTokens.Shapes.medium,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryRoleInput(
    label: String,
    value: String,
    options: List<String>,
    isError: Boolean,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (options.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    val selected = option.equals(value.trim(), ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onValueChange(if (selected) "" else option) },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatGraphDesignTokens.Colors.softBlue,
                            selectedLabelColor = StatGraphDesignTokens.Colors.primary
                        )
                    )
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text("Select chip or type category (empty = missing)") },
            isError = isError,
            singleLine = true,
            shape = StatGraphDesignTokens.Shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EstimateRoleInput(
    cellKey: String,
    field: StatField,
    role: FieldRole.EstimateWithLimits,
    value: StatValue.EstimateValue?,
    isError: Boolean,
    onValueChange: (StatValue?) -> Unit
) {
    var estText by remember(cellKey) { mutableStateOf(value?.estimate?.toString().orEmpty()) }
    var loText by remember(cellKey) { mutableStateOf(value?.lowerLimit?.toString().orEmpty()) }
    var hiText by remember(cellKey) { mutableStateOf(value?.upperLimit?.toString().orEmpty()) }

    fun emitUpdated(est: String, lo: String, hi: String) {
        val e = est.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        val l = lo.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        val h = hi.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        if (e == null && l == null && h == null && est.isBlank() && lo.isBlank() && hi.isBlank()) {
            onValueChange(null)
        } else {
            onValueChange(StatValue.EstimateValue(estimate = e, lowerLimit = l, upperLimit = h))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (field.unit.isNotBlank()) "${field.name} (${field.unit})" else field.name,
            style = MaterialTheme.typography.labelLarge,
            color = StatGraphDesignTokens.Colors.ink,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = estText,
                onValueChange = {
                    estText = it
                    emitUpdated(it, loText, hiText)
                },
                label = { Text("Estimate") },
                placeholder = { Text("—") },
                isError = isError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = StatGraphDesignTokens.Shapes.medium,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = loText,
                onValueChange = {
                    loText = it
                    emitUpdated(estText, it, hiText)
                },
                label = { Text(role.lowerLabel) },
                placeholder = { Text("—") },
                isError = isError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = StatGraphDesignTokens.Shapes.medium,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = hiText,
                onValueChange = {
                    hiText = it
                    emitUpdated(estText, loText, it)
                },
                label = { Text(role.upperLabel) },
                placeholder = { Text("—") },
                isError = isError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = StatGraphDesignTokens.Shapes.medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DurationEventRoleInput(
    cellKey: String,
    label: String,
    eventLabel: String,
    value: StatValue.DurationEventValue?,
    isError: Boolean,
    onValueChange: (StatValue?) -> Unit
) {
    var durText by remember(cellKey) { mutableStateOf(value?.duration?.toString().orEmpty()) }
    val eventFlag = value?.eventOccurred ?: false

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = durText,
            onValueChange = { raw ->
                durText = raw
                val parsed = raw.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
                if (raw.isBlank() && value?.eventOccurred == null) {
                    onValueChange(null)
                } else {
                    onValueChange(StatValue.DurationEventValue(duration = parsed, eventOccurred = eventFlag))
                }
            },
            label = { Text(label) },
            placeholder = { Text("Missing") },
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = StatGraphDesignTokens.Shapes.medium,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = if (eventFlag) StatGraphDesignTokens.Colors.softMint else StatGraphDesignTokens.Colors.softSlate,
            shape = StatGraphDesignTokens.Shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (eventFlag) eventLabel else "Censored",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatGraphDesignTokens.Colors.ink,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = eventFlag,
                    onCheckedChange = { checked ->
                        val parsed = durText.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
                        onValueChange(StatValue.DurationEventValue(duration = parsed, eventOccurred = checked))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRoleInput(
    label: String,
    granularity: TimeGranularity,
    value: StatValue.TimePeriodValue?,
    isError: Boolean,
    onValueChange: (StatValue?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayValue = value?.rawText ?: ""

    OutlinedTextField(
        value = displayValue,
        onValueChange = { raw ->
            if (raw.isBlank()) {
                onValueChange(null)
            } else {
                val parsed = TimePeriodParser.parse(raw, granularity)
                onValueChange(StatValue.TimePeriodValue(period = parsed, rawText = raw))
            }
        },
        label = { Text("$label (${granularity.formatHint})") },
        placeholder = { Text("e.g., ${granularity.formatHint} · empty = missing") },
        isError = isError,
        singleLine = true,
        keyboardOptions = if (granularity == TimeGranularity.YEAR) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick ${granularity.displayName}",
                    tint = StatGraphDesignTokens.Colors.primary
                )
            }
        },
        shape = StatGraphDesignTokens.Shapes.medium,
        modifier = Modifier.fillMaxWidth()
    )

    if (showPicker) {
        when (granularity) {
            TimeGranularity.DAY -> {
                val initialMillis = (value?.period as? TimePeriod.Day)?.date
                    ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                DatePickerDialog(
                    onDismissRequest = { showPicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val pickedDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                    val period = TimePeriod.Day(pickedDate)
                                    onValueChange(StatValue.TimePeriodValue(period, period.formatIso()))
                                }
                                showPicker = false
                            }
                        ) { Text("Select") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            TimeGranularity.MONTH -> {
                MonthPeriodPickerDialog(
                    initial = (value?.period as? TimePeriod.Month)?.yearMonth ?: YearMonth.now(),
                    onConfirm = { ym ->
                        val period = TimePeriod.Month(ym)
                        onValueChange(StatValue.TimePeriodValue(period, period.formatIso()))
                        showPicker = false
                    },
                    onDismiss = { showPicker = false }
                )
            }
            TimeGranularity.WEEK -> {
                val today = LocalDate.now()
                val initialWeek = (value?.period as? TimePeriod.Week) ?: TimePeriod.Week(
                    today.get(IsoFields.WEEK_BASED_YEAR),
                    today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                )
                WeekPeriodPickerDialog(
                    initial = initialWeek,
                    onConfirm = { week ->
                        onValueChange(StatValue.TimePeriodValue(week, week.formatIso()))
                        showPicker = false
                    },
                    onDismiss = { showPicker = false }
                )
            }
            TimeGranularity.YEAR -> {
                val initialYear = (value?.period as? TimePeriod.Year)?.year ?: LocalDate.now().year
                YearPeriodPickerDialog(
                    initialYear = initialYear,
                    onConfirm = { year ->
                        val period = TimePeriod.Year(year)
                        onValueChange(StatValue.TimePeriodValue(period, period.formatIso()))
                        showPicker = false
                    },
                    onDismiss = { showPicker = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthPeriodPickerDialog(
    initial: YearMonth,
    onConfirm: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.monthValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                YearStepperRow(year = year, onYearChange = { year = it })
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Month.entries.forEach { m ->
                        FilterChip(
                            selected = month == m.value,
                            onClick = { month = m.value },
                            label = { Text(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(YearMonth.of(year, month)) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun WeekPeriodPickerDialog(
    initial: TimePeriod.Week,
    onConfirm: (TimePeriod.Week) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableIntStateOf(initial.year) }
    var week by remember { mutableIntStateOf(initial.weekOfYear) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Epidemiological Week") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                YearStepperRow(year = year, onYearChange = { year = it })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { week = if (week <= 1) 52 else week - 1 }) {
                        Icon(Icons.Default.Remove, contentDescription = "Previous week")
                    }
                    Text(
                        text = "Week $week",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { week = if (week >= 53) 1 else week + 1 }) {
                        Icon(Icons.Default.Add, contentDescription = "Next week")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(TimePeriod.Week(year, week)) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun YearPeriodPickerDialog(
    initialYear: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableIntStateOf(initialYear) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Year") },
        text = { YearStepperRow(year = year, onYearChange = { year = it }) },
        confirmButton = {
            TextButton(onClick = { onConfirm(year) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun YearStepperRow(year: Int, onYearChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onYearChange((year - 1).coerceAtLeast(1900)) }) {
            Icon(Icons.Default.Remove, contentDescription = "Previous year")
        }
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        IconButton(onClick = { onYearChange((year + 1).coerceAtMost(2100)) }) {
            Icon(Icons.Default.Add, contentDescription = "Next year")
        }
    }
}
