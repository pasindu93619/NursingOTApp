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
import kotlin.math.round

private val PedsBg = Color(0xFFF7FAFF)
private val PedsInk = Color(0xFF12204A)
private val PedsSlate = Color(0xFF64748B)
private val PedsBlue = Color(0xFF1769E8)
private val PedsCyan = Color(0xFF149FE3)
private val PedsPurple = Color(0xFF7B5CEB)
private val PedsSoftBlue = Color(0xFFEAF6FF)
private val PedsSoftPurple = Color(0xFFF3EEFF)
private val PedsSoftAmber = Color(0xFFFFF6E7)
private val PedsHeroGradient = Brush.horizontalGradient(listOf(PedsBlue, PedsCyan, Color(0xFF4B78F2), PedsPurple))

private enum class PedsMode(val title: String, val shortTitle: String, val emoji: String) {
    CLARK("Clark's Rule", "Clark", "⚖️"),
    YOUNG("Young's Rule", "Young", "🧒"),
    FRIED("Fried's Rule", "Fried", "🍼")
}

@Composable
fun PediatricRulesScreen() {
    val haptic = LocalHapticFeedback.current
    var mode by remember { mutableStateOf(PedsMode.CLARK) }
    var showGuide by remember { mutableStateOf(false) }

    var adultDoseMg by remember { mutableStateOf("") }
    var childWeightKg by remember { mutableStateOf("") }
    var childAgeYears by remember { mutableStateOf("") }
    var infantAgeMonths by remember { mutableStateOf("") }

    val calculatedDose = remember(mode, adultDoseMg, childWeightKg, childAgeYears, infantAgeMonths) {
        val adult = adultDoseMg.toDoubleOrNull() ?: 0.0
        if (adult <= 0.0) return@remember 0.0
        val raw = when (mode) {
            PedsMode.CLARK -> {
                val kg = childWeightKg.toDoubleOrNull() ?: 0.0
                if (kg > 0.0) (kg / 68.0) * adult else 0.0
            }
            PedsMode.YOUNG -> {
                val age = childAgeYears.toDoubleOrNull() ?: 0.0
                if (age > 0.0) (age / (age + 12.0)) * adult else 0.0
            }
            PedsMode.FRIED -> {
                val months = infantAgeMonths.toDoubleOrNull() ?: 0.0
                if (months > 0.0) (months / 150.0) * adult else 0.0
            }
        }
        round(raw * 100.0) / 100.0
    }

    val fraction = remember(calculatedDose, adultDoseMg) {
        val adult = adultDoseMg.toDoubleOrNull() ?: 0.0
        if (adult > 0.0 && calculatedDose > 0.0) calculatedDose / adult else 0.0
    }

    if (showGuide) PedsGuideDialog(mode) { showGuide = false }

    Column(
        Modifier
            .fillMaxSize()
            .background(PedsBg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(6.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .background(PedsHeroGradient, RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                            Text("CLINICAL TOOL", color = Color.White.copy(alpha = 0.86f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("LEGACY", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text("Paediatric Rules", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("Historical approximation tools. Verify against a current weight/BSA-based order.", color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp, lineHeight = 16.sp)
                }
                Surface(color = Color.White.copy(alpha = 0.15f), shape = CircleShape) {
                    Text("🧒", fontSize = 25.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }

        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose a rule", color = PedsSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    PedsMode.values().forEach { item ->
                        val selected = mode == item
                        Surface(
                            Modifier.weight(1f).height(52.dp).clickable {
                                mode = item
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            color = if (selected) PedsBlue else PedsBg,
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(item.emoji, fontSize = 16.sp)
                                Text(item.shortTitle, color = if (selected) Color.White else PedsSlate, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        Surface(color = PedsSoftAmber, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Text("⚠️", fontSize = 18.sp)
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("Approximation only", color = Color(0xFF7A4A00), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("These historical rules should not replace a prescribed paediatric mg/kg or BSA regimen.", color = Color(0xFF7A4A00), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${mode.title}", color = PedsInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(ruleDescription(mode), color = PedsSlate, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    IconButton(onClick = { showGuide = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Rule guide", tint = PedsBlue)
                    }
                }

                InputField("Standard adult dose (mg)", adultDoseMg) { adultDoseMg = it }

                when (mode) {
                    PedsMode.CLARK -> InputField("Child weight (kg)", childWeightKg) { childWeightKg = it }
                    PedsMode.YOUNG -> InputField("Child age (years)", childAgeYears) { childAgeYears = it }
                    PedsMode.FRIED -> InputField("Infant age (months)", infantAgeMonths) { infantAgeMonths = it }
                }
            }
        }

        if (calculatedDose > 0.0) {
            Surface(color = PedsSoftBlue, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CALCULATED PROPORTION", color = PedsBlue, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("${formatPct(fraction)}% of adult dose", color = PedsInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(formatDose(calculatedDose), color = PedsInk, fontSize = 46.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(7.dp))
                        Text("mg", color = PedsBlue, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("Pediatric dose estimate", color = PedsSlate, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }

        Surface(color = PedsSoftPurple, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CLINICAL CHECK", color = PedsPurple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Confirm age, current weight, indication, formulation, prescribed units, maximum dose and local paediatric guidance before administration.", color = Color(0xFF55408F), fontSize = 11.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InputField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
    )
}

private fun ruleDescription(mode: PedsMode): String = when (mode) {
    PedsMode.CLARK -> "Weight-based historical fraction of an adult dose."
    PedsMode.YOUNG -> "Age-based historical fraction for children."
    PedsMode.FRIED -> "Age-in-months historical fraction for infants."
}

@Composable
private fun PedsGuideDialog(mode: PedsMode, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(0.93f).fillMaxHeight(0.78f), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${mode.emoji} ${mode.title}", color = PedsInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Surface(color = PedsSoftBlue, shape = RoundedCornerShape(16.dp)) {
                    Text(ruleFormula(mode), Modifier.fillMaxWidth().padding(15.dp), color = PedsBlue, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
                }
                Text(ruleNote(mode), color = PedsSlate, fontSize = 13.sp, lineHeight = 20.sp)
                Surface(color = PedsSoftAmber, shape = RoundedCornerShape(16.dp)) {
                    Text("Safety: legacy dose-estimation rules are not a substitute for a current patient-specific paediatric prescription. Verify the drug-specific dose and maximum dose independently.", Modifier.padding(14.dp), color = Color(0xFF7A4A00), fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                }
                Button(onClick = onDismiss, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) {
                    Text("Close", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

private fun ruleFormula(mode: PedsMode): String = when (mode) {
    PedsMode.CLARK -> "Dose = (child weight in kg ÷ 68 kg) × adult dose"
    PedsMode.YOUNG -> "Dose = [age in years ÷ (age in years + 12)] × adult dose"
    PedsMode.FRIED -> "Dose = [age in months ÷ 150] × adult dose"
}

private fun ruleNote(mode: PedsMode): String = when (mode) {
    PedsMode.CLARK -> "Clark's rule is a historical weight-based adult-to-child approximation. Contemporary clinical dosing generally relies on the specific medicine's paediatric dosing recommendations, often expressed per kg or by BSA."
    PedsMode.YOUNG -> "Young's rule uses age to estimate a fraction of an adult dose. It is an historical approximation and can be inaccurate because children of the same age can differ substantially in weight and physiology."
    PedsMode.FRIED -> "Fried's rule uses age in months to estimate an infant dose as a fraction of the adult dose. Infant dosing is particularly sensitive to developmental differences, so a drug-specific paediatric source should be used."
}

private fun formatDose(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(java.util.Locale.US, value)
private fun formatPct(value: Double): String = "%.1f".format(java.util.Locale.US, value * 100.0)
