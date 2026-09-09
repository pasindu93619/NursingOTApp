package com.pasindu.nursingotapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val IcuBg = Color(0xFFF7FAFF)
private val IcuInk = Color(0xFF14213D)
private val IcuSlate = Color(0xFF667085)
private val IcuBlue = Color(0xFF1769E8)
private val IcuCyan = Color(0xFF149FE3)
private val IcuIndigo = Color(0xFF4B78F2)
private val IcuPurple = Color(0xFF7B5CEB)
private val IcuRed = Color(0xFFEF4444)
private val IcuGreen = Color(0xFF10B981)
private val IcuAmber = Color(0xFFF59E0B)
private val IcuHero = Brush.horizontalGradient(listOf(IcuBlue, IcuCyan, IcuIndigo, IcuPurple))

data class IcuCalculatorDefinition(
    val title: String,
    val subtitle: String,
    val icon: String,
    val accent: Color,
    val description: String
)

@Composable
fun IcuCalculatorScreen(onNavigateBack: () -> Unit) {
    var selected by remember { mutableStateOf<IcuCalculatorDefinition?>(null) }
    BackHandler(enabled = selected != null) { selected = null }

    val calculators = remember {
        listOf(
            IcuCalculatorDefinition("Vasoactive Inotropes", "Pressors • inotropes • pump rate", "💉", IcuRed, "Dose and pump-rate conversion for vasoactive infusions."),
            IcuCalculatorDefinition("Sedation & Analgesia", "Sedation • analgesia • infusion", "🧠", IcuPurple, "Weight-based critical-care infusion-rate conversion."),
            IcuCalculatorDefinition("Electrolyte Protocols", "K⁺ • Mg²⁺ • replacement", "⚡", IcuGreen, "Infusion flow and hourly replacement calculations."),
            IcuCalculatorDefinition("Glycemic Control", "Insulin • syringe pump", "🩸", IcuCyan, "Insulin concentration and pump-rate calculation."),
            IcuCalculatorDefinition("Fluid Resuscitation", "Burns • fluids • Parkland", "💧", IcuBlue, "Parkland 24-hour volume and phase-rate calculation."),
            IcuCalculatorDefinition("Renal Function", "Creatinine clearance", "🫘", Color(0xFF6366F1), "Cockcroft-Gault creatinine clearance."),
            IcuCalculatorDefinition("Hemodynamics", "MAP • cardiac output • SVR", "❤️", IcuAmber, "Rapid MAP and SVR calculation.")
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(IcuBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            IcuHeader(selected = selected, onNavigateBack = onNavigateBack, onClose = { selected = null })
            if (selected == null) {
                IcuHome(calculators = calculators, onSelect = { selected = it })
            } else {
                IcuDetail(selected!!)
            }
        }
    }
}

@Composable
private fun IcuHeader(
    selected: IcuCalculatorDefinition?,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White, CircleShape)
                .clickable { if (selected == null) onNavigateBack() else onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected == null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                contentDescription = if (selected == null) "Back" else "Close",
                tint = IcuInk
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                selected?.title ?: "ICU Clinical Tools",
                color = IcuInk,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                if (selected == null) "Critical-care calculations, organized for bedside use" else "Focused bedside calculator",
                color = IcuBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun IcuHome(
    calculators: List<IcuCalculatorDefinition>,
    onSelect: (IcuCalculatorDefinition) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(Modifier.fillMaxWidth().background(IcuHero, RoundedCornerShape(28.dp)).padding(22.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text("CRITICAL CARE WORKSPACE", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, color = Color.White.copy(alpha = .75f))
                                Text("ICU calculations, without the clutter", fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(Modifier.height(4.dp))
                                Text("Choose the clinical task first. Enter only the values needed for that calculation.", fontSize = 11.sp, lineHeight = 16.sp, color = Color.White.copy(alpha = .88f))
                            }
                            Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) {
                                Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IcuStat("TOOLS", calculators.size.toString(), Modifier.weight(1f))
                            IcuStat("OFFLINE", "READY", Modifier.weight(1f))
                            IcuStat("MODE", "ICU", Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(alpha = .85f), shape = CircleShape) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = IcuAmber, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Bedside safety", color = IcuInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Verify patient weight, concentration, units and the local ICU protocol before administration.", color = IcuSlate, fontSize = 10.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Critical-care engines", color = IcuInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("Tap a workflow to focus on one calculation.", color = IcuSlate, fontSize = 11.sp)
                }
                Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = IcuBlue, modifier = Modifier.padding(8.dp))
                }
            }
        }
        items(calculators, key = { it.title }) { calculator ->
            IcuToolCard(calculator) { onSelect(calculator) }
        }
    }
}

