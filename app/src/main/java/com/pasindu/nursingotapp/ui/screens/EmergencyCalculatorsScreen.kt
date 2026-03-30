// com/pasindu/nursingotapp/ui/screens/EmergencyCalculatorsScreen.kt
package com.pasindu.nursingotapp.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin

// --- BULLETPROOF COLOR CONVERSION HELPER ---
fun Color.toLegacyInt(): Int {
    return android.graphics.Color.argb(
        (this.alpha * 255).toInt(),
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt()
    )
}

// --- LIGHT THEME VIBRANT PALETTE ---
val EmergencyBgWhite = Color(0xFFF4F7FB)
val EmergencySlateDark = Color(0xFF1E293B)
val EmergencySlateLight = Color(0xFF64748B)

val AlertRedStart = Color(0xFFFF5252)
val AlertRedEnd = Color(0xFFD32F2F)
val AlertOrangeStart = Color(0xFFFFB74D)
val AlertOrangeEnd = Color(0xFFF57C00)
val AlertPurpleStart = Color(0xFFCE93D8)
val AlertPurpleEnd = Color(0xFF8E24AA)
val AlertCyanStart = Color(0xFF4DD0E1)
val AlertCyanEnd = Color(0xFF0097A6)
val AlertBlueStart = Color(0xFF64B5F6)
val AlertBlueEnd = Color(0xFF1976D2)

val NeonCyan = Color(0xFF00E5FF)
val NeonMagenta = Color(0xFFFF00FF)

val ClinicalPanelBg = Color(0xFFF8FAFC)
val ClinicalBorder = Color(0xFFE2E8F0)
val DividerColor = Color(0xFFCBD5E1)

// Universal IV Cannula Color Codes
val CannulaOrange = Color(0xFFFF9800) // 14G
val CannulaGray = Color(0xFF9E9E9E)   // 16G
val CannulaGreen = Color(0xFF4CAF50)  // 18G
val CannulaPink = Color(0xFFE91E63)   // 20G
val CannulaBlue = Color(0xFF2196F3)   // 22G

enum class DeliveryType { SYRINGE_PUSH, IV_INFUSION }

data class EmergencyDrug(
    val category: String,
    val name: String,
    val concentration: String,
    val doseText: String,
    val volumeText: String,
    val diluent: String,
    val pushSpeed: String,
    val cannulaGauge: String,
    val cannulaColor: Color,
    val preparation: String,
    val safetyTip: String,
    val clinicalPearl: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val deliveryType: DeliveryType,
    val maxContainerVolume: Float,
    val calculatedVolume: Float
)

// --- CLINICAL ECG RHYTHMS ---
enum class EcgRhythm(val displayName: String) {
    NSR("NORMAL SINUS RHYTHM"),
    VFIB("VENTRICULAR FIBRILLATION"),
    VTACH("PULSELESS V-TACH"),
    SVT("SUPRAVENTRICULAR TACHY"),
    AFIB("ATRIAL FIBRILLATION"),
    TORSADES("TORSADES DE POINTES"),
    ASYSTOLE("ASYSTOLE")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCalculatorsScreen() {
    val haptic = LocalHapticFeedback.current
    var weightInput by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }

    var expandedCardIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { delay(100); isVisible = true }

    val weight = weightInput.toFloatOrNull() ?: 0f

