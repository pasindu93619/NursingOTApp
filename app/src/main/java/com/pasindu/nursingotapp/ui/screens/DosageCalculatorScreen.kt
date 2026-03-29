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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Theme Colors
val ThemePurple = Color(0xFF6650a4)
val ThemePurpleLight = Color(0xFFD0BCFF)
val ThemePink = Color(0xFF7D5260)
val ThemePinkLight = Color(0xFFEFB8C8)
val ThemeBlue = Color(0xFF29B6F6)
val ThemeBlueLight = Color(0xFF81D4FA)
val ThemeTeal = Color(0xFF26A69A)
val ThemeTealLight = Color(0xFF80CBC4)

enum class CalcMode(val title: String, val emoji: String) {
    STANDARD("Standard", "💉"),
    WEIGHT("Mg/Kg", "⚖️"),
    PERCENTAGE("% to mg", "💧"),
    DILUTION("Dilute", "🧪"),
    RECONSTITUTE("Powder", "🫙")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DosageCalculatorScreen() {
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(CalcMode.STANDARD) }

    // Inputs
    var orderedDose by remember { mutableStateOf("") }
    var availableDose by remember { mutableStateOf("") }
    var availableVolume by remember { mutableStateOf("") }

    // Weight Specific
    var patientWeight by remember { mutableStateOf("") }
    var dosePerKg by remember { mutableStateOf("") }

    // Dilution
    var targetConc by remember { mutableStateOf("") }
    var targetVol by remember { mutableStateOf("") }
    var stockConc by remember { mutableStateOf("") }

    // Percentage
    var percentValue by remember { mutableStateOf("") }
    var percentTotalVol by remember { mutableStateOf("") }

    // Reconstitution
    var vialPowderMg by remember { mutableStateOf("") }
    var diluentAddedMl by remember { mutableStateOf("") }
    var reconOrderedDose by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { delay(100); isVisible = true }

    // --- MATH ENGINES ---

    // 1. Standard Math
    val calculatedMedVolume = remember(currentMode, orderedDose, availableDose, availableVolume, patientWeight, dosePerKg) {
        val have = availableDose.toFloatOrNull() ?: 0f
        val vol = availableVolume.toFloatOrNull() ?: 0f
        var desired = orderedDose.toFloatOrNull() ?: 0f
        if (currentMode == CalcMode.WEIGHT) desired = (patientWeight.toFloatOrNull() ?: 0f) * (dosePerKg.toFloatOrNull() ?: 0f)

        if (have > 0f && (currentMode == CalcMode.STANDARD || currentMode == CalcMode.WEIGHT)) {
            val result = (desired / have) * vol
            (Math.round(result * 100.0) / 100.0).toFloat()
        } else 0f
    }

    // 2. Weight Math (Target Dose)
    val calculatedTargetDoseMg = remember(currentMode, patientWeight, dosePerKg) {
        if (currentMode == CalcMode.WEIGHT) {
            val weight = patientWeight.toFloatOrNull() ?: 0f
            val mgKg = dosePerKg.toFloatOrNull() ?: 0f
            (Math.round((weight * mgKg) * 100.0) / 100.0).toFloat()
        } else 0f
    }

    // 3. Percentage Math
    val percentMgPerMl = remember(percentValue, currentMode) {
        if (currentMode == CalcMode.PERCENTAGE) {
            val p = percentValue.toFloatOrNull() ?: 0f
            (Math.round((p * 10) * 100.0) / 100.0).toFloat()
        } else 0f
    }

    val percentTotalGrams = remember(percentMgPerMl, percentTotalVol) {
        val vol = percentTotalVol.toFloatOrNull() ?: 0f
        if (vol > 0) {
            val totalMg = percentMgPerMl * vol
            (Math.round((totalMg / 1000) * 100.0) / 100.0).toFloat()
        } else 0f
    }

