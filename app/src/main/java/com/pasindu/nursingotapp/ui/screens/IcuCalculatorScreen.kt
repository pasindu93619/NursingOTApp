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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

private val IcuScreenBg = Color(0xFFF7FAFF)
private val IcuScreenNavy = Color(0xFF14213D)
private val IcuScreenSlate = Color(0xFF667085)
private val IcuScreenBlue = Color(0xFF1769E8)
private val IcuScreenCyan = Color(0xFF149FE3)
private val IcuScreenIndigo = Color(0xFF4B78F2)
private val IcuScreenPurple = Color(0xFF7B5CEB)
private val IcuScreenAmber = Color(0xFFF59E0B)
private val IcuScreenRed = Color(0xFFEF4444)
private val IcuScreenGreen = Color(0xFF10B981)
private val IcuScreenHero = Brush.horizontalGradient(listOf(IcuScreenBlue, IcuScreenCyan, IcuScreenIndigo, IcuScreenPurple))

data class IcuCalculatorDefinition(val title: String, val subtitle: String, val icon: String, val accent: Color, val description: String)

@Composable
fun IcuCalculatorScreen(onNavigateBack: () -> Unit) {
    var selected by remember { mutableStateOf<IcuCalculatorDefinition?>(null) }
    var selectedDrug by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = selected != null) { selected = null }

    val calculators = remember {
        listOf(
            IcuCalculatorDefinition("Vasoactive Inotropes", "Pressors • inotropes • pump rate", "💉", IcuScreenRed, "Dose ↔ rate conversion for vasoactive infusions."),
            IcuCalculatorDefinition("Sedation & Analgesia", "Sedation • analgesia • infusion", "🧠", IcuScreenPurple, "Weight-based infusion-rate conversion."),
            IcuCalculatorDefinition("Electrolyte Protocols", "K⁺ • Mg²⁺ • replacement", "⚡", IcuScreenGreen, "Infusion flow and hourly replacement calculations."),
            IcuCalculatorDefinition("Glycemic Control", "Insulin • syringe pump", "🩸", IcuScreenCyan, "Insulin concentration and pump-rate calculation."),
            IcuCalculatorDefinition("Fluid Resuscitation", "Burns • fluids • Parkland", "💧", IcuScreenBlue, "Parkland 24-hour volume and phase rates."),
            IcuCalculatorDefinition("Renal Function", "Creatinine clearance", "🫘", Color(0xFF6366F1), "Cockcroft-Gault creatinine clearance."),
            IcuCalculatorDefinition("Hemodynamics", "MAP • cardiac output • SVR", "❤️", IcuScreenAmber, "Rapid MAP and SVR calculations.")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(selected?.title ?: "ICU Clinical Tools", fontSize = 20.sp, fontWeight = FontWeight.Black, color = IcuScreenNavy)
                        Text(if (selected == null) "Critical-care calculations, organized for bedside use" else "Focused bedside calculator", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IcuScreenBlue)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (selected == null) onNavigateBack() else selected = null }) {
                        Icon(if (selected == null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close, contentDescription = "Back", tint = IcuScreenNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IcuScreenBg)
            )
        },
        containerColor = IcuScreenBg
    ) { padding ->
        if (selected == null) {
            IcuCalculatorHome(calculators = calculators, contentPadding = padding) { selected = it }
        } else {
            IcuCalculatorDetail(calculator = selected!!, contentPadding = padding) { selectedDrug = it }
        }
    }

    selectedDrug?.let { drug -> IcuDrugDialog(drug, onDismiss = { selectedDrug = null }) }
}

