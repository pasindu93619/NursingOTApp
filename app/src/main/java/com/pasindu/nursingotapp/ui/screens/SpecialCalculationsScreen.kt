package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val ThemeRuby = Color(0xFFD32F2F)
val ThemeSlate = Color(0xFF455A64)
val ThemeInsulinBlue = Color(0xFF0288D1)
val ThemePCAPurple = Color(0xFF8E24AA)

enum class SpecialMode(val title: String, val emoji: String, val themeColor: Color) {
    INSULIN("Insulin", "💉", ThemeInsulinBlue),
    HEPARIN("Heparin", "🩸", ThemeRuby),
    PCA("PCA & Opioids", "🔒", ThemePCAPurple)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialCalculationsScreen() {
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(SpecialMode.INSULIN) }

    // --- INSULIN STATE ---
    var bgLevel by remember { mutableStateOf("") }
    var insWeight by remember { mutableStateOf("") }
    var insUkghr by remember { mutableStateOf("") }
    var insUml by remember { mutableStateOf("") }

    // --- HEPARIN STATE ---
    var hepWeight by remember { mutableStateOf("") }
    var hepUkghr by remember { mutableStateOf("") }
    var hepBagUnits by remember { mutableStateOf("") }
    var hepBagMl by remember { mutableStateOf("") }

    // --- PCA STATE ---
    var pcaBolusDose by remember { mutableStateOf("") }
    var pcaLockoutMins by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { delay(100); isVisible = true }

    // --- ENGINES ---
    val slidingScaleUnits = remember(bgLevel) {
        val bg = bgLevel.toFloatOrNull() ?: 0f
        if (bg > 100f) Math.round((bg - 100f) / 10f) else 0
    }

    val insIvRate = remember(insWeight, insUkghr, insUml) {
        val w = insWeight.toFloatOrNull() ?: 0f
        val dose = insUkghr.toFloatOrNull() ?: 0f
        val conc = insUml.toFloatOrNull() ?: 0f
        if (w > 0 && dose > 0 && conc > 0) {
            val uHr = w * dose
            (Math.round((uHr / conc) * 10.0) / 10.0).toFloat()
        } else 0f
    }

    val hepUnitsHr = remember(hepWeight, hepUkghr) {
        val w = hepWeight.toFloatOrNull() ?: 0f
        val dose = hepUkghr.toFloatOrNull() ?: 0f
        if (w > 0 && dose > 0) Math.round(w * dose).toFloat() else 0f
    }

    val hepRateMlHr = remember(hepUnitsHr, hepBagUnits, hepBagMl) {
        val units = hepBagUnits.toFloatOrNull() ?: 0f
        val vol = hepBagMl.toFloatOrNull() ?: 0f
        if (hepUnitsHr > 0 && units > 0 && vol > 0) {
            (Math.round((hepUnitsHr * vol / units) * 10.0) / 10.0).toFloat()
        } else 0f
    }

    val pcaMaxDoses = remember(pcaLockoutMins) {
        val lockout = pcaLockoutMins.toFloatOrNull() ?: 0f
        if (lockout > 0) Math.floor((60f / lockout).toDouble()).toInt() else 0
    }

    val pcaMaxLimit = remember(pcaMaxDoses, pcaBolusDose) {
        val bolus = pcaBolusDose.toFloatOrNull() ?: 0f
        if (pcaMaxDoses > 0 && bolus > 0) (Math.round((pcaMaxDoses * bolus) * 100.0) / 100.0).toFloat() else 0f
    }

    LaunchedEffect(slidingScaleUnits, insIvRate, hepRateMlHr, pcaMaxLimit) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    if (showGuideDialog) SpecialClinicalGuideDialog(currentMode, onDismiss = { showGuideDialog = false })

