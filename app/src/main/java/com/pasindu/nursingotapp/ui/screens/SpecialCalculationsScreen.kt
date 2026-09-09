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
fun SpecialCalculationsScreen(initialMode: SpecialMode = SpecialMode.INSULIN) {
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var currentMode by remember(initialMode) { mutableStateOf(initialMode) }

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

    LaunchedEffect(slidingScaleUnits, insIvRate, hepRateMlHr, pcaMaxLimit) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

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
                SpecialMode.PCA -> listOf(pcaMaxLimit.toString(), "Limit/hr", "THEORETICAL LOCKOUT CAPACITY ($pcaMaxDoses doses)", pcaMaxLimit > 0f)
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

    Canvas(modifier = Modifier.size(160.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 3
        val pulse = (sin(phase) + 1f) / 2f
        drawCircle(color = mode.themeColor.copy(alpha = 0.10f + pulse * 0.10f), radius = radius + pulse * 20f, center = center)
        drawCircle(color = mode.themeColor.copy(alpha = 0.18f), radius = radius, center = center, style = Stroke(width = 4f))
        drawCircle(color = mode.themeColor, radius = 18f, center = center)
    }
}

@Composable
fun SpecialClinicalGuideDialog(mode: SpecialMode, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth().padding(20.dp), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text("${mode.title} clinical guide", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = mode.themeColor)
                Spacer(Modifier.height(10.dp))
                Text(
                    when (mode) {
                        SpecialMode.INSULIN -> "Use the prescribed insulin order and institution-specific glucose protocol. Verify units, concentration, timing and patient-specific targets before administration."
                        SpecialMode.HEPARIN -> "Use the prescribed heparin order and institutional titration protocol. Verify patient weight, concentration, units/hr and pump settings independently."
                        SpecialMode.PCA -> "PCA settings are order- and policy-dependent. Lockout-derived capacity is not a patient-specific maximum dose. Verify basal rate, loading dose, one-hour/four-hour limits and all other programmed safeguards."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF475467),
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(16.dp))
                Text("CLOSE", modifier = Modifier.clickable(onClick = onDismiss), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mode.themeColor)
            }
        }
    }
}
