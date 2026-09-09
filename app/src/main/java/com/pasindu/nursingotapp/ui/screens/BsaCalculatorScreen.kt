package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.sqrt

private val BsaBg = Color(0xFFF8FAFC)
private val BsaInk = Color(0xFF12204A)
private val BsaSlate = Color(0xFF64748B)
private val BsaBlue = Color(0xFF1769E8)
private val BsaPurple = Color(0xFF7B5CEB)
private val BsaBlueSoft = Color(0xFFEAF6FF)
private val BsaMintSoft = Color(0xFFEAFBF5)
private val BsaAmberSoft = Color(0xFFFFF6E7)
private val BsaHeroGradient = Brush.horizontalGradient(
    listOf(Color(0xFF1769E8), Color(0xFF149FE3), Color(0xFF4B78F2), Color(0xFF7B5CEB))
)

@Composable
fun BsaCalculatorScreen() {
    val haptic = LocalHapticFeedback.current
    var showGuide by remember { mutableStateOf(false) }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var orderedDoseMgM2 by remember { mutableStateOf("") }

    val height = heightCm.toDoubleOrNull() ?: 0.0
    val weight = weightKg.toDoubleOrNull() ?: 0.0
    val dosePerM2 = orderedDoseMgM2.toDoubleOrNull() ?: 0.0

    // Existing clinical math preserved: Mosteller BSA, then BSA x ordered mg/m².
    val calculatedBsa = remember(heightCm, weightKg) {
        if (height > 0.0 && weight > 0.0) sqrt((height * weight) / 3600.0) else 0.0
    }
    val totalDoseMg = remember(calculatedBsa, orderedDoseMgM2) {
        if (calculatedBsa > 0.0 && dosePerM2 > 0.0) calculatedBsa * dosePerM2 else 0.0
    }

    if (showGuide) BsaClinicalGuideDialog(onDismiss = { showGuide = false })

    Column(
        Modifier
            .fillMaxSize()
            .background(BsaBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .background(BsaHeroGradient, RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("CLINICAL TOOL", color = Color.White.copy(alpha = 0.76f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("BSA & Chemo Dose", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text("Mosteller body surface area with optional mg/m² dose calculation.", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(999.dp)) {
                        Text(if (calculatedBsa > 0.0) "●  CALCULATION READY" else "○  ENTER PATIENT DATA", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), letterSpacing = 0.7.sp)
                    }
                }
                Surface(color = Color.White.copy(alpha = 0.16f), shape = CircleShape) {
                    Text("📐", fontSize = 26.sp, modifier = Modifier.padding(11.dp))
                }
            }
        }

        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                BsaModeChip("📐  BSA", true, Modifier.weight(1f))
                Surface(
                    Modifier.size(46.dp).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showGuide = true },
                    color = BsaBlueSoft,
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Info, contentDescription = "Clinical guide", tint = BsaBlue) }
                }
            }
        }

        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Patient measurements", color = BsaInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("Use current height and weight in the stated units.", color = BsaSlate, fontSize = 11.sp)
                    }
                    Surface(color = BsaBlueSoft, shape = RoundedCornerShape(10.dp)) {
                        Text("STEP 01", color = BsaBlue, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BsaInput("Height", "cm", heightCm, { heightCm = it }, Modifier.weight(1f))
                    BsaInput("Weight", "kg", weightKg, { weightKg = it }, Modifier.weight(1f))
                }
            }
        }

        Surface(color = BsaBlueSoft, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BODY SURFACE AREA", color = BsaBlue, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(3.dp))
                if (calculatedBsa > 0.0) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(formatBsa(calculatedBsa), color = BsaInk, fontSize = 48.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(7.dp))
                        Text("m²", color = BsaBlue, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text("√(height × weight ÷ 3600)", color = BsaSlate, fontSize = 11.sp)
                } else {
                    Text("—", color = BsaInk, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text("Enter height and weight to calculate", color = BsaSlate, fontSize = 11.sp)
                }
            }
        }

        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Optional chemotherapy dose", color = BsaInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("Enter only when the prescription is expressed in mg/m².", color = BsaSlate, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    Surface(color = BsaMintSoft, shape = RoundedCornerShape(10.dp)) {
                        Text("STEP 02", color = Color(0xFF087F5B), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                    }
                }
                BsaInput("Ordered dose", "mg/m²", orderedDoseMgM2, { orderedDoseMgM2 = it })
            }
        }

        if (totalDoseMg > 0.0) {
            Surface(color = BsaMintSoft, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL TARGET DOSE", color = Color(0xFF087F5B), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(formatDose(totalDoseMg), color = BsaInk, fontSize = 44.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(7.dp))
                        Text("mg", color = Color(0xFF087F5B), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 7.dp))
                    }
                    Text("BSA × prescribed mg/m²", color = BsaSlate, fontSize = 11.sp)
                }
            }
        }

        Surface(color = BsaAmberSoft, shape = RoundedCornerShape(18.dp)) {
            Text("⚠  Calculation support only. BSA formula choice, chemotherapy dosing limits, dose caps, renal/hepatic adjustments and rounding must follow the actual prescription and local oncology protocol.", Modifier.fillMaxWidth().padding(14.dp), color = Color(0xFF7A4A00), fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BsaModeChip(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier.height(46.dp), color = if (selected) BsaBlue else BsaBg, shape = RoundedCornerShape(15.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = if (selected) Color.White else BsaSlate, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun BsaInput(label: String, unit: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text(unit, color = BsaBlue, fontWeight = FontWeight.Bold) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun BsaClinicalGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.78f), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("📐  BSA clinical guide", color = BsaInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("Mosteller formula", color = BsaBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Surface(color = BsaBlueSoft, shape = RoundedCornerShape(16.dp)) {
                    Text("BSA (m²) = √[(height in cm × weight in kg) ÷ 3600]", Modifier.padding(15.dp), color = BsaInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("The current calculator retains the existing Mosteller method and calculates a target drug dose only when a prescription is entered in mg/m².", color = BsaSlate, fontSize = 14.sp, lineHeight = 20.sp)
                Surface(color = BsaAmberSoft, shape = RoundedCornerShape(16.dp)) {
                    Text("Do not use this calculator to select a drug, regimen, dose cap, renal/hepatic adjustment or patient-specific treatment plan. Verify the prescription and institutional protocol independently.", Modifier.padding(14.dp), color = Color(0xFF7A4A00), fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = BsaBlue)) {
                    Text("Close", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

private fun formatBsa(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
private fun formatDose(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
