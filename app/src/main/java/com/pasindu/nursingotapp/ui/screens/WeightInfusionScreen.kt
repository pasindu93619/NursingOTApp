package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightInfusionScreen() {
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var isInfusionMode by remember { mutableStateOf(false) } // False = Simple Dose, True = Infusion

    // Inputs
    var weightKg by remember { mutableStateOf("") }

    // Simple Dose Inputs
    var doseMgKg by remember { mutableStateOf("") }
    var availableMg by remember { mutableStateOf("") }
    var availableMl by remember { mutableStateOf("") }

    // Infusion Inputs
    var doseMcgKgMin by remember { mutableStateOf("") }
    var drugTotalMg by remember { mutableStateOf("") }
    var ivBagTotalMl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { delay(100); isVisible = true }

    // Math Engine: Simple Dose
    val simpleTargetMg = remember(weightKg, doseMgKg) {
        val w = weightKg.toFloatOrNull() ?: 0f
        val d = doseMgKg.toFloatOrNull() ?: 0f
        Math.round((w * d) * 100.0) / 100.0
    }

    val simpleDrawMl = remember(simpleTargetMg, availableMg, availableMl) {
        val have = availableMg.toFloatOrNull() ?: 0f
        val vol = availableMl.toFloatOrNull() ?: 0f
        if (have > 0f) Math.round((simpleTargetMg / have) * vol * 100.0) / 100.0 else 0.0
    }

    // Math Engine: Infusion Rate (mcg/kg/min -> mL/hr)
    val infusionRateMlHr = remember(weightKg, doseMcgKgMin, drugTotalMg, ivBagTotalMl) {
        val w = weightKg.toFloatOrNull() ?: 0f
        val dose = doseMcgKgMin.toFloatOrNull() ?: 0f
        val drugMg = drugTotalMg.toFloatOrNull() ?: 0f
        val bagMl = ivBagTotalMl.toFloatOrNull() ?: 0f

        if (drugMg > 0f && bagMl > 0f) {
            val mcgPerMin = w * dose
            val mgPerHr = (mcgPerMin * 60) / 1000
            val concentrationMgMl = drugMg / bagMl
            Math.round((mgPerHr / concentrationMgMl) * 10.0) / 10.0 // Round to 1 decimal for pump
        } else 0.0
    }

    LaunchedEffect(simpleDrawMl, infusionRateMlHr) {
        if (simpleDrawMl > 0 || infusionRateMlHr > 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    if (showGuideDialog) WeightClinicalGuideDialog(onDismiss = { showGuideDialog = false })

    val bgGradient = Brush.verticalGradient(listOf(Color(0xFFB2DFDB).copy(alpha = 0.3f), MaterialTheme.colorScheme.surface))

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            // HEADER
            AnimatedVisibility(visible = isVisible, enter = slideInVertically { -50 } + fadeIn()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(Color(0xFF00796B), CircleShape).shadow(8.dp, CircleShape), contentAlignment = Alignment.Center) { Text("⚖️", fontSize = 24.sp) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Weight & Infusions", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00796B))
                            Text("Paediatrics & ICU Rates", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(modifier = Modifier.size(42.dp).background(Color(0xFF00796B).copy(alpha = 0.2f), CircleShape).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showGuideDialog = true }, contentAlignment = Alignment.Center) { Text("❓", fontSize = 18.sp) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // MODE TOGGLE
            Row(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(4.dp)) {
                Box(modifier = Modifier.weight(1f).height(40.dp).background(if (!isInfusionMode) Color(0xFF00796B) else Color.Transparent, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).clickable { isInfusionMode = false }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text("Simple Dose", color = if (!isInfusionMode) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(1f).height(40.dp).background(if (isInfusionMode) Color(0xFF00796B) else Color.Transparent, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).clickable { isInfusionMode = true }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text("Continuous IV", color = if (isInfusionMode) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // GLOWING IV PUMP DISPLAY (Only for Infusion Mode)
            AnimatedVisibility(visible = isVisible && isInfusionMode, enter = scaleIn() + fadeIn()) {
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B), RoundedCornerShape(16.dp)).border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("IV PUMP RATE TARGET", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            val alpha by rememberInfiniteTransition("").animateFloat(0.5f, 1f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "")
                            Text(if (infusionRateMlHr > 0) infusionRateMlHr.toString() else "0.0", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0EA5E9).copy(alpha = if (infusionRateMlHr > 0) alpha else 1f))
                            Text(" mL/hr", fontSize = 24.sp, color = Color(0xFF38BDF8), modifier = Modifier.padding(bottom = 10.dp, start = 8.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isVisible && !isInfusionMode, enter = scaleIn() + fadeIn()) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF00796B), Color(0xFF26A69A))), RoundedCornerShape(16.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL TARGET DOSE", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(if (simpleTargetMg > 0) simpleTargetMg.toString() else "0.0", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text(" mg", fontSize = 24.sp, color = Color.White, modifier = Modifier.padding(bottom = 10.dp, start = 8.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // INPUT FIELDS
            Card(modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Universal Weight Input
                    OutlinedTextField(value = weightKg, onValueChange = { weightKg = it }, label = { Text("Patient Weight (kg)") }, leadingIcon = { Text("⚖️") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00796B)))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    if (!isInfusionMode) {
                        OutlinedTextField(value = doseMgKg, onValueChange = { doseMgKg = it }, label = { Text("Prescribed Dose (mg/kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = availableMg, onValueChange = { availableMg = it }, label = { Text("Have Dose (mg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                            OutlinedTextField(value = availableMl, onValueChange = { availableMl = it }, label = { Text("Volume (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                        }
                    } else {
                        OutlinedTextField(value = doseMcgKgMin, onValueChange = { doseMcgKgMin = it }, label = { Text("Rate (mcg/kg/min)") }, leadingIcon = { Text("⏱️") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = drugTotalMg, onValueChange = { drugTotalMg = it }, label = { Text("Drug in Bag (mg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                            OutlinedTextField(value = ivBagTotalMl, onValueChange = { ivBagTotalMl = it }, label = { Text("Bag Vol (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // RESULTS FOR SIMPLE DOSE
            if (!isInfusionMode && simpleDrawMl > 0) {
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF26A69A), RoundedCornerShape(20.dp)).padding(2.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AMOUNT TO DRAW UP", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(simpleDrawMl.toString(), fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00796B))
                                Text(" mL", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26A69A), modifier = Modifier.padding(bottom = 6.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// --- EDUCATIONAL DIALOG ---
@Composable
fun WeightClinicalGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚖️", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Clinical Guide", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("Weight & Infusions", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF00796B))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text("Example 1: Simple Dose", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Text("Child 20kg needs gentamicin 5mg/kg.\nTotal Dose = 5mg/kg × 20kg = 100mg.\n\nIf vial is 40mg/mL:\nVolume = (100mg / 40mg) × 1mL = 2.5mL.", fontSize = 14.sp, color = Color(0xFF00796B), lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                Text("Example 2: Infusion Rate", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Text("Order adrenaline 5µg/kg/min for 70kg patient.\n\n1. Calculate mcg/min:\n5 × 70 = 350 µg/min\n\n2. Convert to mg/hr:\n(350 × 60) ÷ 1000 = 21 mg/hr\n\n3. Calculate mL/hr (If conc is 1mg/mL):\nRate = 21 mL/hr.", fontSize = 14.sp, color = Color(0xFF1E88E5), lineHeight = 20.sp)

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(16.dp)).border(2.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp)).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(Color(0xFFEF5350), CircleShape), contentAlignment = Alignment.Center) { Text("⚠️", fontSize = 16.sp, color = Color.White) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Pitfalls & Safety Checks", fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828), fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• Always confirm weight units (KG vs LBS).\n• Red flag extreme weights. Ensure decimal points are visible to avoid 2.0 becoming 20.0.\n• Watch that mg/kg doses don't exceed adult maximums.\n• Adjusted/Ideal Body Weight (IBW): Avoid using IBW for normal patients. Check institutional guidance if using IBW or AdjBW (often for aminoglycosides).", fontSize = 13.sp, color = Color(0xFFB71C1C), lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))) { Text("Understood", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
    }
}