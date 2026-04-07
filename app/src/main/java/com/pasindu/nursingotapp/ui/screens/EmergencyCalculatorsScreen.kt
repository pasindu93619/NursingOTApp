package com.pasindu.nursingotapp.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin

// --- PREMIUM LIGHT THEME PALETTE ---
val EmergencyBgWhite = Color(0xFFF4F7FB)
val EmergencySlateDark = Color(0xFF0F172A)
val EmergencySlateLight = Color(0xFF64748B)

val AlertRedStart = Color(0xFFFF1744)
val AlertRedEnd = Color(0xFFD50000)
val AlertOrangeStart = Color(0xFFFF9100)
val AlertOrangeEnd = Color(0xFFE65100)
val AlertPurpleStart = Color(0xFFD500F9)
val AlertPurpleEnd = Color(0xFF6A1B9A)
val AlertCyanStart = Color(0xFF00E5FF)
val AlertCyanEnd = Color(0xFF00838F)
val AlertBlueStart = Color(0xFF2979FF)
val AlertBlueEnd = Color(0xFF1565C0)
val AlertGreenStart = Color(0xFF00E676)
val AlertGreenEnd = Color(0xFF1B5E20)

// Universal IV Cannula Color Codes
val CannulaOrange = Color(0xFFFF9800) // 14G
val CannulaGray = Color(0xFF9E9E9E)   // 16G
val CannulaGreen = Color(0xFF4CAF50)  // 18G
val CannulaPink = Color(0xFFE91E63)   // 20G
val CannulaBlue = Color(0xFF2196F3)   // 22G

enum class DeliveryType { SYRINGE_PUSH, IV_INFUSION }

// --- CLINICAL ECG RHYTHMS & DATA ---
enum class EcgRhythm(val displayName: String, val color: Color, val pattern: String, val causes: String, val treatment: String) {
    NSR("NORMAL SINUS RHYTHM", Color(0xFF2979FF), "Regular P-QRS-T complex. Rate 60-100 bpm.", "Normal physiological state, adequate perfusion.", "1. Continue Monitoring\n2. No immediate intervention required"),
    VFIB("VENTRICULAR FIBRILLATION", Color(0xFFD50000), "Chaotic, irregular waveform. No P/QRS/T waves. No cardiac output.", "Myocardial Infarction, Hypoxia, Severe Electrolyte Imbalance.", "1. Immediate Defibrillation\n2. CPR\n3. Epinephrine 1mg\n4. Amiodarone 300mg"),
    VTACH("PULSELESS V-TACH", Color(0xFFE65100), "Wide QRS (>120ms). Regular, extremely fast rhythm. No pulse.", "Ischemia, Structural heart disease, QT prolongation.", "1. Defibrillation\n2. CPR\n3. Epinephrine 1mg\n4. Amiodarone 300mg"),
    SVT("SUPRAVENTRICULAR TACHY", Color(0xFF6A1B9A), "Narrow QRS. Regular, fast (150-250 bpm). P waves often hidden.", "Re-entry pathways (AVNRT), Stress, Stimulants, Hypoxia.", "1. Vagal maneuvers\n2. Adenosine 6mg rapid IV push\n3. Synchronized cardioversion"),
    AFIB("ATRIAL FIBRILLATION", Color(0xFF00B0FF), "No P waves. Irregularly irregular rhythm. Variable rate.", "Hypertension, Coronary Artery Disease, Valve disease.", "1. Rate control (Metoprolol/Diltiazem)\n2. Anticoagulation"),
    TORSADES("TORSADES DE POINTES", Color(0xFFFF8F00), "Polymorphic VT. Twisting QRS complexes around baseline. Prolonged QT.", "Hypomagnesemia, Hypokalemia, QT-prolonging drugs.", "1. Magnesium sulfate 2g IV over 10-15 mins\n2. Defibrillation if pulseless"),
    HEART_BLOCK("COMPLETE HEART BLOCK", Color(0xFF00C853), "P waves and QRS completely unrelated. Severe bradycardia.", "MI, Drug toxicity (Beta-blockers/CCBs), Aging conduction system.", "1. Atropine 0.5mg IV\n2. Temporary Pacing\n3. Dopamine/Epi infusion"),
    ASYSTOLE("ASYSTOLE", Color(0xFF616161), "Flat line. No electrical activity.", "The H's and T's (Hypoxia, Hypovolemia, Tension Pneumo, etc.)", "1. High-quality CPR\n2. Epinephrine 1mg IV\n3. DO NOT DEFIBRILLATE")
}

data class MoAStep(val title: String, val description: String)