    val drugs = remember(weight) {
        listOf(
            run {
                val dose = if (weight == 0f) 0f else if (weight >= 50f) 1f else min(1f, weight * 0.01f)
                val vol = dose / 0.1f
                EmergencyDrug(
                    "CARDIAC ARREST", "Epinephrine (1:10,000)", "Ampoule: 0.1 mg/mL",
                    "${String.format("%.2f", dose)} mg", "${String.format("%.1f", vol)} mL",
                    "FLUSH 20mL NS", "RAPID (1-2s)",
                    "18G (Green) / IO", CannulaGreen,
                    "Use 1:10,000 pre-filled syringe. Round calculated volumes to nearest 0.1 mL.",
                    "Flush line rapidly with 20mL NS to force into central circulation.",
                    "Epi has a half-life of 2-3 mins. Rapid flush is required to trigger alpha-1 vasoconstriction.",
                    AlertRedStart, AlertRedEnd, DeliveryType.SYRINGE_PUSH, 10f, vol
                )
            },
            run {
                val dose = if (weight == 0f) 0f else min(0.5f, weight * 0.01f)
                val vol = dose / 1.0f
                EmergencyDrug(
                    "ANAPHYLAXIS", "Epinephrine (1:1,000)", "Ampoule: 1.0 mg/mL",
                    "${String.format("%.2f", dose)} mg", "${String.format("%.2f", vol)} mL",
                    "DO NOT DILUTE", "IM INJECTION",
                    "21G-23G IM", CannulaBlue,
                    "Use a strict 1 mL syringe for exact precision. Give IM in vastus lateralis.",
                    "WARNING: 10x stronger! DO NOT GIVE IV PUSH.",
                    "IV push in a beating heart can cause lethal arrhythmias. Vastus Lateralis allows massive absorption.",
                    AlertOrangeStart, AlertOrangeEnd, DeliveryType.SYRINGE_PUSH, 1f, vol
                )
            },
            run {
                val rawDose = weight * 60f
                val cappedDose = min(4000f, rawDose)
                val roundedDose = if (weight == 0f) 0f else (Math.round(cappedDose / 100.0) * 100).toFloat()
                val vol = roundedDose / 1000f
                EmergencyDrug(
                    "ACS / THROMBOSIS", "Heparin Bolus", "Vial: 1,000 U/mL",
                    "${roundedDose.toInt()} Units", "${String.format("%.1f", vol)} mL",
                    "UNDILUTED", "SLOW PUSH",
                    "20G (Pink)", CannulaPink,
                    "Round to nearest 100 Units. Draw strictly from 1,000 U/mL vial. Always use two-person check.",
                    "Max bolus is 4000 U. Must be followed by infusion.",
                    "1 mg of Heparin ≈ 100-150 IU. Follow with infusion strictly via IV pump (12-18 U/kg/hr).",
                    Color(0xFF5E35B1), Color(0xFF311B92), DeliveryType.SYRINGE_PUSH, 5f, vol
                )
            },
            run {
                val dose = if (weight == 0f) 0f else if (weight > 40f) 300f else min(300f, weight * 5f)
                val vol = dose / 50f
                EmergencyDrug(
                    "PULSELESS VT", "Amiodarone", "Ampoule: 50 mg/mL",
                    "${String.format("%.0f", dose)} mg", "${String.format("%.1f", vol)} mL",
                    "LIVE: D5W ONLY", "RAPID PUSH",
                    "18G (Green)", CannulaGreen,
                    "Draw with large bore needle (foams easily due to polysorbate 80).",
                    "Give undiluted ONLY for arrest. Do not shake.",
                    "If giving as a live infusion, it MUST be diluted in D5W, never Saline.",
                    AlertRedStart, AlertRedEnd, DeliveryType.SYRINGE_PUSH, 10f, vol
                )
            },
            run {
                val totalDose = if (weight == 0f) 0f else min(90f, weight * 0.9f)
                val bolusDose = totalDose * 0.1f
                EmergencyDrug(
                    "ISCHEMIC STROKE", "Alteplase (tPA)", "Reconstituted: 1 mg/mL",
                    "Bolus: ${String.format("%.1f", bolusDose)} mg\nInfuse: ${String.format("%.1f", totalDose * 0.9f)} mg",
                    "${String.format("%.1f", totalDose)} mL",
                    "STERILE WATER", "1 HR INFUSION",
                    "18G (Green) x2", CannulaGreen,
                    "Give 10% as a rapid bolus, and infuse the remainder via IV pump over 60 minutes.",
                    "DO NOT shake to avoid degrading fragile protein chains.",
                    "Strict BP control < 185/110 required before administration. Fibrinolytic agent.",
                    AlertPurpleStart, AlertPurpleEnd, DeliveryType.IV_INFUSION, 100f, totalDose
                )
            },
            run {
                val vol = if (weight == 0f) 0f else (weight * 20f)
                EmergencyDrug(
                    "HYPOVOLEMIA", "Fluid Resuscitation", "Normal Saline 0.9%",
                    "${(weight * 20).toInt()}-${(weight * 30).toInt()} mL", "${vol.toInt()} mL",
                    "CRYSTALLOID", "WIDE OPEN",
                    "14G (Orange)", CannulaOrange,
                    "Use pressure bag for rapid infusion. Assess lungs every 500mL.",
                    "If a pump is unavailable, strictly use microdrip sets (60 gtt/mL).",
                    "With a 60 gtt/mL microdrip set, 1 drop/min exactly equals 1 mL/hr. Poiseuille's Law: doubling diameter increases flow 16x!",
                    AlertBlueStart, AlertBlueEnd, DeliveryType.IV_INFUSION, 1000f, vol
                )
            },
            run {
                val dose = if (weight == 0f) 0f else weight * 2f
                val vol = dose / 50f
                EmergencyDrug(
                    "RSI: INDUCTION", "Ketamine", "Vial: 50 mg/mL",
                    "${String.format("%.0f", dose)} mg", "${String.format("%.1f", vol)} mL",
                    "NS / D5W", "SLOW (60s)",
                    "20G (Pink)", CannulaPink,
                    "Dilute in NS or D5W if needed.",
                    "Push slowly over 60s to prevent emergence delirium.",
                    "Excellent for asthmatics (bronchodilation) and shock (catecholamine release).",
                    AlertCyanStart, AlertCyanEnd, DeliveryType.SYRINGE_PUSH, 10f, vol
                )
            },
            run {
                val dose = if (weight == 0f) 0f else if (weight > 40f) 25f else (weight * 0.5f)
                val vol = if (weight > 40f) dose / 0.5f else dose / 0.1f
                val isInfusion = vol > 50f

                EmergencyDrug(
                    "HYPOGLYCEMIA", "Dextrose", if (weight > 40f) "D50% (0.5 g/mL)" else "D10% (0.1 g/mL)",
                    "${String.format("%.1f", dose)} g", "${String.format("%.1f", vol)} mL",
                    "UNDILUTED", if (isInfusion) "INFUSE OVER 15m" else "SLOW PUSH",
                    "18G (Green)", CannulaGreen,
                    "Given undiluted. Highly hyperosmolar/vesicant.",
                    "Draw back blood to confirm patency before administration.",
                    "D50 is immensely viscous. It causes severe tissue necrosis if infiltrated.",
                    AlertOrangeStart, AlertOrangeEnd,
                    if (isInfusion) DeliveryType.IV_INFUSION else DeliveryType.SYRINGE_PUSH,
                    if (isInfusion) 250f else 50f, vol
                )
            }
        )
    }

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(EmergencyBgWhite)) {
            EmergencyAuroraBackground(isVisible)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // LIGHT THEMED TELEMETRY COMMAND CENTER HEADER
                CommandCenterHeader(isVisible, weightInput) { newWeight ->
                    weightInput = newWeight
                    expandedCardIndex = null
                    if (newWeight.isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(600)) + fadeIn()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(drugs) { index, drug ->
                            val isExpanded = expandedCardIndex == index

                            EmergencyAccordionCard(
                                drug = drug,
                                isActive = weight > 0f,
                                isExpanded = isExpanded,
                                onCardClick = {
                                    expandedCardIndex = if (isExpanded) null else index
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(60.dp)) }
                    }
                }
            }
        }
    }
}

// --- SYNCED HEARTBEAT ICON ---
@Composable
fun AnimatedHeart(rhythm: EcgRhythm, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")

    val duration = when(rhythm) {
        EcgRhythm.SVT -> 300
        EcgRhythm.VTACH -> 400
        EcgRhythm.VFIB -> 150
        EcgRhythm.NSR -> 800
        EcgRhythm.AFIB -> 800
        EcgRhythm.TORSADES -> 400
        EcgRhythm.ASYSTOLE -> 1200
    }

    val pumpTime = when(rhythm) {
        EcgRhythm.NSR -> 240
        EcgRhythm.SVT -> 120
        EcgRhythm.AFIB -> 240
        EcgRhythm.VTACH -> 100
        else -> duration / 2
    }

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (rhythm == EcgRhythm.ASYSTOLE) 1f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = duration
                if (rhythm == EcgRhythm.VFIB) {
                    1.00f at 0
                    1.15f at 75
                    1.00f at 150
                } else if (rhythm == EcgRhythm.ASYSTOLE) {
                    1.00f at 0
                    1.00f at duration
                } else {
                    1.00f at 0
                    1.15f at pumpTime
                    1.00f at pumpTime + 100
                    1.00f at duration
                }
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartbeat_scale"
    )

    Image(
        painter = painterResource(id = R.drawable.heart),
        contentDescription = "Heart",
        modifier = modifier.scale(scale)
    )
}

