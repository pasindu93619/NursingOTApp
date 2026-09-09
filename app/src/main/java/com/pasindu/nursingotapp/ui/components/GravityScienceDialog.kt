package com.pasindu.nursingotapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun GravityScienceDialog(dropsPerMinute: Int, onDismiss: () -> Unit) {
    val status = when {
        dropsPerMinute <= 0 -> Color.Gray to "Enter volume and time to see the rate guidance."
        dropsPerMinute < 20 -> Color(0xFFFFB74D) to "Slow drip: individual drops are easy to see, but small clamp changes can have a large percentage effect."
        dropsPerMinute <= 80 -> Color(0xFF2E9D70) to "Practical visual range: a regular drip rhythm is easier to follow consistently."
        dropsPerMinute <= 120 -> Color(0xFFFFB74D) to "Fast drip: visual timing becomes harder. Recheck the prescribed rate and consider a pump when appropriate."
        else -> Color(0xFFE53935) to "Very fast drip: visual counting becomes difficult. Follow the prescribed method and local protocol."
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.86f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).background(
                            brush = Brush.linearGradient(listOf(Color(0xFF1769E8), Color(0xFF7B5CEB))),
                            shape = RoundedCornerShape(14.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) { Text("💧", fontSize = 23.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Gravity Drip Science", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        Text("Understand the deterministic calculation", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }

                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFEAF6FF)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("Core formula", fontSize = 12.sp, color = Color(0xFF1769E8), fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(6.dp))
                        Text("drops/min = (volume mL × drop factor) ÷ total minutes", fontSize = 16.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("The calculator only uses the values entered on this screen. Always verify the prescribed rate and the drop factor printed on the giving set.", fontSize = 12.sp, color = Color(0xFF475569), lineHeight = 18.sp)
                    }
                }

                Text("Why drop factor matters", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                Text("Macrodrip and microdrip sets produce different numbers of drops for the same volume. The printed drop factor is therefore a required input, not an assumption.", fontSize = 13.sp, color = Color(0xFF475569), lineHeight = 20.sp)

                Text("Current calculated rate", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(if (dropsPerMinute > 0) dropsPerMinute.toString() else "0", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFF1769E8))
                    Spacer(Modifier.width(8.dp))
                    Text("drops/min", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 8.dp))
                }

                val (statusColor, statusText) = status
                Row(
                    modifier = Modifier.fillMaxWidth().background(statusColor.copy(alpha = 0.10f), RoundedCornerShape(16.dp)).border(1.dp, statusColor.copy(alpha = 0.28f), RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(if (dropsPerMinute in 20..80) "✓" else "!", color = statusColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                }

                HorizontalDivider()
                Text("Safety reminder", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                Text("This screen is a deterministic calculator and visual aid. It does not replace the prescription, institutional protocol, or clinical assessment. Verify the bag volume, time, drop factor and required rate before administration.", fontSize = 13.sp, color = Color(0xFF475569), lineHeight = 20.sp)

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1769E8))) {
                    Text("Close", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
