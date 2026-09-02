package com.pasindu.nursingotapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import com.pasindu.nursingotapp.data.paysheet.PaySheetVaultManager
import com.pasindu.nursingotapp.ui.theme.AdvancedGradient
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.EmergencyColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaySheetBankScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { DatabaseProvider.getDatabase(context).paySheetDocumentDao() }
    val vault = remember { PaySheetVaultManager(context) }
    val scope = rememberCoroutineScope()
    val documents by dao.observeAll().collectAsState(initial = emptyList())
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    var selected by remember { mutableStateOf<PaySheetDocumentEntity?>(null) }
    var pendingMonth by remember { mutableStateOf<String?>(null) }
    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<PaySheetDocumentEntity?>(null) }
    var showAddChooser by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showDownloadCenter by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableStateOf(currentYear()) }

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
                            displayMonth = displayMonth(monthKey),
                            filePath = target.absolutePath,
                            fileSizeBytes = target.length(),
                            sha256 = vault.sha256(target.absolutePath),
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
                    if (existing != null && existing.filePath != target.absolutePath) {
                        vault.deleteFile(existing.filePath)
                    }
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
        val monthKey = pendingMonth ?: currentMonthKey()
        if (granted) {
            prepareAndLaunchCamera(
                context = context,
                monthKey = monthKey,
                cameraPicker = cameraPicker,
                onUriReady = { cameraOutputUri = it },
                onError = { pendingMonth = null; message = it }
            )
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
        val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
            prepareAndLaunchCamera(
                context = context,
                monthKey = monthKey,
                cameraPicker = cameraPicker,
                onUriReady = { cameraOutputUri = it },
                onError = { pendingMonth = null; message = it }
            )
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
    val pageBottomPadding = 28.dp

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(top = systemBarsPadding.calculateTopPadding())
    ) {
        // Purpose-built compact header: safe-area aware, visually separated from system UI.
        Surface(color = AppBackground) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = NursingDimensions.Spacing.md,
                        end = NursingDimensions.Spacing.md,
                        top = NursingDimensions.Spacing.xs,
                        bottom = NursingDimensions.Spacing.md
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SurfaceMuted,
                    shape = RoundedCornerShape(NursingDimensions.Radius.medium)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Slate)
                    }
                }
                Spacer(Modifier.width(NursingDimensions.Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Pay Sheet Bank",
                        color = TextPrimary,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Your private monthly paysheet vault",
                        color = TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                Surface(
                    color = Emerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(NursingDimensions.Radius.pill)
                ) {
                    Row(
                        Modifier.padding(horizontal = NursingDimensions.Spacing.md, vertical = NursingDimensions.Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, null, tint = Emerald, modifier = Modifier.size(NursingDimensions.Icon.small))
                        Spacer(Modifier.width(NursingDimensions.Spacing.xs))
                        Text("PRIVATE", color = Emerald, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = NursingDimensions.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)
        ) {
            item { VaultHero(count = documents.size, bytes = usedBytes, onAdd = { showMonthPicker = true; pickerYear = currentYear() }) }
            item { PaySheetVaultStorageCard(documents) }
            item { PaySheetVaultSearchPanel(documents = documents, onDocumentSelected = { selected = it }) }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = NursingDimensions.Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(NursingDimensions.Icon.featured)
                            .clip(RoundedCornerShape(NursingDimensions.Radius.medium))
                            .background(Purple.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, null, tint = Purple)
                    }
                    Spacer(Modifier.width(NursingDimensions.Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text("Paysheet Archive", color = TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                        Text("Choose any month and year when adding", color = TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { showDownloadCenter = true }) {
                        Icon(Icons.Default.Download, "Find and download", tint = Purple)
                    }
                }
            }
            items(sortedDocuments, key = { it.id }) { document ->
                VaultMonthCard(
                    document = document,
                    onOpen = { selected = document },
                    onReplace = { pendingMonth = document.monthKey; showAddChooser = true },
                    onDelete = { deleteTarget = document }
                )
            }
            if (documents.isEmpty()) {
                item {
                    Surface(Modifier.fillMaxWidth(), color = SurfaceWhiteSafe(), shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge)) {
                        Row(Modifier.padding(NursingDimensions.Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(54.dp).clip(RoundedCornerShape(NursingDimensions.Radius.large)).background(SurfaceMuted),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = TextSecondary)
                            }
                            Spacer(Modifier.width(NursingDimensions.Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text("Your vault is empty", color = TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                                Text("Choose a month, then add your paysheet.", color = TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { showMonthPicker = true; pickerYear = currentYear() }) {
                                Icon(Icons.Default.PhotoCamera, null, tint = ClinicalBlueSafe())
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(pageBottomPadding)) }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            initialYear = pickerYear,
            initialMonth = pendingMonth?.substringAfter("-")?.toIntOrNull()?.minus(1) ?: currentMonthIndex(),
            onDismiss = { showMonthPicker = false },
            onConfirm = { year, monthIndex ->
                pickerYear = year
                chooseMonth(String.format(Locale.US, "%04d-%02d", year, monthIndex + 1))
            }
        )
    }

    if (showAddChooser) {
        AddSourceDialog(
            monthKey = pendingMonth ?: currentMonthKey(),
            onDismiss = { showAddChooser = false; pendingMonth = null },
            onCamera = { month -> showAddChooser = false; startCamera(month) },
            onGallery = { month -> showAddChooser = false; startGallery(month) },
            onChangeMonth = { showAddChooser = false; showMonthPicker = true }
        )
    }

    if (showDownloadCenter) {
        PaySheetDownloadCenterDialog(
            documents = documents,
            initialYear = currentYear(),
            context = context,
            onDismiss = { showDownloadCenter = false },
            onMessage = { message = it }
        )
    }

    selected?.let { document ->
        VaultViewer(document = document, onDismiss = { selected = null }, onShare = { shareFile(context, document) }, onDownload = { message = downloadCopy(context, document) })
    }

    deleteTarget?.let { document ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete paysheet?", fontWeight = FontWeight.Black) },
            text = { Text("Remove ${document.displayMonth} from your private vault?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch(Dispatchers.IO) {
                        vault.deleteFile(document.filePath)
                        dao.delete(document)
                    }
                }) { Text("DELETE", color = EmergencyColor, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("CANCEL") } }
        )
    }

    message?.let { text ->
        AlertDialog(onDismissRequest = { message = null }, title = { Text("Pay Sheet Bank", fontWeight = FontWeight.Black) }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } })
    }
}

@Composable
private fun SurfaceWhiteSafe(): Color = androidx.compose.material3.MaterialTheme.colorScheme.surface

private fun ClinicalBlueSafe(): Color = androidx.compose.ui.graphics.Color(0xFF0EA5E9)
