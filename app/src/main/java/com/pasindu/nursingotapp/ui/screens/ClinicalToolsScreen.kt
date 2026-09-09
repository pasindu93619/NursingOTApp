package com.pasindu.nursingotapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay

private val ToolsBgWhite = Color(0xFFF8FAFC)
private val ToolsInk = Color(0xFF12204A)
private val ToolsSlateLight = Color(0xFF64748B)
private val ToolsBlue = Color(0xFF1769E8)
private val ToolsCyan = Color(0xFF14A6E0)
private val ToolsPurple = Color(0xFF7257E8)
private val ToolsBlueSoft = Color(0xFFEAF6FF)
private val ToolsPurpleSoft = Color(0xFFF3EEFF)
private val ToolsMintSoft = Color(0xFFEAFBF5)
private val ToolsAmberSoft = Color(0xFFFFF6E7)
private val ToolsRedSoft = Color(0xFFFFEEEE)

private val NursingHeroGradient = Brush.horizontalGradient(
    listOf(Color(0xFF1769E8), Color(0xFF149FE3), Color(0xFF4B78F2), Color(0xFF7B5CEB))
)

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
    onNavigateToIvDrip: () -> Unit,
    onNavigateToDosage: () -> Unit,
    onNavigateToWeightInfusion: () -> Unit,
    onNavigateToBsa: () -> Unit,
    onNavigateToPediatric: () -> Unit,
    onNavigateToConversions: () -> Unit,
    onNavigateToSpecialCalcs: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToIcu: () -> Unit,
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

    val allTools = remember {
        listOf(
            ClinicalToolModule("emergency", "Crash Cart Engine", "Cardiac Arrest, Anaphylaxis, RSI", "• Instant Parallel Processing\n• Weight-Based Resuscitation\n• Code Red Animated ECG", "🚨", Color(0xFFFF1744), Color(0xFFD50000), 50, { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToEmergency() }, listOf("crash", "cart", "cardiac", "arrest", "anaphylaxis", "rsi", "emergency", "resuscitation", "cpr", "adrenaline", "epinephrine", "shock", "defib", "vtach", "vfib", "asystole"), listOf("හෘදයාබාධ", "ඇඩ්‍රිනලින්", "හදිසි", "ශොක්", "ඇනෆිලැක්සිස්", "cpr", "arrest", "hadisi"), "ACLS Protocols, Rapid Push Adrenaline/Amiodarone derivations.", "Cannot calculate custom continuous infusions outside of the crash cart rapid push protocol."),
            ClinicalToolModule("icu", "ICU Critical Care", "Vasoactive, Sedation & Fluids", "• Inotrope Dose-Rate (μg/kg/min)\n• Electrolyte Repletion (K+, Mg++)\n• TPN & Pharmacokinetics", "🫀", Color(0xFF2979FF), Color(0xFF0D47A1), 100, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIcu() }, listOf("icu", "critical", "vasoactive", "sedation", "fluid", "infusion", "electrolyte", "potassium", "kcl", "noradrenaline", "norepinephrine", "dopamine", "dobutamine", "inotrope", "magnesium"), listOf("අයිසීයූ", "පොටෑසියම්", "නොරැඩ්‍රිනලින්", "ඩොපමයින්", "ඉලෙක්ට්‍රොලයිට්"), "Inotropes, Syringe Pumps, Analgesia, Sedation & Electrolyte Balancing.", "Does not calculate anion gap, ABG interpretation, or MAP calculations."),
            ClinicalToolModule("dosage", "Advanced Dosage", "5-in-1 Math Engine", "• Standard Liquid & % Solutions\n• Dilutions (C₁V₁=C₂V₂)\n• Powder Reconstitution", "💊", Color(0xFF7257E8), Color(0xFF4A148C), 150, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToDosage() }, listOf("dosage", "math", "dilution", "reconstitution", "powder", "dextrose", "concentration", "liquid", "c1v1", "c2v2", "ampoule", "vial", "diluent", "antibiotic", "ceftriaxone"), listOf("ඩෙක්ස්ට්‍රෝස්", "දියාරු", "කුඩු", "මාත්‍රාව", "බෙහෙත්", "ප්‍රතිශතය", "ද්‍රාවණය"), "Dextrose Dilutions (Active + Zero % Diluent), Reconstitution & Liquid Dosage (D/H*Q).", "CANNOT mix two active percentages together (e.g. 10% + 50%). Cannot do Alligation Math."),
            ClinicalToolModule("iv_drip", "IV Drip Sync", "Drops Per Minute & AR", "• Macro (10/15/20) & Micro (60) Sets\n• Live AR Hologram Synchronization\n• 15-Second Clinical Tap Verification", "💧", Color(0xFF14A6E0), Color(0xFF006064), 200, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIvDrip() }, listOf("iv", "drip", "sync", "drops", "minute", "macro", "micro", "flow rate", "drop factor", "gtt", "saline", "normal saline", "dpm", "chamber", "gravity set"), listOf("ඩ්‍රිප්", "බිංදු", "පැයට බිංදු", "සේලයින්", "වතුර"), "Gravity Drip Calibration (Drops/min) & Visual Holographic Sync.", "Does not control physical electronic hardware syringe pumps via Bluetooth."),
            ClinicalToolModule("special_calcs", "High-Alert Specials", "Insulin, Heparin, PCA", "• Sliding Scale & IV Insulin\n• Heparin Weight-Based Protocols\n• Opioid PCA Lockout Limits", "🩸", Color(0xFFE53935), Color(0xFFB71C1C), 250, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToSpecialCalcs() }, listOf("high alert", "insulin", "heparin", "pca", "opioid", "sliding scale", "actrapid", "blood sugar", "glucose", "cbg", "dka", "aptt"), listOf("ඉන්සියුලින්", "හෙපරින්", "සීනි", "ග්ලූකෝස්", "රුධිර සීනි"), "Continuous IV Insulin Protocols & Heparin Nomograms.", "Does not adjust algorithm dynamically for acute renal impairment."),
            ClinicalToolModule("weight_infusion", "Weight & Infusions", "mg/kg & mcg/kg/min", "• Pediatric Simple Dosing\n• Complex Continuous Infusion Rates\n• Built-in Safety & Weight Alerts", "⚖️", Color(0xFF0E9F73), Color(0xFF004D40), 300, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToWeightInfusion() }, listOf("weight", "infusion", "mg/kg", "mcg/kg/min", "continuous", "rate", "body weight", "milligrams per kilogram"), listOf("බර", "බර අනුව", "ශරීර බර", "කිලෝග්‍රෑම්", "bara", "weight"), "Weight-Based Continuous Drug Infusion Rates (mcg/kg/min & mg/kg/hr).", "Does not substitute clinical judgement for absolute maximum ceiling doses."),
            ClinicalToolModule("conversions", "Unit Conversions", "Mass, Volume & mEq", "• Metric & Household Equivalents\n• Electrolyte mEq to mg Engine\n• Instant Bi-Directional Translation", "🔄", Color(0xFF651FFF), Color(0xFF311B92), 350, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToConversions() }, listOf("unit", "conversion", "convert", "mass", "volume", "meq", "mg", "mcg", "grams", "micrograms", "milligrams", "liters"), listOf("හුවමාරුව", "ඒකක", "මිලිග්‍රෑම්", "මයික්‍රෝග්‍රෑම්", "පරිවර්තනය"), "Bi-Directional Metric, Mass, Volume & Electrolyte (mEq to mg) Conversions.", "Temperature or complex radiological unit conversions not included."),
            ClinicalToolModule("bsa", "BSA & Chemo", "Mosteller BSA (m²)", "• Chemotherapy Surface Area Dosing\n• High-Risk Pediatric Calculations\n• Height/Weight Nomogram Engine", "📏", Color(0xFFF50057), Color(0xFF880E4F), 400, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToBsa() }, listOf("bsa", "chemo", "chemotherapy", "mosteller", "surface area", "body surface area", "m2", "height", "height weight", "oncology"), listOf("පිළිකා", "කීමෝ", "ශරීර වර්ගඵලය", "උස බර", "chemo", "cancer", "bsa"), "Oncology Body Surface Area (BSA) using Mosteller & Chemotherapy Dosing.", "Does not calculate Cockcroft-Gault Creatinine Clearance for chemo limits."),
            ClinicalToolModule("pediatric", "Legacy Paediatric", "Clark, Young & Fried", "• Age & Weight Approximation Rules\n• Fraction of Adult Dose Calculation\n• Built-in Legacy Safety Limits", "🧒", Color(0xFFFF9100), Color(0xFFE65100), 450, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToPediatric() }, listOf("pediatric", "paediatric", "clark", "young", "fried", "child", "age", "infant", "baby", "child dose", "fraction of adult dose"), listOf("ළමා", "ළමුන්", "බබා", "ළමා මාත්‍රාව", "වයස", "බබාගේ බර", "lamaa"), "Pediatric Rule Approximations (Clark, Young, Fried).", "Modern standard recommends exact mg/kg dosing, not these historical age formulas.")
        )
    }

    val searchIntentResult = remember(searchQuery, evaluationTrigger) {
        val trimmed = searchQuery.trim().lowercase()
        if (trimmed.isBlank()) return@remember Pair<ClinicalToolModule?, List<ClinicalToolModule>>(null, allTools)
        val hasMultiSolutes = (trimmed.count { it == '%' } >= 2) && (trimmed.contains("mix") || trimmed.contains("using") || trimmed.contains("make") || trimmed.contains("and") || trimmed.contains("හා"))
        val isExplicitlyUnsupported = trimmed.contains("creatinine") || trimmed.contains("gfr") || trimmed.contains("anion gap") || trimmed.contains("parkland") || trimmed.contains("burn") || trimmed.contains("map") || trimmed.contains("arterial pressure") || trimmed.contains("sodium correction") || trimmed.contains("hyponatremia")
        if (hasMultiSolutes || isExplicitlyUnsupported) return@remember Pair<ClinicalToolModule?, List<ClinicalToolModule>>(null, emptyList())
        val tokens = trimmed.split(" ", ",", "-", "/", "%").filter { it.isNotBlank() }
        val scoredTools = allTools.map { tool ->
            var score = 0
            if (tool.title.lowercase().contains(trimmed)) score += 50
            if (tool.subtitle.lowercase().contains(trimmed)) score += 30
            tokens.forEach { token ->
                if (tool.keywords.any { it.equals(token, ignoreCase = true) }) score += 20 else if (tool.keywords.any { it.contains(token, ignoreCase = true) }) score += 10
                if (tool.sinhalaKeywords.any { it.equals(token, ignoreCase = true) }) score += 25 else if (tool.sinhalaKeywords.any { it.contains(token, ignoreCase = true) }) score += 12
            }
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
        keyboardController?.hide(); focusManager.clearFocus(); saveSearchHistory(context, searchQuery); historyItems = getSearchHistory(context); evaluationTrigger++
        if (bestMatchTool != null) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); bestMatchTool.onClick() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Clinical Tools", fontWeight = FontWeight.Black, color = ToolsInk, fontSize = 22.sp)
                        Text("Fast, verified bedside calculations", fontSize = 12.sp, color = ToolsCyan, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ToolsInk) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(Modifier.fillMaxSize().background(ToolsBgWhite).verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(4.dp))

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(3.dp)) {
                Box(Modifier.fillMaxWidth().background(NursingHeroGradient, RoundedCornerShape(28.dp)).padding(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text("CLINICAL WORKSPACE", color = Color.White.copy(alpha = .74f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                                Text("Calculate with confidence", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                                Text("Essential bedside math in one focused workspace", color = Color.White.copy(alpha = .84f), fontSize = 11.sp)
                            }
                            Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) { Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color.White, modifier = Modifier.padding(11.dp)) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ClinicalHeroStat(Modifier.weight(1f), "ENGINES", "8")
                            ClinicalHeroStat(Modifier.weight(1f), "OFFLINE", "READY")
                            ClinicalHeroStat(Modifier.weight(1f), "LANGUAGE", "EN + සිං")
                        }
                    }
                }
            }

            ClinicalSafetyBanner()

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = ToolsPurpleSoft, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.AutoAwesome, null, tint = ToolsPurple, modifier = Modifier.padding(8.dp)) }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Find the right calculator", color = ToolsInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Type in English or Sinhala", color = ToolsSlateLight, fontSize = 10.sp)
                        }
                    }
                    ClinicalSearchField(searchQuery, { searchQuery = it }, { executeSearch() }, { searchBarFocused = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("IV drip", "Dosage", "ICU", "Emergency").forEach { chip ->
                            Surface(onClick = { searchQuery = chip; executeSearch() }, shape = RoundedCornerShape(50.dp), color = ToolsBlueSoft, tonalElevation = 0.dp) {
                                Text(chip, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = ToolsBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    AnimatedVisibility(searchBarFocused && searchQuery.isBlank() && historyItems.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        Column {
                            Text("Recent searches", color = ToolsSlateLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            historyItems.forEach { pastQuery ->
                                Row(Modifier.fillMaxWidth().clickable { searchQuery = pastQuery; executeSearch() }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.History, null, tint = ToolsSlateLight, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(pastQuery, color = ToolsInk, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    if (!searchBarFocused && searchQuery.isNotBlank() && bestMatchTool != null) {
                        Spacer(Modifier.height(2.dp))
                        GlowingIntentBadge(bestMatchTool) { executeSearch() }
                    }
                }
            }

            Text(if (searchQuery.isBlank()) "Clinical engines" else "Matching calculators (${filteredTools.size})", color = ToolsSlateLight, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 3.dp))
            filteredTools.forEach { tool -> ClinicalToolCard(isVisible, tool) }
            if (filteredTools.isEmpty()) SmartWebCalculatorResolverCard(searchQuery) { targetUrl -> saveSearchHistory(context, searchQuery); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ClinicalHeroStat(modifier: Modifier, title: String, value: String) {
    Surface(modifier, color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(title, color = Color.White.copy(alpha = .72f), fontSize = 7.sp, fontWeight = FontWeight.Black)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ClinicalSafetyBanner() {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = ToolsAmberSoft), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .75f)) { Text("!", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Color(0xFF9A6700), fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Clinical check", color = ToolsInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text("Confirm the order, concentration, patient weight and local protocol before administration.", color = ToolsSlateLight, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun ClinicalSearchField(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit, onFocusChanged: (Boolean) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
        placeholder = { Text("Ask: IV drip, insulin, ICU, ළමා මාත්‍රාව...", fontSize = 12.sp, color = ToolsSlateLight, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Default.Psychology, null, tint = ToolsPurple) },
        trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null, tint = ToolsSlateLight) } else Icon(Icons.Default.TravelExplore, null, tint = ToolsBlue) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        singleLine = true,
        shape = RoundedCornerShape(17.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = ToolsCyan, unfocusedBorderColor = Color(0xFFE2E8F0)),
        textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ToolsInk)
    )
}

@Composable
private fun ClinicalToolCard(visible: Boolean, tool: ClinicalToolModule) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isExpanded by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.985f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "clinical_card_scale")
    AnimatedVisibility(visible = visible, enter = slideInVertically(initialOffsetY = { 80 }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) {
        Card(
            Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interactionSource, indication = null, onClick = tool.onClick).border(1.dp, tool.colorStart.copy(alpha = .18f), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color.White, tool.colorStart.copy(alpha = .04f)))).padding(17.dp).animateContentSize()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(tool.colorStart.copy(alpha = .11f)), contentAlignment = Alignment.Center) { Text(tool.emoji, fontSize = 25.sp) }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tool.title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = ToolsInk)
                        Text(tool.subtitle, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = tool.colorStart)
                        Spacer(Modifier.height(5.dp))
                        Text(tool.description, color = ToolsSlateLight, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    Surface(color = tool.colorStart, shape = CircleShape) {
                        Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(44.dp).clickable { isExpanded = !isExpanded }.padding(7.dp))
                    }
                }
                AnimatedVisibility(isExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = ToolsMintSoft, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(12.dp)) { Text("CAPABILITIES", color = Color(0xFF087F5B), fontSize = 9.sp, fontWeight = FontWeight.Black); Text(tool.capabilities, color = ToolsInk, fontSize = 10.sp) } }
                        Surface(color = ToolsRedSoft, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(12.dp)) { Text("LIMITATION", color = Color(0xFFC62828), fontSize = 9.sp, fontWeight = FontWeight.Black); Text(tool.limitations, color = ToolsInk, fontSize = 10.sp) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlowingIntentBadge(tool: ClinicalToolModule, onLaunch: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLaunch),
        shape = RoundedCornerShape(16.dp),
        color = ToolsBlueSoft
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tool.colorStart.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(tool.emoji, fontSize = 18.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Best match", color = ToolsSlateLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(tool.title, color = ToolsInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("OPEN", color = ToolsBlue, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SmartWebCalculatorResolverCard(query: String, onOpen: (String) -> Unit) {
    val sources = listOf(
        WebCalculatorSource("MDCalc", "Medical calculators", "https://www.mdcalc.com/", ToolsBlue),
        WebCalculatorSource("Medscape", "Clinical reference", "https://reference.medscape.com/", ToolsPurple),
        WebCalculatorSource("ClinCalc", "Clinical calculator library", "https://clincalc.com/", ToolsCyan),
        WebCalculatorSource("PubMed", "Evidence search", "https://pubmed.ncbi.nlm.nih.gov/", Color(0xFF0E9F73))
    )
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = ToolsBlueSoft, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.TravelExplore, null, tint = ToolsBlue, modifier = Modifier.padding(8.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text("No verified in-app engine found", color = ToolsInk, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp); Text("Search for: $query", color = ToolsSlateLight, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Text("For unsupported calculations, use an external clinical reference rather than guessing.", color = ToolsSlateLight, fontSize = 10.sp, lineHeight = 15.sp)
            sources.forEach { source ->
                Surface(onClick = { onOpen(source.url) }, color = source.badgeColor.copy(alpha = .08f), shape = RoundedCornerShape(13.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(8.dp), CircleShape, source.badgeColor) {}
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(source.name, color = ToolsInk, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(source.subtitle, color = ToolsSlateLight, fontSize = 9.sp) }
                        Icon(Icons.Default.Language, null, tint = source.badgeColor, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}
