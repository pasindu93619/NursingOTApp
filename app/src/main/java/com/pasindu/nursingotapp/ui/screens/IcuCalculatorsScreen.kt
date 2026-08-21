package com.pasindu.nursingotapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
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
    var openedCalculator by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = openedCalculator != null) {
        openedCalculator = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (openedCalculator == null) "ICU Clinical Protocols" else openedCalculator!!,
                        fontWeight = FontWeight.Black, fontSize = 20.sp, color = techBlue, letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (openedCalculator != null) openedCalculator = null else onNavigateBack() }) {
                        if (openedCalculator == null) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = techBlue)
                        } else {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = techBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgSoftWhite, scrolledContainerColor = bgSoftWhite)
            )
        },
        containerColor = bgSoftWhite
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // 1. DASHBOARD MENU VIEW
            AnimatedVisibility(
                visible = openedCalculator == null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IcuMenuCard("Vasoactive\nInotropes", "Noradrenaline, Adrenaline", Color(0xFFEF5350), R.drawable.heart) { openedCalculator = "Vasoactive Inotropes" }
                    IcuMenuCard("Sedation &\nAnalgesia", "Propofol, Midazolam", Color(0xFFAB47BC), R.drawable.brain_1) { openedCalculator = "Sedation & Analgesia" }
                    IcuMenuCard("Electrolyte Protocols", "KCl, MgSO4", Color(0xFF26A69A), R.drawable.infusion_bag_green) { openedCalculator = "Electrolyte Protocols" }
                    IcuMenuCard("Glycemic Control", "Actrapid Drips", Color(0xFF26C6DA), R.drawable.cell_2) { openedCalculator = "Glycemic Control" }
                    IcuMenuCard("Fluid Resuscitation", "Parkland Burn Formula", Color(0xFF42A5F5), R.drawable.skin) { openedCalculator = "Fluid Resuscitation" }
                    IcuMenuCard("Renal Function", "eGFR & Clearance", Color(0xFF5C6BC0), R.drawable.kidney) { openedCalculator = "Renal Function" }
                    IcuMenuCard("Hemodynamics", "MAP & SVR Analysis", Color(0xFFFFA726), R.drawable.heart) { openedCalculator = "Hemodynamics" }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // 2. FULL SCREEN CALCULATOR VIEW
            AnimatedVisibility(
                visible = openedCalculator != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    when (openedCalculator) {
                        "Vasoactive Inotropes" -> VasoactiveEngineCard(onDrugClick = { selectedDrug = it })
                        "Sedation & Analgesia" -> SedationEngineCard(onDrugClick = { selectedDrug = it })
                        "Electrolyte Protocols" -> ElectrolyteEngineCard(onDrugClick = { selectedDrug = it })
                        "Glycemic Control" -> InsulinEngineCard(onDrugClick = { selectedDrug = it })
                        "Fluid Resuscitation" -> FluidResuscitationCard(onDrugClick = { selectedDrug = it })
                        "Renal Function" -> RenalFunctionEngineCard()
                        "Hemodynamics" -> HemodynamicsEngineCard()
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // 3. PHARMACOLOGY DIALOG (The missing piece)
            selectedDrug?.let { drugName ->
                DrugIntelligenceDialog(drugName = drugName, onDismiss = { selectedDrug = null })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI MENU CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun IcuMenuCard(title: String, subtitle: String, color: Color, iconRes: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = color.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(76.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(42.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 26.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VASOACTIVE ENGINE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VasoactiveEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFFEF5350)
    val colorSecondary = Color(0xFFB71C1C)

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

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        AnimatedVascularHeart(rate = if (!isReverseCalc) targetMlHr else targetMcg)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Vasoactive Inotropes", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        DrugTagRow(listOf("Noradrenaline", "Adrenaline", "Dopamine", "Dobutamine"), onDrugClick)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFEBEE)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
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
                ClinicalPearlPanel(colorPrimary, "Inotropic Physiology", "Alpha-1 agonism mediates massive vasoconstriction to increase SVR and MAP. Always administer via Central Venous Catheter.")
            }
        }
    }
}

@Composable
fun AnimatedVascularHeart(rate: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "beat")
    val speed = if (rate > 0) (600 / (rate / 5f).coerceIn(1f, 3f)).toInt() else 1200

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(speed, easing = FastOutLinearInEasing), RepeatMode.Reverse), label = ""
    )

    val ringStroke by animateFloatAsState(targetValue = if (rate > 0) 6f else 2f, label = "")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(color = Color.White.copy(alpha = 0.3f), radius = size.width * 0.45f, center = center, style = Stroke(width = ringStroke))
        drawCircle(color = Color.White.copy(alpha = 0.1f), radius = size.width * 0.35f, center = center, style = Stroke(width = ringStroke))
        drawCircle(color = Color.White, radius = (size.width * 0.2f) * pulseScale, center = center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEDATION ENGINE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SedationEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFFAB47BC)
    val colorSecondary = Color(0xFF6A1B9A)

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

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        AnimatedEegGraphic(rate = rate)
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
                ClinicalPearlPanel(colorPrimary, "Neuro-Depression Mechanism", "Most sedatives enhance the inhibitory neurotransmitter GABA. Fentanyl provides analgesia via Mu-opioid receptors. Monitor for respiratory depression.")
            }
        }
    }
}

