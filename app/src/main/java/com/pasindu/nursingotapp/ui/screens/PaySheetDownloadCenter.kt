package com.pasindu.nursingotapp.ui.screens

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import com.pasindu.nursingotapp.ui.theme.BrandIndigo
import com.pasindu.nursingotapp.ui.theme.BrandIndigoSoft
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import java.io.File
import java.io.FileOutputStream

@Composable
fun PaySheetDownloadCenterDialog(
    documents: List<PaySheetDocumentEntity>,
    initialYear: Int,
    context: Context,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }

    val months = remember {
        listOf(
            "Jan" to "01", "Feb" to "02", "Mar" to "03",
            "Apr" to "04", "May" to "05", "Jun" to "06",
            "Jul" to "07", "Aug" to "08", "Sep" to "09",
            "Oct" to "10", "Nov" to "11", "Dec" to "12"
        )
    }

    val documentsByMonth = remember(documents, selectedYear) {
        documents
            .filter { it.monthKey.startsWith("$selectedYear-") }
            .associateBy { it.monthKey }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = BrandIndigo
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Find & Download", fontWeight = FontWeight.Black)
                }
                Text(
                    "Choose a year, then tap a saved month.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { selectedYear-- }) {
                            Icon(
                                Icons.Default.ArrowBackIosNew,
                                contentDescription = "Previous year",
                                tint = BrandIndigo
                            )
                        }
                        Text(
                            selectedYear.toString(),
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFeatureSettings = "tnum"
                            )
                        )
                        IconButton(onClick = { selectedYear++ }) {
                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = "Next year",
                                tint = BrandIndigo
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                months.chunked(3).forEach { monthRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        monthRow.forEach { (label, number) ->
                            val key = "$selectedYear-$number"
                            val document = documentsByMonth[key]
                            val saved = document != null

                            Surface(
                                onClick = {
                                    document?.let {
                                        onMessage(downloadPaysheet(context, it))
                                    }
                                },
                                enabled = saved,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 72.dp),
                                color = if (saved) BrandIndigoSoft else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (saved) BrandIndigo.copy(alpha = 0.25f) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (saved) BrandIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (saved) "Download" else "Empty",
                                        color = if (saved) Color(0xFF0E7490) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", fontWeight = FontWeight.Bold, color = BrandIndigo)
            }
        }
    )
}

private fun downloadPaysheet(
    context: Context,
    document: PaySheetDocumentEntity
): String {
    val source = File(document.filePath)
    if (!source.exists()) return "Paysheet file is no longer available."

    return try {
        val downloadsDirectory = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ),
            "NursingOS Pay Sheets"
        ).apply { mkdirs() }

        val target = File(
            downloadsDirectory,
            "Paysheet_${document.monthKey}.jpg"
        )

        source.inputStream().use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }

        "${document.displayMonth} downloaded to Downloads/NursingOS Pay Sheets."
    } catch (e: Exception) {
        "Download failed: ${e.message ?: "unknown error"}"
    }
}
