package com.pasindu.nursingotapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// --- PREMIUM LIGHT THEME PALETTE ---
private val ToolsBgWhite = Color(0xFFF8FAFC)
private val ToolsSlateDark = Color(0xFF0F172A)
private val ToolsSlateLight = Color(0xFF64748B)
private val TechBluePrimary = Color(0xFF2979FF)
private val AiPurple = Color(0xFFD500F9)
private val AiCyan = Color(0xFF00E5FF)

// --- DATA MODELS ---
data class ClinicalToolModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val emoji: String,
    val colorStart: Color,
    val colorEnd: Color,
    val delay: Int,
    val onClick: () -> Unit,
    val keywords: List<String>,
    val sinhalaKeywords: List<String>,
    val capabilities: String,
    val limitations: String
)

data class WebCalculatorSource(val name: String, val subtitle: String, val url: String, val badgeColor: Color)

// --- SHARED PREFERENCES HELPER FOR AI HISTORY ---
private const val PREFS_NAME = "ai_search_prefs"
private const val KEY_HISTORY = "search_history"

private fun getSearchHistory(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return (prefs.getString(KEY_HISTORY, "") ?: "").split("|::|").filter { it.isNotBlank() }
}

private fun saveSearchHistory(context: Context, query: String) {
    if (query.isBlank()) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val currentHistory = getSearchHistory(context).toMutableList()
    currentHistory.remove(query.trim())
    currentHistory.add(0, query.trim())
    prefs.edit().putString(KEY_HISTORY, currentHistory.take(5).joinToString("|::|")).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalToolsScreen(
    onNavigateToIvDrip: () -> Unit, onNavigateToDosage: () -> Unit, onNavigateToWeightInfusion: () -> Unit,
    onNavigateToBsa: () -> Unit, onNavigateToPediatric: () -> Unit, onNavigateToConversions: () -> Unit,
    onNavigateToSpecialCalcs: () -> Unit, onNavigateToEmergency: () -> Unit, onNavigateToIcu: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchBarFocused by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(getSearchHistory(context)) }
    var evaluationTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { delay(100); isVisible = true }

    // --- DEEP APP AWARENESS REPOSITORY ---
    val allTools = remember {
        listOf(
            ClinicalToolModule("emergency", "Crash Cart Engine", "Cardiac Arrest, Anaphylaxis, RSI", "• Instant Parallel Processing\n• Weight-Based Resuscitation\n• Code Red Animated ECG", "🚨", Color(0xFFFF1744), Color(0xFFD50000), 50, { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToEmergency() }, listOf("crash", "cart", "cardiac", "arrest", "anaphylaxis", "rsi", "emergency", "resuscitation", "cpr", "adrenaline", "epinephrine", "shock", "defib", "vtach", "vfib", "asystole"), listOf("හෘදයාබාධ", "ඇඩ්‍රිනලින්", "හදිසි", "ශොක්", "ඇනෆිලැක්සිස්", "cpr", "arrest", "hadisi"), "ACLS Protocols, Rapid Push Adrenaline/Amiodarone derivations.", "Cannot calculate custom continuous infusions outside of the crash cart rapid push protocol."),
            ClinicalToolModule("icu", "ICU Critical Care", "Vasoactive, Sedation & Fluids", "• Inotrope Dose-Rate (μg/kg/min)\n• Electrolyte Repletion (K+, Mg++)\n• TPN & Pharmacokinetics", "🫀", Color(0xFF2979FF), Color(0xFF0D47A1), 100, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIcu() }, listOf("icu", "critical", "vasoactive", "sedation", "fluid", "infusion", "electrolyte", "potassium", "kcl", "noradrenaline", "norepinephrine", "dopamine", "dobutamine", "inotrope", "magnesium"), listOf("අයිසීයූ", "පොටෑසියම්", "නොරැඩ්‍රිනලින්", "ඩොපමයින්", "ඉලෙක්ට්‍රොලයිට්"), "Inotropes, Syringe Pumps, Analgesia, Sedation & Electrolyte Balancing.", "Does not calculate anion gap, ABG interpretation, or MAP calculations."),
            ClinicalToolModule("dosage", "Advanced Dosage", "5-in-1 Math Engine", "• Standard Liquid & % Solutions\n• Dilutions (C₁V₁=C₂V₂)\n• Powder Reconstitution", "💊", Color(0xFFD500F9), Color(0xFF4A148C), 150, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToDosage() }, listOf("dosage", "math", "dilution", "reconstitution", "powder", "dextrose", "concentration", "liquid", "c1v1", "c2v2", "ampoule", "vial", "diluent", "antibiotic", "ceftriaxone"), listOf("ඩෙක්ස්ට්‍රෝස්", "දියාරු", "කුඩු", "මාත්‍රාව", "බෙහෙත්", "ප්‍රතිශතය", "ද්‍රාවණය"), "Dextrose Dilutions (Active + Zero % Diluent), Reconstitution & Liquid Dosage (D/H*Q).", "CANNOT mix two active percentages together (e.g. 10% + 50%). Cannot do Alligation Math."),
            ClinicalToolModule("iv_drip", "IV Drip Sync", "Drops Per Minute & AR", "• Macro (10/15/20) & Micro (60) Sets\n• Live AR Hologram Synchronization\n• 15-Second Clinical Tap Verification", "💧", Color(0xFF00E5FF), Color(0xFF006064), 200, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIvDrip() }, listOf("iv", "drip", "sync", "drops", "minute", "macro", "micro", "flow rate", "drop factor", "gtt", "saline", "normal saline", "dpm", "chamber", "gravity set"), listOf("ඩ්‍රිප්", "බිංදු", "පැයට බිංදු", "සේලයින්", "වතුර"), "Gravity Drip Calibration (Drops/min) & Visual Holographic Sync.", "Does not control physical electronic hardware syringe pumps via Bluetooth."),
            ClinicalToolModule("special_calcs", "High-Alert Specials", "Insulin, Heparin, PCA", "• Sliding Scale & IV Insulin\n• Heparin Weight-Based Protocols\n• Opioid PCA Lockout Limits", "🩸", Color(0xFFE53935), Color(0xFFB71C1C), 250, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToSpecialCalcs() }, listOf("high alert", "insulin", "heparin", "pca", "opioid", "sliding scale", "actrapid", "blood sugar", "glucose", "cbg", "dka", "aptt"), listOf("ඉන්සියුලින්", "හෙපරින්", "සීනි", "ග්ලූකෝස්", "රුධිර සීනි"), "Continuous IV Insulin Protocols & Heparin Nomograms.", "Does not adjust algorithm dynamically for acute renal impairment."),
            ClinicalToolModule("weight_infusion", "Weight & Infusions", "mg/kg & mcg/kg/min", "• Pediatric Simple Dosing\n• Complex Continuous Infusion Rates\n• Built-in Safety & Weight Alerts", "⚖️", Color(0xFF00BFA5), Color(0xFF004D40), 300, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToWeightInfusion() }, listOf("weight", "infusion", "mg/kg", "mcg/kg/min", "continuous", "rate", "body weight", "milligrams per kilogram"), listOf("බර", "බර අනුව", "ශරීර බර", "කිලෝග්‍රෑම්", "bara", "weight"), "Weight-Based Continuous Drug Infusion Rates (mcg/kg/min & mg/kg/hr).", "Does not substitute clinical judgement for absolute maximum ceiling doses."),
            ClinicalToolModule("conversions", "Unit Conversions", "Mass, Volume & mEq", "• Metric & Household Equivalents\n• Electrolyte mEq to mg Engine\n• Instant Bi-Directional Translation", "🔄", Color(0xFF651FFF), Color(0xFF311B92), 350, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToConversions() }, listOf("unit", "conversion", "convert", "mass", "volume", "meq", "mg", "mcg", "grams", "micrograms", "milligrams", "liters"), listOf("හුවමාරුව", "ඒකක", "මිලිග්‍රෑම්", "මයික්‍රෝග්‍රෑම්", "පරිවර්තනය"), "Bi-Directional Metric, Mass, Volume & Electrolyte (mEq to mg) Conversions.", "Temperature or complex radiological unit conversions not included."),
            ClinicalToolModule("bsa", "BSA & Chemo", "Mosteller BSA (m²)", "• Chemotherapy Surface Area Dosing\n• High-Risk Pediatric Calculations\n• Height/Weight Nomogram Engine", "📏", Color(0xFFF50057), Color(0xFF880E4F), 400, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToBsa() }, listOf("bsa", "chemo", "chemotherapy", "mosteller", "surface area", "body surface area", "m2", "height", "height weight", "oncology"), listOf("පිළිකා", "කීමෝ", "ශරීර වර්ගඵලය", "උස බර", "chemo", "cancer", "bsa"), "Oncology Body Surface Area (BSA) using Mosteller & Chemotherapy Dosing.", "Does not calculate Cockcroft-Gault Creatinine Clearance for chemo limits."),
            ClinicalToolModule("pediatric", "Legacy Paediatric", "Clark, Young & Fried", "• Age & Weight Approximation Rules\n• Fraction of Adult Dose Calculation\n• Built-in Legacy Safety Limits", "🧒", Color(0xFFFF9100), Color(0xFFE65100), 450, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToPediatric() }, listOf("pediatric", "paediatric", "clark", "young", "fried", "child", "age", "infant", "baby", "child dose", "fraction of adult dose"), listOf("ළමා", "ළමුන්", "බබා", "ළමා මාත්‍රාව", "වයස", "බබාගේ බර", "lamaa"), "Pediatric Rule Approximations (Clark, Young, Fried).", "Modern standard recommends exact mg/kg dosing, not these historical age formulas.")
        )
    }

    // --- AI MATH LIMITATION & INTENT SCORER ---
    val searchIntentResult = remember(searchQuery, evaluationTrigger) {
        val trimmed = searchQuery.trim().lowercase()
        if (trimmed.isBlank()) return@remember Pair<ClinicalToolModule?, List<ClinicalToolModule>>(null, allTools)

        // 🚨 CRITICAL MEDICAL LIMITATION INTERCEPTOR 🚨
        val hasMultiSolutes = (trimmed.count { it == '%' } >= 2) && (trimmed.contains("mix") || trimmed.contains("using") || trimmed.contains("make") || trimmed.contains("and") || trimmed.contains("හා"))
        val isExplicitlyUnsupported = trimmed.contains("creatinine") || trimmed.contains("gfr") || trimmed.contains("anion gap") || trimmed.contains("parkland") || trimmed.contains("burn") || trimmed.contains("map") || trimmed.contains("arterial pressure") || trimmed.contains("sodium correction") || trimmed.contains("hyponatremia")

        if (hasMultiSolutes || isExplicitlyUnsupported) {
            return@remember Pair<ClinicalToolModule?, List<ClinicalToolModule>>(null, emptyList()) // Triggers Web Fallback
        }

        val tokens = trimmed.split(" ", ",", "-", "/", "%").filter { it.isNotBlank() }
        val scoredTools = allTools.map { tool ->
            var score = 0
            if (tool.title.lowercase().contains(trimmed)) score += 50
            if (tool.subtitle.lowercase().contains(trimmed)) score += 30

            for (token in tokens) {
                if (tool.keywords.any { it.equals(token, ignoreCase = true) }) score += 20
                else if (tool.keywords.any { it.contains(token, ignoreCase = true) }) score += 10
                if (tool.sinhalaKeywords.any { it.equals(token, ignoreCase = true) }) score += 25
                else if (tool.sinhalaKeywords.any { it.contains(token, ignoreCase = true) }) score += 12
            }

            // High-Confidence Nursing Context Overrides
            if (trimmed.contains("dextrose") || trimmed.contains("dilut") || trimmed.contains("powder") || trimmed.contains("කුඩු") || trimmed.contains("දියාරු")) if (tool.id == "dosage") score += 60
            if (trimmed.contains("arrest") || trimmed.contains("cpr") || trimmed.contains("adrenaline") || trimmed.contains("anaphylaxis") || trimmed.contains("shock") || trimmed.contains("හදිසි")) if (tool.id == "emergency") score += 60
            if (trimmed.contains("drip") || trimmed.contains("drop") || trimmed.contains("gtt") || trimmed.contains("බිංදු") || trimmed.contains("සේලයින්") || trimmed.contains("වතුර")) if (tool.id == "iv_drip") score += 60
            if (trimmed.contains("insulin") || trimmed.contains("sugar") || trimmed.contains("glucose") || trimmed.contains("heparin") || trimmed.contains("ඉන්සියුලින්") || trimmed.contains("සීනි")) if (tool.id == "special_calcs") score += 60
            if (trimmed.contains("noradrenalin") || trimmed.contains("inotrope") || trimmed.contains("icu") || trimmed.contains("potassium") || trimmed.contains("kcl") || trimmed.contains("පොටෑසියම්")) if (tool.id == "icu") score += 60
            if (trimmed.contains("chemo") || trimmed.contains("bsa") || trimmed.contains("surface") || trimmed.contains("mosteller") || trimmed.contains("පිළිකා") || trimmed.contains("කීමෝ")) if (tool.id == "bsa") score += 60
            if (trimmed.contains("convert") || trimmed.contains("mcg") || trimmed.contains("meq") || trimmed.contains("හුවමාරු")) if (tool.id == "conversions") score += 60
            if (trimmed.contains("child") || trimmed.contains("baby") || trimmed.contains("lamaa") || trimmed.contains("clark") || trimmed.contains("young") || trimmed.contains("ළමා")) if (tool.id == "pediatric") score += 60
            if (trimmed.contains("mcg/kg/min") || trimmed.contains("mg/kg") || trimmed.contains("rate") || trimmed.contains("බර අනුව")) if (tool.id == "weight_infusion") score += 60

            Pair(tool, score)
        }

        val bestMatch = scoredTools.filter { it.second >= 30 }.maxByOrNull { it.second }?.first
        val matchingList = scoredTools.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }

        Pair(bestMatch, matchingList)
    }

    val bestMatchTool = searchIntentResult.first
    val filteredTools = if (searchQuery.isBlank()) allTools else searchIntentResult.second

    fun executeSearch() {
        keyboardController?.hide()
        focusManager.clearFocus()
        saveSearchHistory(context, searchQuery)
        historyItems = getSearchHistory(context)
        evaluationTrigger++

        if (bestMatchTool != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            bestMatchTool.onClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Clinical Tools", fontWeight = FontWeight.Black, color = ToolsSlateDark, fontSize = 22.sp)
                        Text("Medical Math & AI Guidance", fontSize = 12.sp, color = TechBluePrimary, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ToolsSlateDark) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(ToolsBgWhite).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus() }) {

            // 🧠 MIND-BLOWING NEURAL AI WAVEFORM BACKGROUND 🧠
            AnimatedNeuralWaveBackground(isVisible)

            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(modifier = Modifier.height(4.dp))

                // 🌟 GLOWING AI SEARCH BAR 🌟
                CreativeAiSearchBar(query = searchQuery, onQueryChange = { searchQuery = it }, onSubmit = { executeSearch() }, onFocusChanged = { searchBarFocused = it })

                // 🌟 DROPDOWN SEARCH HISTORY 🌟
                AnimatedVisibility(
                    visible = searchBarFocused && searchQuery.isBlank() && historyItems.isNotEmpty(),
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).shadow(12.dp, RoundedCornerShape(16.dp), spotColor = AiPurple.copy(alpha = 0.3f)),
                        colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text("Recent AI Searches", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            historyItems.forEach { pastQuery ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { searchQuery = pastQuery; executeSearch() }.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.History, contentDescription = "History", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(pastQuery, fontSize = 14.sp, color = ToolsSlateDark, fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }

                // 🌟 HIGH-CONFIDENCE INTENT BADGE 🌟
                AnimatedVisibility(
                    visible = !searchBarFocused && searchQuery.isNotBlank() && bestMatchTool != null,
                    enter = slideInVertically(
                        initialOffsetY = { -20 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -20 }) + fadeOut()
                ) {
                    if (bestMatchTool != null) {
                        GlowingIntentBadge(tool = bestMatchTool, onLaunch = { executeSearch() })
                    }
                }

                if (!searchBarFocused) {
                    Text(
                        text = if (searchQuery.isBlank()) "Select a clinical engine:" else "Matching Calculators (${filteredTools.size}):",
                        fontSize = 15.sp, color = ToolsSlateLight, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )

                    // 🌟 3D HOLOGRAPHIC TOOL CARDS WITH AI EXPANSION 🌟
                    if (filteredTools.isNotEmpty()) {
                        filteredTools.forEach { tool ->
                            HolographicToolCard(
                                visible = isVisible,
                                tool = tool
                            )
                        }
                    } else {
                        // 🌟 SMART CLINICAL WEB SUPPORT FALLBACK 🌟
                        SmartWebCalculatorResolverCard(query = searchQuery) { targetUrl ->
                            saveSearchHistory(context, searchQuery)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            context.startActivity(intent)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun AnimatedNeuralWaveBackground(isVisible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase1 by infiniteTransition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "")
    val phase2 by infiniteTransition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "")
    val alphaAnim by animateFloatAsState(if (isVisible) 1f else 0f, tween(2000), label = "")

    Canvas(modifier = Modifier.fillMaxSize().alpha(alphaAnim)) {
        val w = size.width
        val h = size.height * 0.35f

        drawRect(Color(0xFFF8FAFC))

        for (i in 0..2) {
            val wavePath = Path()
            wavePath.moveTo(0f, h)

            for (x in 0..w.toInt() step 10) {
                val normalizedX = x / w
                val amplitude = h * 0.15f * (1f - normalizedX)
                val y = (h * 0.5f) + sin((normalizedX * 10f) + phase1 + (i * 2f)) * amplitude + cos((normalizedX * 5f) + phase2) * amplitude

                if (x == 0) wavePath.moveTo(x.toFloat(), y) else wavePath.lineTo(x.toFloat(), y)
            }

            val strokeColor = if (i == 0) AiCyan else if (i == 1) AiPurple else TechBluePrimary
            drawPath(
                path = wavePath,
                color = strokeColor.copy(alpha = 0.3f - (i * 0.1f)),
                style = Stroke(width = 8f + (i * 4f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun CreativeAiSearchBar(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit, onFocusChanged: (Boolean) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "sweep")
    val angle by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = AiPurple.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .drawBehind {
                rotate(angle) {
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(TechBluePrimary, AiPurple, AiCyan, TechBluePrimary)),
                        radius = size.width,
                        blendMode = BlendMode.SrcIn
                    )
                }
            }
            .padding(2.dp)
            .background(Color.White, RoundedCornerShape(22.dp))
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
            placeholder = { Text("Ask AI: '10% dextrose mix' or 'ළමා මාත්‍රාව'", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = AiPurple, modifier = Modifier.size(24.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray) }
                } else {
                    Icon(Icons.Default.Psychology, tint = TechBluePrimary.copy(alpha = 0.7f), contentDescription = "NLP Analyzer")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
            ),
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ToolsSlateDark)
        )
    }
}

@Composable
fun HolographicToolCard(
    visible: Boolean,
    tool: ClinicalToolModule
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var isExpanded by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "")
    val shadowAlpha by animateFloatAsState(if (isPressed) 0.2f else 0.5f, spring(), label = "")

    val infiniteTransition = rememberInfiniteTransition(label = "CardFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "FloatY"
    )

    val dashPhase by infiniteTransition.animateFloat(0f, 100f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "")

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { 150 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeIn(tween(600, delayMillis = tool.delay))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = floatY.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .zIndex(if (isExpanded || isPressed) 10f else 0f)
                .shadow(elevation = if (isExpanded) 20.dp else if (isPressed) 8.dp else 16.dp, shape = RoundedCornerShape(24.dp), spotColor = tool.colorStart.copy(alpha = shadowAlpha))
                .clickable(interactionSource = interactionSource, indication = null, onClick = tool.onClick)
                .border(1.dp, tool.colorStart.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color.White, tool.colorStart.copy(alpha = 0.05f)))).padding(20.dp)) {

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(64.dp).drawWithContent {
                            drawRoundRect(
                                brush = Brush.linearGradient(listOf(tool.colorStart, tool.colorEnd)),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(32f, 32f),
                                style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), dashPhase))
                            )
                            drawContent()
                        }.background(tool.colorStart.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tool.emoji, fontSize = 28.sp, modifier = Modifier.scale(if(isPressed) 0.8f else 1f))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = tool.title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = ToolsSlateDark, letterSpacing = (-0.5).sp)
                        Text(text = tool.subtitle, fontSize = 13.sp, color = tool.colorStart, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = tool.description, fontSize = 13.sp, color = ToolsSlateLight, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tool.colorStart.copy(alpha = 0.1f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isExpanded = !isExpanded
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = "AI", tint = tool.colorStart, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View AI Engine Specs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tool.colorStart)
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = tool.colorStart
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text("✅ CAPABILITIES", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32), letterSpacing = 1.sp)
                        Text(tool.capabilities, fontSize = 13.sp, color = ToolsSlateDark, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                        Text("❌ LIMITATIONS (Triggers Web Resolver)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFD32F2F), letterSpacing = 1.sp)
                        Text(tool.limitations, fontSize = 13.sp, color = ToolsSlateDark, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun GlowingIntentBadge(tool: ClinicalToolModule, onLaunch: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val pulse by infiniteTransition.animateFloat(0.8f, 1f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp * pulse, RoundedCornerShape(20.dp), spotColor = tool.colorStart).clip(RoundedCornerShape(20.dp)).clickable { onLaunch() },
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0F172A), tool.colorStart.copy(alpha = 0.35f)))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).background(tool.colorStart, CircleShape).scale(pulse), contentAlignment = Alignment.Center) { Text(tool.emoji, fontSize = 22.sp) }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI MATCH FOUND", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = tool.colorStart, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.background(tool.colorStart.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Auto-Route Ready", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(tool.title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Solves: ${tool.capabilities}", fontSize = 11.sp, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onLaunch, modifier = Modifier.background(tool.colorStart, CircleShape).size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SmartWebCalculatorResolverCard(query: String, onOpenUrl: (String) -> Unit) {
    val cleanQuery = query.trim()
    val encodedQuery = Uri.encode(cleanQuery)
    val isAlligation = cleanQuery.contains("%") && (cleanQuery.contains("mix") || cleanQuery.contains("using") || cleanQuery.contains("හා"))

    val webSources = listOf(
        WebCalculatorSource("MDCalc Medical Engine", "Evidence-Based Clinical Decision Rules", "https://www.mdcalc.com/calc?search=$encodedQuery", Color(0xFF00ACC1)),
        WebCalculatorSource("Medscape Calculator Hub", "Formulas, Critical Care & Dosing", "https://reference.medscape.com/search?q=$encodedQuery+calculator", Color(0xFF1E88E5)),
        WebCalculatorSource("ClinCalc Online Suite", "Clinical Pharmacokinetics & Equations", "https://clincalc.com/?s=$encodedQuery", Color(0xFF43A047)),
        WebCalculatorSource("NCBI / NIH Guidelines", "Peer-Reviewed Medical Literature", "https://pubmed.ncbi.nlm.nih.gov/?term=$encodedQuery+calculation+formula", Color(0xFF8E24AA))
    )

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(Color(0xFF0F172A), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TravelExplore, contentDescription = "Web Search", tint = AiCyan, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(if (isAlligation) "Complex Admixture Detected" else "Clinical Web Resolver", fontSize = 18.sp, fontWeight = FontWeight.Black, color = ToolsSlateDark)
                    Text(if (isAlligation) "Requires Pearson's Square (Alligation)" else "No exact offline match for: \"$cleanQuery\"", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                if (isAlligation) "Mixing two different active concentrations (e.g. 5% and 50%) is not a standard dilution (C1V1). Our AI has found the correct 'Alligation' web calculators for you:"
                else "Our AI has generated verified clinical links to perform this calculation directly online:",
                fontSize = 13.sp, color = ToolsSlateLight, lineHeight = 18.sp, fontWeight = FontWeight.Medium
            )

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                webSources.forEach { source ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF8FAFC)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)).clickable { onOpenUrl(source.url) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(source.badgeColor, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(source.name, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = ToolsSlateDark)
                            }
                            Text(source.subtitle, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 16.dp, top = 2.dp))
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Link", tint = TechBluePrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Button(
                onClick = {
                    val searchString = if (isAlligation) "$cleanQuery alligation pearson square calculator nursing" else "$cleanQuery medical clinical calculation formula guidelines"
                    onOpenUrl("https://www.google.com/search?q=" + Uri.encode(searchString))
                },
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Google Medical Guidelines", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}