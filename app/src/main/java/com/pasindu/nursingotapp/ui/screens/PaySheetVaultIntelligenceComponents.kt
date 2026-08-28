package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import java.util.Locale

@Composable
fun PaySheetVaultSearchPanel(
    documents: List<PaySheetDocumentEntity>,
    onDocumentSelected: (PaySheetDocumentEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf<Int?>(null) }

    val years = remember(documents) {
        documents.mapNotNull { it.monthKey.take(4).toIntOrNull() }
            .distinct()
            .sortedDescending()
    }

    val filtered = remember(documents, query, selectedYear) {
        val normalized = query.trim()
        documents
            .filter { selectedYear == null || it.monthKey.startsWith("$selectedYear-") }
            .filter {
                normalized.isBlank() ||
                    it.displayMonth.contains(normalized, ignoreCase = true) ||
                    it.monthKey.contains(normalized, ignoreCase = true)
            }
            .sortedByDescending { it.monthKey }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(Color(0xFFEEF2FF), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Search, null, tint = Color(0xFF4338CA))
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        "Find a paysheet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        "Search your archive instantly",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("August 2026 or 2026-08") },
                shape = RoundedCornerShape(14.dp)
            )

            if (years.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    YearChip("ALL", selectedYear == null) { selectedYear = null }
                    years.take(5).forEach { year ->
                        YearChip(year.toString(), selectedYear == year) { selectedYear = year }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                Text("No paysheet found.", color = Color(0xFF64748B), fontSize = 11.sp)
            } else {
                filtered.take(6).forEach { document ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onDocumentSelected(document) },
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    document.displayMonth,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    formatVaultBytes(document.fileSizeBytes),
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Text(
                                "OPEN",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF4338CA)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) Color(0xFF4338CA) else Color(0xFFF1F5F9),
        shape = RoundedCornerShape(50.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            color = if (selected) Color.White else Color(0xFF475569),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun PaySheetVaultStorageCard(documents: List<PaySheetDocumentEntity>) {
    val bytes = remember(documents) { documents.sumOf { it.fileSizeBytes } }
    val years = remember(documents) {
        documents.mapNotNull { it.monthKey.take(4).toIntOrNull() }.distinct().size
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, null, tint = Color(0xFF0891B2))
                Spacer(Modifier.size(8.dp))
                Text(
                    "Vault intelligence",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox(Modifier.weight(1f), documents.size.toString(), "PAY SHEETS", Color(0xFF4338CA))
                StatBox(Modifier.weight(1f), formatVaultBytes(bytes), "STORAGE USED", Color(0xFF0891B2))
                StatBox(Modifier.weight(1f), years.toString(), "YEARS", Color(0xFF10B981))
            }
        }
    }
}

@Composable
private fun StatBox(modifier: Modifier, value: String, label: String, accent: Color) {
    Surface(modifier, color = accent.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatVaultBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
