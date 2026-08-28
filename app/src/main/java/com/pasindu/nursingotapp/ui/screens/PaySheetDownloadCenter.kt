package com.pasindu.nursingotapp.ui.screens

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
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
            "JAN" to "01", "FEB" to "02", "MAR" to "03",
            "APR" to "04", "MAY" to "05", "JUN" to "06",
            "JUL" to "07", "AUG" to "08", "SEP" to "09",
            "OCT" to "10", "NOV" to "11", "DEC" to "12"
        )
    }

    val documentsByMonth = remember(documents, selectedYear) {
        documents
            .filter { it.monthKey.startsWith("$selectedYear-") }
            .associateBy { it.monthKey }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFF4338CA)
                    )
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Find & Download", fontWeight = FontWeight.Black)
                }
                Text(
                    "Choose a year, then tap a saved month.",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }
        },
        text = {
            Column {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF8FAFC),
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
                                tint = Color(0xFF4338CA)
                            )
                        }
                        Text(
                            selectedYear.toString(),
                            color = Color(0xFF0F172A),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(onClick = { selectedYear++ }) {
                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = "Next year",
                                tint = Color(0xFF4338CA)
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
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp)
                                    .clickable(enabled = saved) {
                                        document?.let {
                                            onMessage(downloadPaysheet(context, it))
                                        }
                                    },
                                color = if (saved) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (saved) Color(0xFF4338CA) else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (saved) "DOWNLOAD" else "EMPTY",
                                        color = if (saved) Color(0xFF0891B2) else Color(0xFFCBD5E1),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold
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
            TextButton(onClick = onDismiss) { Text("DONE") }
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