@Composable
private fun IcuStat(title: String, value: String, modifier: Modifier) {
    Surface(modifier, color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(title, color = Color.White.copy(alpha = .68f), fontSize = 7.sp, fontWeight = FontWeight.Black)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun IcuToolCard(tool: IcuCalculatorDefinition, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).background(tool.accent.copy(alpha = .10f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Text(tool.icon, fontSize = 26.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(tool.title, color = IcuInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(tool.subtitle, color = tool.accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(tool.description, color = IcuSlate, fontSize = 10.sp, lineHeight = 15.sp)
            }
            Surface(color = tool.accent, shape = CircleShape) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.padding(9.dp).size(17.dp))
            }
        }
    }
}

@Composable
private fun IcuDetail(calculator: IcuCalculatorDefinition) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(color = calculator.accent.copy(alpha = .09f), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(calculator.accent.copy(alpha = .13f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                        Text(calculator.icon, fontSize = 21.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(calculator.subtitle, color = calculator.accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text("Focused bedside workspace", color = IcuInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        item {
            when (calculator.title) {
                "Vasoactive Inotropes" -> VasoactiveIcuCalculator()
                "Sedation & Analgesia" -> SedationIcuCalculator()
                "Electrolyte Protocols" -> ElectrolyteIcuCalculator()
                "Glycemic Control" -> GlycemicIcuCalculator()
                "Fluid Resuscitation" -> FluidIcuCalculator()
                "Renal Function" -> RenalIcuCalculator()
                "Hemodynamics" -> HemodynamicsIcuCalculator()
            }
        }
    }
}

@Composable
private fun VasoactiveIcuCalculator() {
    var dose by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("70") }
    var mg by remember { mutableStateOf("4") }
    var volume by remember { mutableStateOf("50") }
    val d = dose.toDoubleOrNull() ?: 0.0
    val w = weight.toDoubleOrNull() ?: 0.0
    val m = mg.toDoubleOrNull() ?: 0.0
    val v = volume.toDoubleOrNull() ?: 0.0
    val concentration = if (m > 0 && v > 0) m * 1000.0 / v else 0.0
    val rate = if (d > 0 && w > 0 && concentration > 0) d * w * 60.0 / concentration else 0.0
    CalculatorCard("Vasoactive dosing", "Dose → syringe-pump rate", IcuRed) {
        PairInput("Dose (mcg/kg/min)", dose, { value -> dose = value }, "Weight (kg)", weight, { value -> weight = value })
        PairInput("Drug amount (mg)", mg, { value -> mg = value }, "Final volume (mL)", volume, { value -> volume = value })
        ResultPanel(IcuRed, "Set pump to", String.format(Locale.US, "%.1f", rate), "mL/hr")
        FormulaText("Concentration = mg × 1000 ÷ mL; rate = dose × kg × 60 ÷ concentration")
        SafetyText("Confirm the exact drug, concentration, target dose and local titration protocol before use.")
    }
}

@Composable
private fun SedationIcuCalculator() {
    var dose by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("70") }
    var mg by remember { mutableStateOf("500") }
    var volume by remember { mutableStateOf("50") }
    val d = dose.toDoubleOrNull() ?: 0.0
    val w = weight.toDoubleOrNull() ?: 0.0
    val m = mg.toDoubleOrNull() ?: 0.0
    val v = volume.toDoubleOrNull() ?: 0.0
    val concentration = if (m > 0 && v > 0) m / v else 0.0
    val rate = if (d > 0 && w > 0 && concentration > 0) d * w / concentration else 0.0
    CalculatorCard("Sedation & analgesia", "Weight-based infusion setup", IcuPurple) {
        PairInput("Dose (mg/kg/hr)", dose, { value -> dose = value }, "Weight (kg)", weight, { value -> weight = value })
        PairInput("Drug amount (mg)", mg, { value -> mg = value }, "Final volume (mL)", volume, { value -> volume = value })
        ResultPanel(IcuPurple, "Set pump to", String.format(Locale.US, "%.1f", rate), "mL/hr")
        FormulaText("Rate = ordered dose × weight ÷ concentration")
        SafetyText("Verify the medication, concentration, target dose and patient-specific sedation/analgesia plan.")
    }
}

@Composable
private fun ElectrolyteIcuCalculator() {
    var dose by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("1") }
    var volume by remember { mutableStateOf("100") }
    val d = dose.toDoubleOrNull() ?: 0.0
    val h = hours.toDoubleOrNull() ?: 0.0
    val v = volume.toDoubleOrNull() ?: 0.0
    val flow = if (h > 0) v / h else 0.0
    val delivery = if (h > 0) d / h else 0.0
    CalculatorCard("Electrolyte replacement", "Infusion volume and hourly delivery", IcuGreen) {
        PairInput("Target dose (mEq or g)", dose, { value -> dose = value }, "Infusion time (hr)", hours, { value -> hours = value })
        NumberInput("Diluent volume (mL)", volume, { value -> volume = value })
        ResultPanel(IcuGreen, "Target flow rate", String.format(Locale.US, "%.1f", flow), "mL/hr")
        if (delivery > 0) SupportingValue("Dose delivery", String.format(Locale.US, "%.2f", delivery), "per hour", IcuGreen)
        FormulaText("Flow = diluent volume ÷ infusion time")
        SafetyText("Electrolyte limits and route-specific administration rates must follow the local ICU protocol.")
    }
}

@Composable
private fun GlycemicIcuCalculator() {
    var target by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("50") }
    var volume by remember { mutableStateOf("50") }
    val t = target.toDoubleOrNull() ?: 0.0
    val u = units.toDoubleOrNull() ?: 0.0
    val v = volume.toDoubleOrNull() ?: 0.0
    val concentration = if (v > 0) u / v else 0.0
    val rate = if (concentration > 0 && t > 0) t / concentration else 0.0
    CalculatorCard("Glycemic control", "Insulin concentration → pump rate", IcuCyan) {
        NumberInput("Target rate (Units/hr)", target, { value -> target = value })
        PairInput("Insulin (Units)", units, { value -> units = value }, "Final volume (mL)", volume, { value -> volume = value })
        ResultPanel(IcuCyan, "Set syringe pump to", String.format(Locale.US, "%.1f", rate), "mL/hr")
        FormulaText("Concentration = units ÷ volume; rate = target units/hr ÷ concentration")
        SafetyText("Verify the prescribed insulin preparation and glucose-management protocol before programming the pump.")
    }
}

@Composable
private fun FluidIcuCalculator() {
    var weight by remember { mutableStateOf("") }
    var tbsa by remember { mutableStateOf("") }
    val w = weight.toDoubleOrNull() ?: 0.0
    val b = tbsa.toDoubleOrNull() ?: 0.0
    val total = 4.0 * w * b
    val first8 = if (total > 0) total / 16.0 else 0.0
    val next16 = if (total > 0) total / 32.0 else 0.0
    CalculatorCard("Fluid resuscitation", "Parkland calculation workspace", IcuBlue) {
        PairInput("Weight (kg)", weight, { value -> weight = value }, "Burn TBSA (%)", tbsa, { value -> tbsa = value })
        ResultPanel(IcuBlue, "Total 24-hour volume", String.format(Locale.US, "%.0f", total), "mL")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SupportingValue("First 8 hr rate", String.format(Locale.US, "%.1f", first8), "mL/hr", IcuBlue, Modifier.weight(1f))
            SupportingValue("Next 16 hr rate", String.format(Locale.US, "%.1f", next16), "mL/hr", IcuBlue, Modifier.weight(1f))
        }
        FormulaText("Total = 4 × weight × TBSA; half in first 8 hr and half over next 16 hr")
        SafetyText("This is a formula workspace. Confirm TBSA method, elapsed time and current burn-resuscitation protocol before use.")
    }
}

