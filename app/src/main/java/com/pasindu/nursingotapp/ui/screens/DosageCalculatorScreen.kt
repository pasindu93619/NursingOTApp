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

private val DosageBg = Color(0xFFF8FAFC)
private val DosageInk = Color(0xFF12204A)
private val DosageSlate = Color(0xFF64748B)
private val DosageBlue = Color(0xFF1769E8)
private val DosagePurple = Color(0xFF7B5CEB)
private val DosageBlueSoft = Color(0xFFEAF6FF)
private val DosagePurpleSoft = Color(0xFFF3EEFF)
private val DosageMintSoft = Color(0xFFEAFBF5)
private val DosageAmberSoft = Color(0xFFFFF6E7)
private val DosageHeroGradient = Brush.horizontalGradient(listOf(Color(0xFF1769E8), Color(0xFF149FE3), Color(0xFF4B78F2), Color(0xFF7B5CEB)))

enum class CalcMode(val title: String, val emoji: String, val shortTitle: String) {
    STANDARD("Standard dose", "💊", "Standard"),
    WEIGHT("Weight-based", "⚖️", "Mg/kg"),
    PERCENTAGE("Percentage solution", "💧", "% → mg"),
    DILUTION("Dilution", "🧪", "Dilute"),
    RECONSTITUTE("Reconstitution", "🫙", "Powder")
}