@Composable
private fun IcuCalculatorHome(calculators: List<IcuCalculatorDefinition>, contentPadding: PaddingValues, onSelect: (IcuCalculatorDefinition) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(3.dp)) {
            Box(Modifier.fillMaxWidth().background(IcuScreenHero, RoundedCornerShape(28.dp)).padding(22.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("CRITICAL CARE WORKSPACE", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, color = Color.White.copy(alpha = .75f))
                            Text("ICU calculations, without the clutter", fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text("Select the clinical task first. Enter only the values needed for that calculation.", fontSize = 11.sp, lineHeight = 16.sp, color = Color.White.copy(alpha = .88f))
                        }
                        Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) {
                            Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricChip("TOOLS", "7", Modifier.weight(1f))
                        MetricChip("OFFLINE", "READY", Modifier.weight(1f))
                        MetricChip("MODE", "ICU", Modifier.weight(1f))
                    }
                }
            }
        }

        SafetyStrip()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Critical-care engines", fontSize = 17.sp, fontWeight = FontWeight.Black, color = IcuScreenNavy)
                Text("Tap a workflow to focus on one calculation.", fontSize = 10.sp, color = IcuScreenSlate)
            }
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = IcuScreenBlue, modifier = Modifier.padding(9.dp))
            }
        }

        calculators.forEach { item -> CalculatorTile(item) { onSelect(item) } }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun MetricChip(title: String, value: String, modifier: Modifier) {
    Surface(modifier, color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(title, fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = .68f))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
private fun SafetyStrip() {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E8)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color.White, shape = CircleShape) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = IcuScreenAmber, modifier = Modifier.padding(9.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Bedside safety", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = IcuScreenNavy)
                Text("Verify patient weight, concentration, units and local ICU protocol before administration.", fontSize = 10.sp, lineHeight = 15.sp, color = IcuScreenSlate)
            }
        }
    }
}

@Composable
private fun CalculatorTile(item: IcuCalculatorDefinition, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).background(item.accent.copy(alpha = .10f), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                Text(item.icon, fontSize = 26.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = IcuScreenNavy)
                Text(item.subtitle, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = item.accent)
                Spacer(Modifier.height(4.dp))
                Text(item.description, fontSize = 10.sp, lineHeight = 15.sp, color = IcuScreenSlate)
            }
            Surface(color = item.accent, shape = CircleShape) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.padding(9.dp).size(17.dp))
            }
        }
    }
}

@Composable
private fun IcuCalculatorDetail(calculator: IcuCalculatorDefinition, contentPadding: PaddingValues, onDrugClick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Surface(color = calculator.accent.copy(alpha = .09f), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(calculator.accent.copy(alpha = .14f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Text(calculator.icon, fontSize = 20.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(calculator.subtitle, fontSize = 9.sp, fontWeight = FontWeight.Black, color = calculator.accent)
                    Text("Focused clinical workspace", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = IcuScreenNavy)
                }
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = calculator.accent, modifier = Modifier.size(19.dp))
            }
        }

        when (calculator.title) {
            "Vasoactive Inotropes" -> VasoactiveNewCalculator(calculator, onDrugClick)
            "Sedation & Analgesia" -> SedationNewCalculator(calculator, onDrugClick)
            "Electrolyte Protocols" -> ElectrolyteNewCalculator(calculator, onDrugClick)
            "Glycemic Control" -> GlycemicNewCalculator(calculator, onDrugClick)
            "Fluid Resuscitation" -> FluidNewCalculator(calculator, onDrugClick)
            "Renal Function" -> RenalNewCalculator(calculator)
            "Hemodynamics" -> HemodynamicsNewCalculator(calculator)
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun CalculatorFrame(item: IcuCalculatorDefinition, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(item.accent.copy(alpha = .10f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Text(item.icon, fontSize = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontSize = 19.sp, fontWeight = FontWeight.Black, color = IcuScreenNavy)
                    Text(item.description, fontSize = 11.sp, color = IcuScreenSlate)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Color(0xFFE6EAF0))
            content()
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label, fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = modifier, shape = RoundedCornerShape(15.dp))
}

@Composable
private fun ActionRow(accent: Color, label: String, value: String, unit: String, note: String? = null) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = .08f)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(accent.copy(alpha = .12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = IcuScreenSlate, letterSpacing = .8.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, fontSize = 29.sp, fontWeight = FontWeight.Black, color = accent)
                    Spacer(Modifier.width(6.dp))
                    Text(unit, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent.copy(alpha = .75f))
                }
                note?.let { Text(it, fontSize = 10.sp, color = IcuScreenSlate, lineHeight = 15.sp) }
            }
        }
    }
}

