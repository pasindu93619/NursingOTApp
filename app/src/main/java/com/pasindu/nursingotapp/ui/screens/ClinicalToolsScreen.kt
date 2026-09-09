package com.pasindu.nursingotapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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

private val ClinicalBg = Color(0xFFF8FAFC)
private val ClinicalInk = Color(0xFF12204A)
private val ClinicalSecondary = Color(0xFF5B6B82)
private val ClinicalBlue = Color(0xFF1769E8)
private val ClinicalCyan = Color(0xFF14A6E0)
private val ClinicalPurple = Color(0xFF7257E8)
private val ClinicalMint = Color(0xFFEAFBF5)
private val ClinicalBlueSoft = Color(0xFFEAF6FF)
private val ClinicalPurpleSoft = Color(0xFFF3EEFF)
private val ClinicalAmberSoft = Color(0xFFFFF6E7)
private val ClinicalAmber = Color(0xFFF59E0B)
private val ClinicalRedSoft = Color(0xFFFFF0F1)
private val ClinicalRed = Color(0xFFE5484D)

private data class ClinicalToolModule(
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

private data class WebCalculatorSource(val name: String, val subtitle: String, val url: String, val badgeColor: Color)

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

    LaunchedEffect(Unit) { delay(120); isVisible = true }

    val allTools = remember {
        listOf(
            ClinicalToolModule("emergency", "Crash Cart Engine", "Cardiac arrest • Anaphylaxis • RSI", "Rapid emergency calculations with protocol-focused support.", "🚨", ClinicalRed, Color(0xFFC62828), 40, { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToEmergency() }, listOf("crash", "cart", "cardiac", "arrest", "anaphylaxis", "rsi", "emergency", "resuscitation", "cpr", "adrenaline", "epinephrine", "shock", "defib", "vtach", "vfib", "asystole"), listOf("හෘදයාබාධ", "ඇඩ්‍රිනලින්", "හදිසි", "ශොක්", "ඇනෆිලැක්සිස්", "cpr", "arrest", "hadisi"), "ACLS-oriented rapid calculations and resuscitation support.", "Does not replace local emergency protocols or clinician judgement."),
            ClinicalToolModule("icu", "ICU Critical Care", "Vasoactive • Sedation • Fluids", "Weight-based critical-care calculations in one focused workspace.", "🫀", ClinicalBlue, Color(0xFF0D47A1), 80, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIcu() }, listOf("icu", "critical", "vasoactive", "sedation", "fluid", "infusion", "electrolyte", "potassium", "kcl", "noradrenaline", "norepinephrine", "dopamine", "dobutamine", "inotrope", "magnesium"), listOf("අයිසීයූ", "පොටෑසියම්", "නොරැඩ්‍රිනලින්", "ඩොපමීන්", "ඉලෙක්ට්‍රොලයිට්"), "Inotropes, syringe pumps, analgesia, sedation and electrolyte support.", "Does not calculate anion gap, ABG interpretation, or MAP."),
            ClinicalToolModule("dosage", "Advanced Dosage", "Liquid • Dilution • Reconstitution", "Five-in-one medication-math workspace for common bedside calculations.", "💊", ClinicalPurple, Color(0xFF4A148C), 120, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToDosage() }, listOf("dosage", "math", "dilution", "reconstitution", "powder", "dextrose", "concentration", "liquid", "c1v1", "c2v2", "ampoule", "vial", "diluent", "antibiotic", "ceftriaxone"), listOf("ඩෙක්ස්ට්‍රෝස්", "දියාරු", "කුඩු", "මාත්‍රාව", "බෙහෙත්", "ප්‍රතිශතය", "ද්‍රාවණය"), "Liquid dosage, dilution and powder reconstitution support.", "Does not mix two active percentages or perform alligation math."),
            ClinicalToolModule("iv_drip", "IV Drip Sync", "Drops / min • Gravity sets", "Quick drip-rate support for common macro and micro sets.", "💧", ClinicalCyan, Color(0xFF006064), 160, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIvDrip() }, listOf("iv", "drip", "sync", "drops", "minute", "macro", "micro", "flow rate", "drop factor", "gtt", "saline", "normal saline", "dpm", "chamber", "gravity set"), listOf("ඩ්‍රිප්", "බිංදු", "පැයට බිංදු", "සේලයින්", "වතුර"), "Gravity drip calculations and visual synchronisation.", "Does not control physical syringe pumps by Bluetooth."),
            ClinicalToolModule("special_calcs", "High-Alert Specials", "Insulin • Heparin • PCA", "Focused tools for medications where calculation accuracy matters most.", "🩸", ClinicalRed, Color(0xFFB71C1C), 200, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToSpecialCalcs() }, listOf("high alert", "insulin", "heparin", "pca", "opioid", "sliding scale", "actrapid", "blood sugar", "glucose", "cbg", "dka", "aptt"), listOf("ඉන්සියුලින්", "හෙපරින්", "සීනි", "ග්ලූකෝස්", "රුධිර සීනි"), "Insulin, heparin and PCA calculation support.", "Does not dynamically adjust for acute renal impairment."),
            ClinicalToolModule("weight_infusion", "Weight & Infusions", "mg/kg • mcg/kg/min", "Convert weight-based orders into practical infusion rates.", "⚖️", Color(0xFF00BFA5), Color(0xFF004D40), 240, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToWeightInfusion() }, listOf("weight", "infusion", "mg/kg", "mcg/kg/min", "continuous", "rate", "body weight", "milligrams per kilogram"), listOf("බර", "බර අනුව", "ශරීර බර", "කිලෝග්‍රෑම්", "bara", "weight"), "Weight-based continuous infusion rate calculations.", "Does not replace maximum-dose or patient-specific clinical checks."),
            ClinicalToolModule("conversions", "Unit Conversions", "Mass • Volume • mEq", "Fast bidirectional conversions without leaving the clinical workspace.", "🔄", ClinicalPurple, Color(0xFF311B92), 280, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToConversions() }, listOf("unit", "conversion", "convert", "mass", "volume", "meq", "mg", "mcg", "grams", "micrograms", "milligrams", "liters"), listOf("හුවමාරුව", "ඒකක", "මිලිග්‍රෑම්", "මයික්‍රෝග්‍රෑම්", "පරිවර්තනය"), "Metric, mass, volume and electrolyte conversions.", "Complex radiological unit conversions are not included."),
            ClinicalToolModule("bsa", "BSA & Chemo", "Mosteller BSA • m²", "Height and weight based body-surface-area support for oncology workflows.", "📏", Color(0xFFF50057), Color(0xFF880E4F), 320, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToBsa() }, listOf("bsa", "chemo", "chemotherapy", "mosteller", "surface area", "body surface area", "m2", "height", "height weight", "oncology"), listOf("පිළිකා", "කීමෝ", "ශරීර වර්ගඵලය", "උස බර", "chemo", "cancer", "bsa"), "Mosteller BSA and chemotherapy support.", "Does not calculate Cockcroft-Gault creatinine clearance."),
            ClinicalToolModule("pediatric", "Paediatric Rules", "Clark • Young • Fried", "Legacy age and weight approximations presented with their limitations.", "🧒", ClinicalAmber, Color(0xFFE65100), 360, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToPediatric() }, listOf("pediatric", "paediatric", "clark", "young", "fried", "child", "age", "infant", "baby", "child dose", "fraction of adult dose"), listOf("ළමා", "ළමුන්", "බබා", "ළමා මාත්‍රාව", "වයස", "බබාගේ බර", "lamaa"), "Clark, Young and Fried approximations.", "Historical approximations; exact mg/kg dosing may be preferred clinically.")
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
            for (token in tokens) {
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
        containerColor = ClinicalBg,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Column {
                        Text("Clinical Tools", color = ClinicalInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Verified bedside calculations", color = ClinicalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ClinicalInk) } },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ClinicalHeroHeader(toolCount = allTools.size)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ClinicalSearchCard(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSubmit = ::executeSearch,
                    focused = searchBarFocused,
                    onFocusChanged = { searchBarFocused = it }
                )

                AnimatedVisibility(
                    visible = searchBarFocused && searchQuery.isBlank() && historyItems.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text("Recent searches", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = ClinicalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            historyItems.forEach { pastQuery ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { searchQuery = pastQuery; executeSearch() }.padding(horizontal = 16.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.History, null, tint = ClinicalBlue, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(pastQuery, color = ClinicalInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                if (!searchBarFocused && searchQuery.isNotBlank() && bestMatchTool != null) {
                    ClinicalIntentCard(bestMatchTool) { executeSearch() }
                }

                ClinicalQuickActions(
                    emergency = { onNavigateToEmergency() },
                    dosage = { onNavigateToDosage() },
                    drip = { onNavigateToIvDrip() }
                )

                ClinicalSectionHeader(
                    title = if (searchQuery.isBlank()) "Clinical engines" else "Matching engines (${filteredTools.size})",
                    subtitle = "Select the calculation workspace you need"
                )

                if (filteredTools.isNotEmpty()) {
                    filteredTools.forEach { tool ->
                        ClinicalToolCard(visible = isVisible, tool = tool)
                    }
                } else {
                    SmartWebCalculatorResolverCard(query = searchQuery) { targetUrl ->
                        saveSearchHistory(context, searchQuery)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                    }
                }

                ClinicalSafetyCard()
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ClinicalHeroHeader(toolCount: Int) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(Color(0xFF17233F), Color(0xFF24538C), Color(0xFF08A5D9))),
                RoundedCornerShape(28.dp)
            ).padding(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(62.dp), CircleShape, Color.White.copy(alpha = .15f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MedicalServices, null, tint = Color.White, modifier = Modifier.size(31.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("CLINICAL WORKSPACE", color = Color.White.copy(alpha = .72f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("Calculate with confidence", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text("${toolCount} focused engines • offline-first support", color = Color.White.copy(alpha = .84f), fontSize = 11.sp)
                }
            }
            Row(Modifier.align(Alignment.BottomEnd).padding(top = 76.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ClinicalHeroPill("OFFLINE", ClinicalMint)
                ClinicalHeroPill("NURSE-FIRST", ClinicalBlueSoft)
            }
        }
    }
}

@Composable
private fun ClinicalHeroPill(text: String, surface: Color) {
    Surface(shape = RoundedCornerShape(50.dp), color = surface.copy(alpha = .16f)) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ClinicalSearchCard(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit, focused: Boolean, onFocusChanged: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(42.dp), CircleShape, ClinicalPurpleSoft) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Psychology, null, tint = ClinicalPurple, modifier = Modifier.size(23.dp)) }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Find a calculator", color = ClinicalInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("English or Sinhala • smart routing only", color = ClinicalSecondary, fontSize = 10.sp)
                }
                Surface(shape = RoundedCornerShape(50.dp), color = ClinicalBlueSoft) {
                    Text("AI ROUTER", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = ClinicalBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
                placeholder = { Text("Try: IV drip, insulin, ICU, ළමා මාත්‍රාව…", color = ClinicalSecondary, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = ClinicalPurple) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null, tint = ClinicalSecondary) } else Icon(Icons.Default.Calculate, null, tint = ClinicalBlue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ClinicalBlue, unfocusedBorderColor = Color(0xFFD9E1EC), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ClinicalInk)
            )
        }
    }
}

@Composable
private fun ClinicalQuickActions(emergency: () -> Unit, dosage: () -> Unit, drip: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ClinicalSectionHeader("Quick clinical actions", "Jump straight into the tools you use most")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClinicalQuickAction("Emergency", Icons.Default.Emergency, ClinicalRedSoft, ClinicalRed, emergency, Modifier.weight(1f))
            ClinicalQuickAction("Dosage", Icons.Default.Calculate, ClinicalPurpleSoft, ClinicalPurple, dosage, Modifier.weight(1f))
            ClinicalQuickAction("IV drip", Icons.Default.WaterDrop, ClinicalBlueSoft, ClinicalBlue, drip, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ClinicalQuickAction(title: String, icon: ImageVector, background: Color, accent: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.height(86.dp), shape = RoundedCornerShape(19.dp), color = background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(5.dp))
            Text(title, color = ClinicalInk, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ClinicalSectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, color = ClinicalInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = ClinicalSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun ClinicalIntentCard(tool: ClinicalToolModule, onLaunch: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ClinicalInk), elevation = CardDefaults.cardElevation(3.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onLaunch)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp), CircleShape, tool.colorStart) { Box(contentAlignment = Alignment.Center) { Text(tool.emoji, fontSize = 22.sp) } }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("BEST MATCH", color = ClinicalCyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(tool.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(tool.subtitle, color = Color.White.copy(alpha = .72f), fontSize = 10.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ClinicalToolCard(visible: Boolean, tool: ClinicalToolModule) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var expanded by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) .985f else 1f, tween(120, easing = FastOutSlowInEasing), label = "tool_card_scale")

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350, delayMillis = tool.delay)) + expandVertically(),
        exit = fadeOut(tween(150)) + shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.shadow(4.dp, RoundedCornerShape(23.dp)).clickable(interactionSource, indication = null, onClick = tool.onClick),
            shape = RoundedCornerShape(23.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Surface(Modifier.size(54.dp), RoundedCornerShape(17.dp), tool.colorStart.copy(alpha = .12f)) {
                        Box(contentAlignment = Alignment.Center) { Text(tool.emoji, fontSize = 25.sp) }
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tool.title, color = ClinicalInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(tool.subtitle, color = tool.colorStart, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(tool.description, color = ClinicalSecondary, fontSize = 11.sp, lineHeight = 17.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = tool.colorStart, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                }
                Spacer(Modifier.height(10.dp))
                Surface(onClick = { expanded = !expanded }, shape = RoundedCornerShape(12.dp), color = tool.colorStart.copy(alpha = .08f), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = tool.colorStart, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (expanded) "Hide clinical notes" else "View capabilities & limits", color = tool.colorStart, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = tool.colorStart)
                    }
                }
                AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(Modifier.padding(top = 11.dp)) {
                        Text("CAPABILITIES", color = Color(0xFF198754), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(tool.capabilities, color = ClinicalInk, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp, bottom = 9.dp))
                        Text("LIMITS", color = ClinicalRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(tool.limitations, color = ClinicalInk, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClinicalSafetyCard() {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ClinicalAmberSoft), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Surface(Modifier.size(38.dp), CircleShape, Color.White.copy(alpha = .7f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.WarningAmber, null, tint = ClinicalAmber, modifier = Modifier.size(21.dp)) } }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Clinical calculation aid", color = ClinicalInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text("Confirm the medication order, concentration, patient weight and local protocol before administration.", color = ClinicalSecondary, fontSize = 10.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun SmartWebCalculatorResolverCard(query: String, onOpenUrl: (String) -> Unit) {
    val cleanQuery = query.trim()
    val encodedQuery = Uri.encode(cleanQuery)
    val isAlligation = cleanQuery.contains("%") && (cleanQuery.contains("mix") || cleanQuery.contains("using") || cleanQuery.contains("හා"))
    val webSources = listOf(
        WebCalculatorSource("MDCalc Medical Engine", "Evidence-based clinical decision rules", "https://www.mdcalc.com/calc?search=$encodedQuery", ClinicalCyan),
        WebCalculatorSource("Medscape Calculator Hub", "Clinical dosing and equations", "https://reference.medscape.com/search?q=$encodedQuery+calculator", ClinicalBlue),
        WebCalculatorSource("ClinCalc Online Suite", "Pharmacokinetics and equations", "https://clincalc.com/?s=$encodedQuery", Color(0xFF43A047)),
        WebCalculatorSource("NCBI / NIH", "Biomedical literature and guidance", "https://pubmed.ncbi.nlm.nih.gov/?term=$encodedQuery+calculation+formula", ClinicalPurple)
    )
    Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(46.dp), CircleShape, ClinicalInk) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.TravelExplore, null, tint = ClinicalCyan, modifier = Modifier.size(25.dp)) } }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (isAlligation) "Complex admixture detected" else "Clinical web resolver", color = ClinicalInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (isAlligation) "This route needs Pearson's Square / alligation" else "No exact offline engine matched this request", color = ClinicalSecondary, fontSize = 10.sp)
                }
            }
            Text(if (isAlligation) "The app keeps unsupported calculations outside its deterministic engines and provides reference destinations instead." else "Use an external reference only when the requested calculation is outside the supported offline tools.", color = ClinicalSecondary, fontSize = 11.sp, lineHeight = 17.sp)
            HorizontalDivider(color = Color(0xFFE8EDF3))
            webSources.forEach { source ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ClinicalBg).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)).clickable { onOpenUrl(source.url) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(source.badgeColor, CircleShape))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(source.name, color = ClinicalInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text(source.subtitle, color = ClinicalSecondary, fontSize = 9.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = ClinicalBlue, modifier = Modifier.size(17.dp))
                }
            }
            Button(onClick = {
                val searchString = if (isAlligation) "$cleanQuery alligation pearson square calculator nursing" else "$cleanQuery medical clinical calculation formula guidelines"
                onOpenUrl("https://www.google.com/search?q=" + Uri.encode(searchString))
            }, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = ClinicalInk)) {
                Icon(Icons.Default.Language, null, tint = Color.White, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Search medical references", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
