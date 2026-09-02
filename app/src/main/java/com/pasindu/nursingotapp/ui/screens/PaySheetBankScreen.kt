package com.pasindu.nursingotapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import com.pasindu.nursingotapp.data.paysheet.PaySheetVaultManager
import com.pasindu.nursingotapp.ui.theme.AdvancedGradient
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PaySheetBankScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { DatabaseProvider.getDatabase(context).paySheetDocumentDao() }
    val vault = remember { PaySheetVaultManager(context) }
    val scope = rememberCoroutineScope()
    val documents by dao.observeAll().collectAsState(initial = emptyList())

    var selected by remember { mutableStateOf<PaySheetDocumentEntity?>(null) }
    var pendingMonth by remember { mutableStateOf<String?>(null) }
    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<PaySheetDocumentEntity?>(null) }
    var showAddChooser by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showDownloadCenter by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableStateOf(Year.now().value) }

    fun saveImage(uri: Uri, monthKey: String, successMessage: String) {
        scope.launch {
            message = try {
                withContext(Dispatchers.IO) {
                    val existing = dao.findByMonth(monthKey)
                    val target = vault.prepareInput(uri, monthKey)
                    val now = System.currentTimeMillis()
                    dao.upsert(PaySheetDocumentEntity(id = existing?.id ?: 0L, monthKey = monthKey, displayMonth = displayMonthText(monthKey), filePath = target.absolutePath, fileSizeBytes = target.length(), sha256 = vault.sha256(target.absolutePath), createdAt = existing?.createdAt ?: now, updatedAt = now))
                    if (existing != null && existing.filePath != target.absolutePath) vault.deleteFile(existing.filePath)
                }
                successMessage
            } catch (e: Exception) { e.message ?: "Unable to save paysheet." }
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val monthKey = pendingMonth ?: return@rememberLauncherForActivityResult
        pendingMonth = null
        if (uri != null) saveImage(uri, monthKey, "Paysheet saved securely in your private vault.")
    }

    val cameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        val monthKey = pendingMonth
        val capturedUri = cameraOutputUri
        pendingMonth = null
        cameraOutputUri = null
        if (success && monthKey != null && capturedUri != null) saveImage(capturedUri, monthKey, "Camera photo saved securely in your private vault.")
    }

    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
        val monthKey = pendingMonth ?: currentMonthKeySafe()
        if (granted) launchCameraForPaySheet(context, monthKey, cameraPicker, { uri: Uri -> cameraOutputUri = uri }) { error: String -> pendingMonth = null; message = error }
        else { pendingMonth = null; message = "Camera permission was denied. You can still add a paysheet from the gallery." }
    }

    fun startGallery(monthKey: String) { pendingMonth = monthKey; galleryPicker.launch("image/*") }
    fun startCamera(monthKey: String) {
        pendingMonth = monthKey
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCameraForPaySheet(context, monthKey, cameraPicker, { uri: Uri -> cameraOutputUri = uri }) { error: String -> pendingMonth = null; message = error }
        else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }
    fun chooseMonth(monthKey: String) { pendingMonth = monthKey; showMonthPicker = false; showAddChooser = true }

    val usedBytes = remember(documents) { documents.sumOf { it.fileSizeBytes } }
    val sortedDocuments = remember(documents) { documents.sortedByDescending { it.monthKey } }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        Surface(color = AppBackground) {
            Row(Modifier.fillMaxWidth().padding(horizontal = NursingDimensions.Spacing.md, vertical = NursingDimensions.Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate) }
                Spacer(Modifier.width(NursingDimensions.Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text("Pay Sheet Bank", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                    Text("Your private monthly paysheet vault", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Surface(color = Emerald.copy(alpha = 0.12f), shape = RoundedCornerShape(NursingDimensions.Radius.pill)) {
                    Row(Modifier.padding(horizontal = NursingDimensions.Spacing.sm, vertical = NursingDimensions.Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Emerald, modifier = Modifier.size(NursingDimensions.Icon.small))
                        Spacer(Modifier.width(NursingDimensions.Spacing.xs))
                        Text("PRIVATE", color = Emerald, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = NursingDimensions.Spacing.lg), verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)) {
            item { VaultHero(count = documents.size, bytes = usedBytes, onAdd = { pickerYear = Year.now().value; showMonthPicker = true }) }
            item { PaySheetVaultStorageCard(documents) }
            item { PaySheetVaultSearchPanel(documents = documents, onDocumentSelected = { document: PaySheetDocumentEntity -> selected = document }) }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = NursingDimensions.Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(NursingDimensions.Icon.featured).clip(RoundedCornerShape(NursingDimensions.Radius.medium)).background(Purple.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Folder, contentDescription = null, tint = Purple) }
                    Spacer(Modifier.width(NursingDimensions.Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text("Paysheet Archive", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                        Text("Choose any month and year when adding", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { showDownloadCenter = true }) { Icon(Icons.Default.Download, contentDescription = "Download", tint = Purple) }
                }
            }
            items(sortedDocuments, key = { document: PaySheetDocumentEntity -> document.id }) { document: PaySheetDocumentEntity ->
                VaultMonthCard(document, onOpen = { selected = document }, onReplace = { pendingMonth = document.monthKey; showAddChooser = true }, onDelete = { deleteTarget = document })
            }
            if (documents.isEmpty()) item { EmptyVaultCard { pickerYear = Year.now().value; showMonthPicker = true } }
            item { Spacer(Modifier.height(NursingDimensions.Spacing.lg)) }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(pickerYear, pendingMonth?.substringAfter("-")?.toIntOrNull()?.minus(1) ?: (LocalDate.now().monthValue - 1), onDismiss = { showMonthPicker = false }) { year: Int, monthIndex: Int ->
            pickerYear = year
            chooseMonth(String.format(Locale.US, "%04d-%02d", year, monthIndex + 1))
        }
    }
    if (showAddChooser) {
        AddSourceDialog(pendingMonth ?: currentMonthKeySafe(), onDismiss = { showAddChooser = false; pendingMonth = null }, onCamera = { month: String -> showAddChooser = false; startCamera(month) }, onGallery = { month: String -> showAddChooser = false; startGallery(month) }, onChangeMonth = { showAddChooser = false; showMonthPicker = true })
    }
    if (showDownloadCenter) {
        PaySheetDownloadCenterDialog(documents, Year.now().value, context, onDismiss = { showDownloadCenter = false }, onMessage = { updatedMessage: String -> message = updatedMessage })
    }
    selected?.let { document: PaySheetDocumentEntity ->
        PaySheetViewerDialog(document = document, onDismiss = { selected = null }, onShare = { sharePaySheetFile(context, document) }, onDownload = { message = downloadPaySheetCopy(context, document) })
    }
    deleteTarget?.let { document: PaySheetDocumentEntity ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete paysheet?", fontWeight = FontWeight.Black) }, text = { Text("Remove ${document.displayMonth} from your private vault?") }, confirmButton = { TextButton(onClick = { deleteTarget = null; scope.launch(Dispatchers.IO) { vault.deleteFile(document.filePath); dao.delete(document) } }) { Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black) } }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("CANCEL") } })
    }
    message?.let { text: String -> AlertDialog(onDismissRequest = { message = null }, title = { Text("Pay Sheet Bank", fontWeight = FontWeight.Black) }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }) }
}

@Composable
private fun EmptyVaultCard(onAdd: () -> Unit) { Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge)) { Row(Modifier.padding(NursingDimensions.Spacing.lg), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(54.dp).clip(RoundedCornerShape(NursingDimensions.Radius.large)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = TextSecondary) }; Spacer(Modifier.width(NursingDimensions.Spacing.md)); Column(Modifier.weight(1f)) { Text("Your vault is empty", color = TextPrimary, style = MaterialTheme.typography.titleSmall); Text("Choose a month, then add your paysheet.", color = TextSecondary, style = MaterialTheme.typography.bodySmall) }; IconButton(onClick = onAdd) { Icon(Icons.Default.PhotoCamera, contentDescription = "Add paysheet", tint = MaterialTheme.colorScheme.primary) } } } }

