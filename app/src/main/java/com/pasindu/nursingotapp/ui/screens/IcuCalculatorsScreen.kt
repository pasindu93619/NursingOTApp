package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pasindu.nursingotapp.R
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcuCalculatorsScreen(
    onNavigateBack: () -> Unit
) {
    val bgSoftWhite = Color(0xFFF4F7FB)
    val techBlue = Color(0xFF1976D2)

    var selectedDrug by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ICU Clinical Protocols", fontWeight = FontWeight.Black, color = techBlue, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = techBlue) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgSoftWhite, scrolledContainerColor = bgSoftWhite)
            )
        },
        containerColor = bgSoftWhite
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                VasoactiveEngineCard(onDrugClick = { selectedDrug = it })
                SedationEngineCard(onDrugClick = { selectedDrug = it })
                ElectrolyteEngineCard(onDrugClick = { selectedDrug = it })
                InsulinEngineCard(onDrugClick = { selectedDrug = it })
                FluidResuscitationCard(onDrugClick = { selectedDrug = it })
                Spacer(modifier = Modifier.height(32.dp))
            }

            selectedDrug?.let { drugName ->
                DrugIntelligenceDialog(drugName = drugName, onDismiss = { selectedDrug = null })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. VASOACTIVE ENGINE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VasoactiveEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFFD32F2F)
    val colorSecondary = Color(0xFFFF8F00)
    val lightGlow = Color(0xFFFFEBEE)

    var isReverseCalc by remember { mutableStateOf(false) }
    var primaryInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("70") }
    var mgInput by remember { mutableStateOf("4") }
    var mlInput by remember { mutableStateOf("50") }

    val primaryVal = primaryInput.toFloatOrNull() ?: 0f
    val weight = weightInput.toFloatOrNull() ?: 0f
    val mg = mgInput.toFloatOrNull() ?: 0f
    val volume = mlInput.toFloatOrNull() ?: 0f

    val concMcg = if (volume > 0) (mg * 1000) / volume else 0f
    val targetMlHr = if (!isReverseCalc && concMcg > 0) (primaryVal * weight * 60) / concMcg else primaryVal
    val targetMcg = if (isReverseCalc && weight > 0) (primaryVal * concMcg) / (weight * 60) else primaryVal

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.4f, animationSpec = infiniteRepeatable(tween(1500, easing = FastOutLinearInEasing), RepeatMode.Restart), label = "")
    val pulseAlpha by infiniteTransition.animateFloat(initialValue = 0.6f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1500, easing = FastOutLinearInEasing), RepeatMode.Restart), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (size.width > 0) drawCircle(color = Color.White.copy(alpha = pulseAlpha), radius = (size.width / 2) * pulseScale)
                        }
                        Image(painter = painterResource(id = R.drawable.heart), contentDescription = "Heart", modifier = Modifier.size(60.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Vasoactive Inotropes", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        DrugTagRow(listOf("Noradrenaline", "Adrenaline", "Dopamine", "Dobutamine", "Milrinone"), onDrugClick)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(lightGlow).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (!isReverseCalc) colorPrimary else Color.Transparent).clickable { isReverseCalc = false }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text("Dose ➔ Rate (mL/hr)", color = if (!isReverseCalc) Color.White else colorPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (isReverseCalc) colorPrimary else Color.Transparent).clickable { isReverseCalc = true }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text("Rate ➔ Dose (mcg)", color = if (isReverseCalc) Color.White else colorPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(value = primaryInput, onValueChange = { primaryInput = it }, label = if (!isReverseCalc) "Dose (mcg/kg/min)" else "Pump Rate (mL/hr)", color = colorPrimary, modifier = Modifier.weight(1f))
                    PerfectTextField(value = weightInput, onValueChange = { weightInput = it }, label = "Weight (kg)", color = colorPrimary, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(value = mgInput, onValueChange = { mgInput = it }, label = "Ampoule (mg)", color = colorPrimary, modifier = Modifier.weight(1f))
                    PerfectTextField(value = mlInput, onValueChange = { mlInput = it }, label = "Diluent (mL)", color = colorPrimary, modifier = Modifier.weight(1f))
                }

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorPrimary).padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(if (!isReverseCalc) "SET SYRINGE PUMP TO:" else "PATIENT IS RECEIVING:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (!isReverseCalc) String.format(Locale.US, "%.1f", targetMlHr) else String.format(Locale.US, "%.2f", targetMcg), fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Text(if (!isReverseCalc) "mL/hr" else "mcg/kg/min", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                ClinicalPearlPanel(colorPrimary, "Inotropic Physiology", "Alpha-1 agonism mediates massive vasoconstriction to increase SVR and MAP. Beta-1 agonism increases myocardial contractility. Always administer via Central Venous Catheter.")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. SEDATION ENGINE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SedationEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF6A1B9A)
    val colorSecondary = Color(0xFF00B0FF)

    var doseInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("70") }
    var mgInput by remember { mutableStateOf("500") }
    var mlInput by remember { mutableStateOf("50") }

    val dose = doseInput.toFloatOrNull() ?: 0f
    val weight = weightInput.toFloatOrNull() ?: 0f
    val mg = mgInput.toFloatOrNull() ?: 0f
    val volume = mlInput.toFloatOrNull() ?: 0f

    val concMg = if (volume > 0) mg / volume else 0f
    val rate = if (concMg > 0) (dose * weight) / concMg else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "hover")
    val hoverY by infiniteTransition.animateFloat(initialValue = -5f, targetValue = 5f, animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).offset(y = hoverY.dp), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.brain_1), contentDescription = "Brain", modifier = Modifier.size(60.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Sedation & Analgesia", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        DrugTagRow(listOf("Propofol", "Midazolam", "Fentanyl", "Dexmedetomidine"), onDrugClick)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(value = doseInput, onValueChange = { doseInput = it }, label = "Dose (mg/kg/hr)", color = colorPrimary, modifier = Modifier.weight(1f))
                    PerfectTextField(value = weightInput, onValueChange = { weightInput = it }, label = "Weight (kg)", color = colorPrimary, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(value = mgInput, onValueChange = { mgInput = it }, label = "Ampoule (mg)", color = colorPrimary, modifier = Modifier.weight(1f))
                    PerfectTextField(value = mlInput, onValueChange = { mlInput = it }, label = "Diluent (mL)", color = colorPrimary, modifier = Modifier.weight(1f))
                }

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorPrimary).padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("SET SYRINGE PUMP TO:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (rate > 0) String.format(Locale.US, "%.1f", rate) else "0.0", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Text("mL/hr", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                ClinicalPearlPanel(colorPrimary, "Neuro-Depression Mechanism", "Most sedatives enhance the inhibitory neurotransmitter GABA. Fentanyl provides analgesia via Mu-opioid receptors. Monitor for respiratory depression and hypotension.")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. ELECTROLYTE ENGINE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ElectrolyteEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF00897B)
    val colorSecondary = Color(0xFF00E676)

    var doseInput by remember { mutableStateOf("") }
    var timeInput by remember { mutableStateOf("1") }
    var mlInput by remember { mutableStateOf("100") }

    val dose = doseInput.toFloatOrNull() ?: 0f
    val time = timeInput.toFloatOrNull() ?: 0f
    val volume = mlInput.toFloatOrNull() ?: 0f

    val rate = if (time > 0) volume / time else 0f
    val speed = if (time > 0) dose / time else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "drip")
    val dropY by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 40f, animationSpec = infiniteRepeatable(tween(1000, easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)), RepeatMode.Restart), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.infusion_bag_green), contentDescription = "IV Bag", modifier = Modifier.size(50.dp).offset(y = (-5).dp))
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (size.width > 0) {
                                val cx = size.width / 2f
                                val startY = size.height * 0.7f
                                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(cx, startY + dropY))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Electrolyte Protocols", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        DrugTagRow(listOf("KCl (Potassium)", "MgSO4 (Magnesium)", "Ca Gluconate", "NaCl 3%"), onDrugClick)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(value = doseInput, onValueChange = { doseInput = it }, label = "Target Dose (mEq or g)", color = colorPrimary, modifier = Modifier.weight(1f))
                    PerfectTextField(value = timeInput, onValueChange = { timeInput = it }, label = "Infusion Time (Hrs)", color = colorPrimary, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(value = mlInput, onValueChange = { mlInput = it }, label = "Diluent Vol (mL)", color = colorPrimary, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorPrimary).padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("TARGET FLOW RATE:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (rate > 0) String.format(Locale.US, "%.1f", rate) else "0.0", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
                            if (speed > 0) Text(String.format(Locale.US, "Delivery Speed: %.1f / hr", speed), color = colorSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("mL/hr", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                ClinicalPearlPanel(colorPrimary, "Action Potential Dynamics", "Potassium dictates cellular repolarization. Pushing K+ faster than 10 mEq/hr peripherally risks severe burning and fatal hyperkalemic arrhythmias.")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. INSULIN GLYCEMIC CONTROL ENGINE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InsulinEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF00695C)
    val colorSecondary = Color(0xFF00BFA5)

    var doseInput by remember { mutableStateOf("") }
    var unitsInput by remember { mutableStateOf("50") }
    var mlInput by remember { mutableStateOf("50") }

    val dose = doseInput.toFloatOrNull() ?: 0f
    val units = unitsInput.toFloatOrNull() ?: 0f
    val volume = mlInput.toFloatOrNull() ?: 0f

    val conc = if (volume > 0) units / volume else 0f
    val rate = if (conc > 0) dose / conc else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "hex")
    val pulse by infiniteTransition.animateFloat(initialValue = 0.9f, targetValue = 1.1f, animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (size.width > 0) {
                                val radius = 25f * pulse
                                val path = Path()
                                for (i in 0..5) {
                                    val angle = i * (PI / 3)
                                    val px = (size.width/2) + radius * cos(angle).toFloat()
                                    val py = (size.height/2) + radius * sin(angle).toFloat()
                                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                }
                                path.close()
                                drawPath(path, Color.White, style = Stroke(4f))
                                drawCircle(Color.White.copy(alpha = 0.5f), radius * 0.3f, Offset(size.width/2, size.height/2))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Glycemic Control", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        DrugTagRow(listOf("Actrapid (Insulin)", "Novorapid"), onDrugClick)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(value = doseInput, onValueChange = { doseInput = it }, label = "Target (Units/hr)", color = colorPrimary, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(value = unitsInput, onValueChange = { unitsInput = it }, label = "Insulin (Units)", color = colorPrimary, modifier = Modifier.weight(1f))
                    PerfectTextField(value = mlInput, onValueChange = { mlInput = it }, label = "Diluent (mL)", color = colorPrimary, modifier = Modifier.weight(1f))
                }

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorPrimary).padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("SET SYRINGE PUMP TO:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (rate > 0) String.format(Locale.US, "%.1f", rate) else "0.0", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Text("mL/hr", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                ClinicalPearlPanel(colorPrimary, "Intracellular Transport", "IV Insulin binds to receptors to translocate GLUT4 transporters to the cell membrane, actively pulling glucose from the serum into the cells.")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. FLUID RESUSCITATION (Parkland Formula)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FluidResuscitationCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF0277BD) // Deep Blue
    val colorSecondary = Color(0xFF29B6F6) // Light Cyan

    var weightInput by remember { mutableStateOf("") }
    var tbsaInput by remember { mutableStateOf("") }

    val weight = weightInput.toFloatOrNull() ?: 0f
    val tbsa = tbsaInput.toFloatOrNull() ?: 0f

    val totalFluid = 4f * weight * tbsa
    val first8HoursRate = if (totalFluid > 0) (totalFluid / 2f) / 8f else 0f
    val next16HoursRate = if (totalFluid > 0) (totalFluid / 2f) / 16f else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2 * PI.toFloat(), animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(35.dp)).background(Color.White.copy(0.2f))) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (size.width > 0) {
                                val path = Path()
                                path.moveTo(0f, size.height)
                                for (x in 0..size.width.toInt() step 5) {
                                    val y = (size.height * 0.5f) + sin((x / size.width) * 2 * PI + phase).toFloat() * 10f
                                    if (x == 0) path.lineTo(0f, y) else path.lineTo(x.toFloat(), y)
                                }
                                path.lineTo(size.width, size.height)
                                path.close()
                                drawPath(path, Color.White.copy(0.6f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Fluid Resuscitation", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        DrugTagRow(listOf("Hartmann's (RL)", "0.9% Normal Saline", "Albumin"), onDrugClick)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(value = weightInput, onValueChange = { weightInput = it }, label = "Weight (kg)", color = colorPrimary, modifier = Modifier.weight(1f))
                    PerfectTextField(value = tbsaInput, onValueChange = { tbsaInput = it }, label = "Burn TBSA (%)", color = colorPrimary, modifier = Modifier.weight(1f))
                }

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorPrimary).padding(20.dp)) {
                    Column {
                        Text("PARKLAND FORMULA (TOTAL 24H: ${totalFluid.toInt()} mL)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("First 8 Hours", color = colorSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(if (first8HoursRate > 0) String.format(Locale.US, "%.1f", first8HoursRate) else "0.0", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Next 16 Hours", color = colorSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(if (next16HoursRate > 0) String.format(Locale.US, "%.1f", next16HoursRate) else "0.0", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }

                ClinicalPearlPanel(colorPrimary, "Fluid Shifts in Trauma", "Severe burns cause massive capillary leak (Third-Spacing). The Parkland Formula uses Lactated Ringer's to aggressively replace lost intravascular volume and maintain organ perfusion.")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PHARMACOLOGY INTELLIGENCE DIALOG & DICTIONARY
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DrugIntelligenceDialog(drugName: String, onDismiss: () -> Unit) {
    val themeColor = when {
        listOf("Noradrenaline", "Adrenaline", "Dopamine", "Dobutamine", "Milrinone").contains(drugName) -> Color(0xFFD32F2F)
        listOf("Propofol", "Midazolam", "Fentanyl", "Dexmedetomidine").contains(drugName) -> Color(0xFF6A1B9A)
        listOf("Actrapid (Insulin)", "Novorapid").contains(drugName) -> Color(0xFF00695C)
        listOf("Hartmann's (RL)", "0.9% Normal Saline", "Albumin").contains(drugName) -> Color(0xFF0277BD)
        else -> Color(0xFF00897B)
    }

    val (className, considerations) = getDrugIntelligence(drugName)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(24.dp)).background(Color.White)) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().background(themeColor).padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalHospital, contentDescription = "Rx", tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(drugName.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
                                Text(className, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                    }
                }
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("NURSING CONSIDERATIONS", color = themeColor, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    considerations.forEach { point ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•", color = themeColor, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(point, color = Color.DarkGray, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = themeColor), shape = RoundedCornerShape(12.dp)) {
                        Text("ACKNOWLEDGE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

fun getDrugIntelligence(drugName: String): Pair<String, List<String>> {
    return when (drugName) {
        "Noradrenaline" -> "Alpha/Beta Agonist" to listOf("First-line vasopressor for Septic Shock.", "Requires Central Venous Catheter (CVP). Extravasation causes severe tissue necrosis.", "If extravasation occurs, antidote is Phentolamine SC injection.")
        "Adrenaline" -> "Potent Alpha/Beta Agonist" to listOf("Drug of choice for Anaphylaxis and Cardiac Arrest.", "Significantly increases myocardial oxygen demand.", "High risk of inducing tachyarrhythmias (V-Tach, V-Fib).")
        "Dopamine" -> "Dose-Dependent Inotrope" to listOf("Cardiac Dose (5-10 mcg): Increases contractility.", "Vasopressor Dose (>10 mcg): Causes vasoconstriction.", "Extremely arrhythmogenic. Monitor continuous ECG.")
        "Dobutamine" -> "Inodilator" to listOf("Increases cardiac output while reducing afterload.", "May cause a paradoxical drop in blood pressure initially.")
        "Milrinone" -> "Phosphodiesterase-3 Inhibitor" to listOf("Increases cardiac contractility and causes severe vasodilation.", "Requires renal dose adjustments. Has a long half-life.")
        "Propofol" -> "General Anesthetic" to listOf("Rapid onset (30 sec) and rapid offset.", "Must change IV tubing every 12 hours to prevent bacterial growth.", "Watch for PRIS (Propofol Infusion Syndrome).")
        "Midazolam" -> "Benzodiazepine" to listOf("Provides excellent anterograde amnesia.", "Does NOT provide pain relief (analgesia).", "Reversal Agent: Flumazenil.")
        "Fentanyl" -> "Synthetic Opioid" to listOf("100x more potent than Morphine.", "Rapid IV push can cause rigid chest syndrome.", "Reversal Agent: Naloxone (Narcan).")
        "Dexmedetomidine" -> "Alpha-2 Agonist" to listOf("Provides 'cooperative sedation' - patient can be awakened to follow commands.", "Does NOT cause respiratory depression (safe for extubation).")
        "KCl (Potassium)" -> "Intracellular Cation" to listOf("NEVER administer via IV Push (Causes fatal cardiac arrest).", "Max safe peripheral rate is 10 mEq/hour.", "Hyperkalemia causes peaked T-waves and widened QRS.")
        "MgSO4 (Magnesium)" -> "Intracellular Cation" to listOf("Used for Torsades de Pointes and Eclampsia.", "Toxicity causes loss of DTRs and respiratory depression.", "Reversal Agent: 10% Calcium Gluconate.")
        "Ca Gluconate" -> "Extracellular Cation" to listOf("Used to stabilize the myocardium in hyperkalemia.", "Must be pushed slowly to avoid bradycardia/asystole.")
        "NaCl 3%" -> "Hypertonic Saline" to listOf("Used to reduce elevated ICP in brain injuries.", "Must be given via Central Line.", "Correcting Sodium too fast causes Osmotic Demyelination.")
        "Actrapid (Insulin)" -> "Short-Acting Insulin" to listOf("IV half-life is only 5-8 minutes.", "Always prime IV tubing with 20mL of insulin mixture as insulin binds to the plastic tubing.", "Monitor CBG hourly when on a continuous drip.")
        "Novorapid" -> "Rapid-Acting Insulin" to listOf("Primarily used for subcutaneous injection, rarely for continuous IV infusion in ICU compared to Actrapid.")
        "Hartmann's (RL)" -> "Isotonic Crystalloid" to listOf("Contains Potassium and Lactate. Lactate is metabolized by the liver into bicarbonate.", "Avoid in severe liver failure or severe hyperkalemia.", "Preferred fluid for massive burn resuscitation (Parkland).")
        "0.9% Normal Saline" -> "Isotonic Crystalloid" to listOf("Can cause hyperchloremic metabolic acidosis if given in massive volumes.", "The ONLY fluid compatible with PRBC blood transfusions.")
        "Albumin" -> "Colloid Volume Expander" to listOf("Pulls fluid from the interstitial space back into the intravascular space.", "Expensive and carries a very rare risk of anaphylaxis as a blood product.")
        else -> "Clinical Agent" to listOf("Refer to local ward protocol before administration.")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE ALIGNMENT COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfectTextField(value: String, onValueChange: (String) -> Unit, label: String, color: Color, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp),
        colors = TextFieldDefaults.colors(focusedIndicatorColor = color, unfocusedIndicatorColor = Color.LightGray, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedLabelColor = color),
        modifier = modifier
    )
}

@Composable
fun DrugTagRow(drugs: List<String>, onDrugClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        items(drugs) { drug ->
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.2f)).clickable { onDrugClick(drug) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(drug, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ClinicalPearlPanel(color: Color, title: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.08f)).border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.AutoAwesome, contentDescription = "Pearl", tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(text, color = Color.DarkGray, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}