@Composable
private fun ModeCaption(text: String) {
    Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(14.dp)) { Text(text, Modifier.padding(12.dp), fontSize = 10.sp, color = IcuScreenSlate, lineHeight = 15.sp) }
}

@Composable
private fun VasoactiveNewCalculator(item: IcuCalculatorDefinition, onDrugClick: (String) -> Unit) {
    var reverse by remember { mutableStateOf(false) }
    var primary by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("70") }
    var mg by remember { mutableStateOf("4") }
    var volume by remember { mutableStateOf("50") }
    val concentration = IcuCalculatorMath.vasoactiveConcentrationMcgPerMl(mg, volume)
    val rate = IcuCalculatorMath.vasoactiveRateMlPerHr(primary, weight, concentration.toString())
    val dose = IcuCalculatorMath.vasoactiveDoseMcgKgMin(primary, weight, concentration.toString())
    CalculatorFrame(item) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Noradrenaline", "Adrenaline", "Dopamine", "Dobutamine")) { drug ->
                Surface(Modifier.clickable { onDrugClick(drug) }, color = item.accent.copy(alpha = .10f), shape = RoundedCornerShape(50.dp)) { Text(drug, Modifier.padding(horizontal = 11.dp, vertical = 7.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = item.accent) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().background(item.accent.copy(alpha = .07f), RoundedCornerShape(15.dp)).padding(4.dp)) {
            ToggleChoice("Dose → Rate", !reverse, item.accent, Modifier.weight(1f)) { reverse = false }
            ToggleChoice("Rate → Dose", reverse, item.accent, Modifier.weight(1f)) { reverse = true }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(if (!reverse) "Dose (mcg/kg/min)" else "Pump rate (mL/hr)", primary, { primary = it }, Modifier.weight(1f))
            Field("Weight (kg)", weight, { weight = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Drug (mg)", mg, { mg = it }, Modifier.weight(1f))
            Field("Final volume (mL)", volume, { volume = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        ActionRow(item.accent, if (!reverse) "Set pump to" else "Delivered dose", if (!reverse) String.format(Locale.US, "%.1f", rate) else String.format(Locale.US, "%.2f", dose), if (!reverse) "mL/hr" else "mcg/kg/min")
        ModeCaption("Calculation only. Verify the active order, drug concentration, route and local ICU titration protocol before use.")
    }
}

@Composable
private fun ToggleChoice(label: String, selected: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), color = if (selected) accent else Color.Transparent, shape = RoundedCornerShape(12.dp)) {
        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else accent) }
    }
}