@Composable
private fun VaultHero(count: Int, bytes: Long, onAdd: () -> Unit) { val storageText = formatBytes(bytes); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge), color = Slate) { Column(Modifier.background(AdvancedGradient, RoundedCornerShape(NursingDimensions.Radius.extraLarge)).padding(NursingDimensions.Spacing.xxl), verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)) { Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(NursingDimensions.Radius.pill)) { Text("PRIVATE DOCUMENT VAULT", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = NursingDimensions.Spacing.md, vertical = NursingDimensions.Spacing.sm)) }; Text("Never lose a paysheet again.", color = Color.White, style = MaterialTheme.typography.headlineMedium); Text("One organized, private archive for every month.", color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodyLarge); Row(horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md), modifier = Modifier.fillMaxWidth()) { VaultStat("$count", "MONTHS", Modifier.weight(1f), MaterialTheme.colorScheme.primary); VaultStat(storageText, "STORAGE", Modifier.weight(1f), Emerald) }; Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Slate)) { Icon(Icons.Default.CameraAlt, contentDescription = null); Spacer(Modifier.width(NursingDimensions.Spacing.sm)); Text("ADD PAY SHEET", style = MaterialTheme.typography.titleMedium) } } } }

@Composable
private fun VaultStat(value: String, label: String, modifier: Modifier, accent: Color) { Surface(modifier = modifier, color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge)) { Column(Modifier.padding(NursingDimensions.Spacing.lg)) { Text(value, color = accent, style = MaterialTheme.typography.headlineMedium); Text(label, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium) } } }