// --- FULL CARD ECG TELEMETRY HEADER ---
@Composable
fun CommandCenterHeader(isVisible: Boolean, weight: String, onWeightChange: (String) -> Unit) {
    var currentRhythm by remember { mutableStateOf(EcgRhythm.NSR) }
    var ecgPhase by remember { mutableFloatStateOf(0f) }

    // REALISTIC 4.0 SECOND SWEEP (ZOOMED IN HORIZONTALLY)
    LaunchedEffect(Unit) {
        val rhythms = EcgRhythm.values()
        var index = 0
        while (true) {
            currentRhythm = rhythms[index]
            repeat(2) {
                ecgPhase = 0f
                animate(0f, 1f, animationSpec = tween(4000, easing = LinearEasing)) { value, _ ->
                    ecgPhase = value
                }
            }
            index = (index + 1) % rhythms.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_and_colors")

    val bgColor1 by infiniteTransition.animateColor(
        initialValue = Color(0xFFF0F8FF),
        targetValue = Color(0xFFF3E5F5),
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Reverse), label = "bg1"
    )
    val bgColor2 by infiniteTransition.animateColor(
        initialValue = Color(0xFFE0F7FA),
        targetValue = Color(0xFFFFF0F5),
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Reverse), label = "bg2"
    )
    val bgColor3 by infiniteTransition.animateColor(
        initialValue = Color(0xFFE8F5E9),
        targetValue = Color(0xFFFFF3E0),
        animationSpec = infiniteRepeatable(tween(5500, easing = LinearEasing), RepeatMode.Reverse), label = "bg3"
    )

    val dynamicFluidBackground = Brush.linearGradient(
        colors = listOf(Color.White, bgColor1, bgColor2, bgColor3),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val glowingSolidBorderColor by infiniteTransition.animateColor(
        initialValue = AlertCyanStart.copy(alpha = 0.8f),
        targetValue = AlertCyanStart.copy(alpha = 0.8f),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 8000
                AlertCyanStart.copy(alpha = 0.8f) at 0
                AlertBlueStart.copy(alpha = 0.8f) at 2000
                AlertPurpleStart.copy(alpha = 0.8f) at 4000
                Color(0xFF00E676).copy(alpha = 0.8f) at 6000
                AlertCyanStart.copy(alpha = 0.8f) at 8000
            },
            repeatMode = RepeatMode.Restart
        ), label = "solid_border"
    )

    val shadowTint by infiniteTransition.animateColor(
        initialValue = AlertBlueStart.copy(alpha = 0.15f),
        targetValue = AlertPurpleStart.copy(alpha = 0.35f),
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "shadow"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )

    val entranceOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else -100f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
            .offset(y = entranceOffset.dp)
            .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = shadowTint, ambientColor = shadowTint)
            .clip(RoundedCornerShape(24.dp))
            .background(dynamicFluidBackground)
            .drawBehind {
                val gridColor = AlertBlueStart.copy(alpha = 0.05f)
                val gridColorThick = AlertBlueStart.copy(alpha = 0.1f)
                val gridStep = 15f

                for (x in 0..(size.width / gridStep).toInt()) {
                    val isThick = x % 5 == 0
                    drawLine(if(isThick) gridColorThick else gridColor, Offset(x * gridStep, 0f), Offset(x * gridStep, size.height), strokeWidth = if(isThick) 2f else 1f)
                }
                for (y in 0..(size.height / gridStep).toInt()) {
                    val isThick = y % 5 == 0
                    drawLine(if(isThick) gridColorThick else gridColor, Offset(0f, y * gridStep), Offset(size.width, y * gridStep), strokeWidth = if(isThick) 2f else 1f)
                }
            }
            .border(2.5.dp, glowingSolidBorderColor, RoundedCornerShape(24.dp))
            .height(115.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. HEART
            Box(
                modifier = Modifier.padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedHeart(
                    rhythm = currentRhythm,
                    modifier = Modifier.size(64.dp)
                )
            }

            // 2. ECG WAVE & LABEL
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cy = h / 2f

                    fun getEcgY(t: Float, rhythm: EcgRhythm): Float {
                        var y = cy
                        val ampH = 48f
                        when (rhythm) {
                            EcgRhythm.NSR -> {
                                val localT = t % 800f
                                if (localT in 100f..180f) y -= sin((localT - 100f) / 80f * PI).toFloat() * ampH * 0.15f
                                if (localT in 220f..280f) {
                                    if (localT < 235f) y += ampH * 0.1f
                                    else if (localT < 245f) y -= ampH * 0.75f
                                    else if (localT < 265f) y += ampH * 0.25f
                                }
                                if (localT in 400f..560f) y -= sin((localT - 400f) / 160f * PI).toFloat() * ampH * 0.25f
                            }
                            EcgRhythm.SVT -> {
                                val localT = t % 300f
                                if (localT in 100f..150f) {
                                    if (localT < 115f) y += ampH * 0.1f
                                    else if (localT < 125f) y -= ampH * 0.7f
                                    else if (localT < 140f) y += ampH * 0.2f
                                }
                                if (localT in 180f..260f) y -= sin((localT - 180f) / 80f * PI).toFloat() * ampH * 0.2f
                            }
                            EcgRhythm.VTACH -> {
                                val localT = t % 400f
                                y -= sin((localT / 400f) * 2 * PI).toFloat() * ampH * 0.45f
                            }
                            EcgRhythm.VFIB -> {
                                y -= (sin(t * 0.015f) * ampH * 0.2f + cos(t * 0.04f) * ampH * 0.1f + sin(t * 0.008f) * ampH * 0.15f).toFloat()
                            }
                            EcgRhythm.TORSADES -> {
                                val amplitude = sin(t * 0.0015f) * ampH * 0.45f
                                y -= (sin((t % 400f) / 400f * 2 * PI) * amplitude).toFloat()
                            }
                            EcgRhythm.AFIB -> {
                                val localT = t % 800f
                                val fib = (sin(t * 0.03f) * ampH * 0.06f + cos(t * 0.06f) * ampH * 0.04f).toFloat()
                                y -= fib
                                if (localT in 200f..280f) {
                                    if (localT < 220f) y += ampH * 0.1f
                                    else if (localT < 240f) y -= ampH * 0.75f
                                    else if (localT < 260f) y += ampH * 0.25f
                                }
                                if (localT in 350f..480f) y -= sin((localT - 350f) / 130f * PI).toFloat() * ampH * 0.15f
                            }
                            EcgRhythm.ASYSTOLE -> {
                                y -= (sin(t * 0.003f) * ampH * 0.02f).toFloat()
                            }
                        }
                        return y
                    }

                    val ecgPath = Path().apply {
                        moveTo(0f, cy)
                        val steps = 600
                        for (i in 0..steps) {
                            val progress = i.toFloat() / steps
                            val x = progress * w
                            val tMs = progress * 4000f
                            lineTo(x, getEcgY(tMs, currentRhythm))
                        }
                    }

                    val headX = ecgPhase * w
                    val gapWidth = w * 0.05f
                    val traceColor = AlertRedEnd

                    clipRect(left = 0f, right = headX, top = 0f, bottom = h) {
                        drawPath(ecgPath, color = traceColor, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    if (headX + gapWidth < w) {
                        clipRect(left = headX + gapWidth, right = w, top = 0f, bottom = h) {
                            drawPath(ecgPath, color = traceColor.copy(alpha = 0.35f), style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }

                // --- DYNAMIC FONT SIZER FOR LONG NAMES ---
                val labelText = currentRhythm.displayName
                val labelFontSize = if (labelText.length > 20) 8.5.sp else 11.sp
                val labelLetterSpacing = if (labelText.length > 20) 0.5.sp else 1.sp

                Text(
                    text = labelText,
                    color = AlertRedEnd,
                    fontSize = labelFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = labelLetterSpacing,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }

            // 3. WEIGHT INPUT BOX
            val isWeightEmpty = weight.isEmpty()
            Box(
                modifier = Modifier.padding(start = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (isWeightEmpty) "ENTER WEIGHT" else "WEIGHT (KG)",
                        color = if (isWeightEmpty) AlertRedEnd.copy(alpha = pulseAlpha) else EmergencySlateLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    BasicTextField(
                        value = weight,
                        onValueChange = onWeightChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = EmergencySlateDark,
                            textAlign = TextAlign.End
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.width(65.dp)) {
                                if (weight.isEmpty()) {
                                    Text("0.0", color = EmergencySlateLight.copy(alpha = 0.3f), fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End)
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    }
}

// ─── MASSIVE CONTINUOUS ACCUMULATION SYRINGE WITH TRUE GRAVITY PHYSICS ───
@Composable
fun AnimatedMassiveSyringe(drug: EmergencyDrug, isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "syringe_engine")

    val globalTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(5000000, easing = LinearEasing)), label = ""
    )

    val cycleCount = globalTime.toInt()
    val phase = globalTime - cycleCount

    val plungerWobble by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(50, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = ""
    )

    val fillRatio = if (!isActive) 0f else when {
        phase < 0.25f -> phase / 0.25f
        phase < 0.40f -> 1f
        phase < 0.70f -> 1f - ((phase - 0.40f) / 0.30f)
        else          -> 0f
    }

    val isDrawPhase  = isActive && phase < 0.25f
    val isReadyPhase = isActive && phase in 0.25f..0.40f
    val isPushPhase  = isActive && phase in 0.40f..0.70f

    val pushProgress = if (!isActive) 0f else when {
        phase < 0.40f -> 0f
        phase > 0.70f -> 1f
        else -> (phase - 0.40f) / 0.30f
    }

    val totalPushes = if (isActive) cycleCount + pushProgress else 0f
    val puddleFillLevel = min(1f, totalPushes / 5f)

    val targetVol = drug.calculatedVolume
    val maxContainer = drug.maxContainerVolume
    val maxFillPct = if (maxContainer > 0) min(1f, targetVol / maxContainer) else 0f
    val currentFill = maxFillPct * fillRatio

    val actionText = when {
        !isActive    -> ""
        isDrawPhase  -> "DRAWING MEDICATION..."
        isReadyPhase -> "READY TO PUSH"
        isPushPhase  -> "PUSH! (${drug.pushSpeed})"
        else         -> "FLUSH LINE"
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
        val w = size.width
        val h = size.height
        val cy = h * 0.35f

        val barrelHalf = when {
            maxContainer <= 1f -> 20f
            maxContainer <= 10f -> 38f
            else -> 56f
        }

        val barrelRight = w * 0.65f
        val barrelLeft = when {
            maxContainer <= 1f -> w * 0.35f
            maxContainer <= 10f -> w * 0.30f
            else -> w * 0.20f
        }

        val barrelW = barrelRight - barrelLeft
        val maxLiquidLength = barrelW * 0.90f

        val liquidLength = maxLiquidLength * currentFill
        val stopperX = barrelRight - liquidLength
        val stopperXFinal = stopperX + if (isPushPhase) plungerWobble * 2f else 0f

        val rodLength = maxLiquidLength + 40f
        val handleX = stopperXFinal - rodLength

        clipRect(left = 0f, right = w, top = 0f, bottom = h) {

            // --- 1. CONTINUOUS PUDDLE PHYSICS ---
            if (puddleFillLevel > 0.01f) {
                val maxPuddleH = 55f
                val puddleH = maxPuddleH * puddleFillLevel
                val puddleW = w * 0.9f
                val puddleL = w * 0.05f
                val puddleTop = h - puddleH

                val puddleAlpha = 0.2f + (0.7f * puddleFillLevel)

                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(drug.gradientStart.copy(alpha = puddleAlpha * 0.4f), drug.gradientEnd.copy(alpha = puddleAlpha)), startY = puddleTop, endY = h),
                    topLeft = Offset(puddleL, puddleTop), size = Size(puddleW, puddleH), cornerRadius = CornerRadius(puddleH / 2, puddleH / 2)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f * puddleFillLevel),
                    topLeft = Offset(puddleL + 15f, puddleTop + 2f), size = Size(puddleW - 30f, 6f), cornerRadius = CornerRadius(3f, 3f)
                )
            }

            // --- 2. ADVANCED BEZIER FLUID DYNAMICS (Real Gravity) ---
            val needleLen = 40f
            val hubW = 18f
            val needleStartX = barrelRight + hubW + 8f
            val needleEndX = needleStartX + needleLen

            if (isPushPhase && isActive) {
                val squirtIntensity = sin(pushProgress * PI).toFloat()
                val streamStartX = needleEndX
                val streamStartY = cy
                val streamDistance = 100f * squirtIntensity
                val streamEndX = needleEndX + streamDistance
                val streamEndY = h - (55f * puddleFillLevel)

                val streamPath = Path().apply {
                    moveTo(streamStartX, streamStartY)
                    cubicTo(
                        streamStartX + streamDistance * 0.6f, streamStartY,
                        streamEndX - streamDistance * 0.1f, streamEndY,
                        streamEndX, streamEndY
                    )
                }

                drawPath(streamPath, color = drug.gradientStart.copy(alpha = 0.5f * squirtIntensity), style = Stroke(width = 14f * squirtIntensity, cap = StrokeCap.Round))
                drawPath(streamPath, color = drug.gradientEnd.copy(alpha = 0.9f * squirtIntensity), style = Stroke(width = 7f * squirtIntensity, cap = StrokeCap.Round))
                drawPath(streamPath, color = Color.White.copy(alpha = 0.6f * squirtIntensity), style = Stroke(width = 2.5f * squirtIntensity, cap = StrokeCap.Round))

                if (squirtIntensity > 0.2f) {
                    val dropScattering = (globalTime * 80f) % 1f
                    drawCircle(drug.gradientEnd, radius = 3.5f * squirtIntensity, center = Offset(streamEndX - 15f + 30f * dropScattering, streamEndY - 10f - 20f * ((globalTime * 43f) % 1f)))
                    drawCircle(drug.gradientStart, radius = 2.5f * squirtIntensity, center = Offset(streamEndX + 10f - 20f * dropScattering, streamEndY - 5f - 15f * ((globalTime * 67f) % 1f)))
                }
            }

            // --- 3. REALISTIC CROSS-RIB PLUNGER ROD ---
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFCFD8DC), Color(0xFFFFFFFF), Color(0xFF90A4AE)), startY = cy - 4f, endY = cy + 4f),
                topLeft = Offset(handleX, cy - 4f), size = Size(stopperXFinal - handleX, 8f)
            )
            drawRoundRect(Color(0xFFB0BEC5), Offset(handleX, cy - barrelHalf * 0.5f), Size(stopperXFinal - handleX, 2f))
            drawRoundRect(Color(0xFF90A4AE), Offset(handleX, cy + barrelHalf * 0.5f - 2f), Size(stopperXFinal - handleX, 2f))

            val thumbW = 12f
            val thumbH = barrelHalf * 3.2f
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF90A4AE), Color(0xFFECEFF1), Color(0xFF607D8B)), startY = cy - thumbH/2, endY = cy + thumbH/2),
                topLeft = Offset(handleX - thumbW, cy - thumbH/2), size = Size(thumbW, thumbH), cornerRadius = CornerRadius(4f)
            )
            drawRoundRect(Color(0xFF78909C), Offset(handleX - thumbW + 2f, cy - thumbH/2 + 2f), Size(thumbW - 4f, thumbH - 4f), CornerRadius(2f))

            // --- 4. RUBBER STOPPER ---
            val stopperW = 16f
            val stopperH = barrelHalf * 1.9f
            drawRoundRect(Color(0xFF263238), Offset(stopperXFinal - stopperW, cy - stopperH/2), Size(stopperW, stopperH), CornerRadius(4f))
            drawLine(Color.Black, Offset(stopperXFinal - stopperW + 3f, cy - stopperH/2), Offset(stopperXFinal - stopperW + 3f, cy + stopperH/2), strokeWidth = 2f)
            drawLine(Color.Black, Offset(stopperXFinal - 3f, cy - stopperH/2), Offset(stopperXFinal - 3f, cy + stopperH/2), strokeWidth = 2f)

            // --- 5. DRUG LIQUID (Inside Syringe) ---
            if (liquidLength > 1f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(drug.gradientStart.copy(alpha=0.6f), drug.gradientEnd, drug.gradientEnd.copy(alpha=0.8f)), startY = cy - barrelHalf + 3f, endY = cy + barrelHalf - 3f),
                    topLeft = Offset(stopperXFinal, cy - barrelHalf + 3f), size = Size(barrelRight - stopperXFinal, (barrelHalf - 3f) * 2f), cornerRadius = CornerRadius(3f)
                )
                drawOval(Color.White.copy(alpha = 0.3f), Offset(stopperXFinal + 2f, cy - barrelHalf + 4f), Size(8f, barrelHalf * 1.5f))
            }

            // --- 6. GLASS BARREL ---
            val flangeW = 18f
            val flangeH = barrelHalf * 3.4f
            drawRoundRect(Color.White.copy(alpha = 0.6f), Offset(barrelLeft - flangeW, cy - flangeH/2), Size(flangeW, flangeH), CornerRadius(6f))
            drawRoundRect(Color(0xFF90A4AE).copy(alpha = 0.8f), Offset(barrelLeft - flangeW, cy - flangeH/2), Size(flangeW, flangeH), CornerRadius(6f), style = Stroke(4f))

            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha=0.5f), Color.Transparent, Color.Black.copy(alpha=0.15f)), startY = cy - barrelHalf, endY = cy + barrelHalf),
                topLeft = Offset(barrelLeft, cy - barrelHalf), size = Size(barrelW, barrelHalf * 2f), cornerRadius = CornerRadius(6f)
            )
            drawRoundRect(Color(0xFF64B5F6).copy(alpha=0.6f), Offset(barrelLeft, cy - barrelHalf), Size(barrelW, barrelHalf * 2f), CornerRadius(6f), style = Stroke(4f))

            // --- 7. PRINTED VOLUME GAUGE ---
            val tickCount = if (maxContainer <= 1f) 5 else 10
            val tickStep = maxLiquidLength / tickCount
            val volStep = maxContainer / tickCount

            for (i in 0..tickCount) {
                val tickX = barrelRight - (i * tickStep)
                val isMajor = i % 2 == 0 || tickCount <= 5
                val tickH = if (isMajor) barrelHalf * 0.65f else barrelHalf * 0.35f

                drawLine(Color(0xFF1E293B).copy(alpha=0.85f), Offset(tickX, cy), Offset(tickX, cy + tickH), strokeWidth = if(isMajor) 4f else 2.5f)

                if (isMajor) {
                    val volValue = volStep * i
                    val textStr = if (maxContainer <= 1f) String.format("%.1f", volValue).replace(".0","") else volValue.toInt().toString()
                    val textY = cy - 10f

                    val shadowPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = if (maxContainer <= 1f) 18f else 26f
                        isFakeBoldText = true
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeWidth = 4f
                    }
                    drawContext.canvas.nativeCanvas.drawText(textStr, tickX, textY, shadowPaint)

                    val paint = Paint().apply {
                        color = android.graphics.Color.parseColor("#1E293B")
                        textSize = if (maxContainer <= 1f) 18f else 26f
                        isFakeBoldText = true
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(textStr, tickX, textY, paint)
                }
            }

            if (isActive) {
                val targetX = barrelRight - (maxLiquidLength * maxFillPct)
                drawLine(drug.gradientEnd, Offset(targetX, cy - barrelHalf - 14f), Offset(targetX, cy + barrelHalf + 14f), strokeWidth = 5f)
                drawOval(drug.gradientEnd, Offset(targetX - 7f, cy - barrelHalf - 18f), Size(14f, 14f))
            }

            // --- 8. NEEDLE HUB & NEEDLE ---
            val hubH = barrelHalf * 0.8f
            val hubPath = Path().apply { moveTo(barrelRight, cy - hubH/2); lineTo(barrelRight + hubW, cy - 6f); lineTo(barrelRight + hubW, cy + 6f); lineTo(barrelRight, cy + hubH/2); close() }
            drawPath(hubPath, Color(0xFFCFD8DC))
            drawPath(hubPath, Color(0xFF90A4AE), style = Stroke(3f))

            drawRect(drug.cannulaColor, Offset(barrelRight + hubW, cy - 8f), Size(8f, 16f))

            drawLine(Color(0xFFBDBDBD), Offset(needleStartX, cy), Offset(needleEndX, cy), strokeWidth = 6f)
            drawLine(Color.White, Offset(needleStartX, cy - 1.5f), Offset(needleEndX, cy - 1.5f), strokeWidth = 2.5f)

            if (isActive && actionText.isNotEmpty()) {
                val bannerPaint = Paint().apply {
                    color = if (isPushPhase) android.graphics.Color.parseColor("#D32F2F") else if (isReadyPhase) android.graphics.Color.parseColor("#388E3C") else android.graphics.Color.parseColor("#1565C0")
                    textSize = 34f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(actionText, w / 2f, cy - barrelHalf - 26f, bannerPaint)
            }
        }
    }
}