@Composable
fun AnimatedEegGraphic(rate: Float) {
    val phase = rememberInfiniteTransition(label = "").animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing))).value
    val frequency = if (rate > 0) 2f else 12f
    val amplitude = if (rate > 0) 0.4f else 0.2f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val midY = size.height / 2f
        path.moveTo(0f, midY)
        for (x in 0..size.width.toInt()) {
            val normalizedX = x / size.width
            val y = sin((normalizedX * frequency + phase) * 2 * PI).toFloat() * (size.height * amplitude)
            path.lineTo(x.toFloat(), midY + y)
        }
        drawPath(path, color = Color.White, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ELECTROLYTE PROTOCOLS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ElectrolyteEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF26A69A)
    val colorSecondary = Color(0xFF00695C)

    var doseInput by remember { mutableStateOf("") }
    var timeInput by remember { mutableStateOf("1") }
    var mlInput by remember { mutableStateOf("100") }

    val dose = doseInput.toFloatOrNull() ?: 0f
    val time = timeInput.toFloatOrNull() ?: 0f
    val volume = mlInput.toFloatOrNull() ?: 0f

    val rate = if (time > 0) volume / time else 0f
    val speed = if (time > 0) dose / time else 0f

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        BioElectricIonAnimation(color = Color.White, speedMultiplier = (rate / 50f).coerceIn(0.5f, 3f))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Electrolyte Protocols", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        DrugTagRow(listOf("KCl (Potassium)", "MgSO4 (Magnesium)", "Ca Gluconate"), onDrugClick)
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
                            if (speed > 0) Text(String.format(Locale.US, "Speed: %.1f / hr", speed), color = Color(0xFFB2DFDB), fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Text("mL/hr", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
                ClinicalPearlPanel(colorPrimary, "Action Potential Dynamics", "Potassium dictates cellular repolarization. Pushing K+ faster than 10 mEq/hr peripherally risks severe burning and fatal arrhythmias.")
            }
        }
    }
}