data class EmergencyDrug(
    val category: String, val name: String, val concentration: String, val doseText: String, val volumeText: String,
    val diluent: String, val pushSpeed: String, val cannulaGauge: String, val cannulaColor: Color, val preparation: String,
    val safetyTip: String, val clinicalPearl: String, val mechanism: String, val halfLife: String,
    val gradientStart: Color, val gradientEnd: Color, val deliveryType: DeliveryType, val maxContainerVolume: Float, val calculatedVolume: Float,
    val moaSteps: List<MoAStep>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCalculatorsScreen(onNavigateBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var weightInput by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }

    var selectedDrugIndex by remember { mutableStateOf<Int?>(null) }
    var zoomedEcg by remember { mutableStateOf<EcgRhythm?>(null) }

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
                    "FLUSH 20mL NS", "RAPID PUSH", "18G (Green) / IO", CannulaGreen,
                    "Use 1:10,000 pre-filled syringe. Round calculated volumes to nearest 0.1 mL.",
                    "Flush line rapidly with 20mL NS to force into central circulation.",
                    "Epi has a half-life of 2-3 mins. Rapid flush is required to trigger alpha-1 vasoconstriction.",
                    "Potent α/β adrenergic agonist", "2-3 minutes",
                    AlertRedStart, AlertRedEnd, DeliveryType.SYRINGE_PUSH, 10f, vol,
                    listOf(
                        MoAStep("Receptor Binding", "Binds powerfully as a nonselective α and β adrenergic agonist."),
                        MoAStep("Vascular Constriction", "α1 activation causes intense peripheral vasoconstriction, shunting blood to the core."),
                        MoAStep("Cardiac Stimulation", "β1 activation drastically increases heart rate and myocardial contractility."),
                        MoAStep("Systemic Perfusion", "Restores critical coronary and cerebral blood flow during CPR.")
                    )
                )
            },
            run {
                val dose = if (weight == 0f) 0f else min(0.5f, weight * 0.01f)
                val vol = dose / 1.0f
                EmergencyDrug(
                    "ANAPHYLAXIS", "Epinephrine (1:1,000)", "Ampoule: 1.0 mg/mL",
                    "${String.format("%.2f", dose)} mg", "${String.format("%.2f", vol)} mL",
                    "DO NOT DILUTE", "IM INJECTION", "21G-23G IM", CannulaBlue,
                    "Use a strict 1 mL syringe for exact precision. Give IM in vastus lateralis.",
                    "WARNING: 10x stronger! DO NOT GIVE IV PUSH.",
                    "IV push in a beating heart can cause lethal arrhythmias. Vastus Lateralis allows massive absorption.",
                    "Reverses vasodilation & bronchoconstriction", "2-3 minutes",
                    AlertOrangeStart, AlertOrangeEnd, DeliveryType.SYRINGE_PUSH, 1f, vol,
                    listOf(
                        MoAStep("Receptor Binding", "Triggers systemic α/β adrenergic activation via muscular absorption."),
                        MoAStep("Bronchodilation", "β2 agonism immediately relaxes bronchial smooth muscle, opening airways."),
                        MoAStep("Edema Reduction", "α1 vasoconstriction decreases mucosal edema and airway swelling."),
                        MoAStep("Mast Cell Stabilization", "Inhibits further release of histamine and inflammatory mediators.")
                    )
                )
            },
            run {
                val dose = if (weight == 0f) 0f else if (weight > 40f) 300f else min(300f, weight * 5f)
                val vol = dose / 50f
                EmergencyDrug(
                    "PULSELESS VT/VF", "Amiodarone", "Ampoule: 50 mg/mL",
                    "${String.format("%.0f", dose)} mg", "${String.format("%.1f", vol)} mL",
                    "LIVE: D5W ONLY", "RAPID PUSH", "18G (Green)", CannulaGreen,
                    "Draw with large bore needle (foams easily due to polysorbate 80).",
                    "Give undiluted ONLY for arrest. Do not shake.",
                    "If giving as a live infusion, it MUST be diluted in D5W, never Saline.",
                    "Class III antiarrhythmic (K+ channel block)", "Up to 58 days",
                    AlertPurpleStart, AlertRedEnd, DeliveryType.SYRINGE_PUSH, 10f, vol,
                    listOf(
                        MoAStep("Channel Blockade", "Primarily blocks myocardial Potassium (K+) channels across the heart."),
                        MoAStep("Repolarization Delay", "Significantly prolongs Phase 3 of the cardiac action potential."),
                        MoAStep("Refractory Extension", "Increases the effective refractory period and prolongs the QT interval."),
                        MoAStep("Rhythm Stabilization", "Suppresses chaotic ectopic foci, halting ventricular arrhythmias.")
                    )
                )
            },
            run {
                val dose = if (weight == 0f) 0f else weight * 1f
                val vol = dose / 1f
                EmergencyDrug(
                    "METABOLIC ACIDOSIS", "Sodium Bicarb 8.4%", "Ampoule: 1 mEq/mL",
                    "${String.format("%.0f", dose)} mEq", "${String.format("%.1f", vol)} mL",
                    "UNDILUTED", "SLOW PUSH", "18G (Green)", CannulaGreen,
                    "Give 1 mEq/kg slow IV push. Typically supplied in 50mL pre-filled syringes.",
                    "Flush line thoroughly before/after. Precipitates with Calcium.",
                    "Used for severe acidosis (pH < 7.1), TCA overdose, or hyperkalemia.",
                    "Systemic alkalinizing agent", "Requires ventilation to clear",
                    AlertBlueStart, AlertCyanEnd, DeliveryType.SYRINGE_PUSH, 60f, vol,
                    listOf(
                        MoAStep("Ionic Dissociation", "Rapidly dissociates into Na+ and HCO3- directly in the plasma."),
                        MoAStep("Proton Binding", "HCO3- acts as a sponge, actively binding to excess circulating H+ ions."),
                        MoAStep("Carbonic Acid Conversion", "Forms H2CO3, which breaks down into H2O and CO2."),
                        MoAStep("pH Normalization", "Raises systemic pH, reversing severe cellular metabolic acidosis.")
                    )
                )
            },
            run {
                val dose = if (weight == 0f) 0f else min(2f, 0.4f)
                val vol = dose / 0.4f
                EmergencyDrug(
                    "OPIOID OVERDOSE", "Naloxone (Narcan)", "Ampoule: 0.4 mg/mL",
                    "${String.format("%.1f", dose)} mg", "${String.format("%.1f", vol)} mL",
                    "UNDILUTED", "SLOW PUSH", "20G (Pink)", CannulaPink,
                    "Titrate to respiratory rate, not full consciousness.",
                    "Rapid push may cause acute withdrawal, vomiting, and flash pulmonary edema.",
                    "Half-life is shorter than opioids. May require repeat doses or continuous infusion.",
                    "Competitive µ-opioid antagonist", "30-81 mins (Watch for relapse)",
                    AlertPurpleStart, AlertBlueEnd, DeliveryType.SYRINGE_PUSH, 3f, vol,
                    listOf(
                        MoAStep("BBB Crossing", "Rapidly crosses the blood-brain barrier into the Central Nervous System."),
                        MoAStep("Receptor Competition", "Fiercely competes for µ (mu), κ (kappa), and σ (sigma) opioid receptors."),
                        MoAStep("Opioid Displacement", "Due to higher affinity, it physically displaces bound synthetic and natural opioids."),
                        MoAStep("Instant Reversal", "Instantly reverses respiratory depression, sedation, and hypotension.")
                    )
                )
            },
            run {
                val totalDose = if (weight == 0f) 0f else min(90f, weight * 0.9f)
                val bolusDose = totalDose * 0.1f
                EmergencyDrug(
                    "ISCHEMIC STROKE", "Alteplase (tPA)", "Reconstituted: 1 mg/mL",
                    "Bolus: ${String.format("%.1f", bolusDose)} mg\nInfuse: ${String.format("%.1f", totalDose * 0.9f)} mg",
                    "${String.format("%.1f", totalDose)} mL",
                    "STERILE WATER", "1 HR INFUSION", "18G (Green) x2", CannulaGreen,
                    "Give 10% as a rapid bolus, and infuse the remainder via IV pump over 60 minutes.",
                    "DO NOT shake to avoid degrading fragile protein chains.",
                    "Strict BP control < 185/110 required before administration. Fibrinolytic agent.",
                    "Tissue plasminogen activator (Fibrinolysis)", "5 minutes",
                    AlertPurpleStart, AlertPurpleEnd, DeliveryType.IV_INFUSION, 100f, totalDose,
                    listOf(
                        MoAStep("Fibrin Binding", "Directly binds to fibrin strands embedded within an occlusive thrombus."),
                        MoAStep("Enzymatic Activation", "Converts trapped, inactive plasminogen into active plasmin."),
                        MoAStep("Proteolytic Cleavage", "Plasmin aggressively degrades the structural fibrin matrix of the clot."),
                        MoAStep("Clot Lysis", "Dissolves the physical clot, immediately restoring distal blood flow.")
                    )
                )
            },
            run {
                val vol = if (weight == 0f) 0f else (weight * 20f)
                EmergencyDrug(
                    "HYPOVOLEMIA", "Fluid Resuscitation", "Normal Saline 0.9%",
                    "${(weight * 20).toInt()}-${(weight * 30).toInt()} mL", "${vol.toInt()} mL",
                    "CRYSTALLOID", "WIDE OPEN", "14G (Orange)", CannulaOrange,
                    "Use pressure bag for rapid infusion. Assess lungs every 500mL.",
                    "If a pump is unavailable, strictly use microdrip sets (60 gtt/mL).",
                    "With a 60 gtt/mL microdrip set, 1 drop/min exactly equals 1 mL/hr. Poiseuille's Law: doubling diameter increases flow 16x!",
                    "Isotonic intravascular volume expansion", "Redistributes in 20-30 mins",
                    AlertBlueStart, AlertBlueEnd, DeliveryType.IV_INFUSION, 1000f, vol,
                    listOf(
                        MoAStep("Intravascular Expansion", "Isotonic crystalloid directly fills the depleted vascular space."),
                        MoAStep("Venous Return", "Increases the volume of preload returning to the right atrium."),
                        MoAStep("Starling's Mechanism", "Myocardial stretch increases overall ventricular stroke volume."),
                        MoAStep("Hemodynamic Boost", "Elevates overall cardiac output and stabilizes systemic blood pressure.")
                    )
                )
            },
            run {
                val dose = if (weight == 0f) 0f else weight * 2f
                val vol = dose / 50f
                EmergencyDrug(
                    "RSI: INDUCTION", "Ketamine", "Vial: 50 mg/mL",
                    "${String.format("%.0f", dose)} mg", "${String.format("%.1f", vol)} mL",
                    "NS / D5W", "SLOW (60s)", "20G (Pink)", CannulaPink,
                    "Dilute in NS or D5W if needed.",
                    "Push slowly over 60s to prevent emergence delirium.",
                    "Excellent for asthmatics (bronchodilation) and shock (catecholamine release).",
                    "NMDA receptor antagonist (Dissociative)", "2.5 hours",
                    AlertCyanStart, AlertCyanEnd, DeliveryType.SYRINGE_PUSH, 10f, vol,
                    listOf(
                        MoAStep("CNS Penetration", "Highly lipophilic, it rapidly crosses into the central nervous system."),
                        MoAStep("NMDA Antagonism", "Non-competitively blocks NMDA receptors in the cerebral cortex."),
                        MoAStep("Glutamate Inhibition", "Prevents critical excitatory neurotransmission pathways."),
                        MoAStep("Dissociation", "Induces profound analgesia, amnesia, and sensory detachment.")
                    )
                )
            }
        )
    }

    BackHandler(enabled = selectedDrugIndex != null) {
        selectedDrugIndex = null
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AnimatedVisibility(visible = selectedDrugIndex == null, enter = fadeIn(), exit = fadeOut()) {
                TopAppBar(
                    title = { Text("Emergency & Resuscitation", fontWeight = FontWeight.Black, color = EmergencySlateDark, fontSize = 20.sp) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(EmergencyBgWhite)) {

            SmoothMeshBackground(isVisible)

            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

                EcgTelemetryDeck(
                    isVisible = isVisible,
                    weight = weightInput,
                    onWeightChange = { newWeight -> weightInput = newWeight },
                    onEcgLongPress = { rhythm ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        zoomedEcg = rhythm
                    }
                )

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(600)) + fadeIn()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(drugs) { index, drug ->
                            EmergencyProtocolTriggerCard(
                                drug = drug,
                                isActive = weight > 0f,
                                onCardClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedDrugIndex = index
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedDrugIndex != null,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)) + fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)) + fadeOut(tween(300)),
                modifier = Modifier.zIndex(50f)
            ) {
                selectedDrugIndex?.let { index ->
                    val drug = drugs.getOrNull(index)
                    if (drug != null) {
                        DrugDetailFullScreenOverlay(
                            drug = drug,
                            isActive = weight > 0f,
                            weightInput = weightInput,
                            onWeightChange = { newWeight -> weightInput = newWeight },
                            onClose = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDrugIndex = null
                            }
                        )
                    }
                }
            }

            if (zoomedEcg != null) {
                EcgZoomedOverlay(rhythm = zoomedEcg!!) { zoomedEcg = null }
            }
        }
    }
}

