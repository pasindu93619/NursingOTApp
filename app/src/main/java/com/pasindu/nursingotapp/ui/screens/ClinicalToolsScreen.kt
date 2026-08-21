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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// --- COLORS ---
private val ToolsBgWhite = Color(0xFFF4F7FB)
private val ToolsSlateDark = Color(0xFF0F172A)
private val ToolsSlateLight = Color(0xFF64748B)

// Data Model for the Smart AI Search Engine
data class ClinicalToolModule(
    val title: String,
    val subtitle: String,
    val description: String,
    val emoji: String,
    val color1: Color,
    val color2: Color,
    val delay: Int,
    val onClick: () -> Unit,
    val keywords: List<String>
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

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Smart Engine Database
    val allTools = listOf(
        ClinicalToolModule(
            title = "Crash Cart Engine", subtitle = "Cardiac Arrest, Anaphylaxis, RSI",
            description = "• Instant Parallel Processing\n• Weight-Based Resuscitation\n• Code Red Animated ECG",
            emoji = "🚨", color1 = Color(0xFFFF1744), color2 = Color(0xFFD50000), delay = 50,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToEmergency() },
            keywords = listOf("crash", "cart", "cardiac", "arrest", "anaphylaxis", "rsi", "emergency", "code red", "resuscitation", "cpr", "adrenaline")
        ),
        ClinicalToolModule(
            title = "ICU Critical Care", subtitle = "Vasoactive, Sedation & Fluids",
            description = "• Inotrope Dose-Rate (μg/kg/min)\n• Electrolyte Repletion (K+, Mg++)\n• TPN & Pharmacokinetics",
            emoji = "🫀", color1 = Color(0xFF2979FF), color2 = Color(0xFF0D47A1), delay = 100,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIcu() },
            keywords = listOf("icu", "critical", "vasoactive", "sedation", "fluid", "infusion", "electrolyte", "potassium", "kcl", "noradrenaline")
        ),
        ClinicalToolModule(
            title = "Unit Conversions", subtitle = "Mass, Volume & mEq",
            description = "• Metric & Household Equivalents\n• Electrolyte mEq to mg Engine\n• Instant Bi-Directional Translation",
            emoji = "🔄", color1 = Color(0xFF651FFF), color2 = Color(0xFF311B92), delay = 150,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToConversions() },
            keywords = listOf("unit", "conversion", "mass", "volume", "meq", "mg", "mcg", "metric")
        ),
        ClinicalToolModule(
            title = "High-Alert Specials", subtitle = "Insulin, Heparin, PCA",
            description = "• Sliding Scale & IV Insulin\n• Heparin Weight-Based Protocols\n• Opioid PCA Lockout Limits",
            emoji = "🩸", color1 = Color(0xFFE53935), color2 = Color(0xFFB71C1C), delay = 200,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToSpecialCalcs() },
            keywords = listOf("high alert", "insulin", "heparin", "pca", "opioid", "sliding scale")
        ),
        ClinicalToolModule(
            title = "IV Drip Sync", subtitle = "Drops Per Minute & AR",
            description = "• Macro (10/15/20) & Micro (60) Sets\n• Live AR Hologram Synchronization\n• 15-Second Clinical Tap Verification",
            emoji = "💧", color1 = Color(0xFF00E5FF), color2 = Color(0xFF006064), delay = 250,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIvDrip() },
            keywords = listOf("iv", "drip", "sync", "drops", "minute", "macro", "micro", "flow rate")
        ),
        ClinicalToolModule(
            title = "Advanced Dosage", subtitle = "5-in-1 Math Engine",
            description = "• Standard Liquid & % Solutions\n• Dilutions (C₁V₁=C₂V₂)\n• Powder Reconstitution",
            emoji = "💊", color1 = Color(0xFFD500F9), color2 = Color(0xFF4A148C), delay = 300,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToDosage() },
            keywords = listOf("dosage", "math", "dilution", "reconstitution", "powder", "dextrose", "concentration", "liquid", "c1v1")
        ),
        ClinicalToolModule(
            title = "Weight & Infusions", subtitle = "mg/kg & mcg/kg/min",
            description = "• Pediatric Simple Dosing\n• Complex Continuous Infusion Rates\n• Built-in Safety & Weight Alerts",
            emoji = "⚖️", color1 = Color(0xFF00BFA5), color2 = Color(0xFF004D40), delay = 350,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToWeightInfusion() },
            keywords = listOf("weight", "infusion", "mg/kg", "mcg/kg/min", "continuous", "rate")
        ),
        ClinicalToolModule(
            title = "BSA & Chemo", subtitle = "Mosteller BSA (m²)",
            description = "• Chemotherapy Surface Area Dosing\n• High-Risk Pediatric Calculations\n• Height/Weight Nomogram Engine",
            emoji = "📏", color1 = Color(0xFFF50057), color2 = Color(0xFF880E4F), delay = 400,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToBsa() },
            keywords = listOf("bsa", "chemo", "mosteller", "surface area", "height", "nomogram")
        ),
        ClinicalToolModule(
            title = "Legacy Paediatric", subtitle = "Clark, Young & Fried",
            description = "• Age & Weight Approximation Rules\n• Fraction of Adult Dose Calculation\n• Built-in Legacy Safety Limits",
            emoji = "🧒", color1 = Color(0xFFFF9100), color2 = Color(0xFFE65100), delay = 450,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToPediatric() },
            keywords = listOf("pediatric", "paediatric", "clark", "young", "fried", "child", "age")
        )
    )

    // The AI Search Filter Logic
    val filteredTools = allTools.filter { tool ->
        searchQuery.isBlank() ||
                tool.title.contains(searchQuery, ignoreCase = true) ||
                tool.subtitle.contains(searchQuery, ignoreCase = true) ||
                tool.keywords.any { it.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Tools", fontWeight = FontWeight.Black, color = ToolsSlateDark, fontSize = 22.sp) },
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

                // 🌟 MIND-BLOWING AI SEARCH BAR 🌟
                AiClinicalSearchBar(searchQuery) { searchQuery = it }

                Text(
                    text = if (searchQuery.isBlank()) "Select a clinical engine:" else "AI Search Results:",
                    fontSize = 15.sp,
                    color = ToolsSlateLight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                // Render Cards or Fallback Web Search
                if (filteredTools.isNotEmpty()) {
                    filteredTools.forEach { tool ->
                        PremiumToolCard(
                            visible = isVisible, delay = tool.delay,
                            title = tool.title, subtitle = tool.subtitle,
                            description = tool.description, emoji = tool.emoji,
                            color1 = tool.color1, color2 = tool.color2,
                            onClick = tool.onClick
                        )
                    }
                } else {
                    // 🌟 WEB SEARCH FALLBACK CARD 🌟
                    AiWebSearchFallbackCard(query = searchQuery) {
                        val encodedQuery = Uri.encode("$searchQuery nursing clinical calculation formula")
                        val uri = Uri.parse("https://www.google.com/search?q=$encodedQuery")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🌟 AI SMART SEARCH UI 🌟
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiClinicalSearchBar(query: String, onQueryChange: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = ""
    )

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFFD500F9).copy(alpha = glowAlpha))
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(2.dp, Brush.linearGradient(listOf(Color(0xFF2979FF), Color(0xFFD500F9))), RoundedCornerShape(24.dp)),
        placeholder = { Text("Ask AI: '25% dextrose dilution'", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFFD500F9)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray) }
            } else {
                Icon(Icons.Default.Search, tint = Color.Gray, contentDescription = "Search")
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Black, color = ToolsSlateDark)
    )
}

