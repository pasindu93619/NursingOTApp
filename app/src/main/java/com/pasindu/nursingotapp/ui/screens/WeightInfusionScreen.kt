package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import kotlin.math.round

private val WeightBg = Color(0xFFF8FAFC)
private val WeightInk = Color(0xFF12204A)
private val WeightSlate = Color(0xFF64748B)
private val WeightBlue = Color(0xFF1769E8)
private val WeightPurple = Color(0xFF7B5CEB)
private val WeightBlueSoft = Color(0xFFEAF6FF)
private val WeightMintSoft = Color(0xFFEAFBF5)
private val WeightAmberSoft = Color(0xFFFFF6E7)
private val WeightHeroGradient = Brush.horizontalGradient(
    listOf(Color(0xFF1769E8), Color(0xFF149FE3), Color(0xFF4B78F2), Color(0xFF7B5CEB))
)

private enum class WeightMode(val title: String, val shortTitle: String, val emoji: String) {
    SIMPLE("Simple dose", "Dose", "⚖️"),
    INFUSION("Continuous IV", "Infusion", "💧")
}

@Composable
fun WeightInfusionScreen() {
    val haptic = LocalHapticFeedback.current
    var mode by remember { mutableStateOf(WeightMode.SIMPLE) }
    var showGuide by remember { mutableStateOf(false) }

    var weightKg by remember { mutableStateOf("") }
    var doseMgKg by remember { mutableStateOf("") }
    var availableMg by remember { mutableStateOf("") }
    var availableMl by remember { mutableStateOf("") }
    var doseMcgKgMin by remember { mutableStateOf("") }
    var drugTotalMg by remember { mutableStateOf("") }
    var ivBagTotalMl by remember { mutableStateOf("") }

    val simpleTargetMg = remember(weightKg, doseMgKg) {
        val w = weightKg.toFloatOrNull() ?: 0f
        val d = doseMgKg.toFloatOrNull() ?: 0f
        round(w * d * 100f) / 100f
    }

    val simpleDrawMl = remember(simpleTargetMg, availableMg, availableMl) {
        val have = availableMg.toFloatOrNull() ?: 0f
        val vol = availableMl.toFloatOrNull() ?: 0f
        if (have > 0f) round((simpleTargetMg / have) * vol * 100f) / 100f else 0f
    }

    val infusionRateMlHr = remember(weightKg, doseMcgKgMin, drugTotalMg, ivBagTotalMl) {
        val w = weightKg.toFloatOrNull() ?: 0f
        val dose = doseMcgKgMin.toFloatOrNull() ?: 0f
        val drugMg = drugTotalMg.toFloatOrNull() ?: 0f
        val bagMl = ivBagTotalMl.toFloatOrNull() ?: 0f
        if (drugMg > 0f && bagMl > 0f) {
            val mcgPerMin = w * dose
            val mgPerHr = (mcgPerMin * 60f) / 1000f
            val concentrationMgMl = drugMg / bagMl
            if (concentrationMgMl > 0f) round((mgPerHr / concentrationMgMl) * 10f) / 10f else 0f
        } else 0f
    }

    if (showGuide) WeightGuideDialog(mode, onDismiss = { showGuide = false })

    Column(
        Modifier
            .fillMaxSize()
            .background(WeightBg)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().background(WeightHeroGradient, RoundedCornerShape(26.dp)).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("CLINICAL TOOL", color = Color.White.copy(alpha = 0.74f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Weight & Infusions", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("Weight-based doses and continuous IV rates in one workspace.", color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp, lineHeight = 16.sp)
                }
                Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp)) {
                    Text("⚖️", fontSize = 25.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }

        Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(22.dp)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeightMode.values().forEach { item ->
                val selected = mode == item
                Surface(
                    Modifier.weight(1f).height(48.dp).clickable { mode = item; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) WeightBlue else WeightBg
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.emoji, fontSize = 17.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(item.shortTitle, color = if (selected) Color.White else WeightSlate, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Patient weight", color = WeightInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("Enter kilograms used by the prescription", color = WeightSlate, fontSize = 11.sp)
                    }
                    IconButton(onClick = { showGuide = true }) { Icon(Icons.Default.Info, contentDescription = "Calculation guide", tint = WeightBlue) }
                }
                WeightInput("Weight (kg)", weightKg, { weightKg = it })
            }
        }

        when (mode) {
            WeightMode.SIMPLE -> {
                Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Simple weight-based dose", color = WeightInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("Calculate the target dose, then optionally convert it to a draw volume.", color = WeightSlate, fontSize = 11.sp, lineHeight = 16.sp)
                        WeightInput("Prescribed dose (mg/kg)", doseMgKg, { doseMgKg = it })
                    }
                }
                WeightResultCard("TARGET DOSE", formatWeight(simpleTargetMg), "mg", WeightMintSoft, Color(0xFF087F5B))
                Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Optional draw-volume conversion", color = WeightInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("Enter the available strength and supplied volume.", color = WeightSlate, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WeightInput("Available dose (mg)", availableMg, { availableMg = it }, Modifier.weight(1f))
                            WeightInput("Volume (mL)", availableMl, { availableMl = it }, Modifier.weight(1f))
                        }
                    }
                }
                if (simpleDrawMl > 0f) WeightResultCard("DRAW THIS VOLUME", formatWeight(simpleDrawMl), "mL", WeightBlueSoft, WeightBlue)
            }
            WeightMode.INFUSION -> {
                Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Continuous IV rate", color = WeightInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("Convert a prescribed mcg/kg/min dose into the pump setting.", color = WeightSlate, fontSize = 11.sp, lineHeight = 16.sp)
                        WeightInput("Dose / rate (mcg/kg/min)", doseMcgKgMin, { doseMcgKgMin = it })
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WeightInput("Drug in bag (mg)", drugTotalMg, { drugTotalMg = it }, Modifier.weight(1f))
                            WeightInput("Bag volume (mL)", ivBagTotalMl, { ivBagTotalMl = it }, Modifier.weight(1f))
                        }
                    }
                }
                WeightResultCard("PUMP RATE TARGET", formatWeight(infusionRateMlHr), "mL/hr", WeightBlueSoft, WeightBlue)
            }
        }

        Surface(color = WeightAmberSoft, shape = RoundedCornerShape(18.dp)) {
            Text("Safety check: verify patient weight, units, prescribed dose/rate, preparation concentration and applicable local protocol before administration.", Modifier.padding(14.dp), color = Color(0xFF7A4A00), fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun WeightInput(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(), label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(16.dp))
}

@Composable
private fun WeightResultCard(title: String, value: String, unit: String, background: Color, accent: Color) {
    Surface(color = background, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(19.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = WeightInk, fontSize = 44.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(7.dp))
                Text(unit, color = accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 7.dp))
            }
        }
    }
}

@Composable
private fun WeightGuideDialog(mode: WeightMode, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.72f), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${mode.emoji} ${mode.title}", color = WeightInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
                if (mode == WeightMode.SIMPLE) {
                    Text("Dose workflow", color = WeightBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("Weight × prescribed mg/kg = target dose (mg).\n\nTarget dose ÷ available dose × supplied volume = draw volume.", color = WeightSlate, fontSize = 14.sp, lineHeight = 20.sp)
                } else {
                    Text("Infusion workflow", color = WeightBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("Weight × mcg/kg/min gives mcg/min. Convert to mg/hr, then divide by prepared concentration (mg/mL) to obtain mL/hr.", color = WeightSlate, fontSize = 14.sp, lineHeight = 20.sp)
                }
                Surface(color = WeightAmberSoft, shape = RoundedCornerShape(16.dp)) {
                    Text("Always independently verify the prescription, concentration and local protocol.", Modifier.padding(14.dp), color = Color(0xFF7A4A00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Text("Close", fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

private fun formatWeight(value: Float): String = when {
    value == 0f -> "0"
    value % 1f == 0f -> value.toInt().toString()
    else -> value.toString()
}
