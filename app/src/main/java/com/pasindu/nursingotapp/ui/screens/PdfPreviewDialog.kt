package com.pasindu.nursingotapp.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PdfPreviewDialog(
    pdfFile: File,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var pages by remember(pdfFile) {
        mutableStateOf<List<androidx.compose.ui.graphics.ImageBitmap>>(emptyList())
    }
    var errorMessage by remember(pdfFile) { mutableStateOf<String?>(null) }
    var isLoading by remember(pdfFile) { mutableStateOf(true) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    fun resetZoom() {
        zoom = 1f
        panX = 0f
        panY = 0f
    }

    LaunchedEffect(pdfFile) {
        isLoading = true
        errorMessage = null
        pages = emptyList()

        withContext(Dispatchers.IO) {
            try {
                ParcelFileDescriptor.open(
                    pdfFile,
                    ParcelFileDescriptor.MODE_READ_ONLY
                ).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        if (renderer.pageCount == 0) {
                            errorMessage = "The generated PDF is empty."
                        } else {
                            val renderedPages = buildList {
                                for (index in 0 until renderer.pageCount) {
                                    renderer.openPage(index).use { page ->
                                        val width = (page.width * 1.35f).toInt().coerceAtLeast(1)
                                        val height = (page.height * 1.35f).toInt().coerceAtLeast(1)
                                        val bitmap = Bitmap.createBitmap(
                                            width,
                                            height,
                                            Bitmap.Config.ARGB_8888
                                        )
                                        bitmap.eraseColor(android.graphics.Color.WHITE)
                                        page.render(
                                            bitmap,
                                            null,
                                            null,
                                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                        )
                                        add(bitmap.asImageBitmap())
                                    }
                                }
                            }
                            pages = renderedPages
                        }
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                errorMessage =
                    "Preview could not be loaded on this device.\n\nYour PDF is still ready to save."
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF6F8FC),
            tonalElevation = 4.dp,
            shadowElevation = 12.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 3.dp
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFEAF6FF)
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFF14A6E0),
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "PDF Review",
                                    color = Color(0xFF12204A),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "Review your OT claim before saving",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEAFBF5)
                            ) {
                                Text(
                                    if (errorMessage == null) "READY" else "SAVE READY",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    color = Color(0xFF0E9F73),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "OT Claim document",
                                color = Color(0xFF12204A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (pages.isEmpty()) "Preparing preview…" else "${pages.size} page${if (pages.size == 1) "" else "s"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        isLoading -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFF14A6E0))
                                Text(
                                    "Preparing document preview…",
                                    color = Color(0xFF12204A),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "This does not change the generated PDF.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        errorMessage != null -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEAF6FF)
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFF14A6E0),
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                                Text(
                                    "Preview unavailable",
                                    color = Color(0xFF12204A),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    errorMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        else -> {
                            Column(Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(50.dp),
                                        tonalElevation = 2.dp
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { zoom = (zoom - 0.25f).coerceAtLeast(1f) }) {
                                                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
                                            }
                                            Text(
                                                "${(zoom * 100).toInt()}%",
                                                modifier = Modifier.width(52.dp),
                                                textAlign = TextAlign.Center,
                                                color = Color(0xFF12204A),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            IconButton(onClick = { zoom = (zoom + 0.25f).coerceAtMost(4f) }) {
                                                Icon(Icons.Default.Add, contentDescription = "Zoom in")
                                            }
                                            IconButton(onClick = { resetZoom() }) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Reset zoom")
                                            }
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Surface(
                                        color = Color(0xFFEAF6FF),
                                        shape = RoundedCornerShape(50.dp)
                                    ) {
                                        Text(
                                            "Pinch / drag to inspect",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            color = Color(0xFF1769E8),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                                zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                                if (zoom > 1f) {
                                                    panX += pan.x
                                                    panY += pan.y
                                                } else {
                                                    panX = 0f
                                                    panY = 0f
                                                }
                                            }
                                        }
                                ) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = zoom,
                                                scaleY = zoom,
                                                translationX = panX,
                                                translationY = panY
                                            )
                                            .padding(horizontal = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        itemsIndexed(pages) { index, bitmap ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color.White,
                                                    tonalElevation = 2.dp,
                                                    shadowElevation = 4.dp
                                                ) {
                                                    Image(
                                                        bitmap = bitmap,
                                                        contentDescription = "PDF page ${index + 1}",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .border(1.dp, Color(0xFFD9DEE8), RoundedCornerShape(6.dp))
                                                    )
                                                }
                                                Spacer(Modifier.height(5.dp))
                                                Text(
                                                    "Page ${index + 1} of ${pages.size}",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = Color.White,
                    shadowElevation = 10.dp
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (errorMessage == null && !isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFEAFBF5)
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFF0E9F73),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Ready to save",
                                        color = Color(0xFF12204A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        "Your original PDF remains unchanged",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedActionButton(
                                text = "Edit Data",
                                icon = Icons.Default.Edit,
                                onClick = onDismiss,
                                modifier = Modifier.weight(0.85f)
                            )
                            Button(
                                onClick = onConfirm,
                                enabled = !isLoading,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(17.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF14A6E0)
                                )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    "Save PDF",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlinedActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(17.dp),
        color = Color(0xFFEAF6FF)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1769E8))
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                color = Color(0xFF1769E8),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