@Composable
private fun SedationNewCalculator(item: IcuCalculatorDefinition, onDrugClick: (String) -> Unit) {
    var dose by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("70") }
    var mg by remember { mutableStateOf("500") }
    var volume by remember { mutableStateOf("50") }
    val concentration = if ((volume.toDoubleOrNull() ?: 0.0) > 0) (mg.toDoubleOrNull() ?: 0.0) / (volume.toDoubleOrNull() ?: 1.0) else 0.0
    val rate = IcuCalculatorMath.sedationRateMlPerHr(dose, weight, concentration.toString())
    CalculatorFrame(item) {
        DrugChips(item.accent, listOf("Propofol", "Midazolam", "Fentanyl", "Dexmedetomidine"), onDrugClick)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Dose (mg/kg/hr)", dose, { dose = it }, Modifier.weight(1f))
            Field("Weight (kg)", weight, { weight = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Drug (mg)", mg, { mg = it }, Modifier.weight(1f))
            Field("Final volume (mL)", volume, { volume = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        ActionRow(item.accent, "Set syringe pump to", String.format(Locale.US, "%.1f", rate), "mL/hr")
        ModeCaption("Verify drug-specific units, concentration, sedation target and respiratory monitoring requirements against the active order.")
    }
}

@Composable
private fun ElectrolyteNewCalculator(item: IcuCalculatorDefinition, onDrugClick: (String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("1") }
    var volume by remember { mutableStateOf("100") }
    val rate = IcuCalculatorMath.electrolyteRateMlPerHr(volume, hours)
    val hourly = IcuCalculatorMath.electrolyteDosePerHr(amount, hours)
    CalculatorFrame(item) {
        DrugChips(item.accent, listOf("KCl (Potassium)", "MgSO4 (Magnesium)", "Ca Gluconate"), onDrugClick)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Target dose (mEq or g)", amount, { amount = it }, Modifier.weight(1f))
            Field("Infusion time (hr)", hours, { hours = it }, Modifier.weight(1f))
        }
        Field("Diluent volume (mL)", volume, { volume = it })
        Spacer(Modifier.height(12.dp))
        ActionRow(item.accent, "Target flow rate", String.format(Locale.US, "%.1f", rate), "mL/hr", if (hourly > 0) "Target replacement: ${String.format(Locale.US, "%.1f", hourly)}/hr" else null)
        ModeCaption("Verify electrolyte concentration, route, pump limits, cardiac monitoring and local replacement protocol before administration.")
    }
}

@Composable
private fun GlycemicNewCalculator(item: IcuCalculatorDefinition, onDrugClick: (String) -> Unit) {
    var target by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("50") }
    var volume by remember { mutableStateOf("50") }
    val rate = IcuCalculatorMath.insulinRateMlPerHr(target, units, volume)
    CalculatorFrame(item) {
        DrugChips(item.accent, listOf("Actrapid (Insulin)", "Novorapid"), onDrugClick)
        Spacer(Modifier.height(12.dp))
        Field("Target (units/hr)", target, { target = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Insulin (units)", units, { units = it }, Modifier.weight(1f))
            Field("Final volume (mL)", volume, { volume = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        ActionRow(item.accent, "Set syringe pump to", String.format(Locale.US, "%.1f", rate), "mL/hr")
        ModeCaption("Use the prescribed insulin preparation and hospital glucose-management protocol; check glucose monitoring and pump-library safeguards.")
    }
}

@Composable
private fun FluidNewCalculator(item: IcuCalculatorDefinition, onDrugClick: (String) -> Unit) {
    var weight by remember { mutableStateOf("") }
    var tbsa by remember { mutableStateOf("") }
    val total = IcuCalculatorMath.parklandTotalMl(weight, tbsa)
    val first = IcuCalculatorMath.parklandFirstEightHoursRateMlPerHr(total)
    val next = IcuCalculatorMath.parklandNextSixteenHoursRateMlPerHr(total)
    CalculatorFrame(item) {
        DrugChips(item.accent, listOf("Hartmann's (RL)", "0.9% Normal Saline", "Albumin"), onDrugClick)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Weight (kg)", weight, { weight = it }, Modifier.weight(1f))
            Field("Burn TBSA (%)", tbsa, { tbsa = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        ActionRow(item.accent, "Parkland total · 24 hr", String.format(Locale.US, "%.0f", total), "mL")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricResult("First 8 hr", String.format(Locale.US, "%.1f", first), "mL/hr", item.accent, Modifier.weight(1f))
            MetricResult("Next 16 hr", String.format(Locale.US, "%.1f", next), "mL/hr", item.accent, Modifier.weight(1f))
        }
        ModeCaption("This is a calculation aid. Fluid choice, timing from burn, urine-output targets and ongoing resuscitation require clinical reassessment and local protocol.")
    }
}

@Composable
private fun RenalNewCalculator(item: IcuCalculatorDefinition) {
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("70") }
    var creatinine by remember { mutableStateOf("") }
    var female by remember { mutableStateOf(false) }
    val ccr = IcuCalculatorMath.cockcroftGaultCrCl(age, weight, creatinine, female)
    CalculatorFrame(item) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Age (years)", age, { age = it }, Modifier.weight(1f))
            ToggleChoice(if (female) "Female" else "Male", true, item.accent, Modifier.weight(1f)) { female = !female }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Weight (kg)", weight, { weight = it }, Modifier.weight(1f))
            Field("Creatinine (mg/dL)", creatinine, { creatinine = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        ActionRow(item.accent, "Creatinine clearance", String.format(Locale.US, "%.1f", ccr), "mL/min")
        ModeCaption("Cockcroft-Gault estimate. Verify the patient's clinical context, renal function trend and local dosing guidance before medication adjustment.")
    }
}

@Composable
private fun HemodynamicsNewCalculator(item: IcuCalculatorDefinition) {
    var sbp by remember { mutableStateOf("120") }
    var dbp by remember { mutableStateOf("80") }
    var co by remember { mutableStateOf("5") }
    val map = IcuCalculatorMath.map(sbp, dbp)
    val svr = IcuCalculatorMath.svr(map, co)
    CalculatorFrame(item) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Systolic BP", sbp, { sbp = it }, Modifier.weight(1f))
            Field("Diastolic BP", dbp, { dbp = it }, Modifier.weight(1f))
        }
        Field("Cardiac output (L/min)", co, { co = it })
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricResult("MAP", String.format(Locale.US, "%.0f", map), "mmHg", item.accent, Modifier.weight(1f))
            MetricResult("SVR", String.format(Locale.US, "%.0f", svr), "dyn·s·cm⁻⁵", item.accent, Modifier.weight(1f))
        }
        ModeCaption("MAP = (SBP + 2×DBP) ÷ 3. SVR formula shown is a bedside calculation aid using the simplified pressure relationship in the existing module.")
    }
}

@Composable
private fun MetricResult(title: String, value: String, unit: String, accent: Color, modifier: Modifier) {
    Card(modifier, RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = .08f)), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = IcuScreenSlate, letterSpacing = .8.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 25.sp, fontWeight = FontWeight.Black, color = accent)
                Spacer(Modifier.width(5.dp))
                Text(unit, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent.copy(alpha = .75f))
            }
        }
    }
}