@Composable
private fun RenalIcuCalculator() {
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("70") }
    var creatinine by remember { mutableStateOf("") }
    var male by remember { mutableStateOf(true) }
    val a = age.toDoubleOrNull() ?: 0.0
    val w = weight.toDoubleOrNull() ?: 0.0
    val c = creatinine.toDoubleOrNull() ?: 0.0
    val clearance = if (a > 0 && w > 0 && c > 0) ((140.0 - a) * w / (72.0 * c)) * if (male) 1.0 else 0.85 else 0.0
    CalculatorCard("Renal function", "Cockcroft-Gault creatinine clearance", Color(0xFF6366F1)) {
        PairInput("Age (yr)", age, { value -> age = value }, "Weight (kg)", weight, { value -> weight = value })
        NumberInput("Creatinine (mg/dL)", creatinine, { value -> creatinine = value })
        SimpleActionButton(if (male) "SEX: MALE" else "SEX: FEMALE", Color(0xFF6366F1)) { male = !male }
        ResultPanel(Color(0xFF6366F1), "Estimated clearance", String.format(Locale.US, "%.1f", clearance), "mL/min")
        FormulaText("CrCl = (140 − age) × weight ÷ (72 × serum creatinine); × 0.85 for female")
        SafetyText("Use the result as a calculation aid and apply the prescribing guideline appropriate to the patient and indication.")
    }
}