// ─── ALARIS-STYLE IV PUMP WITH SQUASH-AND-STRETCH DRIP ───
@Composable
fun AnimatedProIVPump(drug: EmergencyDrug, isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "drip")
    val dropPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)), label = ""
    )
    val pumpArrowsPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = ""
    )

    Box(modifier = Modifier.padding(top = 16.dp)) {
        Canvas(modifier = Modifier.width(110.dp).height(240.dp)) {
            val baseW = 240f
            val baseH = 580f
            val scaleFactor = min(size.width / baseW, size.height / baseH)

            withTransform({
                scale(scaleFactor, scaleFactor, Offset(0f, 0f))
                translate((size.width / scaleFactor - baseW) / 2f, (size.height / scaleFactor - baseH) / 2f + 30f)
            }) {
                val cx = baseW / 2f

                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF9E9E9E), Color(0xFFE8E8E8), Color(0xFF757575))),
                    topLeft = Offset(cx - 5f, 10f), size = Size(10f, 560f), cornerRadius = CornerRadius(5f)
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF9E9E9E), Color(0xFFE8E8E8), Color(0xFF9E9E9E))),
                    topLeft = Offset(cx - 90f, 10f), size = Size(180f, 8f), cornerRadius = CornerRadius(4f)
                )

                val bagTop = 30f
                val bagBot = 160f
                val bagLeft = cx - 45f
                val bagRight = cx + 45f
                val bagH = bagBot - bagTop

                val loopPath = Path().apply {
                    moveTo(cx, bagTop)
                    quadraticBezierTo(cx, bagTop - 9f, cx - 6f, bagTop - 11f)
                    quadraticBezierTo(cx - 12f, bagTop - 13f, cx - 13f, bagTop - 5f)
                    quadraticBezierTo(cx - 14f, bagTop + 3f, cx - 6f, bagTop + 3f)
                }
                drawPath(loopPath, Color(0xFF607D8B), style = Stroke(width = 3f, cap = StrokeCap.Round))

                val bagPath = Path().apply {
                    moveTo(bagLeft + 15f, bagTop)
                    lineTo(bagRight - 15f, bagTop)
                    quadraticBezierTo(bagRight + 4f, bagTop + 20f, bagRight + 6f, bagTop + bagH * 0.55f)
                    quadraticBezierTo(bagRight + 6f, bagBot, cx, bagBot + 5f)
                    quadraticBezierTo(bagLeft - 6f, bagBot, bagLeft - 6f, bagTop + bagH * 0.55f)
                    quadraticBezierTo(bagLeft - 4f, bagTop + 20f, bagLeft + 15f, bagTop)
                    close()
                }
                drawPath(bagPath, Brush.horizontalGradient(listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFFECEFF1), Color(0xFF90A4AE))))

                val fluidTop = bagTop + bagH * 0.42f
                val fluidPath = Path().apply {
                    moveTo(bagLeft - 6f, fluidTop)
                    lineTo(bagRight + 6f, fluidTop)
                    lineTo(bagRight + 6f, bagTop + bagH * 0.55f)
                    quadraticBezierTo(bagRight + 6f, bagBot, cx, bagBot + 5f)
                    quadraticBezierTo(bagLeft - 6f, bagBot, bagLeft - 6f, bagTop + bagH * 0.55f)
                    close()
                }
                drawPath(fluidPath, Brush.verticalGradient(listOf(drug.gradientStart.copy(alpha = 0.6f), drug.gradientEnd.copy(alpha = 0.9f)), startY = fluidTop, endY = bagBot))
                drawPath(path = bagPath, color = Color(0xFF90A4AE), style = Stroke(width = 1.5f))

                drawRoundRect(Color.White.copy(alpha = 0.6f), topLeft = Offset(cx - 30f, bagTop + 20f), size = Size(60f, 40f), cornerRadius = CornerRadius(4f))
                drawContext.canvas.nativeCanvas.apply {
                    drawText(drug.name.take(6), cx - 22f, bagTop + 36f, android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#37474F"); textSize = 16f; isFakeBoldText = true; isAntiAlias = true })
                    drawText(drug.volumeText, cx - 18f, bagTop + 52f, android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#546E7A"); textSize = 12f; isAntiAlias = true })
                }

                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)), startY = bagBot + 4f, endY = bagBot + 18f), topLeft = Offset(cx - 10f, bagBot + 4f), size = Size(20f, 14f), cornerRadius = CornerRadius(3f))
                val spikeY = bagBot + 18f
                drawPath(Path().apply { moveTo(cx - 5f, spikeY); lineTo(cx, spikeY + 10f); lineTo(cx + 5f, spikeY); close() }, Color(0xFF0D47A1))

                drawLine(color = Color(0xFFB0BEC5), start = Offset(cx, spikeY + 10f), end = Offset(cx, 210f), strokeWidth = 6f)
                drawLine(color = drug.gradientEnd.copy(alpha = 0.3f), start = Offset(cx, spikeY + 10f), end = Offset(cx, 210f), strokeWidth = 2f)

                val chamberTop = 210f
                val chamberH2  = 70f
                val chamberBot = chamberTop + chamberH2
                val chamberW2  = 32f

                drawRoundRect(Brush.horizontalGradient(listOf(Color(0x72B2EBF2), Color(0xF2E0F7FA), Color(0x7280DEEA))), topLeft = Offset(cx - chamberW2 / 2f, chamberTop), size = Size(chamberW2, chamberH2), cornerRadius = CornerRadius(16f))
                drawRoundRect(color = Color(0xFF80DEEA), topLeft = Offset(cx - chamberW2 / 2f, chamberTop), size = Size(chamberW2, chamberH2), cornerRadius = CornerRadius(16f), style = Stroke(width = 1.5f))
                drawRoundRect(color = drug.gradientEnd.copy(alpha = 0.65f), topLeft = Offset(cx - chamberW2 / 2f, chamberTop + chamberH2 * 0.5f), size = Size(chamberW2, chamberH2 * 0.5f), cornerRadius = CornerRadius(16f))

                if (isActive) {
                    val dropY = chamberTop + 5f + ((chamberH2 - 25f) * dropPhase)
                    val dropStretch = if (dropPhase < 0.2f) 1f else if (dropPhase < 0.8f) 1.5f else 0.8f
                    drawOval(color = drug.gradientEnd.copy(alpha = 0.9f), topLeft = Offset(cx - 6f, dropY), size = Size(12f, 16f * dropStretch))
                } else {
                    drawOval(color = drug.gradientEnd.copy(alpha = 0.5f), topLeft = Offset(cx - 4f, chamberTop + chamberH2 * 0.28f), size = Size(8f, 12f))
                }

                drawRoundRect(color = Color(0xFF80CBC4), topLeft = Offset(cx - 5f, chamberBot), size = Size(10f, 8f), cornerRadius = CornerRadius(2f))

                val pumpTop2 = 330f
                val pumpH2   = 180f
                val pumpW2   = 120f
                val pumpL2   = cx - 60f
                val pumpR2   = cx + 60f

                val tubePath = Path().apply {
                    moveTo(cx, chamberBot + 8f)
                    cubicTo(cx, chamberBot + 30f, cx + 45f, pumpTop2 - 20f, cx + 45f, pumpTop2 + 10f)
                }
                drawPath(tubePath, Brush.horizontalGradient(listOf(Color(0x8090A4AE), Color(0xF0CFD8DC), Color(0x8078909C))), style = Stroke(width = 5f, cap = StrokeCap.Round))
                drawPath(tubePath, color = Color.White.copy(alpha = 0.25f), style = Stroke(width = 1.5f, cap = StrokeCap.Round))

                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF607D8B), Color(0xFFCFD8DC), Color(0xFFB0BEC5), Color(0xFF546E7A))), topLeft = Offset(pumpL2, pumpTop2), size = Size(pumpW2, pumpH2), cornerRadius = CornerRadius(8f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE0F2F1), Color(0xFF80CBC4)), startY = pumpTop2, endY = pumpTop2 + pumpH2), topLeft = Offset(pumpL2 + 4f, pumpTop2 + 4f), size = Size(pumpW2 - 8f, pumpH2 - 8f), cornerRadius = CornerRadius(6f))

                val scrW3 = 90f
                val scrH3 = 60f
                val scrL2 = pumpL2 + 10f
                val scrT2 = pumpTop2 + 12f

                drawRoundRect(color = Color(0xFF263238), topLeft = Offset(scrL2, scrT2), size = Size(scrW3, scrH3), cornerRadius = CornerRadius(8f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF1A2E35), Color(0xFF0D1F26)), startY = scrT2, endY = scrT2 + scrH3), topLeft = Offset(scrL2 + 4f, scrT2 + 4f), size = Size(scrW3 - 8f, scrH3 - 8f), cornerRadius = CornerRadius(6f))
                drawCircle(color = if (isActive) Color(0xFF00E676) else Color(0xFFFF1744), radius = 3f, center = Offset(scrL2 + 8f, scrT2 + 8f))

                drawContext.canvas.nativeCanvas.apply {
                    val rateText = if (isActive) String.format("%.0f", drug.calculatedVolume) else "---"
                    drawText(rateText, scrL2 + 15f, scrT2 + 38f, android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 26f; isFakeBoldText = true; typeface = android.graphics.Typeface.MONOSPACE; isAntiAlias = true })
                    drawText("mL/hr", scrL2 + 58f, scrT2 + 34f, android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#80CBC4"); textSize = 11f; isAntiAlias = true })

                    if (isActive) {
                        val runAlpha = ((sin(pumpArrowsPhase * PI) * 0.5 + 0.5) * 255).toInt()
                        drawText("RUNNING >>>", scrL2 + 12f, scrT2 + 52f, android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#69F0AE"); textSize = 9f; isAntiAlias = true; alpha = runAlpha })
                    } else {
                        drawText("STOPPED", scrL2 + 12f, scrT2 + 52f, android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 9f; isAntiAlias = true })
                    }
                }

                val bpT2 = scrT2 + scrH3 + 12f
                drawRoundRect(color = drug.gradientStart.copy(alpha = 0.85f), topLeft = Offset(scrL2, bpT2), size = Size(40f, 22f), cornerRadius = CornerRadius(4f))
                drawRoundRect(color = drug.gradientStart.copy(alpha = 0.85f), topLeft = Offset(scrL2 + 50f, bpT2), size = Size(40f, 22f), cornerRadius = CornerRadius(4f))

                drawRoundRect(color = Color(0xFF4CAF50), topLeft = Offset(scrL2, bpT2 + 32f), size = Size(40f, 22f), cornerRadius = CornerRadius(4f))
                drawRoundRect(color = Color(0xFFF44336), topLeft = Offset(scrL2 + 50f, bpT2 + 32f), size = Size(40f, 22f), cornerRadius = CornerRadius(4f))

                drawContext.canvas.nativeCanvas.apply {
                    val bp = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 16f; isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER }
                    drawText("+", scrL2 + 20f, bpT2 + 16f, bp)
                    drawText("−", scrL2 + 70f, bpT2 + 16f, bp)
                    drawText("▶", scrL2 + 20f, bpT2 + 48f, bp)
                }

                drawRoundRect(color = Color(0xFF546E7A), topLeft = Offset(pumpR2 - 14f, pumpTop2 + 10f), size = Size(10f, pumpH2 - 20f), cornerRadius = CornerRadius(5f))
                drawRoundRect(color = Color(0xFF37474F), topLeft = Offset(pumpR2 - 12f, pumpTop2 + 12f), size = Size(6f, pumpH2 - 24f), cornerRadius = CornerRadius(3f))
                drawRoundRect(Brush.horizontalGradient(listOf(Color(0x8090A4AE), Color(0xF0CFD8DC), Color(0x8078909C))), topLeft = Offset(pumpR2 - 12f, pumpTop2 + 12f), size = Size(5f, pumpH2 - 24f), cornerRadius = CornerRadius(2f))
            }
        }
    }
}

