package com.pasindu.nursingotapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ElectrolyteReplacementCard(modifier: Modifier = Modifier) {
    // Shared App Palette (Mandate: Cool Tech Blue & Crisp Slate-White)
    val techBlue = Color(0xFF1976D2)
    val slateWhite = Color(0xFFF8F9FA)
    val alertCrimson = Color(0xFFD32F2F)
    val warningAmber = Color(0xFFF57C00)

    // Electrolyte Specific Colors
    val potassiumColor = Color(0xFF8E24AA) // Deep Purple for K+
    val magnesiumColor = Color(0xFF00ACC1) // Cyan for Mg2+

    // State Variables
    var isPotassium by remember { mutableStateOf(true) } // true = KCl, false = MgSO4
    var doseInput by remember { mutableStateOf("") }
    var volumeInput by remember { mutableStateOf("100") } // Standard 100ml NS diluent
    var timeHoursInput by remember { mutableStateOf("1") }

    // Math Logic
    val dose = doseInput.toFloatOrNull() ?: 0f
    val volume = volumeInput.toFloatOrNull() ?: 0f
    val timeHours = timeHoursInput.toFloatOrNull() ?: 0f

    val rateMlHr = if (timeHours > 0f) volume / timeHours else 0f
    val dosePerHour = if (timeHours > 0f) dose / timeHours else 0f

    // Safety Engine (Standard ICU Protocols)
    var showWarning = false
    var warningText = ""
    var warningColor = warningAmber

    if (isPotassium && dosePerHour > 0f) {
        if (dosePerHour > 20f) {
            showWarning = true
            warningText = "CRITICAL: >20 mEq/hr! Usually requires Central Line (CVP) and continuous ECG."
            warningColor = alertCrimson
        } else if (dosePerHour > 10f) {
            showWarning = true
            warningText = "WARNING: >10 mEq/hr. Ensure large peripheral vein and ECG monitoring. Burns likely."
            warningColor = warningAmber
        }
    } else if (!isPotassium && dosePerHour > 0f) {
        if (dosePerHour > 2f) { // Usually max 1-2g per hour for non-eclampsia
            showWarning = true
            warningText = "HIGH RATE: Rapid Mg push can cause hypotension and respiratory depression."
            warningColor = warningAmber
        }
    }

    val activeColor = if (isPotassium) potassiumColor else magnesiumColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = activeColor.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(slateWhite)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ElectricBolt,
                    contentDescription = "Electrolytes",
                    tint = activeColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Electrolyte Replacement",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = techBlue
                    )
                    Text(
                        if (isPotassium) "Potassium Chloride (KCl) Engine" else "Magnesium Sulfate (MgSO4) Engine",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // Custom Segmented Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPotassium) potassiumColor else Color.Transparent)
                        .clickable { isPotassium = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Potassium (K+)", color = if (isPotassium) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isPotassium) magnesiumColor else Color.Transparent)
                        .clickable { isPotassium = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Magnesium (Mg2+)", color = if (!isPotassium) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold)
                }
            }

            // Input Fields
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = doseInput,
                    onValueChange = { doseInput = it },
                    label = { Text(if (isPotassium) "Dose (mEq)" else "Dose (grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = activeColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                    singleLine = true
                )
                OutlinedTextField(
                    value = volumeInput,
                    onValueChange = { volumeInput = it },
                    label = { Text("Diluent (mL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = activeColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = timeHoursInput,
                onValueChange = { timeHoursInput = it },
                label = { Text("Duration (Hours)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = activeColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                singleLine = true
            )

            // Dynamic Safety Warning
            AnimatedVisibility(visible = showWarning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(warningColor.copy(alpha = 0.1f))
                        .border(1.dp, warningColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = "Warning", tint = warningColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(warningText, color = warningColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                }
            }

            Divider(color = Color(0xFFE2E8F0), thickness = 2.dp)

            // Result & Bio-Electric Animation Layer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A), // Deep monitor background
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TARGET RATE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (rateMlHr > 0f) String.format("%.1f", rateMlHr) else "0.0",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text("mL/hr", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = activeColor)

                        if (dosePerHour > 0f) {
                            Text(
                                text = String.format("Speed: %.1f %s/hr", dosePerHour, if (isPotassium) "mEq" else "g"),
                                fontSize = 13.sp,
                                color = activeColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Mind-Blowing Animation
                    if (rateMlHr > 0f) {
                        BioElectricIonAnimation(color = activeColor, speedMultiplier = (rateMlHr / 50f).coerceIn(0.5f, 3f))
                    } else {
                        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(60.dp)) {
                                drawCircle(color = Color.DarkGray, style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BIO-ELECTRIC ION REACTOR ANIMATION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BioElectricIonAnimation(color: Color, speedMultiplier: Float) {
    val transition = rememberInfiniteTransition(label = "ion_reactor")

    // The core pulse effect
    val corePulse by transition.animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween((1000 / speedMultiplier).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "core"
    )

    // The orbital rotation of the ions
    val rotationAngle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween((3000 / speedMultiplier).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "orbit"
    )

    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2

            // 1. Glowing Reactor Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.8f * corePulse), Color.Transparent),
                    center = center,
                    radius = maxRadius * 0.8f * corePulse
                ),
                radius = maxRadius * 0.8f * corePulse,
                center = center
            )
            drawCircle(color = Color.White, radius = maxRadius * 0.15f, center = center)

            // 2. Orbital Tracks
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = maxRadius * 0.65f,
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            )
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = maxRadius * 0.9f,
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Orbiting Ions (Electrons/Cations)
            val ionRadius1 = maxRadius * 0.65f
            val ionRadius2 = maxRadius * 0.9f

            // Inner orbit ion
            val innerAngle = Math.toRadians(rotationAngle.toDouble())
            val innerIon = Offset(
                x = center.x + (ionRadius1 * cos(innerAngle)).toFloat(),
                y = center.y + (ionRadius1 * sin(innerAngle)).toFloat()
            )
            drawCircle(color = color, radius = 6.dp.toPx(), center = innerIon)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = innerIon)

            // Outer orbit ion (Spins opposite direction)
            val outerAngle = Math.toRadians((-rotationAngle * 0.7f).toDouble())
            val outerIon = Offset(
                x = center.x + (ionRadius2 * cos(outerAngle)).toFloat(),
                y = center.y + (ionRadius2 * sin(outerAngle)).toFloat()
            )
            drawCircle(color = color, radius = 4.dp.toPx(), center = outerIon)
            drawCircle(color = Color.White, radius = 1.dp.toPx(), center = outerIon)
        }
    }
}