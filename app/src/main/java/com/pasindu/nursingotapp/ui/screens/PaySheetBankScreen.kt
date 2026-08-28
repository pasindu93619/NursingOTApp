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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import com.pasindu.nursingotapp.data.paysheet.PaySheetVaultManager
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

    var selected by remember { mutableStateOf<PaySheetDocumentEntity?>(null) }
    var pendingMonth by remember { mutableStateOf<String?>(null) }
    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<PaySheetDocumentEntity?>(null) }
    var showAddChooser by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showDownloadCenter by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableStateOf(currentYear()) }
    var downloadYear by remember { mutableStateOf(currentYear()) }

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
    val availableByYear = remember(documents, downloadYear) {
        documents.filter { it.monthKey.startsWith("$downloadYear-") }.associateBy { it.monthKey }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F7FC))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF172554))
            }
            Column(Modifier.weight(1f)) {
                Text("Pay Sheet Bank", color = Color(0xFF0F172A), fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Your private monthly paysheet vault", color = Color(0xFF64748B), fontSize = 11.sp)
            }
            Surface(color = Color(0xFF10B981).copy(alpha = 0.10f), shape = RoundedCornerShape(50.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("PRIVATE", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { VaultHero(count = documents.size, bytes = usedBytes, onAdd = { showMonthPicker = true; pickerYear = currentYear() }) }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF7C3AED).copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Folder, null, tint = Color(0xFF7C3AED))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Paysheet Archive", color = Color(0xFF0F172A), fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text("Choose any month and year when adding", color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                    IconButton(onClick = { showDownloadCenter = true; downloadYear = currentYear() }) {
                        Icon(Icons.Default.Download, "Find and download", tint = Color(0xFF4338CA))
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
                    Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(22.dp)) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF8FAFC)), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = Color(0xFF94A3B8))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Your vault is empty", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Choose a month, then add your paysheet.", color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                            IconButton(onClick = { showMonthPicker = true; pickerYear = currentYear() }) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFF1976D2))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
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
            initialYear = downloadYear,
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
                }) { Text("DELETE", color = Color(0xFFDC2626), fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("CANCEL") } }
        )
    }

    message?.let { text ->
        AlertDialog(onDismissRequest = { message = null }, title = { Text("Pay Sheet Bank", fontWeight = FontWeight.Black) }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } })
    }
}

@Composable
private fun AddSourceDialog(
    monthKey: String,
    onDismiss: () -> Unit,
    onCamera: (String) -> Unit,
    onGallery: (String) -> Unit,
    onChangeMonth: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add paysheet", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = Color(0xFFEEF2FF), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onChangeMonth)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("ARCHIVE MONTH", color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text(displayMonth(monthKey), color = Color(0xFF4338CA), fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("Tap to change month / year", color = Color(0xFF64748B), fontSize = 9.sp)
                    }
                }
                Button(onClick = { onCamera(monthKey) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(7.dp)); Text("TAKE PHOTO", fontWeight = FontWeight.Black)
                }
                Button(onClick = { onGallery(monthKey) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2FF), contentColor = Color(0xFF4338CA))) {
                    Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(7.dp)); Text("CHOOSE FROM GALLERY", fontWeight = FontWeight.Black)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
private fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var year by remember { mutableStateOf(initialYear) }
    var month by remember { mutableStateOf(initialMonth.coerceIn(0, 11)) }
    val months = remember { listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = Color(0xFF4338CA))
                Spacer(Modifier.width(8.dp))
                Text("Choose pay period", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(year.toString(), color = Color(0xFF0F172A), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Row {
                        TextButton(onClick = { year-- }) { Text("‹") }
                        TextButton(onClick = { year++ }) { Text("›") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                months.chunked(3).forEach { rowMonths ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowMonths.forEachIndexed { index, _ ->
                            val absoluteMonth = months.indexOf(rowMonths[index])
                            val isSelected = month == absoluteMonth
                            Surface(
                                modifier = Modifier.weight(1f).height(48.dp).clickable { month = absoluteMonth },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF4338CA) else Color(0xFFF8FAFC)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(months[absoluteMonth], color = if (isSelected) Color.White else Color(0xFF334155), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Surface(color = Color(0xFFECFEFF), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Selected: ${String.format(Locale.US, "%02d/%d", month + 1, year)}", Modifier.padding(12.dp), color = Color(0xFF0F766E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(year, month) }) { Text("CONTINUE", fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

private fun prepareAndLaunchCamera(
    context: Context,
    monthKey: String,
    cameraPicker: androidx.activity.result.ActivityResultLauncher<Uri>,
    onUriReady: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val directory = File(context.filesDir, "paysheet_camera").apply { mkdirs() }
        val outputFile = File(directory, "capture_${monthKey}_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
        onUriReady(uri)
        cameraPicker.launch(uri)
    } catch (e: Exception) {
        onError(e.message ?: "Unable to prepare camera capture.")
    }
}

@Composable
private fun VaultHero(count: Int, bytes: Long, onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(30.dp)), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF172554))) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF172554), Color(0xFF312E81))))) {
            Column(Modifier.padding(22.dp)) {
                Surface(color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(50.dp)) { Text("PRIVATE DOCUMENT VAULT", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(12.dp))
                Text("Never lose a paysheet again.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text("One organized, private archive for every month.", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroMetric(Modifier.weight(1f), count.toString(), "MONTHS", Color(0xFF06B6D4))
                    HeroMetric(Modifier.weight(1f), formatBytes(bytes), "STORAGE", Color(0xFF10B981))
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAdd, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                    Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFF172554))
                    Spacer(Modifier.width(8.dp))
                    Text("ADD PAY SHEET", color = Color(0xFF172554), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(modifier: Modifier, value: String, label: String, accent: Color) {
    Surface(modifier, color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color.White.copy(alpha = 0.58f), fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun VaultMonthCard(document: PaySheetDocumentEntity, onOpen: () -> Unit, onReplace: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AndroidView(
                modifier = Modifier.size(90.dp, 112.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF1F5F9)),
                factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                update = { image -> image.setImageBitmap(BitmapFactory.decodeFile(document.filePath)) }
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(document.displayMonth, color = Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(formatBytes(document.fileSizeBytes), color = Color(0xFF64748B), fontSize = 10.sp)
                Text("Private device storage", color = Color(0xFF94A3B8), fontSize = 9.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    VaultAction(Icons.Default.Visibility, "OPEN", onOpen, Color(0xFF4338CA))
                    VaultAction(Icons.Default.Download, "REPLACE", onReplace, Color(0xFF0891B2))
                    VaultAction(Icons.Default.DeleteOutline, "DEL", onDelete, Color(0xFFDC2626))
                }
            }
        }
    }
}

@Composable
private fun VaultAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, color: Color) {
    Surface(Modifier.clickable(onClick = onClick), color = color.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = color, fontSize = 7.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun VaultViewer(document: PaySheetDocumentEntity, onDismiss: () -> Unit, onShare: () -> Unit, onDownload: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Paysheet", color = Color(0xFF0F172A), fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(document.displayMonth, color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                    IconButton(onClick = onDismiss) { Text("×", color = Color(0xFF475569), fontSize = 27.sp) }
                }
                Spacer(Modifier.height(10.dp))
                AndroidView(modifier = Modifier.fillMaxWidth().height(430.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF1F5F9)), factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } }, update = { it.setImageBitmap(BitmapFactory.decodeFile(document.filePath)) })
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onDownload, Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2FF), contentColor = Color(0xFF4338CA))) {
                        Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("DOWNLOAD", fontWeight = FontWeight.Black)
                    }
                    Button(onClick = onShare, Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("SHARE", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

private fun downloadCopy(context: Context, document: PaySheetDocumentEntity): String {
    val source = File(document.filePath)
    if (!source.exists()) return "Paysheet file is no longer available."
    return try {
        val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NursingOS Pay Sheets").apply { mkdirs() }
        val target = File(downloadsDir, "Paysheet_${document.monthKey}.jpg")
        source.inputStream().use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
        "Paysheet downloaded to Downloads/NursingOS Pay Sheets."
    } catch (e: Exception) {
        "Download failed: ${e.message ?: "unknown error"}"
    }
}

private fun shareFile(context: Context, document: PaySheetDocumentEntity) {
    val file = File(document.filePath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share paysheet"))
}

private fun currentMonthKey(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
private fun currentYear(): Int = SimpleDateFormat("yyyy", Locale.US).format(Date()).toInt()
private fun currentMonthIndex(): Int = SimpleDateFormat("M", Locale.US).format(Date()).toInt() - 1
private fun displayMonth(key: String): String = SimpleDateFormat("MMMM yyyy", Locale.US).format(SimpleDateFormat("yyyy-MM", Locale.US).parse(key) ?: Date())
private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
