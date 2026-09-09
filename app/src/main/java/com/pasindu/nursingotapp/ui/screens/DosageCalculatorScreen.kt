package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val DoseBg = Color(0xFFF6F9FD)
private val DoseInk = Color(0xFF12204A)
private val DoseSlate = Color(0xFF64748B)
private val DoseBlue = Color(0xFF1769E8)
private val DoseCyan = Color(0xFF149FE3)
private val DosePurple = Color(0xFF7B5CEB)
private val DoseGreen = Color(0xFF0A9B72)
private val DoseAmber = Color(0xFFE58A00)
private val DoseRed = Color(0xFFD92D4F)
private val DoseSoftBlue = Color(0xFFEAF6FF)
private val DoseSoftGreen = Color(0xFFEAFBF5)
private val DoseSoftAmber = Color(0xFFFFF6E7)
private val DoseSoftRed = Color(0xFFFFEEF1)
private val DoseHero = Brush.linearGradient(listOf(DoseBlue, DoseCyan, Color(0xFF4B78F2), DosePurple))

enum class CalcMode(val title: String, val shortTitle: String) {
    STANDARD("Standard dose", "Standard"),
    WEIGHT("Weight-based dose", "mg/kg"),
    PERCENTAGE("Percentage solution", "%"),
    DILUTION("Dilution", "Dilution"),
    RECONSTITUTE("Reconstitution", "Vial")
}

