package com.pasindu.nursingotapp.ui.clinicalai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ClinicalAiAssistCard(
    suggestion: ClinicalAiSuggestion?,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestion == null) return
    val accent = when (suggestion.severity) {
        AiSeverity.INFO -> Color(0xFF1769E8)
        AiSeverity.CHECK -> Color(0xFFE88A00)
        AiSeverity.HIGH_ALERT -> Color(0xFFD92D4F)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.06f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(suggestion.title, color = Color(0xFF12204A), fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Text(suggestion.message, color = Color(0xFF64748B), fontSize = 10.sp, lineHeight = 15.sp)
            if (suggestion.severity == AiSeverity.HIGH_ALERT) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("High-alert pathway: AI does not calculate or authorize the dose.", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            suggestion.actions.firstOrNull()?.let { route ->
                val label = when (route) {
                    "emergency" -> "Open Emergency"
                    "icu" -> "Open ICU"
                    "dosage" -> "Open Dosage"
                    "iv" -> "Open IV Drip"
                    "high" -> "Open High-Alert"
                    "weight" -> "Open Weight & Infusions"
                    "convert" -> "Open Conversions"
                    "bsa" -> "Open BSA"
                    "pediatric" -> "Open Paediatric"
                    else -> "Open tool"
                }
                Surface(
                    color = accent,
                    shape = RoundedCornerShape(12.dp),
                    onClick = { onOpen(route) }
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