// ─── PROTOCOL TRIGGER CARD ───
@Composable
fun EmergencyProtocolTriggerCard(drug: EmergencyDrug, isActive: Boolean, onCardClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = drug.gradientEnd.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onCardClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color.White, drug.gradientStart.copy(alpha = 0.08f))))
                .drawBehind { drawRoundRect(drug.gradientEnd, Offset.Zero, Size(12f, size.height), CornerRadius(20f, 0f)) }
                .padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(drug.category.uppercase(), color = EmergencySlateLight, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(drug.name, color = EmergencySlateDark, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(if (isActive) drug.gradientEnd else Color(0xFFCBD5E1), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isActive) drug.doseText else "Enter Weight",
                        color = if (isActive) drug.gradientEnd else EmergencySlateLight,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(drug.gradientStart.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, drug.gradientEnd.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open Protocol", tint = drug.gradientEnd, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── IMMERSIVE FULL SCREEN DRUG PROTOCOL OVERLAY ───
@Composable
fun DrugDetailFullScreenOverlay(
    drug: EmergencyDrug,
    isActive: Boolean,
    weightInput: String,
    onWeightChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    // ── MoA overlay toggle ──────────────────────────────────────────────────
    // MoAPathwayOverlay lives in MoAPathwayComponents.kt (same package)
    var showMoAPathway by remember { mutableStateOf(false) }

    val infiniteTransitionAura = rememberInfiniteTransition(label = "expanded_aura")
    val phase by infiniteTransitionAura.animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB).copy(alpha = 0.98f))
            .drawBehind {
                val w = size.width; val h = size.height
                if (w <= 0f || h <= 0f) return@drawBehind
                val gridColor = drug.gradientStart.copy(alpha = 0.05f)
                for (x in 0..(w / 40f).toInt()) drawLine(gridColor, Offset(x * 40f, 0f), Offset(x * 40f, h), 2f)
                for (y in 0..(h / 40f).toInt()) drawLine(gridColor, Offset(0f, y * 40f), Offset(w, y * 40f), 2f)
                val cx1 = w * 0.2f + sin(phase) * w * 0.2f
                val cy1 = h * 0.2f + cos(phase) * h * 0.1f
                val cx2 = w * 0.8f + cos(phase + PI.toFloat()) * w * 0.2f
                val cy2 = h * 0.8f + sin(phase + PI.toFloat()) * h * 0.1f
                val safeRadius = maxOf(1f, w * 0.7f)
                drawCircle(brush = Brush.radialGradient(listOf(drug.gradientStart.copy(alpha = 0.15f), Color.Transparent), center = Offset(cx1, cy1), radius = safeRadius), center = Offset(cx1, cy1), radius = safeRadius)
                drawCircle(brush = Brush.radialGradient(listOf(drug.gradientEnd.copy(alpha = 0.15f), Color.Transparent), center = Offset(cx2, cy2), radius = safeRadius), center = Offset(cx2, cy2), radius = safeRadius)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 60.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 20.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(drug.category.uppercase(), color = drug.gradientEnd, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(drug.name, color = EmergencySlateDark, fontSize = 32.sp, fontWeight = FontWeight.Black, lineHeight = 36.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(drug.concentration, color = EmergencySlateLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose, modifier = Modifier.background(Color.White, CircleShape).shadow(4.dp, CircleShape)) {
                    Icon(Icons.Default.Close, "Close", tint = EmergencySlateDark)
                }
            }

            // --- IN-OVERLAY WEIGHT INPUT ---
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = drug.gradientStart.copy(alpha = 0.3f))
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, drug.gradientStart.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = weightInput, onValueChange = onWeightChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, color = EmergencySlateDark, textAlign = TextAlign.End),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (weightInput.isEmpty()) "WT" else "KG", color = EmergencySlateLight, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.width(60.dp)) {
                                    if (weightInput.isEmpty()) Text("0.0", color = Color(0xFFCBD5E1), fontSize = 24.sp, fontWeight = FontWeight.Black)
                                    innerTextField()
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- PHYSICS ENGINE BOX ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = drug.gradientEnd.copy(alpha = 0.4f))
                    .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                    .border(2.dp, drug.gradientEnd.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                if (drug.deliveryType == DeliveryType.IV_INFUSION) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) { AnimatedProIVPump(drug, isActive) }
                } else {
                    AnimatedMassiveSyringe(drug, isActive)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- DATA GRIDS ---
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                CardDetailsContent(drug, isActive)

                Spacer(modifier = Modifier.height(24.dp))

                // --- BIO-PULSE EDUCATIONAL HUD ---
                Text("PHARMACODYNAMICS", color = EmergencySlateLight, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                AnimatedBioPulseHUD(
                    drug = drug,
                    onLongPressMechanism = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMoAPathway = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- WARNING PEARL ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = AlertOrangeEnd.copy(alpha = 0.3f))
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, AlertOrangeEnd.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, null, tint = AlertOrangeEnd, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(drug.safetyTip, color = EmergencySlateDark, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = AlertOrangeEnd.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(drug.clinicalPearl, color = EmergencySlateLight, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                }
            }
        }

// ── MoA PATHWAY OVERLAY ─────────────────────────────────────────────
        // Composable is defined in Moapathwaycomponents.kt (same package)
        if (showMoAPathway) {
            MoAPathwayOverlay(
                drug = drug,
                onClose = { showMoAPathway = false }
            )
        }
    }
}

// ─── BIOLUMINESCENT PHARMACODYNAMICS HUD ───
@Composable
fun AnimatedBioPulseHUD(drug: EmergencyDrug, onLongPressMechanism: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "bio_pulse")
    val pulse by infiniteTransition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "")

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = drug.gradientStart.copy(alpha = 0.3f))
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, drug.gradientStart.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPressMechanism() }) }
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(drug.gradientStart.copy(alpha = 0.2f * pulse), radius = size.width / 2f * pulse)
                        drawCircle(drug.gradientStart.copy(alpha = 0.5f), radius = size.width / 4f)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("MECHANISM", color = EmergencySlateLight, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(drug.mechanism, color = drug.gradientStart, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Hold to view pathway", color = drug.gradientStart.copy(alpha = pulse), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = drug.gradientEnd.copy(alpha = 0.3f))
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, drug.gradientEnd.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawArc(color = drug.gradientEnd.copy(alpha = 0.2f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(4f))
                        drawArc(color = drug.gradientEnd, startAngle = -90f, sweepAngle = 360f * pulse, useCenter = false, style = Stroke(6f, cap = StrokeCap.Round))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("HALF-LIFE", color = EmergencySlateLight, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(drug.halfLife, color = drug.gradientEnd, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
        }
    }
}

// ─── SMOOTH LIQUID MESH BACKGROUND ───
@Composable
fun SmoothMeshBackground(isVisible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val phase1 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(25000, easing = LinearEasing)), label = "")
    val phase2 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "")
    val alphaAnim by animateFloatAsState(if (isVisible) 1f else 0f, tween(2000), label = "")

    Canvas(modifier = Modifier.fillMaxSize().alpha(alphaAnim)) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        drawRect(Color(0xFFF4F7FB))
        val r1 = maxOf(1f, w * 0.8f)
        val r2 = maxOf(1f, w * 0.9f)
        val r3 = maxOf(1f, w * 0.7f)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE3F2FD).copy(alpha = 0.8f), Color.Transparent), center = Offset(w * 0.3f + (sin(phase1) * w * 0.3f).toFloat(), h * 0.2f + (cos(phase2) * h * 0.1f).toFloat()), radius = r1), center = Offset(w * 0.3f + (sin(phase1) * w * 0.3f).toFloat(), h * 0.2f + (cos(phase2) * h * 0.1f).toFloat()), radius = r1)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE0F7FA).copy(alpha = 0.6f), Color.Transparent), center = Offset(w * 0.7f + (cos(phase1) * w * 0.2f).toFloat(), h * 0.6f + (sin(phase2) * h * 0.2f).toFloat()), radius = r2), center = Offset(w * 0.7f + (cos(phase1) * w * 0.2f).toFloat(), h * 0.6f + (sin(phase2) * h * 0.2f).toFloat()), radius = r2)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF3E5F5).copy(alpha = 0.5f), Color.Transparent), center = Offset(w * 0.5f + (sin(phase2) * w * 0.4f).toFloat(), h * 0.8f), radius = r3), center = Offset(w * 0.5f + (sin(phase2) * w * 0.4f).toFloat(), h * 0.8f), radius = r3)
    }
}

