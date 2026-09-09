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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.round

private val ConvBg = Color(0xFFF7FAFF)
private val ConvInk = Color(0xFF12204A)
private val ConvSlate = Color(0xFF64748B)
private val ConvBlue = Color(0xFF1769E8)
private val ConvPurple = Color(0xFF7B5CEB)
private val ConvBlueSoft = Color(0xFFEAF6FF)
private val ConvPurpleSoft = Color(0xFFF3EEFF)
private val ConvAmberSoft = Color(0xFFFFF6E7)
private val ConvHeroGradient = Brush.horizontalGradient(listOf(Color(0xFF1769E8), Color(0xFF149FE3), Color(0xFF4B78F2), Color(0xFF7B5CEB)))

private enum class ConvMode(val title: String, val shortTitle: String, val emoji: String) {
    METRIC("Mass & Volume", "Metric", "⚖️"),
    HOUSEHOLD("Household volume", "Household", "🥄"),
    ELECTROLYTE("Electrolyte", "mEq", "⚡")
}

private enum class MetricUnit(val label: String, val symbol: String, val microgramsPerUnit: Double) {
    MICROGRAM("Micrograms", "µg", 1.0),
    MILLIGRAM("Milligrams", "mg", 1_000.0),
    GRAM("Grams", "g", 1_000_000.0),
    KILOGRAM("Kilograms", "kg", 1_000_000_000.0)
}

private enum class HouseholdUnit(val label: String, val symbol: String, val millilitresPerUnit: Double) {
    MILLILITRE("Millilitres", "mL", 1.0),
    TEASPOON("Teaspoons", "tsp", 4.92892159375),
    TABLESPOON("Tablespoons", "tbsp", 14.78676478125),
    FLUID_OUNCE("US fluid ounces", "fl oz", 29.5735295625)
}

private enum class Electrolyte(val symbol: String, val formulaWeight: Double, val valence: Int) {
    SODIUM("Na⁺", 22.98976928, 1),
    POTASSIUM("K⁺", 39.0983, 1),
    CALCIUM("Ca²⁺", 40.078, 2)
}

