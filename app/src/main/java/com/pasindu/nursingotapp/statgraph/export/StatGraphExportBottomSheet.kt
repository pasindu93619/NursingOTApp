package com.pasindu.nursingotapp.statgraph.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.pasindu.nursingotapp.statgraph.chart.toolkit.BarDensityPolicy
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartExportLayoutPlanner
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ExportDensityAlternative
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ExportDensityAssessment
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphWorkspaceTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatGraphExportBottomSheet(
    payload: StatGraphExportPayload,
    onDismiss: () -> Unit,
    onSwitchToLineChart: (() -> Unit)? = null,
    onGroupIntoFourWeekBlocks: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var options by remember { mutableStateOf(StatGraphExportOptions()) }
    var uiState by remember { mutableStateOf<StatGraphExportUiState>(StatGraphExportUiState.Idle) }

    val pdfExportPlan = remember(
        payload.renderData,
        options.pdfOrientation,
        options.autoSelectOrientation,
        options.allowMultiPagePanels,
    ) {
        ChartExportLayoutPlanner.planPdfExport(
            data = payload.renderData,
            requestedOrientation = if (options.autoSelectOrientation) null else options.pdfOrientation,
            allowMultiPagePanels = options.allowMultiPagePanels,
        )
    }

    val densityAssessment: ExportDensityAssessment = remember(
        payload.renderData,
        payload.isOrderedTimeSeries,
        options.format,
        pdfExportPlan.orientation,
        options.allowMultiPagePanels,
        options.imageScale,
    ) {
        when (options.format) {
            StatGraphExportFormat.CSV -> ExportDensityAssessment(
                canFit = true,
                requiredWidthPt = 0f,
                availableWidthPt = 0f,
                estimatedBarWidthPt = 0f,
                totalBars = 0,
                categoryCount = payload.renderData.domainLabels.size,
                effectiveSeriesCount = 1,
                message = "",
                alternatives = emptyList(),
            )
            StatGraphExportFormat.PDF -> {
                val availablePlotPt = (pdfExportPlan.orientation.pageWidthPt - 72f - 72f).coerceAtLeast(100f)
                BarDensityPolicy.evaluateExportDensity(
                    data = payload.renderData,
                    availablePlotWidthPt = availablePlotPt,
                    isOrderedTimeSeries = payload.isOrderedTimeSeries,
                    allowMultiPagePanels = options.allowMultiPagePanels,
                )
            }
            StatGraphExportFormat.PNG, StatGraphExportFormat.JPG -> {
                val (plannedW, _) = ChartExportLayoutPlanner.planImageExportDimensions(
                    data = payload.renderData,
                    scale = options.imageScale,
                )
                val availablePlotPt = (plannedW / (2f * options.imageScale.multiplier) - 72f)
                    .coerceAtLeast(100f)
                BarDensityPolicy.evaluateExportDensity(
                    data = payload.renderData,
                    availablePlotWidthPt = availablePlotPt,
                    isOrderedTimeSeries = payload.isOrderedTimeSeries,
                    allowMultiPagePanels = true,
                )
            }
        }
    }

    val suggestedFilename = remember(payload.renderData.title, options.format) {
        StatGraphExportFormatter.sanitizeFilename(payload.renderData.title, options.format)
    }

    val safCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(options.format.mimeType),
    ) { uri: Uri? ->
        if (uri == null) {
            uiState = StatGraphExportUiState.Cancelled()
            return@rememberLauncherForActivityResult
        }
        if (!payload.renderData.hasDrawablePoints) {
            uiState = StatGraphExportUiState.Error("Chart has no valid data points to export.")
            return@rememberLauncherForActivityResult
        }
        uiState = StatGraphExportUiState.Exporting
        scope.launch {
            uiState = executeExportToSafAndCache(
                context = context,
                payload = payload,
                options = options,
                targetSafUri = uri,
                suggestedFileName = suggestedFilename,
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StatGraphWorkspaceTokens.Colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Download your chart",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = StatGraphWorkspaceTokens.Colors.ink,
            )

            Text(
                text = "Exports use the exact chart, selected palette, and aggregate values only.",
                style = MaterialTheme.typography.bodySmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )

            // Format selection
            Text(
                text = "Format",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = StatGraphWorkspaceTokens.Colors.ink,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatGraphExportFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = options.format == fmt,
                        onClick = {
                            options = options.copy(format = fmt)
                            uiState = StatGraphExportUiState.Idle
                        },
                        label = { Text(fmt.displayName) },
                    )
                }
            }

            HorizontalDivider(color = StatGraphWorkspaceTokens.Colors.borderSubtle)

            // Format-specific options
            when (options.format) {
                StatGraphExportFormat.PDF -> {
                    Text(
                        text = "Page orientation",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = options.autoSelectOrientation,
                            onClick = { options = options.copy(autoSelectOrientation = true) },
                            label = { Text("Auto (${pdfExportPlan.orientation.displayName})") },
                        )
                        PdfOrientation.entries.forEach { orientation ->
                            FilterChip(
                                selected = !options.autoSelectOrientation && options.pdfOrientation == orientation,
                                onClick = {
                                    options = options.copy(
                                        autoSelectOrientation = false,
                                        pdfOrientation = orientation,
                                    )
                                },
                                label = { Text(orientation.displayName) },
                            )
                        }
                    }
                    Text(
                        text = pdfExportPlan.orientationReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphWorkspaceTokens.Colors.slate,
                    )
                    if (pdfExportPlan.panels.size > 1 || pdfExportPlan.abbreviationKey.isNotEmpty()) {
                        Text(
                            text = pdfExportPlan.adaptationSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include data table",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Automatically adds extra A4 pages for long tables",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatGraphWorkspaceTokens.Colors.slate,
                            )
                        }
                        Switch(
                            checked = options.includeDataTable,
                            onCheckedChange = { options = options.copy(includeDataTable = it) },
                        )
                    }

                    OutlinedTextField(
                        value = options.customNotes,
                        onValueChange = { options = options.copy(customNotes = it) },
                        label = { Text("Optional notes") },
                        placeholder = { Text("e.g., Q1 surgical ward review") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                }

                StatGraphExportFormat.PNG,
                StatGraphExportFormat.JPG -> {
                    Text(
                        text = "Resolution scale",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ImageExportScale.entries.forEach { scale ->
                            FilterChip(
                                selected = options.imageScale == scale,
                                onClick = { options = options.copy(imageScale = scale) },
                                label = { Text("${scale.label} (${scale.widthPx}×${scale.heightPx})") },
                            )
                        }
                    }

                    if (options.format == StatGraphExportFormat.PNG) {
                        Text(
                            text = "Background",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ImageBackgroundMode.entries.forEach { bg ->
                                FilterChip(
                                    selected = options.imageBackground == bg,
                                    onClick = { options = options.copy(imageBackground = bg) },
                                    label = { Text(bg.displayName) },
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "JPG exports always use a crisp white background.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatGraphWorkspaceTokens.Colors.slate,
                        )
                    }
                }

                StatGraphExportFormat.CSV -> {
                    Text(
                        text = "Exports the chart's aggregate data table (${payload.renderData.domainLabels.size} rows). Blank cells remain empty and no patient identifiers are included.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphWorkspaceTokens.Colors.slate,
                    )
                }
            }

            // Status / Feedback banner
            when (val current = uiState) {
                is StatGraphExportUiState.Exporting -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Preparing ${options.format.displayName}…", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                is StatGraphExportUiState.Success -> {
                    Surface(
                        shape = StatGraphDesignTokens.Shapes.large,
                        color = androidx.compose.ui.graphics.Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color(0xFF059669),
                                )
                                Text(
                                    text = if (current.savedToSaf) {
                                        "Saved ${current.fileName}"
                                    } else {
                                        "Ready to share ${current.fileName}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatGraphWorkspaceTokens.Colors.ink,
                                )
                            }

                            val shareUri = current.shareableUriString?.let { Uri.parse(it) }
                            if (shareUri != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            launchViewIntent(context, shareUri, current.mimeType) { msg ->
                                                uiState = StatGraphExportUiState.Error(msg)
                                            }
                                        },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Open")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            launchShareIntent(context, shareUri, current.mimeType, payload.renderData.title) { msg ->
                                                uiState = StatGraphExportUiState.Error(msg)
                                            }
                                        },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Share")
                                    }
                                }
                            }
                        }
                    }
                }

                is StatGraphExportUiState.Cancelled -> {
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatGraphWorkspaceTokens.Colors.slate,
                    )
                }

                is StatGraphExportUiState.Error -> {
                    Surface(
                        shape = StatGraphDesignTokens.Shapes.large,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = current.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                StatGraphExportUiState.Idle -> Unit
            }

            if (!densityAssessment.canFit) {
                Surface(
                    shape = StatGraphDesignTokens.Shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "Too dense for ${options.format.displayName} width",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        Text(
                            text = densityAssessment.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            densityAssessment.alternatives.forEach { alt ->
                                when (alt) {
                                    ExportDensityAlternative.SWITCH_TO_LINE_OR_AREA -> {
                                        if (onSwitchToLineChart != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    onSwitchToLineChart()
                                                    onDismiss()
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = 48.dp),
                                            ) {
                                                Text(alt.title)
                                            }
                                        } else {
                                            Text(
                                                text = "• ${alt.title}: ${alt.description}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                        }
                                    }
                                    ExportDensityAlternative.GROUP_INTO_FOUR_WEEK_BLOCKS -> {
                                        if (onGroupIntoFourWeekBlocks != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    onGroupIntoFourWeekBlocks()
                                                    onDismiss()
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = 48.dp),
                                            ) {
                                                Text(alt.title)
                                            }
                                        } else {
                                            Text(
                                                text = "• ${alt.title}: ${alt.description}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                        }
                                    }
                                    ExportDensityAlternative.USE_LANDSCAPE_OR_WIDER_OUTPUT -> {
                                        if (options.format == StatGraphExportFormat.PDF &&
                                            options.pdfOrientation == PdfOrientation.PORTRAIT
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    options = options.copy(pdfOrientation = PdfOrientation.LANDSCAPE)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = 48.dp),
                                            ) {
                                                Text("Switch to Landscape A4")
                                            }
                                        } else {
                                            Text(
                                                text = "• ${alt.title}: ${alt.description}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Primary Save + Quick Share actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        if (!payload.renderData.hasDrawablePoints) {
                            uiState = StatGraphExportUiState.Error("Chart has no valid data points to export.")
                        } else if (!densityAssessment.canFit) {
                            uiState = StatGraphExportUiState.Error(densityAssessment.message)
                        } else {
                            safCreateLauncher.launch(suggestedFilename)
                        }
                    },
                    enabled = uiState !is StatGraphExportUiState.Exporting && densityAssessment.canFit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatGraphWorkspaceTokens.Colors.brandIndigo,
                        contentColor = StatGraphWorkspaceTokens.Colors.onBrandIndigo,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save ${options.format.extension.uppercase()}")
                }

                OutlinedButton(
                    onClick = {
                        if (!payload.renderData.hasDrawablePoints) {
                            uiState = StatGraphExportUiState.Error("Chart has no valid data points to export.")
                            return@OutlinedButton
                        }
                        if (!densityAssessment.canFit) {
                            uiState = StatGraphExportUiState.Error(densityAssessment.message)
                            return@OutlinedButton
                        }
                        uiState = StatGraphExportUiState.Exporting
                        scope.launch {
                            val result = executeExportToSafAndCache(
                                context = context,
                                payload = payload,
                                options = options,
                                targetSafUri = null,
                                suggestedFileName = suggestedFilename,
                            )
                            uiState = result
                            if (result is StatGraphExportUiState.Success && result.shareableUriString != null) {
                                launchShareIntent(
                                    context = context,
                                    uri = Uri.parse(result.shareableUriString),
                                    mimeType = result.mimeType,
                                    title = payload.renderData.title,
                                ) { msg ->
                                    uiState = StatGraphExportUiState.Error(msg)
                                }
                            }
                        }
                    },
                    enabled = uiState !is StatGraphExportUiState.Exporting && densityAssessment.canFit,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }

            Text(
                text = StatGraphExportFormatter.PRIVACY_FOOTER,
                style = MaterialTheme.typography.labelSmall,
                color = StatGraphWorkspaceTokens.Colors.slate,
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

private suspend fun executeExportToSafAndCache(
    context: Context,
    payload: StatGraphExportPayload,
    options: StatGraphExportOptions,
    targetSafUri: Uri?,
    suggestedFileName: String,
): StatGraphExportUiState = runCatching {
    val bytes: ByteArray = when (options.format) {
        StatGraphExportFormat.PDF -> withContext(Dispatchers.Main) {
            StatGraphPdfExporter.generatePdfBytes(context, payload, options)
        }
        StatGraphExportFormat.PNG,
        StatGraphExportFormat.JPG -> withContext(Dispatchers.Main) {
            StatGraphChartBitmapRenderer.renderImageBytes(
                context = context,
                data = payload.renderData,
                style = payload.chartStyle,
                format = options.format,
                scale = options.imageScale,
                backgroundMode = options.imageBackground,
            )
        }
        StatGraphExportFormat.CSV -> withContext(Dispatchers.Default) {
            val table = StatGraphExportFormatter.buildDataTable(payload.renderData)
            StatGraphExportFormatter.toCsvString(table).toByteArray(Charsets.UTF_8)
        }
    }

    withContext(Dispatchers.IO) {
        if (targetSafUri != null) {
            context.contentResolver.openOutputStream(targetSafUri)?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: error("Could not open selected destination for writing.")
        }

        val exportDir = File(context.filesDir, "statgraph_exports").apply { mkdirs() }
        val shareFile = File(exportDir, suggestedFileName)
        shareFile.writeBytes(bytes)
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile,
        )

        StatGraphExportUiState.Success(
            format = options.format,
            fileName = suggestedFileName,
            savedToSaf = targetSafUri != null,
            shareableUriString = shareUri.toString(),
            mimeType = options.format.mimeType,
        )
    }
}.getOrElse { err ->
    StatGraphExportUiState.Error(err.localizedMessage ?: "Export failed.")
}

private fun launchViewIntent(
    context: Context,
    uri: Uri,
    mimeType: String,
    onError: (String) -> Unit,
) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open exported chart"))
    }.onFailure {
        onError("No app installed on this device can open $mimeType files.")
    }
}

private fun launchShareIntent(
    context: Context,
    uri: Uri,
    mimeType: String,
    title: String,
    onError: (String) -> Unit,
) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Stat Graph Studio Chart" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share exported chart"))
    }.onFailure {
        onError("Could not launch share sheet for this file.")
    }
}