// ─── ECG MATH ENGINE ───
fun getEcgY(t: Float, rhythm: EcgRhythm, cy: Float, ampH: Float): Float {
    var y = cy
    when (rhythm) {
        EcgRhythm.NSR -> {
            val localT = t % 800f
            if (localT in 100f..180f) y -= sin((localT - 100f) / 80f * PI).toFloat() * ampH * 0.15f
            if (localT in 220f..280f) {
                if (localT < 235f) y += ampH * 0.1f
                else if (localT < 245f) y -= ampH * 0.85f
                else if (localT < 265f) y += ampH * 0.25f
            }
            if (localT in 400f..560f) y -= sin((localT - 400f) / 160f * PI).toFloat() * ampH * 0.25f
        }
        EcgRhythm.VFIB -> y -= (sin(t * 0.015f) * ampH * 0.25f + cos(t * 0.04f) * ampH * 0.15f + sin(t * 0.008f) * ampH * 0.2f).toFloat()
        EcgRhythm.VTACH -> y -= sin(((t % 350f) / 350f) * 2 * PI).toFloat() * ampH * 0.5f
        EcgRhythm.SVT -> {
            val localT = t % 200f
            if (localT in 80f..120f) {
                if (localT < 90f) y += ampH * 0.15f
                else if (localT < 100f) y -= ampH * 0.8f
                else if (localT < 110f) y += ampH * 0.25f
            }
            if (localT in 140f..180f) y -= sin((localT - 140f) / 40f * PI).toFloat() * ampH * 0.15f
        }
        EcgRhythm.AFIB -> {
            val localT = t % 750f
            y -= (sin(t * 0.03f) * ampH * 0.08f + cos(t * 0.06f) * ampH * 0.05f).toFloat()
            if (localT in 200f..280f) {
                if (localT < 220f) y += ampH * 0.1f
                else if (localT < 240f) y -= ampH * 0.8f
                else if (localT < 260f) y += ampH * 0.2f
            }
            if (localT in 320f..450f) y -= sin((localT - 320f) / 130f * PI).toFloat() * ampH * 0.2f
        }
        EcgRhythm.TORSADES -> {
            val spindle = sin(t * 0.001f) * ampH * 0.6f
            y -= (sin((t % 300f) / 300f * 2 * PI) * spindle).toFloat()
        }
        EcgRhythm.HEART_BLOCK -> {
            if (t % 500f in 100f..180f) y -= sin(((t % 500f) - 100f) / 80f * PI).toFloat() * ampH * 0.15f
            val qrsT = t % 1500f
            if (qrsT in 600f..680f) {
                if (qrsT < 620f) y += ampH * 0.1f
                else if (qrsT < 640f) y -= ampH * 0.7f
                else if (qrsT < 660f) y += ampH * 0.2f
            }
            if (qrsT in 800f..1000f) y -= sin((qrsT - 800f) / 200f * PI).toFloat() * ampH * 0.3f
        }
        EcgRhythm.ASYSTOLE -> y -= (sin(t * 0.005f) * ampH * 0.03f).toFloat()
    }
    return y
}

// ─── BRIGHT HORIZONTAL ECG DECK ───
@Composable
fun EcgTelemetryDeck(isVisible: Boolean, weight: String, onWeightChange: (String) -> Unit, onEcgLongPress: (EcgRhythm) -> Unit) {
    val entranceOffset by animateFloatAsState(targetValue = if (isVisible) 0f else -100f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f), label = "")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = entranceOffset.dp)
            .padding(bottom = 8.dp)
            .zIndex(10f)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF00E5FF).copy(alpha = 0.3f))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = weight, onValueChange = onWeightChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, color = EmergencySlateDark, textAlign = TextAlign.End),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (weight.isEmpty()) "WT" else "KG", color = EmergencySlateLight, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                            Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.width(60.dp)) {
                                if (weight.isEmpty()) Text("0.0", color = Color(0xFFCBD5E1), fontSize = 24.sp, fontWeight = FontWeight.Black)
                                innerTextField()
                            }
                        }
                    }
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(EcgRhythm.values()) { rhythm ->
                EcgMiniCardLight(rhythm = rhythm, onLongPress = { onEcgLongPress(rhythm) })
            }
        }

        Text("Long-press rhythm to analyze & zoom", color = EmergencySlateLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 24.dp, top = 12.dp))
    }
}

