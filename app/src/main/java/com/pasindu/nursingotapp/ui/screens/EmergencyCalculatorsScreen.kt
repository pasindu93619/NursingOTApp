package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle // Explicitly imported for Compose
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pasindu.nursingotapp.ui.components.EcgWaveformGenerator
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.min

// --- THEME PALETTE ---
private val EmergencyBgWhite = Color(0xFFF4F7FB)
private val EmergencySlateDark = Color(0xFF0F172A)
private val EmergencySlateLight = Color(0xFF64748B)

private val AlertRedStart = Color(0xFFFF1744)
private val AlertRedEnd = Color(0xFFD50000)
private val AlertOrangeStart = Color(0xFFFF9100)
private val AlertOrangeEnd = Color(0xFFE65100)
private val AlertPurpleStart = Color(0xFFD500F9)
private val AlertPurpleEnd = Color(0xFF6A1B9A)
private val AlertCyanStart = Color(0xFF00E5FF)
private val AlertCyanEnd = Color(0xFF00838F)
private val AlertBlueStart = Color(0xFF2979FF)
private val AlertBlueEnd = Color(0xFF1565C0)
private val AlertGreenStart = Color(0xFF00E676)
private val AlertGreenEnd = Color(0xFF1B5E20)

// Universal IV Cannula Color Coding
private val CannulaOrange = Color(0xFFFF9800) // 14G
private val CannulaGray = Color(0xFF9E9E9E)   // 16G
private val CannulaGreen = Color(0xFF4CAF50)  // 18G
private val CannulaPink = Color(0xFFE91E63)   // 20G
private val CannulaBlue = Color(0xFF2196F3)   // 22G

enum class DeliveryType { SYRINGE_PUSH, IV_INFUSION }

enum class EcgRhythm(
    val displayName: String,
    val color: Color,
    val pattern: String,
    val causes: String,
    val treatment: String
) {
    NSR("NORMAL SINUS RHYTHM", Color(0xFF2979FF), "Regular P-QRS-T complex. Rate 60-100 bpm.", "Normal physiological state, adequate perfusion.", "1. Continue Monitoring\n2. No emergency intervention required"),
    VFIB("VENTRICULAR FIBRILLATION", Color(0xFFD50000), "Chaotic, irregular waveform. No P/QRS/T waves. No cardiac output.", "Myocardial Infarction, Severe Hypoxia, Severe Electrolyte Imbalance.", "1. Immediate Defibrillation (200J Biphasic)\n2. High-quality CPR\n3. Epinephrine 1mg IV every 3-5 min\n4. Amiodarone 300mg IV push"),
    VTACH("PULSELESS V-TACH", Color(0xFFE65100), "Wide QRS (>120ms). Rapid, regular rhythm. No palpable pulse.", "Coronary Ischemia, Structural Heart Disease, Prolonged QT interval.", "1. Immediate Defibrillation (200J Biphasic)\n2. High-quality CPR\n3. Epinephrine 1mg IV\n4. Amiodarone 300mg IV push"),
    SVT("SUPRAVENTRICULAR TACHY", Color(0xFF6A1B9A), "Narrow QRS. Regular, extremely fast (150-250 bpm). P waves buried in T waves.", "AVNRT Re-entry pathways, Stress, Stimulants, Hypoxia, Electrolyte shifts.", "1. Vagal maneuvers\n2. Adenosine 6mg rapid IV push + 20mL flush\n3. Adenosine 12mg if no conversion\n4. Synchronized Cardioversion"),
    AFIB("ATRIAL FIBRILLATION", Color(0xFF00B0FF), "Absence of P waves. Irregularly irregular rhythm. Variable ventricular rate.", "Long-standing Hypertension, Valvular Disease, Thyroid disease.", "1. Rate control (Beta-blockers / Diltiazem)\n2. Anticoagulation assessment (CHA2DS2-VASc)\n3. Cardioversion if unstable"),
    TORSADES("TORSADES DE POINTES", Color(0xFFFF8F00), "Polymorphic VT. Spindle-like twisting of QRS complexes around baseline.", "Hypomagnesemia, Severe Hypokalemia, QT-prolonging drugs.", "1. Magnesium Sulfate 2g IV over 10-15 min\n2. Defibrillation if pulseless\n3. Overdrive pacing if refractory"),
    HEART_BLOCK("COMPLETE 3RD DEGREE BLOCK", Color(0xFF00C853), "P waves and QRS complexes completely dissociated. Severe bradycardia.", "Extensive Myocardial Infarction, Conduction Sclerosis, Beta-Blocker toxicity.", "1. Atropine 0.5mg - 1mg IV\n2. Transcutaneous Pacing (TCP)\n3. Epinephrine or Dopamine Infusion"),
    ASYSTOLE("ASYSTOLE", Color(0xFF616161), "Flat line. Absence of all ventricular electrical and mechanical activity.", "Reversible H's and T's (Hypoxia, Hypovolemia, Tension Pneumo, Tamponade, Toxins).", "1. High-quality CPR immediately\n2. Epinephrine 1mg IV every 3-5 min\n3. Search for reversible causes\n4. DO NOT DEFIBRILLATE")
}