@Composable
private fun DrugChips(accent: Color, drugs: List<String>, onDrugClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(drugs) { drug ->
            Surface(Modifier.clickable { onDrugClick(drug) }, color = accent.copy(alpha = .10f), shape = RoundedCornerShape(50.dp)) { Text(drug, Modifier.padding(horizontal = 11.dp, vertical = 7.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent) }
        }
    }
}

@Composable
private fun IcuDrugDialog(drug: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().padding(18.dp), RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp)) {
                Text(drug, fontSize = 21.sp, fontWeight = FontWeight.Black, color = IcuScreenNavy)
                Spacer(Modifier.height(8.dp))
                Text("Use the active prescription and institutional ICU protocol for preparation, administration, titration and monitoring. This screen provides calculation support only.", fontSize = 12.sp, lineHeight = 18.sp, color = IcuScreenSlate)
                Spacer(Modifier.height(16.dp))
                Surface(Modifier.fillMaxWidth(), color = IcuScreenAmber.copy(alpha = .10f), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = IcuScreenAmber)
                        Spacer(Modifier.width(8.dp))
                        Text("Independent verification is required before high-alert medication administration.", fontSize = 11.sp, lineHeight = 16.sp, color = IcuScreenNavy)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Surface(Modifier.clickable(onClick = onDismiss), color = IcuScreenBlue, shape = RoundedCornerShape(14.dp)) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text("CLOSE", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp) }
                }
            }
        }
    }
}
