package com.pasindu.nursingotapp.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import kotlin.math.floor
import kotlin.math.round

private val CleanBlue = Color(0xFF1769E8)
private val CleanCyan = Color(0xFF149FE3)
private val CleanIndigo = Color(0xFF4B78F2)
private val CleanPurple = Color(0xFF7B5CEB)
private val CleanNavy = Color(0xFF14213D)
private val CleanSlate = Color(0xFF667085)
private val CleanBackground = Color(0xFFF6F8FC)
private val CleanRed = Color(0xFFD32F2F)
private val CleanPcaPurple = Color(0xFF8E24AA)
private val CleanGradient = Brush.linearGradient(listOf(CleanBlue, CleanCyan, CleanIndigo, CleanPurple))

@Composable
fun HighAlertCleanCalculatorScreen(initialMode: SpecialMode = SpecialMode.INSULIN, onNavigateBack: () -> Unit = {}) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(Color.White, CircleShape).clickable(onClick = onNavigateBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CleanNavy)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("High-Alert Calcs", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = mode.themeColor)
                    Text("Bedside medication workspace", fontSize = 12.sp, color = CleanSlate)
                }
            }
            Box(Modifier.size(44.dp).background(mode.themeColor.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Safety guide", tint = mode.themeColor)
            }
        }

        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().background(CleanGradient, RoundedCornerShape(28.dp)).padding(22.dp)) {
            Column {
                Text("HIGH-ALERT WORKSPACE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = .82f), letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(mode.title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text(mode.subtitle(), fontSize = 13.sp, color = Color.White.copy(alpha = .92f), lineHeight = 18.sp)
                    }
                    Text(mode.emoji, fontSize = 30.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderPill("ORDER-LED")
                    HeaderPill("kg-first")
                    HeaderPill("offline")
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            items(SpecialMode.values()) { item ->
                val selected = item == mode
                Box(
                    Modifier
                        .background(if (selected) item.themeColor else Color.White, RoundedCornerShape(18.dp))
                        .clickable { mode = item }
                        .padding(horizontal = 15.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.emoji, fontSize = 15.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else CleanSlate)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SafetyBanner(mode)
        Spacer(Modifier.height(16.dp))

        when (mode) {
            SpecialMode.INSULIN -> InsulinCleanCard()
            SpecialMode.HEPARIN -> HeparinCleanCard()
            SpecialMode.PCA -> PcaCleanCard()
        }

        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = CleanNavy), elevation = CardDefaults.cardElevation(0.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(Color.White.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("DOUBLE-CHECK BEFORE ACTION", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp)
                    Text("Confirm the patient, order, units, concentration, weight and pump settings.", fontSize = 12.sp, color = Color.White.copy(alpha = .82f), lineHeight = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

private fun SpecialMode.subtitle(): String = when (this) {
    SpecialMode.INSULIN -> "Dose, glucose and continuous IV infusion review"
    SpecialMode.HEPARIN -> "Weight-based anticoagulation and pump setup"
    SpecialMode.PCA -> "Lockout and programmed-limit review"
}

@Composable
private fun HeaderPill(text: String) {
    Box(Modifier.background(Color.White.copy(alpha = .16f), RoundedCornerShape(50.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SafetyBanner(mode: SpecialMode) {
    val message = when (mode) {
        SpecialMode.INSULIN -> "Verify insulin type, units, concentration, timing and patient-specific targets against the active order."
        SpecialMode.HEPARIN -> "Verify weight, prescribed units/hr, concentration and pump settings against the active order."
        SpecialMode.PCA -> "Lockout-derived capacity is mathematical only. Verify basal rate, loading dose and all programmed limits."
    }
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = mode.themeColor.copy(alpha = .08f)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = mode.themeColor)
            Spacer(Modifier.width(10.dp))
            Text(message, fontSize = 12.sp, color = CleanNavy, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun InsulinCleanCard() {
    var bg by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var concentration by remember { mutableStateOf("") }
    val scaleUnits = remember(bg) { val v = bg.toFloatOrNull() ?: 0f; if (v > 100f) round((v - 100f) / 10f).toInt() else 0 }
    val rate = remember(weight, dose, concentration) {
        val w = weight.toFloatOrNull() ?: 0f
        val d = dose.toFloatOrNull() ?: 0f
        val c = concentration.toFloatOrNull() ?: 0f
        if (w > 0f && d > 0f && c > 0f) (round((w * d / c) * 10.0) / 10.0).toFloat() else 0f
    }
    CalculatorCard("Insulin dosing", "Use only the prescribed protocol and concentration", CleanBlue) {
        SectionLabel("GLUCOSE CHECK")
        NumericField("Blood glucose (mg/dL)", bg) { bg = it }
        if (scaleUnits > 0) ResultCard(CleanBlue, Icons.Default.Medication, "Protocol input result", scaleUnits.toString(), "units")
        HorizontalDivider(color = Color(0xFFE5E7EB))
        SectionLabel("CONTINUOUS IV INFUSION")
        PairFields("Weight (kg)", weight, { weight = it }, "Order (U/kg/hr)", dose, { dose = it })
        NumericField("Concentration (U/mL)", concentration) { concentration = it }
        if (rate > 0f) ResultCard(CleanBlue, Icons.Default.Speed, "IV infusion rate", "%.1f".format(rate), "mL/hr")
        FormulaCard("IV rate = (weight × ordered U/kg/hr) ÷ concentration")
    }
}

@Composable
private fun HeparinCleanCard() {
    var weight by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var bagUnits by remember { mutableStateOf("") }
    var bagMl by remember { mutableStateOf("") }
    val unitsHr = remember(weight, dose) { val w = weight.toFloatOrNull() ?: 0f; val d = dose.toFloatOrNull() ?: 0f; if (w > 0f && d > 0f) round(w * d).toFloat() else 0f }
    val rate = remember(unitsHr, bagUnits, bagMl) { val u = bagUnits.toFloatOrNull() ?: 0f; val v = bagMl.toFloatOrNull() ?: 0f; if (unitsHr > 0f && u > 0f && v > 0f) (round(unitsHr * v / u * 10.0) / 10.0).toFloat() else 0f }
    CalculatorCard("Heparin pump", "Weight-based order to pump setup", CleanRed) {
        SectionLabel("STEP 1 · PATIENT NEED")
        PairFields("Weight (kg)", weight, { weight = it }, "Order (U/kg/hr)", dose, { dose = it })
        if (unitsHr > 0f) ResultCard(CleanRed, Icons.Default.Speed, "Required dose", "%.0f".format(unitsHr), "units/hr")
        HorizontalDivider(color = Color(0xFFE5E7EB))
        SectionLabel("STEP 2 · IV PUMP SETUP")
        PairFields("Bag units", bagUnits, { bagUnits = it }, "Bag volume (mL)", bagMl, { bagMl = it })
        if (rate > 0f) ResultCard(CleanRed, Icons.Default.Opacity, "Heparin pump rate", "%.1f".format(rate), "mL/hr")
        FormulaCard("Pump rate = units/hr × bag volume ÷ bag units")
    }
}

@Composable
private fun PcaCleanCard() {
    var bolus by remember { mutableStateOf("") }
    var lockout by remember { mutableStateOf("") }
    val maxDoses = remember(lockout) { val m = lockout.toFloatOrNull() ?: 0f; if (m > 0f) floor(60f / m).toInt() else 0 }
    val limit = remember(maxDoses, bolus) { val d = bolus.toFloatOrNull() ?: 0f; if (maxDoses > 0 && d > 0f) (round(maxDoses * d * 100.0) / 100.0).toFloat() else 0f }
    CalculatorCard("PCA lockout review", "Check programmed safeguards against the active order", CleanPcaPurple) {
        SectionLabel("PROGRAMMED VALUES")
        NumericField("Bolus dose", bolus) { bolus = it }
        NumericField("Lockout interval (min)", lockout) { lockout = it }
        if (limit > 0f) {
            ResultCard(CleanPcaPurple, Icons.Default.Lock, "Theoretical lockout capacity", "%.2f".format(limit), "dose-units/hr")
            Text("Mathematical capacity only — not a patient-specific safe maximum.", fontSize = 11.sp, color = CleanPcaPurple, fontWeight = FontWeight.Bold)
        }
        FormulaCard("Maximum lockout events/hr = floor(60 ÷ lockout minutes)")
    }
}

@Composable
private fun CalculatorCard(title: String, subtitle: String, accent: Color, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(accent.copy(alpha = .10f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = accent)
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = CleanNavy)
                    Text(subtitle, fontSize = 12.sp, color = CleanSlate)
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = CleanSlate, letterSpacing = 1.sp)
}

@Composable
private fun NumericField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier, shape = RoundedCornerShape(16.dp))
}

@Composable
private fun PairFields(left: String, leftValue: String, onLeft: (String) -> Unit, right: String, rightValue: String, onRight: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumericField(left, leftValue, onLeft, Modifier.weight(1f))
        NumericField(right, rightValue, onRight, Modifier.weight(1f))
    }
}

@Composable
private fun ResultCard(accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, unit: String) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = .08f)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(accent.copy(alpha = .13f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = CleanSlate, letterSpacing = .8.sp)
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
private fun FormulaCard(text: String) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), elevation = CardDefaults.cardElevation(0.dp)) {
        Text("Formula · $text", Modifier.padding(14.dp), fontSize = 11.sp, color = CleanSlate, lineHeight = 17.sp)
    }
}
