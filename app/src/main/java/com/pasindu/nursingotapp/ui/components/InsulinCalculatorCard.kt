package com.pasindu.nursingotapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsulinCalculatorCard(modifier: Modifier = Modifier) {
    // Shared App Palette
    val TechBluePrimary = Color(0xFF0277BD)
    val TechBlueLight = Color(0xFFE1F5FE)
    val BgSlateWhite = Color(0xFFF4F7FA)
    val AlertCrimson = Color(0xFFD32F2F)
    val WarningAmber = Color(0xFFF57C00)
    val SafeGreen = Color(0xFF388E3C)

    // State Variables
    var cbgInput by remember { mutableStateOf("") }
    var totalUnitsInput by remember { mutableStateOf("50") } // Default 50 Units Actrapid
    var totalVolumeInput by remember { mutableStateOf("50") } // Default 50 mL NS

    val cbg = cbgInput.toFloatOrNull() ?: 0f
    val units = totalUnitsInput.toFloatOrNull() ?: 50f
    val volume = totalVolumeInput.toFloatOrNull() ?: 50f

    // Standard DKA / Sliding Scale Logic (Example Protocol)
    val recommendedUnitsPerHour = when {
        cbg < 70 -> 0f // Hypoglycemia - STOP
        cbg in 70f..140f -> 0f // Target Range
        cbg in 141f..180f -> 2f
        cbg in 181f..250f -> 4f
        cbg in 251f..300f -> 6f
        cbg > 300f -> 8f
        else -> 0f
    }

    val concentration = if (volume > 0) units / volume else 0f // Units per mL
    val rateMlHr = if (concentration > 0) recommendedUnitsPerHour / concentration else 0f

    // Color logic based on CBG level
    val statusColor = when {
        cbg == 0f -> TechBluePrimary
        cbg < 70 -> AlertCrimson
        cbg in 70f..140f -> SafeGreen
        cbg in 141f..250f -> WarningAmber
        else -> AlertCrimson
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = statusColor.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.White, BgSlateWhite)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bloodtype, contentDescription = "Insulin", tint = statusColor, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("IV Insulin Protocol", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TechBluePrimary)
                    Text("Continuous Drip & Sliding Scale", fontSize = 13.sp, color = Color.Gray)
                }
            }

            // Input Fields
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cbgInput,
                    onValueChange = { cbgInput = it },
                    label = { Text("Blood Glucose (mg/dL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = statusColor,
                        focusedLabelColor = statusColor,
                        focusedContainerColor = statusColor.copy(alpha = 0.05f)
                    ),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = totalUnitsInput,
                    onValueChange = { totalUnitsInput = it },
                    label = { Text("Actrapid (Units)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = totalVolumeInput,
                    onValueChange = { totalVolumeInput = it },
                    label = { Text("Diluent (mL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dashboard & Animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A)) // Dark Tech Background for contrast
                    .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Data Column
                    Column {
                        Text("TARGET DOSE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (cbg > 0) "${recommendedUnitsPerHour.toInt()} Units/hr" else "--",
                            color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("PUMP RATE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (cbg > 0) "${"%.1f".format(rateMlHr)} mL/hr" else "--",
                            color = statusColor, fontSize = 32.sp, fontWeight = FontWeight.Black
                        )

                        if (cbg < 70 && cbg > 0) {
                            Text("⚠️ HYPOGLYCEMIA - STOP PUMP", color = AlertCrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    // Right Animation (Glucose Dial)
                    GlucoseDialAnimation(cbg = cbg, statusColor = statusColor)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GLUCOSE SPEEDOMETER & FLUID DIAL ANIMATION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlucoseDialAnimation(cbg: Float, statusColor: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")

    // Heartbeat/Warning Pulse Effect
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (cbg < 70 || cbg > 300) 400 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Needle rotation (70 mg/dl = -90 deg, 300 mg/dl = +90 deg)
    val targetAngle = when {
        cbg == 0f -> -90f
        cbg <= 70f -> -90f
        cbg >= 300f -> 90f
        else -> -90f + ((cbg - 70f) / (300f - 70f) * 180f)
    }

    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "needle"
    )

    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2f
            val arcStroke = 8.dp.toPx()

            // Outer Glow
            drawCircle(
                color = statusColor.copy(alpha = 0.15f * pulse),
                radius = radius * 1.2f,
                center = center
            )

            // Dial Background Arc
            drawArc(
                color = Color.DarkGray,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = arcStroke, cap = StrokeCap.Round)
            )

            // Colored Zones (Red-Low, Green-Normal, Yellow-High, Red-Critical)
            val safeSweep = 180f * ((140f - 70f) / (300f - 70f))
            val warnSweep = 180f * ((250f - 140f) / (300f - 70f))
            val critSweep = 180f - safeSweep - warnSweep

            // Safe Zone (Green)
            drawArc(
                color = Color(0xFF388E3C),
                startAngle = 180f,
                sweepAngle = safeSweep,
                useCenter = false,
                style = Stroke(width = arcStroke, cap = StrokeCap.Round)
            )
            // Warning Zone (Amber)
            drawArc(
                color = Color(0xFFF57C00),
                startAngle = 180f + safeSweep,
                sweepAngle = warnSweep,
                useCenter = false,
                style = Stroke(width = arcStroke, cap = StrokeCap.Round)
            )
            // Critical Zone (Red)
            drawArc(
                color = Color(0xFFD32F2F),
                startAngle = 180f + safeSweep + warnSweep,
                sweepAngle = critSweep,
                useCenter = false,
                style = Stroke(width = arcStroke, cap = StrokeCap.Round)
            )

            // Needle Pointer
            val needleLen = radius * 0.75f
            val angleRad = Math.toRadians((animatedAngle - 90).toDouble())
            val needleEnd = Offset(
                x = center.x + (needleLen * cos(angleRad)).toFloat(),
                y = center.y + (needleLen * sin(angleRad)).toFloat()
            )

            drawLine(
                color = Color.White,
                start = center,
                end = needleEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Center Pin
            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = center)
            drawCircle(color = statusColor, radius = 3.dp.toPx(), center = center)
        }
    }
}