@Composable
private fun HemodynamicsIcuCalculator() {
    var systolic by remember { mutableStateOf("120") }
    var diastolic by remember { mutableStateOf("80") }
    var cardiacOutput by remember { mutableStateOf("5") }
    val sbp = systolic.toDoubleOrNull() ?: 0.0
    val dbp = diastolic.toDoubleOrNull() ?: 0.0
    val co = cardiacOutput.toDoubleOrNull() ?: 0.0
    val map = (sbp + 2.0 * dbp) / 3.0
    val svr = if (co > 0) ((map - 10.0) * 80.0) / co else 0.0
    CalculatorCard("Hemodynamics", "MAP and SVR review", IcuAmber) {
        PairInput("Systolic BP", systolic, { value -> systolic = value }, "Diastolic BP", diastolic, { value -> diastolic = value })
        NumberInput("Cardiac output (L/min)", cardiacOutput, { value -> cardiacOutput = value })
        ResultPanel(IcuAmber, "Mean arterial pressure", String.format(Locale.US, "%.0f", map), "mmHg")
        SupportingValue("Systemic vascular resistance", String.format(Locale.US, "%.0f", svr), "dyn·s/cm⁵", IcuAmber)
        FormulaText("MAP = (SBP + 2 × DBP) ÷ 3; SVR = (MAP − 10) × 80 ÷ cardiac output")
    }
}

@Composable
private fun CalculatorCard(title: String, subtitle: String, accent: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(accent.copy(alpha = .10f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = accent)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = IcuInk)
                    Text(subtitle, fontSize = 12.sp, color = IcuSlate)
                }
            }
            content()
        }
    }
}

@Composable
private fun PairInput(
    leftLabel: String,
    leftValue: String,
    onLeft: (String) -> Unit,
    rightLabel: String,
    rightValue: String,
    onRight: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberInput(leftLabel, leftValue, onLeft, Modifier.weight(1f))
        NumberInput(rightLabel, rightValue, onRight, Modifier.weight(1f))
    }
}

@Composable
private fun NumberInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(15.dp)
    )
}

@Composable
private fun ResultPanel(accent: Color, label: String, value: String, unit: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = .08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(accent.copy(alpha = .13f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = IcuSlate, letterSpacing = .7.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                    Spacer(Modifier.width(6.dp))
                    Text(unit, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent.copy(alpha = .75f))
                }
            }
        }
    }
}

@Composable
private fun SupportingValue(title: String, value: String, unit: String, accent: Color, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = IcuSlate)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IcuSlate)
            }
        }
    }
}

@Composable
private fun SimpleActionButton(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().background(accent, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FormulaText(text: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), elevation = CardDefaults.cardElevation(0.dp)) {
        Text("Formula · $text", Modifier.padding(14.dp), fontSize = 11.sp, color = IcuSlate, lineHeight = 17.sp)
    }
}

@Composable
private fun SafetyText(text: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E8)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = IcuAmber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 11.sp, lineHeight = 16.sp, color = IcuInk)
        }
    }
}