@Composable
fun DosageCalculatorScreen() {
    val haptic = LocalHapticFeedback.current
    var currentMode by remember { mutableStateOf(CalcMode.STANDARD) }
    var showGuideDialog by remember { mutableStateOf(false) }

    var orderedDose by remember { mutableStateOf("") }
    var availableDose by remember { mutableStateOf("") }
    var availableVolume by remember { mutableStateOf("") }
    var patientWeight by remember { mutableStateOf("") }
    var dosePerKg by remember { mutableStateOf("") }
    var targetConc by remember { mutableStateOf("") }
    var targetVol by remember { mutableStateOf("") }
    var stockConc by remember { mutableStateOf("") }
    var percentValue by remember { mutableStateOf("") }
    var percentTotalVol by remember { mutableStateOf("") }
    var vialPowderMg by remember { mutableStateOf("") }
    var diluentAddedMl by remember { mutableStateOf("") }
    var reconOrderedDose by remember { mutableStateOf("") }

    val calculatedMedVolume = remember(currentMode, orderedDose, availableDose, availableVolume, patientWeight, dosePerKg) {
        val have = availableDose.toFloatOrNull() ?: 0f
        val vol = availableVolume.toFloatOrNull() ?: 0f
        var desired = orderedDose.toFloatOrNull() ?: 0f
        if (currentMode == CalcMode.WEIGHT) desired = (patientWeight.toFloatOrNull() ?: 0f) * (dosePerKg.toFloatOrNull() ?: 0f)
        if (have > 0f && (currentMode == CalcMode.STANDARD || currentMode == CalcMode.WEIGHT)) round((desired / have) * vol * 100.0) / 100.0 else 0.0
    }

    val calculatedTargetDoseMg = remember(currentMode, patientWeight, dosePerKg) {
        if (currentMode == CalcMode.WEIGHT) {
            val weight = patientWeight.toFloatOrNull() ?: 0f
            val mgKg = dosePerKg.toFloatOrNull() ?: 0f
            round((weight * mgKg) * 100.0) / 100.0
        } else 0.0
    }

    val percentMgPerMl = remember(percentValue, currentMode) {
        if (currentMode == CalcMode.PERCENTAGE) {
            val p = percentValue.toFloatOrNull() ?: 0f
            round((p * 10f) * 100.0) / 100.0
        } else 0.0
    }

    val percentTotalGrams = remember(percentMgPerMl, percentTotalVol) {
        val vol = percentTotalVol.toFloatOrNull() ?: 0f
        if (vol > 0f) round((percentMgPerMl * vol / 1000f) * 100.0) / 100.0 else 0.0
    }

    var stockVolToDraw by remember { mutableFloatStateOf(0f) }
    var diluentVolToAdd by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(currentMode, targetConc, targetVol, stockConc) {
        if (currentMode == CalcMode.DILUTION) {
            val c2 = targetConc.toFloatOrNull() ?: 0f
            val v2 = targetVol.toFloatOrNull() ?: 0f
            val c1 = stockConc.toFloatOrNull() ?: 0f
            if (c1 > 0f && c1 >= c2) {
                stockVolToDraw = (round(((c2 * v2) / c1) * 100.0) / 100.0).toFloat()
                diluentVolToAdd = (round((v2 - stockVolToDraw) * 100.0) / 100.0).toFloat()
            } else {
                stockVolToDraw = 0f
                diluentVolToAdd = 0f
            }
        }
    }

    var reconConcMgMl by remember { mutableFloatStateOf(0f) }
    var reconDrawMl by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(currentMode, vialPowderMg, diluentAddedMl, reconOrderedDose) {
        if (currentMode == CalcMode.RECONSTITUTE) {
            val powder = vialPowderMg.toFloatOrNull() ?: 0f
            val diluent = diluentAddedMl.toFloatOrNull() ?: 0f
            val ordered = reconOrderedDose.toFloatOrNull() ?: 0f
            if (diluent > 0f) {
                reconConcMgMl = (round((powder / diluent) * 100.0) / 100.0).toFloat()
                reconDrawMl = if (reconConcMgMl > 0f) (round((ordered / reconConcMgMl) * 100.0) / 100.0).toFloat() else 0f
            } else {
                reconConcMgMl = 0f
                reconDrawMl = 0f
            }
        }
    }

    if (showGuideDialog) DosageGuideDialog(currentMode) { showGuideDialog = false }

    Column(
        Modifier
            .fillMaxSize()
            .background(DosageBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.fillMaxWidth().background(DosageHeroGradient, RoundedCornerShape(26.dp)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("CLINICAL TOOL", color = Color.White.copy(alpha = 0.74f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Advanced Dosage", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("Five focused dose workflows. Clear math. Fewer taps.", color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp, lineHeight = 16.sp)
                }
                Surface(color = Color.White.copy(alpha = 0.15f), shape = CircleShape) { Text("💊", fontSize = 25.sp, modifier = Modifier.padding(10.dp)) }
            }
        }

        ModeSelector(currentMode) {
            currentMode = it
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        when (currentMode) {
            CalcMode.STANDARD -> StandardDosePanel(orderedDose, { orderedDose = it }, availableDose, { availableDose = it }, availableVolume, { availableVolume = it }, calculatedMedVolume, { showGuideDialog = true })
            CalcMode.WEIGHT -> WeightDosePanel(patientWeight, { patientWeight = it }, dosePerKg, { dosePerKg = it }, calculatedTargetDoseMg, availableDose, { availableDose = it }, availableVolume, { availableVolume = it }, calculatedMedVolume, { showGuideDialog = true })
            CalcMode.PERCENTAGE -> PercentagePanel(percentValue, { percentValue = it }, percentTotalVol, { percentTotalVol = it }, percentMgPerMl, percentTotalGrams)
            CalcMode.DILUTION -> DilutionPanel(targetConc, { targetConc = it }, targetVol, { targetVol = it }, stockConc, { stockConc = it }, stockVolToDraw, diluentVolToAdd)
            CalcMode.RECONSTITUTE -> ReconstitutionPanel(vialPowderMg, { vialPowderMg = it }, diluentAddedMl, { diluentAddedMl = it }, reconOrderedDose, { reconOrderedDose = it }, reconConcMgMl, reconDrawMl)
        }

        Surface(color = DosageAmberSoft, shape = RoundedCornerShape(18.dp)) {
            Text("⚠️  Arithmetic support only. Confirm the prescription, units, preparation details and local protocol before administration.", Modifier.fillMaxWidth().padding(14.dp), color = Color(0xFF7A4A00), fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun ModeSelector(selected: CalcMode, onSelected: (CalcMode) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Choose a calculation", color = DosageSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
            val rows = CalcMode.values().toList().chunked(3)
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { mode ->
                        val active = mode == selected
                        Surface(Modifier.weight(1f).height(48.dp).clickable { onSelected(mode) }, shape = RoundedCornerShape(14.dp), color = if (active) DosageBlue else DosageBg) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(mode.emoji, fontSize = 16.sp)
                                Text(mode.shortTitle, color = if (active) Color.White else DosageSlate, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun StandardDosePanel(ordered: String, onOrdered: (String) -> Unit, available: String, onAvailable: (String) -> Unit, volume: String, onVolume: (String) -> Unit, result: Double, onGuide: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard("Standard dose", "Desired ÷ available × volume") {
            InputField("Desired dose (D)", ordered, onOrdered)
            InputField("Available dose (H)", available, onAvailable)
            InputField("Available volume (Q)", volume, onVolume)
        }
        ResultCard("DRAW VOLUME", formatNumber(result), "mL", DosageBlueSoft, DosageBlue)
        GuideButton(onGuide)
    }
}

@Composable
private fun WeightDosePanel(weight: String, onWeight: (String) -> Unit, dose: String, onDose: (String) -> Unit, targetDose: Double, availableDose: String, onAvailableDose: (String) -> Unit, availableVolume: String, onAvailableVolume: (String) -> Unit, medVolume: Double, onGuide: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard("Weight-based dose", "Calculate the required dose first") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InputField("Weight (kg)", weight, onWeight, Modifier.weight(1f))
                InputField("Dose (mg/kg)", dose, onDose, Modifier.weight(1f))
            }
        }
        ResultCard("CALCULATED DOSE", formatNumber(targetDose), "mg", DosageMintSoft, Color(0xFF087F5B))
        SectionCard("Optional draw-volume conversion", "Use the available strength and volume") {
            InputField("Available dose (mg)", availableDose, onAvailableDose)
            InputField("Available volume (mL)", availableVolume, onAvailableVolume)
        }
        ResultCard("DRAW VOLUME", formatNumber(medVolume), "mL", DosageBlueSoft, DosageBlue)
        GuideButton(onGuide)
    }
}

@Composable
private fun PercentagePanel(percent: String, onPercent: (String) -> Unit, totalVolume: String, onTotalVolume: (String) -> Unit, mgPerMl: Double, totalGrams: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard("Percentage solution", "% w/v → mg/mL and total grams") {
            InputField("Concentration (%)", percent, onPercent)
            InputField("Total volume (mL)", totalVolume, onTotalVolume)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultCard("STRENGTH", formatNumber(mgPerMl), "mg/mL", DosageBlueSoft, DosageBlue, Modifier.weight(1f))
            ResultCard("TOTAL DRUG", formatNumber(totalGrams), "g", DosagePurpleSoft, DosagePurple, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DilutionPanel(target: String, onTarget: (String) -> Unit, targetVolume: String, onTargetVolume: (String) -> Unit, stock: String, onStock: (String) -> Unit, stockDraw: Float, diluent: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard("Dilution", "C₁V₁ = C₂V₂") {
            InputField("Target concentration (C₂)", target, onTarget)
            InputField("Target volume (V₂, mL)", targetVolume, onTargetVolume)
            InputField("Stock concentration (C₁)", stock, onStock)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultCard("DRAW STOCK", formatNumber(stockDraw.toDouble()), "mL", DosageBlueSoft, DosageBlue, Modifier.weight(1f))
            ResultCard("ADD DILUENT", formatNumber(diluent.toDouble()), "mL", DosageMintSoft, Color(0xFF087F5B), Modifier.weight(1f))
        }
        Surface(color = DosagePurpleSoft, shape = RoundedCornerShape(18.dp)) { Text("Mixed-active alligation calculations are intentionally not included.", Modifier.padding(14.dp), color = Color(0xFF5B42B5), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ReconstitutionPanel(powder: String, onPowder: (String) -> Unit, diluent: String, onDiluent: (String) -> Unit, orderedDose: String, onOrderedDose: (String) -> Unit, concentration: Float, drawVolume: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard("Powder reconstitution", "Find concentration, then draw volume") {
            InputField("Vial powder (mg)", powder, onPowder)
            InputField("Diluent added (mL)", diluent, onDiluent)
            InputField("Ordered dose (mg)", orderedDose, onOrderedDose)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultCard("RECONSTITUTED", formatNumber(concentration.toDouble()), "mg/mL", DosageBlueSoft, DosageBlue, Modifier.weight(1f))
            ResultCard("DRAW VOLUME", formatNumber(drawVolume.toDouble()), "mL", DosageMintSoft, Color(0xFF087F5B), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(title, color = DosageInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = DosageSlate, fontSize = 11.sp, lineHeight = 16.sp)
            content()
        }
    }
}

@Composable
private fun InputField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
}

@Composable
private fun ResultCard(title: String, value: String, unit: String, background: Color, accent: Color, modifier: Modifier = Modifier) {
    Surface(Modifier.fillMaxWidth().then(modifier), color = background, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = DosageInk, fontSize = 37.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Text(unit, color = accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

@Composable
private fun GuideButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = DosageBlue)) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text("How this calculation works", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
    }
}

@Composable
private fun DosageGuideDialog(mode: CalcMode, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.78f), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${mode.emoji} ${mode.title}", fontSize = 23.sp, fontWeight = FontWeight.Black, color = DosageInk)
                when (mode) {
                    CalcMode.STANDARD -> GuideText("Formula", "Desired ÷ Available × Volume")
                    CalcMode.WEIGHT -> GuideText("Workflow", "Weight × prescribed mg/kg gives the required dose. Available strength and volume can then convert that dose to a draw volume.")
                    CalcMode.PERCENTAGE -> GuideText("Conversion", "The current calculator converts percentage strength to mg/mL and total grams using the existing deterministic calculation.")
                    CalcMode.DILUTION -> GuideText("Formula", "C₁V₁ = C₂V₂ using the entered stock, target concentration and target volume.")
                    CalcMode.RECONSTITUTE -> GuideText("Workflow", "Powder ÷ diluent volume gives reconstituted mg/mL; ordered dose ÷ concentration gives the draw volume.")
                }
                Surface(color = DosageAmberSoft, shape = RoundedCornerShape(16.dp)) { Text("Verify the prescription, preparation instructions, units and local protocol independently before administration.", Modifier.padding(14.dp), color = Color(0xFF7A4A00), fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp) }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Text("Close", fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun GuideText(title: String, body: String) {
    Text(title, color = DosageBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
    Text(body, color = DosageSlate, fontSize = 14.sp, lineHeight = 20.sp)
}

private fun formatNumber(value: Double): String = when {
    value == 0.0 -> "0"
    value % 1.0 == 0.0 -> value.toInt().toString()
    else -> value.toString()
}
