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
import androidx.compose.material.icons.filled.ArrowBack
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
import kotlin.math.pow
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcuCalculatorsScreen(
    onNavigateBack: () -> Unit
) {
    val bgSoftWhite = Color(0xFFF4F7FB)
    val techBlue = Color(0xFF1976D2)

    var selectedDrug by remember { mutableStateOf<String?>(null) }
    var openedCalculator by remember { mutableStateOf<String?>(null) } // Controls Full Screen

    // If a calculator is open, the back button closes it instead of leaving the screen
    BackHandler(enabled = openedCalculator != null) {
        openedCalculator = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (openedCalculator == null) "ICU Clinical Protocols" else openedCalculator!!,
                        fontWeight = FontWeight.Black, fontSize = 20.sp, color = techBlue, letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (openedCalculator != null) openedCalculator = null else onNavigateBack() }) {
                        Icon(if (openedCalculator == null) Icons.Default.ArrowBack else Icons.Default.Close, contentDescription = "Back", tint = techBlue)
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
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp) // Adjusted spacing for full screen use
                ) {
                    IcuGlowingLaunchCard("Vasoactive Inotropes", "Noradrenaline, Adrenaline", Color(0xFFE53935), R.drawable.heart) { openedCalculator = "Vasoactive Inotropes" }
                    IcuGlowingLaunchCard("Sedation & Analgesia", "Propofol, Midazolam", Color(0xFF8E24AA), R.drawable.brain_1) { openedCalculator = "Sedation & Analgesia" }
                    IcuGlowingLaunchCard("Electrolyte Protocols", "KCl, MgSO4", Color(0xFF00897B), R.drawable.infusion_bag_green) { openedCalculator = "Electrolyte Protocols" }
                    IcuGlowingLaunchCard("Glycemic Control", "Actrapid Drips", Color(0xFF00ACC1), R.drawable.cell_2) { openedCalculator = "Glycemic Control" }
                    IcuGlowingLaunchCard("Fluid Resuscitation", "Parkland Burn Formula", Color(0xFF1E88E5), R.drawable.skin) { openedCalculator = "Fluid Resuscitation" }
                    IcuGlowingLaunchCard("Renal Function", "eGFR & Clearance", Color(0xFF6A1B9A), R.drawable.infusion_bag_blue) { openedCalculator = "Renal Function" }
                    IcuGlowingLaunchCard("Hemodynamics", "MAP & SVR", Color(0xFFD32F2F), R.drawable.heart) { openedCalculator = "Hemodynamics" }
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

            // Pharmacology Popup (With PQRST Animator)
            selectedDrug?.let { drugName ->
                DrugIntelligenceDialog(drugName = drugName, onDismiss = { selectedDrug = null })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GLOWING DASHBOARD MENU CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun IcuGlowingLaunchCard(title: String, subtitle: String, color: Color, iconRes: Int, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = color.copy(alpha = glowAlpha))
            .border(3.dp, color.copy(alpha = glowAlpha), RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        val gradient = Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f)), start = Offset(0f, 0f), end = Offset(1000f, 1000f))
        Row(modifier = Modifier.fillMaxSize().background(gradient).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(65.dp).background(Color.White.copy(0.25f), CircleShape), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(38.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(subtitle, color = Color.White.copy(0.85f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FLUID RESUSCITATION ENGINE (Burn Anatomy & Dynamic Pearls)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FluidResuscitationCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF1E88E5) // Ocean Blue
    val colorSecondary = Color(0xFF00ACC1) // Aqua

    var weightInput by remember { mutableStateOf("") }
    var tbsaInput by remember { mutableStateOf("") }

    val weight = weightInput.toFloatOrNull() ?: 0f
    val tbsa = tbsaInput.toFloatOrNull() ?: 0f

    val totalFluid = 4f * weight * tbsa
    val first8HoursRate = if (totalFluid > 0) (totalFluid / 2f) / 8f else 0f
    val next16HoursRate = if (totalFluid > 0) (totalFluid / 2f) / 16f else 0f

    // Dynamic Skin Image Evaluation
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

    val burnPearl = when {
        tbsa >= 50f -> "Destroys epidermis & dermis into subcutaneous fat. Painless (nerve destruction). Requires grafting & massive IV fluid resus."
        tbsa >= 20f -> "Affects epidermis & part of dermis. Blistered, red, extremely painful. High fluid shift risk. Start Parkland Formula."
        tbsa > 0f -> "Epidermis only. Red, painful, blanches with pressure. IV fluids typically NOT required based on 1st degree alone."
        else -> "Enter TBSA % to evaluate burn severity and calculate IV fluid requirements."
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = colorPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.White.copy(0.2f)).border(2.dp, Color.White.copy(0.5f), CircleShape)) {
                        Image(painter = painterResource(id = burnIcon), contentDescription = "Skin Status", modifier = Modifier.fillMaxSize().padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Fluid Resuscitation", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        DrugTagRow(listOf("Hartmann's (RL)", "0.9% Normal Saline", "Albumin"), onDrugClick)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // LEARNING DIAGRAM: BURN ANATOMY
                if (tbsa > 0) {
                    Text("BURN DEPTH ANALYSIS: $burnDegreeStr", color = colorPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            // Normal Layers
                            drawRoundRect(Color(0xFFFFCCBC), Offset(0f, 0f), Size(w, h*0.3f), CornerRadius(8f)) // Epidermis
                            drawRoundRect(Color(0xFFFFAB91), Offset(0f, h*0.35f), Size(w, h*0.35f), CornerRadius(8f)) // Dermis
                            drawRoundRect(Color(0xFFFFF59D), Offset(0f, h*0.75f), Size(w, h*0.25f), CornerRadius(8f)) // Subcut

                            // Damage Overlays based on TBSA
                            if (tbsa > 0f) drawRoundRect(Color(0xFFD32F2F).copy(0.6f), Offset(0f, 0f), Size(w, h*0.3f), CornerRadius(8f))
                            if (tbsa >= 20f) drawRoundRect(Color(0xFFC62828).copy(0.8f), Offset(0f, h*0.35f), Size(w, h*0.35f), CornerRadius(8f))
                            if (tbsa >= 50f) drawRoundRect(Color.Black.copy(0.8f), Offset(0f, h*0.75f), Size(w, h*0.25f), CornerRadius(8f))
                        }
                        Column(modifier = Modifier.fillMaxHeight().padding(start = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(" Epidermis", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
                            Text(" Dermis", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(" Subcutaneous", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                    ClinicalPearlPanel(colorPrimary, "Nursing Considerations", burnPearl)
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
            }
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

                    // Inject Mind-Blowing PQRST ECG
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

    // Physics engine for wave morphing
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

                // Track positions for Text Labels
                var pLoc = Offset.Zero
                var qrsLoc = Offset.Zero
                var tLoc = Offset.Zero

                for (x in 0..size.width.toInt() step 2) {
                    val time = ((x / size.width) + phase) % 1f
                    val y = when {
                        // P Wave (Fades out in hyperkalemia)
                        time in 0.1f..0.15f -> {
                            val v = -12f * sin((time - 0.1f) * 20 * PI).toFloat() * pAlpha
                            if (time > 0.12f && time < 0.13f) pLoc = Offset(x.toFloat() - 5f, centerY + v - 25f)
                            v
                        }
                        // QRS Complex (Widens)
                        time in 0.35f..(0.35f + qrsWidth) -> {
                            val qrsT = (time - 0.35f) / qrsWidth
                            val v = when {
                                qrsT < 0.2f -> 20f * (qrsT / 0.2f) // Q
                                qrsT < 0.5f -> 20f - 100f * ((qrsT - 0.2f) / 0.3f) // R
                                qrsT < 0.8f -> -80f + 110f * ((qrsT - 0.5f) / 0.3f) // S
                                else -> 30f - 30f * ((qrsT - 0.8f) / 0.2f)
                            }
                            if (qrsT > 0.45f && qrsT < 0.55f) qrsLoc = Offset(x.toFloat() - 15f, centerY + v - 30f)
                            v
                        }
                        // T Wave (Peaks massively)
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

                // Draw Dynamic Labels locked to the moving wave
                val labelStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                if (pAlpha > 0.1f) drawText(textMeasurer, "P", pLoc, labelStyle.copy(color = Color.White.copy(alpha = pAlpha)))
                drawText(textMeasurer, "QRS", qrsLoc, labelStyle)
                drawText(textMeasurer, "T", tLoc, labelStyle)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIOUS CALCULATORS (Unchanged Logic, Applied Fixes)
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
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
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
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
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

@Composable
fun ElectrolyteEngineCard(onDrugClick: (String) -> Unit) {
    val colorPrimary = Color(0xFF00897B) // Teal
    val colorSecondary = Color(0xFF7CB342) // Light Green

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
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
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
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorPrimary, colorSecondary), start = Offset(0f, 0f), end = Offset(1000f, 1000f)))) {
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

@Composable
fun RenalFunctionEngineCard() {
    val colorPrimary = Color(0xFF6A1B9A)
    val colorSecondary = Color(0xFFAB47BC)

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
                    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        val drip = rememberInfiniteTransition().animateFloat(0f, 60f, infiniteRepeatable(tween(1200, easing = LinearEasing))).value
                        Image(painter = painterResource(R.drawable.infusion_bag_blue), contentDescription = null, modifier = Modifier.size(50.dp))
                        Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(Color(0xFFFFC107), radius = 4.dp.toPx(), center = Offset(size.width/2, size.height/2 + drip)) }
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
                            Text(" mL/min", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorSecondary, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
                        }
                    }
                }
                ClinicalPearlPanel(colorPrimary, "Renal Dose Adjustment", "Clearance <50 mL/min requires dose adjustments for many ICU antibiotics (e.g., Vancomycin) and anticoagulants (LMWH).")
            }
        }
    }
}

@Composable
fun HemodynamicsEngineCard() {
    val colorPrimary = Color(0xFFD32F2F)
    val colorSecondary = Color(0xFFFF6F00)

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
                    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(R.drawable.heart), contentDescription = null, modifier = Modifier.size(50.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Hemodynamics", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                        Text("MAP & SVR Analysis", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha=0.8f))
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // LIVE A-LINE
                Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Color(0xFF0F172A), RoundedCornerShape(16.dp)).border(1.dp, colorPrimary, RoundedCornerShape(16.dp))) {
                    val phase = rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing))).value
                    Text("LIVE A-LINE TRACE", color = Color.Red.copy(0.8f), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(12.dp))
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
                        drawPath(path, Color.Red, style = Stroke(3f, join = StrokeJoin.Round))
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