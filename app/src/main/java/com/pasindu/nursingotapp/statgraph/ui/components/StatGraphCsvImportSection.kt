package com.pasindu.nursingotapp.statgraph.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.data.CsvColumnOverride
import com.pasindu.nursingotapp.statgraph.data.CsvColumnPreview
import com.pasindu.nursingotapp.statgraph.data.CsvImportPreview
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.isMissingValue
import com.pasindu.nursingotapp.statgraph.model.toDisplayString
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatGraphCsvImportSection(
    rawCsvText: String,
    fileName: String?,
    hasHeaderOverride: Boolean?,
    columnOverrides: Map<Int, CsvColumnOverride>,
    preview: CsvImportPreview?,
    onCsvTextChange: (String, String?) -> Unit,
    onLoadSampleCsv: () -> Unit,
    onHeaderToggle: (Boolean) -> Unit,
    onColumnRoleOverride: (Int, FieldRole, Boolean) -> Unit,
    onClearCsv: () -> Unit,
    onAcceptImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fileReadError by remember { mutableStateOf<String?>(null) }

    val openCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = readCsvTextFromUri(context, uri)
            if (result.isSuccess) {
                fileReadError = null
                val (name, text) = result.getOrThrow()
                onCsvTextChange(text, name)
            } else {
                fileReadError = result.exceptionOrNull()?.message ?: "Unable to read CSV file"
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatGraphSectionHeader(
            eyebrow = "STANDALONE CSV ENGINE",
            title = "Paste or Import Aggregate CSV",
            icon = Icons.Default.TableChart
        )

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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            openCsvLauncher.launch(
                                arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Import .CSV File")
                    }

                    OutlinedButton(onClick = onLoadSampleCsv) {
                        Text("Load Sample CSV")
                    }

                    if (rawCsvText.isNotBlank()) {
                        OutlinedButton(onClick = onClearCsv) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                }

                if (fileName != null) {
                    Surface(
                        color = StatGraphDesignTokens.Colors.softBlue,
                        shape = StatGraphDesignTokens.Shapes.pill
                    ) {
                        Text(
                            text = "Source: $fileName",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatGraphDesignTokens.Colors.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (fileReadError != null) {
                    Surface(
                        color = StatGraphDesignTokens.Colors.softRose,
                        shape = StatGraphDesignTokens.Shapes.medium
                    ) {
                        Text(
                            text = fileReadError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphDesignTokens.Colors.onSoftRose,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = rawCsvText,
                    onValueChange = { onCsvTextChange(it, fileName) },
                    label = { Text("Paste CSV text") },
                    placeholder = {
                        Text("Month,Ward,Dengue Patients,Total Patients\n2026-01,Medical,18,120\n2026-02,Medical,,132")
                    },
                    supportingText = {
                        Text("Blank cells (e.g. ,,) remain missing and are never converted to 0.")
                    },
                    minLines = 4,
                    maxLines = 8,
                    shape = StatGraphDesignTokens.Shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                )
            }
        }

        if (preview != null) {
            CsvPreviewCard(
                preview = preview,
                hasHeaderOverride = hasHeaderOverride,
                columnOverrides = columnOverrides,
                onHeaderToggle = onHeaderToggle,
                onColumnRoleOverride = onColumnRoleOverride,
                onAcceptImport = onAcceptImport
            )
        }
    }
}

@Composable
private fun CsvPreviewCard(
    preview: CsvImportPreview,
    hasHeaderOverride: Boolean?,
    columnOverrides: Map<Int, CsvColumnOverride>,
    onHeaderToggle: (Boolean) -> Unit,
    onColumnRoleOverride: (Int, FieldRole, Boolean) -> Unit,
    onAcceptImport: () -> Unit
) {
    Surface(
        color = StatGraphDesignTokens.Colors.surface,
        shape = StatGraphDesignTokens.Shapes.extraLarge,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CSV Preview & Role Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        color = StatGraphDesignTokens.Colors.ink,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${preview.columns.size} columns · ${preview.rawRowCount} data rows · Header ${
                            if (preview.detectedHasHeader) "auto-detected" else "not detected"
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphDesignTokens.Colors.slate
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Headers",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = hasHeaderOverride ?: preview.effectiveHasHeader,
                        onCheckedChange = onHeaderToggle
                    )
                }
            }

            if (preview.parseWarnings.isNotEmpty()) {
                Surface(
                    color = StatGraphDesignTokens.Colors.softAmber,
                    shape = StatGraphDesignTokens.Shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CSV Parsing Notices (${preview.parseWarnings.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = StatGraphDesignTokens.Colors.onSoftAmber,
                            fontWeight = FontWeight.Bold
                        )
                        preview.parseWarnings.take(4).forEach { msg ->
                            Text(
                                text = "• $msg",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatGraphDesignTokens.Colors.slate
                            )
                        }
                    }
                }
            }

            // Column Role Confirmation Cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                preview.columns.forEach { col ->
                    val confirmed = columnOverrides[col.columnIndex]?.isRoleConfirmed ?: col.isRoleConfirmed
                    CsvColumnRoleRow(
                        column = col,
                        isConfirmed = confirmed,
                        onSelectRole = { role ->
                            onColumnRoleOverride(col.columnIndex, role, true)
                        }
                    )
                }
            }

            HorizontalDivider(color = StatGraphDesignTokens.Colors.borderSubtle)

            // Row Preview Strip
            Text(
                text = "Preview Rows (first ${preview.candidateDataset.rows.take(5).size} of ${preview.candidateDataset.rows.size})",
                style = MaterialTheme.typography.labelLarge,
                color = StatGraphDesignTokens.Colors.ink,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    preview.candidateDataset.fields.forEach { field ->
                        Surface(
                            color = StatGraphDesignTokens.Colors.softBlue,
                            shape = StatGraphDesignTokens.Shapes.small
                        ) {
                            Text(
                                text = field.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = StatGraphDesignTokens.Colors.ink,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .width(120.dp)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Data rows
                preview.candidateDataset.rows.take(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        preview.candidateDataset.fields.forEach { field ->
                            val cell = row.values[field.id]
                            val isMissing = cell.isMissingValue()
                            Surface(
                                color = if (isMissing) {
                                    StatGraphDesignTokens.Colors.softSlate
                                } else {
                                    StatGraphDesignTokens.Colors.surface
                                },
                                border = BorderStroke(1.dp, StatGraphDesignTokens.Colors.borderSubtle),
                                shape = StatGraphDesignTokens.Shapes.small
                            ) {
                                Text(
                                    text = if (isMissing) "— (missing)" else cell.toDisplayString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isMissing) {
                                        StatGraphDesignTokens.Colors.slate
                                    } else {
                                        StatGraphDesignTokens.Colors.ink
                                    },
                                    modifier = Modifier
                                        .width(120.dp)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Pre-import validation check
            StatGraphValidationBanner(report = preview.validationReport)

            Button(
                onClick = onAcceptImport,
                enabled = preview.candidateDataset.rows.isNotEmpty() && !preview.validationReport.hasBlockingErrors,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatGraphDesignTokens.Colors.primary
                ),
                shape = StatGraphDesignTokens.Shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (preview.validationReport.hasBlockingErrors) {
                        "Fix Blocking Errors Before Import"
                    } else {
                        "Accept Import (${preview.candidateDataset.rows.size} Rows)"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CsvColumnRoleRow(
    column: CsvColumnPreview,
    isConfirmed: Boolean,
    onSelectRole: (FieldRole) -> Unit
) {
    var roleMenuExpanded by remember { mutableStateOf(false) }
    val presets = remember { FieldRole.selectablePresets() }

    Surface(
        color = StatGraphDesignTokens.Colors.softSlate,
        shape = StatGraphDesignTokens.Shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = column.headerName,
                    style = MaterialTheme.typography.titleSmall,
                    color = StatGraphDesignTokens.Colors.ink,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildString {
                        append(column.detection.reason)
                        if (column.missingCount > 0) append(" · ${column.missingCount} missing")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StatGraphDesignTokens.Colors.slate
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    onClick = { roleMenuExpanded = true },
                    color = if (isConfirmed) {
                        StatGraphDesignTokens.Colors.softMint
                    } else {
                        StatGraphDesignTokens.Colors.softPurple
                    },
                    shape = StatGraphDesignTokens.Shapes.pill
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!isConfirmed) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StatGraphDesignTokens.Colors.purpleAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = column.selectedRole.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = StatGraphDesignTokens.Colors.ink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                DropdownMenu(
                    expanded = roleMenuExpanded,
                    onDismissRequest = { roleMenuExpanded = false }
                ) {
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.displayName) },
                            onClick = {
                                roleMenuExpanded = false
                                onSelectRole(preset)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun readCsvTextFromUri(context: Context, uri: Uri): Result<Pair<String, String>> = runCatching {
    val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    } ?: "imported.csv"

    val content = context.contentResolver.openInputStream(uri)?.use { stream ->
        stream.bufferedReader(Charsets.UTF_8).readText()
    } ?: error("Empty or unreadable CSV stream")

    displayName to content
}