@Composable
fun EcgMiniCardLight(rhythm: EcgRhythm, onLongPress: () -> Unit) {
    var ecgPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            ecgPhase = 0f
            animate(0f, 1f, animationSpec = tween(3000, easing = LinearEasing)) { value, _ -> ecgPhase = value }
        }
    }

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(100.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = rhythm.color.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, rhythm.color.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
            val w = size.width; val h = size.height; val cy = h / 2f
            if (w <= 0f || h <= 0f) return@Canvas
            val gridColor = Color(0xFFF1F5F9)
            for (x in 0..(w / 15f).toInt()) drawLine(gridColor, Offset(x * 15f, 0f), Offset(x * 15f, h))
            for (y in 0..(h / 15f).toInt()) drawLine(gridColor, Offset(0f, y * 15f), Offset(w, y * 15f))
            val ecgPath = Path().apply {
                val startY = getEcgY(0f, rhythm, cy, 25f)
                moveTo(0f, startY)
                val steps = 200
                for (i in 1..steps) {
                    val progress = i.toFloat() / steps
                    lineTo(progress * w, getEcgY(progress * 4000f, rhythm, cy, 25f))
                }
            }
            val headX = ecgPhase * w
            clipRect(left = 0f, right = headX, top = 0f, bottom = h) {
                drawPath(ecgPath, color = rhythm.color.copy(alpha = 0.25f), style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(ecgPath, color = rhythm.color, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        val charCount = rhythm.displayName.length
        val dynamicFontSize = when {
            charCount <= 12 -> 12.sp
            charCount <= 16 -> 10.sp
            charCount <= 20 -> 9.sp
            else -> 8.sp
        }
        Text(
            text = rhythm.displayName,
            color = EmergencySlateDark,
            fontSize = dynamicFontSize,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

// ─── MASSIVE DARK-MODE ECG ZOOM OVERLAY WITH PINCH/DOUBLE-TAP TO ZOOM ───
@Composable
fun EcgZoomedOverlay(rhythm: EcgRhythm, onClose: () -> Unit) {
    var ecgPhase by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val isZoomed = scale > 1.1f

    LaunchedEffect(Unit) {
        while (true) {
            ecgPhase = 0f
            animate(0f, 1f, animationSpec = tween(5000, easing = LinearEasing)) { value, _ -> ecgPhase = value }
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.85f))
                .padding(16.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = rhythm.color.copy(alpha = 0.6f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(1.5.dp, rhythm.color.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(rhythm.displayName, color = EmergencySlateDark, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose, modifier = Modifier.background(Color(0xFFF1F5F9), CircleShape)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = EmergencySlateDark)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color(0xFF0B1120))
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.1f) { scale = 1f; offset = Offset.Zero }
                                    else { scale = 2.5f; offset = Offset.Zero }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                val maxPan = (scale - 1) * 400f
                                val newX = (offset.x + pan.x * scale).coerceIn(-maxPan, maxPan)
                                val newY = (offset.y + pan.y * scale).coerceIn(-maxPan, maxPan)
                                offset = Offset(newX, newY)
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale; scaleY = scale
                                translationX = offset.x; translationY = offset.y
                            }
                    ) {
                        val w = size.width; val h = size.height; val cy = h / 2f
                        if (w <= 0f || h <= 0f) return@Canvas

                        if (isZoomed) {
                            val largeSq = w / 30f
                            val smallSq = largeSq / 5f
                            val gridRedMajor = Color(0xFFEF5350).copy(alpha = 0.7f)
                            val gridRedMinor = Color(0xFFEF5350).copy(alpha = 0.4f)
                            val majorStroke = 2.5f / scale
                            val minorStroke = 1.2f / scale
                            for (i in -150..300) {
                                val px = i * smallSq
                                if (i % 5 == 0) drawLine(gridRedMajor, Offset(px, -h), Offset(px, h * 2), strokeWidth = majorStroke)
                                else drawLine(gridRedMinor, Offset(px, -h), Offset(px, h * 2), strokeWidth = minorStroke)
                            }
                            for (i in -100..100) {
                                val py = cy + (i * smallSq)
                                if (i % 5 == 0) drawLine(gridRedMajor, Offset(-w, py), Offset(w * 2, py), strokeWidth = majorStroke)
                                else drawLine(gridRedMinor, Offset(-w, py), Offset(w * 2, py), strokeWidth = minorStroke)
                            }
                        } else {
                            val gridColor = Color.White.copy(alpha = 0.05f)
                            for (x in -50..(w / 20f).toInt() + 50) drawLine(gridColor, Offset(x * 20f, -h), Offset(x * 20f, h * 2))
                            for (y in -50..(h / 20f).toInt() + 50) drawLine(gridColor, Offset(-w, y * 20f), Offset(w * 2, y * 20f))
                        }

                        val ecgPath = Path().apply {
                            val startY = getEcgY(0f, rhythm, cy, 60f)
                            moveTo(0f, startY)
                            val steps = 500
                            for (i in 1..steps) {
                                val progress = i.toFloat() / steps
                                lineTo(progress * w, getEcgY(progress * 6000f, rhythm, cy, 60f))
                            }
                        }
                        val currentHeadX = if (isZoomed) Float.POSITIVE_INFINITY else ecgPhase * w

                        clipRect(left = Float.NEGATIVE_INFINITY, right = currentHeadX, top = -h, bottom = h * 2) {
                            drawPath(ecgPath, color = rhythm.color.copy(alpha = 0.4f), style = Stroke(width = 8f / scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawPath(ecgPath, color = rhythm.color, style = Stroke(width = 3.5f / scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

                            if (scale > 1.4f) {
                                val labelAlpha = ((scale - 1.4f) * 2f).coerceIn(0f, 1f)
                                val textPaint = Paint().apply {
                                    color = android.graphics.Color.WHITE; textSize = 34f / scale; isFakeBoldText = true
                                    textAlign = Paint.Align.CENTER; alpha = (255 * labelAlpha).toInt()
                                }
                                val textPaintSmall = Paint().apply {
                                    color = android.graphics.Color.parseColor("#00E676"); textSize = 24f / scale
                                    textAlign = Paint.Align.CENTER; alpha = (255 * labelAlpha).toInt()
                                }

                                if (rhythm == EcgRhythm.NSR) {
                                    val points = listOf("P" to 140f, "Q" to 230f, "R" to 240f, "S" to 255f, "T" to 480f)
                                    for (p in 0..10) {
                                        val baseT = p * 800f
                                        val pxP = ((baseT + 100f) / 6000f) * w
                                        val pxQ = ((baseT + 220f) / 6000f) * w
                                        val pxS = ((baseT + 265f) / 6000f) * w
                                        points.forEach { (label, offsetT) ->
                                            val tMs = baseT + offsetT
                                            if (tMs <= 6000f) {
                                                val px = (tMs / 6000f) * w
                                                val py = getEcgY(tMs, rhythm, cy, 60f)
                                                val yOffset = if (label == "R") -40f / scale else 50f / scale
                                                drawContext.canvas.nativeCanvas.drawText(label, px, py + yOffset, textPaint)
                                            }
                                        }
                                        val caliperColor = Color(0xFF00E676).copy(alpha = labelAlpha)
                                        val caliperY = cy + (120f / scale)
                                        val caliperYTop = cy - (120f / scale)
                                        drawLine(caliperColor, Offset(pxP, caliperY - 10f / scale), Offset(pxP, caliperY + 10f / scale), 2f / scale)
                                        drawLine(caliperColor, Offset(pxQ, caliperY - 10f / scale), Offset(pxQ, caliperY + 10f / scale), 2f / scale)
                                        drawLine(caliperColor, Offset(pxP, caliperY), Offset(pxQ, caliperY), 2f / scale)
                                        drawContext.canvas.nativeCanvas.drawText("PR", (pxP + pxQ) / 2, caliperY + 30f / scale, textPaintSmall)
                                        drawLine(caliperColor, Offset(pxQ, caliperYTop - 10f / scale), Offset(pxQ, caliperYTop + 10f / scale), 2f / scale)
                                        drawLine(caliperColor, Offset(pxS, caliperYTop - 10f / scale), Offset(pxS, caliperYTop + 10f / scale), 2f / scale)
                                        drawLine(caliperColor, Offset(pxQ, caliperYTop), Offset(pxS, caliperYTop), 2f / scale)
                                        drawContext.canvas.nativeCanvas.drawText("QRS", (pxQ + pxS) / 2, caliperYTop - 15f / scale, textPaintSmall)
                                    }
                                } else if (rhythm == EcgRhythm.SVT) {
                                    val points = listOf("Q" to 85f, "R" to 95f, "S" to 105f, "T" to 160f)
                                    for (p in 0..30) {
                                        val baseT = p * 200f
                                        points.forEach { (label, offsetT) ->
                                            val tMs = baseT + offsetT
                                            if (tMs <= 6000f) {
                                                val px = (tMs / 6000f) * w
                                                val py = getEcgY(tMs, rhythm, cy, 60f)
                                                val yOffset = if (label == "R") -40f / scale else 50f / scale
                                                drawContext.canvas.nativeCanvas.drawText(label, px, py + yOffset, textPaint)
                                            }
                                        }
                                    }
                                } else if (rhythm == EcgRhythm.HEART_BLOCK) {
                                    for (p in 0..20) {
                                        val tMs = p * 500f + 140f
                                        if (tMs <= 6000f) {
                                            val px = (tMs / 6000f) * w
                                            val py = getEcgY(tMs, rhythm, cy, 60f)
                                            drawContext.canvas.nativeCanvas.drawText("P", px, py + 50f / scale, textPaint)
                                        }
                                    }
                                    val qrsPoints = listOf("Q" to 610f, "R" to 630f, "S" to 650f, "T" to 900f)
                                    for (p in 0..10) {
                                        val baseT = p * 1500f
                                        qrsPoints.forEach { (label, offsetT) ->
                                            val tMs = baseT + offsetT
                                            if (tMs <= 6000f) {
                                                val px = (tMs / 6000f) * w
                                                val py = getEcgY(tMs, rhythm, cy, 60f)
                                                val yOffset = if (label == "R") -40f / scale else 50f / scale
                                                drawContext.canvas.nativeCanvas.drawText(label, px, py + yOffset, textPaint)
                                            }
                                        }
                                    }
                                } else if (rhythm == EcgRhythm.AFIB) {
                                    val qrsPoints = listOf("Q" to 230f, "R" to 240f, "S" to 250f, "T" to 385f)
                                    for (p in 0..10) {
                                        val baseT = p * 750f
                                        qrsPoints.forEach { (label, offsetT) ->
                                            val tMs = baseT + offsetT
                                            if (tMs <= 6000f) {
                                                val px = (tMs / 6000f) * w
                                                val py = getEcgY(tMs, rhythm, cy, 60f)
                                                val yOffset = if (label == "R") -40f / scale else 50f / scale
                                                drawContext.canvas.nativeCanvas.drawText(label, px, py + yOffset, textPaint)
                                            }
                                        }
                                    }
                                } else if (rhythm == EcgRhythm.VTACH) {
                                    for (p in 0..20) {
                                        val tMs = p * 350f + 87.5f
                                        if (tMs <= 6000f) {
                                            val px = (tMs / 6000f) * w
                                            val py = getEcgY(tMs, rhythm, cy, 60f)
                                            drawContext.canvas.nativeCanvas.drawText("R", px, py - 40f / scale, textPaint)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isZoomed) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFEF5350).copy(alpha = 0.5f)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1mm = 0.04s", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.size(10.dp).border(1.5.dp, Color(0xFFEF5350).copy(alpha = 0.8f)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("5mm = 0.20s", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Speed: 25mm/s", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            "Pinch or double-tap to zoom & analyze points",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        )
                    }
                }

                AnimatedContent(targetState = isZoomed, label = "hud_crossfade") { zoomed ->
                    if (zoomed) {
                        RhythmAnalysisDetails(rhythm)
                    } else {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ClinicalSection("PATTERN", rhythm.pattern, rhythm.color)
                            ClinicalSection("CAUSES", rhythm.causes, AlertOrangeEnd)
                            ClinicalSection("TREATMENT", rhythm.treatment, AlertCyanEnd)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RhythmAnalysisDetails(rhythm: EcgRhythm) {
    val analysisTitle: String
    val analysisBody: String
    val analysisColor: Color

    when (rhythm) {
        EcgRhythm.NSR -> { analysisTitle = "NORMAL INTERVALS"; analysisBody = "• PR Interval: 0.12 - 0.20s\n• QRS Duration: < 0.12s\n• QT Interval: < 0.44s\n• Rhythm: Regular"; analysisColor = Color(0xFF00E676) }
        EcgRhythm.VFIB -> { analysisTitle = "VFIB CHARACTERISTICS"; analysisBody = "• Rate: Indeterminable\n• Rhythm: Chaotic & disorganized\n• P-waves: Absent\n• QRS: Absent"; analysisColor = Color(0xFFFF1744) }
        EcgRhythm.VTACH -> { analysisTitle = "VTACH CHARACTERISTICS"; analysisBody = "• Rate: > 100 bpm (Fast)\n• Rhythm: Regular\n• P-waves: Usually absent/hidden\n• QRS Duration: > 0.12s (Wide)"; analysisColor = Color(0xFFFF9100) }
        EcgRhythm.SVT -> { analysisTitle = "SVT CHARACTERISTICS"; analysisBody = "• Rate: 150 - 250 bpm\n• Rhythm: Regular\n• P-waves: Hidden in T-waves\n• QRS Duration: < 0.12s (Narrow)"; analysisColor = Color(0xFFD500F9) }
        EcgRhythm.AFIB -> { analysisTitle = "AFIB CHARACTERISTICS"; analysisBody = "• Rate: Variable\n• Rhythm: Irregularly Irregular\n• P-waves: Fibrillatory waves\n• QRS Duration: < 0.12s (Normal)"; analysisColor = Color(0xFF00E5FF) }
        EcgRhythm.TORSADES -> { analysisTitle = "TORSADES CHARACTERISTICS"; analysisBody = "• Rate: 150 - 250 bpm\n• Rhythm: Irregular (twisting)\n• P-waves: Absent\n• QRS Duration: Wide & polymorphic"; analysisColor = Color(0xFFFFEA00) }
        EcgRhythm.HEART_BLOCK -> { analysisTitle = "3rd DEGREE BLOCK CHARACTERISTICS"; analysisBody = "• Rate: Slow (Escape Rhythm)\n• Rhythm: P-P regular, R-R regular\n• P-waves: Independent from QRS\n• QRS Duration: Usually wide"; analysisColor = Color(0xFF00E676) }
        EcgRhythm.ASYSTOLE -> { analysisTitle = "ASYSTOLE CHARACTERISTICS"; analysisBody = "• Rate: Absent\n• Rhythm: Flatline\n• P-waves: Absent\n• QRS: Absent"; analysisColor = Color(0xFF9E9E9E) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .background(analysisColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .border(1.5.dp, analysisColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = "Analysis", tint = analysisColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(analysisTitle, color = analysisColor, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(analysisBody, color = EmergencySlateDark, fontSize = 15.sp, fontWeight = FontWeight.Black, lineHeight = 24.sp)
    }
}

@Composable
fun ClinicalSection(title: String, body: String, tint: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, tint.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(title, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(body, color = EmergencySlateDark, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
    }
}

@Composable
fun CardDetailsContent(drug: EmergencyDrug, isActive: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = drug.gradientEnd.copy(alpha = 0.25f))
                .background(Color.White.copy(alpha = 0.98f), RoundedCornerShape(16.dp))
                .border(1.dp, drug.gradientStart.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardCell("DOSE", if (isActive) drug.doseText else "0.0", EmergencySlateDark, modifier = Modifier.weight(1f))
            VerticalDivider(drug)
            DashboardCell(if (drug.deliveryType == DeliveryType.SYRINGE_PUSH) "VOL" else "TOTAL", if (isActive) drug.volumeText else "0.0", drug.gradientEnd, true, modifier = Modifier.weight(1f))
            VerticalDivider(drug)
            DashboardCell("SPEED", drug.pushSpeed, EmergencySlateDark, modifier = Modifier.weight(1.3f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = drug.gradientStart.copy(alpha = 0.25f))
                .background(Color.White.copy(alpha = 0.98f), RoundedCornerShape(16.dp))
                .border(1.dp, drug.gradientEnd.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DiluentBadge(modifier = Modifier.weight(1f).fillMaxHeight(), drug)
            VerticalDivider(drug)
            AccessDashboardCell(drug = drug, modifier = Modifier.weight(1.2f).fillMaxHeight())
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = drug.gradientEnd.copy(alpha = 0.2f))
                .background(Color.White.copy(alpha = 0.98f), RoundedCornerShape(12.dp))
                .drawBehind { drawRoundRect(drug.gradientEnd, Offset.Zero, Size(12f, size.height), CornerRadius(12f, 0f)) }
                .border(1.dp, drug.gradientStart.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(start = 24.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text("PREPARATION", color = drug.gradientEnd, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(drug.preparation, color = EmergencySlateDark, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun VerticalDivider(drug: EmergencyDrug) {
    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(drug.gradientStart.copy(alpha = 0.2f)))
}

@Composable
fun DashboardCell(title: String, value: String, color: Color, isBold: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 16.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, color = EmergencySlateLight, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = color, fontSize = if (isBold) 19.sp else 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun DiluentBadge(modifier: Modifier = Modifier, drug: EmergencyDrug) {
    Box(modifier = modifier.padding(vertical = 16.dp, horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DILUENT", color = EmergencySlateLight, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(drug.diluent.uppercase(), color = drug.gradientEnd, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AccessDashboardCell(drug: EmergencyDrug, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(vertical = 16.dp, horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CANNULA", color = EmergencySlateLight, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(drug.cannulaColor, CircleShape).shadow(6.dp, CircleShape, spotColor = drug.cannulaColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(drug.cannulaGauge, color = EmergencySlateDark, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

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
        else -> 0f
    }

    val isDrawPhase = isActive && phase < 0.25f
    val isReadyPhase = isActive && phase in 0.25f..0.40f
    val isPushPhase = isActive && phase in 0.40f..0.70f

    val pushProgress = if (!isActive) 0f else when {
        phase < 0.40f -> 0f
        phase > 0.70f -> 1f
        else -> (phase - 0.40f) / 0.30f
    }

    val targetVol = drug.calculatedVolume
    val maxContainer = drug.maxContainerVolume
    val maxFillPct = if (maxContainer > 0) min(1f, targetVol / maxContainer) else 0f
    val currentFill = maxFillPct * fillRatio

    val actionText = when {
        !isActive -> ""
        isDrawPhase -> "DRAWING MEDICATION..."
        isReadyPhase -> "READY TO PUSH"
        isPushPhase -> "PUSH! (${drug.pushSpeed})"
        else -> "FLUSH LINE"
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val cy = h * 0.5f

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
            val needleLen = 40f
            val hubW = 18f
            val needleStartX = barrelRight + hubW + 8f
            val needleEndX = needleStartX + needleLen

            if (isPushPhase && isActive) {
                val speedMultiplier = if (drug.pushSpeed.contains("RAPID", true)) 2.0f else 0.8f
                val squirtIntensity = sin(pushProgress * PI).toFloat() * speedMultiplier
                val streamStartX = needleEndX
                val streamMaxDist = w - streamStartX
                val streamDistance = streamMaxDist * squirtIntensity
                if (streamDistance > 5f) {
                    drawLine(brush = Brush.horizontalGradient(colors = listOf(Color.White, drug.gradientEnd, Color.Transparent), startX = streamStartX, endX = streamStartX + streamDistance), start = Offset(streamStartX, cy), end = Offset(streamStartX + streamDistance, cy), strokeWidth = 6f * squirtIntensity, cap = StrokeCap.Round)
                    drawLine(brush = Brush.horizontalGradient(colors = listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.0f)), startX = streamStartX, endX = streamStartX + streamDistance * 0.8f), start = Offset(streamStartX, cy), end = Offset(streamStartX + streamDistance * 0.8f, cy), strokeWidth = 4f * squirtIntensity, cap = StrokeCap.Round)
                }
            }

            drawRoundRect(brush = Brush.verticalGradient(listOf(Color(0xFFCFD8DC), Color(0xFFFFFFFF), Color(0xFF90A4AE)), startY = cy - 4f, endY = cy + 4f), topLeft = Offset(handleX, cy - 4f), size = Size(stopperXFinal - handleX, 8f))
            drawRoundRect(Color(0xFFB0BEC5), Offset(handleX, cy - barrelHalf * 0.5f), Size(stopperXFinal - handleX, 2f))
            drawRoundRect(Color(0xFF90A4AE), Offset(handleX, cy + barrelHalf * 0.5f - 2f), Size(stopperXFinal - handleX, 2f))

            val thumbW = 12f
            val thumbH = barrelHalf * 3.2f
            drawRoundRect(brush = Brush.verticalGradient(listOf(Color(0xFF90A4AE), Color(0xFFECEFF1), Color(0xFF607D8B)), startY = cy - thumbH / 2, endY = cy + thumbH / 2), topLeft = Offset(handleX - thumbW, cy - thumbH / 2), size = Size(thumbW, thumbH), cornerRadius = CornerRadius(4f))
            drawRoundRect(Color(0xFF78909C), Offset(handleX - thumbW + 2f, cy - thumbH / 2 + 2f), Size(thumbW - 4f, thumbH - 4f), CornerRadius(2f))

            val stopperW = 16f
            val stopperH = barrelHalf * 1.9f
            drawRoundRect(Color(0xFF1E293B), Offset(stopperXFinal - stopperW, cy - stopperH / 2), Size(stopperW, stopperH), CornerRadius(4f))
            drawLine(Color.Black, Offset(stopperXFinal - stopperW + 3f, cy - stopperH / 2), Offset(stopperXFinal - stopperW + 3f, cy + stopperH / 2), strokeWidth = 2f)
            drawLine(Color.Black, Offset(stopperXFinal - 3f, cy - stopperH / 2), Offset(stopperXFinal - 3f, cy + stopperH / 2), strokeWidth = 2f)

            if (liquidLength > 1f) {
                val shimmerOffset = (globalTime * 200f) % (barrelW * 2)
                drawRoundRect(brush = Brush.horizontalGradient(colors = listOf(drug.gradientEnd.copy(alpha = 0.7f), drug.gradientStart, drug.gradientEnd.copy(alpha = 0.7f)), startX = stopperXFinal - shimmerOffset, endX = stopperXFinal + barrelW - shimmerOffset), topLeft = Offset(stopperXFinal, cy - barrelHalf + 3f), size = Size(barrelRight - stopperXFinal, (barrelHalf - 3f) * 2f), cornerRadius = CornerRadius(3f))
                drawRoundRect(brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)), topLeft = Offset(stopperXFinal, cy - barrelHalf + 3f), size = Size(barrelRight - stopperXFinal, (barrelHalf - 3f)), cornerRadius = CornerRadius(3f))
                drawLine(color = drug.gradientEnd, start = Offset(stopperXFinal + 2f, cy - barrelHalf + 4f), end = Offset(stopperXFinal + 2f, cy + barrelHalf - 4f), strokeWidth = 4f, cap = StrokeCap.Round)

                if (isPushPhase) {
                    val flowCount = 6
                    val flowSpeed = if (drug.pushSpeed.contains("RAPID")) 250f else 100f
                    for (i in 0 until flowCount) {
                        val flowOffset = ((globalTime * flowSpeed) + (i * 30f)) % liquidLength
                        drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(stopperXFinal + flowOffset, cy - barrelHalf * 0.4f), end = Offset(stopperXFinal + flowOffset + 20f, cy - barrelHalf * 0.4f), strokeWidth = 2f, cap = StrokeCap.Round)
                        drawLine(color = Color.White.copy(alpha = 0.3f), start = Offset(stopperXFinal + flowOffset - 10f, cy + barrelHalf * 0.3f), end = Offset(stopperXFinal + flowOffset + 10f, cy + barrelHalf * 0.3f), strokeWidth = 2f, cap = StrokeCap.Round)
                    }
                }
            }

            val flangeW = 18f
            val flangeH = barrelHalf * 3.4f
            drawRoundRect(Color.White.copy(alpha = 0.6f), Offset(barrelLeft - flangeW, cy - flangeH / 2), Size(flangeW, flangeH), CornerRadius(6f))
            drawRoundRect(Color(0xFF90A4AE).copy(alpha = 0.8f), Offset(barrelLeft - flangeW, cy - flangeH / 2), Size(flangeW, flangeH), CornerRadius(6f), style = Stroke(4f))

            drawRoundRect(brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.15f)), startY = cy - barrelHalf, endY = cy + barrelHalf), topLeft = Offset(barrelLeft, cy - barrelHalf), size = Size(barrelW, barrelHalf * 2f), cornerRadius = CornerRadius(6f))
            drawRoundRect(Color(0xFF94A3B8).copy(alpha = 0.6f), Offset(barrelLeft, cy - barrelHalf), Size(barrelW, barrelHalf * 2f), CornerRadius(6f), style = Stroke(4f))

            val tickCount = if (maxContainer <= 1f) 5 else 10
            val tickStep = maxLiquidLength / tickCount
            val volStep = maxContainer / tickCount

            for (i in 0..tickCount) {
                val tickX = barrelRight - (i * tickStep)
                val isMajor = i % 2 == 0 || tickCount <= 5
                val tickH = if (isMajor) barrelHalf * 0.65f else barrelHalf * 0.35f
                drawLine(Color(0xFF0F172A).copy(alpha = 0.85f), Offset(tickX, cy), Offset(tickX, cy + tickH), strokeWidth = if (isMajor) 4f else 2.5f)
                if (isMajor) {
                    val volValue = volStep * i
                    val textStr = if (maxContainer <= 1f) String.format("%.1f", volValue).replace(".0", "") else volValue.toInt().toString()
                    val textY = cy - 10f
                    val shadowPaint = Paint().apply { color = android.graphics.Color.WHITE; textSize = if (maxContainer <= 1f) 18f else 26f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 4f }
                    drawContext.canvas.nativeCanvas.drawText(textStr, tickX, textY, shadowPaint)
                    val paint = Paint().apply { color = android.graphics.Color.parseColor("#0F172A"); textSize = if (maxContainer <= 1f) 18f else 26f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true }
                    drawContext.canvas.nativeCanvas.drawText(textStr, tickX, textY, paint)
                }
            }

            if (isActive) {
                val targetX = barrelRight - (maxLiquidLength * maxFillPct)
                drawLine(drug.gradientEnd, Offset(targetX, cy - barrelHalf - 14f), Offset(targetX, cy + barrelHalf + 14f), strokeWidth = 5f)
                drawOval(drug.gradientEnd, Offset(targetX - 7f, cy - barrelHalf - 18f), Size(14f, 14f))
            }

            val hubH = barrelHalf * 0.8f
            val hubPath = Path().apply { moveTo(barrelRight, cy - hubH / 2); lineTo(barrelRight + hubW, cy - 6f); lineTo(barrelRight + hubW, cy + 6f); lineTo(barrelRight, cy + hubH / 2); close() }
            drawPath(hubPath, Color(0xFFCFD8DC))
            drawPath(hubPath, Color(0xFF94A3B8), style = Stroke(3f))

            drawRect(drug.cannulaColor, Offset(barrelRight + hubW, cy - 8f), Size(8f, 16f))
            drawLine(Color(0xFFBDBDBD), Offset(needleStartX, cy), Offset(needleEndX, cy), strokeWidth = 6f)
            drawLine(Color.White, Offset(needleStartX, cy - 1.5f), Offset(needleEndX, cy - 1.5f), strokeWidth = 2.5f)

            if (isActive && actionText.isNotEmpty()) {
                val bannerPaint = Paint().apply {
                    color = if (isPushPhase) android.graphics.Color.parseColor("#D32F2F") else if (isReadyPhase) android.graphics.Color.parseColor("#388E3C") else android.graphics.Color.parseColor("#1565C0")
                    textSize = 34f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true
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
    val dropPhase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)), label = "")
    val pumpArrowsPhase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "")

    Box(modifier = Modifier.padding(top = 16.dp)) {
        Canvas(modifier = Modifier.width(110.dp).height(240.dp)) {
            val baseW = 240f; val baseH = 580f
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val scaleFactor = min(size.width / baseW, size.height / baseH)
            if (scaleFactor <= 0f) return@Canvas

            withTransform({
                scale(scaleFactor, scaleFactor, Offset(0f, 0f))
                translate((size.width / scaleFactor - baseW) / 2f, (size.height / scaleFactor - baseH) / 2f + 30f)
            }) {
                val cx = baseW / 2f

                drawRoundRect(brush = Brush.horizontalGradient(listOf(Color(0xFF9E9E9E), Color(0xFFE8E8E8), Color(0xFF757575))), topLeft = Offset(cx - 5f, 10f), size = Size(10f, 560f), cornerRadius = CornerRadius(5f))
                drawRoundRect(brush = Brush.horizontalGradient(listOf(Color(0xFF9E9E9E), Color(0xFFE8E8E8), Color(0xFF9E9E9E))), topLeft = Offset(cx - 90f, 10f), size = Size(180f, 8f), cornerRadius = CornerRadius(4f))

                val bagTop = 30f; val bagBot = 160f; val bagLeft = cx - 45f; val bagRight = cx + 45f; val bagH = bagBot - bagTop

                val loopPath = Path().apply {
                    moveTo(cx, bagTop); quadraticBezierTo(cx, bagTop - 9f, cx - 6f, bagTop - 11f)
                    quadraticBezierTo(cx - 12f, bagTop - 13f, cx - 13f, bagTop - 5f)
                    quadraticBezierTo(cx - 14f, bagTop + 3f, cx - 6f, bagTop + 3f)
                }
                drawPath(loopPath, Color(0xFF607D8B), style = Stroke(width = 3f, cap = StrokeCap.Round))

                val bagPath = Path().apply {
                    moveTo(bagLeft + 15f, bagTop); lineTo(bagRight - 15f, bagTop)
                    quadraticBezierTo(bagRight + 4f, bagTop + 20f, bagRight + 6f, bagTop + bagH * 0.55f)
                    quadraticBezierTo(bagRight + 6f, bagBot, cx, bagBot + 5f)
                    quadraticBezierTo(bagLeft - 6f, bagBot, bagLeft - 6f, bagTop + bagH * 0.55f)
                    quadraticBezierTo(bagLeft - 4f, bagTop + 20f, bagLeft + 15f, bagTop); close()
                }
                drawPath(bagPath, Brush.horizontalGradient(listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFFECEFF1), Color(0xFF90A4AE))))

                val fluidTop = bagTop + bagH * 0.42f
                val fluidPath = Path().apply {
                    moveTo(bagLeft - 6f, fluidTop); lineTo(bagRight + 6f, fluidTop)
                    lineTo(bagRight + 6f, bagTop + bagH * 0.55f)
                    quadraticBezierTo(bagRight + 6f, bagBot, cx, bagBot + 5f)
                    quadraticBezierTo(bagLeft - 6f, bagBot, bagLeft - 6f, bagTop + bagH * 0.55f); close()
                }
                drawPath(fluidPath, Brush.verticalGradient(listOf(drug.gradientStart.copy(alpha = 0.6f), drug.gradientEnd.copy(alpha = 0.9f)), startY = fluidTop, endY = bagBot))
                drawPath(path = bagPath, color = Color(0xFF90A4AE), style = Stroke(width = 1.5f))

                drawRoundRect(Color.White.copy(alpha = 0.6f), topLeft = Offset(cx - 30f, bagTop + 20f), size = Size(60f, 40f), cornerRadius = CornerRadius(4f))
                drawContext.canvas.nativeCanvas.apply {
                    drawText(drug.name.take(6), cx - 22f, bagTop + 36f, android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#0F172A"); textSize = 16f; isFakeBoldText = true; isAntiAlias = true })
                    drawText(drug.volumeText, cx - 18f, bagTop + 52f, android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#475569"); textSize = 12f; isAntiAlias = true })
                }

                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)), startY = bagBot + 4f, endY = bagBot + 18f), topLeft = Offset(cx - 10f, bagBot + 4f), size = Size(20f, 14f), cornerRadius = CornerRadius(3f))
                val spikeY = bagBot + 18f
                drawPath(Path().apply { moveTo(cx - 5f, spikeY); lineTo(cx, spikeY + 10f); lineTo(cx + 5f, spikeY); close() }, Color(0xFF0D47A1))

                drawLine(color = Color(0xFFB0BEC5), start = Offset(cx, spikeY + 10f), end = Offset(cx, 210f), strokeWidth = 6f)
                drawLine(color = drug.gradientEnd.copy(alpha = 0.3f), start = Offset(cx, spikeY + 10f), end = Offset(cx, 210f), strokeWidth = 2f)

                val chamberTop = 210f; val chamberH2 = 70f; val chamberBot = chamberTop + chamberH2; val chamberW2 = 32f

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

                val pumpTop2 = 330f; val pumpH2 = 180f; val pumpW2 = 120f; val pumpL2 = cx - 60f; val pumpR2 = cx + 60f

                val tubePath = Path().apply {
                    moveTo(cx, chamberBot + 8f)
                    cubicTo(cx, chamberBot + 30f, cx + 45f, pumpTop2 - 20f, cx + 45f, pumpTop2 + 10f)
                }
                drawPath(tubePath, Brush.horizontalGradient(listOf(Color(0x8090A4AE), Color(0xF0CFD8DC), Color(0x8078909C))), style = Stroke(width = 5f, cap = StrokeCap.Round))
                drawPath(tubePath, color = Color.White.copy(alpha = 0.25f), style = Stroke(width = 1.5f, cap = StrokeCap.Round))

                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF607D8B), Color(0xFFCFD8DC), Color(0xFFB0BEC5), Color(0xFF546E7A))), topLeft = Offset(pumpL2, pumpTop2), size = Size(pumpW2, pumpH2), cornerRadius = CornerRadius(8f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE0F2F1), Color(0xFF80CBC4)), startY = pumpTop2, endY = pumpTop2 + pumpH2), topLeft = Offset(pumpL2 + 4f, pumpTop2 + 4f), size = Size(pumpW2 - 8f, pumpH2 - 8f), cornerRadius = CornerRadius(6f))

                val scrW3 = 90f; val scrH3 = 60f; val scrL2 = pumpL2 + 10f; val scrT2 = pumpTop2 + 12f

                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(scrL2, scrT2), size = Size(scrW3, scrH3), cornerRadius = CornerRadius(8f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF020617)), startY = scrT2, endY = scrT2 + scrH3), topLeft = Offset(scrL2 + 4f, scrT2 + 4f), size = Size(scrW3 - 8f, scrH3 - 8f), cornerRadius = CornerRadius(6f))
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
// ─── MISSING MOA PATHWAY OVERLAY COMPOSABLE ───
@Composable
fun MoAPathwayOverlay(
    drug: EmergencyDrug,
    onClose: () -> Unit
) {
    // Map the string name from EmergencyDrug to the MoADrug enum expected by the animation engine
    val moaDrug = when {
        drug.name.contains("Epinephrine", ignoreCase = true) -> com.pasindu.nursingotapp.ui.screens.emergency.MoADrug.EPINEPHRINE
        drug.name.contains("Amiodarone", ignoreCase = true) -> com.pasindu.nursingotapp.ui.screens.emergency.MoADrug.AMIODARONE
        drug.name.contains("Naloxone", ignoreCase = true) -> com.pasindu.nursingotapp.ui.screens.emergency.MoADrug.NALOXONE
        drug.name.contains("Alteplase", ignoreCase = true) -> com.pasindu.nursingotapp.ui.screens.emergency.MoADrug.ALTEPLASE
        drug.name.contains("Adenosine", ignoreCase = true) -> com.pasindu.nursingotapp.ui.screens.emergency.MoADrug.ADENOSINE
        drug.name.contains("Atropine", ignoreCase = true) -> com.pasindu.nursingotapp.ui.screens.emergency.MoADrug.ATROPINE
        drug.name.contains("Dopamine", ignoreCase = true) -> com.pasindu.nursingotapp.ui.screens.emergency.MoADrug.DOPAMINE
        drug.name.contains("Magnesium", ignoreCase = true) -> com.pasindu.nursingotapp.ui.screens.emergency.MoADrug.MAGNESIUM_SULFATE
        drug.name.contains("Norepinephrine", ignoreCase = true) -> com.pasindu.nursingotapp.ui.screens.emergency.MoADrug.NOREPINEPHRINE
        drug.name.contains("Vasopressin", ignoreCase = true) -> com.pasindu.nursingotapp.ui.screens.emergency.MoADrug.VASOPRESSIN
        else -> null // Some drugs (like Fluids/Bicarb) don't have an organ animation yet
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A).copy(alpha = 0.90f))
                .padding(16.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = drug.gradientEnd)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(1.dp, drug.gradientEnd.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {} // Block clicks inside the card from closing the dialog
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(drug.gradientStart.copy(alpha = 0.1f))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MECHANISM OF ACTION", color = drug.gradientEnd, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(drug.name, color = EmergencySlateDark, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.background(Color.White, CircleShape).shadow(2.dp, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = EmergencySlateDark)
                    }
                }

                // --- ORGAN ANIMATION (If available) ---
                if (moaDrug != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFF0B1120)),
                        contentAlignment = Alignment.Center
                    ) {
                        com.pasindu.nursingotapp.ui.screens.emergency.MoAOrganAnimation(
                            drug = moaDrug,
                            modifier = Modifier.size(280.dp, 200.dp)
                        )
                    }
                }

                // --- PATHWAY STEPS ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(drug.moaSteps) { index, step ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(drug.gradientEnd.copy(alpha = 0.15f), CircleShape)
                                    .border(1.5.dp, drug.gradientEnd, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", color = drug.gradientEnd, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(step.title, color = EmergencySlateDark, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(step.description, color = EmergencySlateLight, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                            }
                        }
                        if (index < drug.moaSteps.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = EmergencySlateLight.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }
    }
}