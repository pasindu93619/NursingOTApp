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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.round

private val WorkspaceBlue = Color(0xFF1769E8)
private val WorkspaceCyan = Color(0xFF149FE3)
private val WorkspaceIndigo = Color(0xFF4B78F2)
private val WorkspacePurple = Color(0xFF7B5CEB)
private val WorkspaceNavy = Color(0xFF14213D)
private val WorkspaceSlate = Color(0xFF667085)
private val WorkspaceBg = Color(0xFFF6F8FC)
private val WorkspaceRed = Color(0xFFD32F2F)
private val WorkspacePurpleDeep = Color(0xFF8E24AA)
private val WorkspaceGradient = Brush.linearGradient(listOf(WorkspaceBlue, WorkspaceCyan, WorkspaceIndigo, WorkspacePurple))

@Composable
fun HighAlertCalculatorWorkspaceScreen(
    initialMode: SpecialMode = SpecialMode.INSULIN,
    onNavigateBack: () -> Unit = {}
) {
    var currentMode by remember(initialMode) { mutableStateOf(initialMode) }
    var showGuide by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkspaceBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WorkspaceNavy)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("High-Alert Calcs", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = currentMode.themeColor)
                    Text("Critical medication workflow", fontSize = 12.sp, color = WorkspaceSlate)
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(currentMode.themeColor.copy(alpha = .10f))
                    .clickable { showGuide = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Safety guide", tint = currentMode.themeColor)
            }
        }

        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(WorkspaceGradient)
                .padding(22.dp)
        ) {
            Column {
                Text("HIGH-ALERT", 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = .82f), letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentMode.title, 29.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when (currentMode) {
                                SpecialMode.INSULIN -> "Dose, infusion and glucose workflow"
                                SpecialMode.HEPARIN -> "Weight-based anticoagulation pump workflow"
                                SpecialMode.PCA -> "Lockout and programmed-limit review"
                            },
                            13.sp,
                            color = Color.White.copy(alpha = .90f),
                            lineHeight = 18.sp
                        )
                    }
                    Text(currentMode.emoji, 30.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag("ORDER-LED")
                    Tag("kg-first")
                    Tag("offline")
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(SpecialMode.values()) { mode ->
                val selected = mode == currentMode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) mode.themeColor else Color.White)
                        .clickable { currentMode = mode }
                        .padding(horizontal = 15.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(mode.emoji, 15.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(mode.title, 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else WorkspaceSlate)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SafetyCard(currentMode)
        Spacer(Modifier.height(16.dp))

        when (currentMode) {
            SpecialMode.INSULIN -> InsulinCalculatorCard()
            SpecialMode.HEPARIN -> HeparinCalculatorCard()
            SpecialMode.PCA -> PcaCalculatorCard()
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = WorkspaceNavy)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = .10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("DOUBLE-CHECK BEFORE ACTION", 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp)
                    Text("Confirm patient, order, units, concentration, weight and pump settings.", 12.sp, color = Color.White.copy(alpha = .82f), lineHeight = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showGuide) {
        SpecialClinicalGuideDialog(currentMode) { showGuide = false }
    }
}

@Composable
private fun Tag(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Color.White.copy(alpha = .16f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SafetyCard(mode: SpecialMode) {
    val text = when (mode) {
        SpecialMode.INSULIN -> "Verify insulin type, units, concentration, timing and patient-specific targets before administration."
        SpecialMode.HEPARIN -> "Verify patient weight, units/hr, concentration and pump settings against the active order."
        SpecialMode.PCA -> "Lockout-derived capacity is theoretical. Verify basal rate, loading dose and all programmed limits."
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = mode.themeColor.copy(alpha = .08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = mode.themeColor)
            Spacer(Modifier.width(10.dp))
            Text(text, 12.sp, color = WorkspaceNavy, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun InsulinCalculatorCard() {
    var bg by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var concentration by remember { mutableStateOf("") }

    val scaleUnits = remember(bg) {
        val value = bg.toFloatOrNull() ?: 0f
        if (value > 100f) round((value - 100f) / 10f).toInt() else 0
    }
    val rate = remember(weight, dose, concentration) {
        val w = weight.toFloatOrNull() ?: 0f
        val d = dose.toFloatOrNull() ?: 0f
        val c = concentration.toFloatOrNull() ?: 0f
        if (w > 0 && d > 0 && c > 0) (round((w * d / c) * 10.0) / 10.0).toFloat() else 0f
    }

    CalculatorCard("Insulin dosing", "Enter values exactly as prescribed", currentModeAccent(INSULIN = ThemeInsulinBlue)) {
        Label("GLUCOSE CHECK")
        NumberInput("Blood glucose (mg/dL)", bg) { bg = it }
        if (scaleUnits > 0) ResultCard(ThemeInsulinBlue, Icons.Default.Medication, "Protocol input result", "$scaleUnits", "units")
        HorizontalDivider(color = Color(0xFFE5E7EB))
        Label("CONTINUOUS IV INFUSION")
        PairInputs("Weight (kg)", weight, { weight = it }, "Order (U/kg/hr)", dose, { dose = it })
        NumberInput("Concentration (U/mL)", concentration) { concentration = it }
        if (rate > 0f) ResultCard(ThemeInsulinBlue, Icons.Default.Speed, "IV infusion rate", "%.1f".format(rate), "mL/hr")
        Formula("IV rate = (weight × ordered U/kg/hr) ÷ concentration")
    }
}

@Composable
private fun HeparinCalculatorCard() {
    var weight by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var bagUnits by remember { mutableStateOf("") }
    var bagMl by remember { mutableStateOf("") }

    val unitsHr = remember(weight, dose) {
        val w = weight.toFloatOrNull() ?: 0f
        val d = dose.toFloatOrNull() ?: 0f
        if (w > 0 && d > 0) round(w * d).toFloat() else 0f
    }
    val rate = remember(unitsHr, bagUnits, bagMl) {
        val u = bagUnits.toFloatOrNull() ?: 0f
        val v = bagMl.toFloatOrNull() ?: 0f
        if (unitsHr > 0 && u > 0 && v > 0) (round(unitsHr * v / u * 10.0) / 10.0).toFloat() else 0f
    }

    CalculatorCard("Heparin pump", "Weight-based order to pump setup", ThemeRuby) {
        Label("STEP 1 · PATIENT NEED")
        PairInputs("Weight (kg)", weight, { weight = it }, "Order (U/kg/hr)", dose, { dose = it })
        if (unitsHr > 0f) ResultCard(ThemeRuby, Icons.Default.Speed, "Required dose", "%.0f".format(unitsHr), "units/hr")
        HorizontalDivider(color = Color(0xFFE5E7EB))
        Label("STEP 2 · IV PUMP SETUP")
        PairInputs("Bag units", bagUnits, { bagUnits = it }, "Bag volume (mL)", bagMl, { bagMl = it })
        if (rate > 0f) ResultCard(ThemeRuby, Icons.Default.Opacity, "Heparin pump rate", "%.1f".format(rate), "mL/hr")
        Formula("Pump rate = units/hr × bag volume ÷ bag units")
    }
}

@Composable
private fun PcaCalculatorCard() {
    var bolus by remember { mutableStateOf("") }
    var lockout by remember { mutableStateOf("") }
    val maxDoses = remember(lockout) {
        val minutes = lockout.toFloatOrNull() ?: 0f
        if (minutes > 0) floor(60f / minutes).toInt() else 0
    }
    val limit = remember(maxDoses, bolus) {
        val dose = bolus.toFloatOrNull() ?: 0f
        if (maxDoses > 0 && dose > 0) (round(maxDoses * dose * 100.0) / 100.0).toFloat() else 0f
    }

    CalculatorCard("PCA lockout review", "Check programmed safeguards against the order", ThemePCAPurple) {
        Label("PROGRAMMED VALUES")
        NumberInput("Bolus dose", bolus) { bolus = it }
        NumberInput("Lockout interval (min)", lockout) { lockout = it }
        if (limit > 0f) {
            ResultCard(ThemePCAPurple, Icons.Default.Lock, "Theoretical lockout capacity", "%.2f".format(limit), "dose-units/hr")
            Text("Mathematical capacity only — not a patient-specific safe maximum.", 11.sp, color = ThemePCAPurple, fontWeight = FontWeight.Bold)
        }
        Formula("Maximum lockout events/hr = floor(60 ÷ lockout minutes)")
    }
}

@Composable
private fun CalculatorCard(
    title: String,
    subtitle: String,
    accent: Color,
    content: @Composable Column.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accent.copy(alpha = .10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = accent)
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(title, 19.sp, fontWeight = FontWeight.ExtraBold, color = WorkspaceNavy)
                    Text(subtitle, 12.sp, color = WorkspaceSlate)
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, 10.sp, fontWeight = FontWeight.ExtraBold, color = WorkspaceSlate, letterSpacing = 1.sp)
}

@Composable
private fun NumberInput(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun PairInputs(
    left: String,
    leftValue: String,
    onLeft: (String) -> Unit,
    right: String,
    rightValue: String,
    onRight: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberInput(left, leftValue, onLeft, Modifier.weight(1f))
        NumberInput(right, rightValue, onRight, Modifier.weight(1f))
    }
}

@Composable
private fun ResultCard(
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    unit: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = .08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = .13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title.uppercase(), 9.sp, fontWeight = FontWeight.ExtraBold, color = WorkspaceSlate, letterSpacing = .8.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, 29.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                    Spacer(Modifier.width(6.dp))
                    Text(unit, 13.sp, fontWeight = FontWeight.Bold, color = accent.copy(alpha = .75f))
                }
            }
        }
    }
}

@Composable
private fun Formula(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text("Formula · $text", Modifier.padding(14.dp), 11.sp, color = WorkspaceSlate, lineHeight = 17.sp)
    }
}

private fun currentModeAccent(INSULIN: Color): Color = INSULIN
