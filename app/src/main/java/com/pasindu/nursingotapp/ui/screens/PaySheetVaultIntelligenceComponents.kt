package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import com.pasindu.nursingotapp.ui.theme.BrandIndigo
import com.pasindu.nursingotapp.ui.theme.BrandIndigoSoft
import com.pasindu.nursingotapp.ui.theme.CoolCyanSoft
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
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
        shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(NursingDimensions.Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(BrandIndigoSoft, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Search, null, tint = BrandIndigo)
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        "Find a paysheet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Search your archive instantly",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YearChip("All", selectedYear == null) { selectedYear = null }
                    years.take(5).forEach { year ->
                        YearChip(year.toString(), selectedYear == year) { selectedYear = year }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                Text("No paysheet found.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            } else {
                filtered.take(6).forEach { document ->
                    Surface(
                        onClick = { onDocumentSelected(document) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .heightIn(min = NursingDimensions.TouchTarget.minimum),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(NursingDimensions.Radius.medium)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    document.displayMonth,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    formatVaultBytes(document.fileSizeBytes),
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                            }
                            Surface(
                                color = BrandIndigoSoft,
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    "Open",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandIndigo,
                                    maxLines = 1,
                                    softWrap = false
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
private fun YearChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.heightIn(min = NursingDimensions.TouchTarget.minimum),
        color = if (selected) BrandIndigo else Color(0xFFF1F5F9),
        border = if (selected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(50.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
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
        shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(NursingDimensions.Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(CoolCyanSoft, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BarChart, null, tint = Color(0xFF0E7490))
                }
                Text(
                    "Vault intelligence",
                    modifier = Modifier.weight(1f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = BrandIndigoSoft,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        "${documents.size} saved",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandIndigo,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox(Modifier.weight(1f), documents.size.toString(), "Pay sheets", BrandIndigo)
                StatBox(Modifier.weight(1.15f), formatVaultBytes(bytes), "Storage", Color(0xFF0E7490))
                StatBox(Modifier.weight(0.9f), years.toString(), "Years", Color(0xFF059669))
            }
        }
    }
}

@Composable
private fun StatBox(modifier: Modifier, value: String, label: String, accent: Color) {
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                value,
                style = TextStyle(
                    color = accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatVaultBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
