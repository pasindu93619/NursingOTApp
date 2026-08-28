package com.pasindu.nursingotapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import com.pasindu.nursingotapp.data.paysheet.PaySheetVaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaySheetBankScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { DatabaseProvider.getDatabase(context).paySheetDocumentDao() }
    val vault = remember { PaySheetVaultManager(context) }
    val documents by dao.observeAll().collectAsState(initial = emptyList())
    var selectedDocument by remember { mutableStateOf<PaySheetDocumentEntity?>(null) }
    var pendingMonth by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val monthKey = pendingMonth ?: return@rememberLauncherForActivityResult
        pendingMonth = null
        if (uri == null) return@rememberLauncherForActivityResult
        statusMessage = null
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val source = copyUriToCache(context, uri, monthKey)
                val target = vault.prepareInput(source, monthKey)
                val existing = dao.findByMonth(monthKey)
                if (existing != null && existing.filePath != target.absolutePath) vault.deleteFile(existing.filePath)
                val now = System.currentTimeMillis()
                dao.upsert(
                    PaySheetDocumentEntity(
                        id = existing?.id ?: 0,
                        monthKey = monthKey,
                        displayMonth = displayMonth(monthKey),
                        filePath = target.absolutePath,
                        fileSizeBytes = target.length(),
                        sha256 = vault.sha256(target.absolutePath),
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                source.delete()
                withContext(Dispatchers.Main) { statusMessage = "Paysheet saved to your private vault." }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { statusMessage = e.message ?: "Unable to save paysheet." }
            }
        }
    }

    val usedBytes = remember(documents) { documents.sumOf { it.fileSizeBytes } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FC))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF102A56)) }
            Column(Modifier.weight(1f)) {
                Text("Pay Sheet Bank", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                Text("Your private monthly paysheet vault", fontSize = 11.sp, color = Color(0xFF64748B))
            }
            Surface(color = Color(0xFF10B981).copy(alpha = 0.10f), shape = RoundedCornerShape(50.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("PRIVATE", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                VaultHeroCard(
                    documentCount = documents.size,
                    usedBytes = usedBytes,
                    onAddCurrentMonth = {
                        val key = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                        pendingMonth = key
                        picker.launch("image/*")
                    }
                )
            }

            item {
                VaultYearHeader(year = currentYear())
            }

            items(documents, key = { it.id }) { document ->
                PaysheetMonthCard(
                    document = document,
                    onOpen = { selectedDocument = document },
                    onReplace = {
                        pendingMonth = document.monthKey
                        picker.launch("image/*")
                    },
                    onDelete = {
                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                            vault.deleteFile(document.filePath)
                            dao.delete(document)
                        }
                    }
                )
            }

            item {
                EmptyMonthCard(
                    onAdd = {
                        val key = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                        pendingMonth = key
                        picker.launch("image/*")
                    }
                )
            }

            statusMessage?.let { message ->
                item {
                    Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(message, modifier = Modifier.padding(14.dp), color = Color(0xFF047857), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    selectedDocument?.let { document ->
        PaysheetViewerDialog(
            document = document,
            onDismiss = { selectedDocument = null },
            onShare = { shareFile(context, document) },
            onDownload = { shareFile(context, document) }
        )
    }
}

@Composable
private fun VaultHeroCard(documentCount: Int, usedBytes: Long, onAddCurrentMonth: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "vaultHero")
    val glow by transition.animateFloat(0.16f, 0.30f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "vaultGlow")
    Card(
        modifier = Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(30.dp)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF172554))
    ) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF172554), Color(0xFF312E81))))) {
            Box(Modifier.size(150.dp).clip(CircleShape).background(Color(0xFF06B6D4).copy(alpha = glow)).align(Alignment.TopEnd))
            Column(Modifier.padding(22.dp)) {
                Surface(color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(50.dp)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PRIVATE DOCUMENT VAULT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Never lose a paysheet again.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text("Stored privately on this device with one organized file per month.", color = Color.White.copy(alpha = 0.70f), fontSize = 11.sp)
                Spacer(Modifier.height(17.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaultMetric(Modifier.weight(1f), "$documentCount", "MONTHS", Color(0xFF06B6D4))
                    VaultMetric(Modifier.weight(1f), formatBytes(usedBytes), "STORAGE", Color(0xFF10B981))
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddCurrentMonth, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFF172554))
                    Spacer(Modifier.width(8.dp))
                    Text("ADD THIS MONTH'S PAY SHEET", color = Color(0xFF172554), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable private fun VaultMetric(modifier: Modifier, value: String, label: String, accent: Color) {
    Surface(modifier, color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color.White.copy(alpha = 0.58f), fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable private fun VaultYearHeader(year: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF7C3AED).copy(alpha = 0.10f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Folder, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(21.dp)) }
        Spacer(Modifier.width(10.dp))
        Column { Text("$year", color = Color(0xFF0F172A), fontSize = 19.sp, fontWeight = FontWeight.Black); Text("Monthly paysheet archive", color = Color(0xFF64748B), fontSize = 10.sp) }
    }
}

@Composable private fun PaysheetMonthCard(document: PaySheetDocumentEntity, onOpen: () -> Unit, onReplace: () -> Unit, onDelete: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "monthCard")
    val alpha by pulse.animateFloat(0.96f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "alpha")
    Card(modifier = Modifier.fillMaxWidth().animateContentSize().clickable(onClick = onOpen), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(88.dp, 108.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                AsyncImage(model = document.filePath, contentDescription = document.displayMonth, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = alpha)
                Surface(Modifier.align(Alignment.BottomStart).padding(6.dp), color = Color(0xFF10B981).copy(alpha = 0.90f), shape = RoundedCornerShape(8.dp)) { Text("SAVED", Modifier.padding(horizontal = 6.dp, vertical = 4.dp), color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(document.displayMonth, color = Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(formatBytes(document.fileSizeBytes), color = Color(0xFF64748B), fontSize = 10.sp)
                Text("Private device storage", color = Color(0xFF94A3B8), fontSize = 9.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MiniAction(Icons.Default.Visibility, "OPEN", onOpen, Color(0xFF4338CA))
                    MiniAction(Icons.Default.Download, "SAVE", onReplace, Color(0xFF06B6D4))
                    MiniAction(Icons.Default.DeleteOutline, "DEL", onDelete, Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable private fun MiniAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, color: Color) {
    Surface(modifier = Modifier.clickable(onClick = onClick), color = color.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(label, color = color, fontSize = 7.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable private fun EmptyMonthCard(onAdd: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF8FAFC)), contentAlignment = Alignment.Center) { Icon(Icons.Default.ReceiptLong, null, tint = Color(0xFF94A3B8)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text("Missing a month?", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("Add a paysheet to build your archive.", color = Color(0xFF64748B), fontSize = 10.sp) }
            IconButton(onClick = onAdd) { Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFF1976D2)) }
        }
    }
}

@Composable private fun PaysheetViewerDialog(document: PaySheetDocumentEntity, onDismiss: () -> Unit, onShare: () -> Unit, onDownload: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = Color.White) {
            Column(Modifier.animateContentSize()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(document.displayMonth, color = Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Black); Text("Private paysheet copy", color = Color(0xFF64748B), fontSize = 10.sp) }
                    IconButton(onClick = onDismiss) { Text("✕", color = Color(0xFF64748B), fontWeight = FontWeight.Black) }
                }
                AsyncImage(model = document.filePath, contentDescription = document.displayMonth, modifier = Modifier.fillMaxWidth().height(480.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black), contentScale = ContentScale.Fit)
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VaultViewerButton(Modifier.weight(1f), Icons.Default.Download, "SAVE COPY", onDownload)
                    VaultViewerButton(Modifier.weight(1f), Icons.Default.Share, "SHARE", onShare)
                }
            }
        }
    }
}

@Composable private fun VaultViewerButton(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF172554)), shape = RoundedCornerShape(14.dp)) { Icon(icon, null); Spacer(Modifier.width(6.dp)); Text(label, fontWeight = FontWeight.Black, fontSize = 10.sp) }
}

private fun currentYear(): Int = java.time.LocalDate.now().year
private fun displayMonth(key: String): String = try {
    val date = java.time.YearMonth.parse(key)
    date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
} catch (_: Exception) { key }
private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
private fun shareFile(context: Context, document: PaySheetDocumentEntity) {
    val file = java.io.File(document.filePath)
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply { type = "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    context.startActivity(Intent.createChooser(intent, "Share Paysheet"))
}
private fun copyUriToCache(context: Context, uri: Uri, monthKey: String): java.io.File {
    val target = java.io.File(context.cacheDir, "paysheet_import_${monthKey}_${System.currentTimeMillis()}.img")
    context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input) { "Unable to open selected image." }; target.outputStream().use { output -> input.copyTo(output) } }
    return target
}
