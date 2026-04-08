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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.pow
import kotlin.math.sin
import com.pasindu.nursingotapp.ui.theme.diagonalGradient


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcuCalculatorsScreen(
    onNavigateBack: () -> Unit
) {
    val bgSoftWhite = Color(0xFFF4F7FB)
    val techBlue = Color(0xFF1976D2)

    var selectedDrug by remember { mutableStateOf<String?>(null) }
    var openedCalculator by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ICU Clinical Protocols", fontWeight = FontWeight.Black, fontSize = 20.sp, color = techBlue, letterSpacing = 1.sp) },
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

                // New Creative Calculator Cards
                RenalFunctionCard(onCardClick = { openedCalculator = "renal" })
                HemodynamicsCard(onCardClick = { openedCalculator = "hemodynamics" })
                VentilatorCard(onCardClick = { openedCalculator = "ventilator" })
                NutritionCard(onCardClick = { openedCalculator = "nutrition" })

                Spacer(modifier = Modifier.height(32.dp))
            }

            selectedDrug?.let { drugName ->
                DrugIntelligenceDialog(drugName = drugName, onDismiss = { selectedDrug = null })
            }

            when (openedCalculator) {
                "renal" -> RenalFunctionCalculator(onDismiss = { openedCalculator = null })
                "hemodynamics" -> HemodynamicsCalculator(onDismiss = { openedCalculator = null })
                "ventilator" -> VentilatorCalculator(onDismiss = { openedCalculator = null })
                "nutrition" -> NutritionCalculator(onDismiss = { openedCalculator = null })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. VASOACTIVE ENGINE (Original)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VasoactiveEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFFE53935)
    val colorSecondary = Color(0xFFFFB300)
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
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
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
                        Text(if (!isReverseCalc) "mL/hr" else "mcg/min", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                ClinicalPearlPanel(colorPrimary, "Inotropic Physiology", "Alpha-1 agonism mediates massive vasoconstriction to increase SVR and MAP. Beta-1 agonism increases myocardial contractility. Always administer via Central Venous Catheter.")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. SEDATION ENGINE (Original)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SedationEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF8E24AA)
    val colorSecondary = Color(0xFF039BE5)

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
    val hoverY by infiniteTransition.animateFloat(initialValue = -6f, targetValue = 6f, animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
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
                        Text("mL/hr", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                ClinicalPearlPanel(colorPrimary, "Neuro-Depression Mechanism", "Most sedatives enhance the inhibitory neurotransmitter GABA. Fentanyl provides analgesia via Mu-opioid receptors. Monitor for respiratory depression and hypotension.")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. ELECTROLYTE ENGINE (Original)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ElectrolyteEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF00897B)
    val colorSecondary = Color(0xFF7CB342)

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
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.infusion_bag_green), contentDescription = "IV Bag", modifier = Modifier.size(50.dp).offset(y = (-5).dp))
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (size.width > 0) drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(size.width / 2f, size.height * 0.7f + dropY))
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
                            if (speed > 0) Text(String.format(Locale.US, "Speed: %.1f / hr", speed), color = colorSecondary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Text("mL/hr", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                ClinicalPearlPanel(colorPrimary, "Action Potential Dynamics", "Potassium dictates cellular repolarization. Pushing K+ faster than 10 mEq/hr peripherally risks severe burning and fatal hyperkalemic arrhythmias.")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. INSULIN ENGINE (Original)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InsulinEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF00ACC1)
    val colorSecondary = Color(0xFFF48FB1)

    var doseInput by remember { mutableStateOf("") }
    var unitsInput by remember { mutableStateOf("50") }
    var mlInput by remember { mutableStateOf("50") }

    val dose = doseInput.toFloatOrNull() ?: 0f
    val units = unitsInput.toFloatOrNull() ?: 0f
    val volume = mlInput.toFloatOrNull() ?: 0f

    val conc = if (volume > 0) units / volume else 0f
    val rate = if (conc > 0) dose / conc else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "absorb")
    val orbitPhase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2 * PI.toFloat(), animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "")
    val absorbScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0.2f, animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "")
    val absorbAlpha by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.cell_2), contentDescription = "Cell", modifier = Modifier.size(50.dp))

                        for (i in 0..2) {
                            val angle = orbitPhase + (i * (2 * PI / 3))
                            val distance = 35f * absorbScale
                            val offsetX = cos(angle).toFloat() * distance
                            val offsetY = sin(angle).toFloat() * distance

                            Image(
                                painter = painterResource(id = R.drawable.sugar),
                                contentDescription = "Sugar",
                                modifier = Modifier
                                    .size(20.dp)
                                    .offset(x = offsetX.dp, y = offsetY.dp)
                                    .scale(absorbScale)
                                    .alpha(absorbAlpha)
                            )
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
                        Text("mL/hr", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                ClinicalPearlPanel(colorPrimary, "Intracellular Transport", "IV Insulin binds to receptors to translocate GLUT4 transporters to the cell membrane, actively pulling glucose (sugar) from the serum into the cells.")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. FLUID RESUSCITATION (Original)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FluidResuscitationCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF1E88E5)
    val colorSecondary = Color(0xFF00ACC1)

    var weightInput by remember { mutableStateOf("") }
    var tbsaInput by remember { mutableStateOf("") }

    val weight = weightInput.toFloatOrNull() ?: 0f
    val tbsa = tbsaInput.toFloatOrNull() ?: 0f

    val totalFluid = 4f * weight * tbsa
    val first8HoursRate = if (totalFluid > 0) (totalFluid / 2f) / 8f else 0f
    val next16HoursRate = if (totalFluid > 0) (totalFluid / 2f) / 16f else 0f

    val burnIcon = when {
        tbsa >= 50f -> R.drawable.skin_third_degree_burn
        tbsa >= 20f -> R.drawable.skin_second_degree_burn
        tbsa > 0f -> R.drawable.skin_first_degree_burn
        else -> R.drawable.skin
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2 * PI.toFloat(), animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.White.copy(0.2f)).border(2.dp, Color.White.copy(0.5f), CircleShape)) {
                        Image(painter = painterResource(id = burnIcon), contentDescription = "Skin Status", modifier = Modifier.fillMaxSize().padding(8.dp))

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (size.width > 0) {
                                val path = Path()
                                path.moveTo(0f, size.height)
                                for (x in 0..size.width.toInt() step 5) {
                                    val y = (size.height * 0.5f) + sin((x / size.width) * 2 * PI + phase).toFloat() * 8f
                                    if (x == 0) path.lineTo(0f, y) else path.lineTo(x.toFloat(), y)
                                }
                                path.lineTo(size.width, size.height)
                                path.close()
                                drawPath(path, Color.White.copy(0.5f))
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
// 6. RENAL FUNCTION ENGINE (GFR & Creatinine Clearance)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RenalFunctionCard(onCardClick: () -> Unit) {
    val colorPrimary = Color(0xFF6A1B9A)
    val colorSecondary = Color(0xFFAB47BC)
    val lightGlow = Color(0xFFF3E5F5)

    val infiniteTransition = rememberInfiniteTransition(label = "kidney_pulse")
    val glowScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.3f, animationSpec = infiniteRepeatable(tween(2000, easing = FastOutLinearInEasing), RepeatMode.Restart), label = "")
    val glowAlpha by infiniteTransition.animateFloat(initialValue = 0.5f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(2000, easing = FastOutLinearInEasing), RepeatMode.Restart), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)).clickable(enabled = true, onClick = onCardClick),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color.White.copy(alpha = glowAlpha), radius = (size.width / 2) * glowScale)
                        }
                        Image(painter = painterResource(id = R.drawable.kidney), contentDescription = "Kidney", modifier = Modifier.size(50.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Renal Function", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("GFR & Clearance", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().background(lightGlow).padding(12.dp)) {
                Text("Creatinine Clearance • eGFR • Dosing Adjustments", fontSize = 12.sp, color = colorPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RenalFunctionCalculator(onDismiss: () -> Unit) {
    val colorPrimary = Color(0xFF6A1B9A)
    val colorSecondary = Color(0xFFAB47BC)

    var ageInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("70") }
    var creatinineInput by remember { mutableStateOf("") }
    var genderInput by remember { mutableStateOf("Male") }

    val age = ageInput.toFloatOrNull() ?: 0f
    val weight = weightInput.toFloatOrNull() ?: 0f
    val creatinine = creatinineInput.toFloatOrNull() ?: 0f

    val ccr = if (creatinine > 0 && weight > 0 && age > 0) {
        if (genderInput == "Male") ((140 - age) * weight) / (72 * creatinine)
        else ((140 - age) * weight * 0.85f) / (72 * creatinine)
    } else 0f

    val egfr = if (creatinine > 0 && age > 0) {
        175 * (creatinine).toDouble().pow(-1.154) * (age).toDouble().pow(-0.203) *
                if (genderInput == "Female") 0.742 else 1.0
    } else 0.0

    val infiniteTransition = rememberInfiniteTransition(label = "filter_float")
    val floatY by infiniteTransition.animateFloat(initialValue = -8f, targetValue = 8f, animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(enabled = true, onClick = onDismiss), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.fillMaxHeight(0.95f).fillMaxWidth(0.95f).clip(RoundedCornerShape(28.dp)).background(Color.White).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(colorPrimary, colorSecondary))).padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).offset(y = floatY.dp)) {
                                Image(painter = painterResource(id = R.drawable.kidney), contentDescription = "Kidney", modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Renal Function", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp)) }
                    }
                }

                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PerfectTextField(value = ageInput, onValueChange = { ageInput = it }, label = "Age (yrs)", color = colorPrimary, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).border(2.dp, colorPrimary, RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { genderInput = if (genderInput == "Male") "Female" else "Male" }, horizontalArrangement = Arrangement.Center) {
                                Text(genderInput, fontWeight = FontWeight.Black, fontSize = 14.sp, color = colorPrimary)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PerfectTextField(value = weightInput, onValueChange = { weightInput = it }, label = "Weight (kg)", color = colorPrimary, modifier = Modifier.weight(1f))
                        PerfectTextField(value = creatinineInput, onValueChange = { creatinineInput = it }, label = "Creat (mg/dL)", color = colorPrimary, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorPrimary).padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("CREATININE CLEARANCE (Cockcroft-Gault)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (ccr > 0) String.format(Locale.US, "%.1f", ccr) else "0.0", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("mL/min", fontSize = 14.sp, color = colorSecondary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorSecondary).padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("eGFR (MDRD Formula)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (egfr > 0) String.format(Locale.US, "%.1f", egfr) else "0.0", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("mL/min/1.73m²", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
                        }
                    }

                    ClinicalPearlPanel(colorPrimary, "Renal Dosing", "GFR <30 requires aggressive drug dose reductions. Many aminoglycosides and ACE-inhibitors accumulate in renal failure. Always check the BNF for nephrotoxic drugs.")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. HEMODYNAMICS ENGINE (MAP, CVP, SVR)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HemodynamicsCard(onCardClick: () -> Unit) {
    val colorPrimary = Color(0xFFD32F2F)
    val colorSecondary = Color(0xFFFF6F00)
    val lightGlow = Color(0xFFFFEBEE)

    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val beatScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)).clickable(enabled = true, onClick = onCardClick),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(
                    colors = listOf(colorPrimary, colorSecondary),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f) // diagonal effect
                )
            )) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).scale(beatScale), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.heart), contentDescription = "Heart", modifier = Modifier.size(55.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Hemodynamics", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("MAP • SVR • Perfusion", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().background(lightGlow).padding(12.dp)) {
                Text("Mean Arterial Pressure • SVR • Cardiac Output • Afterload", fontSize = 12.sp, color = colorPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HemodynamicsCalculator(onDismiss: () -> Unit) {
    val colorPrimary = Color(0xFFD32F2F)
    val colorSecondary = Color(0xFFFF6F00)

    var sbpInput by remember { mutableStateOf("120") }
    var dbpInput by remember { mutableStateOf("80") }
    var coInput by remember { mutableStateOf("5") }
    var svriInput by remember { mutableStateOf("") }

    val sbp = sbpInput.toFloatOrNull() ?: 0f
    val dbp = dbpInput.toFloatOrNull() ?: 0f
    val co = coInput.toFloatOrNull() ?: 0f

    val map = (sbp + (2 * dbp)) / 3
    val pp = sbp - dbp
    val svr = if (co > 0) ((map - 10) * 80) / co else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_wave")
    val pulseAlpha by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(800, easing = FastOutLinearInEasing), RepeatMode.Restart), label = "")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(enabled = true, onClick = onDismiss), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.fillMaxHeight(0.95f).fillMaxWidth(0.95f).clip(RoundedCornerShape(28.dp)).background(Color.White).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(
                    colors = listOf(colorPrimary, colorSecondary),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )).padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).alpha(pulseAlpha)) {
                                Image(painter = painterResource(id = R.drawable.heart), contentDescription = "Heart", modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Hemodynamics", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp)) }
                    }
                }

                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PerfectTextField(value = sbpInput, onValueChange = { sbpInput = it }, label = "SBP (mmHg)", color = colorPrimary, modifier = Modifier.weight(1f))
                        PerfectTextField(value = dbpInput, onValueChange = { dbpInput = it }, label = "DBP (mmHg)", color = colorPrimary, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PerfectTextField(value = coInput, onValueChange = { coInput = it }, label = "CO (L/min)", color = colorPrimary, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorPrimary).padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("MEAN ARTERIAL PRESSURE (MAP)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (map > 0) String.format(Locale.US, "%.0f", map) else "0", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("mmHg", fontSize = 14.sp, color = colorSecondary, fontWeight = FontWeight.Bold)
                            Text("Pulse Pressure: ${String.format(Locale.US, "%.0f", pp)} mmHg", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorSecondary).padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("SYSTEMIC VASCULAR RESISTANCE", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (svr > 0) String.format(Locale.US, "%.0f", svr) else "0", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("Wood Units", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
                        }
                    }

                    ClinicalPearlPanel(colorPrimary, "Tissue Perfusion", "MAP >65 mmHg is critical for organ perfusion. SVR >1200 indicates vasoconstriction (sepsis/cardiogenic shock). SVR <800 indicates excessive vasodilation (septic shock).")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. VENTILATOR ENGINE (MV, PEEP, FiO2)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VentilatorCard(onCardClick: () -> Unit) {
    val colorPrimary = Color(0xFF00695C)
    val colorSecondary = Color(0xFF26A69A)
    val lightGlow = Color(0xFFE0F2F1)

    val infiniteTransition = rememberInfiniteTransition(label = "ventilate")
    val expandScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)).clickable(enabled = true, onClick = onCardClick),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).scale(expandScale), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.lungs), contentDescription = "Lungs", modifier = Modifier.size(55.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Ventilator Settings", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("MV • PEEP • FiO2", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().background(lightGlow).padding(12.dp)) {
                Text("Minute Ventilation • Tidal Volume • PEEP • Compliance", fontSize = 12.sp, color = colorPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VentilatorCalculator(onDismiss: () -> Unit) {
    val colorPrimary = Color(0xFF00695C)
    val colorSecondary = Color(0xFF26A69A)

    var rateInput by remember { mutableStateOf("16") }
    var tvInput by remember { mutableStateOf("450") }
    var peepInput by remember { mutableStateOf("5") }
    var plateauInput by remember { mutableStateOf("") }

    val rate = rateInput.toFloatOrNull() ?: 0f
    val tv = tvInput.toFloatOrNull() ?: 0f
    val peep = peepInput.toFloatOrNull() ?: 0f
    val plateau = plateauInput.toFloatOrNull() ?: 0f

    val mv = rate * tv / 1000
    val compliance = if ((plateau - peep) > 0) tv / (plateau - peep) else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "breath_wave")
    val breathY by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 6f, animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(enabled = true, onClick = onDismiss), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.fillMaxHeight(0.95f).fillMaxWidth(0.95f).clip(RoundedCornerShape(28.dp)).background(Color.White).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colorPrimary, colorSecondary))).padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).offset(y = breathY.dp)) {
                                Image(painter = painterResource(id = R.drawable.lungs), contentDescription = "Lungs", modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Ventilator Settings", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp)) }
                    }
                }

                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PerfectTextField(value = rateInput, onValueChange = { rateInput = it }, label = "RR (breaths/min)", color = colorPrimary, modifier = Modifier.weight(1f))
                        PerfectTextField(value = tvInput, onValueChange = { tvInput = it }, label = "TV (mL)", color = colorPrimary, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PerfectTextField(value = peepInput, onValueChange = { peepInput = it }, label = "PEEP (cmH2O)", color = colorPrimary, modifier = Modifier.weight(1f))
                        PerfectTextField(value = plateauInput, onValueChange = { plateauInput = it }, label = "Pplat (cmH2O)", color = colorPrimary, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorPrimary).padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("MINUTE VENTILATION (MV)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (mv > 0) String.format(Locale.US, "%.1f", mv) else "0.0", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("L/min", fontSize = 14.sp, color = colorSecondary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorSecondary).padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("STATIC COMPLIANCE", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (compliance > 0) String.format(Locale.US, "%.1f", compliance) else "0.0", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("mL/cmH2O", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
                        }
                    }

                    ClinicalPearlPanel(colorPrimary, "Lung Protection Strategy", "TV should be 6-8 mL/kg IBW. Plateau pressure <30 cmH2O prevents barotrauma. PEEP >5 is typically needed in ARDS. Monitor compliance for secretion obstruction.")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. NUTRITION ENGINE (Calories & Macros)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NutritionCard(onCardClick: () -> Unit) {
    val colorPrimary = Color(0xFFE65100)
    val colorSecondary = Color(0xFFFF6E40)
    val lightGlow = Color(0xFFFFE0B2)

    val infiniteTransition = rememberInfiniteTransition(label = "nutrition_spin")
    val rotateZ by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)).clickable(enabled = true, onClick = onCardClick),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(colorPrimary, colorSecondary)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.nutrition), contentDescription = "Nutrition", modifier = Modifier.size(50.dp).graphicsLayer(rotationZ = rotateZ))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Nutrition Calc", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("Calories • Protein • Macros", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().background(lightGlow).padding(12.dp)) {
                Text("Harris-Benedict • Energy Expenditure • Protein Requirements", fontSize = 12.sp, color = colorPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NutritionCalculator(onDismiss: () -> Unit) {
    val colorPrimary = Color(0xFFE65100)
    val colorSecondary = Color(0xFFFF6E40)

    var ageInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("70") }
    var heightInput by remember { mutableStateOf("170") }
    var genderInput by remember { mutableStateOf("Male") }
    var acuityInput by remember { mutableStateOf("1.2") }

    val age = ageInput.toFloatOrNull() ?: 0f
    val weight = weightInput.toFloatOrNull() ?: 0f
    val height = heightInput.toFloatOrNull() ?: 0f
    val acuity = acuityInput.toFloatOrNull() ?: 1.0f

    val bmr = if (age > 0 && weight > 0 && height > 0) {
        if (genderInput == "Male") 88.362f + (13.397f * weight) + (4.799f * height) - (5.677f * age)
        else 447.593f + (9.247f * weight) + (3.098f * height) - (4.330f * age)
    } else 0f

    val totalCalories = bmr * acuity
    val protein = weight * 1.5f
    val carbs = (totalCalories * 0.5f) / 4
    val fat = (totalCalories * 0.3f) / 9

    val infiniteTransition = rememberInfiniteTransition(label = "plate_bounce")
    val bounceY by infiniteTransition.animateFloat(initialValue = 0f, targetValue = -8f, animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(enabled = true, onClick = onDismiss), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.fillMaxHeight(0.95f).fillMaxWidth(0.95f).clip(RoundedCornerShape(28.dp)).background(Color.White).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(colorPrimary, colorSecondary))).padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).offset(y = bounceY.dp)) {
                                Image(painter = painterResource(id = R.drawable.nutrition), contentDescription = "Nutrition", modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Nutrition", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp)) }
                    }
                }

                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PerfectTextField(value = ageInput, onValueChange = { ageInput = it }, label = "Age (yrs)", color = colorPrimary, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).border(2.dp, colorPrimary, RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { genderInput = if (genderInput == "Male") "Female" else "Male" }, horizontalArrangement = Arrangement.Center) {
                                Text(genderInput, fontWeight = FontWeight.Black, fontSize = 14.sp, color = colorPrimary)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PerfectTextField(value = weightInput, onValueChange = { weightInput = it }, label = "Weight (kg)", color = colorPrimary, modifier = Modifier.weight(1f))
                        PerfectTextField(value = heightInput, onValueChange = { heightInput = it }, label = "Height (cm)", color = colorPrimary, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PerfectTextField(value = acuityInput, onValueChange = { acuityInput = it }, label = "Acuity (1.0-2.0)", color = colorPrimary, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colorPrimary).padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("TOTAL DAILY CALORIES", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (totalCalories > 0) String.format(Locale.US, "%.0f", totalCalories) else "0", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("kcal", fontSize = 14.sp, color = colorSecondary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(colorSecondary).padding(16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Protein", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(if (protein > 0) String.format(Locale.US, "%.0f", protein) else "0", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text("g", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(colorSecondary).padding(16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Carbs", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(if (carbs > 0) String.format(Locale.US, "%.0f", carbs) else "0", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text("g", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(colorSecondary).padding(16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Fat", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(if (fat > 0) String.format(Locale.US, "%.0f", fat) else "0", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text("g", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    ClinicalPearlPanel(colorPrimary, "Early Enteral Nutrition", "Initiate EN within 24-48 hours. Protein targets 1.2-2.0 g/kg for ICU patients. Use ProMod or High-Protein formulas. Residual volume >250 mL suggests feed intolerance.")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfectTextField(value: String, onValueChange: (String) -> Unit, label: String, color: Color, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 16.sp),
        colors = TextFieldDefaults.colors(focusedIndicatorColor = color, unfocusedIndicatorColor = Color.LightGray, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedLabelColor = color),
        modifier = modifier
    )
}

@Composable
fun DrugTagRow(drugs: List<String>, onDrugClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        items(drugs) { drug ->
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.2f)).clickable { onDrugClick(drug) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(drug, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
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
            Text(title, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(text, color = Color.DarkGray, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DRUG INTELLIGENCE DIALOG (Original)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DrugIntelligenceDialog(drugName: String, onDismiss: () -> Unit) {
    val themeColor = when {
        listOf("Noradrenaline", "Adrenaline", "Dopamine", "Dobutamine", "Milrinone").contains(drugName) -> Color(0xFFE53935)
        listOf("Propofol", "Midazolam", "Fentanyl", "Dexmedetomidine").contains(drugName) -> Color(0xFF8E24AA)
        listOf("Actrapid (Insulin)", "Novorapid").contains(drugName) -> Color(0xFF00ACC1)
        listOf("Hartmann's (RL)", "0.9% Normal Saline", "Albumin").contains(drugName) -> Color(0xFF1E88E5)
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
                    Text("NURSING CONSIDERATIONS", color = themeColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    considerations.forEach { point ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•", color = themeColor, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(point, color = Color(0xFF333333), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = themeColor), shape = RoundedCornerShape(12.dp)) {
                        Text("ACKNOWLEDGE", fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
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
