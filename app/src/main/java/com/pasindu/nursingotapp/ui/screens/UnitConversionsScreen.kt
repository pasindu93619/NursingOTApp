package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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

val ThemeIndigo = Color(0xFF3949AB)
val ThemeViolet = Color(0xFF5E35B1)
val ThemeIndigoLight = Color(0xFFC5CAE9)

enum class ConvMode(val title: String, val emoji: String) {
    METRIC("Mass & Volume", "⚖️"),
    HOUSEHOLD("Household", "🥄"),
    ELECTROLYTE("mEq Ions", "⚡")
}

enum class Electrolyte(val symbol: String, val atomicWeight: Float, val valence: Int) {
    SODIUM("Na⁺", 23f, 1),
    POTASSIUM("K⁺", 39f, 1),
    CALCIUM("Ca²⁺", 40f, 2)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConversionsScreen() {
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(ConvMode.METRIC) }

    // --- STATE: METRIC ---
    var metricMcg by remember { mutableStateOf("") }
    var metricMg by remember { mutableStateOf("") }
    var metricGrams by remember { mutableStateOf("") }
    var metricKg by remember { mutableStateOf("") }

    // --- STATE: HOUSEHOLD ---
    var houseTsp by remember { mutableStateOf("") }
    var houseTbsp by remember { mutableStateOf("") }
    var houseOz by remember { mutableStateOf("") }
    var houseMl by remember { mutableStateOf("") }

    // --- STATE: ELECTROLYTES ---
    var selectedIon by remember { mutableStateOf(Electrolyte.POTASSIUM) }
    var mEqInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { delay(100); isVisible = true }

    // --- ENGINE: ELECTROLYTES ---
    val calculatedMgForIon = remember(selectedIon, mEqInput) {
        val mEq = mEqInput.toFloatOrNull() ?: 0f
        if (mEq > 0) {
            // Formula: mg = (mEq * atomic weight) / valence
            val mg = (mEq * selectedIon.atomicWeight) / selectedIon.valence
            (Math.round(mg * 100.0) / 100.0).toFloat()
        } else 0f
    }

    if (showGuideDialog) ConversionsClinicalGuideDialog(currentMode, onDismiss = { showGuideDialog = false })