data class MoAStep(val title: String, val description: String)

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
    val mechanism: String,
    val halfLife: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val deliveryType: DeliveryType,
    val maxContainerVolume: Float,
    val calculatedVolume: Float,
    val moaSteps: List<MoAStep>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCalculatorsScreen(onNavigateBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var weightInput by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }

    var selectedDrug by remember { mutableStateOf<EmergencyDrug?>(null) }
    var zoomedEcg by remember { mutableStateOf<EcgRhythm?>(null) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val weight = weightInput.toFloatOrNull() ?: 0f

    val drugs = remember(weight) {
        listOf(
            run {
                val dose = if (weight == 0f) 0f else if (weight >= 50f) 1f else min(1f, weight * 0.01f)
                val vol = if (dose > 0f) dose / 0.1f else 0f
                EmergencyDrug(
                    category = "CARDIAC ARREST",
                    name = "Epinephrine (1:10,000)",
                    concentration = "Ampoule: 0.1 mg/mL (1:10,000)",
                    doseText = "${String.format(Locale.US, "%.2f", dose)} mg",
                    volumeText = "${String.format(Locale.US, "%.1f", vol)} mL",
                    diluent = "FLUSH 20mL NS",
                    pushSpeed = "RAPID PUSH",
                    cannulaGauge = "18G (Green) / IO",
                    cannulaColor = CannulaGreen,
                    preparation = "Use 1:10,000 pre-filled cardiac syringe. In pediatric arrest, calculate 0.01 mg/kg (0.1 mL/kg).",
                    safetyTip = "Always follow immediately with a rapid 20mL Normal Saline flush and elevate the limb for 10-20 seconds.",
                    clinicalPearl = "Epinephrine has a circulating half-life of 2-3 mins. Rapid vascular flush forces the drug into central aortic root circulation.",
                    mechanism = "Potent nonselective α1, β1, and β2 adrenergic agonist.",
                    halfLife = "2 - 3 minutes",
                    gradientStart = AlertRedStart,
                    gradientEnd = AlertRedEnd,
                    deliveryType = DeliveryType.SYRINGE_PUSH,
                    maxContainerVolume = 10f,
                    calculatedVolume = vol,
                    moaSteps = listOf(
                        MoAStep("Adrenergic Binding", "Binds powerfully to α1, β1, and β2 adrenergic receptors on vascular and cardiac tissue."),
                        MoAStep("Vascular Constriction", "α1 activation triggers intense peripheral vasoconstriction, raising systemic vascular resistance."),
                        MoAStep("Coronary Perfusion", "Shunts venous return to the vital coronary arteries and cerebral vascular beds during CPR compressions."),
                        MoAStep("Automaticity Spike", "β1 stimulation increases spontaneous myocardial contractility and electrical pacemaker recovery.")
                    )
                )
            },
            run {
                val dose = if (weight == 0f) 0f else min(0.5f, weight * 0.01f)
                val vol = if (dose > 0f) dose / 1.0f else 0f
                EmergencyDrug(
                    category = "ANAPHYLAXIS",
                    name = "Epinephrine (1:1,000)",
                    concentration = "Ampoule: 1.0 mg/mL (1:1,000)",
                    doseText = "${String.format(Locale.US, "%.2f", dose)} mg",
                    volumeText = "${String.format(Locale.US, "%.2f", vol)} mL",
                    diluent = "DO NOT DILUTE",
                    pushSpeed = "INTRAMUSCULAR",
                    cannulaGauge = "21G - 23G IM Needle",
                    cannulaColor = CannulaBlue,
                    preparation = "Use a precision 1 mL tuberculin syringe. Inject deep IM into the anterolateral mid-thigh (Vastus Lateralis).",
                    safetyTip = "CRITICAL: 10x more concentrated than cardiac arrest Epi! NEVER administer 1:1,000 as an IV push.",
                    clinicalPearl = "The Vastus Lateralis muscle achieves higher and faster peak plasma concentrations than deltoid or subcutaneous routes.",
                    mechanism = "Rapid reversal of histamine-induced vasodilation and bronchoconstriction.",
                    halfLife = "2 - 3 minutes",
                    gradientStart = AlertOrangeStart,
                    gradientEnd = AlertOrangeEnd,
                    deliveryType = DeliveryType.SYRINGE_PUSH,
                    maxContainerVolume = 1f,
                    calculatedVolume = vol,
                    moaSteps = listOf(
                        MoAStep("Intramuscular Uptake", "Rapidly absorbed via rich vascular beds of the Vastus Lateralis muscle."),
                        MoAStep("Bronchial Dilation", "β2 receptor agonism relaxes bronchial smooth muscle, opening airways."),
                        MoAStep("Airway Edema Reversal", "α1 vasoconstriction decreases mucosal edema and laryngeal swelling."),
                        MoAStep("Mast Cell Stabilization", "Inhibits antigen-induced release of histamine and leukotrienes from mast cells.")
                    )
                )
            },
            run {
                val dose = if (weight == 0f) 0f else if (weight > 40f) 300f else min(300f, weight * 5f)
                val vol = if (dose > 0f) dose / 50f else 0f
                EmergencyDrug(
                    category = "PULSELESS VT / VF",
                    name = "Amiodarone (Cordarone)",
                    concentration = "Ampoule: 50 mg/mL (150mg/3mL)",
                    doseText = "${String.format(Locale.US, "%.0f", dose)} mg",
                    volumeText = "${String.format(Locale.US, "%.1f", vol)} mL",
                    diluent = "UNDILUTED FOR ARREST",
                    pushSpeed = "RAPID IV PUSH",
                    cannulaGauge = "18G (Green)",
                    cannulaColor = CannulaGreen,
                    preparation = "First Dose: 300mg IV push. Second Dose: 150mg IV push after 3-5 minutes if refractory.",
                    safetyTip = "In cardiac arrest, push undiluted. In LIVE patients with a pulse, infuse strictly in 5% Dextrose over 10 minutes.",
                    clinicalPearl = "Foams heavily when agitated due to polysorbate 80. Draw slowly with a large bore needle.",
                    mechanism = "Class III antiarrhythmic blocking potassium channels and cardiac action potential.",
                    halfLife = "Up to 58 days (Lipophilic)",
                    gradientStart = AlertPurpleStart,
                    gradientEnd = AlertRedEnd,
                    deliveryType = DeliveryType.SYRINGE_PUSH,
                    maxContainerVolume = 10f,
                    calculatedVolume = vol,
                    moaSteps = listOf(
                        MoAStep("Potassium Blockade", "Blocks myocardial potassium channels, prolonging Phase 3 repolarization."),
                        MoAStep("Refractory Extension", "Significantly increases the refractory period of atrial, nodal, and ventricular tissue."),
                        MoAStep("Sodium & Calcium Block", "Provides mild Class I sodium and Class IV calcium channel blocking effects."),
                        MoAStep("Rhythm Stabilization", "Suppresses chaotic re-entry circuits, stabilizing the ventricular myocardium.")
                    )
                )
            },
            run {
                val dose = if (weight == 0f) 0f else weight * 1f
                val vol = dose / 1f
                EmergencyDrug(
                    category = "METABOLIC ACIDOSIS",
                    name = "Sodium Bicarbonate 8.4%",
                    concentration = "Vial: 1.0 mEq/mL (8.4%)",
                    doseText = "${String.format(Locale.US, "%.0f", dose)} mEq",
                    volumeText = "${String.format(Locale.US, "%.1f", vol)} mL",
                    diluent = "UNDILUTED",
                    pushSpeed = "SLOW IV PUSH (2 mins)",
                    cannulaGauge = "18G (Green)",
                    cannulaColor = CannulaGreen,
                    preparation = "Standard dose is 1 mEq/kg (1 mL/kg of 8.4% solution).",
                    safetyTip = "CRITICAL: Flush IV line thoroughly before and after. Precipitously forms chalky precipitate with Calcium.",
                    clinicalPearl = "Generates large amounts of dissolved CO2. Patient must have adequate alveolar ventilation to blow off the CO2.",
                    mechanism = "Direct systemic alkalinizing buffer and sodium donor.",
                    halfLife = "Rapid (Cleared by respiratory system)",
                    gradientStart = AlertBlueStart,
                    gradientEnd = AlertCyanEnd,
                    deliveryType = DeliveryType.SYRINGE_PUSH,
                    maxContainerVolume = 60f,
                    calculatedVolume = vol,
                    moaSteps = listOf(
                        MoAStep("Ionic Dissociation", "Dissociates instantly in plasma into Sodium (Na+) and Bicarbonate (HCO3-)."),
                        MoAStep("Proton Buffering", "HCO3- binds to free Hydrogen (H+) ions, forming Carbonic Acid (H2CO3)."),
                        MoAStep("Gas Conversion", "H2CO3 breaks down into Water (H2O) and Carbon Dioxide (CO2)."),
                        MoAStep("pH Normalization", "Restores plasma pH, stabilizing cellular enzyme function and cardiac contractility.")
                    )
                )
            },
            run {
                val dose = if (weight == 0f) 0f else min(2f, 0.4f)
                val vol = if (dose > 0f) dose / 0.4f else 0f
                EmergencyDrug(
                    category = "OPIOID OVERDOSE",
                    name = "Naloxone (Narcan)",
                    concentration = "Ampoule: 0.4 mg/mL",
                    doseText = "${String.format(Locale.US, "%.2f", dose)} mg",
                    volumeText = "${String.format(Locale.US, "%.1f", vol)} mL",
                    diluent = "UNDILUTED",
                    pushSpeed = "TITRATED IV PUSH",
                    cannulaGauge = "20G (Pink)",
                    cannulaColor = CannulaPink,
                    preparation = "Initial dose: 0.4mg - 2mg IV/IM. Repeat every 2-3 minutes as needed.",
                    safetyTip = "Titrate strictly to restore respiratory drive (RR > 10), not to full alertness, to avoid severe acute withdrawal.",
                    clinicalPearl = "Naloxone's duration of action (30-80 mins) is shorter than most opioids. Monitor closely for rebound overdose.",
                    mechanism = "Pure competitive antagonist at mu, kappa, and sigma opioid receptors.",
                    halfLife = "30 - 80 minutes",
                    gradientStart = AlertPurpleStart,
                    gradientEnd = AlertBlueEnd,
                    deliveryType = DeliveryType.SYRINGE_PUSH,
                    maxContainerVolume = 3f,
                    calculatedVolume = vol,
                    moaSteps = listOf(
                        MoAStep("Blood-Brain Barrier", "Rapidly crosses the blood-brain barrier directly into central nervous system tissue."),
                        MoAStep("Receptor Displacement", "Competitively displaces opioid agonists from mu-opioid receptor sites."),
                        MoAStep("Inhibition Reversal", "Reverses opioid-induced inhibition of the medullary respiratory center."),
                        MoAStep("Drive Restoration", "Instantly restores spontaneous respiratory effort and protective airway reflexes.")
                    )
                )
            },
            run {
                val dose = if (weight == 0f) 0f else min(1f, 0.5f)
                val vol = if (dose > 0f) dose / 0.6f else 0f
                EmergencyDrug(
                    category = "SYMPTOMATIC BRADYCARDIA",
                    name = "Atropine Sulfate",
                    concentration = "Ampoule: 0.6 mg/mL",
                    doseText = "${String.format(Locale.US, "%.2f", dose)} mg",
                    volumeText = "${String.format(Locale.US, "%.2f", vol)} mL",
                    diluent = "UNDILUTED",
                    pushSpeed = "RAPID IV PUSH",
                    cannulaGauge = "20G (Pink)",
                    cannulaColor = CannulaPink,
                    preparation = "First Dose: 0.5mg - 1mg IV push. Repeat every 3-5 mins up to a maximum total of 3mg.",
                    safetyTip = "Do not administer doses under 0.5mg in adults; paradoxical bradycardia may occur.",
                    clinicalPearl = "Ineffective in high-grade 2nd Degree Type II or 3rd Degree Heart Blocks. Prepare transcutaneous pacing.",
                    mechanism = "Competitive muscarinic acetylcholine receptor antagonist blocking vagal tone.",
                    halfLife = "2 - 3 hours",
                    gradientStart = AlertGreenStart,
                    gradientEnd = AlertGreenEnd,
                    deliveryType = DeliveryType.SYRINGE_PUSH,
                    maxContainerVolume = 3f,
                    calculatedVolume = vol,
                    moaSteps = listOf(
                        MoAStep("Vagal Blockade", "Competitively blocks acetylcholine at postganglionic muscarinic receptors."),
                        MoAStep("SA Node Discharge", "Inhibits parasympathetic (vagal) tone, accelerating the SA node firing rate."),
                        MoAStep("AV Conduction Velocity", "Enhances atrioventricular (AV) nodal conduction velocity and reduces PR interval."),
                        MoAStep("Heart Rate Elevation", "Increases cardiac output and blood pressure in vagal-induced bradycardia.")
                    )
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Crash Cart Engine", fontWeight = FontWeight.Black, color = EmergencySlateDark, fontSize = 22.sp)
                        Text("Instant ACLS Emergency Calculations", fontSize = 12.sp, color = AlertRedStart, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmergencySlateDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = EmergencyBgWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- WEIGHT INPUT & RAPID PRESET CHIPS ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = AlertRedStart.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("PATIENT RESUSCITATION WEIGHT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            placeholder = { Text("e.g. 70") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AlertRedStart,
                                focusedLabelColor = AlertRedStart,
                                focusedContainerColor = Color(0xFFFFF1F2),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = EmergencySlateDark)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(AlertRedStart.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (weight > 0f) "${weight.toInt()}kg" else "⚖️", fontSize = 16.sp, fontWeight = FontWeight.Black, color = AlertRedStart)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("10", "20", "50", "60", "70", "80", "100")) { preset ->
                            val isSelected = weightInput == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AlertRedStart else Color(0xFFF1F5F9))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        weightInput = preset
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("${preset}kg", color = if (isSelected) Color.White else EmergencySlateDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // --- INTERACTIVE ECG RHYTHM CAROUSEL ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = AlertRedStart, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ECG Rhythm Monitor", fontSize = 16.sp, fontWeight = FontWeight.Black, color = EmergencySlateDark)
                }
                Text("Tap to Analyze", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(EcgRhythm.values()) { rhythm ->
                    EcgMiniStripCard(rhythm = rhythm) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        zoomedEcg = rhythm
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- DRUG RESUSCITATION CARDS LIST ---
            Text("Emergency Crash Cart Dosing", fontSize = 16.sp, fontWeight = FontWeight.Black, color = EmergencySlateDark)

            // FIX: Using selectedDrug directly to prevent Unresolved Reference
            drugs.forEach { drug ->
                EmergencyDrugCard(
                    drug = drug,
                    weightKg = weight,
                    onViewMoa = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedDrug = drug
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (zoomedEcg != null) {
            EcgZoomAnalysisDialog(rhythm = zoomedEcg!!, onDismiss = { zoomedEcg = null })
        }

        if (selectedDrug != null) {
            DrugMoADetailDialog(drug = selectedDrug!!, onDismiss = { selectedDrug = null })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🫀 INTERACTIVE ECG MINI STRIP CARD (INTEGRATED WITH GENERATOR & GRID)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EcgMiniStripCard(rhythm: EcgRhythm, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ecg_pulse")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        // Increased to 25,000ms for a very slow, realistic monitor sweep
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "offset"
    )

    Card(
        modifier = Modifier
            .width(180.dp)
            .height(110.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = rhythm.color.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)) // Slate Black Monitor
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    rhythm.displayName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = rhythm.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Animated ECG Trace Canvas with Clinical Grid
            Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                val w = size.width
                val h = size.height

                // Draw solid background
                drawRect(Color(0xFF0F172A))

                // Medical Monitor Major/Minor Grid
                val minorGridSize = 10.dp.toPx()
                val majorGridSize = 50.dp.toPx()

                // Minor Grid Lines (Faint)
                for (x in 0..w.toInt() step minorGridSize.toInt()) {
                    drawLine(Color.Gray.copy(alpha = 0.1f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), h))
                }
                for (y in 0..h.toInt() step minorGridSize.toInt()) {
                    drawLine(Color.Gray.copy(alpha = 0.1f), Offset(0f, y.toFloat()), Offset(w, y.toFloat()))
                }

                // Major Grid Lines (Darker)
                for (x in 0..w.toInt() step majorGridSize.toInt()) {
                    drawLine(Color.Gray.copy(alpha = 0.25f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), h), strokeWidth = 2f)
                }
                for (y in 0..h.toInt() step majorGridSize.toInt()) {
                    drawLine(Color.Gray.copy(alpha = 0.25f), Offset(0f, y.toFloat()), Offset(w, y.toFloat()), strokeWidth = 2f)
                }

                val pathWidth = size.width + 2000f
                val ecgPath = EcgWaveformGenerator.generatePath(rhythm, pathWidth, size.height)

                withTransform({
                    translate(left = -offsetX)
                }) {
                    // Faint glow behind the line to simulate CRT phosphors
                    drawPath(
                        path = ecgPath,
                        color = rhythm.color.copy(alpha = 0.25f),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Sharp actual trace line
                    drawPath(
                        path = ecgPath,
                        color = rhythm.color,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 💊 EMERGENCY DRUG RESUSCITATION CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EmergencyDrugCard(
    drug: EmergencyDrug,
    weightKg: Float,
    onViewMoa: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = drug.gradientStart.copy(alpha = 0.35f))
            .border(1.dp, drug.gradientStart.copy(alpha = 0.2f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color.White, drug.gradientStart.copy(alpha = 0.04f))))
                .padding(20.dp)
        ) {
            // Category & Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(drug.gradientStart.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(drug.category, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = drug.gradientStart, letterSpacing = 0.5.sp)
                }

                // Push Speed Badge
                Box(
                    modifier = Modifier
                        .background(EmergencySlateDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(drug.pushSpeed, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Drug Title & Concentration
            Text(drug.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = EmergencySlateDark)
            Text(drug.concentration, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            // Dose & Volume Hero Splitter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CALCULATED DOSE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(if (weightKg > 0f) drug.doseText else "--", fontSize = 22.sp, fontWeight = FontWeight.Black, color = drug.gradientStart)
                }

                Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFCBD5E1)))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VOLUME TO DRAW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(if (weightKg > 0f) drug.volumeText else "--", fontSize = 22.sp, fontWeight = FontWeight.Black, color = EmergencySlateDark)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cannula & Flush Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(drug.cannulaColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(drug.cannulaGauge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmergencySlateDark)
                }

                Text(drug.diluent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = drug.gradientStart)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clinical Safety Alert Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(drug.safetyTip, fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Medium, lineHeight = 16.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: View Mechanism of Action
            Button(
                onClick = onViewMoa,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmergencySlateDark)
            ) {
                Text("Mechanism of Action & Physiology", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🔬 MECHANISM OF ACTION (MoA) DETAIL DIALOG
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DrugMoADetailDialog(drug: EmergencyDrug, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .shadow(24.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(drug.category, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = drug.gradientStart, letterSpacing = 1.sp)
                        Text(drug.name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = EmergencySlateDark)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = EmergencySlateDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(16.dp))

                // Timeline / Pharmacokinetics HUD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("HALF-LIFE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(drug.halfLife, fontSize = 14.sp, fontWeight = FontWeight.Black, color = EmergencySlateDark)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("DELIVERY VELOCITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(drug.pushSpeed, fontSize = 14.sp, fontWeight = FontWeight.Black, color = drug.gradientStart)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Step-by-Step Cellular Mechanism", fontSize = 15.sp, fontWeight = FontWeight.Black, color = EmergencySlateDark)
                Spacer(modifier = Modifier.height(12.dp))

                // Pathway Steps
                drug.moaSteps.forEachIndexed { index, step ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(drug.gradientStart, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(step.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmergencySlateDark)
                            Text(step.description, fontSize = 12.sp, color = EmergencySlateLight, lineHeight = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clinical Pearl Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💡 Senior Sister Clinical Pearl", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D4ED8))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(drug.clinicalPearl, fontSize = 12.sp, color = Color(0xFF1E40AF), lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencySlateDark)
                ) {
                    Text("Return to Crash Cart", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 📈 FULL-SCREEN ECG ZOOM & ACLS INTERVENTION DIALOG (INTEGRATED GENERATOR & GRID)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EcgZoomAnalysisDialog(rhythm: EcgRhythm, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .shadow(24.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ACLS RHYTHM ANALYSIS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = rhythm.color, letterSpacing = 1.sp)
                        Text(rhythm.displayName, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large Animated ECG Oscilloscope
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF020617)) // Darkest Slate
                        .border(1.dp, rhythm.color.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "zoom_wave")
                    val offsetX by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 2000f,
                        // Increased to 25,000ms for slow, realistic clinical sweep
                        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
                        label = "offset"
                    )

                    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                        val w = size.width
                        val h = size.height

                        // Major & Minor Medical Grid Lines
                        val minorGridSize = 10.dp.toPx()
                        val majorGridSize = 50.dp.toPx()

                        // Minor Grid Lines (Faint)
                        for (x in 0..w.toInt() step minorGridSize.toInt()) {
                            drawLine(Color.DarkGray.copy(alpha = 0.15f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), h))
                        }
                        for (y in 0..h.toInt() step minorGridSize.toInt()) {
                            drawLine(Color.DarkGray.copy(alpha = 0.15f), Offset(0f, y.toFloat()), Offset(w, y.toFloat()))
                        }

                        // Major Grid Lines (Darker/Thicker)
                        for (x in 0..w.toInt() step majorGridSize.toInt()) {
                            drawLine(Color.DarkGray.copy(alpha = 0.4f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), h), strokeWidth = 2f)
                        }
                        for (y in 0..h.toInt() step majorGridSize.toInt()) {
                            drawLine(Color.DarkGray.copy(alpha = 0.4f), Offset(0f, y.toFloat()), Offset(w, y.toFloat()), strokeWidth = 2f)
                        }

                        // Medical Path Generation
                        val pathWidth = size.width + 2000f
                        val ecgPath = EcgWaveformGenerator.generatePath(rhythm, pathWidth, size.height)

                        withTransform({
                            translate(left = -offsetX)
                        }) {
                            // Faint monitor glow
                            drawPath(
                                path = ecgPath,
                                color = rhythm.color.copy(alpha = 0.25f),
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            // Sharp actual trace line
                            drawPath(
                                path = ecgPath,
                                color = rhythm.color,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Rhythm Characteristics
                Text("Waveform Pattern", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = rhythm.color)
                Text(rhythm.pattern, fontSize = 13.sp, color = Color.LightGray, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

                Text("Underlying Causes (H's & T's)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(rhythm.causes, fontSize = 13.sp, color = Color.LightGray, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

                Text("Immediate Emergency Interventions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AlertGreenStart)
                Text(rhythm.treatment, fontSize = 13.sp, color = Color.White, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp, bottom = 20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = rhythm.color)
                ) {
                    Text("Close Monitor", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}