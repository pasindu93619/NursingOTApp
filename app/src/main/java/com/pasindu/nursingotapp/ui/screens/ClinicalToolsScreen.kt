package com.pasindu.nursingotapp.ui.screens

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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TravelExplore
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// --- THEME PALETTE ---
private val ToolsBgWhite = Color(0xFFF4F7FB)
private val ToolsSlateDark = Color(0xFF0F172A)
private val ToolsSlateLight = Color(0xFF64748B)
private val TechBluePrimary = Color(0xFF1976D2)
private val AiPurple = Color(0xFFD500F9)
private val AiCyan = Color(0xFF00E5FF)

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
    val clinicalIntentSummary: String
)

data class WebCalculatorSource(
    val name: String,
    val subtitle: String,
    val url: String,
    val badgeColor: Color
)

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
    var isVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // --- COMPLETE CLINICAL TOOLS REPOSITORY WITH MULTILINGUAL NLP INTENT MAPPINGS ---
    val allTools = remember {
        listOf(
            ClinicalToolModule(
                id = "emergency", title = "Crash Cart Engine", subtitle = "Cardiac Arrest, Anaphylaxis, RSI",
                description = "• Instant Parallel Processing\n• Weight-Based Resuscitation\n• Code Red Animated ECG",
                emoji = "🚨", colorStart = Color(0xFFFF1744), colorEnd = Color(0xFFD50000), delay = 50,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToEmergency() },
                keywords = listOf("crash", "cart", "cardiac", "arrest", "anaphylaxis", "rsi", "emergency", "code red", "resuscitation", "cpr", "adrenaline", "epinephrine", "shock", "defib"),
                sinhalaKeywords = listOf("හෘදයාබාධ", "ඇඩ්‍රිනලින්", "හදිසි", "ශොක්", "ඇනෆිලැක්සිස්", "cpr", "arrest", "hadisi", "hadawatha", "adrenalin", "shock"),
                clinicalIntentSummary = "Emergency Resuscitation, Shock Protocols & Cardiac Arrest"
            ),
            ClinicalToolModule(
                id = "icu", title = "ICU Critical Care", subtitle = "Vasoactive, Sedation & Fluids",
                description = "• Inotrope Dose-Rate (μg/kg/min)\n• Electrolyte Repletion (K+, Mg++)\n• TPN & Pharmacokinetics",
                emoji = "🫀", colorStart = Color(0xFF2979FF), colorEnd = Color(0xFF0D47A1), delay = 100,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIcu() },
                keywords = listOf("icu", "critical", "vasoactive", "sedation", "fluid", "infusion", "electrolyte", "potassium", "kcl", "noradrenaline", "dopamine", "inotrope", "magnesium"),
                sinhalaKeywords = listOf("අයිසීයූ", "පොටෑසියම්", "නොරැඩ්‍රිනලින්", "ඉලෙක්ට්‍රොලයිට්", "potassium", "noradrenalin", "icu"),
                clinicalIntentSummary = "Inotropes, Vasoactive Drips & Electrolyte Balancing"
            ),
            ClinicalToolModule(
                id = "dosage", title = "Advanced Dosage", subtitle = "5-in-1 Math Engine",
                description = "• Standard Liquid & % Solutions\n• Dilutions (C₁V₁=C₂V₂)\n• Powder Reconstitution",
                emoji = "💊", colorStart = Color(0xFFD500F9), colorEnd = Color(0xFF4A148C), delay = 150,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToDosage() },
                keywords = listOf("dosage", "math", "dilution", "reconstitution", "powder", "dextrose", "concentration", "liquid", "c1v1", "c2v2", "ampoule", "vial", "diluent"),
                sinhalaKeywords = listOf("ඩෙක්ස්ට්‍රෝස්", "දියාරු", "කුඩු", "මාත්‍රාව", "බෙහෙත්", "ප්‍රතිශතය", "dextrose", "diyaru", "kudu", "mathrawa", "beheth", "dilution"),
                clinicalIntentSummary = "Dextrose Dilutions, Reconstitution & Liquid Dosage"
            ),
            ClinicalToolModule(
                id = "iv_drip", title = "IV Drip Sync", subtitle = "Drops Per Minute & AR",
                description = "• Macro (10/15/20) & Micro (60) Sets\n• Live AR Hologram Synchronization\n• 15-Second Clinical Tap Verification",
                emoji = "💧", colorStart = Color(0xFF00E5FF), colorEnd = Color(0xFF006064), delay = 200,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIvDrip() },
                keywords = listOf("iv", "drip", "sync", "drops", "minute", "macro", "micro", "flow rate", "drop factor", "gtt", "saline", "normal saline", "dpm"),
                sinhalaKeywords = listOf("ඩ්‍රිප්", "බිංදු", "පැයට බිංදු", "සේලයින්", "වතුර", "iv drip", "drip eka", "salin", "bindu"),
                clinicalIntentSummary = "Gravity IV Drip Calibration & Drop Factor Math"
            ),
            ClinicalToolModule(
                id = "special_calcs", title = "High-Alert Specials", subtitle = "Insulin, Heparin, PCA",
                description = "• Sliding Scale & IV Insulin\n• Heparin Weight-Based Protocols\n• Opioid PCA Lockout Limits",
                emoji = "🩸", colorStart = Color(0xFFE53935), colorEnd = Color(0xFFB71C1C), delay = 250,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToSpecialCalcs() },
                keywords = listOf("high alert", "insulin", "heparin", "pca", "opioid", "sliding scale", "actrapid", "blood sugar", "glucose", "dka"),
                sinhalaKeywords = listOf("ඉන්සියුලින්", "හෙපරින්", "සීනි", "ග්ලූකෝස්", "රුධිර සීනි", "insulin", "sugar", "seeni", "heparin", "dka"),
                clinicalIntentSummary = "Continuous IV Insulin Protocols & Heparin"
            ),
            ClinicalToolModule(
                id = "weight_infusion", title = "Weight & Infusions", subtitle = "mg/kg & mcg/kg/min",
                description = "• Pediatric Simple Dosing\n• Complex Continuous Infusion Rates\n• Built-in Safety & Weight Alerts",
                emoji = "⚖️", colorStart = Color(0xFF00BFA5), colorEnd = Color(0xFF004D40), delay = 300,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToWeightInfusion() },
                keywords = listOf("weight", "infusion", "mg/kg", "mcg/kg/min", "continuous", "rate", "body weight"),
                sinhalaKeywords = listOf("බර", "බර අනුව", "ශරීර බර", "කිලෝග්‍රෑම්", "bara", "weight", "kg"),
                clinicalIntentSummary = "Weight-Based Continuous Drug Infusion Rates"
            ),
            ClinicalToolModule(
                id = "conversions", title = "Unit Conversions", subtitle = "Mass, Volume & mEq",
                description = "• Metric & Household Equivalents\n• Electrolyte mEq to mg Engine\n• Instant Bi-Directional Translation",
                emoji = "🔄", colorStart = Color(0xFF651FFF), colorEnd = Color(0xFF311B92), delay = 350,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToConversions() },
                keywords = listOf("unit", "conversion", "convert", "mass", "volume", "meq", "mg", "mcg", "grams", "micrograms"),
                sinhalaKeywords = listOf("හුවමාරුව", "ඒකක", "මිලිග්‍රෑම්", "පරිවර්තනය", "unit", "convert"),
                clinicalIntentSummary = "Bi-Directional Metric, Mass & Electrolyte Conversions"
            ),
            ClinicalToolModule(
                id = "bsa", title = "BSA & Chemo", subtitle = "Mosteller BSA (m²)",
                description = "• Chemotherapy Surface Area Dosing\n• High-Risk Pediatric Calculations\n• Height/Weight Nomogram Engine",
                emoji = "📏", colorStart = Color(0xFFF50057), colorEnd = Color(0xFF880E4F), delay = 400,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToBsa() },
                keywords = listOf("bsa", "chemo", "chemotherapy", "mosteller", "surface area", "body surface area", "m2", "height weight", "oncology"),
                sinhalaKeywords = listOf("පිළිකා", "කීමෝ", "ශරීර වර්ගඵලය", "උස බර", "chemo", "cancer", "pilika", "bsa"),
                clinicalIntentSummary = "Oncology Body Surface Area (BSA) & Chemotherapy Dosing"
            ),
            ClinicalToolModule(
                id = "pediatric", title = "Legacy Paediatric", subtitle = "Clark, Young & Fried",
                description = "• Age & Weight Approximation Rules\n• Fraction of Adult Dose Calculation\n• Built-in Legacy Safety Limits",
                emoji = "🧒", colorStart = Color(0xFFFF9100), colorEnd = Color(0xFFE65100), delay = 450,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToPediatric() },
                keywords = listOf("pediatric", "paediatric", "clark", "young", "fried", "child", "age", "infant", "baby", "child dose"),
                sinhalaKeywords = listOf("ළමා", "ළමුන්", "බබා", "ළමා මාත්‍රාව", "වයස", "lamaa", "baba", "lamun", "pediatric"),
                clinicalIntentSummary = "Pediatric Rule Approximations (Clark, Young, Fried)"
            )
        )
    }

    // --- ADVANCED NATURAL LANGUAGE INTENT PARSING ENGINE ---
    val searchIntentResult = remember(searchQuery) {
        val trimmed = searchQuery.trim().lowercase()
        if (trimmed.isBlank()) {
            return@remember Pair<ClinicalToolModule?, List<ClinicalToolModule>>(null, allTools)
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

            // Semantic Overrides for highly common nursing queries
            if (trimmed.contains("dextrose") || trimmed.contains("dilut") || trimmed.contains("powder") || trimmed.contains("කුඩු") || trimmed.contains("දියාරු")) {
                if (tool.id == "dosage") score += 60
            }
            if (trimmed.contains("arrest") || trimmed.contains("cpr") || trimmed.contains("adrenaline") || trimmed.contains("හදිසි")) {
                if (tool.id == "emergency") score += 60
            }
            if (trimmed.contains("drip") || trimmed.contains("gtt") || trimmed.contains("බිංදු") || trimmed.contains("සේලයින්")) {
                if (tool.id == "iv_drip") score += 60
            }
            if (trimmed.contains("insulin") || trimmed.contains("sugar") || trimmed.contains("ඉන්සියුලින්")) {
                if (tool.id == "special_calcs") score += 60
            }
            if (trimmed.contains("noradrenalin") || trimmed.contains("icu") || trimmed.contains("potassium") || trimmed.contains("පොටෑසියම්")) {
                if (tool.id == "icu") score += 60
            }
            if (trimmed.contains("chemo") || trimmed.contains("bsa") || trimmed.contains("පිළිකා")) {
                if (tool.id == "bsa") score += 60
            }
            if (trimmed.contains("child") || trimmed.contains("baby") || trimmed.contains("lamaa") || trimmed.contains("ළමා")) {
                if (tool.id == "pediatric") score += 60
            }

            Pair(tool, score)
        }

        val bestMatch = scoredTools.filter { it.second >= 30 }.maxByOrNull { it.second }?.first
        val matchingList = scoredTools.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }

        Pair(bestMatch, matchingList)
    }

    val bestMatchTool = searchIntentResult.first
    val filteredTools = if (searchQuery.isBlank()) allTools else searchIntentResult.second

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Clinical Tools", fontWeight = FontWeight.Black, color = ToolsSlateDark, fontSize = 22.sp)
                        Text("Medical Math & AI Guidance", fontSize = 12.sp, color = TechBluePrimary, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ToolsSlateDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(ToolsBgWhite)) {

            ToolsSmoothMeshBackground(isVisible)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // --- ADVANCED MULTILINGUAL AI SEARCH BAR ---
                AdvancedAiSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSubmit = {
                        keyboardController?.hide()
                        if (bestMatchTool != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            bestMatchTool.onClick()
                        }
                    }
                )

                // --- REAL-TIME HIGH-CONFIDENCE INTENT DETECTOR BANNER ---
                AnimatedVisibility(
                    visible = searchQuery.isNotBlank() && bestMatchTool != null,
                    enter = slideInVertically { -20 } + fadeIn(),
                    exit = slideOutVertically { -20 } + fadeOut()
                ) {
                    if (bestMatchTool != null) {
                        BestIntentMatchBadge(
                            tool = bestMatchTool,
                            onLaunch = {
                                keyboardController?.hide()
                                bestMatchTool.onClick()
                            }
                        )
                    }
                }

                Text(
                    text = if (searchQuery.isBlank()) "Select a clinical engine:" else "Matching Calculators (${filteredTools.size}):",
                    fontSize = 15.sp, color = ToolsSlateLight, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                // Render Cards or Fallback
                if (filteredTools.isNotEmpty()) {
                    filteredTools.forEach { tool ->
                        PremiumToolCard(
                            visible = isVisible, delay = tool.delay,
                            title = tool.title, subtitle = tool.subtitle, description = tool.description,
                            emoji = tool.emoji, color1 = tool.colorStart, color2 = tool.colorEnd,
                            onClick = tool.onClick
                        )
                    }
                } else {
                    // --- SMART CLINICAL WEB SUPPORT FALLBACK ---
                    SmartWebCalculatorResolverCard(query = searchQuery) { targetUrl ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                        context.startActivity(intent)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🌟 AI SEARCH BAR & INTENT BADGE 🌟
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AdvancedAiSearchBar(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_border_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = AiPurple.copy(alpha = glowAlpha))
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(2.dp, Brush.linearGradient(listOf(TechBluePrimary, AiPurple, AiCyan)), RoundedCornerShape(24.dp)),
        placeholder = { Text("Ask AI: '25% dextrose dilution'", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Powered", tint = AiPurple, modifier = Modifier.size(24.dp)) },
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

@Composable
fun BestIntentMatchBadge(tool: ClinicalToolModule, onLaunch: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp), spotColor = tool.colorStart.copy(alpha = 0.4f)).clip(RoundedCornerShape(20.dp)).clickable { onLaunch() },
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0F172A), tool.colorStart.copy(alpha = 0.25f)))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).background(tool.colorStart, CircleShape), contentAlignment = Alignment.Center) { Text(tool.emoji, fontSize = 22.sp) }
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
                    Text(tool.clinicalIntentSummary, fontSize = 11.sp, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onLaunch, modifier = Modifier.background(tool.colorStart, CircleShape).size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🌐 SMART CLINICAL WEB CALCULATOR RESOLVER CARD 🌐
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SmartWebCalculatorResolverCard(query: String, onOpenUrl: (String) -> Unit) {
    val cleanQuery = query.trim()
    val encodedQuery = Uri.encode(cleanQuery)

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
                    Text("Clinical Web Resolver", fontSize = 18.sp, fontWeight = FontWeight.Black, color = ToolsSlateDark)
                    Text("No exact offline match for: \"$cleanQuery\"", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }

            Text("Our AI has generated verified clinical links to perform this calculation directly online:", fontSize = 13.sp, color = ToolsSlateLight, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
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
                onClick = { onOpenUrl("https://www.google.com/search?q=" + Uri.encode("$cleanQuery medical clinical calculation formula guidelines")) },
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Google Medical Guidelines", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🌊 SMOOTH LIQUID MESH BACKGROUND 🌊
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ToolsSmoothMeshBackground(isVisible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val phase1 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(25000, easing = LinearEasing)), label = "")
    val phase2 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "")
    val alphaAnim by animateFloatAsState(if (isVisible) 1f else 0f, tween(2000), label = "")

    Canvas(modifier = Modifier.fillMaxSize().alpha(alphaAnim)) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        drawRect(Color(0xFFF4F7FB))
        val r1 = maxOf(1f, w * 0.8f); val r2 = maxOf(1f, w * 0.9f); val r3 = maxOf(1f, w * 0.7f)

        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE3F2FD).copy(alpha = 0.8f), Color.Transparent), center = Offset(w * 0.3f + (sin(phase1) * w * 0.3f).toFloat(), h * 0.2f + (cos(phase2) * h * 0.1f).toFloat()), radius = r1), center = Offset(w * 0.3f + (sin(phase1) * w * 0.3f).toFloat(), h * 0.2f + (cos(phase2) * h * 0.1f).toFloat()), radius = r1)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE0F7FA).copy(alpha = 0.6f), Color.Transparent), center = Offset(w * 0.7f + (cos(phase1) * w * 0.2f).toFloat(), h * 0.6f + (sin(phase2) * h * 0.2f).toFloat()), radius = r2), center = Offset(w * 0.7f + (cos(phase1) * w * 0.2f).toFloat(), h * 0.6f + (sin(phase2) * h * 0.2f).toFloat()), radius = r2)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF3E5F5).copy(alpha = 0.5f), Color.Transparent), center = Offset(w * 0.5f + (sin(phase2) * w * 0.4f).toFloat(), h * 0.8f), radius = r3), center = Offset(w * 0.5f + (sin(phase2) * w * 0.4f).toFloat(), h * 0.8f), radius = r3)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3D INTERACTIVE PREMIUM TOOL CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PremiumToolCard(
    visible: Boolean, delay: Int, title: String, subtitle: String,
    description: String, emoji: String, color1: Color, color2: Color, onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "")

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(tween(500, delayMillis = delay))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().scale(scale).zIndex(if (isPressed) 10f else 0f).shadow(elevation = if (isPressed) 6.dp else 14.dp, shape = RoundedCornerShape(24.dp), spotColor = color1.copy(alpha = 0.45f)).clip(RoundedCornerShape(24.dp)).background(Color.White).clickable(interactionSource = interactionSource, indication = null, onClick = onClick).border(1.dp, color1.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
        ) {
            Row(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color.White, color1.copy(alpha = 0.06f)))).padding(20.dp), verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(60.dp).shadow(8.dp, CircleShape, spotColor = color1).background(Brush.linearGradient(listOf(color1, color2)), CircleShape).border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = title, fontSize = 19.sp, fontWeight = FontWeight.Black, color = ToolsSlateDark, letterSpacing = (-0.5).sp)
                    Text(text = subtitle, fontSize = 13.sp, color = color1, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = description, fontSize = 13.sp, color = ToolsSlateLight, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}