@Composable
private fun VaultMonthCard(document: PaySheetDocumentEntity, onOpen: () -> Unit, onReplace: () -> Unit, onDelete: () -> Unit) { Surface(Modifier.fillMaxWidth().clickable(onClick = onOpen), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(NursingDimensions.Radius.large), tonalElevation = NursingDimensions.Elevation.card, shadowElevation = NursingDimensions.Elevation.card) { Row(Modifier.padding(NursingDimensions.Spacing.lg), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(NursingDimensions.Icon.featured).clip(RoundedCornerShape(NursingDimensions.Radius.medium)).background(Purple.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Purple) }; Spacer(Modifier.width(NursingDimensions.Spacing.md)); Column(Modifier.weight(1f)) { Text(document.displayMonth, color = TextPrimary, style = MaterialTheme.typography.titleMedium); Text(formatBytes(document.fileSizeBytes), color = TextSecondary, style = MaterialTheme.typography.bodySmall) }; IconButton(onClick = onReplace) { Icon(Icons.Default.PhotoCamera, contentDescription = "Replace paysheet", tint = MaterialTheme.colorScheme.primary) }; IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete paysheet", tint = MaterialTheme.colorScheme.error) }; TextButton(onClick = onOpen) { Text("OPEN") } } } }

@Composable
private fun MonthYearPickerDialog(initialYear: Int, initialMonth: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) { var year by remember(initialYear) { mutableStateOf(initialYear) }; var month by remember(initialMonth) { mutableStateOf(initialMonth.coerceIn(0, 11)) }; val months = remember { listOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Choose pay period", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Year", style = MaterialTheme.typography.titleMedium); Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { year-- }) { Text("−") }; Text(year.toString(), style = MaterialTheme.typography.titleMedium); TextButton(onClick = { year++ }) { Text("+") } } }; Text("Month", style = MaterialTheme.typography.titleMedium); Row(horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs)) { for (index in 0 until 6) TextButton(onClick = { month = index }) { Text(months[index]) } }; Row(horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs)) { for (index in 6 until 12) TextButton(onClick = { month = index }) { Text(months[index]) } } } }, confirmButton = { TextButton(onClick = { onConfirm(year, month) }) { Text("CONTINUE") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }) }

@Composable
private fun AddSourceDialog(monthKey: String, onDismiss: () -> Unit, onCamera: (String) -> Unit, onGallery: (String) -> Unit, onChangeMonth: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Add paysheet", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)) { Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(NursingDimensions.Radius.medium), modifier = Modifier.fillMaxWidth().clickable(onClick = onChangeMonth)) { Column(Modifier.padding(NursingDimensions.Spacing.md)) { Text("ARCHIVE MONTH", color = TextSecondary, style = MaterialTheme.typography.labelSmall); Text(displayMonthText(monthKey), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge); Text("Tap to change month / year", color = TextSecondary, style = MaterialTheme.typography.bodySmall) } }; Button(onClick = { onCamera(monthKey) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(NursingDimensions.Radius.medium)) { Icon(Icons.Default.CameraAlt, contentDescription = null); Spacer(Modifier.width(NursingDimensions.Spacing.sm)); Text("TAKE PHOTO") }; Button(onClick = { onGallery(monthKey) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(NursingDimensions.Radius.medium), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) { Icon(Icons.Default.PhotoCamera, contentDescription = null); Spacer(Modifier.width(NursingDimensions.Spacing.sm)); Text("CHOOSE FROM GALLERY") } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }) }

@Composable
private fun PaySheetViewerDialog(document: PaySheetDocumentEntity, onDismiss: () -> Unit, onShare: () -> Unit, onDownload: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(document.displayMonth, fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)) { Text("Private paysheet document", color = TextSecondary, style = MaterialTheme.typography.bodyMedium); Text(formatBytes(document.fileSizeBytes), color = TextSecondary, style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Row { TextButton(onClick = onShare) { Text("SHARE") }; TextButton(onClick = onDownload) { Text("DOWNLOAD") } } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }) }

private fun currentMonthKeySafe(): String = String.format(Locale.US, "%04d-%02d", Year.now().value, LocalDate.now().monthValue)

private fun displayMonthText(monthKey: String): String = try {
    val parts = monthKey.split('-')
    Month.of(parts[1].toInt()).getDisplayName(TextStyle.FULL, Locale.US) + " " + parts[0]
} catch (_: Exception) { monthKey }

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun launchCameraForPaySheet(context: Context, monthKey: String, cameraPicker: ManagedActivityResultLauncher<Uri, Boolean>, onUriReady: (Uri) -> Unit, onError: (String) -> Unit) {
    try {
        val file = File(context.cacheDir, "paysheet_${monthKey.replace('-', '_')}_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        onUriReady(uri)
        cameraPicker.launch(uri)
    } catch (e: Exception) { onError(e.message ?: "Unable to open camera.") }
}

private fun sharePaySheetFile(context: Context, document: PaySheetDocumentEntity) {
    val file = File(document.filePath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/*"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Pay Sheet"))
}

private fun downloadPaySheetCopy(context: Context, document: PaySheetDocumentEntity): String {
    return try {
        val source = File(document.filePath)
        if (!source.exists()) "Pay sheet file is no longer available." else {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloads.exists()) downloads.mkdirs()
            val target = File(downloads, source.name)
            source.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            "Saved a copy to Downloads."
        }
    } catch (e: Exception) { e.message ?: "Unable to save a copy." }
}