@Composable
fun UnitConversionsScreen() {
    val haptic = LocalHapticFeedback.current
    var mode by remember { mutableStateOf(ConvMode.METRIC) }
    var showGuide by remember { mutableStateOf(false) }
    var metricValue by remember { mutableStateOf("") }
    var metricUnit by remember { mutableStateOf(MetricUnit.MILLIGRAM) }
    var householdValue by remember { mutableStateOf("") }
    var householdUnit by remember { mutableStateOf(HouseholdUnit.MILLILITRE) }
    var electrolyte by remember { mutableStateOf(Electrolyte.POTASSIUM) }
    var mEqInput by remember { mutableStateOf("") }

    val metricResults = remember(metricValue, metricUnit) {
        metricValue.toDoubleOrNull()?.takeIf { it >= 0.0 }?.let { value ->
            MetricUnit.values().associateWith { unit -> roundDecimal(value * metricUnit.microgramsPerUnit / unit.microgramsPerUnit, 8) }
        }.orEmpty()
    }
    val householdResults = remember(householdValue, householdUnit) {
        householdValue.toDoubleOrNull()?.takeIf { it >= 0.0 }?.let { value ->
            HouseholdUnit.values().associateWith { unit -> roundDecimal(value * householdUnit.millilitresPerUnit / unit.millilitresPerUnit, 6) }
        }.orEmpty()
    }
    val electrolyteMg = remember(electrolyte, mEqInput) {
        mEqInput.toDoubleOrNull()?.takeIf { it >= 0.0 }?.let { mEq -> roundDecimal(mEq * electrolyte.formulaWeight / electrolyte.valence, 4) } ?: 0.0
    }

    if (showGuide) ConversionGuideDialog(mode) { showGuide = false }

    Column(
        modifier = Modifier.fillMaxSize().background(ConvBg).verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().background(ConvHeroGradient, RoundedCornerShape(28.dp)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("CLINICAL TOOL", color = Color.White.copy(alpha = 0.74f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Unit Conversions", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("Fast, transparent conversion workspace for bedside medication checks.", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(50.dp)) {
                        Text("●  CALCULATION READY", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(18.dp)) {
                    Text("↔", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(11.dp))
                }
            }
        }

        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(8.dp)) {
                Text("Choose a conversion", color = ConvSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    ConvMode.values().forEach { item ->
                        val active = item == mode
                        Surface(Modifier.weight(1f).height(52.dp).clickable {
                            mode = item
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }, color = if (active) ConvBlue else ConvBg, shape = RoundedCornerShape(15.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(item.emoji, fontSize = 17.sp)
                                Text(item.shortTitle, color = if (active) Color.White else ConvSlate, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        when (mode) {
            ConvMode.METRIC -> {
                ConversionPanel("STEP 01 · ENTER MASS", "Metric mass converter", "Enter a value and select its unit; all outputs are derived from the same base quantity.") {
                    UnitDropdownRow(metricUnit, MetricUnit.values().toList(), { it.label }, { it.symbol }) { metricUnit = it }
                    ConversionInput("Amount", metricValue, { metricValue = it }, metricUnit.symbol)
                }
                if (metricResults.isNotEmpty()) ResultsGrid("Converted values", MetricUnit.values().map { it.symbol to formatDecimal(metricResults.getValue(it), 8) })
            }
            ConvMode.HOUSEHOLD -> {
                ConversionPanel("STEP 01 · ENTER VOLUME", "Household volume converter", "Defined US customary factors are used. For medication administration, use the prescribed mL volume and a calibrated device.") {
                    UnitDropdownRow(householdUnit, HouseholdUnit.values().toList(), { it.label }, { it.symbol }) { householdUnit = it }
                    ConversionInput("Amount", householdValue, { householdValue = it }, householdUnit.symbol)
                }
                if (householdResults.isNotEmpty()) {
                    ResultsGrid("Converted values", HouseholdUnit.values().map { it.symbol to formatDecimal(householdResults.getValue(it), 6) })
                    Surface(color = ConvBlueSoft, shape = RoundedCornerShape(17.dp)) {
                        Text("Defined reference factors: 1 tsp = 4.92892159375 mL · 1 tbsp = 14.78676478125 mL · 1 US fl oz = 29.5735295625 mL.", Modifier.padding(13.dp), color = ConvBlue, fontSize = 10.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            ConvMode.ELECTROLYTE -> {
                ConversionPanel("STEP 01 · SELECT ION", "mEq ↔ mg converter", "Milliequivalent conversion uses formula weight and ionic valence.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        Electrolyte.values().forEach { ion ->
                            val active = ion == electrolyte
                            Surface(Modifier.weight(1f).height(48.dp).clickable { electrolyte = ion; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, color = if (active) ConvPurple else ConvPurpleSoft, shape = RoundedCornerShape(14.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text(ion.symbol, color = if (active) Color.White else ConvPurple, fontWeight = FontWeight.Black, fontSize = 15.sp) }
                            }
                        }
                    }
                    ConversionInput("Ordered amount", mEqInput, { mEqInput = it }, "mEq")
                    Surface(color = ConvBlueSoft, shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(13.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Formula weight", color = ConvSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(formatDecimal(electrolyte.formulaWeight, 6), color = ConvInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Valence", color = ConvSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(electrolyte.valence.toString(), color = ConvInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Button(onClick = { showGuide = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = ConvBlue)) {
                        Icon(Icons.Default.Info, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("How this conversion works", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
                if (electrolyteMg > 0.0) ResultHero("MILLIGRAMS", formatDecimal(electrolyteMg, 4), "mg", ConvPurpleSoft, ConvPurple)
            }
        }

        Surface(color = ConvAmberSoft, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⚠️  Clinical measurement check", color = Color(0xFF7A4A00), fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text("Verify the exact unit, formulation, concentration and prescription before administration. Household spoons should not be used when precise medication measurement is required.", color = Color(0xFF7A4A00), fontSize = 11.sp, lineHeight = 17.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun <T> UnitDropdownRow(selected: T, options: List<T>, label: (T) -> String, symbol: (T) -> String, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(Modifier.fillMaxWidth().clickable { expanded = true }, color = ConvBg, shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Input unit", color = ConvSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${label(selected)}  ·  ${symbol(selected)}", color = ConvInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Text("▾", color = ConvBlue, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text("${label(option)} (${symbol(option)})") }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}

@Composable
private fun ConversionInput(label: String, value: String, onValueChange: (String) -> Unit, unit: String) {
    OutlinedTextField(value = value, onValueChange = { if (it.length <= 18) onValueChange(it) }, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, suffix = { Text(unit, fontWeight = FontWeight.Bold, color = ConvBlue) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(17.dp))
}

@Composable
private fun ConversionPanel(eyebrow: String, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(eyebrow, color = ConvBlue, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Text(title, color = ConvInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = ConvSlate, fontSize = 11.sp, lineHeight = 17.sp)
            content()
        }
    }
}

@Composable
private fun ResultsGrid(title: String, results: List<Pair<String, String>>) {
    Surface(color = ConvBlueSoft, shape = RoundedCornerShape(23.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("RESULTS", color = ConvBlue, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Text(title, color = ConvInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                Text("READY", color = ConvBlue, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            results.forEach { (unit, value) ->
                Surface(color = Color.White, shape = RoundedCornerShape(15.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(unit, color = ConvSlate, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        Text(value, color = ConvInk, fontSize = 16.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHero(title: String, value: String, unit: String, background: Color, accent: Color) {
    Surface(color = background, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(19.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = ConvInk, fontSize = 43.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Text(unit, color = accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 7.dp))
            }
        }
    }
}

@Composable
private fun ConversionGuideDialog(mode: ConvMode, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.78f), shape = RoundedCornerShape(27.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(21.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${mode.emoji} ${mode.title}", color = ConvInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
                when (mode) {
                    ConvMode.METRIC -> Text("Metric mass is based on powers of ten: 1 mg = 1000 µg, 1 g = 1000 mg and 1 kg = 1000 g.", color = ConvSlate, fontSize = 14.sp, lineHeight = 20.sp)
                    ConvMode.HOUSEHOLD -> Text("Household volume output uses defined US customary factors. A calibrated mL device should be used when precise medication measurement is required.", color = ConvSlate, fontSize = 14.sp, lineHeight = 20.sp)
                    ConvMode.ELECTROLYTE -> Text("mEq = mg × valence ÷ formula weight, therefore mg = mEq × formula weight ÷ valence.", color = ConvSlate, fontSize = 14.sp, lineHeight = 20.sp)
                }
                Surface(color = ConvAmberSoft, shape = RoundedCornerShape(16.dp)) {
                    Text("Verify formulation, unit, concentration and prescription independently before administration.", Modifier.padding(14.dp), color = Color(0xFF7A4A00), fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Text("Close", fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

private fun roundDecimal(value: Double, places: Int): Double {
    val scale = Math.pow(10.0, places.toDouble())
    return round(value * scale) / scale
}

private fun formatDecimal(value: Double, places: Int): String {
    val rounded = roundDecimal(value, places)
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}