@Composable
fun DosageCalculatorScreen() {
    var mode by remember { mutableStateOf(CalcMode.STANDARD) }
    var showInfo by remember { mutableStateOf(false) }

    var desired by remember { mutableStateOf("") }
    var available by remember { mutableStateOf("") }
    var suppliedVolume by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var dosePerKg by remember { mutableStateOf("") }
    var percent by remember { mutableStateOf("") }
    var percentVolume by remember { mutableStateOf("") }
    var stockConcentration by remember { mutableStateOf("") }
    var targetConcentration by remember { mutableStateOf("") }
    var finalVolume by remember { mutableStateOf("") }
    var vialDrugAmount by remember { mutableStateOf("") }
    var reconstitutedFinalVolume by remember { mutableStateOf("") }
    var reconstitutionDose by remember { mutableStateOf("") }

    val standardResult = calculateStandard(desired, available, suppliedVolume)
    val weightDose = calculateWeightDose(weight, dosePerKg)
    val weightDraw = calculateStandard(weightDose, available, suppliedVolume)
    val percentMgMl = calculatePercentageMgMl(percent)
    val percentGrams = calculatePercentageTotalGrams(percentMgMl, percentVolume)
    val dilutionStockDraw = calculateDilutionDraw(stockConcentration, targetConcentration, finalVolume)
    val dilutionSolvent = if (dilutionStockDraw != null) {
        val total = finalVolume.toDoubleOrNull()
        total?.let { it - dilutionStockDraw }
    } else null
    val reconstitutionConcentration = calculateReconstitutionConcentration(vialDrugAmount, reconstitutedFinalVolume)
    val reconstitutionDraw = calculateReconstitutionDraw(reconstitutionDose, reconstitutionConcentration)

    if (showInfo) {
        DosageInfoDialog(mode) { showInfo = false }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DoseBg)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DoseHero(mode, onInfo = { showInfo = true })
        ModeRail(mode) { mode = it }
        WorkflowLabel(mode)

        when (mode) {
            CalcMode.STANDARD -> StandardWorkspace(desired, { desired = it }, available, { available = it }, suppliedVolume, { suppliedVolume = it }, standardResult)
            CalcMode.WEIGHT -> WeightWorkspace(weight, { weight = it }, dosePerKg, { dosePerKg = it }, weightDose, available, { available = it }, suppliedVolume, { suppliedVolume = it }, weightDraw)
            CalcMode.PERCENTAGE -> PercentageWorkspace(percent, { percent = it }, percentVolume, { percentVolume = it }, percentMgMl, percentGrams)
            CalcMode.DILUTION -> DilutionWorkspace(stockConcentration, { stockConcentration = it }, targetConcentration, { targetConcentration = it }, finalVolume, { finalVolume = it }, dilutionStockDraw, dilutionSolvent)
            CalcMode.RECONSTITUTE -> ReconstitutionWorkspace(vialDrugAmount, { vialDrugAmount = it }, reconstitutedFinalVolume, { reconstitutedFinalVolume = it }, reconstitutionDose, { reconstitutionDose = it }, reconstitutionConcentration, reconstitutionDraw)
        }

        SafetyPanel(mode)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DoseHero(mode: CalcMode, onInfo: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(Modifier.fillMaxWidth().background(DoseHero, RoundedCornerShape(28.dp)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("MEDICATION CALCULATION", color = Color.White.copy(.72f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Advanced Dosage", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${mode.title} · calculate first, verify before administration", color = Color.White.copy(.88f), fontSize = 11.sp, lineHeight = 16.sp)
                }
                IconButton(onClick = onInfo, modifier = Modifier.background(Color.White.copy(.14f), RoundedCornerShape(14.dp))) {
                    Icon(Icons.Default.Info, contentDescription = "Dosage information", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ModeRail(selected: CalcMode, onSelected: (CalcMode) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, null, tint = DoseBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Choose calculation", color = DoseInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CalcMode.values().forEach { item ->
                    val active = item == selected
                    Surface(
                        Modifier.width(94.dp).height(56.dp).clickable { onSelected(item) },
                        color = if (active) DoseBlue else DoseBg,
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(modeSymbol(item), fontSize = 18.sp)
                            Text(item.shortTitle, color = if (active) Color.White else DoseSlate, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowLabel(mode: CalcMode) {
    Column(Modifier.padding(horizontal = 2.dp)) {
        Text("STEP-BY-STEP WORKSPACE", color = DoseBlue, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
        Text(mode.title, color = DoseInk, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text(workflowHint(mode), color = DoseSlate, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun StandardWorkspace(desired: String, onDesired: (String) -> Unit, available: String, onAvailable: (String) -> Unit, volume: String, onVolume: (String) -> Unit, result: Double?) {
    StepCard("01", "Prescription", "Keep desired and available dose in the same unit") {
        DoseInput("Desired dose", desired, onDesired, "dose unit", "e.g. 500")
        DoseInput("Available dose", available, onAvailable, "same unit", "e.g. 250")
        DoseInput("Supplied volume", volume, onVolume, "mL", "e.g. 5")
    }
    ResultPanel("DRAW VOLUME", result, "mL", DoseSoftBlue, DoseBlue)
    FormulaPanel("Formula", "(Desired dose ÷ Available dose) × Supplied volume")
}

@Composable
private fun WeightWorkspace(weight: String, onWeight: (String) -> Unit, dose: String, onDose: (String) -> Unit, calculatedDose: Double?, available: String, onAvailable: (String) -> Unit, volume: String, onVolume: (String) -> Unit, draw: Double?) {
    StepCard("01", "Patient-specific dose", "Calculate the ordered dose from validated weight") {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DoseInput("Weight", weight, onWeight, "kg", "e.g. 20", Modifier.weight(1f))
            DoseInput("Dose", dose, onDose, "mg/kg", "e.g. 10", Modifier.weight(1f))
        }
    }
    ResultPanel("CALCULATED DOSE", calculatedDose, "mg", DoseSoftGreen, DoseGreen)
    StepCard("02", "Draw conversion", "Use the actual medication concentration on the label") {
        DoseInput("Available dose", available, onAvailable, "mg", "e.g. 100")
        DoseInput("Supplied volume", volume, onVolume, "mL", "e.g. 2")
    }
    ResultPanel("DRAW VOLUME", draw, "mL", DoseSoftBlue, DoseBlue)
}

@Composable
private fun PercentageWorkspace(percent: String, onPercent: (String) -> Unit, volume: String, onVolume: (String) -> Unit, mgMl: Double?, grams: Double?) {
    StepCard("01", "Percentage concentration", "This conversion assumes a % w/v solution") {
        DoseInput("Concentration", percent, onPercent, "% w/v", "e.g. 5")
        DoseInput("Total volume", volume, onVolume, "mL", "e.g. 100")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        ResultPanel("CONCENTRATION", mgMl, "mg/mL", DoseSoftBlue, DoseBlue, Modifier.weight(1f))
        ResultPanel("TOTAL DRUG", grams, "g", DoseSoftGreen, DoseGreen, Modifier.weight(1f))
    }
    FormulaPanel("Conversion", "1% w/v = 10 mg/mL")
}

@Composable
private fun DilutionWorkspace(stock: String, onStock: (String) -> Unit, target: String, onTarget: (String) -> Unit, volume: String, onVolume: (String) -> Unit, draw: Double?, solvent: Double?) {
    StepCard("01", "Dilution setup", "C1V1 = C2V2 · target concentration must not exceed stock") {
        DoseInput("Stock concentration", stock, onStock, "same unit", "e.g. 10")
        DoseInput("Target concentration", target, onTarget, "same unit", "e.g. 2")
        DoseInput("Final volume", volume, onVolume, "mL", "e.g. 100")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        ResultPanel("STOCK TO DRAW", draw, "mL", DoseSoftBlue, DoseBlue, Modifier.weight(1f))
        ResultPanel("DILUENT", solvent, "mL", DoseSoftGreen, DoseGreen, Modifier.weight(1f))
    }
    FormulaPanel("Formula", "Stock volume = (Target concentration × Final volume) ÷ Stock concentration")
}

@Composable
private fun ReconstitutionWorkspace(vialAmount: String, onVialAmount: (String) -> Unit, finalVolume: String, onFinalVolume: (String) -> Unit, ordered: String, onOrdered: (String) -> Unit, concentration: Double?, draw: Double?) {
    StepCard("01", "Manufacturer-defined final volume", "Use the final volume stated for the actual product after reconstitution") {
        DoseInput("Drug in vial", vialAmount, onVialAmount, "mg", "e.g. 1000")
        DoseInput("Final volume", finalVolume, onFinalVolume, "mL", "e.g. 10")
        DoseInput("Ordered dose", ordered, onOrdered, "mg", "e.g. 250")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        ResultPanel("FINAL CONCENTRATION", concentration, "mg/mL", DoseSoftBlue, DoseBlue, Modifier.weight(1f))
        ResultPanel("DRAW VOLUME", draw, "mL", DoseSoftGreen, DoseGreen, Modifier.weight(1f))
    }
    FormulaPanel("Formula", "Final concentration = vial drug amount ÷ final volume; draw = ordered dose ÷ concentration")
}

@Composable
private fun StepCard(step: String, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = DoseBlue, shape = RoundedCornerShape(10.dp)) {
                    Text(step, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = DoseInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = DoseSlate, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
            content()
        }
    }
}

@Composable
private fun DoseInput(label: String, value: String, onValueChange: (String) -> Unit, suffix: String, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> if (text.length <= 12 && text.matches(Regex("^\\d*\\.?\\d*$"))) onValueChange(text) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = DoseSlate.copy(.55f)) },
        suffix = { Text(suffix, color = DoseBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(15.dp)
    )
}

@Composable
private fun ResultPanel(title: String, value: Double?, unit: String, background: Color, accent: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = background, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
            if (value == null || !value.isFinite()) {
                Text("—", color = DoseSlate, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("Enter valid values", color = DoseSlate, fontSize = 9.sp)
            } else {
                Text(formatNumber(value), color = DoseInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(unit, color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun FormulaPanel(title: String, text: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Calculate, null, tint = DosePurple, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = DoseInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(text, color = DoseSlate, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun SafetyPanel(mode: CalcMode) {
    Surface(color = DoseSoftAmber, shape = RoundedCornerShape(19.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.WarningAmber, null, tint = DoseAmber, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("VERIFY BEFORE ADMINISTRATION", color = DoseInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(safetyText(mode), color = Color(0xFF765000), fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun DosageInfoDialog(mode: CalcMode, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = DoseBlue, fontWeight = FontWeight.Bold) } },
        icon = { Icon(Icons.Default.Medication, null, tint = DoseBlue) },
        title = { Text(mode.title, color = DoseInk, fontWeight = FontWeight.Black) },
        text = { Text(infoText(mode), color = DoseSlate, fontSize = 12.sp, lineHeight = 18.sp) },
        containerColor = Color.White
    )
}

private fun calculateStandard(desired: String, available: String, volume: String): Double? {
    val d = desired.toDoubleOrNull() ?: return null
    val a = available.toDoubleOrNull() ?: return null
    val v = volume.toDoubleOrNull() ?: return null
    if (d <= 0 || a <= 0 || v <= 0) return null
    return (d / a) * v
}

private fun calculateWeightDose(weight: String, dosePerKg: String): Double? {
    val w = weight.toDoubleOrNull() ?: return null
    val d = dosePerKg.toDoubleOrNull() ?: return null
    if (w <= 0 || d <= 0) return null
    return w * d
}

private fun calculatePercentageMgMl(percent: String): Double? {
    val p = percent.toDoubleOrNull() ?: return null
    if (p <= 0) return null
    return p * 10.0
}

private fun calculatePercentageTotalGrams(mgMl: Double?, volume: String): Double? {
    val concentration = mgMl ?: return null
    val v = volume.toDoubleOrNull() ?: return null
    if (v <= 0) return null
    return concentration * v / 1000.0
}

private fun calculateDilutionDraw(stock: String, target: String, finalVolume: String): Double? {
    val c1 = stock.toDoubleOrNull() ?: return null
    val c2 = target.toDoubleOrNull() ?: return null
    val v2 = finalVolume.toDoubleOrNull() ?: return null
    if (c1 <= 0 || c2 <= 0 || v2 <= 0 || c2 > c1) return null
    return (c2 * v2) / c1
}

private fun calculateReconstitutionConcentration(amount: String, finalVolume: String): Double? {
    val drug = amount.toDoubleOrNull() ?: return null
    val volume = finalVolume.toDoubleOrNull() ?: return null
    if (drug <= 0 || volume <= 0) return null
    return drug / volume
}

private fun calculateReconstitutionDraw(ordered: String, concentration: Double?): Double? {
    val dose = ordered.toDoubleOrNull() ?: return null
    if (dose <= 0 || concentration == null || concentration <= 0) return null
    return dose / concentration
}

private fun formatNumber(value: Double): String {
    return String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}

private fun modeSymbol(mode: CalcMode): String = when (mode) {
    CalcMode.STANDARD -> "💊"
    CalcMode.WEIGHT -> "⚖️"
    CalcMode.PERCENTAGE -> "💧"
    CalcMode.DILUTION -> "🧪"
    CalcMode.RECONSTITUTE -> "🫙"
}

private fun workflowHint(mode: CalcMode): String = when (mode) {
    CalcMode.STANDARD -> "Convert the prescription into a measurable volume."
    CalcMode.WEIGHT -> "Weight → dose → medication volume."
    CalcMode.PERCENTAGE -> "Convert % w/v into mg/mL and total drug."
    CalcMode.DILUTION -> "Prepare a target concentration from a stronger stock."
    CalcMode.RECONSTITUTE -> "Use the manufacturer's final volume, then calculate the draw."
}

private fun safetyText(mode: CalcMode): String = when (mode) {
    CalcMode.STANDARD -> "Confirm medication, prescribed dose, available strength, units and supplied volume."
    CalcMode.WEIGHT -> "Use a validated current weight. Confirm dose units and maximum dose from the applicable protocol."
    CalcMode.PERCENTAGE -> "Confirm that the product is expressed as % w/v before using this conversion."
    CalcMode.DILUTION -> "Confirm compatible diluent, stock concentration, target concentration and final volume."
    CalcMode.RECONSTITUTE -> "Use the product manufacturer's stated reconstitution instructions and final volume."
}

private fun infoText(mode: CalcMode): String = when (mode) {
    CalcMode.STANDARD -> "Standard dose: (desired dose ÷ available dose) × supplied volume. Desired and available dose must use the same unit."
    CalcMode.WEIGHT -> "Weight-based dose: weight × prescribed dose per kg. The resulting dose can then be converted to a medication volume."
    CalcMode.PERCENTAGE -> "For % w/v solutions, 1% equals 1 g/100 mL, which is 10 mg/mL."
    CalcMode.DILUTION -> "Dilution uses C1V1 = C2V2. The target concentration must not exceed the stock concentration."
    CalcMode.RECONSTITUTE -> "Reconstitution uses the manufacturer's final volume: concentration = drug amount ÷ final volume; draw volume = ordered dose ÷ concentration."
}
