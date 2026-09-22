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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import com.pasindu.nursingotapp.data.paysheet.PaySheetVaultManager
import com.pasindu.nursingotapp.ui.PaySheetBankViewModel
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
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
import java.time.Instant
import java.time.ZoneId
import java.time.Year
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PaySheetBankScreen(
    onBack: () -> Unit,
    viewModel: PaySheetBankViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val vault = remember { PaySheetVaultManager(context) }
    val scope = rememberCoroutineScope()

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
            try {
                withContext(Dispatchers.IO) {
                    val existing = viewModel.findByMonth(monthKey)
                    val target = vault.prepareInput(uri, monthKey)
                    val now = System.currentTimeMillis()
                    viewModel.save(
                        PaySheetDocumentEntity(
                            id = existing?.id ?: 0L,
                            monthKey = monthKey,
                            displayMonth = displayMonthText(monthKey),
                            filePath = target.absolutePath,
                            fileSizeBytes = target.length(),
                            sha256 = vault.sha256(target.absolutePath),
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now
                        ),
                        successMessage = successMessage
                    )
                    if (existing != null && existing.filePath != target.absolutePath) {
                        vault.deleteFile(existing.filePath)
                    }
                }
            } catch (e: Exception) {
                message = e.message ?: "Unable to prepare paysheet."
            }
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val operationMessage = uiState.errorMessage ?: uiState.successMessage
        if (operationMessage != null) {
            message = operationMessage
            viewModel.clearMessage()
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
        if (success && monthKey != null && capturedUri != null) {
            saveImage(capturedUri, monthKey, "Camera photo saved securely in your private vault.")
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
        val monthKey = pendingMonth ?: currentMonthKeySafe()
        if (granted) {
            launchCameraForPaySheet(
                context,
                monthKey,
                cameraPicker,
                { uri: Uri -> cameraOutputUri = uri }
            ) { error: String ->
                pendingMonth = null
                message = error
            }
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
            launchCameraForPaySheet(
                context,
                monthKey,
                cameraPicker,
                { uri: Uri -> cameraOutputUri = uri }
            ) { error: String ->
                pendingMonth = null
                message = error
            }
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
    val monthsCovered = remember(documents) { documents.map { it.monthKey }.distinct().size }
    val sortedDocuments = remember(documents) { documents.sortedByDescending { it.monthKey } }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NursingDimensions.Spacing.lg, vertical = NursingDimensions.Spacing.lg)
                .clickable(onClick = onBack),
            shape = RoundedCornerShape(28.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClinicalAiGradient, RoundedCornerShape(28.dp))
                    .padding(horizontal = 18.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    Modifier.size(56.dp),
                    RoundedCornerShape(20.dp),
                    Color.White.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(27.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pay Sheet Bank", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Your private monthly paysheet vault", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(50.dp)) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("PRIVATE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = NursingDimensions.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)
        ) {
            item {
                VaultHero(
                    count = documents.size,
                    bytes = usedBytes,
                    monthsCovered = monthsCovered,
                    onAdd = { pickerYear = Year.now().value; showMonthPicker = true }
                )
            }
            item { PaySheetVaultSearchPanel(documents = documents, onDocumentSelected = { document: PaySheetDocumentEntity -> selected = document }) }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Paysheet Archive", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Your saved monthly documents", color = TextSecondary, fontSize = 10.sp)
                    }
                    Surface(
                        color = Purple.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.clickable { showDownloadCenter = true }
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, null, tint = Purple, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("EXPORT", color = Purple, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            items(sortedDocuments, key = { document: PaySheetDocumentEntity -> document.id }) { document: PaySheetDocumentEntity ->
                VaultMonthCard(
                    document,
                    onOpen = { selected = document },
                    onReplace = { pendingMonth = document.monthKey; showAddChooser = true },
                    onDelete = { deleteTarget = document }
                )
            }
            if (documents.isEmpty()) item { EmptyVaultCard { pickerYear = Year.now().value; showMonthPicker = true } }
            item { Spacer(Modifier.height(NursingDimensions.Spacing.lg)) }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            pickerYear,
            pendingMonth?.substringAfter("-")?.toIntOrNull()?.minus(1) ?: (LocalDate.now().monthValue - 1),
            onDismiss = { showMonthPicker = false }
        ) { year: Int, monthIndex: Int ->
            pickerYear = year
            chooseMonth(String.format(Locale.US, "%04d-%02d", year, monthIndex + 1))
        }
    }
    if (showAddChooser) {
        AddSourceDialog(
            pendingMonth ?: currentMonthKeySafe(),
            onDismiss = { showAddChooser = false; pendingMonth = null },
            onCamera = { month: String -> showAddChooser = false; startCamera(month) },
            onGallery = { month: String -> showAddChooser = false; startGallery(month) },
            onChangeMonth = { showAddChooser = false; showMonthPicker = true }
        )
    }
    if (showDownloadCenter) {
        PaySheetDownloadCenterDialog(documents, Year.now().value, context, onDismiss = { showDownloadCenter = false }, onMessage = { updatedMessage: String -> message = updatedMessage })
    }
    selected?.let { document: PaySheetDocumentEntity ->
        PaySheetViewerDialog(document = document, onDismiss = { selected = null }, onShare = { sharePaySheetFile(context, document) }, onDownload = { message = downloadPaySheetCopy(context, document) })
    }
    deleteTarget?.let { document: PaySheetDocumentEntity ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete paysheet?", fontWeight = FontWeight.Black) },
            text = { Text("Remove ${document.displayMonth} from your private vault?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch(Dispatchers.IO) {
                        vault.deleteFile(document.filePath)
                        viewModel.delete(document)
                    }
                }) { Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("CANCEL") } }
        )
    }
    message?.let { text: String ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Pay Sheet Bank", fontWeight = FontWeight.Black) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun EmptyVaultCard(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFF3EEFF)),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(62.dp), androidx.compose.foundation.shape.CircleShape, Color.White.copy(alpha = 0.82f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = Purple, modifier = Modifier.size(29.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Your vault is empty", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(
                "Keep every monthly paysheet in one private, organized place.",
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("ADD FIRST PAY SHEET", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun VaultHero(count: Int, bytes: Long, monthsCovered: Int, onAdd: () -> Unit) {
    val storageText = formatBytes(bytes)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("PRIVATE DOCUMENT VAULT", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your paysheets, safely organized", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("One private archive for every month.", color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Surface(Modifier.size(42.dp), androidx.compose.foundation.shape.CircleShape, Purple.copy(alpha = 0.10f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, null, tint = Purple, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VaultStat("$count", "PAY SHEETS", Modifier.weight(1f), Purple)
                VaultStat(storageText, "STORAGE USED", Modifier.weight(1f), Emerald)
                VaultStat("$monthsCovered", "MONTHS COVERED", Modifier.weight(1f), ClinicalPrimaryColor)
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ADD PAY SHEET", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun VaultStat(value: String, label: String, modifier: Modifier, accent: Color) {
    Surface(modifier = modifier, color = accent.copy(alpha = 0.08f), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 12.dp)) {
            Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = TextSecondary, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp)
        }
    }
}

@Composable
private fun VaultMonthCard(
    document: PaySheetDocumentEntity,
    onOpen: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(46.dp), RoundedCornerShape(14.dp), Purple.copy(alpha = 0.10f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = Purple, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(document.displayMonth, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(
                    formatBytes(document.fileSizeBytes) + " • Updated " + formatStoredDate(document.updatedAt),
                    color = TextSecondary,
                    fontSize = 8.5.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Emerald.copy(alpha = 0.10f), shape = RoundedCornerShape(50.dp)) {
                        Text("STORED", color = Emerald, fontSize = 7.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Private vault", color = TextSecondary, fontSize = 8.sp)
                }
            }
            IconButton(onClick = onReplace) {
                Icon(Icons.Default.PhotoCamera, "Replace paysheet", tint = ClinicalPrimaryColor)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, "Delete paysheet", tint = MaterialTheme.colorScheme.error)
            }
            Icon(Icons.Default.ChevronRight, "Open paysheet", tint = Slate, modifier = Modifier.size(18.dp))
        }
    }
}

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

private fun formatStoredDate(timestamp: Long): String = try {
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()
} catch (_: Exception) {
    "—"
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