@Composable
fun AiWebSearchFallbackCard(query: String, onSearchClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "web_glow")
    val pulseScale by infiniteTransition.animateFloat(initialValue = 0.98f, targetValue = 1.02f, animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp)).scale(pulseScale).clickable { onSearchClick() },
        shape = RoundedCornerShape(24.dp)
    ) {
        val gradient = Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF2979FF)), start = Offset(0f, 0f), end = Offset(1000f, 1000f))
        Column(
            modifier = Modifier.fillMaxWidth().background(gradient).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(70.dp).background(Color.White.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.TravelExplore, contentDescription = "Web", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Text("Calculator Not Found", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("Our AI can search the clinical web to find the formula and guidelines for:\n\n\"$query\"", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
            Button(onClick = onSearchClick, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Search Clinical Web", color = Color(0xFF1976D2), fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

// ─── SMOOTH LIQUID MESH BACKGROUND ───
@Composable
fun ToolsSmoothMeshBackground(isVisible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val phase1 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(25000, easing = LinearEasing)), label = "")
    val phase2 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "")

    val alphaAnim by animateFloatAsState(if (isVisible) 1f else 0f, tween(2000), label = "")

    Canvas(modifier = Modifier.fillMaxSize().alpha(alphaAnim)) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas // CRASH FIX

        drawRect(Color(0xFFF4F7FB))

        val r1 = maxOf(1f, w * 0.8f)
        val r2 = maxOf(1f, w * 0.9f)
        val r3 = maxOf(1f, w * 0.7f)

        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFE3F2FD).copy(alpha = 0.8f), Color.Transparent), center = Offset(w * 0.3f + (sin(phase1) * w * 0.3f).toFloat(), h * 0.2f + (cos(phase2) * h * 0.1f).toFloat()), radius = r1),
            center = Offset(w * 0.3f + (sin(phase1) * w * 0.3f).toFloat(), h * 0.2f + (cos(phase2) * h * 0.1f).toFloat()),
            radius = r1
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFE0F7FA).copy(alpha = 0.6f), Color.Transparent), center = Offset(w * 0.7f + (cos(phase1) * w * 0.2f).toFloat(), h * 0.6f + (sin(phase2) * h * 0.2f).toFloat()), radius = r2),
            center = Offset(w * 0.7f + (cos(phase1) * w * 0.2f).toFloat(), h * 0.6f + (sin(phase2) * h * 0.2f).toFloat()),
            radius = r2
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFF3E5F5).copy(alpha = 0.5f), Color.Transparent), center = Offset(w * 0.5f + (sin(phase2) * w * 0.4f).toFloat(), h * 0.8f), radius = r3),
            center = Offset(w * 0.5f + (sin(phase2) * w * 0.4f).toFloat(), h * 0.8f),
            radius = r3
        )
    }
}

// ─── 3D INTERACTIVE PREMIUM TOOL CARD ───
@Composable
fun PremiumToolCard(
    visible: Boolean, delay: Int, title: String, subtitle: String,
    description: String, emoji: String, color1: Color, color2: Color, onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "press_physics"
    )

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(tween(500, delayMillis = delay))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .zIndex(if (isPressed) 10f else 0f)
                .shadow(elevation = if (isPressed) 6.dp else 16.dp, shape = RoundedCornerShape(24.dp), spotColor = color1.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .border(1.dp, color1.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color.White, color1.copy(alpha = 0.05f)))).padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.size(60.dp).shadow(8.dp, CircleShape, spotColor = color1).background(Brush.linearGradient(listOf(color1, color2)), CircleShape).border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
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