    // 4. Dilution Math
    var stockVolToDraw by remember { mutableFloatStateOf(0f) }
    var diluentVolToAdd by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentMode, targetConc, targetVol, stockConc) {
        if (currentMode == CalcMode.DILUTION) {
            val c2 = targetConc.toFloatOrNull() ?: 0f
            val v2 = targetVol.toFloatOrNull() ?: 0f
            val c1 = stockConc.toFloatOrNull() ?: 0f
            if (c1 > 0f && c1 >= c2) {
                stockVolToDraw = ((c2 * v2) / c1).let { Math.round(it * 100.0) / 100.0 }.toFloat()
                diluentVolToAdd = (v2 - stockVolToDraw).let { Math.round(it * 100.0) / 100.0 }.toFloat()
            } else { stockVolToDraw = 0f; diluentVolToAdd = 0f }
        }
    }

    // 5. Reconstitution Math
    var reconConcMgMl by remember { mutableFloatStateOf(0f) }
    var reconDrawMl by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentMode, vialPowderMg, diluentAddedMl, reconOrderedDose) {
        if (currentMode == CalcMode.RECONSTITUTE) {
            val powder = vialPowderMg.toFloatOrNull() ?: 0f
            val diluent = diluentAddedMl.toFloatOrNull() ?: 0f
            val ordered = reconOrderedDose.toFloatOrNull() ?: 0f

            if (diluent > 0f) {
                reconConcMgMl = (powder / diluent).let { Math.round(it * 100.0) / 100.0 }.toFloat()
                if (reconConcMgMl > 0f) {
                    reconDrawMl = (ordered / reconConcMgMl).let { Math.round(it * 100.0) / 100.0 }.toFloat()
                } else reconDrawMl = 0f
            } else { reconConcMgMl = 0f; reconDrawMl = 0f }
        }
    }

    // Haptics Trigger
    LaunchedEffect(calculatedMedVolume, stockVolToDraw, percentMgPerMl, reconDrawMl) {
        if (calculatedMedVolume > 0 || stockVolToDraw > 0 || percentMgPerMl > 0 || reconDrawMl > 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Dynamic Scale
    val dynamicMaxVolume = remember(calculatedMedVolume, stockVolToDraw, diluentVolToAdd, reconDrawMl, currentMode) {
        val totalVol = when (currentMode) {
            CalcMode.DILUTION -> (stockVolToDraw + diluentVolToAdd)
            CalcMode.RECONSTITUTE -> reconDrawMl
            else -> calculatedMedVolume
        }
        when {
            totalVol <= 0f -> 10f
            totalVol <= 3f -> 3f
            totalVol <= 5f -> 5f
            totalVol <= 10f -> 10f
            totalVol <= 20f -> 20f
            totalVol <= 30f -> 30f
            totalVol <= 50f -> 50f
            else -> ((totalVol / 50).toInt() + 1) * 50f
        }
    }

    if (showGuideDialog) ClinicalMathGuideDialog(currentMode = currentMode, onDismiss = { showGuideDialog = false })

    val bgGradient = Brush.verticalGradient(listOf(ThemePurpleLight.copy(alpha = 0.3f), MaterialTheme.colorScheme.surface))

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // HEADER
            AnimatedVisibility(visible = isVisible, enter = slideInVertically { -50 } + fadeIn()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(ThemePurple, CircleShape).shadow(8.dp, CircleShape), contentAlignment = Alignment.Center) { Text("🏥", fontSize = 24.sp) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Advanced Dosage", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ThemePurple)
                            Text("5-in-1 Math Engine", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(modifier = Modifier.size(42.dp).background(ThemePurpleLight.copy(alpha = 0.5f), CircleShape).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showGuideDialog = true }, contentAlignment = Alignment.Center) { Text("❓", fontSize = 18.sp) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // HORIZONTAL SCROLLING TABS
            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CalcMode.values()) { mode ->
                    val isSelected = currentMode == mode
                    val bgColor by animateColorAsState(if (isSelected) ThemePurple else Color.White.copy(alpha = 0.6f), label = "")
                    val textColor by animateColorAsState(if (isSelected) Color.White else Color.Gray, label = "")
                    Box(
                        modifier = Modifier.height(40.dp).background(bgColor, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentMode = mode }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("${mode.emoji} ${mode.title}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // HERO GRAPHICS
            AnimatedVisibility(visible = isVisible, enter = scaleIn(spring(0.6f)) + fadeIn()) {
                Box(modifier = Modifier.height(240.dp), contentAlignment = Alignment.Center) {
                    AnimatedContent(targetState = currentMode, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "") { mode ->
                        when (mode) {
                            CalcMode.STANDARD, CalcMode.WEIGHT -> AnimatedSyringeGraphic(calculatedMedVolume, dynamicMaxVolume)
                            CalcMode.DILUTION -> AnimatedMultiLiquidSyringe(stockVolToDraw, diluentVolToAdd, dynamicMaxVolume)
                            CalcMode.PERCENTAGE -> AnimatedIVBagGraphic(percentMgPerMl)
                            CalcMode.RECONSTITUTE -> AnimatedVialGraphic(reconDrawMl, dynamicMaxVolume, diluentAddedMl.toFloatOrNull() ?: 0f)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // INPUT FIELDS
            Card(modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                AnimatedContent(targetState = currentMode, label = "inputs") { mode ->
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when (mode) {
                            CalcMode.STANDARD -> {
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Ideal for measuring:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BadgeTag("💊 Tablets", ThemePinkLight, ThemePink)
                                        BadgeTag("💉 IV Pushes", ThemePurpleLight, ThemePurple)
                                        BadgeTag("💧 Liquids", ThemeBlueLight, ThemeBlue)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = orderedDose, onValueChange = { orderedDose = it }, label = { Text("Desired Dose (D)") }, placeholder = { Text("e.g. 4") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("÷", fontSize = 24.sp, color = ThemePurple, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 12.dp))
                                    OutlinedTextField(value = availableDose, onValueChange = { availableDose = it }, label = { Text("Have Dose (H)") }, placeholder = { Text("e.g. 2") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("×", fontSize = 24.sp, color = ThemePink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 12.dp))
                                    OutlinedTextField(value = availableVolume, onValueChange = { availableVolume = it }, label = { Text("Volume (Q)") }, placeholder = { Text("e.g. 1") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                }
                            }
                            CalcMode.WEIGHT -> {
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Critical for:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BadgeTag("👶 Pediatrics", ThemeBlueLight, ThemeBlue)
                                        BadgeTag("⏱️ Infusions", ThemeTealLight, ThemeTeal)
                                        BadgeTag("⚠️ Weight-Critical", ThemePinkLight, ThemePink)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Step 1: Patient Need", fontSize = 14.sp, color = ThemeTeal, fontWeight = FontWeight.ExtraBold)
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = patientWeight, onValueChange = { patientWeight = it }, label = { Text("Patient Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                    Text("×", fontSize = 24.sp, color = ThemeTeal, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp))
                                    OutlinedTextField(value = dosePerKg, onValueChange = { dosePerKg = it }, label = { Text("Dose (mg/kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                                Text("Step 2: Medication On Hand", fontSize = 14.sp, color = ThemePurple, fontWeight = FontWeight.ExtraBold)
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("÷", fontSize = 24.sp, color = ThemePurple, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 8.dp))
                                    OutlinedTextField(value = availableDose, onValueChange = { availableDose = it }, label = { Text("Have Dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                    Text("×", fontSize = 24.sp, color = ThemePink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp))
                                    OutlinedTextField(value = availableVolume, onValueChange = { availableVolume = it }, label = { Text("Volume (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                }
                            }
                            CalcMode.PERCENTAGE -> {
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Ideal for measuring:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BadgeTag("💧 IV Fluids", ThemeBlueLight, ThemeBlue)
                                        BadgeTag("🧂 Electrolytes", ThemeTealLight, ThemeTeal)
                                        BadgeTag("📊 % w/v", ThemePurpleLight, ThemePurple)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = percentValue, onValueChange = { percentValue = it }, label = { Text("Solution Percentage (%)") }, placeholder = { Text("e.g. 5 for 5% Dextrose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                                OutlinedTextField(value = percentTotalVol, onValueChange = { percentTotalVol = it }, label = { Text("Total Bag Volume (mL) - Optional") }, placeholder = { Text("e.g. 500") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                            }
                            CalcMode.DILUTION -> {
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Ideal for measuring:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BadgeTag("🧪 Concentrates", ThemeBlueLight, ThemeBlue)
                                        BadgeTag("💧 IV Titration", ThemeTealLight, ThemeTeal)
                                        BadgeTag("⚠️ High-Risk", ThemePinkLight, ThemePink)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Step 1: Target Need (C₂ & V₂)", fontSize = 14.sp, color = ThemeBlue, fontWeight = FontWeight.ExtraBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(value = targetConc, onValueChange = { targetConc = it }, label = { Text("Target Conc (C₂)") }, placeholder = { Text("e.g. 1") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                    OutlinedTextField(value = targetVol, onValueChange = { targetVol = it }, label = { Text("Target Vol (V₂)") }, placeholder = { Text("e.g. 500") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                                Text("Step 2: Divide by Stock (C₁)", fontSize = 14.sp, color = ThemePurple, fontWeight = FontWeight.ExtraBold)
                                OutlinedTextField(value = stockConc, onValueChange = { stockConc = it }, label = { Text("Stock Conc (C₁)") }, placeholder = { Text("e.g. 10") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                            }
                            CalcMode.RECONSTITUTE -> {
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Ideal for measuring:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BadgeTag("🫙 Powder Vials", ThemePurpleLight, ThemePurple)
                                        BadgeTag("💧 Diluent Mixing", ThemeBlueLight, ThemeBlue)
                                        BadgeTag("🏷️ Antibiotics", ThemePinkLight, ThemePink)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Step 1: Create Yield Concentration", fontSize = 14.sp, color = ThemePurple, fontWeight = FontWeight.ExtraBold)
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = vialPowderMg, onValueChange = { vialPowderMg = it }, label = { Text("Powder (mg)") }, placeholder = { Text("e.g. 500") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                    Text("÷", fontSize = 24.sp, color = ThemePurple, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp))
                                    OutlinedTextField(value = diluentAddedMl, onValueChange = { diluentAddedMl = it }, label = { Text("Diluent (mL)") }, placeholder = { Text("e.g. 10") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                                Text("Step 2: Administer Dose", fontSize = 14.sp, color = ThemePink, fontWeight = FontWeight.ExtraBold)
                                OutlinedTextField(value = reconOrderedDose, onValueChange = { reconOrderedDose = it }, label = { Text("Desired Dose (Order)") }, placeholder = { Text("e.g. 1000 for 1g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // RESULTS HUD
            when (currentMode) {
                CalcMode.STANDARD -> {
                    if (calculatedMedVolume > 0f) ResultCard("AMOUNT TO DRAW UP", calculatedMedVolume.toString(), "mL", ThemePurple, ThemePink)
                }
                CalcMode.WEIGHT -> {
                    if (calculatedTargetDoseMg > 0f) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ResultCard("1. TOTAL TARGET DOSE", calculatedTargetDoseMg.toString(), "mg", ThemeTeal, ThemeTealLight)
                            if (calculatedMedVolume > 0f) ResultCard("2. AMOUNT TO DRAW UP", calculatedMedVolume.toString(), "mL", ThemePurple, ThemePink)
                        }
                    }
                }
                CalcMode.PERCENTAGE -> {
                    if (percentMgPerMl > 0f) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ResultCard("CONCENTRATION", percentMgPerMl.toString(), "mg/mL", ThemeTeal, ThemeTealLight)
                            if (percentTotalGrams > 0f) ResultCard("TOTAL SOLUTE IN BAG", percentTotalGrams.toString(), "g", ThemeBlue, ThemeBlueLight)
                        }
                    }
                }
                CalcMode.DILUTION -> {
                    if (stockVolToDraw > 0f) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ResultCard("1. DRAW UP STOCK DRUG", stockVolToDraw.toString(), "mL", ThemePurple, ThemePurpleLight)
                            ResultCard("2. ADD DILUENT (WATER/SALINE)", diluentVolToAdd.toString(), "mL", ThemeBlue, ThemeBlueLight)
                        }
                    }
                }
                CalcMode.RECONSTITUTE -> {
                    if (reconDrawMl > 0f) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ResultCard("YIELD CONCENTRATION", reconConcMgMl.toString(), "mg/mL", ThemeBlue, ThemeBlueLight)
                            ResultCard("AMOUNT TO DRAW UP", reconDrawMl.toString(), "mL", ThemePurple, ThemePink)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// --- REUSABLE COMPONENTS & GRAPHICS ---

@Composable
fun BadgeTag(text: String, bgColor: Color, textColor: Color) {
    Box(modifier = Modifier.background(bgColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun ResultCard(title: String, value: String, unit: String, color1: Color, color2: Color) {
    Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(color1, color2)), RoundedCornerShape(20.dp)).padding(2.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = color1)
                    Text(" $unit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color1.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 6.dp))
                }
            }
        }
    }
}

@Composable
fun AnimatedSyringeGraphic(targetVolume: Float, maxVolume: Float) {
    val textMeasurer = rememberTextMeasurer()
    val fill by animateFloatAsState(if (maxVolume > 0) (targetVolume / maxVolume).coerceIn(0f, 1f) else 0f, tween(1500, easing = FastOutSlowInEasing), label = "")
    val waveFront by rememberInfiniteTransition("").animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "")
    val waveBack by rememberInfiniteTransition("").animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val sW = w * 0.4f; val sL = (w - sW) / 2
        val sTop = h * 0.1f; val sBot = h * 0.85f; val bH = sBot - sTop
        val fH = bH * fill; val fTop = sBot - fH

        drawRect(Color.LightGray, Offset((w - sW*0.2f)/2, fTop - h*0.4f), Size(sW*0.2f, h*0.4f))
        drawRoundRect(Color.DarkGray, Offset(sL + 4f, fTop - 15f), Size(sW - 8f, 15f), CornerRadius(10f, 10f))

        if (fH > 0f) {
            val pathBack = Path().apply {
                moveTo(sL, sBot); lineTo(sL + sW, sBot); lineTo(sL + sW, fTop)
                for (x in (sL + sW).toInt() downTo sL.toInt()) lineTo(x.toFloat(), fTop + sin(x * 0.04f + waveBack) * 3f)
                close()
            }
            drawPath(pathBack, ThemePurple.copy(alpha = 0.5f))

            val pathFront = Path().apply {
                moveTo(sL, sBot); lineTo(sL + sW, sBot); lineTo(sL + sW, fTop)
                for (x in (sL + sW).toInt() downTo sL.toInt()) lineTo(x.toFloat(), fTop + sin(x * 0.05f + waveFront) * 4f)
                close()
            }
            drawPath(pathFront, Brush.verticalGradient(listOf(ThemePinkLight, ThemePurple)))
        }

        drawRoundRect(Color.White.copy(0.4f), Offset(sL, sTop), Size(sW, bH), CornerRadius(8f, 8f))
        drawRoundRect(Color.Gray.copy(0.8f), Offset(sL, sTop), Size(sW, bH), CornerRadius(8f, 8f), style = Stroke(4f))

        for (i in 1..10) {
            val y = sBot - (i * (bH/10))
            val isMajor = i % 2 == 0
            drawLine(Color.Gray, Offset(sL, y), Offset(sL + (if (isMajor) sW*0.3f else sW*0.15f), y), strokeWidth = 3f)
            if (isMajor) {
                val tickValue = (maxVolume / 10f) * i
                val tickText = if (tickValue % 1f == 0f) tickValue.toInt().toString() else String.format("%.1f", tickValue)
                val textLayout = textMeasurer.measure(tickText, TextStyle(color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold))
                drawText(textLayoutResult = textLayout, topLeft = Offset(sL - textLayout.size.width - 8f, y - textLayout.size.height / 2f))
            }
        }
        drawRect(ThemePink, Offset((w - sW*0.4f)/2, sBot), Size(sW*0.4f, 15f))
        drawLine(Color.LightGray, Offset(w/2, sBot + 15f), Offset(w/2, h), strokeWidth = 3f)
    }
}

@Composable
fun AnimatedMultiLiquidSyringe(stockVolume: Float, diluentVolume: Float, maxVolume: Float) {
    val textMeasurer = rememberTextMeasurer()
    val totalVol = stockVolume + diluentVolume
    val totalFill by animateFloatAsState(if (maxVolume > 0) (totalVol / maxVolume).coerceIn(0f, 1f) else 0f, tween(1500, easing = FastOutSlowInEasing), label = "")
    val stockRatio by animateFloatAsState(if (totalVol > 0) stockVolume / totalVol else 0f, tween(1500, easing = FastOutSlowInEasing), label = "")
    val waveFront by rememberInfiniteTransition("").animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val sW = w * 0.4f; val sL = (w - sW) / 2
        val sTop = h * 0.1f; val sBot = h * 0.85f; val bH = sBot - sTop
        val fH = bH * totalFill; val fTop = sBot - fH
        val stockH = fH * stockRatio; val stockTop = sBot - stockH

        drawRect(Color.LightGray, Offset((w - sW*0.2f)/2, fTop - h*0.4f), Size(sW*0.2f, h*0.4f))
        drawRoundRect(Color.DarkGray, Offset(sL + 4f, fTop - 15f), Size(sW - 8f, 15f), CornerRadius(10f, 10f))

        if (fH > 0f) {
            drawRect(ThemePurple, Offset(sL, stockTop), Size(sW, stockH))
            if (fH > stockH) {
                val path = Path().apply {
                    moveTo(sL, stockTop); lineTo(sL + sW, stockTop); lineTo(sL + sW, fTop)
                    for (x in (sL + sW).toInt() downTo sL.toInt()) lineTo(x.toFloat(), fTop + sin(x * 0.05f + waveFront) * 4f)
                    close()
                }
                drawPath(path, Brush.verticalGradient(listOf(ThemeBlueLight, ThemeBlue)))
            }
        }

        drawRoundRect(Color.White.copy(0.4f), Offset(sL, sTop), Size(sW, bH), CornerRadius(8f, 8f))
        drawRoundRect(Color.Gray.copy(0.8f), Offset(sL, sTop), Size(sW, bH), CornerRadius(8f, 8f), style = Stroke(4f))

        for (i in 1..10) {
            val y = sBot - (i * (bH/10)); val isMajor = i % 2 == 0
            drawLine(Color.Gray, Offset(sL, y), Offset(sL + (if(isMajor) sW*0.3f else sW*0.15f), y), strokeWidth = 3f)
            if (isMajor) {
                val tickValue = (maxVolume / 10f) * i
                val tickText = if (tickValue % 1f == 0f) tickValue.toInt().toString() else String.format("%.1f", tickValue)
                val textLayout = textMeasurer.measure(tickText, TextStyle(color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold))
                drawText(textLayoutResult = textLayout, topLeft = Offset(sL - textLayout.size.width - 8f, y - textLayout.size.height / 2f))
            }
        }
        drawRect(ThemeBlue, Offset((w - sW*0.4f)/2, sBot), Size(sW*0.4f, 15f))
        drawLine(Color.LightGray, Offset(w/2, sBot + 15f), Offset(w/2, h), strokeWidth = 3f)
    }
}

@Composable
fun AnimatedIVBagGraphic(mgPerMl: Float) {
    val textMeasurer = rememberTextMeasurer()
    val wave by rememberInfiniteTransition("").animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2500, easing = LinearEasing)), label = "")
    val fill by animateFloatAsState(if (mgPerMl > 0) 0.7f else 0.1f, tween(1500), label = "")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val bW = w * 0.6f; val bL = (w - bW) / 2
        val bTop = h * 0.2f; val bBot = h * 0.8f; val bH = bBot - bTop

        drawCircle(Color.Gray, radius = 10f, center = Offset(w/2, bTop - 15f), style = Stroke(4f))

        val fH = bH * fill; val fTop = bBot - fH
        val path = Path().apply {
            moveTo(bL, bBot); lineTo(bL + bW, bBot); lineTo(bL + bW, fTop)
            for (x in (bL + bW).toInt() downTo bL.toInt()) lineTo(x.toFloat(), fTop + sin(x * 0.05f + wave) * 5f)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(ThemeTealLight, ThemeTeal)))

        drawRoundRect(Color.White.copy(0.3f), Offset(bL, bTop), Size(bW, bH), CornerRadius(20f, 20f))
        drawRoundRect(Color.Gray.copy(0.8f), Offset(bL, bTop), Size(bW, bH), CornerRadius(20f, 20f), style = Stroke(4f))
        drawRect(Color.Gray, Offset(w/2 - 10f, bBot), Size(20f, 20f))

        if (mgPerMl > 0) {
            val textLayout = textMeasurer.measure("${mgPerMl}mg/mL", TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
            drawText(textLayoutResult = textLayout, topLeft = Offset((w - textLayout.size.width) / 2, bTop + bH * 0.5f))
        }
    }
}

@Composable
fun AnimatedVialGraphic(drawVol: Float, maxVol: Float, diluentAdded: Float) {
    val fill by animateFloatAsState(if (diluentAdded > 0) 0.6f else 0.1f, tween(1500), label = "")
    val wave by rememberInfiniteTransition("").animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val vW = w * 0.5f; val vL = (w - vW) / 2
        val vBot = h * 0.9f; val vTop = h * 0.3f; val vH = vBot - vTop

        val fH = vH * fill; val fTop = vBot - fH
        if (diluentAdded > 0) {
            val path = Path().apply {
                moveTo(vL, vBot); lineTo(vL + vW, vBot); lineTo(vL + vW, fTop)
                for (x in (vL + vW).toInt() downTo vL.toInt()) lineTo(x.toFloat(), fTop + sin(x * 0.05f + wave) * 4f)
                close()
            }
            drawPath(path, Brush.verticalGradient(listOf(ThemeBlueLight, ThemeBlue)))
        } else {
            drawRect(Color.LightGray, Offset(vL, vBot - 20f), Size(vW, 20f))
            for(i in 0..50) drawCircle(Color.White, 2f, Offset(vL + Random.nextInt(vW.toInt()), vBot - Random.nextInt(20)))
        }

        drawRoundRect(Color.White.copy(0.4f), Offset(vL, vTop), Size(vW, vH), CornerRadius(16f, 16f))
        drawRoundRect(Color.Gray.copy(0.8f), Offset(vL, vTop), Size(vW, vH), CornerRadius(16f, 16f), style = Stroke(4f))

        drawRect(Color.Gray.copy(0.8f), Offset(w/2 - vW*0.2f, vTop - 20f), Size(vW*0.4f, 20f), style = Stroke(4f))
        drawRoundRect(Color.LightGray, Offset(w/2 - vW*0.25f, vTop - 35f), Size(vW*0.5f, 15f), CornerRadius(4f, 4f))
        drawRoundRect(ThemePurple, Offset(w/2 - vW*0.25f, vTop - 35f), Size(vW*0.5f, 15f), CornerRadius(4f, 4f), style = Stroke(2f))
    }
}

// --- CONTEXT-AWARE EDUCATIONAL DIALOG ---
@Composable
fun ClinicalMathGuideDialog(currentMode: CalcMode, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentMode.emoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Clinical Guide", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(currentMode.title, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = ThemePurple)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                when (currentMode) {
                    CalcMode.STANDARD -> {
                        Text("Basic Dosage Calculations", fontWeight = FontWeight.ExtraBold, color = ThemePurple, fontSize = 18.sp)
                        Text("Determining the volume to administer for tablets, IV pushes, and liquids.", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(ThemePurpleLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Formula: D/H × Q = V\n(Desired ÷ Have) × Volume = mL", fontWeight = FontWeight.ExtraBold, color = ThemePurple)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Clinical Example:", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("Doctor orders 4mg lorazepam IV. Vial concentration is 2mg/mL.\n(4mg ÷ 2mg) × 1mL = 2mL.\nDraw up 2mL of solution.", fontSize = 15.sp, color = ThemePink, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        WarningBox("• Match units (convert mg to g if needed).\n• Beware decimal errors (e.g. 0.2 vs 0.02 mL).\n• Round only the final answer to a safe syringe measurement.\n• Verify vial strength matches calculation.")
                    }
                    CalcMode.WEIGHT -> {
                        Text("Weight-Based Dosing", fontWeight = FontWeight.ExtraBold, color = ThemePurple, fontSize = 18.sp)
                        Text("Essential for paediatrics and infusions where doses depend strictly on body weight.", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(ThemeTealLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Total Dose = Weight (kg) × Dose (mg/kg)", fontWeight = FontWeight.ExtraBold, color = ThemeTeal)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Example 1: Simple Dose", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("Child 20kg needs gentamicin 5mg/kg.\nDose = 5 × 20 = 100mg.\nIf vial is 40mg/mL: (100 ÷ 40) × 1 = 2.5mL.", fontSize = 14.sp, color = ThemePink, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Example 2: Infusion Rate", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("Order adrenaline 5µg/kg/min for 70kg patient.\n5 × 70 = 350 µg/min = 21,000 µg/hr = 21 mg/hr.\nIf conc is 1mg/mL, rate = 21 mL/hr.", fontSize = 14.sp, color = ThemeBlue, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        WarningBox("• Confirm weight units (KG vs LBS) strictly.\n• Red flag extreme weights.\n• Do NOT exceed adult maximums.\n• Check institutional guidance if using IBW/AdjBW for obesity.\n• Ensure decimal points are visible to avoid 2.0 becoming 20.0.")
                    }
                    CalcMode.PERCENTAGE -> {
                        Text("Percentage Solutions (% w/v)", fontWeight = FontWeight.ExtraBold, color = ThemeTeal, fontSize = 18.sp)
                        Text("Converting IV fluid percentages into practical dosing units like mg/mL.", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(ThemeTealLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Formula: x% w/v = x × 10 mg/mL", fontWeight = FontWeight.ExtraBold, color = ThemeTeal)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Clinical Example:", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("A 5% Dextrose solution has 5g per 100mL.\nMultiply by 10:\n5 × 10 = 50 mg/mL.\nSo, a 500mL bag contains 250g total dextrose.", fontSize = 15.sp, color = ThemeTeal, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        WarningBox("• Do not confuse percentages with milligrams.\n• Formula assumes strength is the active drug moiety in mg.\n• Verify if solution is w/v or v/v (v/v is rare).\n• Treat IU (International Units) as standard \"units\" in math.")
                    }
                    CalcMode.DILUTION -> {
                        Text("Dilution Calculator (C₁V₁ = C₂V₂)", fontWeight = FontWeight.ExtraBold, color = ThemeBlue, fontSize = 18.sp)
                        Text("Preparing diluted solutions from stock concentrates.", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(ThemeBlueLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("V₁ = (C₂ × V₂) ÷ C₁\nDraw V₁, then add diluent to reach V₂.", fontWeight = FontWeight.ExtraBold, color = ThemeBlue)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Clinical Example:", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("Need 500mL of 1mg/mL from a 10mg/mL stock.\nV₁ = (1 × 500) ÷ 10 = 50mL.\nDraw 50mL stock, add diluent to reach 500mL total.", fontSize = 15.sp, color = ThemeBlue, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        WarningBox("• Ensure units match on both sides (don't mix % with mg/mL).\n• Mix gently and clearly label new solution with concentration and time.\n• Double-check calculations for electrolytes and high-risk meds.")
                    }
                    CalcMode.RECONSTITUTE -> {
                        Text("Powder Reconstitution", fontWeight = FontWeight.ExtraBold, color = ThemePurple, fontSize = 18.sp)
                        Text("Diluting dry powder medications into a specific liquid volume before administration.", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(ThemePurpleLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Step 1: Yield (mg/mL) = Powder (mg) ÷ Diluent (mL)\nStep 2: Volume (mL) = Target Dose ÷ Yield", fontWeight = FontWeight.ExtraBold, color = ThemePurple)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Clinical Example:", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("500mg vial of ceftriaxone powder. Instructions say add 10mL saline.\nYield = 500mg ÷ 10mL = 50mg/mL.\n\nIf order is 1g (1000mg):\nVolume = 1000mg ÷ 50mg/mL = 20mL to administer.", fontSize = 14.sp, color = ThemePink, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        WarningBox("• Only use recommended diluents; do not overfill vial.\n• Ensure powder is fully dissolved after mixing.\n• Consider volume contributed by medication powder if significant.\n• Label reconstituted syringe/vial with drug name, concentration, date/time, and expiration (usually 24h for IV).")
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ThemePurple)) {
                    Text("Understood - Back to Calculator", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun WarningBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEBEE), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).background(Color(0xFFEF5350), CircleShape), contentAlignment = Alignment.Center) {
                    Text("⚠️", fontSize = 16.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Pitfalls & Safety Checks", fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828), fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text, fontSize = 14.sp, color = Color(0xFFB71C1C), lineHeight = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}