    val bgGradient = Brush.verticalGradient(listOf(ThemeIndigoLight.copy(alpha = 0.4f), MaterialTheme.colorScheme.surface))

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            // HEADER
            AnimatedVisibility(visible = isVisible, enter = slideInVertically { -50 } + fadeIn()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(ThemeIndigo, CircleShape).shadow(8.dp, CircleShape), contentAlignment = Alignment.Center) { Text("🔄", fontSize = 24.sp) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Unit Conversions", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ThemeIndigo)
                            Text("Equivalents Engine", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(modifier = Modifier.size(42.dp).background(ThemeViolet.copy(alpha = 0.2f), CircleShape).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showGuideDialog = true }, contentAlignment = Alignment.Center) { Text("❓", fontSize = 18.sp) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // TABS
            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ConvMode.values()) { mode ->
                    val isSelected = currentMode == mode
                    val bgColor by animateColorAsState(if (isSelected) ThemeIndigo else Color.White.copy(alpha = 0.6f), label = "")
                    val textColor by animateColorAsState(if (isSelected) Color.White else Color.Gray, label = "")
                    Box(
                        modifier = Modifier.height(40.dp).background(bgColor, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentMode = mode }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("${mode.emoji} ${mode.title}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // INPUTS & BI-DIRECTIONAL ENGINE
            Card(modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                AnimatedContent(targetState = currentMode, label = "") { mode ->
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        when (mode) {
                            ConvMode.METRIC -> {
                                Text("Bi-Directional Mass Calculator", fontSize = 14.sp, color = ThemeViolet, fontWeight = FontWeight.ExtraBold)
                                Text("Type in any field to instantly convert.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = metricMcg,
                                    onValueChange = {
                                        metricMcg = it; val v = it.toFloatOrNull() ?: 0f
                                        metricMg = if(v>0) (v / 1000f).toString() else ""; metricGrams = if(v>0) (v / 1000000f).toString() else ""; metricKg = if(v>0) (v / 1000000000f).toString() else ""
                                    },
                                    label = { Text("Micrograms (µg / mcg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = metricMg,
                                    onValueChange = {
                                        metricMg = it; val v = it.toFloatOrNull() ?: 0f
                                        metricMcg = if(v>0) (v * 1000f).toString() else ""; metricGrams = if(v>0) (v / 1000f).toString() else ""; metricKg = if(v>0) (v / 1000000f).toString() else ""
                                    },
                                    label = { Text("Milligrams (mg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = metricGrams,
                                    onValueChange = {
                                        metricGrams = it; val v = it.toFloatOrNull() ?: 0f
                                        metricMg = if(v>0) (v * 1000f).toString() else ""; metricMcg = if(v>0) (v * 1000000f).toString() else ""; metricKg = if(v>0) (v / 1000f).toString() else ""
                                    },
                                    label = { Text("Grams (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = metricKg,
                                    onValueChange = {
                                        metricKg = it; val v = it.toFloatOrNull() ?: 0f
                                        metricGrams = if(v>0) (v * 1000f).toString() else ""; metricMg = if(v>0) (v * 1000000f).toString() else ""; metricMcg = if(v>0) (v * 1000000000f).toString() else ""
                                    },
                                    label = { Text("Kilograms (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                )
                            }

                            ConvMode.HOUSEHOLD -> {
                                Text("Bi-Directional Fluid Equivalents", fontSize = 14.sp, color = ThemeViolet, fontWeight = FontWeight.ExtraBold)
                                Text("Type in any field to instantly convert.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = houseMl,
                                    onValueChange = {
                                        houseMl = it; val v = it.toFloatOrNull() ?: 0f
                                        houseTsp = if(v>0) (Math.round((v / 5f)*100.0)/100.0).toString() else ""; houseTbsp = if(v>0) (Math.round((v / 15f)*100.0)/100.0).toString() else ""; houseOz = if(v>0) (Math.round((v / 30f)*100.0)/100.0).toString() else ""
                                    },
                                    label = { Text("Milliliters (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = houseTsp,
                                    onValueChange = {
                                        houseTsp = it; val v = it.toFloatOrNull() ?: 0f
                                        houseMl = if(v>0) (v * 5f).toString() else ""; houseTbsp = if(v>0) (Math.round((v / 3f)*100.0)/100.0).toString() else ""; houseOz = if(v>0) (Math.round((v / 6f)*100.0)/100.0).toString() else ""
                                    },
                                    label = { Text("Teaspoons (tsp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = houseTbsp,
                                    onValueChange = {
                                        houseTbsp = it; val v = it.toFloatOrNull() ?: 0f
                                        houseMl = if(v>0) (v * 15f).toString() else ""; houseTsp = if(v>0) (v * 3f).toString() else ""; houseOz = if(v>0) (Math.round((v / 2f)*100.0)/100.0).toString() else ""
                                    },
                                    label = { Text("Tablespoons (tbsp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = houseOz,
                                    onValueChange = {
                                        houseOz = it; val v = it.toFloatOrNull() ?: 0f
                                        houseMl = if(v>0) (v * 30f).toString() else ""; houseTsp = if(v>0) (v * 6f).toString() else ""; houseTbsp = if(v>0) (v * 2f).toString() else ""
                                    },
                                    label = { Text("Fluid Ounces (oz)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                )

                                // Extra Info Box for drops
                                if (houseTsp.isNotEmpty() && houseTsp.toFloatOrNull() ?: 0f > 0) {
                                    val gtt = (houseTsp.toFloat() * 60f).toInt()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("💧", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Equivalent to exactly $gtt drops (gtt).", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 14.sp)
                                    }
                                }
                            }

                            ConvMode.ELECTROLYTE -> {
                                Text("mEq to mg Converter", fontSize = 14.sp, color = ThemeViolet, fontWeight = FontWeight.ExtraBold)
                                Text("Select a multivalent ion and enter the ordered mEq.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Electrolyte.values().forEach { ion ->
                                        val isSelected = selectedIon == ion
                                        val bgColor = if (isSelected) ThemeIndigo else Color.LightGray.copy(alpha = 0.2f)
                                        Box(modifier = Modifier.weight(1f).background(bgColor, RoundedCornerShape(12.dp)).clickable { selectedIon = ion; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }.padding(12.dp), contentAlignment = Alignment.Center) {
                                            Text(ion.symbol, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if(isSelected) Color.White else Color.Gray)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = mEqInput,
                                    onValueChange = { mEqInput = it },
                                    label = { Text("Ordered mEq") }, placeholder = { Text("e.g. 20") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                // Info banner about the specific ion
                                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF3E5F5), RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔬", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Atomic Weight: ${selectedIon.atomicWeight}", fontWeight = FontWeight.Bold, color = ThemeViolet, fontSize = 12.sp)
                                        Text("Valence: ${selectedIon.valence}", fontWeight = FontWeight.Bold, color = ThemeViolet, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // RESULT (Only for Electrolyte Mode since others are bi-directional)
            if (currentMode == ConvMode.ELECTROLYTE && calculatedMgForIon > 0f) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(ThemeViolet, ThemeIndigo)), RoundedCornerShape(20.dp)).padding(2.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("EQUIVALENT MASS IN mg", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(calculatedMgForIon.toString(), fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, color = ThemeIndigo)
                                Text(" mg", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ThemeViolet, modifier = Modifier.padding(bottom = 8.dp, start = 6.dp))
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
fun ConversionsClinicalGuideDialog(currentMode: ConvMode, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentMode.emoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Clinical Guide", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(currentMode.title, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = ThemeIndigo)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                when (currentMode) {
                    ConvMode.METRIC, ConvMode.HOUSEHOLD -> {
                        Text("Basic Conversions", fontWeight = FontWeight.ExtraBold, color = ThemeIndigo, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• 1 g = 1000 mg\n• 1 mg = 1000 µg (mcg)\n• 1 kg = 1000 g\n• 1 L = 1000 mL", fontSize = 14.sp, color = Color.DarkGray, lineHeight = 24.sp)

                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Household Equivalents", fontWeight = FontWeight.ExtraBold, color = ThemeIndigo, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• 1 tsp = 5 mL\n• 1 tbsp = 15 mL (which is 3 tsp)\n• 1 oz (fluid) ≈ 30 mL\n• 1 pint ≈ 500 mL\n• 1 tsp = exactly 60 drops (gtt)", fontSize = 14.sp, color = Color.DarkGray, lineHeight = 24.sp)
                    }
                    ConvMode.ELECTROLYTE -> {
                        Text("Milliequivalents (mEq)", fontWeight = FontWeight.ExtraBold, color = ThemeIndigo, fontSize = 18.sp)
                        Text("Used specifically for calculating electrolyte doses based on atomic activity.", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(ThemeIndigoLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("mEq = (mg × valence) ÷ atomic weight", fontWeight = FontWeight.ExtraBold, color = ThemeViolet)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Clinical Example:", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("Physician orders KCl 20 mEq IV.\nAvailable vial contains 2 mEq/mL (which is 16.2 mg KCl per mEq).\nVolume needed = 20 mEq × (1 mL / 2 mEq) = 10 mL.\n(10 mL contains exactly 324 mg KCl).", fontSize = 14.sp, color = ThemeViolet, lineHeight = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CRITICAL SAFETY BOX
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(16.dp)).border(2.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp)).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(Color(0xFFEF5350), CircleShape), contentAlignment = Alignment.Center) { Text("⚠️", fontSize = 16.sp, color = Color.White) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Safety & Pitfalls", fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828), fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• International Units (IU): 1 IU of insulin does NOT have the same mass as 1 IU of heparin. You must ALWAYS use the manufacturer's drug label for IU-to-mass conversions. Never assume IU = mg.\n• Valences: Pay attention to the valence. For multivalent ions (like Ca²⁺ or Mg²⁺), 1 mmol yields 2 mEq.\n• Always write \"mEq\" specifically when labeling electrolyte infusions.", fontSize = 13.sp, color = Color(0xFFB71C1C), lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ThemeIndigo)) { Text("Acknowledge & Close", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
    }
}