package com.pasindu.nursingotapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
fun InfusionGuideDialog(mode: InfusionMode, onDismiss: () -> Unit) {
    val accent = if (mode == InfusionMode.GRAVITY) Color(0xFF1769E8) else Color(0xFF7B5CEB)
    val title = if (mode == InfusionMode.GRAVITY) "Gravity Drip Guide" else "IV Pump Guide"

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
                            Brush.linearGradient(listOf(Color(0xFF1769E8), Color(0xFF7B5CEB))),
                            RoundedCornerShape(14.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) { Text("ℹ️", fontSize = 22.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        Text("Quick checks before administration", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }

                GuideStep(1, "Confirm the prescription", "Check the prescribed volume, duration, concentration or dose, and route before entering any value.", accent)
                GuideStep(2, "Confirm the equipment", "For gravity infusions, read the drop factor printed on the giving set. For a pump, confirm the programmed units and concentration.", accent)
                GuideStep(3, "Enter values exactly", "Use the same units shown by the field labels. Do not estimate missing values or substitute a different concentration.", accent)
                GuideStep(4, "Review the result", "Compare the calculated result with the intended prescription and local clinical protocol before starting or changing an infusion.", accent)

                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF6E7)) {
                    Text(
                        "Safety: this calculator supports arithmetic and workflow checks. It does not determine whether an infusion is clinically indicated or replace an independent medication check.",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = Color(0xFF7A4A00),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Start
                    )
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("Close guide", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun GuideStep(number: Int, title: String, body: String, accent: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(32.dp).background(accent.copy(alpha = 0.12f), CircleShape).border(1.dp, accent.copy(alpha = 0.35f), CircleShape), contentAlignment = Alignment.Center) {
            Text(number.toString(), color = accent, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color(0xFF0F172A), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(body, color = Color(0xFF475569), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
