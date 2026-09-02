package com.pasindu.nursingotapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.Brush
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
    val topInset = WindowInsets.asPaddingValues().calculateTopPadding()

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
                    dao.upsert(
                        PaySheetDocumentEntity(
                            id = existing?.id ?: 0L,
                            monthKey = monthKey,
                            displayMonth = displayMonthText(monthKey),
                            filePath = target.absolutePath,
                            fileSizeBytes = target.length(),
                            sha256 = vault.sha256(target.absolutePath),
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
                    if (existing != null && existing.filePath != target.absolutePath) vault.deleteFile(existing.filePath)
                }
                successMessage
            } catch (e: Exception) {
                e.message ?: "Unable to save paysheet."
            }
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val monthKey = pendingMonth ?: return@rememberLauncherForActivityResult
        pendingMonth = null
        if (uri != null) saveImage(uri, monthKey, "Paysheet saved securely in your private vault.")
    }

    val cameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val monthKey = pendingMonth
        val capturedUri = cameraOutputUri
        pendingMonth = null
        cameraOutputUri = null
        if (success && monthKey != null && capturedUri != null) {
            saveImage(capturedUri, monthKey, "Camera photo saved securely in your private vault.")
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val monthKey = pendingMonth ?: currentMonthKeySafe()
        if (granted) {
            launchCameraForPaySheet(context, monthKey, cameraPicker, { cameraOutputUri = it }) { message = it; pendingMonth = null }
        } else {
            pendingMonth = null
            message = "Camera permission was denied. You can still add a paysheet from the gallery."
        }
    }

    fun startGallery(monthKey: String) {
        pendingMonth = monthKey
        galleryPicker.launch("image/*")
    }

    fun startCamera(monthKey: String) {
        pendingMonth = monthKey
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCameraForPaySheet(context, monthKey, cameraPicker, { cameraOutputUri = it }) { message = it; pendingMonth = null }
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun chooseMonth(monthKey: String) {
        pendingMonth = monthKey
        showMonthPicker = false
        showAddChooser = true
    }

    val usedBytes = remember(documents) { documents.sumOf { it.fileSizeBytes } }
    val sortedDocuments = remember(documents) { documents.sortedByDescending { it.monthKey } }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(top = topInset)
    ) {
        Surface(color = AppBackground) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = NursingDimensions.Spacing.md, vertical = NursingDimensions.Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(NursingDimensions.Radius.medium)) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Slate) }
                }
                Spacer(Modifier.width(NursingDimensions.Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text("Pay Sheet Bank", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                    Text("Your private monthly paysheet vault", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Surface(color = Emerald.copy(alpha = 0.12f), shape = RoundedCornerShape(NursingDimensions.Radius.pill)) {
                    Row(Modifier.padding(horizontal = NursingDimensions.Spacing.sm, vertical = NursingDimensions.Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Emerald, modifier = Modifier.size(NursingDimensions.Icon.small))
                        Spacer(Modifier.width(NursingDimensions.Spacing.xs))
                        Text("PRIVATE", color = Emerald, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = NursingDimensions.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)
        ) {
            item { VaultHero(count = documents.size, bytes = usedBytes, onAdd = { showMonthPicker = true; pickerYear = Year.now().value }) }
            item { PaySheetVaultStorageCard(documents) }
            item { PaySheetVaultSearchPanel(documents = documents, onDocumentSelected = { selected = it }) }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = NursingDimensions.Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(NursingDimensions.Icon.featured).clip(RoundedCornerShape(NursingDimensions.Radius.medium)).background(Purple.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Folder, null, tint = Purple)
                    }
                    Spacer(Modifier.width(NursingDimensions.Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text("Paysheet Archive", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                        Text("Choose any month and year when adding", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { showDownloadCenter = true }) { Icon(Icons.Default.Download, "Find and download", tint = Purple) }
                }
            }
            items(sortedDocuments, key = { it.id }) { document ->
                VaultMonthCard(document = document, onOpen = { selected = document }, onReplace = { pendingMonth = document.monthKey; showAddChooser = true }, onDelete = { deleteTarget = document })
            }
            if (documents.isEmpty()) {
                item {
                    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge)) {
                        Row(Modifier.padding(NursingDimensions.Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(54.dp).clip(RoundedCornerShape(NursingDimensions.Radius.large)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = TextSecondary)
                            }
                            Spacer(Modifier.width(NursingDimensions.Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text("Your vault is empty", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                Text("Choose a month, then add your paysheet.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { showMonthPicker = true; pickerYear = Year.now().value }) { Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFF0EA5E9)) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(initialYear = pickerYear, initialMonth = pendingMonth?.substringAfter("-")?.toIntOrNull()?.minus(1) ?: LocalDate.now().monthValue - 1, onDismiss = { showMonthPicker = false }) { year, monthIndex ->
            pickerYear = year
            chooseMonth(String.format(Locale.US, "%04d-%02d", year, monthIndex + 1))
        }
    }
    if (showAddChooser) {
        AddSourceDialog(monthKey = pendingMonth ?: currentMonthKeySafe(), onDismiss = { showAddChooser = false; pendingMonth = null }, onCamera = { month -> showAddChooser = false; startCamera(month) }, onGallery = { month -> showAddChooser = false; startGallery(month) }, onChangeMonth = { showAddChooser = false; showMonthPicker = true })
    }
    if (showDownloadCenter) {
        PaySheetDownloadCenterDialog(documents = documents, initialYear = Year.now().value, context = context, onDismiss = { showDownloadCenter = false }, onMessage = { message = it })
    }
    selected?.let { document -> VaultViewer(document = document, onDismiss = { selected = null }, onShare = { sharePaySheetFile(context, document) }, onDownload = { message = downloadPaySheetCopy(context, document) }) }
    deleteTarget?.let { document ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete paysheet?", fontWeight = FontWeight.Black) }, text = { Text("Remove ${document.displayMonth} from your private vault?") }, confirmButton = {
            TextButton(onClick = { deleteTarget = null; scope.launch(Dispatchers.IO) { vault.deleteFile(document.filePath); dao.delete(document) } }) { Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black) }
        }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("CANCEL") } })
    }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text("Pay Sheet Bank", fontWeight = FontWeight.Black) }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }) }
}

private fun currentMonthKeySafe(): String = String.format(Locale.US, "%04d-%02d", Year.now().value, LocalDate.now().monthValue)

private fun displayMonthText(monthKey: String): String = try {
    val parts = monthKey.split('-')
    Month.of(parts[1].toInt()).getDisplayName(TextStyle.FULL, Locale.US) + " " + parts[0]
} catch (_: Exception) { monthKey }

private fun launchCameraForPaySheet(context: Context, monthKey: String, cameraPicker: ActivityResultLauncher<Uri>, onUriReady: (Uri) -> Unit, onError: (String) -> Unit) {
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

private fun downloadPaySheetCopy(context: Context, document: PaySheetDocumentEntity): String = try {
    val source = File(document.filePath)
    if (!source.exists()) return "Pay sheet file is no longer available."
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloads.exists()) downloads.mkdirs()
    val target = File(downloads, source.name)
    source.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
    "Saved a copy to Downloads."
} catch (e: Exception) { e.message ?: "Unable to save a copy." }