@Composable
fun BioElectricIonAnimation(color: Color, speedMultiplier: Float) {
    val transition = rememberInfiniteTransition(label = "ion_reactor")
    val corePulse by transition.animateFloat(initialValue = 0.6f, targetValue = 1.0f, animationSpec = infiniteRepeatable(animation = tween((1000 / speedMultiplier).toInt(), easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "")
    val rotationAngle by transition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween((3000 / speedMultiplier).toInt(), easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2

        drawCircle(color = color.copy(alpha = 0.4f * corePulse), radius = maxRadius * 0.8f * corePulse, center = center)
        drawCircle(color = color.copy(alpha = 0.3f), radius = maxRadius * 0.65f, style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
        drawCircle(color = color.copy(alpha = 0.15f), radius = maxRadius * 0.9f, style = Stroke(width = 2f))

        val ionRadius1 = maxRadius * 0.65f
        val ionRadius2 = maxRadius * 0.9f

        val innerAngle = Math.toRadians(rotationAngle.toDouble())
        val innerIon = Offset(center.x + (ionRadius1 * cos(innerAngle)).toFloat(), center.y + (ionRadius1 * sin(innerAngle)).toFloat())
        drawCircle(color = color, radius = 6f, center = innerIon)

        val outerAngle = Math.toRadians((-rotationAngle * 0.7f).toDouble())
        val outerIon = Offset(center.x + (ionRadius2 * cos(outerAngle)).toFloat(), center.y + (ionRadius2 * sin(outerAngle)).toFloat())
        drawCircle(color = color, radius = 4f, center = outerIon)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GLYCEMIC CONTROL ENGINE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InsulinEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF26C6DA)
    val colorSecondary = Color(0xFF00838F)

    var doseInput by remember { mutableStateOf("") }
    var unitsInput by remember { mutableStateOf("50") }
    var mlInput by remember { mutableStateOf("50") }

    val dose = doseInput.toFloatOrNull() ?: 0f
    val units = unitsInput.toFloatOrNull() ?: 0f
    val volume = mlInput.toFloatOrNull() ?: 0f

    val conc = if (volume > 0) units / volume else 0f
    val rate = if (conc > 0) dose / conc else 0f

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.cell_2), contentDescription = "Cell", modifier = Modifier.size(40.dp))
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

                ClinicalPearlPanel(colorPrimary, "Intracellular Transport", "IV Insulin translocates GLUT4 transporters to the cell membrane, actively pulling glucose and Potassium into the cells.")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FLUID RESUSCITATION ENGINE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FluidResuscitationCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF42A5F5)
    val colorSecondary = Color(0xFF1565C0)

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

    val burnDegreeStr = when {
        tbsa >= 50f -> "3rd Degree (Full Thickness)"
        tbsa >= 20f -> "2nd Degree (Partial Thickness)"
        tbsa > 0f -> "1st Degree (Superficial)"
        else -> "Healthy Skin"
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = burnIcon), contentDescription = "Skin Status", modifier = Modifier.size(45.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Fluid Resuscitation", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        DrugTagRow(listOf("Hartmann's (RL)", "0.9% Normal Saline", "Albumin"), onDrugClick)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                if (tbsa > 0) {
                    Text("BURN DEPTH ANALYSIS: $burnDegreeStr", color = colorPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width; val h = size.height
                            drawRoundRect(Color(0xFFFFCCBC), Offset(0f, 0f), Size(w, h*0.3f), CornerRadius(8f))
                            drawRoundRect(Color(0xFFFFAB91), Offset(0f, h*0.35f), Size(w, h*0.35f), CornerRadius(8f))
                            drawRoundRect(Color(0xFFFFF59D), Offset(0f, h*0.75f), Size(w, h*0.25f), CornerRadius(8f))
                            if (tbsa > 0f) drawRoundRect(Color(0xFFD32F2F).copy(0.6f), Offset(0f, 0f), Size(w, h*0.3f), CornerRadius(8f))
                            if (tbsa >= 20f) drawRoundRect(Color(0xFFC62828).copy(0.8f), Offset(0f, h*0.35f), Size(w, h*0.35f), CornerRadius(8f))
                            if (tbsa >= 50f) drawRoundRect(Color.Black.copy(0.8f), Offset(0f, h*0.75f), Size(w, h*0.25f), CornerRadius(8f))
                        }
                        Column(modifier = Modifier.fillMaxHeight().padding(start = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(" Epidermis", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
                            Text(" Dermis", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text(" Subcutaneous", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                }

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
                                Text("First 8 Hours", color = Color(0xFFBBDEFB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(if (first8HoursRate > 0) String.format(Locale.US, "%.1f", first8HoursRate) else "0.0", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Next 16 Hours", color = Color(0xFFBBDEFB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(if (next16HoursRate > 0) String.format(Locale.US, "%.1f", next16HoursRate) else "0.0", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RENAL FUNCTION ENGINE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RenalFunctionEngineCard() {
    val colorPrimary = Color(0xFF5C6BC0)
    val colorSecondary = Color(0xFF3949AB)

    var ageInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("70") }
    var creatInput by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }

    val age = ageInput.toFloatOrNull() ?: 0f
    val weight = weightInput.toFloatOrNull() ?: 0f
    val creat = creatInput.toFloatOrNull() ?: 0f

    val ccr = if (creat > 0 && weight > 0 && age > 0) {
        val base = ((140 - age) * weight) / (72 * creat)
        if (gender == "Male") base else base * 0.85f
    } else 0f

    Card(modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        AnimatedNephronGraphic(ccr = ccr)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Renal Function", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                        Text("Cockcroft-Gault Equation", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha=0.8f))
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(ageInput, { ageInput = it }, "Age (yrs)", colorPrimary, Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).border(2.dp, colorPrimary, RoundedCornerShape(12.dp)).clickable { gender = if (gender == "Male") "Female" else "Male" }.padding(18.dp), contentAlignment = Alignment.Center) {
                        Text(gender.uppercase(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = colorPrimary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(weightInput, { weightInput = it }, "Weight (kg)", colorPrimary, Modifier.weight(1f))
                    PerfectTextField(creatInput, { creatInput = it }, "Creatinine (mg/dL)", colorPrimary, Modifier.weight(1f))
                }

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colorPrimary).padding(24.dp)) {
                    Column {
                        Text("CREATININE CLEARANCE", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(if (ccr > 0) String.format(Locale.US, "%.1f", ccr) else "0.0", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(" mL/min", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5CAE9), modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
                        }
                    }
                }
                ClinicalPearlPanel(colorPrimary, "Renal Dose Adjustment", "Clearance <50 mL/min requires dose adjustments for many ICU antibiotics (e.g., Vancomycin) and anticoagulants (LMWH).")
            }
        }
    }
}

@Composable
fun AnimatedNephronGraphic(ccr: Float) {
    val speed = if (ccr > 0) (3000 / (ccr / 50f).coerceIn(0.5f, 2f)).toInt() else 4000
    val phase = rememberInfiniteTransition(label = "").animateFloat(0f, 1f, infiniteRepeatable(tween(speed, easing = LinearEasing))).value

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val tubePath = Path().apply {
            moveTo(w * 0.3f, h * 0.2f)
            lineTo(w * 0.3f, h * 0.7f)
            arcTo(androidx.compose.ui.geometry.Rect(w * 0.3f, h * 0.5f, w * 0.7f, h * 0.9f), 180f, -180f, false)
            lineTo(w * 0.7f, h * 0.2f)
        }
        drawPath(tubePath, color = Color.White.copy(alpha = 0.3f), style = Stroke(width = 6f, cap = StrokeCap.Round))

        val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
        pathMeasure.setPath(tubePath, false)
        val length = pathMeasure.length

        for (i in 0..2) {
            val particlePhase = (phase + (i * 0.33f)) % 1f
            val pos = pathMeasure.getPosition(length * particlePhase)
            drawCircle(Color.White, radius = 4f, center = pos)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HEMODYNAMICS ENGINE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HemodynamicsEngineCard() {
    val colorPrimary = Color(0xFFFFA726)
    val colorSecondary = Color(0xFFF57C00)

    var sbpInput by remember { mutableStateOf("120") }
    var dbpInput by remember { mutableStateOf("80") }
    var coInput by remember { mutableStateOf("5") }

    val sbp = sbpInput.toFloatOrNull() ?: 0f
    val dbp = dbpInput.toFloatOrNull() ?: 0f
    val co = coInput.toFloatOrNull() ?: 0f

    val map = (sbp + (2 * dbp)) / 3
    val svr = if (co > 0) ((map - 10) * 80) / co else 0f

    Card(modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(R.drawable.heart), contentDescription = null, modifier = Modifier.size(45.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Hemodynamics", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                        Text("MAP & SVR Analysis", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha=0.8f))
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // LIVE A-LINE TRACE
                Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Color(0xFF0F172A), RoundedCornerShape(16.dp)).border(1.dp, colorPrimary, RoundedCornerShape(16.dp))) {
                    val phase = rememberInfiniteTransition(label = "").animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing))).value
                    Text("LIVE A-LINE TRACE", color = colorPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(12.dp))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (size.width <= 0) return@Canvas
                        val path = Path(); path.moveTo(0f, size.height * 0.8f)
                        for (x in 0..size.width.toInt() step 2) {
                            val t = ((x / size.width) + phase) % 1f
                            val y = when {
                                t < 0.2f -> -100f * (t / 0.2f)
                                t < 0.4f -> -100f + 50f * ((t - 0.2f) / 0.2f)
                                t < 0.5f -> -50f - 15f * sin(((t - 0.4f) / 0.1f) * PI).toFloat()
                                else -> -50f + 50f * ((t - 0.5f) / 0.5f)
                            }
                            path.lineTo(x.toFloat(), (size.height * 0.8f) + (y * ((map / 100f).coerceIn(0.5f, 1.5f))))
                        }
                        drawPath(path, colorPrimary, style = Stroke(3f, join = StrokeJoin.Round))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PerfectTextField(sbpInput, { sbpInput = it }, "Systolic BP", colorPrimary, Modifier.weight(1f))
                    PerfectTextField(dbpInput, { dbpInput = it }, "Diastolic BP", colorPrimary, Modifier.weight(1f))
                }
                PerfectTextField(coInput, { coInput = it }, "Cardiac Output (L/min)", colorPrimary, Modifier.fillMaxWidth())

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colorPrimary).padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("MAP", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(if (map > 0) String.format(Locale.US, "%.0f", map) else "0", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SVR", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(if (svr > 0) String.format(Locale.US, "%.0f", svr) else "0", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS & DICTIONARIES
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfectTextField(value: String, onValueChange: (String) -> Unit, label: String, color: Color, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
        textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 16.sp),
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
// PHARMACOLOGY DIALOG & LIVE PQRST ECG
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DrugIntelligenceDialog(drugName: String, onDismiss: () -> Unit) {
    val themeColor = when {
        listOf("Noradrenaline", "Adrenaline", "Dopamine", "Dobutamine", "Milrinone").contains(drugName) -> Color(0xFFE53935)
        listOf("Propofol", "Midazolam", "Fentanyl", "Dexmedetomidine").contains(drugName) -> Color(0xFF8E24AA)
        listOf("Actrapid (Insulin)", "Novorapid").contains(drugName) -> Color(0xFF00ACC1)
        listOf("Hartmann's (RL)", "0.9% Normal Saline", "Albumin").contains(drugName) -> Color(0xFF1E88E5)
        else -> Color(0xFF00897B) // Electrolytes
    }

    val (className, considerations) = getDrugIntelligence(drugName)
    val isKcl = drugName == "KCl (Potassium)"

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth(0.95f).clip(RoundedCornerShape(24.dp)).background(Color.White)) {
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

                    if (isKcl) {
                        Text("ECG DYNAMICS: HYPERKALEMIA", color = Color(0xFFD32F2F), fontWeight = FontWeight.Black, fontSize = 14.sp)
                        LiveHyperkalemiaEcgWithLabels()
                    }

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

@OptIn(ExperimentalTextApi::class)
@Composable
fun LiveHyperkalemiaEcgWithLabels() {
    var isToxic by remember { mutableStateOf(false) }

    val tHeight by animateFloatAsState(if (isToxic) -85f else -25f, tween(1500), label="t")
    val pAlpha by animateFloatAsState(if (isToxic) 0f else 1f, tween(1500), label="p")
    val qrsWidth by animateFloatAsState(if (isToxic) 0.08f else 0.04f, tween(1500), label="qrs")

    val textMeasurer = rememberTextMeasurer()

    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { isToxic = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if(!isToxic) Color(0xFF00E676) else Color.LightGray)) { Text("Normal Sinus", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if(!isToxic) Color.White else Color.DarkGray) }
            Button(onClick = { isToxic = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if(isToxic) Color(0xFFD32F2F) else Color.LightGray)) { Text("Toxic K+", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if(isToxic) Color.White else Color.DarkGray) }
        }

        Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Color(0xFF0F172A), RoundedCornerShape(16.dp))) {
            val phase = rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(2500, easing = LinearEasing))).value

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 0) return@Canvas
                val path = Path()
                val centerY = size.height / 2f
                path.moveTo(0f, centerY)

                var pLoc = Offset.Zero
                var qrsLoc = Offset.Zero
                var tLoc = Offset.Zero

                for (x in 0..size.width.toInt() step 2) {
                    val time = ((x / size.width) + phase) % 1f
                    val y = when {
                        time in 0.1f..0.15f -> {
                            val v = -12f * sin((time - 0.1f) * 20 * PI).toFloat() * pAlpha
                            if (time > 0.12f && time < 0.13f) pLoc = Offset(x.toFloat() - 5f, centerY + v - 25f)
                            v
                        }
                        time in 0.35f..(0.35f + qrsWidth) -> {
                            val qrsT = (time - 0.35f) / qrsWidth
                            val v = when {
                                qrsT < 0.2f -> 20f * (qrsT / 0.2f)
                                qrsT < 0.5f -> 20f - 100f * ((qrsT - 0.2f) / 0.3f)
                                qrsT < 0.8f -> -80f + 110f * ((qrsT - 0.5f) / 0.3f)
                                else -> 30f - 30f * ((qrsT - 0.8f) / 0.2f)
                            }
                            if (qrsT > 0.45f && qrsT < 0.55f) qrsLoc = Offset(x.toFloat() - 15f, centerY + v - 30f)
                            v
                        }
                        time in 0.55f..0.85f -> {
                            val v = tHeight * sin(((time - 0.55f) / 0.3f) * PI).toFloat()
                            if (time > 0.68f && time < 0.72f) tLoc = Offset(x.toFloat() - 5f, centerY + v - 25f)
                            v
                        }
                        else -> 0f
                    }
                    path.lineTo(x.toFloat(), centerY + y)
                }

                drawPath(path, if(isToxic) Color(0xFFFF3B30) else Color(0xFF00E676), style = Stroke(4f, join = StrokeJoin.Round))

                val labelStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                if (pAlpha > 0.1f) drawText(textMeasurer, "P", pLoc, labelStyle.copy(color = Color.White.copy(alpha = pAlpha)))
                drawText(textMeasurer, "QRS", qrsLoc, labelStyle)
                drawText(textMeasurer, "T", tLoc, labelStyle)
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