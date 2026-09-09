// com/pasindu/nursingotapp/ui/screens/DailyEntryScreen.kt
package com.pasindu.nursingotapp.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pasindu.nursingotapp.ui.NursingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

val weekend_background_highlight = Color(0xFFF1F3F5)

data class StagedEdit(val shift: String? = null, val ot: String? = null, val leave: String? = null)

private val DailyInk = Color(0xFF12204A)
private val DailyBlue = Color(0xFF1769E8)
private val DailyCyan = Color(0xFF14A6E0)
private val DailyPurple = Color(0xFF7257E8)
private val DailyBlueSoft = Color(0xFFEAF6FF)
private val DailyPurpleSoft = Color(0xFFF3EEFF)
private val DailyMintSoft = Color(0xFFEAFBF5)
private val DailyHeroGradient = Brush.horizontalGradient(listOf(DailyBlue, DailyCyan, Color(0xFF4B78F2), DailyPurple))
private const val CATEGORY_SHIFT_DUTY = "Shift Duty"
private const val CATEGORY_LEAVE_REST = "Leave & Rest"
private const val CATEGORY_SERVICE_DAYS = "Service Days"
private const val CATEGORY_OVERTIME = "Overtime"

@Composable
fun PdfPreviewDialog(pdfFile: File, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var bitmaps by remember { mutableStateOf<List<androidx.compose.ui.graphics.ImageBitmap>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pdfFile) {
        withContext(Dispatchers.IO) {
            try {
                val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val pages = mutableListOf<androidx.compose.ui.graphics.ImageBitmap>()
                if (renderer.pageCount == 0) {
                    errorMessage = "The generated PDF is empty."
                } else {
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val bmp = Bitmap.createBitmap(
                            (page.width * 1.35f).toInt(),
                            (page.height * 1.35f).toInt(),
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages.add(bmp.asImageBitmap())
                        page.close()
                    }
                }
                renderer.close()
                fd.close()
                bitmaps = pages
            } catch (e: Throwable) {
                e.printStackTrace()
                errorMessage = "Preview could not be loaded on this device.\n\nYour PDF is still ready to save."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF5F7FB)
        ) {
            Column(Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 6.dp
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DailyBlueSoft
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = DailyCyan,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Review PDF",
                                    color = DailyInk,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "Check your OT claim before saving",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = DailyMintSoft
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF0E9F73), modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Ready", color = Color(0xFF0E9F73), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = DailyInk)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = DailyBlueSoft
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("OT claim document", color = DailyInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("A4 form • verify details, then save", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                                Text(
                                    "${bitmaps.size.coerceAtLeast(1)} ${if (bitmaps.size == 1) "page" else "pages"}",
                                    color = DailyCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                if (errorMessage == null && bitmaps.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(50.dp), color = Color.White, shadowElevation = 4.dp) {
                            Row(
                                Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { scale = (scale - 0.15f).coerceAtLeast(1f); if (scale == 1f) offset = Offset.Zero }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Remove, "Zoom out", tint = DailyInk)
                                }
                                Text(
                                    "${(scale * 100).toInt()}%",
                                    color = DailyCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.widthIn(min = 48.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(onClick = { scale = (scale + 0.15f).coerceAtMost(4f) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Add, "Zoom in", tint = DailyInk)
                                }
                                IconButton(onClick = { scale = 1f; offset = Offset.Zero }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Refresh, "Reset zoom", tint = DailyCyan)
                                }
                            }
                        }
                    }
                }

                when {
                    errorMessage != null -> {
                        Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 3.dp) {
                                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(shape = CircleShape, color = DailyBlueSoft) {
                                        Icon(Icons.Default.PictureAsPdf, null, tint = DailyCyan, modifier = Modifier.padding(14.dp))
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text("PDF ready to save", color = DailyInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(8.dp))
                                    Text(errorMessage!!, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    bitmaps.isEmpty() -> {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = DailyCyan)
                                Spacer(Modifier.height(14.dp))
                                Text("Preparing your document…", color = DailyInk, fontWeight = FontWeight.Bold)
                                Text("Large forms may take a moment", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }
                    else -> {
                        Box(
                            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp).clipToBounds()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 4f)
                                        if (scale > 1f) offset = Offset(offset.x + pan.x, offset.y + pan.y) else offset = Offset.Zero
                                    }
                                }
                        ) {
                            Column(
                                Modifier.fillMaxSize()
                                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                bitmaps.forEachIndexed { index, bmp ->
                                    Surface(shape = RoundedCornerShape(5.dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                                        Column {
                                            Image(bitmap = bmp, contentDescription = "PDF page ${index + 1}", modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE0E5EC)))
                                            if (bitmaps.size > 1) {
                                                Text("Page ${index + 1}", modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 12.dp
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(0.85f).height(52.dp),
                            shape = RoundedCornerShape(17.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Data", fontWeight = FontWeight.ExtraBold)
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1.15f).height(52.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save PDF", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}