    val bgGradient = Brush.verticalGradient(listOf(currentMode.themeColor.copy(alpha = 0.15f), MaterialTheme.colorScheme.surface))

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            // HEADER
            AnimatedVisibility(visible = isVisible, enter = slideInVertically { -50 } + fadeIn()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(currentMode.themeColor, CircleShape).shadow(8.dp, CircleShape), contentAlignment = Alignment.Center) { Text(currentMode.emoji, fontSize = 24.sp) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("High-Alert Calcs", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = currentMode.themeColor)
                            Text("Critical Protocols", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(modifier = Modifier.size(42.dp).background(currentMode.themeColor.copy(alpha = 0.2f), CircleShape).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showGuideDialog = true }, contentAlignment = Alignment.Center) { Text("❓", fontSize = 18.sp) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // TABS
            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SpecialMode.values()) { mode ->
                    val isSelected = currentMode == mode
                    val bgColor by animateColorAsState(if (isSelected) mode.themeColor else Color.White.copy(alpha = 0.6f), label = "")
                    val textColor by animateColorAsState(if (isSelected) Color.White else Color.Gray, label = "")
                    Box(modifier = Modifier.height(40.dp).background(bgColor, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentMode = mode }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text("${mode.emoji} ${mode.title}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // DYNAMIC CANVAS GRAPHIC
            AnimatedVisibility(visible = isVisible, enter = scaleIn() + fadeIn()) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    AnimatedHighAlertGraphic(mode = currentMode)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // INPUT CARDS
            Card(modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                AnimatedContent(targetState = currentMode, label = "") { mode ->
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when (mode) {
                            SpecialMode.INSULIN -> {
                                Text("Subcutaneous Sliding Scale", fontSize = 14.sp, color = ThemeInsulinBlue, fontWeight = FontWeight.ExtraBold)
                                OutlinedTextField(value = bgLevel, onValueChange = { bgLevel = it }, label = { Text("Blood Glucose (mg/dL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                                if (slidingScaleUnits > 0) {
                                    Row(modifier = Modifier.fillMaxWidth().background(ThemeInsulinBlue.copy(alpha=0.1f), RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("💉", fontSize = 24.sp); Spacer(modifier = Modifier.width(16.dp))
                                        Column { Text("GIVE INSULIN:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("$slidingScaleUnits Units", color = ThemeInsulinBlue, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
                                    }
                                }

                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                                Text("Continuous IV Infusion", fontSize = 14.sp, color = ThemeInsulinBlue, fontWeight = FontWeight.ExtraBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(value = insWeight, onValueChange = { insWeight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                                    OutlinedTextField(value = insUkghr, onValueChange = { insUkghr = it }, label = { Text("Order (U/kg/hr)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                                }
                                OutlinedTextField(value = insUml, onValueChange = { insUml = it }, label = { Text("Concentration (U/mL)") }, placeholder = { Text("e.g. 1") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            }

                            SpecialMode.HEPARIN -> {
                                Text("Step 1: Patient Hourly Need", fontSize = 14.sp, color = ThemeRuby, fontWeight = FontWeight.ExtraBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(value = hepWeight, onValueChange = { hepWeight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                                    OutlinedTextField(value = hepUkghr, onValueChange = { hepUkghr = it }, label = { Text("Order (U/kg/hr)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                                }
                                if (hepUnitsHr > 0f) {
                                    Text("➡ Requires $hepUnitsHr Units/hr", color = ThemeRuby, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                                Text("Step 2: IV Pump Setup", fontSize = 14.sp, color = ThemeSlate, fontWeight = FontWeight.ExtraBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(value = hepBagUnits, onValueChange = { hepBagUnits = it }, label = { Text("Bag Units") }, placeholder = { Text("e.g. 25000") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                                    OutlinedTextField(value = hepBagMl, onValueChange = { hepBagMl = it }, label = { Text("Bag Vol (mL)") }, placeholder = { Text("e.g. 250") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                                }
                            }

                            SpecialMode.PCA -> {
                                Text("PCA Lockout Verification", fontSize = 14.sp, color = ThemePCAPurple, fontWeight = FontWeight.ExtraBold)
                                Text("Calculate the maximum hourly limit based on lockout time.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(value = pcaBolusDose, onValueChange = { pcaBolusDose = it }, label = { Text("Bolus Dose") }, placeholder = { Text("e.g. 1 mg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                OutlinedTextField(value = pcaLockoutMins, onValueChange = { pcaLockoutMins = it }, label = { Text("Lockout Interval (Mins)") }, placeholder = { Text("e.g. 5") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // DYNAMIC RESULT HUD
            val (resultVal, resultUnit, resultTitle, show) = when (currentMode) {
                SpecialMode.INSULIN -> listOf(insIvRate.toString(), "mL/hr", "IV INFUSION RATE", insIvRate > 0f)
                SpecialMode.HEPARIN -> listOf(hepRateMlHr.toString(), "mL/hr", "HEPARIN PUMP RATE", hepRateMlHr > 0f)
                SpecialMode.PCA -> listOf(pcaMaxLimit.toString(), "Limit/hr", "MAXIMUM ALLOWED ($pcaMaxDoses doses)", pcaMaxLimit > 0f)
            }

            if (show as Boolean) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(currentMode.themeColor, ThemeSlate)), RoundedCornerShape(20.dp)).padding(2.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(resultTitle as String, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(resultVal as String, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, color = currentMode.themeColor)
                                Text(" ${resultUnit as String}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = currentMode.themeColor.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp, start = 6.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun AnimatedHighAlertGraphic(mode: SpecialMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val phase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2 * PI.toFloat(), animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val center = Offset(w/2, h/2)

        when (mode) {
            SpecialMode.INSULIN -> {
                // Glucose Hexagons
                val hexRadius = 40f
                for (i in -1..1) {
                    val xOffset = center.x + (i * hexRadius * 1.8f)
                    val yOffset = center.y + sin(phase + i) * 20f

                    val path = Path().apply {
                        for (j in 0..5) {
                            val angle = j * (PI / 3) + (PI / 6)
                            val px = xOffset + hexRadius * cos(angle).toFloat()
                            val py = yOffset + hexRadius * sin(angle).toFloat()
                            if (j == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                    drawPath(path, color = ThemeInsulinBlue.copy(alpha = 0.4f), style = Stroke(width = 4f, cap = StrokeCap.Round))
                    drawCircle(color = ThemeInsulinBlue, radius = 8f, center = Offset(xOffset, yOffset))
                }
            }
            SpecialMode.HEPARIN -> {
                // Arterial Waveform - FIXED WITH PROPER PATH APPLY
                val path = Path().apply {
                    moveTo(0f, center.y)
                    for (x in 0..w.toInt() step 5) {
                        val nx = x.toFloat()
                        val y = center.y - (sin((nx / w) * 4 * PI + phase).toFloat() * 30f) - (if (x % 150 in 60..90) sin(phase) * 60f else 0f)
                        lineTo(nx, y)
                    }
                }
                drawPath(path, brush = Brush.horizontalGradient(listOf(Color.Transparent, ThemeRuby, Color.Transparent)), style = Stroke(width = 8f, cap = StrokeCap.Round))

                // Blood particles
                for (i in 0..2) {
                    val px = (w * ((phase / (2 * PI)) + (i * 0.3f))) % w
                    val py = center.y - (sin((px / w) * 4 * PI + phase).toFloat() * 30f)
                    drawCircle(color = ThemeRuby, radius = 12f, center = Offset(px.toFloat(), py))
                }
            }
            SpecialMode.PCA -> {
                // Security Lockout Shield
                val pulseRadius = 50f + (sin(phase) + 1f) * 20f

                // Shield body
                val shieldPath = Path().apply {
                    moveTo(center.x, center.y - 60f)
                    lineTo(center.x + 50f, center.y - 40f)
                    lineTo(center.x + 50f, center.y + 20f)
                    quadraticBezierTo(center.x + 50f, center.y + 60f, center.x, center.y + 80f)
                    quadraticBezierTo(center.x - 50f, center.y + 60f, center.x - 50f, center.y + 20f)
                    lineTo(center.x - 50f, center.y - 40f)
                    close()
                }
                drawPath(shieldPath, color = ThemePCAPurple.copy(alpha = 0.2f))
                drawPath(shieldPath, color = ThemePCAPurple, style = Stroke(width = 6f))

                // Pulsing rings indicating lockout time
                drawCircle(color = ThemePCAPurple.copy(alpha = 0.5f), radius = pulseRadius, center = center, style = Stroke(width = 4f))
                drawCircle(color = ThemePCAPurple.copy(alpha = 0.2f), radius = pulseRadius * 1.5f, center = center, style = Stroke(width = 2f))

                // Center keyhole
                drawCircle(color = ThemePCAPurple, radius = 10f, center = Offset(center.x, center.y - 10f))
                drawRoundRect(color = ThemePCAPurple, topLeft = Offset(center.x - 6f, center.y - 5f), size = Size(12f, 20f), cornerRadius = CornerRadius(4f, 4f))
            }
        }
    }
}

@Composable
fun SpecialClinicalGuideDialog(currentMode: SpecialMode, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            // FIXED: Added explicit background to prevent the dialog from showing a black background
            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentMode.emoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Clinical Guide", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(currentMode.title, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = currentMode.themeColor)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))

                when (currentMode) {
                    SpecialMode.INSULIN -> {
                        Text("International Units (IU)", fontWeight = FontWeight.ExtraBold, color = ThemeInsulinBlue, fontSize = 18.sp)
                        Text("1 IU of insulin ≈ 0.0347 mg. Always process dose orders as \"units\" and calculate fluids in units/mL.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.8f))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("1. Sliding Scale", fontWeight = FontWeight.ExtraBold, color = ThemeInsulinBlue, fontSize = 16.sp)
                        Box(modifier = Modifier.fillMaxWidth().background(ThemeInsulinBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Units to give = (BG − 100) ÷ 10", fontWeight = FontWeight.ExtraBold, color = ThemeInsulinBlue)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("2. Intravenous Infusion", fontWeight = FontWeight.ExtraBold, color = ThemeInsulinBlue, fontSize = 16.sp)
                        Text("Rate (mL/hr) = [ (Ordered U/kg/hr) × Weight (kg) ] ÷ Concentration (U/mL)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.8f), fontWeight = FontWeight.Bold)
                    }
                    SpecialMode.HEPARIN -> {
                        Text("Heparin Infusions", fontWeight = FontWeight.ExtraBold, color = ThemeRuby, fontSize = 18.sp)
                        Text("Heparin is completely weight-dependent and must be calculated in two precise steps.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.8f))
                        Spacer(modifier = Modifier.height(20.dp))

                        Text("Step 1: Total Units/hr", fontWeight = FontWeight.Bold, color = ThemeRuby)
                        Box(modifier = Modifier.fillMaxWidth().background(ThemeRuby.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Units/hr = (units/kg/hr) × weight (kg)", fontWeight = FontWeight.ExtraBold, color = ThemeRuby)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Step 2: Pump Rate (mL/hr)", fontWeight = FontWeight.Bold, color = ThemeRuby)
                        Box(modifier = Modifier.fillMaxWidth().background(ThemeRuby.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Rate (mL/hr) = (Units/hr × Bag Vol) ÷ Total Units in Bag", fontWeight = FontWeight.ExtraBold, color = ThemeRuby)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Titration Adjustments: If a nomogram dictates a change in dose (ΔU/kg/hr), you MUST recalculate the new total Units/hr from scratch and repeat the formula.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.8f))
                    }
                    SpecialMode.PCA -> {
                        Text("Patient-Controlled Analgesia (PCA)", fontWeight = FontWeight.ExtraBold, color = ThemePCAPurple, fontSize = 18.sp)
                        Text("Verifying a patient cannot exceed their safe maximum hourly limit.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.8f))
                        Spacer(modifier = Modifier.height(20.dp))

                        Text("1. Calculate Max Doses", fontWeight = FontWeight.Bold, color = ThemePCAPurple)
                        Box(modifier = Modifier.fillMaxWidth().background(ThemePCAPurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Max Doses/hr = 60 minutes ÷ Lockout Time", fontWeight = FontWeight.ExtraBold, color = ThemePCAPurple)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Example: A 5-minute lockout allows a max of 12 doses per hour (60/5). If the bolus is 1 mg, the maximum limit is 12 mg/h.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.8f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CRITICAL SAFETY BOX
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(16.dp)).border(2.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp)).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(Color(0xFFEF5350), CircleShape), contentAlignment = Alignment.Center) { Text("⚠️", fontSize = 16.sp, color = Color.White) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("HIGH-ALERT MEDICATION", fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828), fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• Insulin, Heparin, and Opioids are classified globally as High-Alert medications.\n• Errors in these calculations can be immediately fatal.\n• ALWAYS require an independent double-check by a second registered nurse before beginning or titrating these infusions.", fontSize = 13.sp, color = Color(0xFFB71C1C), lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = currentMode.themeColor)) { Text("Acknowledge Risk & Close", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
    }
}