// --- ACCORDION DRUG CARD LAYOUT (Super Fast Animations) ---
@Composable
fun EmergencyAccordionCard(drug: EmergencyDrug, isActive: Boolean, isExpanded: Boolean, onCardClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_shadow")
    val shadowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(250, easing = FastOutSlowInEasing))
            .shadow(if (isExpanded && isActive) 24.dp else 12.dp, RoundedCornerShape(20.dp), spotColor = if (isExpanded && isActive) drug.gradientEnd.copy(alpha=shadowAlpha) else Color.Black.copy(alpha=0.1f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable { onCardClick() }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRoundRect(
                            color = drug.gradientEnd,
                            topLeft = Offset.Zero,
                            size = Size(16f, size.height),
                            cornerRadius = CornerRadius(20f, 0f)
                        )
                    }
                    .padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(drug.category.uppercase(), color = EmergencySlateLight, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(drug.name, color = EmergencySlateDark, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(drug.concentration, color = drug.gradientEnd, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = EmergencySlateLight,
                    modifier = Modifier.size(32.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(200))
            ) {
                Column(modifier = Modifier.background(EmergencyBgWhite)) {
                    HorizontalDivider(color = ClinicalBorder)

                    Column(modifier = Modifier.padding(16.dp)) {
                        CardDetailsContent(drug, isActive)

                        Spacer(modifier = Modifier.height(16.dp))

                        if (drug.deliveryType == DeliveryType.IV_INFUSION) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                                AnimatedProIVPump(drug, isActive)
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                AnimatedMassiveSyringe(drug, isActive)
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().background(ClinicalPanelBg).border(1.dp, ClinicalBorder).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertOrangeStart, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(drug.safetyTip, color = EmergencySlateDark.copy(alpha=0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(drug.clinicalPearl, color = drug.gradientEnd, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

// --- UNIFIED DASHBOARD GRID (Intrinsic Height Alignment) ---
@Composable
fun CardDetailsContent(drug: EmergencyDrug, isActive: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, ClinicalBorder, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardCell(
                title = "DOSE",
                value = if (isActive) drug.doseText else "0.0",
                color = EmergencySlateDark,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider()
            DashboardCell(
                title = if (drug.deliveryType == DeliveryType.SYRINGE_PUSH) "VOL" else "TOTAL",
                value = if (isActive) drug.volumeText else "0.0",
                color = drug.gradientEnd,
                isBold = true,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider()
            DashboardCell(
                title = "SPEED",
                value = drug.pushSpeed,
                color = EmergencySlateDark,
                modifier = Modifier.weight(1.3f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, ClinicalBorder, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DiluentBadge(modifier = Modifier.weight(1f).fillMaxHeight(), drug)
            VerticalDivider()
            AccessDashboardCell(
                drug = drug,
                modifier = Modifier.weight(1.2f).fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .drawBehind {
                    drawRoundRect(
                        color = drug.gradientEnd,
                        topLeft = Offset.Zero,
                        size = Size(12f, size.height),
                        cornerRadius = CornerRadius(8f, 0f)
                    )
                }
                .border(1.dp, ClinicalBorder, RoundedCornerShape(8.dp))
                .padding(start = 24.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text("PREPARATION", color = EmergencySlateLight, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(drug.preparation, color = EmergencySlateDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(DividerColor))
}

@Composable
fun DashboardCell(title: String, value: String, color: Color, isBold: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = EmergencySlateLight, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            color = color,
            fontSize = if (isBold) 18.sp else 16.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DiluentBadge(modifier: Modifier = Modifier, drug: EmergencyDrug) {
    Box(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DILUENT", color = EmergencySlateLight, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(drug.diluent.uppercase(), color = drug.gradientEnd, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AccessDashboardCell(drug: EmergencyDrug, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CANNULA", color = EmergencySlateLight, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(drug.cannulaColor, CircleShape).shadow(4.dp, CircleShape, spotColor = drug.cannulaColor))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    drug.cannulaGauge,
                    color = EmergencySlateDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmergencyAuroraBackground(isVisible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val phase1 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "")
    val alphaAnim by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, tween(2000), label = "")

    Canvas(modifier = Modifier.fillMaxSize().alpha(alphaAnim).blur(120.dp, BlurredEdgeTreatment.Unbounded)) {
        val w = size.width; val h = size.height
        drawCircle(brush = Brush.radialGradient(listOf(AlertRedStart.copy(alpha = 0.15f), Color.Transparent), radius = w * 0.9f), center = Offset(w * 0.2f + (sin(phase1) * w * 0.2f).toFloat(), h * 0.3f))
        drawCircle(brush = Brush.radialGradient(listOf(AlertCyanStart.copy(alpha = 0.15f), Color.Transparent), radius = w * 1.0f), center = Offset(w * 0.8f, h * 0.6f + (cos(phase1.toDouble()) * h * 0.2f).toFloat()))
    }
}