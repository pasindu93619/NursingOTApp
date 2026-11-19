package com.pasindu.nursingotapp.statgraph.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.R
import com.pasindu.nursingotapp.statgraph.engine.RoleAutoDetector
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.stats.DerivedCalculationType
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatGraphFieldEditorDialog(
    initialField: StatField?,
    existingFields: List<StatField>,
    onSave: (StatField) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = initialField != null
    var name by remember { mutableStateOf(initialField?.name.orEmpty()) }
    var unit by remember { mutableStateOf(initialField?.unit.orEmpty()) }
    var minText by remember { mutableStateOf(initialField?.minValue?.toString().orEmpty()) }
    var maxText by remember { mutableStateOf(initialField?.maxValue?.toString().orEmpty()) }
    var isRequired by remember { mutableStateOf(initialField?.isRequired ?: false) }
    var categoriesText by remember {
        mutableStateOf(initialField?.allowedCategories?.joinToString(", ").orEmpty())
    }

    val defaultParentId = existingFields
        .firstOrNull { it.role is FieldRole.Measure && it.id != initialField?.id }
        ?.id

    var userOverrodeRole by remember { mutableStateOf(initialField?.isRoleConfirmed == true) }
    var notSureSelected by remember {
        mutableStateOf(initialField?.isRoleConfirmed == false && initialField.suggestedRole != null)
    }
    var selectedRole by remember {
        mutableStateOf(initialField?.role ?: FieldRole.Measure)
    }

    val autoSuggestion = remember(name, unit, existingFields) {
        if (name.isBlank()) null
        else RoleAutoDetector.suggestRole(name, unit, emptyList(), existingFields)
    }

    val effectiveRole = if ((notSureSelected || !userOverrodeRole) && autoSuggestion != null) {
        autoSuggestion.suggestedRole
    } else if (notSureSelected) {
        initialField?.suggestedRole ?: selectedRole
    } else {
        selectedRole
    }

    var researchExpanded by remember {
        mutableStateOf(
            effectiveRole is FieldRole.EstimateWithLimits ||
                effectiveRole is FieldRole.DurationWithEvent
        )
    }

    val selectableRoles = remember(defaultParentId) {
        FieldRole.selectablePresets(defaultParentFieldId = defaultParentId)
    }

    val measureParents = remember(existingFields, initialField) {
        existingFields.filter { it.role is FieldRole.Measure && it.id != initialField?.id }
    }
    var selectedParentId by remember {
        mutableStateOf((effectiveRole as? FieldRole.PartOfMeasure)?.parentFieldId ?: defaultParentId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Field" else "Add Statistical Field",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Field Name *") },
                    placeholder = { Text("e.g., Month, Dengue Patients, Ward") },
                    singleLine = true,
                    shape = StatGraphDesignTokens.Shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                if (autoSuggestion != null) {
                    Surface(
                        color = StatGraphDesignTokens.Colors.softPurple,
                        shape = StatGraphDesignTokens.Shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StatGraphDesignTokens.Colors.purpleAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Suggested: ${autoSuggestion.suggestedRole.displayName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatGraphDesignTokens.Colors.onSoftPurple,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = autoSuggestion.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatGraphDesignTokens.Colors.slate
                                )
                            }
                            if (userOverrodeRole) {
                                TextButton(onClick = {
                                    userOverrodeRole = false
                                    selectedRole = autoSuggestion.suggestedRole
                                }) {
                                    Text("Use")
                                }
                            }
                        }
                    }
                }

                var helpTopic by remember { mutableStateOf<ContextualHelpTopic?>(null) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.statgraph_role_dialog_heading),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    ContextualHelpButton(
                        onClick = {
                            helpTopic = when (effectiveRole) {
                                is FieldRole.Time -> ContextualHelpTopic.DATE_TIME_COLUMNS
                                is FieldRole.Category,
                                is FieldRole.Group -> ContextualHelpTopic.GROUP_CATEGORY_FIELDS
                                is FieldRole.RateContinuous -> ContextualHelpTopic.PERCENTAGE_VS_COUNT
                                else -> ContextualHelpTopic.COUNT_VS_PART
                            }
                        }
                    )
                }
                helpTopic?.let { topic ->
                    StatGraphContextualHelpDialog(
                        topic = topic,
                        dataset = null,
                        onDismiss = { helpTopic = null }
                    )
                }

                val primaryRoles = remember(defaultParentId, effectiveRole) {
                    val activeTime = (effectiveRole as? FieldRole.Time) ?: FieldRole.Time.Month
                    listOf(
                        activeTime,
                        FieldRole.Measure,
                        FieldRole.PartOfMeasure(parentFieldId = defaultParentId),
                        FieldRole.RateContinuous(),
                        FieldRole.Category,
                        FieldRole.Group,
                        FieldRole.Label
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    primaryRoles.forEach { preset ->
                        val isSelected = !notSureSelected && isSameRoleKind(effectiveRole, preset)
                        RoleOptionCard(
                            title = preset.plainLabel,
                            hint = preset.plainHint,
                            example = preset.example,
                            selected = isSelected,
                            onClick = {
                                notSureSelected = false
                                userOverrodeRole = true
                                selectedRole = if (preset is FieldRole.PartOfMeasure) {
                                    FieldRole.PartOfMeasure(parentFieldId = selectedParentId)
                                } else {
                                    preset
                                }
                            }
                        )
                        if (isSelected && preset is FieldRole.Time) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                listOf(
                                    FieldRole.Time.Day,
                                    FieldRole.Time.Week,
                                    FieldRole.Time.Month,
                                    FieldRole.Time.Year
                                ).forEach { timePreset ->
                                    FilterChip(
                                        selected = effectiveRole == timePreset,
                                        onClick = {
                                            notSureSelected = false
                                            userOverrodeRole = true
                                            selectedRole = timePreset
                                        },
                                        label = { Text(timePreset.shortLabel) }
                                    )
                                }
                            }
                        }
                    }

                    RoleOptionCard(
                        title = stringResource(R.string.statgraph_role_not_sure_title),
                        hint = if (autoSuggestion != null) {
                            "Suggested: ${autoSuggestion.suggestedRole.plainLabel}"
                        } else {
                            stringResource(R.string.statgraph_role_not_sure_hint)
                        },
                        example = autoSuggestion?.suggestedRole?.example.orEmpty(),
                        selected = notSureSelected,
                        onClick = {
                            notSureSelected = true
                            userOverrodeRole = false
                        }
                    )
                }

                Surface(
                    onClick = { researchExpanded = !researchExpanded },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.statgraph_role_research_options),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (researchExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = researchExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectableRoles
                            .filter { it is FieldRole.EstimateWithLimits || it is FieldRole.DurationWithEvent }
                            .forEach { preset ->
                                val isSelected = !notSureSelected && isSameRoleKind(effectiveRole, preset)
                                RoleOptionCard(
                                    title = preset.plainLabel,
                                    hint = preset.plainHint,
                                    example = preset.example,
                                    selected = isSelected,
                                    onClick = {
                                        notSureSelected = false
                                        userOverrodeRole = true
                                        selectedRole = preset
                                    }
                                )
                            }
                    }
                }

                if (effectiveRole is FieldRole.PartOfMeasure && measureParents.isNotEmpty()) {
                    Text(
                        text = "Associated Total Measure",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        measureParents.forEach { parent ->
                            FilterChip(
                                selected = selectedParentId == parent.id,
                                onClick = {
                                    selectedParentId = parent.id
                                    userOverrodeRole = true
                                    selectedRole = FieldRole.PartOfMeasure(parentFieldId = parent.id)
                                },
                                label = { Text(parent.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (optional)") },
                    placeholder = { Text("e.g., patients, %, per 1,000, days") },
                    singleLine = true,
                    shape = StatGraphDesignTokens.Shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                if (effectiveRole.isNumeric) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { minText = it },
                            label = { Text("Min (optional)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = StatGraphDesignTokens.Shapes.medium,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxText,
                            onValueChange = { maxText = it },
                            label = { Text("Max (optional)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = StatGraphDesignTokens.Shapes.medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (effectiveRole is FieldRole.Category || effectiveRole is FieldRole.Group) {
                    OutlinedTextField(
                        value = categoriesText,
                        onValueChange = { categoriesText = it },
                        label = { Text("Quick Chips (comma-separated)") },
                        placeholder = { Text("e.g., Medical, Surgical, ICU") },
                        shape = StatGraphDesignTokens.Shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Required field",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Empty cells trigger a blocking error instead of staying optional.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphDesignTokens.Colors.slate
                        )
                    }
                    Switch(
                        checked = isRequired,
                        onCheckedChange = { isRequired = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val finalRole = if (effectiveRole is FieldRole.PartOfMeasure) {
                        FieldRole.PartOfMeasure(parentFieldId = selectedParentId)
                    } else {
                        effectiveRole
                    }
                    val parsedCategories = categoriesText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    onSave(
                        StatField(
                            id = initialField?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            unit = unit.trim(),
                            role = finalRole,
                            suggestedRole = autoSuggestion?.suggestedRole ?: initialField?.suggestedRole,
                            isRoleConfirmed = !notSureSelected,
                            minValue = minText.trim().toDoubleOrNull()?.takeIf { it.isFinite() },
                            maxValue = maxText.trim().toDoubleOrNull()?.takeIf { it.isFinite() },
                            isRequired = isRequired,
                            allowedCategories = parsedCategories
                        )
                    )
                }
            ) {
                Text(if (isEditing) "Save Changes" else "Add Field")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RoleOptionCard(
    title: String,
    hint: String,
    example: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (example.isNotBlank()) {
                    Text(
                        text = example,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private enum class DerivedTypeOption(val label: String) {
    RATE_PERCENT("Rate (%)"),
    PER_THOUSAND("Per 1,000"),
    MOVING_AVERAGE("Moving Average"),
    CUMULATIVE_TOTAL("Cumulative Total")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatGraphDerivedFieldDialog(
    fields: List<StatField>,
    onSave: (DerivedFieldDefinition) -> Unit,
    onDismiss: () -> Unit
) {
    val numericFields = remember(fields) { fields.filter { it.role.isNumeric } }
    val groupFields = remember(fields) {
        fields.filter { it.role is FieldRole.Category || it.role is FieldRole.Group }
    }

    var selectedType by remember { mutableStateOf(DerivedTypeOption.RATE_PERCENT) }
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("%") }
    var numeratorId by remember { mutableStateOf(numericFields.firstOrNull()?.id.orEmpty()) }
    var denominatorId by remember { mutableStateOf(numericFields.getOrNull(1)?.id) }
    var constantDenominatorText by remember { mutableStateOf("") }
    var windowSize by remember { mutableIntStateOf(3) }
    var groupById by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Derived Calculation", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (numericFields.isEmpty()) {
                    Text(
                        text = "Add at least one numeric Measure or Rate field before creating derived calculations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatGraphDesignTokens.Colors.slate
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DerivedTypeOption.entries.forEach { opt ->
                            FilterChip(
                                selected = selectedType == opt,
                                onClick = {
                                    selectedType = opt
                                    unit = when (opt) {
                                        DerivedTypeOption.RATE_PERCENT -> "%"
                                        DerivedTypeOption.PER_THOUSAND -> "per 1,000"
                                        DerivedTypeOption.MOVING_AVERAGE,
                                        DerivedTypeOption.CUMULATIVE_TOTAL ->
                                            numericFields.firstOrNull { it.id == numeratorId }?.unit.orEmpty()
                                    }
                                },
                                label = { Text(opt.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Derived Field Name *") },
                        placeholder = { Text("e.g., Dengue Rate %, 3-Mo Avg") },
                        singleLine = true,
                        shape = StatGraphDesignTokens.Shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Output Unit") },
                        singleLine = true,
                        shape = StatGraphDesignTokens.Shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (selectedType == DerivedTypeOption.RATE_PERCENT || selectedType == DerivedTypeOption.PER_THOUSAND) {
                            "Numerator Field"
                        } else {
                            "Source Field"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        numericFields.forEach { f ->
                            FilterChip(
                                selected = numeratorId == f.id,
                                onClick = { numeratorId = f.id },
                                label = { Text(f.name) }
                            )
                        }
                    }

                    when (selectedType) {
                        DerivedTypeOption.RATE_PERCENT,
                        DerivedTypeOption.PER_THOUSAND -> {
                            Text(
                                text = "Denominator Field (or enter constant below)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = denominatorId == null,
                                    onClick = { denominatorId = null },
                                    label = { Text("Constant") }
                                )
                                numericFields.forEach { f ->
                                    FilterChip(
                                        selected = denominatorId == f.id,
                                        onClick = { denominatorId = f.id },
                                        label = { Text(f.name) }
                                    )
                                }
                            }
                            if (denominatorId == null) {
                                OutlinedTextField(
                                    value = constantDenominatorText,
                                    onValueChange = { constantDenominatorText = it },
                                    label = { Text("Constant Denominator") },
                                    placeholder = { Text("e.g., 100000") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = StatGraphDesignTokens.Shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        DerivedTypeOption.MOVING_AVERAGE -> {
                            Text(
                                text = "Moving Average Window: $windowSize periods",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(2, 3, 5, 7).forEach { w ->
                                    FilterChip(
                                        selected = windowSize == w,
                                        onClick = { windowSize = w },
                                        label = { Text("${w}-pt") }
                                    )
                                }
                            }
                        }
                        DerivedTypeOption.CUMULATIVE_TOTAL -> {
                            if (groupFields.isNotEmpty()) {
                                Text(
                                    text = "Reset Cumulative Total Per Group (Optional)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = groupById == null,
                                        onClick = { groupById = null },
                                        label = { Text("Continuous") }
                                    )
                                    groupFields.forEach { gf ->
                                        FilterChip(
                                            selected = groupById == gf.id,
                                            onClick = { groupById = gf.id },
                                            label = { Text(gf.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val constDen = constantDenominatorText.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
            val needsDenom = selectedType == DerivedTypeOption.RATE_PERCENT || selectedType == DerivedTypeOption.PER_THOUSAND
            val canSave = numericFields.isNotEmpty() &&
                name.isNotBlank() &&
                numeratorId.isNotBlank() &&
                (!needsDenom || denominatorId != null || constDen != null)

            TextButton(
                enabled = canSave,
                onClick = {
                    val numName = numericFields.firstOrNull { it.id == numeratorId }?.name ?: "Field"
                    val denName = denominatorId?.let { id -> numericFields.firstOrNull { it.id == id }?.name }
                        ?: constantDenominatorText.trim()
                    val calc = when (selectedType) {
                        DerivedTypeOption.RATE_PERCENT -> DerivedCalculationType.RatePercentage(
                            numeratorFieldId = numeratorId,
                            denominatorFieldId = denominatorId,
                            constantDenominator = if (denominatorId == null) constDen else null
                        )
                        DerivedTypeOption.PER_THOUSAND -> DerivedCalculationType.PerThousand(
                            numeratorFieldId = numeratorId,
                            denominatorFieldId = denominatorId,
                            constantDenominator = if (denominatorId == null) constDen else null
                        )
                        DerivedTypeOption.MOVING_AVERAGE -> DerivedCalculationType.MovingAverage(
                            sourceFieldId = numeratorId,
                            windowSize = windowSize
                        )
                        DerivedTypeOption.CUMULATIVE_TOTAL -> DerivedCalculationType.CumulativeTotal(
                            sourceFieldId = numeratorId,
                            groupByFieldId = groupById
                        )
                    }
                    val formula = when (selectedType) {
                        DerivedTypeOption.RATE_PERCENT -> "($numName / $denName) × 100"
                        DerivedTypeOption.PER_THOUSAND -> "($numName / $denName) × 1,000"
                        DerivedTypeOption.MOVING_AVERAGE -> "$windowSize-period moving average of $numName"
                        DerivedTypeOption.CUMULATIVE_TOTAL -> "Running total of $numName"
                    }
                    onSave(
                        DerivedFieldDefinition(
                            name = name.trim(),
                            unit = unit.trim(),
                            calculation = calc,
                            formulaSummary = formula
                        )
                    )
                }
            ) {
                Text("Add Calculation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

internal fun isSameRoleKind(a: FieldRole, b: FieldRole): Boolean = when {
    a is FieldRole.Time && b is FieldRole.Time -> a.granularity == b.granularity
    a is FieldRole.PartOfMeasure && b is FieldRole.PartOfMeasure -> true
    a is FieldRole.RateContinuous && b is FieldRole.RateContinuous -> true
    a is FieldRole.EstimateWithLimits && b is FieldRole.EstimateWithLimits -> true
    a is FieldRole.DurationWithEvent && b is FieldRole.DurationWithEvent -> true
    else -> a == b
}
